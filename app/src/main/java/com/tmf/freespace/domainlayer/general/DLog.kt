package com.tmf.freespace.domainlayer.general

import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tmf.freespace.BaseApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
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
        val fullTag = tag// "$APP_TAG.$tag"

        // 1. Log to Android Logcat
        logToLogcat(level, fullTag, msg)
        throwable?.let {
            logToLogcat(level, fullTag, getStackTraceString(it))
        }

        // 1a. Log to FreeSpaceLog.txt log file
        writeToSupportLog(level, fullTag, msg, throwable)
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
                val eventName = "DLog_${getEventNameForLevel(level)}"
                val bundle = Bundle().apply {
                    putString("log_tag", tag) // Use original tag for cleaner analytics
                    // Truncate message to adhere to Analytics parameter value limits (100 chars)
                    putString("log_message", msg.take(100))
                }
                BaseApplication.firebaseAnalytics.logEvent(eventName, bundle)
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

    private fun writeToSupportLog(level: Int, tag: String, msg: String, throwable: Throwable? = null) {
        if (createSupportLogIfNeeded()) {  //Must be completed before using coroutine for remaining code
            return
        }

        CoroutineScope(Dispatchers.IO).launch {  //Log to disk in background fire-and-forget
            try {
                //Write to the support log file
                supportLogFile?.appendText("${LocalDateTime.now().format(dateFormatter)} $level:  $tag: $msg\n${if (throwable == null) "" else "${throwable.stackTraceToString()}\n"}" )
            }
            catch (e: Exception) {
                // Handle exceptions related to file operations or permissions
                Log.e("DLog", "Error writing to support log", e)  //NOTE: Use Log not DLog to avoid infinite loop
                supportLogFile = null  //Create log file again next time
            }
        }
    }

    /**
     * Create new empty support log if first time writing to log
     *
     * @return TRUE if error creating support log, FALSE otherwise
     */
    private fun createSupportLogIfNeeded(): Boolean {

        //If we haven't checked for the support log file, do so now. If found, clone it to copy for this execution
        if (supportLogFile == null) {
            supportLogFile = createSupportLog()
        }
        return (supportLogFile == null)
    }

    /**
     * Creates a new, empty file named "log.txt" in the public Downloads directory.
     *
     * If the file already exists, it will be overwritten with an empty file.
     * This method uses MediaStore and is compatible with Scoped Storage.
     *
     * @return TRUE if log file created successfully
     */
    private fun createSupportLog(): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val bytesToRetainCnt = 2_000_000L

        try {
            val logFile = File(downloadsDir, "FreeSpaceLog.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            //Trim all but desired retained text from existing log file
            if (logFile.length() > bytesToRetainCnt) {
                val tempFile = File(downloadsDir, "FreeSpaceLog.txt.tmp")
                FileInputStream(logFile).use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.skip(logFile.length() - bytesToRetainCnt) // Skip all but the bytes to retain
                        // Copy the remaining content to the temporary file
                        inputStream.copyTo(outputStream)
                    }
                }
                logFile.delete()
                tempFile.renameTo(logFile)
            }

            return logFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating support log", e)
            return null
        }
    }
}
