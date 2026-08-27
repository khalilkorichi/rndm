package com.rndm.app.domain.model

data class LiveTournamentPreview(
    val tournament: Tournament,
    val participants: List<TournamentParticipant>,
    val matches: List<Match>
)
