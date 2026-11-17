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

package androidx.compose.ui.tooling

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

/** Device spec of a phone-sized preview whose vertically scrollable containers are scrolled. */
internal const val SCROLL_TO_END_DEVICE = "spec:width=411dp,height=891dp,scrollTo=end"

/** Number of items of the scrolled lists: they are taller than the screen of any test device. */
internal const val SCROLL_TO_END_ITEM_COUNT = 100

internal val SCROLL_TO_END_ITEM_HEIGHT = 40.dp

/** State of the list of the last composed [ScrollToEndLazyColumnPreview]. */
internal var scrollToEndLazyListState: LazyListState? = null

@Preview(device = SCROLL_TO_END_DEVICE)
@Composable
fun ScrollToEndLazyColumnPreview() {
    val state = rememberLazyListState()
    scrollToEndLazyListState = state
    LazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        items(SCROLL_TO_END_ITEM_COUNT) {
            Text("Item $it", Modifier.fillMaxWidth().height(SCROLL_TO_END_ITEM_HEIGHT))
        }
    }
}

@Preview(device = SCROLL_TO_END_DEVICE)
@Composable
fun ScrollToEndReverseLazyColumnPreview() {
    val state = rememberLazyListState()
    scrollToEndLazyListState = state
    LazyColumn(modifier = Modifier.fillMaxSize(), state = state, reverseLayout = true) {
        items(SCROLL_TO_END_ITEM_COUNT) {
            Text("Item $it", Modifier.fillMaxWidth().height(SCROLL_TO_END_ITEM_HEIGHT))
        }
    }
}

/** State of the column of the last composed [ScrollToEndVerticalScrollColumnPreview]. */
internal var scrollToEndScrollState: ScrollState? = null

@Preview(device = SCROLL_TO_END_DEVICE)
@Composable
fun ScrollToEndVerticalScrollColumnPreview() {
    val state = rememberScrollState()
    scrollToEndScrollState = state
    Column(Modifier.fillMaxSize().verticalScroll(state)) {
        repeat(SCROLL_TO_END_ITEM_COUNT) {
            Text("Item $it", Modifier.fillMaxWidth().height(SCROLL_TO_END_ITEM_HEIGHT))
        }
    }
}

/** State of the list of the last composed [ScrollToEndWearScalingLazyColumnPreview]. */
internal var scrollToEndScalingLazyListState: ScalingLazyListState? = null

/**
 * Value of Wear's [LocalReduceMotion] observed by the last
 * [ScrollToEndWearScalingLazyColumnPreview].
 */
internal var scrollToEndObservedWearReduceMotion: Boolean? = null

@Preview(device = "spec:parent=wearos_large_round,scrollTo=end")
@Composable
fun ScrollToEndWearScalingLazyColumnPreview() {
    scrollToEndObservedWearReduceMotion = LocalReduceMotion.current
    val state = rememberScalingLazyListState()
    scrollToEndScalingLazyListState = state
    ScalingLazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
        items(SCROLL_TO_END_ITEM_COUNT) {
            Box(Modifier.fillMaxWidth().height(SCROLL_TO_END_ITEM_HEIGHT))
        }
    }
}
