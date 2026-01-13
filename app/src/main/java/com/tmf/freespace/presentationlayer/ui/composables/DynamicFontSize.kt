package com.tmf.freespace.presentationlayer.ui.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.tmf.freespace.domainlayer.general.DLog
import com.tmf.freespace.domainlayer.general.FontSize

@Composable
fun DynamicFontSize(emSpaceCnt: Int, childComposable: @Composable () -> Unit) {
    val tag = "DynamicFontSize"
    val emSpaces = "M".repeat(emSpaceCnt)
    val textStyle = TextStyle(
        fontSize = FontSize.H1 * 3.0f,  //Start with a large font size and shrink from there
        fontWeight = FontWeight.Bold,
    )
    var resizedTextStyle by remember { mutableStateOf(textStyle) }
    var shouldShrink by remember { mutableStateOf(true) }
    var priorFontScale by remember { mutableFloatStateOf(0f) }

    //If font scale changes, recompute the dynamic font sizes
    if (LocalDensity.current.fontScale != priorFontScale) {
        shouldShrink = true
        resizedTextStyle = resizedTextStyle.copy(
            fontSize = FontSize.H1 * 3.0f
        )
        FontSize.DynamicFontResizePct = 1.0f
        priorFontScale = LocalDensity.current.fontScale
    }

    //Text box with text that resizes down until it will fit with constraints
    if (FontSize.DynamicFontResizePct == 1.0f && shouldShrink) {
        Text(
            text = emSpaces,
            color = Color.Transparent,
            // These parameters enable auto-sizing
            softWrap = false,
            maxLines = 1,
            style = resizedTextStyle,
            modifier = Modifier.fillMaxWidth(),
            onTextLayout = { result ->
                // When the text layout is calculated, check if it overflowed
                if (shouldShrink && result.didOverflowWidth) {
                    // If it overflowed, calculate the new smaller font size
                    resizedTextStyle = resizedTextStyle.copy(
                        fontSize = resizedTextStyle.fontSize * 0.95f
                    )
                } else {
                    // If it fits, stop shrinking and compute dynamic font sizes
                    shouldShrink = false
                    with (FontSize) {
                        val fontResizePct = (resizedTextStyle.fontSize.value / H1.value)
                        H1Dynamic = H1 * fontResizePct
                        BodyDynamic = Body * fontResizePct
                        BodySmallDynamic = BodySmall * fontResizePct
                        SmallDynamic = Small * fontResizePct
                        DynamicFontResizePct = fontResizePct
                        DLog.d(tag, "Dynamic font sizes: H1: $H1Dynamic, Body:$BodyDynamic, BodySmall: $BodySmallDynamic, Small: $SmallDynamic")
                    }
                }
            }
        )
    }
    else {
        childComposable()
    }
}
