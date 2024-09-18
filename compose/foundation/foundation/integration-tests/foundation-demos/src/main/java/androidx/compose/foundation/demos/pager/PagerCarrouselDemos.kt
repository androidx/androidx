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

package androidx.compose.foundation.demos.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Carousel
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val Carrousel =
    listOf(
        ComposableDemo("Horizontal") { HorizontalCarrouselDemo() },
        ComposableDemo("Vertical") { VerticalCarrouselDemo() },
        ComposableDemo("3 pages per viewport") { HorizontalCustomPageSizeDemo() },
        ComposableDemo("Max Scroll = 3 pages") {
            HorizontalCustomPageSizeWithCustomMaxScrollDemo()
        },
        ComposableDemo("Always Centered Carousel") { HorizontalCarrouselDemoAlwaysCentered() },
    )

val SnapPositionDemos =
    listOf(
        ComposableDemo("Snap Position - Start") { HorizontalCarrouselDemo(SnapPosition.Start) },
        ComposableDemo("Snap Position - Center") { HorizontalCarrouselDemo(SnapPosition.Center) },
        ComposableDemo("Snap Position - End") { HorizontalCarrouselDemo(SnapPosition.End) },
        ComposableDemo("Snap Position - Custom") {
            HorizontalCarrouselDemoWithCustomSnapPosition()
        },
    )

@Composable
private fun HorizontalCarrouselDemoWithCustomSnapPosition() {
    val pagerState = rememberPagerState { PagesCount }

    val snapPosition = remember {
        object : SnapPosition {
            override fun position(
                layoutSize: Int,
                itemSize: Int,
                beforeContentPadding: Int,
                afterContentPadding: Int,
                itemIndex: Int,
                itemCount: Int
            ): Int {
                val availableLayoutSpace = layoutSize - beforeContentPadding - afterContentPadding
                return when (itemIndex) {
                    0 -> 0
                    itemCount - 1 -> availableLayoutSpace - itemSize
                    else -> availableLayoutSpace / 2 - itemSize / 2
                }
            }
        }
    }

    val pageSize = remember {
        object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int,
                pageSpacing: Int
            ): Int {
                return (availableSpace + pageSpacing) / 2
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageSize = pageSize,
            snapPosition = snapPosition
        ) {
            CarrouselItem(it, Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun HorizontalCarrouselDemoAlwaysCentered() {
    val pagerState = rememberPagerState { PagesCount }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageSize = PageSize.Fixed(100.dp),
            snapPosition = SnapPosition.Center, // center snapping
            contentPadding =
                PaddingValues(start = 200.dp, end = 200.dp) // padding to adjust snap position
        ) {
            val scope = rememberCoroutineScope()

            val goToPage: (Int) -> Unit = { scope.launch { pagerState.animateScrollToPage(it) } }
            if (pagerState.currentPage == it) {
                Box(Modifier.background(Color.Red)) {
                    CarrouselItem(it, Orientation.Vertical, goToPage)
                }
            } else {
                CarrouselItem(it, Orientation.Vertical, goToPage)
            }
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun HorizontalCarrouselDemo(snapPosition: SnapPosition = SnapPosition.Start) {
    val pagerState = rememberPagerState { PagesCount }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            // configures this Pager to work (semantically speaking) as a Carousel (list)
            modifier = Modifier.semantics { role = Carousel },
            state = pagerState,
            pageSize = PageSize.Fixed(100.dp),
            snapPosition = snapPosition
        ) {
            CarrouselItem(it, Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun VerticalCarrouselDemo() {
    val pagerState = rememberPagerState { PagesCount }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        VerticalPager(
            modifier = Modifier.weight(0.9f),
            state = pagerState,
            pageSize = PageSize.Fixed(200.dp)
        ) {
            CarrouselItem(it, Orientation.Horizontal)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun HorizontalCustomPageSizeDemo() {
    val pagerState = rememberPagerState { PagesCount }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageSize = ThreePagesPerViewport,
            pageSpacing = 8.dp
        ) {
            CarrouselItem(index = it, fillOrientation = Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun HorizontalCustomPageSizeWithCustomMaxScrollDemo() {
    val pagerState = rememberPagerState { PagesCount }

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            modifier = Modifier,
            state = pagerState,
            pageSize = ThreePagesPerViewport,
            pageSpacing = 8.dp,
            flingBehavior =
                PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(3)
                )
        ) {
            CarrouselItem(index = it, fillOrientation = Orientation.Vertical)
        }
        PagerControls(Modifier.weight(0.1f), pagerState)
    }
}

@Composable
private fun CarrouselItem(index: Int, fillOrientation: Orientation, onClick: (Int) -> Unit = {}) {
    val fillAxisModifier =
        if (fillOrientation == Orientation.Vertical)
            Modifier.focusable().fillMaxWidth().height(256.dp)
        else Modifier.fillMaxHeight().width(256.dp)
    Box(
        modifier =
            Modifier.then(fillAxisModifier).padding(10.dp).background(Color.Magenta).clickable {
                onClick.invoke(index)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = index.toString(), fontSize = 32.sp)
    }
}

private val ThreePagesPerViewport =
    object : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return (availableSpace - 2 * pageSpacing) / 3
        }
    }
