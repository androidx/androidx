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

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text

@Composable
fun CustomizedHorizontalPageIndicator() {
    val maxPages = 6
    var selectedPage by remember { mutableIntStateOf(0) }
    val animatedSelectedPage by
        animateFloatAsState(
            targetValue = selectedPage.toFloat(),
            animationSpec = TweenSpec(durationMillis = 500),
            label = "page-indicator"
        )

    val pageIndicatorState: PageIndicatorState = remember {
        object : PageIndicatorState {
            override val pageOffset: Float
                get() = animatedSelectedPage - selectedPage

            override val selectedPage: Int
                get() = selectedPage

            override val pageCount: Int
                get() = maxPages
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
        DefaultInlineSlider(
            modifier = Modifier.align(Alignment.Center),
            value = selectedPage,
            segmented = true,
            valueProgression = 0 until maxPages,
            onValueChange = { selectedPage = it }
        )
        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState,
            selectedColor = MaterialTheme.colors.secondary,
            unselectedColor = MaterialTheme.colors.onSecondary,
            indicatorSize = 15.dp,
            indicatorShape = TriangleShape,
            spacing = 8.dp
        )
    }
}

@Composable
fun PagerWithIndicator(swipeState: SwipeToDismissBoxState) {
    val pagesCount = 3
    val pagerState = rememberPagerState { pagesCount }
    var background by remember { mutableStateOf(Color.Black) }
    var selectedPage by remember { mutableIntStateOf(0) }
    val animatedSelectedPage by
        animateFloatAsState(
            targetValue = pagerState.targetPage.toFloat(),
            label = "page-indicator",
        ) {
            selectedPage = it.toInt()
        }

    val pageIndicatorState: PageIndicatorState = remember {
        object : PageIndicatorState {
            override val pageOffset: Float
                get() = animatedSelectedPage - selectedPage

            override val selectedPage: Int
                get() = selectedPage

            override val pageCount: Int
                get() = pagesCount
        }
    }

    Box(modifier = Modifier.background(background)) {
        HorizontalPager(
            modifier = Modifier.fillMaxSize().edgeSwipeToDismiss(swipeState),
            state = pagerState,
            flingBehavior =
                PagerDefaults.flingBehavior(
                    pagerState,
                    snapAnimationSpec = tween(150, 0),
                )
        ) { page ->
            val scrollState = rememberScalingLazyListState()
            ScalingLazyColumn(state = scrollState, modifier = Modifier.fillMaxWidth()) {
                item { ListHeader { Text("Page $page") } }
                items(4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Chip(
                            onClick = {
                                background =
                                    if (background == Color.Black) Color.DarkGray else Color.Black
                            },
                            label = { Text(text = "Click", color = Color.Black) },
                            enabled = !pagerState.isScrollInProgress
                        )
                    }
                }
            }
        }
        HorizontalPageIndicator(
            pageIndicatorState = pageIndicatorState,
        )
    }
}

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
}
