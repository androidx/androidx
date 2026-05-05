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

package androidx.compose.material3.demos

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/** Demo for a volume-style slider with orientation and direction controls. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SysUiVolumeSliderDemo() {
    var volume by remember { mutableFloatStateOf(0.5f) }
    var isVertical by remember { mutableStateOf(false) }
    var isReverseDirection by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Volume: %.2f".format(volume))
        // Controls for slider orientation and direction
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isVertical, onCheckedChange = { isVertical = it })
            Text("Vertical")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isReverseDirection,
                onCheckedChange = { isReverseDirection = it },
                enabled = isVertical,
            )
            Text("Reverse Direction")
        }

        VolumeSlider(
            value = volume,
            onValueChange = { volume = it },
            isVertical = isVertical,
            isReverseDirection = isReverseDirection,
            modifier = if (isVertical) Modifier.align(Alignment.CenterHorizontally) else Modifier,
        )
    }
}

/** Customized slider with system-ui style dimensions. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isVertical: Boolean = false,
    isReverseDirection: Boolean = false,
) {
    // Adapt dimensions and modifiers based on orientation
    val trackModifier =
        if (isVertical) Modifier.width(TrackHeight).height(200.dp) else Modifier.height(TrackHeight)
    val thumbSize =
        if (isVertical) DpSize(ThumbHeight, ThumbWidth) else DpSize(ThumbWidth, ThumbHeight)
    val sliderModifier = if (isVertical) modifier.height(200.dp) else modifier.fillMaxWidth()

    SysUiSlider(
        value = value,
        onValueChanged = onValueChange,
        isVertical = isVertical,
        isReverseDirection = isReverseDirection,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = trackModifier,
                thumbTrackGapSize = 6.dp,
                trackCornerSize = SliderTrackRoundedCorner,
            )
        },
        thumb = { sliderState, interactionSource ->
            SliderDefaults.Thumb(
                sliderState = sliderState,
                interactionSource = interactionSource,
                thumbSize = thumbSize,
            )
        },
        modifier = sliderModifier,
    )
}

/** Generic wrapper for Slider and VerticalSlider with stable state management. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SysUiSlider(
    value: Float,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    isVertical: Boolean = false,
    isReverseDirection: Boolean = false,
    track: @Composable (SliderState) -> Unit = { SliderDefaults.Track(it) },
    thumb: @Composable (SliderState, MutableInteractionSource) -> Unit = { _, interactionSource ->
        SliderDefaults.Thumb(interactionSource = interactionSource)
    },
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Ensure SliderState is stable across orientation/direction changes
    val sliderState =
        rememberSaveable(
            steps,
            valueRange,
            isVertical,
            isReverseDirection,
            saver = SliderState.Saver(null, valueRange),
        ) {
            SliderState(value, steps, valueRange = valueRange)
        }

    // Sync internal state with external value updates
    LaunchedEffect(value) { if (sliderState.value != value) sliderState.value = value }

    sliderState.onValueChange = { newValue ->
        if (sliderState.isDragging) sliderState.value = newValue
        onValueChanged(newValue)
    }

    if (isVertical) {
        VerticalSlider(
            state = sliderState,
            reverseDirection = isReverseDirection,
            interactionSource = interactionSource,
            track = track,
            thumb = { thumb(it, interactionSource) },
            modifier = modifier,
        )
    } else {
        Slider(
            state = sliderState,
            interactionSource = interactionSource,
            track = track,
            thumb = { thumb(it, interactionSource) },
            modifier = modifier,
        )
    }
}

private val TrackHeight = 40.dp
private val SliderTrackRoundedCorner = 12.dp
private val ThumbHeight = 52.dp
private val ThumbWidth = 4.dp
