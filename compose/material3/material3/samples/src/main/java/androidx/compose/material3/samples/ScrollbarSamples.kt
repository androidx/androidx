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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.interactiveScrollbar
import androidx.compose.material3.nonInteractiveScrollbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Preview(showBackground = true)
@Sampled
@Composable
fun NonInteractiveScrollbarWithLazyColumnSample() {
    val state = rememberLazyListState()
    val scrollIndicatorState = state.scrollIndicatorState
    val scrollbarModifier =
        if (scrollIndicatorState != null) {
            Modifier.nonInteractiveScrollbar(
                scrollIndicatorState,
                orientation = Orientation.Vertical,
            )
        } else {
            Modifier
        }

    LazyColumn(state = state, modifier = scrollbarModifier) {
        items(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.fillMaxWidth().height(50.dp).wrapContentSize(Alignment.Center),
            )
        }
    }
}

@Sampled
@Composable
fun NonInteractiveScrollbarWithVerticalScrollSample() {
    val state = rememberScrollState()
    val scrollIndicatorState = state.scrollIndicatorState
    val scrollbarModifier =
        if (scrollIndicatorState != null) {
            Modifier.nonInteractiveScrollbar(
                scrollIndicatorState,
                orientation = Orientation.Vertical,
            )
        } else {
            Modifier
        }

    Column(
        // Chain before verticalScroll so the scrollbar doesn't scroll with content.
        modifier = scrollbarModifier.verticalScroll(state)
    ) {
        repeat(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.fillMaxWidth().height(50.dp).wrapContentSize(Alignment.Center),
            )
        }
    }
}

@Sampled
@Composable
fun InteractiveScrollbarWithVerticalScrollSample() {
    val state = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val scrollIndicatorState = state.scrollIndicatorState
    val scrollToPositionFraction: suspend (Float) -> Unit = { positionFraction ->
        if (scrollIndicatorState != null) {
            val maxScrollOffset =
                scrollIndicatorState.contentSize - scrollIndicatorState.viewportSize
            val targetPx = (positionFraction * maxScrollOffset).roundToInt()
            state.scrollTo(targetPx)
        }
    }
    val scrollbarModifier =
        if (scrollIndicatorState != null) {
            Modifier.interactiveScrollbar(
                state = scrollIndicatorState,
                orientation = Orientation.Vertical,
                onThumbDrag = { coroutineScope.launch { scrollToPositionFraction(it) } },
                onTrackPress = { coroutineScope.launch { scrollToPositionFraction(it) } },
            )
        } else {
            Modifier
        }

    Column(
        // Chain before verticalScroll so the scrollbar doesn't scroll with content.
        modifier = scrollbarModifier.verticalScroll(state)
    ) {
        repeat(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.fillMaxWidth().height(50.dp).wrapContentSize(Alignment.Center),
            )
        }
    }
}

@Sampled
@Composable
fun InteractiveScrollbarWithLazyColumnSample() {
    val state = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollIndicatorState = state.scrollIndicatorState
    val scrollToPositionFraction: suspend (Float) -> Unit = { positionFraction ->
        if (scrollIndicatorState != null) {
            val maxScrollOffset =
                scrollIndicatorState.contentSize - scrollIndicatorState.viewportSize
            val targetPx = positionFraction * maxScrollOffset
            val deltaPx = targetPx - scrollIndicatorState.scrollOffset
            state.scrollBy(deltaPx)
        }
    }
    val scrollbarModifier =
        if (scrollIndicatorState != null) {
            Modifier.interactiveScrollbar(
                state = scrollIndicatorState,
                orientation = Orientation.Vertical,
                onThumbDrag = { coroutineScope.launch { scrollToPositionFraction(it) } },
                onTrackPress = { coroutineScope.launch { scrollToPositionFraction(it) } },
            )
        } else {
            Modifier
        }

    LazyColumn(state = state, modifier = scrollbarModifier) {
        items(100) { index ->
            Text(
                text = "Item $index",
                modifier = Modifier.fillMaxWidth().height(50.dp).wrapContentSize(Alignment.Center),
            )
        }
    }
}
