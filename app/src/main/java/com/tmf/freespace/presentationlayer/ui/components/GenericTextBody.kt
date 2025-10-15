package com.tmf.freespace.presentationlayer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun GenericTextBody(
    modifier: Modifier = Modifier,
    imageID: Int? = null,  //Resource ID for the image, if any
    titleHtml: String? = null,  //Title text, if any
    bodyHtml: String,
    navButtonText: String? = "NEXT",  //Bottom navigation button text, if any
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBodyClick: () -> Unit = {},  //Callback for body click
    onNavButtonClick: () -> Unit = {}  //Callback for navigation button click
    ) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

        //Top image
        if (imageID != null) {
            Image(
                painter = painterResource(id = imageID),
                contentDescription = "Content image",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        //Title
        if (titleHtml != null) {
            Text(
                AnnotatedString.fromHtml(titleHtml),
                modifier
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        //Main body
        Text(
            AnnotatedString.fromHtml(
                bodyHtml,
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        fontStyle = FontStyle.Italic,
                        color = Color.Blue
                    )
                ),
            ),
            modifier = modifier
                .align(Alignment.Start)
                .clickable() {
                    onBodyClick()
                }
        )

        //Bottom navigation button
        Spacer(modifier = Modifier.weight(1f, fill = true))  //Proportionally add space between top and bottom areas

        if (navButtonText != null) {
            Button(onClick = { onNavButtonClick() }) {
                Text(navButtonText)
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Padding at the very bottom
    }
}