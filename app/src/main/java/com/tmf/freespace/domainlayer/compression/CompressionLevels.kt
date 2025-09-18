package com.tmf.freespace.domainlayer.compression

/**
 * Desired compression levels based on date of creation (TODO Maybe use date of last modification)
 */
class CompressionLevels {
    val compressionLevels = listOf(
        CompressionLevel(0, 31, 3, 1),  //No compression allowed  //TODO Delete this level after testing or set to no compression
        CompressionLevel(31, 60, 1, 1),  //Image: Resolution 100% of screen, Compression 25%; Video: Screen resolution, Compression 25%
        CompressionLevel(60, 180, 2, 2),  //Image: Resolution 100% of screen, Compression 50%; Video: Resolution 720p (<=screen resolution), Compression 50%
        CompressionLevel(180, 365, 3, 3),  //Image: Resolution 50% of screen, Compression 75%; Video: Resolution 720p (<=screen), Compression 80%
        CompressionLevel(365, 10000, 4, 4),  //Image: Resolution 50% of screen, Compression 90%; Video: Resolution 480p (<=screen), Compression 90%
    )

    data class CompressionLevel(
        val minDays: Int,
        val maxDays: Int,
        val imageCompressionLevel: Int,
        val videoCompressionLevel: Int,
    )
}