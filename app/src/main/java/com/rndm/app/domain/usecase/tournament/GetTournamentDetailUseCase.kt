package com.rndm.app.domain.usecase.tournament

import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage
import com.rndm.app.domain.model.MatchStatus
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentGroup
import com.rndm.app.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetTournamentDetailUseCase @Inject constructor(
    private val tournamentRepository: TournamentRepository,
    private val calculateGroupStandingsUseCase: CalculateGroupStandingsUseCase
) {

    operator fun invoke(tournamentId: Long): Flow<Tournament?> {
        return combine(
            tournamentRepository.getTournamentById(tournamentId),
            tournamentRepository.getParticipants(tournamentId),
            tournamentRepository.getMatches(tournamentId)
        ) { tournament, participants, matches ->
            tournament?.let { t ->
                val groupIndices = participants.map { it.groupIndex }.distinct().sorted()
                val knockoutMatches = matches
                    .filter { it.stage != MatchStage.GROUP_STAGE }
                    .sortedWith(
                        compareByDescending<Match> { it.status == com.rndm.app.domain.model.MatchStatus.FINISHED }
                            .thenByDescending { it.id }
                    )
                    .distinctBy { Pair(it.stage, it.bracketMatchIndex ?: 1) }
                    .sortedWith(
                        compareBy<Match> { it.stage.ordinal }
                            .thenBy { it.roundIndex }
                            .thenBy { it.bracketMatchIndex ?: 1 }
                    )

                val distinctParticipants = participants.distinctBy { it.playerName }
                val groupMatches = matches.filter { it.stage == MatchStage.GROUP_STAGE }
                val groups = if ((t.type == com.rndm.app.domain.model.TournamentType.GROUPS_KNOCKOUT || 
                        t.type == com.rndm.app.domain.model.TournamentType.LEAGUE) && groupMatches.isNotEmpty()) {
                    groupIndices.map { gIdx ->
                        val groupParticipants = distinctParticipants.filter { it.groupIndex == gIdx }
                        val gMatches = groupMatches.filter { it.groupIndex == gIdx }
                        val standings = calculateGroupStandingsUseCase(
                            participants = groupParticipants,
                            matches = gMatches,
                            qualifiersCount = t.qualifiersPerGroup
                        )
                        TournamentGroup(
                            groupIndex = gIdx,
                            groupName = "المجموعة ${('أ'.code + gIdx).toChar()}",
                            standings = standings,
                            matches = gMatches
                        )
                    }
                } else {
                    emptyList()
                }

                t.copy(
                    participants = distinctParticipants,
                    groups = groups,
                    knockoutMatches = knockoutMatches
                )
            }
        }
    }
}
