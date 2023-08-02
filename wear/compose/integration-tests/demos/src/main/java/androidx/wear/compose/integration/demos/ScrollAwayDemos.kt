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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.scrollAway

@Composable
fun ScrollAwayColumnDemo() { ColumnCardDemo(0.dp) }

@Composable
fun ScrollAwayColumnDelayDemo() { ColumnCardDemo(offset = 20.dp) }

@Composable
fun ScrollAwayLazyColumnDemo() {
    LazyColumnCardDemo(offset = 0.dp, itemIndex = 0, initialVisibleItemIndex = 0)
}

@Composable
fun ScrollAwayLazyColumnDemo2() {
    LazyColumnCardDemo(
        offset = -100.dp,
        itemIndex = 1,
        initialVisibleItemIndex = 2
    )
}

@Composable
fun ScrollAwayLazyColumnDelayDemo() {
    LazyColumnCardDemo(offset = 20.dp, itemIndex = 0, initialVisibleItemIndex = 0)
}

@Composable
fun ScrollAwayScalingLazyColumnCardDemo() {
    ScalingLazyColumnCardDemo(
        itemIndex = 1,
        offset = 0.dp,
        initialCenterItemIndex = 1,
    )
}

@Composable
fun ScrollAwayScalingLazyColumnCardDemo2() {
    ScalingLazyColumnCardDemo(
        itemIndex = 2,
        offset = -95.dp,
        initialCenterItemIndex = 2,
    )
}

@Composable
fun ScrollAwayScalingLazyColumnCardDemoMismatch() {
    ScalingLazyColumnCardDemo(
        itemIndex = 0,
        offset = 75.dp,
        initialCenterItemIndex = 1,
    )
}

@Composable
fun ScrollAwayScalingLazyColumnCardDemoOutOfRange() {
    ScalingLazyColumnCardDemo(
        itemIndex = 100,
        offset = 75.dp,
        initialCenterItemIndex = 1,
    )
}

@Composable
fun ScrollAwayScalingLazyColumnChipDemo() {
    ScalingLazyColumnChipDemo(
        itemIndex = 1,
        offset = 10.dp,
        initialCenterItemIndex = 1,
    )
}

@Composable
fun ScrollAwayScalingLazyColumnChipDemo2() {
    ScalingLazyColumnChipDemo(
        itemIndex = 2,
        offset = -50.dp,
        initialCenterItemIndex = 2,
    )
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun ColumnCardDemo(offset: Dp) {
    val scrollState = rememberScrollState()
    val focusRequester = rememberActiveFocusRequester()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {
            TimeText(
                modifier = Modifier.scrollAway(
                    scrollState = scrollState,
                    offset = offset,
                )
            )
        },
        positionIndicator = {
            PositionIndicator(scrollState = scrollState)
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .rsbScroll(
                    scrollableState = scrollState,
                    flingBehavior = ScrollableDefaults.flingBehavior(),
                    focusRequester = focusRequester
                )
        ) {
            val modifier = Modifier.height(LocalConfiguration.current.screenHeightDp.dp / 2)
            repeat(10) { i ->
                ExampleCard(modifier, i)
            }
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun LazyColumnCardDemo(offset: Dp, itemIndex: Int, initialVisibleItemIndex: Int) {
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = initialVisibleItemIndex)
    val focusRequester = rememberActiveFocusRequester()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {
            TimeText(modifier = Modifier.scrollAway(
                scrollState = scrollState,
                offset = offset,
                itemIndex = itemIndex
            ))
        },
        positionIndicator = {
            PositionIndicator(lazyListState = scrollState)
        }
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.rsbScroll(
                scrollableState = scrollState,
                flingBehavior = ScrollableDefaults.flingBehavior(),
                focusRequester = focusRequester
            )
        ) {
            items(10) { i ->
                val modifier = Modifier.fillParentMaxHeight(0.5f)
                ExampleCard(modifier = modifier, i = i)
            }
        }
    }
}

@Composable
private fun ScalingLazyColumnCardDemo(
    offset: Dp,
    itemIndex: Int,
    initialCenterItemIndex: Int,
) {
    val scrollState =
        rememberScalingLazyListState(
            initialCenterItemIndex = initialCenterItemIndex,
            initialCenterItemScrollOffset = 0
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {
            TimeText(modifier =
            Modifier.scrollAway(
                scrollState = scrollState,
                itemIndex = itemIndex,
                offset = offset,
            ))
        },
        positionIndicator = {
            PositionIndicator(scalingLazyListState = scrollState)
        }
    ) {
        ScalingLazyColumnWithRSB(
            contentPadding = PaddingValues(10.dp),
            state = scrollState,
            autoCentering = AutoCenteringParams(itemIndex = 1, itemOffset = 0)
        ) {
            item {
                ListHeader { Text("Cards") }
            }

            items(10) { i ->
                ExampleCard(Modifier.fillParentMaxHeight(0.5f), i)
            }
        }
    }
}

@Composable
private fun ScalingLazyColumnChipDemo(
    offset: Dp,
    itemIndex: Int,
    initialCenterItemIndex: Int,
) {
    val scrollState =
        rememberScalingLazyListState(
            initialCenterItemIndex = initialCenterItemIndex,
            initialCenterItemScrollOffset = 0
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        timeText = {
            TimeText(modifier =
            Modifier.scrollAway(
                scrollState = scrollState,
                itemIndex = itemIndex,
                offset = offset,
            ))
        },
        positionIndicator = {
            PositionIndicator(scalingLazyListState = scrollState)
        }
    ) {
        ScalingLazyColumnWithRSB(
            contentPadding = PaddingValues(10.dp),
            state = scrollState,
        ) {
            item {
                ListHeader { Text("Chips") }
            }

            items(10) { i ->
                ExampleChip(Modifier.fillMaxWidth(), i)
            }
        }
    }
}

@Composable
private fun ExampleCard(modifier: Modifier, i: Int) {
    Card(
        modifier = modifier,
        onClick = { }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Card $i")
        }
    }
}

@Composable
private fun ExampleChip(modifier: Modifier, i: Int) {
    Chip(
        modifier = modifier,
        onClick = { },
        colors = ChipDefaults.primaryChipColors(),
        border = ChipDefaults.chipBorder()
    ) {
        Text(text = "Chip $i")
    }
}
