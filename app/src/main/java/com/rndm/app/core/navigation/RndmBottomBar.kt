package com.rndm.app.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.rndm.app.core.theme.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun RndmBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Outer Pill Container (Glassmorphic in Light & Dark Mode)
        Row(
            modifier = Modifier
                .shadow(
                    elevation = if (isDark) 20.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = if (isDark) BottomBarShadowDark.copy(alpha = 0.55f) else BottomBarShadowLight.copy(alpha = 0.14f),
                    ambientColor = if (isDark) BottomBarShadowDark.copy(alpha = 0.35f) else BottomBarShadowLight.copy(alpha = 0.08f)
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                BottomBarDarkBgTop.copy(alpha = 0.96f),
                                BottomBarDarkBgBottom.copy(alpha = 0.94f)
                            )
                        } else {
                            listOf(
                                BottomBarLightBgTop.copy(alpha = 0.92f),
                                BottomBarLightBgBottom.copy(alpha = 0.85f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                BottomBarDarkBorderTop.copy(alpha = 0.8f),
                                BottomBarDarkBorderBottom.copy(alpha = 0.5f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.95f),
                                BottomBarLightBorderBottom.copy(alpha = 0.65f)
                            )
                        }
                    ),
                    shape = CircleShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem.items.forEach { item ->
                val isSelected = currentDestination?.hierarchy?.any {
                    it.hasRoute(item.destination::class)
                } == true

                RndmBottomNavItem(
                    item = item,
                    isSelected = isSelected,
                    isDark = isDark,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            navController.navigate(item.destination) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RndmBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav_icon_scale"
    )

    val itemShape = CircleShape

    Box(
        modifier = Modifier
            .clip(itemShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isDark) {
                                    listOf(
                                        BottomBarSelectedDarkBgTop,
                                        BottomBarSelectedDarkBgBottom
                                    )
                                } else {
                                    listOf(
                                        BottomBarSelectedLightBgTop,
                                        BottomBarSelectedLightBgBottom
                                    )
                                }
                            ),
                            shape = itemShape
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (isDark) BottomBarSelectedBorderDark else BottomBarSelectedBorderLight,
                            shape = itemShape
                        )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .padding(
                horizontal = if (isSelected) 16.dp else 12.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isSelected) item.filledIcon else item.outlinedIcon
                ),
                contentDescription = item.title,
                tint = if (isSelected) Color.White else (if (isDark) BottomBarUnselectedIconDark else BottomBarUnselectedIconLight),
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(220, delayMillis = 50)) +
                        expandHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            expandFrom = Alignment.Start
                        ),
                exit = fadeOut(animationSpec = tween(140)) +
                        shrinkHorizontally(
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            shrinkTowards = Alignment.Start
                        )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
