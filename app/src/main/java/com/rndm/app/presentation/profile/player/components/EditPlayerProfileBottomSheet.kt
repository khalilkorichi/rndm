package com.rndm.app.presentation.profile.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.core.ui.components.RndmButton
import com.rndm.app.core.ui.components.RndmButtonType
import com.rndm.app.domain.model.PlayerCareerStats

private val AVATAR_PRESETS = listOf(
    "👑", "⚽", "⚡", "🦁", "🎯", "🚀", "💎", "🔥", "🏆", "🌟", "🎩", "🛡️", "🦅", "🎮"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlayerProfileBottomSheet(
    sheetState: SheetState,
    stats: PlayerCareerStats,
    onDismiss: () -> Unit,
    onSave: (nickname: String?, avatarIcon: String?, favoriteClub: String?, notes: String?) -> Unit
) {
    var nickname by remember { mutableStateOf(stats.nickname ?: "") }
    var selectedAvatar by remember { mutableStateOf(stats.avatarIcon ?: "⚽") }
    var favoriteClub by remember { mutableStateOf(stats.favoriteClub ?: "") }
    var notes by remember { mutableStateOf(stats.notes ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "تخصيص بروفايل اللاعب (${stats.playerName})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Choose Avatar Emoji
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "اختر رمز أو أيقونة اللاعب",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = AVATAR_PRESETS,
                        key = { it }
                    ) { emoji ->
                        val isSelected = selectedAvatar == emoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedAvatar = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            }

            // 2. Nickname Field
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("لقب اللاعب (مثال: القناص، المايسترو، الملك)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // 3. Favorite Club
            OutlinedTextField(
                value = favoriteClub,
                onValueChange = { favoriteClub = it },
                label = { Text("النادي أو المنتخب المفضل") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // 4. Notes / Bio
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات / وصف إضافي") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RndmButton(
                    onClick = onDismiss,
                    type = RndmButtonType.OUTLINED,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إلغاء")
                }

                RndmButton(
                    onClick = {
                        onSave(nickname, selectedAvatar, favoriteClub, notes)
                    },
                    type = RndmButtonType.PRIMARY,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("حفظ التغييرات")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
