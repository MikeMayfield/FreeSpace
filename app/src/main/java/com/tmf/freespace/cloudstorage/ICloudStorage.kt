package com.tmf.freespace.cloudstorage

import com.tmf.freespace.models.MediaFile

interface ICloudStorage {
    suspend fun sendMediaFile(mediaFile: MediaFile, encoded: Boolean = false) : Boolean  //TODO  Before uploading video, use ffmpeg -movflags +faststart option with FFmpeg can relocate the necessary metadata to the beginning, allowing file to be streamed, if sufficient bandwidth

    /**
     * Restore original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputFilePath Path to local file to create from cloud file
     * @param encoded Flag: The file was encoded when saved
     * @return Flag: The file was restored successfully
     */
    suspend fun restoreFileFromCloud(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean = false) : Boolean

    suspend fun close()  //Close file manager, if open
}