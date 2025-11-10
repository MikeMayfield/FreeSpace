package com.tmf.freespace

import android.app.Application
import com.tmf.freespace.datalayer.datasources.local.PropertyBag
import com.tmf.freespace.datalayer.datasources.local.PropertyBag.IS_IDLE

class BaseApplication: Application() {
    companion object {
        lateinit var instance: BaseApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        PropertyBag.getBoolean(IS_IDLE)  //Force PropertyBag to load on startup
    }
}