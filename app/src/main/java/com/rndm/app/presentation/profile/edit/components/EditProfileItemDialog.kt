package com.rndm.app.presentation.profile.edit.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R
import com.rndm.app.core.util.Constants
import com.rndm.app.domain.model.ProfileType

@Composable
fun EditProfileItemDialog(
    initialLabel: String,
    profileType: ProfileType,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(initialLabel) {
        mutableStateOf(
            TextFieldValue(
                text = initialLabel,
                selection = TextRange(0, initialLabel.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val typeColor = when (profileType) {
        ProfileType.PLAYERS -> com.rndm.app.core.theme.ProfilePlayersColor
        ProfileType.CLUBS -> com.rndm.app.core.theme.ProfileClubsColor
        ProfileType.NATIONAL_TEAMS -> com.rndm.app.core.theme.ProfileNationalTeamsColor
    }

    val typeIcon = when (profileType) {
        ProfileType.PLAYERS -> R.drawable.ic_person
        ProfileType.CLUBS -> R.drawable.ic_shield
        ProfileType.NATIONAL_TEAMS -> R.drawable.ic_globe
    }

    val titleText = when (profileType) {
        ProfileType.PLAYERS -> "تعديل اسم اللاعب / الشخص"
        ProfileType.CLUBS -> "تعديل اسم النادي"
        ProfileType.NATIONAL_TEAMS -> "تعديل اسم المنتخب"
    }

    val inputPlaceholder = when (profileType) {
        ProfileType.PLAYERS -> "اسم اللاعب الجديد"
        ProfileType.CLUBS -> "اسم النادي الجديد"
        ProfileType.NATIONAL_TEAMS -> "اسم المنتخب الجديد"
    }

    val isInputValid = textFieldValue.text.trim().isNotBlank() &&
            textFieldValue.text.length <= Constants.MAX_ITEM_LABEL_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = typeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = typeIcon),
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "قم بتعديل الاسم أدناه، وسيتم تحديثه في القائمة مباشرة:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        if (it.text.length <= Constants.MAX_ITEM_LABEL_LENGTH) {
                            textFieldValue = it
                        }
                    },
                    placeholder = { Text(inputPlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = textFieldValue.text.trim().isBlank(),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (textFieldValue.text.trim().isBlank()) {
                                Text(
                                    text = "لا يمكن ترك الاسم فارغاً",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text(
                                text = "${textFieldValue.text.length}/${Constants.MAX_ITEM_LABEL_LENGTH}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isInputValid) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onConfirm(textFieldValue.text.trim())
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = typeColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isInputValid) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(textFieldValue.text.trim())
                    }
                },
                enabled = isInputValid,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = typeColor,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "حفظ التعديل",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "إلغاء",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
