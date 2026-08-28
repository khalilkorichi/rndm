package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TournamentParticipant(
    val id: Long = 0L,
    val tournamentId: Long = 0L,
    val playerItemId: Long,
    val playerName: String,
    val clubName: String? = null,
    val groupIndex: Int = 0,
    val remoteId: String? = null
)

fun String?.isRealPlayerName(): Boolean {
    if (this.isNullOrBlank()) return false
    val trimmed = this.trim()
    if (trimmed.equals("BYE", ignoreCase = true)) return false
    if (trimmed.equals("TBD", ignoreCase = true)) return false
    if (trimmed == "أحسن خاسر") return false
    if (trimmed.startsWith("فائز ")) return false
    if (trimmed.startsWith("خاسر ")) return false
    if (trimmed.startsWith("المركز ")) return false
    return true
}

fun String?.isPlaceholderPlayerName(): Boolean = !this.isRealPlayerName()
