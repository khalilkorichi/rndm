package com.rndm.app.data.repository

import com.rndm.app.data.local.dao.MatchDao
import com.rndm.app.data.local.dao.PlayerProfileDao
import com.rndm.app.data.local.dao.ProfileDao
import com.rndm.app.data.local.dao.TournamentDao
import com.rndm.app.data.local.entity.MatchEntity
import com.rndm.app.data.local.entity.PlayerProfileEntity
import com.rndm.app.data.local.entity.TournamentEntity
import com.rndm.app.data.local.entity.TournamentParticipantEntity
import com.rndm.app.data.mapper.toDomain
import com.rndm.app.domain.model.MatchOutcome
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.PlayerCareerStats
import com.rndm.app.domain.model.PlayerHeadToHead
import com.rndm.app.domain.model.PlayerLeaderboardItem
import com.rndm.app.domain.model.PlayerMatchRecord
import com.rndm.app.domain.model.PlayerQuickStats
import com.rndm.app.domain.model.PlayerTournamentParticipation
import com.rndm.app.domain.model.ProfileType
import com.rndm.app.domain.model.TournamentType
import com.rndm.app.domain.model.isRealPlayerName
import com.rndm.app.domain.model.StageReachedType
import com.rndm.app.domain.repository.PlayerProfileRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlayerProfileRepositoryImpl @Inject constructor(
    private val tournamentDao: TournamentDao,
    private val matchDao: MatchDao,
    private val playerProfileDao: PlayerProfileDao,
    private val profileDao: ProfileDao,
    private val ioDispatcher: CoroutineDispatcher
) : PlayerProfileRepository {

    override fun getPlayerCareerStats(playerName: String): Flow<PlayerCareerStats> {
        return combine(
            matchDao.getAllMatches(),
            tournamentDao.getAllTournaments(),
            tournamentDao.getAllParticipants(),
            playerProfileDao.getPlayerProfileByName(playerName)
        ) { matches, tournaments, participants, customProfile ->
            calculatePlayerCareerStats(playerName, matches, tournaments, participants, customProfile)
        }.flowOn(ioDispatcher)
    }

    override fun getPlayerTournamentHistory(playerName: String): Flow<List<PlayerTournamentParticipation>> {
        return combine(
            matchDao.getAllMatches(),
            tournamentDao.getAllTournaments(),
            tournamentDao.getAllParticipants()
        ) { matches, tournaments, participants ->
            calculateTournamentHistory(playerName, matches, tournaments, participants)
        }.flowOn(ioDispatcher)
    }

    override fun getPlayerMatchHistory(playerName: String): Flow<List<PlayerMatchRecord>> {
        return combine(
            matchDao.getAllMatches(),
            tournamentDao.getAllTournaments()
        ) { matches, tournaments ->
            calculateMatchHistory(playerName, matches, tournaments)
        }.flowOn(ioDispatcher)
    }

    override fun getPlayerHeadToHead(playerName: String): Flow<List<PlayerHeadToHead>> {
        return combine(
            matchDao.getAllMatches(),
            playerProfileDao.getAllPlayerProfiles()
        ) { matches, customProfiles ->
            calculateHeadToHead(playerName, matches, customProfiles)
        }.flowOn(ioDispatcher)
    }

    override fun getAllPlayersLeaderboard(): Flow<List<PlayerLeaderboardItem>> {
        return combine(
            matchDao.getAllMatches(),
            tournamentDao.getAllTournaments(),
            tournamentDao.getAllParticipants(),
            playerProfileDao.getAllPlayerProfiles(),
            profileDao.getAllProfilesWithItems()
        ) { matches, tournaments, participants, customProfiles, allProfilesWithItems ->
            calculateLeaderboard(matches, tournaments, participants, customProfiles, allProfilesWithItems)
        }.flowOn(ioDispatcher)
    }

    override fun getPlayersQuickStats(playerNames: List<String>): Flow<Map<String, PlayerQuickStats>> {
        return combine(
            matchDao.getAllMatches(),
            tournamentDao.getAllTournaments(),
            tournamentDao.getAllParticipants(),
            playerProfileDao.getAllPlayerProfiles()
        ) { matches, tournaments, participants, customProfiles ->
            val profileMap = customProfiles.associateBy { it.name }
            val podiumMap = computeTournamentPodiums(tournaments, matches, participants)
            val resultMap = mutableMapOf<String, PlayerQuickStats>()

            playerNames.filter { it.isRealPlayerName() }.distinct().forEach { name ->
                val playerMatches = matches.filter {
                    (it.playerOneName == name || it.playerTwoName == name) && it.status == MatchStatus.FINISHED
                }
                var goals = 0
                var wins = 0
                val totalMatches = playerMatches.size

                val titles = podiumMap.values.count { it.champion == name }

                playerMatches.forEach { m ->
                    val isP1 = m.playerOneName == name
                    val s1 = m.scoreOne ?: 0
                    val s2 = m.scoreTwo ?: 0
                    val scored = if (isP1) s1 else s2
                    val conceded = if (isP1) s2 else s1
                    goals += scored

                    val isWin = m.winnerName == name || (scored > conceded && m.winnerName.isNullOrBlank())
                    if (isWin) wins++
                }

                val winRate = if (totalMatches > 0) (wins.toFloat() / totalMatches.toFloat()) * 100f else 0f
                val custom = profileMap[name]

                resultMap[name] = PlayerQuickStats(
                    playerName = name,
                    nickname = custom?.nickname,
                    avatarIcon = custom?.avatarIcon,
                    titlesCount = titles,
                    goalsScored = goals,
                    totalMatches = totalMatches,
                    winRate = winRate
                )
            }

            resultMap
        }.flowOn(ioDispatcher)
    }

    override suspend fun savePlayerCustomProfile(
        name: String,
        nickname: String?,
        avatarIcon: String?,
        favoriteClub: String?,
        notes: String?
    ) {
        withContext(ioDispatcher) {
            val existing = playerProfileDao.getPlayerProfileByNameSync(name)
            val now = System.currentTimeMillis()
            val entity = existing?.copy(
                nickname = nickname,
                avatarIcon = avatarIcon,
                favoriteClub = favoriteClub,
                notes = notes,
                updatedAt = now
            ) ?: PlayerProfileEntity(
                name = name,
                nickname = nickname,
                avatarIcon = avatarIcon,
                favoriteClub = favoriteClub,
                notes = notes,
                createdAt = now,
                updatedAt = now
            )
            playerProfileDao.insertOrUpdateProfile(entity)
        }
    }

    // --- Private Calculation Helpers ---

    private fun calculatePlayerCareerStats(
        playerName: String,
        matches: List<MatchEntity>,
        tournaments: List<TournamentEntity>,
        participants: List<TournamentParticipantEntity>,
        customProfile: PlayerProfileEntity?
    ): PlayerCareerStats {
        val tournamentMap = tournaments.associateBy { it.id }
        val playerMatches = matches.filter {
            (it.playerOneName == playerName || it.playerTwoName == playerName) && it.status == MatchStatus.FINISHED
        }

        var wins = 0
        var draws = 0
        var losses = 0
        var goalsScored = 0
        var goalsConceded = 0
        var cleanSheets = 0

        val clubUsageMap = mutableMapOf<String, Int>()
        var biggestWinMargin = 0
        var biggestWinDetail: String? = null

        playerMatches.forEach { match ->
            val isPlayerOne = match.playerOneName == playerName
            val myScore = (if (isPlayerOne) match.scoreOne else match.scoreTwo) ?: 0
            val oppScore = (if (isPlayerOne) match.scoreTwo else match.scoreOne) ?: 0
            val myClub = if (isPlayerOne) match.playerOneClub else match.playerTwoClub
            val oppName = if (isPlayerOne) match.playerTwoName ?: "BYE" else match.playerOneName

            if (!myClub.isNullOrBlank()) {
                clubUsageMap[myClub] = (clubUsageMap[myClub] ?: 0) + 1
            }

            goalsScored += myScore
            goalsConceded += oppScore

            if (oppScore == 0 && myScore > 0) {
                cleanSheets++
            }

            val isWinner = match.winnerName == playerName || (myScore > oppScore && match.winnerName.isNullOrBlank())
            val isDraw = myScore == oppScore && match.winnerName.isNullOrBlank()

            if (isWinner) {
                wins++
                val margin = myScore - oppScore
                if (margin > biggestWinMargin) {
                    biggestWinMargin = margin
                    val tName = tournamentMap[match.tournamentId]?.name ?: "بطولة"
                    biggestWinDetail = "$myScore - $oppScore ضد $oppName ($tName)"
                }
            } else if (isDraw) {
                draws++
            } else {
                losses++
            }
        }

        // Tournaments participated in
        val participatedTournamentIds = (participants.filter { it.playerName == playerName }.map { it.tournamentId } +
                playerMatches.map { it.tournamentId }).distinct()
        val totalTournaments = participatedTournamentIds.size

        // Calculate Titles (Champions), Runner-ups, and Third-places across all tournament types
        val podiumMap = computeTournamentPodiums(tournaments, matches, participants)
        var titlesCount = 0
        var runnerUpCount = 0
        var thirdPlaceCount = 0

        participatedTournamentIds.forEach { tId ->
            val podium = podiumMap[tId]
            if (podium != null) {
                if (podium.champion == playerName) titlesCount++
                if (podium.runnerUp == playerName) runnerUpCount++
                if (podium.thirdPlace == playerName) thirdPlaceCount++
            }
        }

        val totalMatches = playerMatches.size
        val winRate = if (totalMatches > 0) (wins.toFloat() / totalMatches.toFloat()) * 100f else 0f
        val avgGoals = if (totalMatches > 0) goalsScored.toFloat() / totalMatches.toFloat() else 0f
        val mostPlayedClub = clubUsageMap.maxByOrNull { it.value }?.key

        // Recent 5 Form
        val recentForm = playerMatches.take(5).map { match ->
            val isPlayerOne = match.playerOneName == playerName
            val myScore = (if (isPlayerOne) match.scoreOne else match.scoreTwo) ?: 0
            val oppScore = (if (isPlayerOne) match.scoreTwo else match.scoreOne) ?: 0
            val isWinner = match.winnerName == playerName || (myScore > oppScore && match.winnerName.isNullOrBlank())
            val isDraw = myScore == oppScore && match.winnerName.isNullOrBlank()

            when {
                isWinner -> MatchOutcome.WIN
                isDraw -> MatchOutcome.DRAW
                else -> MatchOutcome.LOSS
            }
        }

        val bestAchievement = when {
            titlesCount > 0 -> "بطل $titlesCount بطولة"
            runnerUpCount > 0 -> "وصيف $runnerUpCount بطولة"
            thirdPlaceCount > 0 -> "المركز الثالث في $thirdPlaceCount بطولة"
            totalTournaments > 0 -> "مشارك في $totalTournaments بطولة"
            else -> "لا توجد مشاركات سابقة"
        }

        return PlayerCareerStats(
            playerName = playerName,
            nickname = customProfile?.nickname,
            avatarIcon = customProfile?.avatarIcon,
            favoriteClub = customProfile?.favoriteClub ?: mostPlayedClub,
            notes = customProfile?.notes,
            totalTournaments = totalTournaments,
            titlesCount = titlesCount,
            runnerUpCount = runnerUpCount,
            thirdPlaceCount = thirdPlaceCount,
            bestAchievement = bestAchievement,
            totalMatches = totalMatches,
            totalWins = wins,
            totalDraws = draws,
            totalLosses = losses,
            winRatePercentage = winRate,
            goalsScored = goalsScored,
            goalsConceded = goalsConceded,
            goalDifference = goalsScored - goalsConceded,
            averageGoalsPerMatch = (avgGoals * 10).toInt() / 10f,
            cleanSheets = cleanSheets,
            biggestWin = biggestWinDetail,
            mostPlayedClub = mostPlayedClub,
            recentForm = recentForm
        )
    }

    private fun calculateTournamentHistory(
        playerName: String,
        matches: List<MatchEntity>,
        tournaments: List<TournamentEntity>,
        participants: List<TournamentParticipantEntity>
    ): List<PlayerTournamentParticipation> {
        val tournamentMap = tournaments.associateBy { it.id }
        val playerParticipations = participants.filter { it.playerName == playerName }
        val playerMatches = matches.filter { it.playerOneName == playerName || it.playerTwoName == playerName }
        val podiumMap = computeTournamentPodiums(tournaments, matches, participants)

        val tournamentIds = (playerParticipations.map { it.tournamentId } + playerMatches.map { it.tournamentId }).distinct()

        return tournamentIds.mapNotNull { tId ->
            val tournament = tournamentMap[tId] ?: return@mapNotNull null
            val tMatches = playerMatches.filter { it.tournamentId == tId && it.status == MatchStatus.FINISHED }
            val participant = playerParticipations.firstOrNull { it.tournamentId == tId }

            var matchesWon = 0
            var matchesDrawn = 0
            var matchesLost = 0
            var goalsFor = 0
            var goalsAgainst = 0

            val clubFromMatch = tMatches.firstNotNullOfOrNull { m ->
                if (m.playerOneName == playerName) m.playerOneClub else m.playerTwoClub
            }
            val clubName = participant?.clubName ?: clubFromMatch

            tMatches.forEach { m ->
                val isP1 = m.playerOneName == playerName
                val s1 = m.scoreOne ?: 0
                val s2 = m.scoreTwo ?: 0
                val myScore = if (isP1) s1 else s2
                val oppScore = if (isP1) s2 else s1

                goalsFor += myScore
                goalsAgainst += oppScore

                val isWinner = m.winnerName == playerName || (myScore > oppScore && m.winnerName.isNullOrBlank())
                val isDraw = myScore == oppScore && m.winnerName.isNullOrBlank()

                if (isWinner) matchesWon++
                else if (isDraw) matchesDrawn++
                else matchesLost++
            }

            // Determine stage reached
            val allTMatches = matches.filter { it.tournamentId == tId }
            val finalMatch = allTMatches.firstOrNull { it.stage == MatchStage.FINAL && (it.playerOneName == playerName || it.playerTwoName == playerName) }
            val thirdMatch = allTMatches.firstOrNull { it.stage == MatchStage.THIRD_PLACE && (it.playerOneName == playerName || it.playerTwoName == playerName) }
            val semiMatch = allTMatches.firstOrNull { it.stage == MatchStage.SEMI_FINALS && (it.playerOneName == playerName || it.playerTwoName == playerName) }
            val qfMatch = allTMatches.firstOrNull { it.stage == MatchStage.QUARTER_FINALS && (it.playerOneName == playerName || it.playerTwoName == playerName) }
            val r16Match = allTMatches.firstOrNull { it.stage == MatchStage.ROUND_OF_16 && (it.playerOneName == playerName || it.playerTwoName == playerName) }
            val r32Match = allTMatches.firstOrNull { it.stage == MatchStage.ROUND_OF_32 && (it.playerOneName == playerName || it.playerTwoName == playerName) }

            val podium = podiumMap[tId]

            val (stageTitle, stageType) = when {
                podium?.champion == playerName -> Pair("بطل البطولة", StageReachedType.CHAMPION)
                podium?.runnerUp == playerName -> Pair("وصيف البطولة", StageReachedType.RUNNER_UP)
                podium?.thirdPlace == playerName -> Pair("المركز الثالث", StageReachedType.THIRD_PLACE)
                finalMatch != null && finalMatch.winnerName == playerName -> Pair("بطل البطولة", StageReachedType.CHAMPION)
                finalMatch != null -> Pair("وصيف البطولة", StageReachedType.RUNNER_UP)
                thirdMatch != null && thirdMatch.winnerName == playerName -> Pair("المركز الثالث", StageReachedType.THIRD_PLACE)
                semiMatch != null -> Pair("نصف النهائي", StageReachedType.SEMI_FINALS)
                qfMatch != null -> Pair("ربع النهائي", StageReachedType.QUARTER_FINALS)
                r16Match != null -> Pair("دور الـ 16", StageReachedType.ROUND_OF_16)
                r32Match != null -> Pair("دور الـ 32", StageReachedType.ROUND_OF_32)
                tMatches.any { it.stage == MatchStage.GROUP_STAGE } -> Pair("دور المجموعات", StageReachedType.GROUPS_STAGE)
                else -> Pair("مشارك", StageReachedType.PARTICIPANT)
            }

            PlayerTournamentParticipation(
                tournamentId = tournament.id,
                tournamentName = tournament.name,
                tournamentType = tournament.type,
                tournamentDate = tournament.createdAt,
                isArchived = tournament.isArchived,
                clubName = clubName,
                stageReachedTitle = stageTitle,
                stageReachedType = stageType,
                matchesPlayed = tMatches.size,
                matchesWon = matchesWon,
                matchesDrawn = matchesDrawn,
                matchesLost = matchesLost,
                goalsFor = goalsFor,
                goalsAgainst = goalsAgainst
            )
        }.sortedByDescending { it.tournamentDate }
    }

    private fun calculateMatchHistory(
        playerName: String,
        matches: List<MatchEntity>,
        tournaments: List<TournamentEntity>
    ): List<PlayerMatchRecord> {
        val tournamentMap = tournaments.associateBy { it.id }
        val playerMatches = matches.filter {
            (it.playerOneName == playerName || it.playerTwoName == playerName) && it.status == MatchStatus.FINISHED
        }

        return playerMatches.map { match ->
            val isP1 = match.playerOneName == playerName
            val oppName = if (isP1) match.playerTwoName ?: "BYE" else match.playerOneName
            val myClub = if (isP1) match.playerOneClub else match.playerTwoClub
            val oppClub = if (isP1) match.playerTwoClub else match.playerOneClub

            val myScore = (if (isP1) match.scoreOne else match.scoreTwo) ?: 0
            val oppScore = (if (isP1) match.scoreTwo else match.scoreOne) ?: 0

            val myPen = if (isP1) match.penaltyScoreOne else match.penaltyScoreTwo
            val oppPen = if (isP1) match.penaltyScoreTwo else match.penaltyScoreOne

            val isWinner = match.winnerName == playerName || (myScore > oppScore && match.winnerName.isNullOrBlank())
            val isDraw = myScore == oppScore && match.winnerName.isNullOrBlank()

            val outcome = when {
                isWinner -> MatchOutcome.WIN
                isDraw -> MatchOutcome.DRAW
                else -> MatchOutcome.LOSS
            }

            val t = tournamentMap[match.tournamentId]

            PlayerMatchRecord(
                matchId = match.id,
                tournamentId = match.tournamentId,
                tournamentName = t?.name ?: "بطولة",
                stage = match.stage,
                date = match.scheduledTimestamp ?: t?.createdAt ?: System.currentTimeMillis(),
                playerClub = myClub,
                opponentName = oppName,
                opponentClub = oppClub,
                playerScore = myScore,
                opponentScore = oppScore,
                playerPenalty = myPen,
                opponentPenalty = oppPen,
                outcome = outcome
            )
        }.sortedByDescending { it.matchId }
    }

    private fun calculateHeadToHead(
        playerName: String,
        matches: List<MatchEntity>,
        customProfiles: List<PlayerProfileEntity>
    ): List<PlayerHeadToHead> {
        val profileMap = customProfiles.associateBy { it.name }
        val playerMatches = matches.filter {
            (it.playerOneName == playerName || it.playerTwoName == playerName) && it.status == MatchStatus.FINISHED
        }

        val opponentGroups = playerMatches.groupBy { match ->
            if (match.playerOneName == playerName) match.playerTwoName ?: "BYE" else match.playerOneName
        }.filterKeys { it.isRealPlayerName() }

        return opponentGroups.map { (oppName, oppMatches) ->
            var wins = 0
            var draws = 0
            var losses = 0
            var goalsFor = 0
            var goalsAgainst = 0

            oppMatches.forEach { match ->
                val isP1 = match.playerOneName == playerName
                val myScore = (if (isP1) match.scoreOne else match.scoreTwo) ?: 0
                val oppScore = (if (isP1) match.scoreTwo else match.scoreOne) ?: 0

                goalsFor += myScore
                goalsAgainst += oppScore

                val isWinner = match.winnerName == playerName || (myScore > oppScore && match.winnerName.isNullOrBlank())
                val isDraw = myScore == oppScore && match.winnerName.isNullOrBlank()

                if (isWinner) wins++
                else if (isDraw) draws++
                else losses++
            }

            val total = oppMatches.size
            val winRate = if (total > 0) (wins.toFloat() / total.toFloat()) * 100f else 0f
            val custom = profileMap[oppName]

            PlayerHeadToHead(
                opponentName = oppName,
                opponentNickname = custom?.nickname,
                opponentAvatar = custom?.avatarIcon,
                matchesPlayed = total,
                wins = wins,
                draws = draws,
                losses = losses,
                goalsScored = goalsFor,
                goalsConceded = goalsAgainst,
                winRate = (winRate * 10).toInt() / 10f
            )
        }.sortedWith(
            compareByDescending<PlayerHeadToHead> { it.matchesPlayed }
                .thenByDescending { it.wins }
                .thenByDescending { it.winRate }
        )
    }

    private fun calculateLeaderboard(
        matches: List<MatchEntity>,
        tournaments: List<TournamentEntity>,
        participants: List<TournamentParticipantEntity>,
        customProfiles: List<PlayerProfileEntity>,
        allProfilesWithItems: List<com.rndm.app.data.local.dao.ProfileWithItems>
    ): List<PlayerLeaderboardItem> {
        val playerProfilesOnly = allProfilesWithItems.filter { it.profile.type == ProfileType.PLAYERS.name }
        val playerNamesFromProfiles = playerProfilesOnly.flatMap { it.items.map { item -> item.label } }

        val allNames = (
                playerNamesFromProfiles +
                        participants.map { it.playerName } +
                        matches.flatMap { listOf(it.playerOneName, it.playerTwoName ?: "") } +
                        customProfiles.map { it.name }
                ).filter { it.isRealPlayerName() }.distinct()

        val profileMap = customProfiles.associateBy { it.name }
        val podiumMap = computeTournamentPodiums(tournaments, matches, participants)

        val items = allNames.map { name ->
            val pMatches = matches.filter {
                (it.playerOneName == name || it.playerTwoName == name) && it.status == MatchStatus.FINISHED
            }

            var wins = 0
            var draws = 0
            var losses = 0
            var goalsScored = 0
            var goalsConceded = 0
            var cleanSheets = 0

            pMatches.forEach { match ->
                val isP1 = match.playerOneName == name
                val s1 = match.scoreOne ?: 0
                val s2 = match.scoreTwo ?: 0
                val scored = if (isP1) s1 else s2
                val conceded = if (isP1) s2 else s1

                goalsScored += scored
                goalsConceded += conceded

                if (conceded == 0 && scored > 0) cleanSheets++

                val isWinner = match.winnerName == name || (scored > conceded && match.winnerName.isNullOrBlank())
                val isDraw = scored == conceded && match.winnerName.isNullOrBlank()

                if (isWinner) wins++
                else if (isDraw) draws++
                else losses++
            }

            val titles = podiumMap.values.count { it.champion == name }
            val runnerUps = podiumMap.values.count { it.runnerUp == name }

            val tIds = (participants.filter { it.playerName == name }.map { it.tournamentId } +
                    pMatches.map { it.tournamentId }).distinct().size

            val total = pMatches.size
            val winRate = if (total > 0) (wins.toFloat() / total.toFloat()) * 100f else 0f
            val custom = profileMap[name]
            val points = (wins * 3) + (draws * 1)

            PlayerLeaderboardItem(
                playerName = name,
                nickname = custom?.nickname,
                avatarIcon = custom?.avatarIcon,
                titlesCount = titles,
                runnerUpCount = runnerUps,
                totalTournaments = tIds,
                totalMatches = total,
                totalWins = wins,
                totalDraws = draws,
                totalLosses = losses,
                winRate = (winRate * 10).toInt() / 10f,
                goalsScored = goalsScored,
                goalsConceded = goalsConceded,
                goalDifference = goalsScored - goalsConceded,
                cleanSheets = cleanSheets,
                points = points
            )
        }

        return items
            .sortedWith(
                compareByDescending<PlayerLeaderboardItem> { it.titlesCount }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.winRate }
                    .thenByDescending { it.totalMatches }
                    .thenByDescending { it.goalsScored }
                    .thenByDescending { it.points }
            )
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    private data class TournamentPodium(
        val champion: String? = null,
        val runnerUp: String? = null,
        val thirdPlace: String? = null
    )

    private fun computeTournamentPodiums(
        tournaments: List<TournamentEntity>,
        matches: List<MatchEntity>,
        participants: List<TournamentParticipantEntity>
    ): Map<Long, TournamentPodium> {
        val result = mutableMapOf<Long, TournamentPodium>()
        val participantsByTournament = participants.groupBy { it.tournamentId }
        val matchesByTournament = matches.groupBy { it.tournamentId }

        tournaments.forEach { tournament ->
            val tMatches = matchesByTournament[tournament.id] ?: emptyList()
            val tParticipants = participantsByTournament[tournament.id] ?: emptyList()
            val finishedMatches = tMatches.filter { it.status == MatchStatus.FINISHED }

            val finalMatch = finishedMatches.firstOrNull { it.stage == MatchStage.FINAL }
            val thirdMatch = finishedMatches.firstOrNull { it.stage == MatchStage.THIRD_PLACE }

            if (finalMatch != null) {
                val champ = finalMatch.winnerName?.takeIf { it.isRealPlayerName() }
                val runner = if (champ != null) {
                    if (finalMatch.playerOneName == champ) finalMatch.playerTwoName else finalMatch.playerOneName
                } else null
                val third = thirdMatch?.winnerName?.takeIf { it.isRealPlayerName() }
                result[tournament.id] = TournamentPodium(
                    champion = champ,
                    runnerUp = runner?.takeIf { it.isRealPlayerName() },
                    thirdPlace = third
                )
            } else if (tournament.type == TournamentType.LEAGUE || tournament.type == TournamentType.TRIANGLE_SOLO || (tMatches.isNotEmpty() && tMatches.all { it.stage == MatchStage.GROUP_STAGE })) {
                if (finishedMatches.isNotEmpty()) {
                    val playerNames = (tParticipants.map { it.playerName } + finishedMatches.flatMap { listOf(it.playerOneName, it.playerTwoName ?: "") })
                        .filter { it.isRealPlayerName() }.distinct()

                    val standings = playerNames.map { name ->
                        var pts = 0
                        var gf = 0
                        var ga = 0
                        var wins = 0
                        finishedMatches.filter { it.playerOneName == name || it.playerTwoName == name }.forEach { m ->
                            val isP1 = m.playerOneName == name
                            val s1 = m.scoreOne ?: 0
                            val s2 = m.scoreTwo ?: 0
                            val scored = if (isP1) s1 else s2
                            val conceded = if (isP1) s2 else s1
                            gf += scored
                            ga += conceded
                            val isWin = m.winnerName == name || (scored > conceded && m.winnerName.isNullOrBlank())
                            val isDraw = scored == conceded && m.winnerName.isNullOrBlank()
                            if (isWin) { pts += 3; wins++ }
                            else if (isDraw) { pts += 1 }
                        }
                        StandingData(name, pts, gf - ga, gf, wins)
                    }.sortedWith(
                        compareByDescending<StandingData> { it.points }
                            .thenByDescending { it.diff }
                            .thenByDescending { it.gf }
                            .thenByDescending { it.wins }
                    )

                    val allMatchesFinished = tMatches.isNotEmpty() && tMatches.all { it.status == MatchStatus.FINISHED }
                    if (standings.isNotEmpty() && (allMatchesFinished || tournament.stage.name == "COMPLETED" || tournament.isArchived)) {
                        result[tournament.id] = TournamentPodium(
                            champion = standings.getOrNull(0)?.name,
                            runnerUp = standings.getOrNull(1)?.name,
                            thirdPlace = standings.getOrNull(2)?.name
                        )
                    }
                }
            }
        }
        return result
    }

    private data class StandingData(
        val name: String,
        val points: Int,
        val diff: Int,
        val gf: Int,
        val wins: Int
    )
}
