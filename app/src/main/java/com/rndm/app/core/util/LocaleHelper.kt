package com.rndm.app.core.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.View
import android.view.Window
import java.util.Locale

object LocaleHelper {
    val ARABIC_LOCALE: Locale = Locale("ar")

    fun applyArabicLocale(context: Context): Context {
        Locale.setDefault(ARABIC_LOCALE)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(ARABIC_LOCALE)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = ARABIC_LOCALE
        }

        config.setLayoutDirection(ARABIC_LOCALE)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun updateConfiguration(context: Context) {
        Locale.setDefault(ARABIC_LOCALE)
        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(ARABIC_LOCALE)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = ARABIC_LOCALE
        }

        config.setLayoutDirection(ARABIC_LOCALE)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun enforceRtl(window: Window?) {
        window?.decorView?.layoutDirection = View.LAYOUT_DIRECTION_RTL
    }
}
