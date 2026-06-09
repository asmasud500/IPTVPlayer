package com.iptvplayer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IPTVApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
