package com.tmf.freespace.domainlayer.compression

import android.content.Context
import com.tmf.freespace.datalayer.models.MediaFile

abstract class ICompressor(val context: Context) {
    abstract val ffmpegCompressionCommands : List<String>

    /**
     * Compress media file using FFmpeg. Returns the compressed file size
     *
     * @param mediaFile The MediaFile object representing the current media file
     * @param inputFilePath The full path of the source file to compress
     * @param outputFilePath The full path of the compressed file to create
     * @return Flag: File was compresses successfully
     */
    abstract fun compress(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String) : Boolean

//    /**
//     * Copies a file from the MediaStore (identified by its content URI) to a temporary file
//     * in the app's cache directory. The temporary file's name will be derived from the
//     * MediaStore ID.
//     *
//     * @param contentUri The content URI of the file in MediaStore.
//     * @return The absolute path to the created temporary file, or null if an error occurred.
//     */
//    private fun copyFileFromMediaStoreToCache(contentUri: Uri): String? {  //TODO Move to MediaStoreUtil
//        val contentResolver = context.contentResolver
//        var fileName: String? = null
//        var mediaStoreId: String? = null
//
//        // Try to get the display name and MediaStore ID
//        contentResolver.query(contentUri, null, null, null, null)?.use { cursor ->
//            if (cursor.moveToFirst()) {
//                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
//                if (displayNameIndex != -1) {
//                    fileName = cursor.getString(displayNameIndex)
//                }
//
//                // Extract MediaStore ID from the URI's last path segment
//                // This is a common pattern for MediaStore URIs (e.g., content://media/external/images/media/123)
//                // Adjust if your URI structure is different
//                mediaStoreId = contentUri.lastPathSegment?.substringAfterLast(':') // Handles cases like "image:123" or just "123"
//
//                if (mediaStoreId.isNullOrEmpty()) {
//                    // Fallback if ID extraction from URI fails, try from DISPLAY_NAME (less reliable for uniqueness)
//                    mediaStoreId = fileName?.substringBeforeLast('.') ?: "temp_media"
//                }
//            }
//        }
//
//        if (mediaStoreId.isNullOrEmpty()) {
//            // If we still don't have an ID, generate a generic temporary name
//            mediaStoreId = "temp_media_${System.currentTimeMillis()}"
//        }
//
//        // Get the file extension from the original file name, if available
//        val extension = fileName?.substringAfterLast('.', "")?.let { if (it.isNotEmpty()) ".$it" else "" } ?: ""
//        val tempFileName = "COMPRESSED_${mediaStoreId}$extension"
//        val tempFile = File(context.cacheDir, tempFileName)
//
//        try {
//            contentResolver.openInputStream(contentUri)?.use { inputStream ->
//                FileOutputStream(tempFile).use { outputStream ->
//                    inputStream.copyTo(outputStream)
//                }
//            } ?: return null // openInputStream returned null
//            return tempFile.absolutePath
//        } catch (e: IOException) {
//            e.printStackTrace() // Log the error
//            // Consider deleting the tempFile if it was partially created and an error occurred
//            if (tempFile.exists()) {
//                tempFile.delete()
//            }
//            return null
//        }
//    }

    fun ffmpegCommand(mediaFile: MediaFile, inputFilePath: String, outputFilePath: String) : String {
        return ffmpegCompressionCommands[if (ffmpegCompressionCommands.size > mediaFile.desiredCompressionLevel) mediaFile.desiredCompressionLevel else 0]
            .replace("{{INPUT_FILE_PATH}}", inputFilePath)
            .replace("{{OUTPUT_FILE_PATH}}", outputFilePath)
    }

    companion object {
        val ffmpeg = FFmpeg()  //Shared FFmpeg instance for all compressors
    }
}