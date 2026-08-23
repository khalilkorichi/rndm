package com.rndm.app.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class DrawFixture(
    val id: String = java.util.UUID.randomUUID().toString(),
    val matchNumber: Int,
    val playerOneName: String,
    val playerOneTeam: String? = null,
    val scoreOne: Int? = null,
    val playerTwoName: String? = null,
    val playerTwoTeam: String? = null,
    val scoreTwo: Int? = null,
    val isFinished: Boolean = false
) {
    val isReady: Boolean
        get() = playerTwoName != null

    val isTeamsAssigned: Boolean
        get() = playerOneTeam != null && (playerTwoName == null || playerTwoTeam != null)

    val winnerName: String?
        get() {
            if (!isFinished || scoreOne == null || scoreTwo == null) return null
            return when {
                scoreOne > scoreTwo -> playerOneName
                scoreTwo > scoreOne -> playerTwoName
                else -> null
            }
        }
}
