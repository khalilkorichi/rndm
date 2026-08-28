package com.rndm.app.domain.usecase.tournament

import com.rndm.app.core.util.RandomProvider
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.Profile
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentParticipant
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentType
import com.rndm.app.domain.repository.TournamentRepository
import javax.inject.Inject

class CreateTournamentUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val randomProvider: RandomProvider
) {

    suspend operator fun invoke(
        name: String,
        type: TournamentType,
        playersProfile: Profile,
        clubsProfile: Profile? = null,
        groupsCount: Int = 2,
        qualifiersPerGroup: Int = 2
    ): Long {
        val activePlayers = playersProfile.activeItems.ifEmpty { playersProfile.items }
        val activeClubs = clubsProfile?.activeItems?.ifEmpty { clubsProfile.items } ?: emptyList()
        val shuffledPlayers = randomProvider.shuffle(activePlayers)
        val shuffledClubs = if (activeClubs.isNotEmpty()) randomProvider.shuffle(activeClubs) else emptyList()

        val participants = shuffledPlayers.mapIndexed { index, playerItem ->
            val groupIndex = if (type == TournamentType.GROUPS_KNOCKOUT) index % groupsCount else 0
            val clubName = if (shuffledClubs.isNotEmpty() && index < shuffledClubs.size) {
                shuffledClubs[index].label
            } else null

            TournamentParticipant(
                playerItemId = playerItem.id,
                playerName = playerItem.label,
                clubName = clubName,
                groupIndex = groupIndex
            )
        }

        val matches = mutableListOf<Match>()
        val initialStage: TournamentStage

        if (type == TournamentType.GROUPS_KNOCKOUT) {
            initialStage = TournamentStage.GROUPS
            val participantsByGroup = participants.groupBy { it.groupIndex }
            participantsByGroup.forEach { (groupIndex, groupMembers) ->
                var roundNumber = 1
                for (i in groupMembers.indices) {
                    for (j in (i + 1) until groupMembers.size) {
                        val p1 = groupMembers[i]
                        val p2 = groupMembers[j]
                        matches.add(
                            Match(
                                stage = MatchStage.GROUP_STAGE,
                                groupIndex = groupIndex,
                                roundIndex = roundNumber++,
                                playerOneName = p1.playerName,
                                playerOneClub = p1.clubName,
                                playerTwoName = p2.playerName,
                                playerTwoClub = p2.clubName,
                                status = MatchStatus.PENDING
                            )
                        )
                    }
                }
            }
        } else {
            // Draw Knockout or Knockout only - generate bracket directly
            initialStage = TournamentStage.KNOCKOUT_ROUNDS
            matches.addAll(GenerateKnockoutBracketUseCase.generateBracketMatches(0L, participants))
        }

        val tournament = Tournament(
            name = name,
            type = type,
            stage = initialStage,
            playersProfileId = playersProfile.id,
            clubsProfileId = clubsProfile?.id,
            groupsCount = groupsCount,
            qualifiersPerGroup = qualifiersPerGroup,
            isHost = true
        )

        return tournamentRepository.saveTournament(tournament, participants, matches)
    }
}
