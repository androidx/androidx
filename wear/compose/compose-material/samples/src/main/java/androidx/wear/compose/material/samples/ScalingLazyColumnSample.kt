/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyColumnDefaults
import androidx.wear.compose.material.ScalingLazyListAnchorType
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SimpleScalingLazyColumn() {
    ScalingLazyColumn {
        item {
            ListHeader {
                Text(text = "List Header")
            }
        }
        items(20) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Sampled
@Composable
fun SimpleScalingLazyColumnWithSnap() {
    val state = rememberScalingLazyListState()
    ScalingLazyColumn(
        state = state,
        flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = state)
    ) {
        item {
            ListHeader {
                Text(text = "List Header")
            }
        }
        items(20) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Sampled
@Composable
fun ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo() {
    val coroutineScope = rememberCoroutineScope()
    val itemSpacing = 6.dp
    // Line up the gap between the items on the center-line
    val scrollOffset = with(LocalDensity.current) {
        -(itemSpacing / 2).roundToPx()
    }
    val state = rememberScalingLazyListState(
        initialCenterItemIndex = 1,
        initialCenterItemScrollOffset = scrollOffset
    )

    ScalingLazyColumn(
        anchorType = ScalingLazyListAnchorType.ItemStart,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        state = state
    ) {
        item {
            ListHeader {
                Text(text = "List Header")
            }
        }
        items(20) {
            Chip(
                onClick = {
                    coroutineScope.launch {
                        state.animateScrollToItem(it, scrollOffset)
                    }
                },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Sampled
@Composable
fun SimpleScalingLazyColumnWithContentPadding() {
    ScalingLazyColumn(
        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
        autoCentering = false
    ) {
        item {
            ListHeader {
                Text(text = "List Header")
            }
        }
        items(20) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}

@Sampled
@Composable
fun ScalingLazyColumnWithHeaders() {
    ScalingLazyColumn {
        item { ListHeader { Text("Header1") } }
        items(5) {
            Chip(
                onClick = { },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
        item { ListHeader { Text("Header2") } }
        items(5) {
            Chip(
                onClick = { },
                label = { Text("List item ${it + 5}") },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
