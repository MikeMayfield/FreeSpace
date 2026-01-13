package com.tmf.freespace.presentationlayer.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tmf.freespace.domainlayer.general.FontSize

@Composable
fun DynamicButton(text: String, onClick: () -> Unit) {
    val textStyle = TextStyle(
        fontSize = FontSize.Body,  //Start with a large font size and shrink from there
        fontWeight = FontWeight.Bold,
    )
    var resizedTextStyle by remember { mutableStateOf(textStyle) }
    var shouldShrink by remember { mutableStateOf(true) }

    Button(
        onClick = {
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
        modifier = Modifier
            .sizeIn(maxWidth = 500.dp)
            .fillMaxWidth(0.6f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            //Text box with text that resizes down until it will fit with constraints
            Text(
                text = text,
                color = Color.White,
                // These parameters enable auto-sizing
                softWrap = false,
                maxLines = 1,
                style = resizedTextStyle,
                onTextLayout = { result ->
                    // When the text layout is calculated, check if it overflowed
                    if (shouldShrink && result.didOverflowWidth) {
                        // If it overflowed, calculate the new smaller font size
                        resizedTextStyle = resizedTextStyle.copy(
                            fontSize = resizedTextStyle.fontSize * 0.9f
                        )
                    } else {
                        // If it fits, stop shrinking
                        shouldShrink = false
                    }
                }
            )
        }
    }
}
