package com.example.sidequest

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class SideQuestDebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isNotEmpty()) {
            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        }
    }
}
