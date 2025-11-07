package com.tmf.freespace.domainlayer.general

import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tmf.freespace.BaseApplication
import com.tmf.freespace.MediaReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DLog {
    var supportLogUri: Uri? = Uri.EMPTY
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")


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
        if (supportLogUri != null && "EWD".contains(priority)) {
            if (createSupportLogIfNeeded()) {  //Must be completed before using coroutine for remaining code
                return
            }

            CoroutineScope(Dispatchers.IO).launch {  //Log to disk in background
                try {
                    val context = BaseApplication.instance.applicationContext

                    //Write to the support log file
                    val parcelFileDescriptor: ParcelFileDescriptor? =
                        context.contentResolver.openFileDescriptor(supportLogUri!!, "wa")  //Use "wa" mode for write and append
                    parcelFileDescriptor?.use {
                        val fileOutputStream = FileOutputStream(it.fileDescriptor)
                        // Use FileOutputStream to write binary data
                        fileOutputStream.use { stream ->
                            stream.write("${LocalDateTime.now().format(formatter)} $priority:  $tag: $msg\n".toByteArray())
                            if (throwable != null) {
                                stream.write("${throwable.message}\n$throwable.stackTraceToString()\n".toByteArray())
                            }
                            stream.flush()
                        }
                    }
                }
                catch (e: Exception) {
                    // Handle exceptions related to file operations or permissions
                    Log.e("DLog", "Error writing to support log", e)  //NOTE: Use Log not DLog to avoid infinite loop
                }
            }
        }
    }

    private fun createSupportLogIfNeeded(): Boolean {

        //If we haven't checked for the support log file, do so now. If found, clone it to copy for this execution
        if (supportLogUri == Uri.EMPTY) {
            cloneSupportLog()
        }
        return (supportLogUri == null)
    }

    private fun cloneSupportLog() {
        val context = BaseApplication.instance.applicationContext
        val baseSupportLogUri: Uri? = MediaReader(context).supportLogUri()
        supportLogUri = if (baseSupportLogUri != null) {
            MediaReader(context).cloneSupportLogFile(baseSupportLogUri)
        } else {
            null
        }
    }
}
