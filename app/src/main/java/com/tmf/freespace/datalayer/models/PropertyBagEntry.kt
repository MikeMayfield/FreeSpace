package com.tmf.freespace.datalayer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PropertyBag entry for bags of key/value pairs
 *
 * @property key The unique ID of the property bag element
 * @property value The value of the property bag element
 */
@Entity(tableName = "PropertyBag")
data class PropertyBagEntry(
    @PrimaryKey val key: String,
    val value: String
)