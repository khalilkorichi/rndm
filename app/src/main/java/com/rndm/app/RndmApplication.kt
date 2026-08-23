package com.rndm.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.rndm.app.core.util.LocaleHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RndmApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyArabicLocale(base))
    }

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.updateConfiguration(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.updateConfiguration(this)
    }
}

