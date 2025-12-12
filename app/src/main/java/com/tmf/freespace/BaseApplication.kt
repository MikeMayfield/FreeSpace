package com.tmf.freespace

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.tmf.freespace.domainlayer.general.DLog

class BaseApplication: Application() {
    companion object {
        private const val TAG = "BaseApplication"
        lateinit var instance: BaseApplication
            private set
        lateinit var firebaseAnalytics: FirebaseAnalytics
        lateinit var firebaseAuth: FirebaseAuth
        var firebaseUserID: String = "N/A"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        firebaseAuth = Firebase.auth
        signInAnonymously()
    }

    private fun signInAnonymously() {
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    //Sign in success, get the user ID
                    val user = firebaseAuth.currentUser
                    val userId = user?.uid
                    if (userId != null) {  //Set the custom user ID for Firebase Analytics
                        firebaseUserID = userId
                        firebaseAnalytics.setUserId(userId)
                    }
                } else {
                    //If sign in fails
                    DLog.e(TAG, "signInAnonymously:failure", task.exception)
                }
            }
    }
}