package com.tmf.freespace.domainlayer.general

import android.content.Context
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
        supportLog("E", tag, msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable) {
        Log.e("D-$tag", msg, throwable)
        supportLog("E", tag, msg, throwable)
    }

    fun w(tag: String, msg: String) {
        Log.w("D-$tag", msg)
        supportLog("W", tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i("D-$tag", msg)
        supportLog("I", tag, msg)
    }

    fun d(tag: String, msg: String) {
        Log.d("D-$tag", msg)
        supportLog("D", tag, msg)
    }

    fun v(tag: String, msg: String) {
        Log.v("D-$tag", msg)
        supportLog("V", tag, msg)
    }


    private fun supportLog(priority: String, tag: String, msg: String, throwable: Throwable? = null) {
        if (supportLogUri != null && "EWD".contains(priority)) {
            CoroutineScope(Dispatchers.IO).launch {  //Log to disk in background
                try {
                    val context = BaseApplication.instance.applicationContext

                    //If we haven't checked for the support log file, do so now. If found, clone it to copy for this execution
                    if (supportLogUri == Uri.EMPTY) {
                        supportLogUri = cloneSupportLog(context)
                        if (supportLogUri == null) {
                            return@launch
                        }
                    }

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
                    Log.e("DLog", "Error writing to support log", e)
                }
            }
        }
    }

    private fun cloneSupportLog(context: Context): Uri? {
        val baseSupportLogUri: Uri? = MediaReader(context).supportLogUri()
        if (baseSupportLogUri != null) {
            supportLogUri = MediaReader(context).cloneSupportLogFile(baseSupportLogUri)
        }
        return supportLogUri
    }
}
