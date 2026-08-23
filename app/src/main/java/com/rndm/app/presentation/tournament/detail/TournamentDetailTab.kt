package com.rndm.app.presentation.tournament.detail

import androidx.annotation.DrawableRes
import com.rndm.app.R

enum class TournamentDetailTab(
    val title: String,
    @DrawableRes val iconRes: Int
) {
    OVERVIEW("نظرة عامة", R.drawable.ic_home_outlined),
    MATCHES("المباريات", R.drawable.ic_fixtures),
    STANDINGS("الترتيب", R.drawable.ic_medal),
    KNOCKOUT("خروج المغلوب", R.drawable.ic_tournament_outlined)
}
