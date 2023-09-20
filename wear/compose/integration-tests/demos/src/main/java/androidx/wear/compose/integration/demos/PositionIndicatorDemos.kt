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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.PositionIndicatorAlignment
import androidx.wear.compose.material.PositionIndicatorState
import androidx.wear.compose.material.PositionIndicatorVisibility
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton

@Composable
fun HideWhenFullDemo() {
    var smallList by remember { mutableStateOf(true) }
    val listState = rememberScrollState()

    Scaffold(
        positionIndicator = { PositionIndicator(scrollState = listState) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .verticalScroll(listState)
        ) {
            repeat(if (smallList) 3 else 10) {
                Chip(
                    onClick = { smallList = !smallList },
                    label = { Text("Item #$it") }
                )
            }
        }
    }
}

@Composable
fun HideWhenFullSLCDemo() {
    var smallList by remember { mutableStateOf(true) }
    val listState = rememberScalingLazyListState()
    Scaffold(
        positionIndicator = {
            PositionIndicator(
                scalingLazyListState = listState,
                modifier = Modifier
            )
        }
    ) {
        ScalingLazyColumn(
            state = listState,
            autoCentering = null
        ) {
            items(
                count = if (smallList) 3 else 10
            ) {
                Chip(
                    onClick = { smallList = !smallList },
                    label = { Text("Item #$it") }
                )
            }
        }
    }
}

@Composable
fun ControllablePositionIndicator() {
    val position = remember { mutableFloatStateOf(0.2f) }
    val size = remember { mutableFloatStateOf(0.5f) }
    val visibility = remember { mutableStateOf(PositionIndicatorVisibility.Show) }
    var alignment by remember { mutableIntStateOf(0) }
    var reverseDirection by remember { mutableStateOf(false) }
    var layoutDirection by remember { mutableStateOf(false) }
    val actualLayoutDirection =
        if (layoutDirection) LayoutDirection.Rtl
        else LayoutDirection.Ltr
    val alignmentValues = listOf(
        PositionIndicatorAlignment.End,
        PositionIndicatorAlignment.OppositeRsb,
        PositionIndicatorAlignment.Left,
        PositionIndicatorAlignment.Right
    )
    val alignmentNames = listOf("End", "!Rsb", "Left", "Right")
    CompositionLocalProvider(LocalLayoutDirection provides actualLayoutDirection) {
        Scaffold(
            positionIndicator = {
                PositionIndicator(
                    state = CustomPositionIndicatorState(position, size, visibility),
                    indicatorHeight = 76.dp,
                    indicatorWidth = 6.dp,
                    paddingHorizontal = 5.dp,
                    color = MaterialTheme.colors.secondary,
                    reverseDirection = reverseDirection,
                    position = alignmentValues[alignment]
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Position")
                    DefaultInlineSlider(
                        modifier = Modifier.height(40.dp),
                        value = position.floatValue,
                        valueRange = 0f..1f,
                        steps = 9,
                        onValueChange = { position.floatValue = it })
                    Text("Size")
                    DefaultInlineSlider(
                        modifier = Modifier.height(40.dp),
                        value = size.floatValue,
                        valueRange = 0f..1f,
                        steps = 9,
                        onValueChange = { size.floatValue = it })
                    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Button(onClick = { alignment = (alignment + 1) % 3 }) {
                            Text(alignmentNames[alignment])
                        }
                        ToggleButton(
                            checked = layoutDirection,
                            onCheckedChange = { layoutDirection = !layoutDirection }
                        ) { Text(if (layoutDirection) "Rtl" else "Ltr") }
                        ToggleButton(
                            checked = reverseDirection,
                            onCheckedChange = { reverseDirection = !reverseDirection }
                        ) {
                            Text(
                                text = "Rev Dir",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Button(onClick = {
                        visibility.value = when (visibility.value) {
                            PositionIndicatorVisibility.Show ->
                                PositionIndicatorVisibility.AutoHide
                            PositionIndicatorVisibility.AutoHide ->
                                PositionIndicatorVisibility.Hide
                            else ->
                                PositionIndicatorVisibility.Show
                        }
                    }) {
                        Text(
                            when (visibility.value) {
                                PositionIndicatorVisibility.AutoHide -> "AutoHide"
                                PositionIndicatorVisibility.Show -> "Show"
                                else -> "Hide"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SharedPositionIndicator() {
    val listStates = listOf(rememberScrollState(), rememberScrollState())
    val selected = remember { mutableIntStateOf(0) }
    Scaffold(
        positionIndicator = {
            PositionIndicator(listStates[selected.intValue])
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            repeat(2) { listIndex ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp)
                        .verticalScroll(listStates[listIndex])
                ) {
                    repeat(10) {
                        Chip(
                            onClick = { selected.intValue = listIndex },
                            label = { Text("#$it") }
                        )
                    }
                }
            }
        }
    }
}

internal class CustomPositionIndicatorState(
    private val position: State<Float>,
    private val size: State<Float>,
    private val visibility: State<PositionIndicatorVisibility>
) : PositionIndicatorState {
    override val positionFraction get() = position.value
    override fun sizeFraction(scrollableContainerSizePx: Float) = size.value
    override fun visibility(scrollableContainerSizePx: Float) = visibility.value

    override fun equals(other: Any?) =
        other is CustomPositionIndicatorState &&
            position == other.position &&
            size == other.size &&
            visibility == other.visibility

    override fun hashCode(): Int = position.hashCode() +
        31 * size.hashCode() +
        31 * visibility.hashCode()
}
