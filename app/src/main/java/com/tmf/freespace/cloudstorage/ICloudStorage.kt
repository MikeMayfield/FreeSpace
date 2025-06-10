package com.tmf.freespace.cloudstorage

import android.content.Context
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.User

interface ICloudStorage {
    suspend fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean = false)  //TODO  Before uploading video, use ffmpeg -movflags +faststart option with FFmpeg can relocate the necessary metadata to the beginning, allowing file to be streamed, if sufficient bandwidth

    /**
     * Restore original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputPath Path to local file to create from cloud file
     * @param encoded Flag: The file was encoded when saved
     * @return Flag: The file was restored successfully
     */
    suspend fun restoreMediaFile(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean = false) : Boolean

    suspend fun close()  //Close file manager, if open
}