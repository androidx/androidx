/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.delay

@Sampled
@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun PullToRefreshSample() {
    var itemCount by remember { mutableStateOf(15) }
    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            // fetch something
            delay(1500)
            itemCount += 5
            state.endRefresh()
        }
    }
    Box(Modifier.nestedScroll(state.nestedScrollConnection)) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.isRefreshing) {
                items(itemCount) {
                    ListItem({ Text(text = "Item ${itemCount - it}") })
                }
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )
    }
}

@Sampled
@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun PullToRefreshScalingSample() {
    var itemCount by remember { mutableStateOf(15) }
    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            // fetch something
            delay(1500)
            itemCount += 5
            state.endRefresh()
        }
    }
    val scaleFraction = if (state.isRefreshing) 1f else
        LinearOutSlowInEasing.transform(state.progress).coerceIn(0f, 1f)

    Box(Modifier.nestedScroll(state.nestedScrollConnection)) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.isRefreshing) {
                items(itemCount) {
                    ListItem({ Text(text = "Item ${itemCount - it}") })
                }
            }
        }
        PullToRefreshContainer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer(scaleX = scaleFraction, scaleY = scaleFraction),
            state = state,
        )
    }
}

@Sampled
@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun PullToRefreshLinearProgressIndicatorSample() {
    var itemCount by remember { mutableStateOf(15) }
    val state = rememberPullToRefreshState()
    if (state.isRefreshing) {
        LaunchedEffect(true) {
            // fetch something
            delay(1500)
            itemCount += 5
            state.endRefresh()
        }
    }
    Box(Modifier.nestedScroll(state.nestedScrollConnection)) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.isRefreshing) {
                items(itemCount) {
                    ListItem({ Text(text = "Item ${itemCount - it}") })
                }
            }
        }
        if (state.isRefreshing) {
            LinearProgressIndicator()
        } else {
            LinearProgressIndicator(progress = { state.progress })
        }
    }
}

@Sampled
@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun PullToRefreshSampleCustomState() {
    var itemCount by remember { mutableStateOf(15) }
    val state = remember {
        object : PullToRefreshState {
            override val positionalThreshold: Float = 100f
            override val progress get() = verticalOffset / positionalThreshold
            override var verticalOffset: Float by mutableFloatStateOf(0f)
            override var isRefreshing: Boolean by mutableStateOf(false)

            override fun startRefresh() {
                isRefreshing = true
            }
            override fun endRefresh() {
                isRefreshing = false
            }

            // Provide logic for the PullRefreshContainer to consume scrolls within a nested scroll
            override var nestedScrollConnection: NestedScrollConnection =
                object : NestedScrollConnection {
                    // Pre and post scroll provide the drag logic for PullRefreshContainer.
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset = when {
                        source == NestedScrollSource.Drag && available.y < 0 -> {
                            // Swiping up
                            val y = if (isRefreshing) 0f else {
                                val newOffset = (verticalOffset + available.y).coerceAtLeast(0f)
                                val dragConsumed = newOffset - verticalOffset
                                verticalOffset = newOffset
                                dragConsumed
                            }
                            Offset(0f, y)
                        }

                        else -> Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset = when {
                        source == NestedScrollSource.Drag && available.y > 0 -> {
                            // Swiping Down
                            val y = if (isRefreshing) 0f else {
                                val newOffset = (verticalOffset + available.y).coerceAtLeast(0f)
                                val dragConsumed = newOffset - verticalOffset
                                verticalOffset = newOffset
                                dragConsumed
                            }
                            Offset(0f, y)
                        }

                        else -> Offset.Zero
                    }

                    // Pre-Fling is called when the user releases a drag. This is where you can provide
                    // refresh logic, and verify exceeding positional threshold.
                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (isRefreshing) return Velocity.Zero
                        if (verticalOffset > positionalThreshold) {
                            startRefresh()
                            itemCount += 5
                            endRefresh()
                        }
                        animate(verticalOffset, 0f) { value, _ ->
                            verticalOffset = value
                        }
                        val consumed = when {
                            verticalOffset == 0f -> 0f
                            available.y < 0f -> 0f
                            else -> available.y
                        }
                        return Velocity(0f, consumed)
                    }
                }
        }
    }

    Box(Modifier.nestedScroll(state.nestedScrollConnection)) {
        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.isRefreshing) {
                items(itemCount) {
                    ListItem({ Text(text = "Item ${itemCount - it}") })
                }
            }
        }
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = state,
        )
    }
}
