package com.tmf.freespace.domainlayer.compression // Or your preferred package

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.tmf.freespace.domainlayer.general.DLog
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class CompressImageUtil {
    private val tag = "CompressImageUtil"

    /**
     * Resizes and compresses an image file.
     *
     * @param inputPath Path to the existing image file.
     * @param outputPath Path where the processed image will be saved.
     * @param desiredWidth The desired width for the output image. If the original image width
     *                     is smaller than this, the original width will be used.
     * @param maxCompressedSizeBytes Maximum size of compressed file
     * @return True if processing was successful and the output file was created, false otherwise.
     */
    fun compressImage(
        inputPath: String,
        outputPath: String,
        desiredWidth: Int,
        quality: Int,
    ): Boolean {
        val inputFile = File(inputPath)
        if (!inputFile.exists()) {
            DLog.e(tag, "Input file does not exist: $inputPath")
            return false
        }

        // 1. Get original image dimensions without loading the full bitmap into memory
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(inputPath, options)
        val uncompressedWidth = options.outWidth
        val uncompressedHeight = options.outHeight
        if (uncompressedWidth <= 0 || uncompressedHeight <= 0) {  //Validate reasonable image dimensions
            DLog.e(tag, "Invalid uncompressed image dimensions: ${uncompressedWidth}x${uncompressedHeight} for $inputPath")
            return false // Don't process if dimensions are invalid
        }

        // 2. Determine the target width and height for resizing
        val targetWidth: Int
        val targetHeight: Int
        if (desiredWidth >= uncompressedWidth) {
            // Desired width is greater or equal, use original dimensions (or just compress if no resize needed)
            targetWidth = uncompressedWidth
            targetHeight = uncompressedHeight
            DLog.v(tag, "Using original dimensions: ${targetWidth}x${targetHeight} for $inputPath as desiredWidth ($desiredWidth) >= originalWidth ($uncompressedWidth)")
        } else {
            // Resize needed, maintain aspect ratio
            targetWidth = desiredWidth
            val aspectRatio = uncompressedHeight.toFloat() / uncompressedWidth.toFloat()
            targetHeight = (targetWidth * aspectRatio).roundToInt()
            DLog.v(tag, "Resizing to: ${targetWidth}x${targetHeight} for $inputPath")
        }

        // 3. Decode the bitmap with appropriate sampling options to save memory
        options.inJustDecodeBounds = false  //Load full bitmap
        options.inSampleSize = calculateInSampleSize(uncompressedWidth, uncompressedHeight, targetWidth, targetHeight)  //Sampling size, to handle large images
        var inputBitmap: Bitmap? = null
        try {
            inputBitmap = BitmapFactory.decodeFile(inputPath, options)
        } catch (e: OutOfMemoryError) {
            DLog.e(tag, "OutOfMemoryError while decoding bitmap with inSampleSize: ${options.inSampleSize}", e)
            // Optionally, try with a larger inSampleSize if the first attempt fails
            options.inSampleSize *= 2
            try {
                inputBitmap = BitmapFactory.decodeFile(inputPath, options)
            } catch (e2: OutOfMemoryError) {
                DLog.e(tag, "OutOfMemoryError on second attempt to decode bitmap for $inputPath", e2)
                return false
            }
        } catch (e: Exception) {
            DLog.e(tag, "Error decoding bitmap for $inputPath", e)
            return false
        }

        if (inputBitmap == null) {
            DLog.e(tag, "BitmapFactory.decodeFile returned null for $inputPath")
            return false
        }

        // 4. Perform resizing if necessary (if decoded bitmap size is still larger than target)
        var processedBitmap: Bitmap = inputBitmap
        if (inputBitmap.width > targetWidth || inputBitmap.height > targetHeight) {
            // This condition might be true even with inSampleSize if the sample size calculation
            // doesn't bring it exactly to targetWidth/targetHeight but to the nearest power of 2.
            // Or if we decided not to resize earlier (desiredWidth >= originalWidth) but still want to ensure it's not accidentally larger.
            try {
                DLog.v(tag, "Scaling bitmap from ${inputBitmap.width}x${inputBitmap.height} to ${targetWidth}x${targetHeight}")
                processedBitmap = inputBitmap.scale(targetWidth, targetHeight)
                if (processedBitmap != inputBitmap) { // Only recycle if createScaledBitmap created a new one
                    inputBitmap.recycle()
                }
            } catch (e: OutOfMemoryError) {
                DLog.e(tag, "OutOfMemoryError during Bitmap.createScaledBitmap for $inputPath", e)
                inputBitmap.recycle() // Recycle the original bitmap
                return false
            }  catch (e: Exception) {
                DLog.e(tag, "Error during Bitmap.createScaledBitmap for $inputPath", e)
                inputBitmap.recycle()
                return false
            }
        }

        // 5. Handle EXIF Orientation
        try {
            val exifInterface = ExifInterface(inputPath)
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.preScale(-1.0f, 1.0f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.preScale(-1.0f, 1.0f)
                }
            }
            if (!matrix.isIdentity) { // Only apply if a transformation is needed
                val orientedBitmap = Bitmap.createBitmap(processedBitmap, 0, 0, processedBitmap.width, processedBitmap.height, matrix, true)
                if (orientedBitmap != processedBitmap) { // Only recycle if createBitmap created a new one
                    processedBitmap.recycle()
                }
                processedBitmap = orientedBitmap
            }
        } catch (e: IOException) {
            DLog.e(tag, "Could not read EXIF orientation for $inputPath")
        } catch (e: OutOfMemoryError) {
            DLog.e(tag, "OutOfMemoryError while applying EXIF orientation for $inputPath", e)
            processedBitmap.recycle()
            return false
        } catch (e: Exception) {
            DLog.e(tag, "Error while applying EXIF orientation for $inputPath", e)
            processedBitmap.recycle()
            return false
        }


        // 6. Compress and save the bitmap
        val outputFile = File(outputPath)
        if (File(outputPath).exists()) {
            File(outputPath).delete()
        }
        try {
            FileOutputStream(outputFile).use { fos ->
                // Ensure compressionQuality is within 0-100 range
                if (!processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)) {
                    DLog.e(tag, "Bitmap.compress returned false for $outputPath")
                    processedBitmap.recycle()
                    return false
                }
                fos.flush()
            }
            DLog.v(tag, "Successfully processed and saved image to $outputPath")
        }
        catch (e: IOException) {
            DLog.e(tag, "IOException while saving processed image to $outputPath", e)
            if (outputFile.exists()) {
                outputFile.delete() // Clean up partially created file
            }
            processedBitmap.recycle()
            return false
        }
        catch (e: Exception) {
            DLog.e(tag, "Error saving processed image to $outputPath", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            processedBitmap.recycle()
            return false
        }

        // 7. Copy EXIF from original to processed image
        //  The caller should use ExitCopier to copy EXIF from original to processed image if compressed image is accepted

        if (!processedBitmap.isRecycled) {
            processedBitmap.recycle()
        }
        return true
    }

    /**
     * Calculates the `inSampleSize` for `BitmapFactory.Options`. This value is used
     * to sub-sample the image during decoding, saving memory.
     * The value returned will be a power of 2.
     */
    private fun calculateInSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        var inSampleSize = 1
        if (originalHeight > reqHeight || originalWidth > reqWidth) {
            val halfHeight: Int = originalHeight / 2
            val halfWidth: Int = originalWidth / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        DLog.d(tag, "Calculated inSampleSize: $inSampleSize for original ${originalWidth}x${originalHeight} -> req ${reqWidth}x${reqHeight}")
        return inSampleSize
    }
}

