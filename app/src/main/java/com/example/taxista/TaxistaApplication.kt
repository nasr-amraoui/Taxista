package com.example.taxista

import android.app.Application
import com.google.firebase.FirebaseApp

class TaxistaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
