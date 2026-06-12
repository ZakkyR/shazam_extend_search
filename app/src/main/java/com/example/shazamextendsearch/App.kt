package com.example.shazamextendsearch

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
