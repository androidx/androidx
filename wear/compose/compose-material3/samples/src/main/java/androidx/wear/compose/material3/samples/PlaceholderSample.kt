/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.PlaceholderDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.placeholderShimmer
import androidx.wear.compose.material3.rememberPlaceholderState
import kotlinx.coroutines.delay

/**
 * This sample applies placeholders directly over the content that is waiting to be loaded. This
 * approach is suitable for situations where the developer is confident that the stadium shaped
 * placeholder will cover the content in the period between when it is loaded and when the
 * placeholder visual effects will have finished.
 */
@Sampled
@Composable
fun ButtonWithIconAndLabelAndPlaceholders() {
    var labelText by remember { mutableStateOf("") }
    var imageVector: ImageVector? by remember { mutableStateOf(null) }
    val buttonPlaceholderState = rememberPlaceholderState {
        labelText.isNotEmpty() && imageVector != null
    }

    Button(
        onClick = { /* Do something */ },
        enabled = true,
        label = {
            Text(
                text = labelText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().placeholder(buttonPlaceholderState)
            )
        },
        icon = {
            Box(
                modifier =
                    Modifier.size(ButtonDefaults.IconSize).placeholder(buttonPlaceholderState)
            ) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector!!,
                        contentDescription = "Heart",
                        modifier =
                            Modifier.wrapContentSize(align = Alignment.Center)
                                .size(ButtonDefaults.IconSize)
                                .fillMaxSize(),
                    )
                }
            }
        },
        colors =
            PlaceholderDefaults.placeholderButtonColors(
                originalButtonColors = ButtonDefaults.buttonColors(),
                placeholderState = buttonPlaceholderState
            ),
        modifier = Modifier.fillMaxWidth().placeholderShimmer(buttonPlaceholderState)
    )
    // Simulate content loading completing in stages
    LaunchedEffect(Unit) {
        delay(2000)
        imageVector = Icons.Filled.Favorite
        delay(1000)
        labelText = "A label"
    }
    if (!buttonPlaceholderState.isHidden) {
        LaunchedEffect(buttonPlaceholderState) { buttonPlaceholderState.animatePlaceholder() }
    }
}

/**
 * This sample places a [Button] with placeholder effects applied to it on top of the [Button] that
 * contains the actual content.
 *
 * This approach is needed in situations where the developer wants a higher degree of control over
 * what is shown as a placeholder before the loaded content is revealed. This approach can be used
 * when there would otherwise be content becoming visible as it is loaded and before the
 * placeholders have been wiped-off to reveal the content underneath, e.g. If the content contains
 * left|top aligned Text that would be visible in the part of the content slot not covered by the
 * stadium placeholder shape.
 */
@Sampled
@Composable
fun ButtonWithIconAndLabelsAndOverlaidPlaceholder() {
    var labelText by remember { mutableStateOf("") }
    var secondaryLabelText by remember { mutableStateOf("") }
    var imageVector: ImageVector? by remember { mutableStateOf(null) }

    val buttonPlaceholderState = rememberPlaceholderState {
        labelText.isNotEmpty() && secondaryLabelText.isNotEmpty() && imageVector != null
    }
    Box {
        if (buttonPlaceholderState.isHidden || buttonPlaceholderState.isWipingOff) {
            Button(
                onClick = { /* Do something */ },
                enabled = true,
                label = {
                    Text(
                        text = labelText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                secondaryLabel = {
                    Text(
                        text = secondaryLabelText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                icon = {
                    if (imageVector != null) {
                        Icon(
                            imageVector = imageVector!!,
                            contentDescription = "Heart",
                            modifier =
                                Modifier.wrapContentSize(align = Alignment.Center)
                                    .size(ButtonDefaults.IconSize)
                                    .fillMaxSize(),
                        )
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!buttonPlaceholderState.isHidden) {
            Button(
                onClick = { /* Do something */ },
                enabled = true,
                label = {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(16.dp)
                                .padding(top = 1.dp, bottom = 1.dp)
                                .placeholder(placeholderState = buttonPlaceholderState)
                    )
                },
                secondaryLabel = {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(16.dp)
                                .padding(top = 1.dp, bottom = 1.dp)
                                .placeholder(placeholderState = buttonPlaceholderState)
                    )
                },
                icon = {
                    Box(
                        modifier =
                            Modifier.size(ButtonDefaults.IconSize)
                                .placeholder(buttonPlaceholderState)
                    )
                    // Simulate the icon becoming ready after a period of time
                    LaunchedEffect(Unit) {
                        delay(2000)
                        imageVector = Icons.Filled.Favorite
                    }
                },
                colors =
                    PlaceholderDefaults.placeholderButtonColors(
                        placeholderState = buttonPlaceholderState
                    ),
                modifier = Modifier.fillMaxWidth().placeholderShimmer(buttonPlaceholderState)
            )
        }
    }
    // Simulate data being loaded after a delay
    LaunchedEffect(Unit) {
        delay(2500)
        secondaryLabelText = "A secondary label"
        delay(500)
        labelText = "A label"
    }
    if (!buttonPlaceholderState.isHidden) {
        LaunchedEffect(buttonPlaceholderState) { buttonPlaceholderState.animatePlaceholder() }
    }
}

/**
 * This sample applies a placeholder and placeholderShimmer directly over a single composable.
 *
 * Note that the modifier ordering is important, the placeholderShimmer must be before the
 * placeholder in the modifier chain - otherwise the shimmer will be drawn underneath the
 * placeholder and will not be visible.
 */
@Sampled
@Composable
fun TextPlaceholder() {
    var labelText by remember { mutableStateOf("") }
    val buttonPlaceholderState = rememberPlaceholderState { labelText.isNotEmpty() }

    Text(
        text = labelText,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier =
            Modifier.width(90.dp)
                .placeholderShimmer(buttonPlaceholderState)
                .placeholder(buttonPlaceholderState)
    )

    // Simulate content loading
    LaunchedEffect(Unit) {
        delay(3000)
        labelText = "A label"
    }
    if (!buttonPlaceholderState.isHidden) {
        LaunchedEffect(buttonPlaceholderState) { buttonPlaceholderState.animatePlaceholder() }
    }
}
