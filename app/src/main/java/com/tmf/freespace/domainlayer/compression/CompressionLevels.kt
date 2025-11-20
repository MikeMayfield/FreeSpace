package com.tmf.freespace.domainlayer.compression

/**
 * Desired compression ratios (1:n), based on date of creation (TODO Maybe use date of last modification)
 */
class CompressionLevels {
    val compressionLevels = listOf(
        CompressionLevel(0, 31, 5, 3, 0),  //TODO ***Change to ratio=0 or remove after debugging. This date range is never compressed
        CompressionLevel(31, 60, 1, 1, 1),  //Ratio 1 means minimum compression, but compressed
        CompressionLevel(60, 90, 3, 2, 3),
        CompressionLevel(90, 180, 5, 3, 5),
        CompressionLevel(180, 365, 10, 8, 8),
        CompressionLevel(365, 730, 20, 10, 10),
        CompressionLevel(730, 10000, 20, 20, 20),
    )

    data class CompressionLevel(
        val minDays: Int,
        val maxDays: Int,
        val imageCompressionRatio: Int,
        val videoCompressionRatio: Int,
        val audioCompressionRatio: Int,
    )
}