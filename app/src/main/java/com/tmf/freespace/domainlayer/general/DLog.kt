package com.tmf.freespace.domainlayer.general

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DLog {
    val tag = "DLog"
    var supportLogFile: File? = null
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")


    fun e(tag: String, msg: String) {
        Log.e("D-$tag", msg)
        writeToSupportLog("E", tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable) {
        Log.e("D-$tag", msg, throwable)
        writeToSupportLog("E", tag, msg, throwable)
    }

    fun w(tag: String, msg: String) {
        Log.w("D-$tag", msg)
        writeToSupportLog("W", tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i("D-$tag", msg)
        writeToSupportLog("I", tag, msg)
    }

    fun d(tag: String, msg: String) {
        Log.d("D-$tag", msg)
        writeToSupportLog("D", tag, msg)
    }

    fun v(tag: String, msg: String) {
        Log.v("D-$tag", msg)
        writeToSupportLog("V", tag, msg)
    }


    private fun writeToSupportLog(priority: String, tag: String, msg: String, throwable: Throwable? = null) {
        if ("EWD".contains(priority)) {  //Only log E, W, and D to support log
            if (createSupportLogIfNeeded()) {  //Must be completed before using coroutine for remaining code
                return
            }

            CoroutineScope(Dispatchers.IO).launch {  //Log to disk in background fire-and-forget
                try {
                    //Write to the support log file
                    supportLogFile?.appendText("${LocalDateTime.now().format(dateFormatter)} $priority:  $tag: $msg\n${if (throwable == null) "" else "${throwable.stackTraceToString()}\n"}" )
                }
                catch (e: Exception) {
                    // Handle exceptions related to file operations or permissions
                    Log.e("DLog", "Error writing to support log", e)  //NOTE: Use Log not DLog to avoid infinite loop
                }
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
    fun createSupportLog(): File? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "FreeSpaceLog.txt")

        try {
            if (file.exists()) {
                file.delete()
            }
            file.createNewFile()
            return file
        } catch (e: Exception) {
            Log.e(tag, "Error creating support log", e)
            return null
        }
    }
}
