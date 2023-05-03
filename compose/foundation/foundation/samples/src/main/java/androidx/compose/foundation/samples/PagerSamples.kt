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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SimpleHorizontalPagerSample() {
    // Creates a 1-pager/viewport horizontal pager with single page snapping
    val state = rememberPagerState { 10 }
    HorizontalPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun SimpleVerticalPagerSample() {
    // Creates a 1-pager/viewport vertical pager with single page snapping
    val state = rememberPagerState { 10 }
    VerticalPager(
        state = state,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun PagerWithStateSample() {
    // You can use PagerState to define an initial page
    val state = rememberPagerState(initialPage = 5) { 10 }
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = state
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun CustomPageSizeSample() {

    // [PageSize] should be defined as a top level constant in order to avoid unnecessary re-
    // creations.
    val CustomPageSize = object : PageSize {
        override fun Density.calculateMainAxisPageSize(
            availableSpace: Int,
            pageSpacing: Int
        ): Int {
            // [availableSpace] represents the whole Pager width (in this case), we'd like to have
            // 3 pages per viewport, so we divide it by 3, taking into consideration the start
            // and end spacings.
            return (availableSpace - 2 * pageSpacing) / 3
        }
    }

    val state = rememberPagerState { 10 }
    HorizontalPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
        pageSize = CustomPageSize
    ) { page ->
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Blue)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(text = page.toString(), fontSize = 32.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun ObservingStateChangesInPagerStateSample() {
    val pagerState = rememberPagerState { 10 }
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Blue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.toString(), fontSize = 32.sp)
            }
        }
        Column(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth()
        ) {
            Text(text = "Current Page: ${pagerState.currentPage}")
            Text(text = "Target Page: ${pagerState.targetPage}")
            Text(text = "Settled Page Offset: ${pagerState.settledPage}")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun AnimateScrollPageSample() {
    val state = rememberPagerState { 10 }
    val animationScope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier = Modifier.weight(0.7f),
            state = state
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Blue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.toString(), fontSize = 32.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Button(onClick = {
                animationScope.launch {
                    state.animateScrollToPage(state.currentPage + 1)
                }
            }) {
                Text(text = "Next Page")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun ScrollToPageSample() {
    val state = rememberPagerState { 10 }
    val scrollScope = rememberCoroutineScope()
    Column {
        HorizontalPager(
            modifier = Modifier.height(400.dp),
            state = state
        ) { page ->
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .background(Color.Blue)
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.toString(), fontSize = 32.sp)
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.material.Button(onClick = {
                scrollScope.launch {
                    state.scrollToPage(state.currentPage + 1)
                }
            }) {
                Text(text = "Next Page")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Sampled
@Composable
fun HorizontalPagerWithScrollableContent() {
    // This is a sample using NestedScroll and Pager.
    // We use the toolbar offset changing example from
    // androidx.compose.ui.samples.NestedScrollConnectionSample

    val pagerState = rememberPagerState { 10 }

    val toolbarHeight = 48.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        TopAppBar(
            modifier = Modifier
                .height(toolbarHeight)
                .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
            title = { Text("Toolbar offset is ${toolbarOffsetHeightPx.value}") }
        )

        val paddingOffset =
            toolbarHeight + with(LocalDensity.current) { toolbarOffsetHeightPx.value.toDp() }

        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = pagerState,
            contentPadding = PaddingValues(top = paddingOffset)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                repeat(20) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(4.dp)
                            .background(if (it % 2 == 0) Color.Black else Color.Yellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it.toString(),
                            color = if (it % 2 != 0) Color.Black else Color.Yellow
                        )
                    }
                }
            }
        }
    }
}