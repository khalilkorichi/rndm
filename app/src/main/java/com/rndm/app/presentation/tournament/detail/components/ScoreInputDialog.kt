package com.rndm.app.presentation.tournament.detail.components

import androidx.compose.runtime.Composable
import com.rndm.app.core.ui.components.MatchScoreDialog
import com.rndm.app.domain.model.Match
import com.rndm.app.domain.model.MatchStage

@Composable
fun ScoreInputDialog(
    match: Match,
    onDismiss: () -> Unit,
    onConfirm: (scoreOne: Int, scoreTwo: Int, penaltyOne: Int?, penaltyTwo: Int?) -> Unit
) {
    val subtitleText = when {
        match.groupIndex != null -> "المجموعة ${('أ'.code + match.groupIndex).toChar()}"
        else -> match.stage.displayName
    }

    MatchScoreDialog(
        playerOneName = match.playerOneName,
        playerOneClub = match.playerOneClub,
        playerTwoName = match.playerTwoName,
        playerTwoClub = match.playerTwoClub,
        initialScoreOne = match.scoreOne,
        initialScoreTwo = match.scoreTwo,
        initialPenaltyScoreOne = match.penaltyScoreOne,
        initialPenaltyScoreTwo = match.penaltyScoreTwo,
        isKnockout = match.stage != MatchStage.GROUP_STAGE,
        title = "تسجيل النتيجة",
        subtitle = subtitleText,
        onDismiss = onDismiss,
        onConfirmWithPenalties = onConfirm
    )
}
