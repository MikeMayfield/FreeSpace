package com.tmf.freespace.presentationlayer.ui.composables

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tmf.freespace.domainlayer.general.Const

@Composable
fun GenericTextBody(
    modifier: Modifier = Modifier,
    imageID: Int? = null,  //Resource ID for the image, if any
    title: String? = null,  //Title text, if any
    bodyHtml: String,
    navButtonText: String? = "NEXT",  //Bottom navigation button text, if any
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onBodyClick: () -> Unit = {},  //Callback for body click
    onNavButtonClick: () -> Unit = {},  //Callback for navigation button click
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(White)
            .padding(paddingValues)
            .padding(horizontal = 24.dp), //Apply left/right border padding to the whole screen
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (scrollState.canScrollBackward) {
            HorizontalDivider()
        }

        // This inner Column holds all the scrollable content
        Column(
            modifier = Modifier
                .weight(1f) // Takes up all available space, pushing the button to the bottom
                .verticalScroll(scrollState), // Makes this section scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Const.SpacerHeightDefault)) // Some space at the top

            //Top image
            if (imageID != null) {
                Image(
                    painter = painterResource(id = imageID),
                    contentDescription = "Content image",
                    modifier = Modifier.fillMaxWidth(0.8f)
                )

                Spacer(modifier = Modifier.height(Const.SpacerHeightDivision))
            }

            //Title
            if (title != null) {
                Text(
                    AnnotatedString.fromHtml(title),
                    fontSize = Const.FontSizeH1,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xff04049d),
                    textAlign = TextAlign.Center,
                    modifier = modifier
                )

                Spacer(modifier = Modifier.height(Const.SpacerHeightExtra))
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
                fontSize = Const.FontSizeBody,
                modifier = modifier
                    .align(Alignment.Start)
                    .clickable() {
                        onBodyClick()
                    }
            )
        }

        //Bottom navigation button (This is outside the scrollable Column)
        if (navButtonText != null) {
            Spacer(modifier = Modifier.height(Const.SpacerHeightExtra))
            if (scrollState.canScrollForward) {
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(Const.SpacerHeightDefault))

            DynamicButton(navButtonText) {
                onNavButtonClick()
            }
        }
    }
}
