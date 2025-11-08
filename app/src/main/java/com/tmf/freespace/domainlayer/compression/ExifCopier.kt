package com.tmf.freespace.domainlayer.compression.ExifCopier

import androidx.exifinterface.media.ExifInterface
import com.tmf.freespace.domainlayer.general.DLog
import java.io.File
import java.io.IOException

object ExifCopier {
    private const val TAG = "ExifCopier"

    /**
     * Copies EXIF metadata from a source image file to a destination image file.
     * It attempts to copy all known writable EXIF tags.
     *
     * @param sourcePath Path to the source image file from which to read EXIF data.
     * @param destinationPath Path to the destination image file to which EXIF data will be written.
     *                        This file should already exist and be a valid image file.
     * @return True if EXIF data was successfully copied (or if the source had no EXIF to copy),
     *         false if an error occurred.
     */
    fun copyExifData(sourcePath: String, destinationPath: String): Boolean {
        val sourceFile = File(sourcePath)
        val destinationFile = File(destinationPath)

        if (!sourceFile.exists()) {
            DLog.e(TAG, "Source file does not exist: $sourcePath")
            return false
        }
        if (!destinationFile.exists()) {
            DLog.e(TAG, "Destination file does not exist: $destinationPath")
            return false
        }

        try {
            val sourceExif = ExifInterface(sourceFile.absolutePath)
            val destinationExif = ExifInterface(destinationFile.absolutePath)

            var tagsCopied = 0

            // Iterate over a list of known EXIF tags.
            // ExifInterface doesn't provide a direct "getAllTags" that are all writable.
            // We list common ones. Some might not be present in the source.
            EXIF_TAGS_TO_COPY.forEach { tag ->
                val value = sourceExif.getAttribute(tag)
                if (value != null) {
                    destinationExif.setAttribute(tag, value)
                    tagsCopied++
//                    DLog.d(TAG, "Copied tag '$tag': $value")
                }
            }

            // Specific handling for GPS data as it often involves multiple related tags
            copyGpsTags(sourceExif, destinationExif)

            if (tagsCopied > 0) {
                destinationExif.saveAttributes() // This is crucial to write changes to the file
//                DLog.v(TAG, "Successfully copied $tagsCopied EXIF attributes from $sourcePath to $destinationPath")
            } else {
//                DLog.v(TAG, "No EXIF attributes to copy or no relevant tags found in $sourcePath.")
                // Return true even if no tags were copied, as the operation itself didn't fail due to an error.
                // If you want to return false if no tags were copied, change this.
            }
            return true

        } catch (e: IOException) {
            DLog.e(TAG, "IOException during EXIF copy from $sourcePath to $destinationPath", e)
        } catch (e: IllegalArgumentException) {
            DLog.e(TAG, "IllegalArgumentException: Possibly an issue with file format or tag for $destinationPath", e)
        } catch (e: Exception) {
            DLog.e(TAG, "Unexpected error during EXIF copy from $sourcePath to $destinationPath", e)
        }
        return false
    }

    private fun copyGpsTags(sourceExif: ExifInterface, destinationExif: ExifInterface) {
        // GPS Latitude
        sourceExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let { lat ->
            sourceExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)?.let { latRef ->
                destinationExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, lat)
                destinationExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef)
            }
        }
        // GPS Longitude
        sourceExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let { lon ->
            sourceExif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)?.let { lonRef ->
                destinationExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lon)
                destinationExif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef)
            }
        }
        // GPS Altitude
        sourceExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let { alt ->
            sourceExif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF)?.let { altRef ->
                destinationExif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, alt)
                destinationExif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, altRef)
            }
        }
        // GPS Timestamp and Datestamp (often used together)
        sourceExif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)?.let { destinationExif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, it) }
        sourceExif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)?.let { destinationExif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, it) }
        sourceExif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD)?.let { destinationExif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, it) }
    }

    // A list of common EXIF tags that are generally writable and useful to copy.
    // Refer to ExifInterface documentation for all TAG_ constants.
    @Suppress("DEPRECATION")
    private val EXIF_TAGS_TO_COPY = listOf(
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_DATETIME, // Original datetime
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_EXIF_VERSION, // Should generally not be changed, but can be copied if missing
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_USER_COMMENT,

        // Exposure related (some might be specific to camera processing)
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_BRIGHTNESS_VALUE,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_EXPOSURE_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER, // Aperture
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
        ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
        ExifInterface.TAG_ISO_SPEED_RATINGS, // Deprecated, use TAG_PHOTOGRAPHIC_SENSITIVITY_INDEX
        ExifInterface.TAG_OECF,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, // ISO
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SPECIFICATION,

        // OffsetTime tags (important for correct timezone interpretation of datetime)
        ExifInterface.TAG_OFFSET_TIME,
        ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
        ExifInterface.TAG_OFFSET_TIME_DIGITIZED
        // Add more tags as needed
    )
}

