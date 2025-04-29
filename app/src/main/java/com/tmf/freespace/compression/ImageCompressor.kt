package com.tmf.freespace.compression

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars

import com.tmf.freespace.models.MediaFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class ImageCompressor(context: Context) : ICompressor(context) {
    private var screenWidth = getScreenWidthPixels(context)
    private val screenHeight = getScreenHeightPixels(context)

    override val ffmpegCompressionCommands = listOf(
        "",  //0: No compression
        "{{inputFilePath}}|{{outputFilePath}}|1.0|80",  //1: Compression low  //TODO Tune compression parameters based on real-world testing
        "{{inputFilePath}}|{{outputFilePath}}|1.0|40",  //2: Compression medium
        "{{inputFilePath}}|{{outputFilePath}}|0.50|50",  //3: Compression high
        "{{inputFilePath}}|{{outputFilePath}}|0.3333|50",  //4: Compression very high
        "{{inputFilePath}}|{{outputFilePath}}|0.10|75",  //5: Compression ultra high
    )

    //TODO Implement compression instead of using FFmpeg, which is missing codec for .png and probably others
    override fun compress(mediaFile: MediaFile, outputFilePath: String) : Boolean {
        val ffmpegCommand = super.ffmpegCommand(mediaFile, outputFilePath)
        if (ffmpegCommand.isNotEmpty()) {
            val tokens = ffmpegCommand.split("|")
            if (tokens.size != 4) { throw IllegalArgumentException("Invalid ffmpegCompressionCommand definition: $ffmpegCommand") }
            val compressedWidth = compressedWidth(tokens[2].toFloat())
            val compressedHeight = compressedHeight(tokens[2].toFloat())
            val compressionQuality = tokens[3].toInt()

            return compressToJpeg(tokens[0], tokens[1], compressedWidth, compressedHeight, compressionQuality)
        }

        return true;  //No compression needed
    }

    @Suppress("DEPRECATION")
    private fun getScreenWidthPixels(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.width()
        } else {
            windowManager.defaultDisplay.width
        }
    }

    @Suppress("DEPRECATION")
    private fun getScreenHeightPixels(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            windowMetrics.bounds.height()
        } else {
            windowManager.defaultDisplay.height
        }
    }

    private fun compressedWidth(screenSizePct: Float) : Int {
        return Math.round(screenWidth * screenSizePct + 0.5f)
    }

    private fun compressedHeight(screenSizePct: Float) : Int {
        return Math.round(screenHeight * screenSizePct + 0.5f)
    }

    /**
     * Converts an image file of various types to a compressed JPG file.
     *
     * @param inputFilePath The path to the input image file.
     * @param outputFilePath The path to save the output JPG file.
     * @param targetWidth The desired width of the output image.
     * @param targetHeight The desired height of the output image.
     * @param compressionQuality The compression quality of the output JPG (0-100, 100 being the highest quality).
     * @return True if the conversion was successful, false otherwise.
     */
    private fun compressToJpeg(
        inputFilePath: String,
        outputFilePath: String,
        targetWidth: Int,
        targetHeight: Int,
        compressionQuality: Int
    ): Boolean {
        if (compressionQuality !in 0..100) {
            throw IllegalArgumentException("Compression quality must be between 0 and 100")
        }

        return try {
            // 1. Load the image
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888 // Ensure we get a bitmap we can work with
            }
            val originalBitmap = BitmapFactory.decodeFile(inputFilePath, options)
                ?: throw IOException("Failed to decode input file")

            // 2. Resize the image
            val resizedBitmap = resizeBitmap(originalBitmap, targetWidth, targetHeight)

            // 3. Convert to JPG and save
            val outputFile = File(outputFilePath)
            FileOutputStream(outputFile).use { outputStream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressionQuality, outputStream)
            }

            // 5. Recycle bitmaps
            originalBitmap.recycle()
            resizedBitmap.recycle()

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Resizes a bitmap to the specified target width and height.
     *
     * @param bitmap The bitmap to resize.
     * @param targetWidth The desired width.
     * @param targetHeight The desired height.
     * @return The resized bitmap.
     */
    private fun resizeBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scaleX = targetWidth.toFloat() / originalWidth
        val scaleY = targetHeight.toFloat() / originalHeight

        val scale = scaleX.coerceAtMost(scaleY)

        val newWidth = (originalWidth * scale).roundToInt()
        val newHeight = (originalHeight * scale).roundToInt()

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val resultBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)

        // Draw the resized image onto the new bitmap
        canvas.drawBitmap(resizedBitmap, 0f, 0f, Paint())

        resizedBitmap.recycle()

        return resultBitmap
    }}
