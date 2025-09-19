package com.tmf.freespace.domainlayer.compression

/**
 * Desired compression ratios (1:n), based on date of creation (TODO Maybe use date of last modification)
 */
class CompressionLevels {
    val compressionLevels = listOf(
        CompressionLevel(0, 31, 5, 1, 1),  //TODO Change to ratio=0 or remove after debugging. This date range is never compressed
        CompressionLevel(31, 60, 2, 2, 2),
        CompressionLevel(60, 90, 3, 3, 3),
        CompressionLevel(90, 180, 5, 5, 5),
        CompressionLevel(180, 365, 10, 10, 10),
        CompressionLevel(365, 10000, 20, 20, 20),
    )

    data class CompressionLevel(
        val minDays: Int,
        val maxDays: Int,
        val imageCompressionRatio: Int,
        val videoCompressionRatio: Int,
        val audioCompressionRatio: Int,
    )
}