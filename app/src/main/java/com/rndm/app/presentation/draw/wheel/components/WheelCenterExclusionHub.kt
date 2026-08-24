package com.rndm.app.presentation.draw.wheel.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rndm.app.R

@Composable
fun WheelCenterExclusionHub(
    isSpinning: Boolean,
    excludedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim by animateFloatAsState(
        targetValue = if (isSpinning) 0.5f else 1f,
        label = "hub_alpha"
    )

    Surface(
        onClick = onClick,
        enabled = !isSpinning,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        modifier = modifier
            .size(46.dp)
            .scale(alphaAnim)
            .border(
                width = 2.dp,
                color = if (excludedCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = "استبعاد من القرعة الحالية",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )

            if (excludedCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$excludedCount",
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
