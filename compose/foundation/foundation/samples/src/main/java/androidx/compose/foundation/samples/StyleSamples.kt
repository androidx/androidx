/*
 * Copyright 2026 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationStyleApi::class)
@file:Suppress("UNUSED")

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.StyleStateKey
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@Sampled
fun SimpleStyleSample() {
    // Create a styleable box
    @Composable
    fun StyleableBox(modifier: Modifier = Modifier, style: Style = Style) {
        Box(modifier = modifier.styleable(null, style))
    }

    // Style the styleable box to be a 150x150 green box
    StyleableBox(
        style = {
            background(Color.Green)
            size(150.dp)
        }
    )
}

@Composable
@Sampled
fun StyleStateSample() {
    // Create a styleable clickable box
    @Composable
    fun ClickableStyleableBox(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        style: Style = Style,
        enabled: Boolean = true,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = rememberUpdatedStyleState(interactionSource) { it.isEnabled = enabled }
        Box(
            modifier =
                modifier
                    .clickable(interactionSource = interactionSource, onClick = onClick)
                    .styleable(styleState, style)
        )
    }

    // Create a 150x150 green box that is clickable
    ClickableStyleableBox(
        onClick = {},
        style = {
            background(Color.Green)
            size(150.dp)
            hovered { background(Color.Yellow) }
            pressed { background(Color.Red) }
            disabled { background(Color.Gray) }
        },
    )
}

@Composable
@Sampled
fun StyleAnimationSample() {
    // Create a styleable clickable box
    @Composable
    fun ClickableStyleableBox(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        style: Style = Style,
        enabled: Boolean = true,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = rememberUpdatedStyleState(interactionSource) { it.isEnabled = enabled }
        Box(
            modifier =
                modifier
                    .clickable(interactionSource = interactionSource, onClick = onClick)
                    .styleable(styleState, style)
        )
    }

    ClickableStyleableBox(
        onClick = {},
        style = {
            background(Color.Blue)
            size(150.dp)
            hovered { animate { background(Color.Yellow) } }
            pressed { animate { background(Color.Red) } }
            disabled { animate { background(Color.Gray) } }
        },
    )
}

@Sampled
@Composable
fun StyleStateKeySample() {
    // Create a new StyleStateKey
    val playingStateKey = StyleStateKey(false)

    // Introduce an extension function to read the style state
    fun StyleScope.playerPlaying(value: Style) {
        state(playingStateKey, value) { key, state -> state[key] }
    }

    // Using the extension function to change the border color to green while playing
    val style = Style {
        borderColor(Color.Gray)
        playerPlaying { borderColor(Color.Green) }
    }

    // Using the style in a composable that sets the state.
    MediaPlayer(url = "https://example.com/media/video", style = style)
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MediaPlayer(url: String, modifier: Modifier = Modifier, style: Style = Style) {}
