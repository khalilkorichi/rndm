package com.rndm.app.presentation.draw.wheel.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.theme.RndmThemeTokens
import com.rndm.app.presentation.draw.wheel.DrawCategory

@Composable
fun DrawCategorySelector(
    selectedCategory: DrawCategory,
    onCategorySelected: (DrawCategory) -> Unit,
    playersCount: Int,
    clubsCount: Int,
    teamsCount: Int,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        CategoryCard(
            title = "الأشخاص",
            subtitle = "$playersCount متاح",
            iconRes = R.drawable.ic_person,
            isSelected = selectedCategory == DrawCategory.PLAYERS,
            onClick = { onCategorySelected(DrawCategory.PLAYERS) },
            modifier = Modifier.weight(1f)
        )

        CategoryCard(
            title = "الأندية",
            subtitle = "$clubsCount متاح",
            iconRes = R.drawable.ic_shield,
            isSelected = selectedCategory == DrawCategory.CLUBS,
            onClick = { onCategorySelected(DrawCategory.CLUBS) },
            modifier = Modifier.weight(1f)
        )

        CategoryCard(
            title = "المنتخبات",
            subtitle = "$teamsCount متاح",
            iconRes = R.drawable.ic_globe,
            isSelected = selectedCategory == DrawCategory.NATIONAL_TEAMS,
            onClick = { onCategorySelected(DrawCategory.NATIONAL_TEAMS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = RndmThemeTokens.spacing
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.sm, horizontal = spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
