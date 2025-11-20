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
import androidx.compose.animation.core.animate
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.material3.rememberRangeSliderState
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Preview
@Sampled
@Composable
fun SliderSample() {
    var sliderPosition by rememberSaveable { mutableStateOf(0f) }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderPosition))
        Slider(value = sliderPosition, onValueChange = { sliderPosition = it })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun LegacySliderSample() {
    var sliderPosition by rememberSaveable { mutableStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val trackHeight = 4.dp
    val thumbSize = DpSize(20.dp, 20.dp)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderPosition))
        Slider(
            interactionSource = interactionSource,
            modifier = Modifier.requiredSizeIn(minWidth = thumbSize.width, minHeight = trackHeight),
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            thumb = {
                val modifier =
                    Modifier.size(thumbSize)
                        .shadow(1.dp, CircleShape, clip = false)
                        .indication(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false, radius = 20.dp),
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
                    drawStopIndicator = null,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun StepsSliderSample() {
    val sliderState =
        rememberSliderState(
            // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
            // there are 9 steps (10, 20, ..., 90).
            steps = 9,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderState.value))
        Slider(state = sliderState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SliderWithCustomThumbSample() {
    var sliderPosition by rememberSaveable { mutableStateOf(0f) }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Slider(
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
                    interactionSource = interactionSource,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                        tint = Color.Red,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun SliderWithCustomTrackAndThumbSample() {
    val sliderState =
        rememberSliderState(
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderState.value))
        Slider(
            state = sliderState,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(interactionSource = interactionSource, colors = colors)
            },
            track = { SliderDefaults.Track(colors = colors, sliderState = sliderState) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun SliderWithTrackIconsSample() {
    val sliderState =
        rememberSliderState(
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    val interactionSource = remember { MutableInteractionSource() }
    val startIcon = rememberVectorPainter(Icons.Filled.MusicNote)
    val endIcon = rememberVectorPainter(Icons.Filled.MusicOff)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderState.value))
        Slider(
            state = sliderState,
            interactionSource = interactionSource,
            track = {
                val iconSize = DpSize(20.dp, 20.dp)
                val iconPadding = 10.dp
                val thumbTrackGapSize = 6.dp
                val activeIconColor = SliderDefaults.colors().activeTickColor
                val inactiveIconColor = SliderDefaults.colors().inactiveTickColor
                val trackIconStart: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                    translate(offset.x + iconPadding.toPx(), offset.y) {
                        with(startIcon) {
                            draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                        }
                    }
                }
                val trackIconEnd: DrawScope.(Offset, Color) -> Unit = { offset, color ->
                    translate(offset.x - iconPadding.toPx() - iconSize.toSize().width, offset.y) {
                        with(endIcon) {
                            draw(iconSize.toSize(), colorFilter = ColorFilter.tint(color))
                        }
                    }
                }
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier =
                        Modifier.height(36.dp).drawWithContent {
                            drawContent()

                            val yOffset = size.height / 2 - iconSize.toSize().height / 2
                            val activeTrackStart = 0f
                            val activeTrackEnd =
                                size.width * sliderState.coercedValueAsFraction -
                                    thumbTrackGapSize.toPx()
                            val inactiveTrackStart = activeTrackEnd + thumbTrackGapSize.toPx() * 2
                            val inactiveTrackEnd = size.width

                            val activeTrackWidth = activeTrackEnd - activeTrackStart
                            val inactiveTrackWidth = inactiveTrackEnd - inactiveTrackStart
                            if (
                                iconSize.toSize().width < activeTrackWidth - iconPadding.toPx() * 2
                            ) {
                                trackIconStart(Offset(activeTrackStart, yOffset), activeIconColor)
                                trackIconEnd(Offset(activeTrackEnd, yOffset), activeIconColor)
                            }
                            if (
                                iconSize.toSize().width <
                                    inactiveTrackWidth - iconPadding.toPx() * 2
                            ) {
                                trackIconStart(
                                    Offset(inactiveTrackStart, yOffset),
                                    inactiveIconColor,
                                )
                                trackIconEnd(Offset(inactiveTrackEnd, yOffset), inactiveIconColor)
                            }
                        },
                    trackCornerSize = 12.dp,
                    drawStopIndicator = null,
                    thumbTrackGapSize = thumbTrackGapSize,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun CenteredSliderSample() {
    val sliderState =
        rememberSliderState(
            valueRange = -50f..50f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(text = "%.2f".format(sliderState.value))
        Slider(
            state = sliderState,
            interactionSource = interactionSource,
            thumb = { SliderDefaults.Thumb(interactionSource = interactionSource) },
            track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun VerticalSliderSample() {
    val coroutineScope = rememberCoroutineScope()
    val sliderState =
        rememberSliderState(
            // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
            // there are 9 steps (10, 20, ..., 90).
            steps = 9,
            valueRange = 0f..100f,
        )
    val snapAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    var currentValue by rememberSaveable { mutableFloatStateOf(sliderState.value) }
    var animateJob: Job? by remember { mutableStateOf(null) }
    sliderState.shouldAutoSnap = false
    sliderState.onValueChange = { newValue ->
        currentValue = newValue
        // only update the sliderState instantly if dragging
        if (sliderState.isDragging) {
            animateJob?.cancel()
            sliderState.value = newValue
        }
    }
    sliderState.onValueChangeFinished = {
        animateJob =
            coroutineScope.launch {
                animate(
                    initialValue = sliderState.value,
                    targetValue = currentValue,
                    animationSpec = snapAnimationSpec,
                ) { value, _ ->
                    sliderState.value = value
                }
            }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "%.2f".format(sliderState.value),
        )
        Spacer(Modifier.height(16.dp))
        VerticalSlider(
            state = sliderState,
            modifier =
                Modifier.height(300.dp)
                    .align(Alignment.CenterHorizontally)
                    .progressSemantics(
                        currentValue,
                        sliderState.valueRange.start..sliderState.valueRange.endInclusive,
                        sliderState.steps,
                    ),
            interactionSource = interactionSource,
            track = {
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.width(36.dp),
                    trackCornerSize = 12.dp,
                )
            },
            reverseDirection = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Sampled
@Composable
fun VerticalCenteredSliderSample() {
    val coroutineScope = rememberCoroutineScope()
    val sliderState =
        rememberSliderState(
            // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
            // there are 9 steps (10, 20, ..., 90).
            steps = 9,
            valueRange = -50f..50f,
        )
    val snapAnimationSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    var currentValue by rememberSaveable { mutableFloatStateOf(sliderState.value) }
    var animateJob: Job? by remember { mutableStateOf(null) }
    sliderState.shouldAutoSnap = false
    sliderState.onValueChange = { newValue ->
        currentValue = newValue
        // only update the sliderState instantly if dragging
        if (sliderState.isDragging) {
            animateJob?.cancel()
            sliderState.value = newValue
        }
    }
    sliderState.onValueChangeFinished = {
        animateJob =
            coroutineScope.launch {
                animate(
                    initialValue = sliderState.value,
                    targetValue = currentValue,
                    animationSpec = snapAnimationSpec,
                ) { value, _ ->
                    sliderState.value = value
                }
            }
    }
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = "%.2f".format(sliderState.value),
        )
        Spacer(Modifier.height(16.dp))
        VerticalSlider(
            state = sliderState,
            modifier =
                Modifier.height(300.dp)
                    .align(Alignment.CenterHorizontally)
                    .progressSemantics(
                        currentValue,
                        sliderState.valueRange.start..sliderState.valueRange.endInclusive,
                        sliderState.steps,
                    ),
            interactionSource = interactionSource,
            track = { SliderDefaults.CenteredTrack(sliderState = sliderState) },
            reverseDirection = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun RangeSliderSample() {
    val rangeSliderState =
        rememberRangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val rangeStart = "%.2f".format(rangeSliderState.activeRangeStart)
        val rangeEnd = "%.2f".format(rangeSliderState.activeRangeEnd)
        Text(text = "$rangeStart .. $rangeEnd")
        RangeSlider(state = rangeSliderState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun LegacyRangeSliderSample() {
    val rangeSliderState =
        rememberRangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
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
            modifier = Modifier.requiredSizeIn(minWidth = thumbSize.width, minHeight = trackHeight),
            startThumb = {
                val modifier =
                    Modifier.size(thumbSize)
                        .shadow(1.dp, CircleShape, clip = false)
                        .indication(
                            interactionSource = startInteractionSource,
                            indication = ripple(bounded = false, radius = 20.dp),
                        )
                SliderDefaults.Thumb(
                    interactionSource = startInteractionSource,
                    modifier = modifier,
                )
            },
            endThumb = {
                val modifier =
                    Modifier.size(thumbSize)
                        .shadow(1.dp, CircleShape, clip = false)
                        .indication(
                            interactionSource = endInteractionSource,
                            indication = ripple(bounded = false, radius = 20.dp),
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
                    drawStopIndicator = null,
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun StepRangeSliderSample() {
    val rangeSliderState =
        rememberRangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
            // Only allow multiples of 10. Excluding the endpoints of `valueRange`,
            // there are 9 steps (10, 20, ..., 90).
            steps = 9,
        )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        val rangeStart = rangeSliderState.activeRangeStart.roundToInt()
        val rangeEnd = rangeSliderState.activeRangeEnd.roundToInt()
        Text(text = "$rangeStart .. $rangeEnd")
        RangeSlider(state = rangeSliderState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun RangeSliderWithCustomComponents() {
    val rangeSliderState =
        rememberRangeSliderState(
            0f,
            100f,
            valueRange = 0f..100f,
            onValueChangeFinished = {
                // launch some business logic update with the state you hold
                // viewModel.updateSelectedSliderValue(sliderPosition)
            },
        )
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    val startThumbAndTrackColors =
        SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Red)
    val endThumbColors = SliderDefaults.colors(thumbColor = Color.Green)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        RangeSlider(
            state = rangeSliderState,
            startInteractionSource = startInteractionSource,
            endInteractionSource = endInteractionSource,
            startThumb = {
                Label(
                    label = {
                        PlainTooltip(modifier = Modifier.sizeIn(45.dp, 25.dp).wrapContentWidth()) {
                            Text("%.2f".format(rangeSliderState.activeRangeStart))
                        }
                    },
                    interactionSource = startInteractionSource,
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = startInteractionSource,
                        colors = startThumbAndTrackColors,
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
                    interactionSource = endInteractionSource,
                ) {
                    SliderDefaults.Thumb(
                        interactionSource = endInteractionSource,
                        colors = endThumbColors,
                    )
                }
            },
            track = { rangeSliderState ->
                SliderDefaults.Track(
                    colors = startThumbAndTrackColors,
                    rangeSliderState = rangeSliderState,
                )
            },
        )
    }
}
