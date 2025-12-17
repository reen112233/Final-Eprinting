package com.example.eprinting

import android.app.Application
import com.google.firebase.FirebaseApp

class EPrintingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
