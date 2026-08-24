package com.rndm.app.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Scheme Colors
val PrimaryLight = Color(0xFF5B4FE8)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFE4E0FF)
val OnPrimaryContainerLight = Color(0xFF170065)

val SecondaryLight = Color(0xFF00C896)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFB9FBE5)
val OnSecondaryContainerLight = Color(0xFF002116)

val TertiaryLight = Color(0xFFFF8A5B)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDBCF)
val OnTertiaryContainerLight = Color(0xFF380D00)

val ErrorLight = Color(0xFFE5484D)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFFAFAFC)
val OnBackgroundLight = Color(0xFF1A1A22)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1A1A22)
val SurfaceVariantLight = Color(0xFFEDEDF4)
val OnSurfaceVariantLight = Color(0xFF44444E)
val OutlineLight = Color(0xFFC7C7D4)

// Dark Scheme Colors
val PrimaryDark = Color(0xFFB8B0FF)
val OnPrimaryDark = Color(0xFF28157B)
val PrimaryContainerDark = Color(0xFF4334AF)
val OnPrimaryContainerDark = Color(0xFFE4E0FF)

val SecondaryDark = Color(0xFF5CE0BE)
val OnSecondaryDark = Color(0xFF003828)
val SecondaryContainerDark = Color(0xFF00513B)
val OnSecondaryContainerDark = Color(0xFFB9FBE5)

val TertiaryDark = Color(0xFFFFB088)
val OnTertiaryDark = Color(0xFF5B1B00)
val TertiaryContainerDark = Color(0xFF7E2A00)
val OnTertiaryContainerDark = Color(0xFFFFDBCF)

val ErrorDark = Color(0xFFFF8A8A)
val OnErrorDark = Color(0xFF68000A)
val ErrorContainerDark = Color(0xFF930012)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF121218)
val OnBackgroundDark = Color(0xFFEDEDF4)
val SurfaceDark = Color(0xFF1C1C24)
val OnSurfaceDark = Color(0xFFEDEDF4)
val SurfaceVariantDark = Color(0xFF2A2A34)
val OnSurfaceVariantDark = Color(0xFFC7C7D4)
val OutlineDark = Color(0xFF44444E)

// Navigation & Bottom Bar Tokens
val BottomBarDarkBgTop = Color(0xFF14141A)
val BottomBarDarkBgBottom = Color(0xFF0B0B10)
val BottomBarLightBgTop = Color(0xFFFFFFFF)
val BottomBarLightBgBottom = Color(0xFFF3F3F8)
val BottomBarDarkBorderTop = Color(0xFF2A2A38)
val BottomBarDarkBorderBottom = Color(0xFF14141E)
val BottomBarLightBorderBottom = Color(0xFFD8D8E5)
val BottomBarShadowDark = Color(0xFF000000)
val BottomBarShadowLight = Color(0xFF1A1A28)

val BottomBarSelectedDarkBgTop = Color(0xFF262634)
val BottomBarSelectedDarkBgBottom = Color(0xFF1B1B26)
val BottomBarSelectedLightBgTop = Color(0xFF282836)
val BottomBarSelectedLightBgBottom = Color(0xFF1B1B26)
val BottomBarSelectedBorderDark = Color(0xFF3E3E50)
val BottomBarSelectedBorderLight = Color(0xFF424254)
val BottomBarUnselectedIconDark = Color(0xFF888898)
val BottomBarUnselectedIconLight = Color(0xFF6E6E7E)

// Profile Categories Tokens
val ProfilePlayersColor = Color(0xFF2563EB)
val ProfilePlayersColorDark = Color(0xFF1D4ED8)
val ProfilePlayersColorLight = Color(0xFF3B82F6)

val ProfileClubsColor = Color(0xFF10B981)
val ProfileClubsColorDark = Color(0xFF059669)

val ProfileNationalTeamsColor = Color(0xFFF59E0B)
val ProfileNationalTeamsColorDark = Color(0xFFD97706)

// Achievement & Medal Tokens
val GoldMedalColor = Color(0xFFFFD700)
val SilverMedalColor = Color(0xFFC0C0C0)
val BronzeMedalColor = Color(0xFFCD7F32)
val OrangeGoldColor = Color(0xFFFF8C00)

// Status & Career Stats Tokens
val StatsSuccessGreen = Color(0xFF4CAF50)
val StatsWarningAmber = Color(0xFFFFB300)
val StatsErrorRed = Color(0xFFF44336)
val StatsOrangeFlame = Color(0xFFFF5722)

// In-App Update & Download Tokens
val UpdateSuccessGreen = Color(0xFF10B981)
val UpdateSuccessGreenDark = Color(0xFF047857)
val UpdateSuccessGreenLight = Color(0xFF34D399)
val UpdateBluePrimary = Color(0xFF3B82F6)
val UpdateBlueDark = Color(0xFF1D4ED8)
val UpdateBlueNavy = Color(0xFF1E3A8A)
val UpdateWarningAmber = Color(0xFFF59E0B)
val UpdateErrorRed = Color(0xFFEF4444)
val UpdateErrorRedDark = Color(0xFFDC2626)

// Extended Colors for Wheel & Draw segments
@Immutable
data class ExtendedColors(
    val wheelSegments: List<Color> = listOf(
        Color(0xFF5B4FE8),
        Color(0xFF00C896),
        Color(0xFFFF8A5B),
        Color(0xFFE5484D),
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6)
    ),
    val shimmerHighlight: Color = Color(0xFFFFFFFF).copy(alpha = 0.5f),
    val cardGlow: Color = Color(0xFF5B4FE8).copy(alpha = 0.15f)
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)
