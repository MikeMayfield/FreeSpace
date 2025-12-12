package com.tmf.freespace

import android.app.Application
import com.google.firebase.FirebaseApp

class BaseApplication: Application() {
    companion object {
        lateinit var instance: BaseApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this);
    }
}