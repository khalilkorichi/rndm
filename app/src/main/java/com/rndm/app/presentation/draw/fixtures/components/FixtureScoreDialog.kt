package com.rndm.app.presentation.draw.fixtures.components

import androidx.compose.runtime.Composable
import com.rndm.app.core.ui.components.MatchScoreDialog
import com.rndm.app.domain.model.DrawFixture

@Composable
fun FixtureScoreDialog(
    fixture: DrawFixture,
    scoreOne: String,
    scoreTwo: String,
    onScoreOneChange: (String) -> Unit,
    onScoreTwoChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    MatchScoreDialog(
        playerOneName = fixture.playerOneName,
        playerOneClub = fixture.playerOneTeam,
        playerTwoName = fixture.playerTwoName,
        playerTwoClub = fixture.playerTwoTeam,
        initialScoreOne = scoreOne.toIntOrNull(),
        initialScoreTwo = scoreTwo.toIntOrNull(),
        title = "تسجيل النتيجة",
        subtitle = "المباراة ${fixture.matchNumber}",
        onDismiss = onDismiss,
        onConfirm = { s1, s2 ->
            onScoreOneChange(s1.toString())
            onScoreTwoChange(s2.toString())
            onSave()
        }
    )
}
