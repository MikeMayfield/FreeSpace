package com.tmf.freespace.domainlayer.general

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object FontSize {
    //Static font sizes
    val H1 = 20.sp
    val Body = 16.sp
    val BodySmall = 14.sp
    val Small = 10.sp
    val SpacerHeightSmall = 4.dp
    val SpacerHeightDefault = 8.dp
    val SpacerHeightExtra = 14.dp
    val SpacerHeightDivision = 24.dp

    //Dynamic font sizes, as set by DynamicFontSize.kt
    var DynamicFontResizePct = 1.0f
    var H1Dynamic = H1
    var BodyDynamic = Body
    var BodySmallDynamic = BodySmall
    var SmallDynamic = Small
}