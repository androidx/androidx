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
@file:Suppress("UNUSED", "VariableInitializerIsRedundant")

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.style.CustomStyle
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.StyleStateKey
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.size
import androidx.compose.foundation.style.state
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun StyleForegroundBackgroundSample() {
    // Create a styleable clickable box
    @Composable
    fun ClickableStyleableBox(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        style: Style = Style,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val styleState = rememberUpdatedStyleState(interactionSource)
        Box(
            modifier =
                modifier
                    .clickable(interactionSource = interactionSource, onClick = onClick)
                    .styleable(styleState, style)
        ) {
            BasicText("Hello")
        }
    }

    ClickableStyleableBox(
        onClick = {},
        style = {
            size(150.dp)
            background(Color.Blue)
            // A semi-transparent overlay that appears when the box is pressed
            pressed { animate { foreground(Color.Black.copy(alpha = 0.4f)) } }
        },
    )
}

@Sampled
@Composable
fun StyleStateKeySample() {
    // Create a new StyleStateKey
    val playingStateKey = StyleStateKey(false)

    // In the module scope are the following declarations:
    //
    //    // A custom style scope which has all the properties of StyleScope
    //    interface MediaPlayerStyleScope : StyleScope
    //
    //    // A custom style type
    //    fun interface MediaPlayerStyle : CustomStyle<MediaPlayerStyleScope> {
    //        companion object : MediaPlayerStyle {
    //            override fun MediaPlayerStyleScope.applyStyle() { }
    //        }
    //    }

    // Introduce a function to convert the custom style to a Style
    fun MediaPlayerStyle.toStyle(): Style = Style {
        val scope = object : StyleScope by this, MediaPlayerStyleScope {}
        with(scope) { applyStyle() }
    }

    // Introduce an extension function to read the style state. This will only be
    // available in a MediaPlayerStyle.
    fun MediaPlayerStyleScope.playerPlaying(block: () -> Unit) {
        state(playingStateKey, block)
    }

    // The MediaPlayer composable
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun MediaPlayer(
        url: String,
        modifier: Modifier = Modifier,
        style: MediaPlayerStyle = MediaPlayerStyle,
        playing: Boolean = true,
    ) {
        val styleState =
            rememberUpdatedStyleState(null) { state -> state[playingStateKey] = playing }
        Box(modifier = modifier.styleable(styleState, style.toStyle())) {
            // Implementation of the media player
        }
    }

    // Using the style in a composable that sets the state.
    MediaPlayer(
        url = "https://example.com/media/video",
        style = {
            borderColor(Color.Gray)

            // This only sets the border color to green when the media player is playing
            playerPlaying { borderColor(Color.Green) }
        },
    )
}

@Composable
fun TextStyleTextMotionSample() {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource)

    // Use TextMotion.Animated to create smoother text animations when scaling.
    val style = Style {
        textMotion(TextMotion.Animated)
        fontSize(20.sp)
        pressed {
            // Animate to a larger font size when pressed
            animate(spec = tween(1000)) { fontSize(40.sp) }
        }
    }

    Box(
        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = {})
            .styleable(styleState = styleState, style = style)
    ) {
        BasicText("Animated Smooth Text")
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun MediaPlayer(url: String, modifier: Modifier = Modifier, style: Style = Style) {}

interface MediaPlayerStyleScope : StyleScope

fun interface MediaPlayerStyle : CustomStyle<MediaPlayerStyleScope> {
    companion object : MediaPlayerStyle {
        override fun MediaPlayerStyleScope.applyStyle() {}
    }
}

fun MediaPlayerStyle.toStyle(): Style = Style {
    val scope = object : StyleScope by this, MediaPlayerStyleScope {}
    with(scope) { applyStyle() }
}
