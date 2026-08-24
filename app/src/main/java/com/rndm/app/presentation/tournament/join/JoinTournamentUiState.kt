package com.rndm.app.presentation.tournament.join

import androidx.compose.runtime.Immutable

@Immutable
data class JoinTournamentUiState(
    val codeInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val joinedTournamentId: Long? = null
)
