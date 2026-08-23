package com.rndm.app.presentation.tournament.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rndm.app.domain.model.Match
import com.rndm.app.presentation.tournament.bracket.components.GoogleKnockoutBracketView

@Composable
fun TournamentKnockoutTab(
    knockoutMatches: List<Match>,
    onMatchClick: (Match) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GoogleKnockoutBracketView(
            matches = knockoutMatches,
            onMatchClick = onMatchClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
