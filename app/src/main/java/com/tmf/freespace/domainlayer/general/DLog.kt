package com.tmf.freespace.domainlayer.general

import android.os.Bundle
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tmf.freespace.BaseApplication
import java.io.File
import java.time.format.DateTimeFormatter


object DLog {
    private const val TAG = "DLog"
    private const val MAX_LOG_LENGTH = 4000  //Max size of LogCat entry (break into multiple segments if longer)

    private var supportLogFile: File? = null
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }


    fun d(tag: String, msg: String) {
        log(Log.DEBUG, tag, msg)
    }

    fun v(tag: String, msg: String) {
        log(Log.VERBOSE, tag, msg)
    }

    fun i(tag: String, msg: String) {
        log(Log.INFO, tag, msg)
    }

    fun w(tag: String, msg: String) {
        log(Log.WARN, tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        log(Log.ERROR, tag, msg, throwable)
    }


    private fun log(level: Int, tag: String, msg: String, throwable: Throwable? = null) {
        val fullTag = "DL_$tag"

        // 1. Log to Android Logcat
        logToLogcat(level, fullTag, msg)
        throwable?.let {
            logToLogcat(level, fullTag, getStackTraceString(it))
        }

        // 2. Log to Firebase
        when (level) {
            Log.ERROR -> {
                // Send errors to Crashlytics
                crashlytics.setCustomKey("log_tag", fullTag)
                crashlytics.log(msg)
                if (throwable != null) {
                    crashlytics.recordException(throwable)
                } else {
                    // Log a non-fatal exception to make it more visible in the console
                    crashlytics.recordException(Exception(msg))
                }
            }
            else -> {
                // Send other logs as custom Analytics events
                val eventName = "DL_${getEventNameForLevel(level)}"
                val bundle = Bundle().apply {
                    putString("log_tag", tag) // Use original tag for cleaner analytics
                    // Truncate message to adhere to Analytics parameter value limits (100 chars)
                    putString("log_message", msg.take(100))
                }
                BaseApplication.firebaseAnalytics?.logEvent(eventName, bundle)
            }
        }
    }

    private fun getEventNameForLevel(level: Int): String {
        return when (level) {
            Log.DEBUG -> "debug"
            Log.VERBOSE -> "verbose"
            Log.INFO -> "info"
            Log.WARN -> "warning"
            Log.ERROR -> "error"
            else -> "unknown"
        }
    }

    private fun logToLogcat(level: Int, tag: String, message: String) {
        if (message.length > MAX_LOG_LENGTH) {
            // Split the long message into chunks
            var i = 0
            while (i < message.length) {
                val end = (i + MAX_LOG_LENGTH).coerceAtMost(message.length)
                val chunk = message.substring(i, end)
                Log.println(level, tag, chunk)
                i += MAX_LOG_LENGTH
            }
        } else {
            Log.println(level, tag, message)
        }
    }

    private fun getStackTraceString(t: Throwable?): String {
        return Log.getStackTraceString(t)
    }
}
