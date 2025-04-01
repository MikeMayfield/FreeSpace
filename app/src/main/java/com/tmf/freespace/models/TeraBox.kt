package com.tmf.freespace.models


//Space available and used in TeraBox (space is either 1TB for free or 2TB for premium. More than 2TB is not supported/used)
data class TeraBox(
    val userName: String,
    val password: String,
    val spaceAvailableBytes: Long,  //Free: (32GB) 32,359,738,368 bytes; Paid: (512GB) 549,755,813,888; TeraBox Premium: (2TB+) 2,199,023,255,552 or more
    val spaceUsedBytes: Long,
)
