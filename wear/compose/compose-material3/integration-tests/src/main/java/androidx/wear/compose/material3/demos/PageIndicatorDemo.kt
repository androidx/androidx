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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPageIndicator
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorWithPagerSample
import androidx.wear.compose.material3.samples.VerticalPageIndicatorWithPagerSample

val PageIndicatorDemos =
    listOf(
        ComposableDemo("Horizontal PageIndicator") { HorizontalPageIndicatorWithPagerSample() },
        ComposableDemo("Vertical PageIndicator") { VerticalPageIndicatorWithPagerSample() },
        ComposableDemo("Vertical pager on left") { VerticalPageIndicatorWithPagerOnLeftSample() },
        ComposableDemo("Horizontal with white background") {
            HorizontalPageIndicatorWhiteBackgroundDemo()
        },
    )

@Composable
fun HorizontalPageIndicatorWhiteBackgroundDemo() {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    Box(modifier = Modifier.background(Color.White)) {
        HorizontalPager(
            state = pagerState,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "Page #$page",
                    color = Color.Black
                )
            }
        }
        HorizontalPageIndicator(pagerState = pagerState)
    }
}

@Composable
fun VerticalPageIndicatorWithPagerOnLeftSample() {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box {
            VerticalPager(
                state = pagerState,
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(modifier = Modifier.align(Alignment.Center), text = "Page #$page")
                }
            }
            VerticalPageIndicator(pagerState = pagerState)
        }
    }
}
