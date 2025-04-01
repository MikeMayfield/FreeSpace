package com.tmf.freespace.database

import com.tmf.freespace.models.TeraBox

interface TeraBoxDao {
    //    @Query("SELECT * FROM TeraBox LIMIT 1")
    suspend fun getData(): TeraBox? {
        TODO("Not yet implemented")
    }

    suspend fun upsert(teraBox: TeraBox) {
        TODO("Not yet implemented")
    }
}