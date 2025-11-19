package com.tmf.freespace.domainlayer.general

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import com.tmf.freespace.BaseApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object DLog {
    val tag = "DLog"
    var supportLogUri: Uri? = Uri.EMPTY
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
        if (supportLogUri != null && "EWD".contains(priority)) {  //Only log E, W, and D to support log
            if (createSupportLogIfNeeded()) {  //Must be completed before using coroutine for remaining code
                return
            }

            CoroutineScope(Dispatchers.IO).launch {  //Log to disk in background fire-and-forget
                try {
                    val context = BaseApplication.instance.applicationContext

                    //Write to the support log file
                    val parcelFileDescriptor: ParcelFileDescriptor? =
                        context.contentResolver.openFileDescriptor(supportLogUri!!, "wa")  //Use "wa" mode for write and append
                    parcelFileDescriptor?.use {
                        val fileOutputStream = FileOutputStream(it.fileDescriptor)
                        // Use FileOutputStream to write binary data
                        fileOutputStream.use { stream ->
                            stream.write("${LocalDateTime.now().format(dateFormatter)} $priority:  $tag: $msg\n".toByteArray())
                            if (throwable != null) {
                                stream.write("${throwable.message}\n${throwable.stackTraceToString()}\n".toByteArray())
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

    /**
     * Create new empty support log if first time writing to log
     *
     * @return TRUE if error creating support log, FALSE otherwise
     */
    private fun createSupportLogIfNeeded(): Boolean {

        //If we haven't checked for the support log file, do so now. If found, clone it to copy for this execution
        if (supportLogUri == Uri.EMPTY) {
            supportLogUri = createSupportLog()
        }
        return (supportLogUri == null)
    }

    /**
     * Creates a new, empty file named "log.txt" in the public Downloads directory.
     *
     * If the file already exists, it will be overwritten with an empty file.
     * This method uses MediaStore and is compatible with Scoped Storage.
     *
     * @return The content Uri of the created file, which can be used for writing or sharing.
     *         Returns null if the file creation fails.
     */
    fun createSupportLog(): Uri? {
        val context = BaseApplication.instance.applicationContext
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "FreeSpaceLog.txt")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            // This places the file in the Downloads directory.
            // It's the recommended approach for Android 10+
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        // Use the MediaStore to create the file entry.
        // On Android 10+, this creates the file in Downloads. On older versions,
        // it creates it at the root of the external storage.
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        try {
            val newFileUri = contentResolver.insert(collection, contentValues)
            if (newFileUri == null) {
                Log.e(tag, "Failed to create new MediaStore entry for log file.")
                return null
            }

            // The insert call creates the file entry. To ensure it's an empty file,
            // we open an output stream and immediately close it. This overwrites any
            // existing file with the same name in that directory.
            contentResolver.openOutputStream(newFileUri)?.use {
                // The 'use' block ensures the stream is closed, effectively creating an empty file.
            }

             return newFileUri
        } catch (e: Exception) {
            Log.e(tag, "Error creating log file in Downloads", e)
            e.printStackTrace()
            return null
        }
    }
}
