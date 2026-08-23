package com.rndm.app.presentation.tournament.list

import androidx.compose.runtime.Immutable
import com.rndm.app.domain.model.Tournament
import com.rndm.app.domain.model.TournamentStage
import com.rndm.app.domain.model.TournamentType

@Immutable
data class TournamentListUiState(
    val tournaments: List<Tournament> = emptyList(),
    val selectedFilter: TournamentFilter = TournamentFilter.ALL,
    val selectedSort: TournamentSort = TournamentSort.DATE_DESC,
    val isLoading: Boolean = true,
    val pendingDeleteId: Long? = null,
    val pendingArchiveId: Long? = null
) {
    val filteredTournaments: List<Tournament>
        get() {
            val filtered = when (selectedFilter) {
                TournamentFilter.ALL -> tournaments
                TournamentFilter.DRAW_TOURNAMENTS -> tournaments.filter { it.type == TournamentType.DRAW_KNOCKOUT }
                TournamentFilter.GROUPS_TOURNAMENTS -> tournaments.filter { it.type == TournamentType.GROUPS_KNOCKOUT }
                TournamentFilter.COMPLETED -> tournaments.filter { it.stage == TournamentStage.COMPLETED }
            }

            return when (selectedSort) {
                TournamentSort.DATE_DESC -> filtered.sortedByDescending { it.updatedAt }
                TournamentSort.TYPE -> filtered.sortedBy { it.type.name }
                TournamentSort.NAME -> filtered.sortedBy { it.name }
            }
        }

    val winnerStats: List<WinnerStat>
        get() {
            val counts = mutableMapOf<String, Int>()
            tournaments.filter { it.stage == TournamentStage.COMPLETED }.forEach { t ->
                val finalMatch = t.knockoutMatches.lastOrNull()
                val winner = finalMatch?.winnerName ?: t.participants.firstOrNull()?.playerName
                if (!winner.isNullOrBlank()) {
                    counts[winner] = (counts[winner] ?: 0) + 1
                }
            }
            return counts.entries
                .map { WinnerStat(winnerName = it.key, winsCount = it.value) }
                .sortedByDescending { it.winsCount }
        }
}

data class WinnerStat(
    val winnerName: String,
    val winsCount: Int
)

enum class TournamentFilter {
    ALL,
    DRAW_TOURNAMENTS,
    GROUPS_TOURNAMENTS,
    COMPLETED
}

enum class TournamentSort {
    DATE_DESC,
    TYPE,
    NAME
}
