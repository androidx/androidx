/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Label
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.RangeSliderState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Preview
@Sampled
@Composable
fun SliderSample() {
    var sliderPosition by remember { mutableStateOf(0f) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderPosition))
        Slider(
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            value = sliderPosition,
            onValueChange = { sliderPosition = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun LegacySliderSample() {
    var sliderPosition by remember { mutableStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val trackHeight = 4.dp
    val thumbSize = DpSize(20.dp, 20.dp)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderPosition))
        Slider(
            interactionSource = interactionSource,
            modifier =
                Modifier.semantics { contentDescription = "Localized Description" }
                    .requiredSizeIn(minWidth = thumbSize.width, minHeight = trackHeight),
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            thumb = {
                val modifier =
                    Modifier.size(thumbSize)
                        .shadow(1.dp, CircleShape, clip = false)
                        .indication(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false, radius = 20.dp)
                        )
                SliderDefaults.Thumb(interactionSource = interactionSource, modifier = modifier)
            },
            track = {
                val modifier = Modifier.height(trackHeight)
                SliderDefaults.Track(
                    sliderState = it,
                    modifier = modifier,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                    drawStopIndicator = null
                )
            }
        )
    }
}

@Preview
@Sampled
@Composable
fun StepsSliderSample() {
    var sliderPosition by remember { mutableStateOf(0f) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = sliderPosition.roundToInt().toString())
        Slider(
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
            // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
            // there are 9 steps (10, 20, ..., 90).
            steps = 9
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SliderWithCustomThumbSample() {
    var sliderPosition by remember { mutableStateOf(0f) }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Slider(
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..100f,
            interactionSource = interactionSource,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
            thumb = {
                Label(
                    label = {
                        PlainTooltip(modifier = Modifier.sizeIn(45.dp, 25.dp).wrapContentWidth()) {
                            Text("%.2f".format(sliderPosition))
                        }
                    },
                    interactionSource = interactionSource
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        tint = Color.Red
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SliderWithCustomTrackAndThumb() {
    val sliderState = remember {
        SliderState(
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            }
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderState.value))
        Slider(
            state = sliderState,
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(interactionSource = interactionSource, colors = colors)
            },
            track = { SliderDefaults.Track(colors = colors, sliderState = sliderState) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun RangeSliderSample() {
    val rangeSliderState = remember {
        RangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            }
        )
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val rangeStart = "%.2f".format(rangeSliderState.activeRangeStart)
        val rangeEnd = "%.2f".format(rangeSliderState.activeRangeEnd)
        Text(text = "$rangeStart .. $rangeEnd")
        RangeSlider(
            state = rangeSliderState,
            modifier = Modifier.semantics { contentDescription = "Localized Description" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun LegacyRangeSliderSample() {
    val rangeSliderState = remember {
        RangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            }
        )
    }
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    val trackHeight = 4.dp
    val thumbSize = DpSize(20.dp, 20.dp)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val rangeStart = "%.2f".format(rangeSliderState.activeRangeStart)
        val rangeEnd = "%.2f".format(rangeSliderState.activeRangeEnd)
        Text(text = "$rangeStart .. $rangeEnd")
        RangeSlider(
            state = rangeSliderState,
            startInteractionSource = startInteractionSource,
            endInteractionSource = endInteractionSource,
            modifier =
                Modifier.semantics { contentDescription = "Localized Description" }
                    .requiredSizeIn(minWidth = thumbSize.width, minHeight = trackHeight),
            startThumb = {
                val modifier =
                    Modifier.size(thumbSize)
                        .shadow(1.dp, CircleShape, clip = false)
                        .indication(
                            interactionSource = startInteractionSource,
                            indication = ripple(bounded = false, radius = 20.dp)
                        )
                SliderDefaults.Thumb(
                    interactionSource = startInteractionSource,
                    modifier = modifier
                )
            },
            endThumb = {
                val modifier =
                    Modifier.size(thumbSize)
                        .shadow(1.dp, CircleShape, clip = false)
                        .indication(
                            interactionSource = endInteractionSource,
                            indication = ripple(bounded = false, radius = 20.dp)
                        )
                SliderDefaults.Thumb(interactionSource = endInteractionSource, modifier = modifier)
            },
            track = {
                val modifier = Modifier.height(trackHeight)
                SliderDefaults.Track(
                    rangeSliderState = it,
                    modifier = modifier,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                    drawStopIndicator = null
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun StepRangeSliderSample() {
    val rangeSliderState = remember {
        RangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
            // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
            // there are 9 steps (10, 20, ..., 90).
            steps = 9
        )
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val rangeStart = rangeSliderState.activeRangeStart.roundToInt()
        val rangeEnd = rangeSliderState.activeRangeEnd.roundToInt()
        Text(text = "$rangeStart .. $rangeEnd")
        RangeSlider(
            state = rangeSliderState,
            modifier = Modifier.semantics { contentDescription = "Localized Description" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun RangeSliderWithCustomComponents() {
    val rangeSliderState = remember {
        RangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            }
        )
    }
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    val startThumbAndTrackColors =
        SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Red)
    val endThumbColors = SliderDefaults.colors(thumbColor = Color.Green)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        RangeSlider(
            state = rangeSliderState,
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            startInteractionSource = startInteractionSource,
            endInteractionSource = endInteractionSource,
            startThumb = {
                Label(
                    label = {
                        PlainTooltip(modifier = Modifier.sizeIn(45.dp, 25.dp).wrapContentWidth()) {
                            Text("%.2f".format(rangeSliderState.activeRangeStart))
                        }
                    },
                    interactionSource = startInteractionSource
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = startInteractionSource,
                        colors = startThumbAndTrackColors
                    )
                }
            },
            endThumb = {
                Label(
                    label = {
                        PlainTooltip(
                            modifier = Modifier.requiredSize(45.dp, 25.dp).wrapContentWidth()
                        ) {
                            Text("%.2f".format(rangeSliderState.activeRangeEnd))
                        }
                    },
                    interactionSource = endInteractionSource
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = endInteractionSource,
                        colors = endThumbColors
                    )
                }
            },
            track = { rangeSliderState ->
                SliderDefaults.Track(
                    colors = startThumbAndTrackColors,
                    rangeSliderState = rangeSliderState
                )
            }
        )
    }
}
