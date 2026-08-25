package com.rndm.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.theme.GoldMedalColor

enum class AvatarPreset(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int
) {
    FOOTBALL("football", "كرة قدم", R.drawable.ic_football),
    CROWN("crown", "تاج", R.drawable.ic_crown),
    TROPHY("trophy", "كأس", R.drawable.ic_trophy),
    MEDAL("medal", "ميدالية", R.drawable.ic_medal),
    STAR("star", "نجمة", R.drawable.ic_star),
    FIRE("fire", "شعلة", R.drawable.ic_fire),
    LIGHTNING("lightning", "برق", R.drawable.ic_lightning),
    TARGET("target", "هدف", R.drawable.ic_target),
    SHIELD("shield", "درع", R.drawable.ic_shield),
    SWORDS("swords", "سيوف", R.drawable.ic_swords),
    ROCKET("rocket", "صاروخ", R.drawable.ic_rocket),
    DIAMOND("diamond", "جوهرة", R.drawable.ic_diamond),
    GAMEPAD("gamepad", "يد تحكم", R.drawable.ic_gamepad),
    LION("lion", "أسد", R.drawable.ic_lion),
    EAGLE("eagle", "صقر", R.drawable.ic_eagle),
    HAT("hat", "قبعة", R.drawable.ic_hat);

    companion object {
        fun resolveIconRes(avatarKey: String?): Int {
            if (avatarKey.isNullOrBlank()) return R.drawable.ic_person

            // Match by preset id
            entries.firstOrNull { it.id.equals(avatarKey, ignoreCase = true) }?.let {
                return it.iconRes
            }

            // Match legacy emoji strings if stored in existing database records
            return when (avatarKey.trim()) {
                "⚽" -> R.drawable.ic_football
                "👑" -> R.drawable.ic_crown
                "🏆" -> R.drawable.ic_trophy
                "🥇", "🥈", "🥉", "🏅" -> R.drawable.ic_medal
                "🌟", "⭐" -> R.drawable.ic_star
                "🔥" -> R.drawable.ic_fire
                "⚡" -> R.drawable.ic_lightning
                "🎯" -> R.drawable.ic_target
                "🛡️", "🛡" -> R.drawable.ic_shield
                "⚔️", "⚔" -> R.drawable.ic_swords
                "🚀" -> R.drawable.ic_rocket
                "💎" -> R.drawable.ic_diamond
                "🎮" -> R.drawable.ic_gamepad
                "🦁" -> R.drawable.ic_lion
                "🦅" -> R.drawable.ic_eagle
                "🎩" -> R.drawable.ic_hat
                else -> R.drawable.ic_person
            }
        }
    }
}

@Composable
fun PlayerAvatar(
    avatarIcon: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = size * 0.55f,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    isChampion: Boolean = false,
    showCrownBadge: Boolean = false,
    borderGradient: List<Color>? = null
) {
    val resolvedIconRes = AvatarPreset.resolveIconRes(avatarIcon)

    Box(
        modifier = modifier.size(if (showCrownBadge && isChampion) size + 8.dp else size),
        contentAlignment = Alignment.Center
    ) {
        if (borderGradient != null) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(borderGradient))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = resolvedIconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = resolvedIconRes),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (showCrownBadge && isChampion) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size((size * 0.35f).coerceAtLeast(18.dp))
                    .clip(CircleShape)
                    .background(GoldMedalColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_crown),
                    contentDescription = "بطل",
                    tint = Color.Black,
                    modifier = Modifier.size((size * 0.2f).coerceAtLeast(10.dp))
                )
            }
        }
    }
}
