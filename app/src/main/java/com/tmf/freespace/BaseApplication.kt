package com.tmf.freespace

import android.annotation.SuppressLint
import android.app.Application
import android.os.Debug
import android.os.StrictMode
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.tmf.freespace.domainlayer.general.BillingClientWrapper
import com.tmf.freespace.domainlayer.general.DLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BaseApplication: Application() {
    companion object {
        private const val TAG = "BaseApplication"
        lateinit var instance: BaseApplication
            private set
        @SuppressLint("StaticFieldLeak")
        lateinit var billingClient: BillingClientWrapper
        var firebaseAnalytics: FirebaseAnalytics? = null
        lateinit var firebaseAuth: FirebaseAuth
        var firebaseUserID: String = "N/A"
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        enableStrictModeForDebugging()

        //Init Firebase in background
        CoroutineScope(Dispatchers.Default).launch {
            DLog.d(TAG, "Starting Firebase authentication")

            //Initialize Firebase
            FirebaseApp.initializeApp(this@BaseApplication.applicationContext)
            firebaseAnalytics = FirebaseAnalytics.getInstance(this@BaseApplication.applicationContext)
            firebaseAuth = Firebase.auth
            signInAnonymously()
        }

        //Start billing client in background
        CoroutineScope(Dispatchers.IO).launch {
            DLog.d(TAG, "Starting billing client")
            billingClient = BillingClientWrapper(instance, CoroutineScope(Dispatchers.Default))
            billingClient.startConnection()
        }
    }

    override fun onTerminate() {
        billingClient.endConnection()

        super.onTerminate()
    }

    private fun enableStrictModeForDebugging() {
        if (Debug.isDebuggerConnected()) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
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
                        firebaseAnalytics?.setUserId(userId)
                    }
                } else {
                    //If sign in fails
                    DLog.e(TAG, "signInAnonymously:failure", task.exception)
                }
            }
    }
}