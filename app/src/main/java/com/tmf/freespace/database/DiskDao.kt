package com.tmf.freespace.database

import com.tmf.freespace.models.Disk

class DiskDao {
    suspend fun getData(): Disk? {
        TODO("Not yet implemented")

        /*
            @Query("SELECT dsk.id, dsk.path, SUM(mf.bytesCompressed) AS expandedSizeBytes " +
                "FROM MediaFile AS mf " +
                    "JOIN Directory AS dir ON mf.directoryPathID = dir.id " +
                    "JOIN Disk AS dsk ON dir.diskID = dsk.id " +
                    "GROUP BY dsk.id " +
                "LIMIT 1")
         */
    }

    suspend fun upsert(disk: Disk) {
        TODO("Not yet implemented")
    }
}
