/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.samples.HorizontalPagerScaffoldSample
import androidx.wear.compose.material3.samples.ScaffoldSample
import androidx.wear.compose.material3.samples.ScaffoldWithSLCEdgeButtonSample
import androidx.wear.compose.material3.samples.ScaffoldWithTLCEdgeButtonSample
import androidx.wear.compose.material3.samples.VerticalPagerScaffoldSample
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val ScaffoldDemos =
    listOf(
        ComposableDemo("Scaffold Sample") { ScaffoldSample() },
        ComposableDemo("Screen Scaffold with SLC") { ScaffoldWithSLCEdgeButtonSample() },
        ComposableDemo("Screen Scaffold Loading SLC") { ScaffoldLoadingSLCEdgeButtonSample() },
        ComposableDemo("Screen Scaffold with TLC") { ScaffoldWithTLCEdgeButtonSample() },
        ComposableDemo("Horizontal Pager Scaffold") {
            HorizontalPagerScaffoldSample(it.navigateBack)
        },
        ComposableDemo("Horizontal Pager Scaffold (Fade Out Indicator)") {
            HorizontalPagerScaffoldFadeOutIndicatorDemo(it.navigateBack)
        },
        ComposableDemo("Vertical Pager Scaffold") { VerticalPagerScaffoldSample() },
        ComposableDemo("Vertical Pager Scaffold (Fade Out Indicator)") {
            VerticalPagerScaffoldFadeOutIndicatorDemo()
        },
        ComposableDemo("Complex Horizontal Pager") { ComplexHorizontalPager(it.navigateBack) },
    )

@Composable
fun RandomComponent(page: Int) {
    when (page % 3) {
        0 -> Button(onClick = {}) { Text("Button") }
        1 ->
            RadioButton(
                label = { Text("Radio Button") },
                selected = true,
                onSelect = {},
                enabled = true,
            )
        2 ->
            DefaultSlider(
                value = 5f,
                enabled = true,
                valueRange = 1f..10f,
                steps = 10,
                onValueChange = {},
            )
    }
}

@Composable
fun HorizontalPagerScaffoldFadeOutIndicatorDemo(navigateBack: () -> Unit) {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        HorizontalPagerScaffold(
            pagerState = pagerState,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
        ) {
            HorizontalPager(
                state = pagerState,
                flingBehavior =
                    PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    ScreenScaffold {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Page $page")
                            Spacer(modifier = Modifier.height(16.dp))
                            if (page == 0) {
                                Button(onClick = navigateBack) { Text("Exit") }
                            } else {
                                RandomComponent(page)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalPagerScaffoldFadeOutIndicatorDemo() {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        VerticalPagerScaffold(
            pagerState = pagerState,
            pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior =
                    PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    ScreenScaffold {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Page $page")
                            Spacer(modifier = Modifier.height(16.dp))
                            RandomComponent(page)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScaffoldLoadingSLCEdgeButtonSample() {
    // Simulate the loading of the UI's content
    val loaded = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        launch {
            delay(2000)
            loaded.value = true
        }
    }

    val loadedListState = rememberScalingLazyListState()
    val unLoadedListState = rememberScalingLazyListState()

    val listState = if (loaded.value) loadedListState else unLoadedListState
    ScreenScaffold(scrollState = listState, timeText = { TimeText() }) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            if (loaded.value) {
                items(10) { Button(onClick = {}, label = { Text("Item ${it + 1}") }) }
            } else {
                item { Text("Loading...") }
            }
        }
    }
}

@Composable
fun ComplexHorizontalPager(navigateBack: () -> Unit) {
    AppScaffold {
        val pageCount = 3
        val pagerState = PagerState(currentPage = 0, currentPageOffsetFraction = 0f) { pageCount }

        HorizontalPagerScaffold(
            pagerState = pagerState,
            pageIndicator = {},
            modifier = Modifier.size(300.dp),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                val scrollState = rememberTransformingLazyColumnState()
                val overscrollEffect = rememberOverscrollEffect()

                ScreenScaffold(
                    scrollState = scrollState,
                    overscrollEffect = overscrollEffect,
                    modifier = Modifier.fillMaxSize(),
                    edgeButton = { EdgeButton(onClick = {}) { Text("Edge Button") } },
                ) { paddingValuesFromScaffold ->
                    TransformingLazyColumn(
                        state = scrollState,
                        contentPadding = paddingValuesFromScaffold,
                        overscrollEffect = overscrollEffect,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(20) { Text("Item #$pageIndex-$it") }
                    }
                }
            }
        }
    }
}
