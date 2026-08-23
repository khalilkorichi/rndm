package com.rndm.app.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.rndm.app.R

sealed class BottomNavItem(
    val destination: Destination,
    val title: String,
    @DrawableRes val filledIcon: Int,
    @DrawableRes val outlinedIcon: Int
) {
    data object Home : BottomNavItem(
        destination = Destination.Home,
        title = "الرئيسية",
        filledIcon = R.drawable.ic_home_filled,
        outlinedIcon = R.drawable.ic_home_outlined
    )

    data object Profiles : BottomNavItem(
        destination = Destination.ProfileList,
        title = "البروفايلات",
        filledIcon = R.drawable.ic_profile_filled,
        outlinedIcon = R.drawable.ic_profile_outlined
    )

    data object Tournaments : BottomNavItem(
        destination = Destination.TournamentList,
        title = "البطولات",
        filledIcon = R.drawable.ic_tournament_filled,
        outlinedIcon = R.drawable.ic_tournament_outlined
    )

    data object Settings : BottomNavItem(
        destination = Destination.Settings,
        title = "الإعدادات",
        filledIcon = R.drawable.ic_settings_filled,
        outlinedIcon = R.drawable.ic_settings_outlined
    )

    companion object {
        val items = listOf(Home, Profiles, Tournaments, Settings)
    }
}
