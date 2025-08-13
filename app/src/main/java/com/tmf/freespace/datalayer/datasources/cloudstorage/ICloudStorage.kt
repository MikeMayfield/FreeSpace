package com.tmf.freespace.datalayer.datasources.cloudstorage

import com.tmf.freespace.models.MediaFile

interface ICloudStorage {
    /**
     * Save file to the cloud
     *
     * @param mediaFile MediaFile to save
     * @param encoded Flag: The file was encoded when saved
     * @return Path to temporary copy of source file if saved successfully, else null
     */
    suspend fun uploadMediaFile(mediaFile: MediaFile, encoded: Boolean = false) : String?  //TODO  Before uploading video, use ffmpeg -movflags +faststart option with FFmpeg can relocate the necessary metadata to the beginning, allowing file to be streamed, if sufficient bandwidth

    /**
     * Restore (i.e. download) original file previously saved in the cloud
     *
     * @param mediaFile MediaFile to restore
     * @param outputFilePath Path to local file to create from cloud file
     * @param encoded Flag: The file was encoded when saved
     * @return Flag: The file was restored successfully
     */
    suspend fun downloadMediaFile(mediaFile: MediaFile, outputFilePath: String, encoded: Boolean = false) : Boolean

    suspend fun close()  //Close file manager, if open
}