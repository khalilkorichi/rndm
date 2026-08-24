package com.rndm.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens

@Composable
fun MatchScoreDialog(
    playerOneName: String,
    playerOneClub: String? = null,
    playerTwoName: String?,
    playerTwoClub: String? = null,
    initialScoreOne: Int? = null,
    initialScoreTwo: Int? = null,
    initialPenaltyScoreOne: Int? = null,
    initialPenaltyScoreTwo: Int? = null,
    isKnockout: Boolean = false,
    isRequestMode: Boolean = false,
    title: String = if (isRequestMode) "طلب تعديل النتيجة (إرسال للأدمن)" else "تسجيل النتيجة",
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (scoreOne: Int, scoreTwo: Int) -> Unit = { _, _ -> },
    onConfirmWithPenalties: ((scoreOne: Int, scoreTwo: Int, penaltyOne: Int?, penaltyTwo: Int?) -> Unit)? = null,
    onConfirmRequest: ((scoreOne: Int, scoreTwo: Int, penaltyOne: Int?, penaltyTwo: Int?, note: String) -> Unit)? = null
) {
    var scoreOneText by remember(initialScoreOne) {
        mutableStateOf((initialScoreOne ?: 0).toString())
    }
    var scoreTwoText by remember(initialScoreTwo) {
        mutableStateOf((initialScoreTwo ?: 0).toString())
    }
    var penaltyOneText by remember(initialPenaltyScoreOne) {
        mutableStateOf((initialPenaltyScoreOne ?: 0).toString())
    }
    var penaltyTwoText by remember(initialPenaltyScoreTwo) {
        mutableStateOf((initialPenaltyScoreTwo ?: 0).toString())
    }
    var requestNoteText by remember {
        mutableStateOf("")
    }
    var enablePenalties by remember(initialPenaltyScoreOne, initialPenaltyScoreTwo) {
        mutableStateOf(initialPenaltyScoreOne != null || initialPenaltyScoreTwo != null)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    if (subtitle != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_fixtures),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Competitors & Scores Container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player 1 Team Box
                    CompetitorScoreBox(
                        modifier = Modifier.weight(1f),
                        playerName = playerOneName,
                        clubName = playerOneClub,
                        scoreText = scoreOneText,
                        onScoreChange = { newScore ->
                            if (newScore.isEmpty() || (newScore.all { it.isDigit() } && newScore.length <= 2)) {
                                scoreOneText = newScore
                            }
                        },
                        onIncrement = {
                            val current = scoreOneText.toIntOrNull() ?: 0
                            if (current < 99) scoreOneText = (current + 1).toString()
                        },
                        onDecrement = {
                            val current = scoreOneText.toIntOrNull() ?: 0
                            if (current > 0) scoreOneText = (current - 1).toString()
                        }
                    )

                    // VS Divider
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Player 2 Team Box
                    CompetitorScoreBox(
                        modifier = Modifier.weight(1f),
                        playerName = playerTwoName ?: "BYE",
                        clubName = playerTwoClub,
                        scoreText = scoreTwoText,
                        onScoreChange = { newScore ->
                            if (newScore.isEmpty() || (newScore.all { it.isDigit() } && newScore.length <= 2)) {
                                scoreTwoText = newScore
                            }
                        },
                        onIncrement = {
                            val current = scoreTwoText.toIntOrNull() ?: 0
                            if (current < 99) scoreTwoText = (current + 1).toString()
                        },
                        onDecrement = {
                            val current = scoreTwoText.toIntOrNull() ?: 0
                            if (current > 0) scoreTwoText = (current - 1).toString()
                        }
                    )
                }

                val s1Val = scoreOneText.toIntOrNull() ?: 0
                val s2Val = scoreTwoText.toIntOrNull() ?: 0
                val isTied = s1Val == s2Val

                if (isKnockout && isTied) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ركلات الترجيح (ض.ج)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // P1 Penalty Box
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    StepperButton(
                                        symbol = "−",
                                        onClick = {
                                            val current = penaltyOneText.toIntOrNull() ?: 0
                                            if (current > 0) penaltyOneText = (current - 1).toString()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = penaltyOneText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StepperButton(
                                        symbol = "+",
                                        onClick = {
                                            val current = penaltyOneText.toIntOrNull() ?: 0
                                            if (current < 99) penaltyOneText = (current + 1).toString()
                                        }
                                    )
                                }

                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                // P2 Penalty Box
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    StepperButton(
                                        symbol = "−",
                                        onClick = {
                                            val current = penaltyTwoText.toIntOrNull() ?: 0
                                            if (current > 0) penaltyTwoText = (current - 1).toString()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = penaltyTwoText,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    StepperButton(
                                        symbol = "+",
                                        onClick = {
                                            val current = penaltyTwoText.toIntOrNull() ?: 0
                                            if (current < 99) penaltyTwoText = (current + 1).toString()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (isRequestMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = requestNoteText,
                        onValueChange = { requestNoteText = it },
                        label = { Text("ملاحظة أو سبب التعديل (اختياري)", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("مثال: تم احتساب ركلات الترجيح باتفاق الطرفين", style = MaterialTheme.typography.bodySmall) },
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "إلغاء",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val s1 = scoreOneText.toIntOrNull() ?: 0
                            val s2 = scoreTwoText.toIntOrNull() ?: 0
                            val p1 = if (isKnockout && s1 == s2) penaltyOneText.toIntOrNull() ?: 0 else null
                            val p2 = if (isKnockout && s1 == s2) penaltyTwoText.toIntOrNull() ?: 0 else null

                            if (isRequestMode && onConfirmRequest != null) {
                                onConfirmRequest(s1, s2, p1, p2, requestNoteText)
                            } else if (onConfirmWithPenalties != null) {
                                onConfirmWithPenalties(s1, s2, p1, p2)
                            } else {
                                onConfirm(s1, s2)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRequestMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            contentColor = if (isRequestMode) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1.6f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isRequestMode) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isRequestMode) "إرسال الطلب للأدمن" else "حفظ النتيجة",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitorScoreBox(
    modifier: Modifier = Modifier,
    playerName: String,
    clubName: String?,
    scoreText: String,
    onScoreChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Player Name
            Text(
                text = playerName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Club Name or Badge
            if (clubName != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = clubName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "فردي",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Stepper Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Decrement Button (-)
                StepperButton(
                    symbol = "−",
                    onClick = onDecrement
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Score Number Pill
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = scoreText,
                        onValueChange = onScoreChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Increment Button (+)
                StepperButton(
                    symbol = "+",
                    onClick = onIncrement
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
