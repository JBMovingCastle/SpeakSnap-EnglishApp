package com.speaksnap.english

import android.app.Application

class SpeakSnapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SpeakSnapApp
            private set
    }
}
