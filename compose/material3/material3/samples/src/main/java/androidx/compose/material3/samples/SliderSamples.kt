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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Preview
@Sampled
@Composable
fun SliderSample() {
    var sliderPosition by remember { mutableStateOf(0f) }
    Column {
        Text(text = sliderPosition.toString())
        Slider(
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            value = sliderPosition,
            onValueChange = { sliderPosition = it })
    }
}

@Preview
@Sampled
@Composable
fun StepsSliderSample() {
    var sliderPosition by remember { mutableStateOf(0f) }
    Column {
        Text(text = sliderPosition.toString())
        Slider(
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
            steps = 4
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
    Column {
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
                        PlainTooltip(
                            modifier = Modifier
                                .requiredSize(45.dp, 25.dp)
                                .wrapContentWidth()
                        ) {
                            val roundedEnd =
                                (sliderPosition * 100.0).roundToInt() / 100.0
                            Text(roundedEnd.toString())
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
    Column {
        Text(text = sliderState.value.toString())
        Slider(
            state = sliderState,
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors
                )
            },
            track = {
                SliderDefaults.Track(
                    colors = colors,
                    sliderState = sliderState
                )
            }
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
    Column {
        Text(text = (rangeSliderState.activeRangeStart..rangeSliderState.activeRangeEnd).toString())
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
            steps = 5
        )
    }
    Column {
        Text(text = (rangeSliderState.activeRangeStart..rangeSliderState.activeRangeEnd).toString())
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
    val startThumbAndTrackColors = SliderDefaults.colors(
        thumbColor = Color.Blue,
        activeTrackColor = Color.Red
    )
    val endThumbColors = SliderDefaults.colors(thumbColor = Color.Green)
    Column {
        RangeSlider(
            state = rangeSliderState,
            modifier = Modifier.semantics { contentDescription = "Localized Description" },
            startInteractionSource = startInteractionSource,
            endInteractionSource = endInteractionSource,
            startThumb = {
                Label(
                    label = {
                        PlainTooltip(
                            modifier = Modifier
                                .requiredSize(45.dp, 25.dp)
                                .wrapContentWidth()
                        ) {
                            val roundedStart =
                                (rangeSliderState.activeRangeStart * 100.0).roundToInt() / 100.0
                            Text(roundedStart.toString())
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
                            modifier = Modifier
                                .requiredSize(45.dp, 25.dp)
                                .wrapContentWidth()
                        ) {
                            val roundedEnd =
                                (rangeSliderState.activeRangeEnd * 100.0).roundToInt() / 100.0
                            Text(roundedEnd.toString())
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
