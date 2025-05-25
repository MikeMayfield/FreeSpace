package com.tmf.freespace.models

enum class CloudStorageType(val value: Int) {
    Integrated(0),
    GoggleDrive(1),
    DropBox(2),
    //etc.
    Simulated(99),
}
