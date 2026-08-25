package com.rndm.app.presentation.profile.player.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rndm.app.R
import com.rndm.app.core.ui.components.BentoCard
import com.rndm.app.domain.model.MatchOutcome
import com.rndm.app.domain.model.PlayerCareerStats

@Composable
fun PlayerOverviewTab(
    stats: PlayerCareerStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Trophies & Honors Showcase
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trophy),
                        contentDescription = null,
                        tint = com.rndm.app.core.theme.GoldMedalColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "خزانة البطولات والجوائز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrophyPodiumItem(
                        modifier = Modifier.weight(1f),
                        icon = R.drawable.ic_trophy,
                        label = "البطل",
                        count = stats.titlesCount,
                        color = com.rndm.app.core.theme.GoldMedalColor
                    )
                    TrophyPodiumItem(
                        modifier = Modifier.weight(1f),
                        icon = R.drawable.ic_medal,
                        label = "الوصيف",
                        count = stats.runnerUpCount,
                        color = com.rndm.app.core.theme.SilverMedalColor
                    )
                    TrophyPodiumItem(
                        modifier = Modifier.weight(1f),
                        icon = R.drawable.ic_medal,
                        label = "المركز الثالث",
                        count = stats.thirdPlaceCount,
                        color = com.rndm.app.core.theme.BronzeMedalColor
                    )
                }
            }
        }

        // 2. Match Performance & Win Rate
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chart),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "سجل المباريات والأداء",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${stats.totalMatches} مباراة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Win/Draw/Loss Counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatSummaryText(label = "فوز", count = stats.totalWins, color = com.rndm.app.core.theme.StatsSuccessGreen)
                    StatSummaryText(label = "تعادل", count = stats.totalDraws, color = com.rndm.app.core.theme.StatsWarningAmber)
                    StatSummaryText(label = "خسارة", count = stats.totalLosses, color = com.rndm.app.core.theme.StatsErrorRed)
                    StatSummaryText(label = "شباك نظيفة", count = stats.cleanSheets, color = MaterialTheme.colorScheme.secondary)
                }

                // Segmented Ratio Bar
                if (stats.totalMatches > 0) {
                    val winWeight = (stats.totalWins.toFloat() / stats.totalMatches.toFloat()).coerceAtLeast(0.01f)
                    val drawWeight = (stats.totalDraws.toFloat() / stats.totalMatches.toFloat()).coerceAtLeast(0.01f)
                    val lossWeight = (stats.totalLosses.toFloat() / stats.totalMatches.toFloat()).coerceAtLeast(0.01f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(modifier = Modifier.weight(winWeight).height(10.dp).background(com.rndm.app.core.theme.StatsSuccessGreen))
                        if (stats.totalDraws > 0) {
                            Box(modifier = Modifier.weight(drawWeight).height(10.dp).background(com.rndm.app.core.theme.StatsWarningAmber))
                        }
                        if (stats.totalLosses > 0) {
                            Box(modifier = Modifier.weight(lossWeight).height(10.dp).background(com.rndm.app.core.theme.StatsErrorRed))
                        }
                    }
                }
            }
        }

        // 3. Attacking & Defending Matrix (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatMetricBox(
                modifier = Modifier.weight(1f),
                title = "أهداف له",
                iconRes = R.drawable.ic_football,
                value = "${stats.goalsScored}",
                subtitle = "معدل ${stats.averageGoalsPerMatch} / مباراة",
                color = MaterialTheme.colorScheme.primary
            )
            StatMetricBox(
                modifier = Modifier.weight(1f),
                title = "أهداف عليه",
                iconRes = R.drawable.ic_shield,
                value = "${stats.goalsConceded}",
                subtitle = if (stats.goalDifference >= 0) "فارق +${stats.goalDifference}" else "فارق ${stats.goalDifference}",
                color = if (stats.goalDifference >= 0) com.rndm.app.core.theme.StatsSuccessGreen else com.rndm.app.core.theme.StatsErrorRed
            )
        }

        // 4. Records & Highlights
        BentoCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fire),
                        contentDescription = null,
                        tint = com.rndm.app.core.theme.StatsOrangeFlame,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "أبرز الإنجازات والأرقام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Recent Form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحالة الأخيرة (آخر 5 مباريات)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (stats.recentForm.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            stats.recentForm.forEach { outcome ->
                                val (bg, text) = when (outcome) {
                                    MatchOutcome.WIN -> Pair(com.rndm.app.core.theme.StatsSuccessGreen, "ف")
                                    MatchOutcome.DRAW -> Pair(com.rndm.app.core.theme.StatsWarningAmber, "ت")
                                    MatchOutcome.LOSS -> Pair(com.rndm.app.core.theme.StatsErrorRed, "خ")
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "لا توجد مباريات بعد",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Biggest Win
                stats.biggestWin?.let { win ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أكبر فوز مسجل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = win,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = com.rndm.app.core.theme.StatsSuccessGreen
                        )
                    }
                }

                // Most played club
                stats.mostPlayedClub?.let { club ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النادي الأكثر استخداماً",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = club,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TrophyPodiumItem(
    icon: Int,
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatSummaryText(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatMetricBox(
    title: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    BentoCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
