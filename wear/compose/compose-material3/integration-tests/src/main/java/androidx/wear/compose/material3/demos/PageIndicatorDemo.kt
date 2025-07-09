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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.samples.HorizontalPageIndicatorWithPagerSample
import androidx.wear.compose.material3.samples.VerticalPageIndicatorWithPagerSample

val PageIndicatorDemos =
    listOf(
        ComposableDemo("Horizontal Page Indicator") {
            HorizontalPageIndicatorWithPagerSample(it.navigateBack)
        },
        ComposableDemo("Horizontal with fade out") {
            HorizontalPageIndicatorWithFadeOutDemo(it.navigateBack)
        },
        ComposableDemo("Horizontal with white background") {
            HorizontalPageIndicatorWhiteBackgroundDemo(it.navigateBack)
        },
        ComposableDemo("Vertical Page Indicator") { VerticalPageIndicatorWithPagerSample() },
        ComposableDemo("Vertical pager on left") { VerticalPageIndicatorWithPagerOnLeftDemo() },
    )

@Composable
fun HorizontalPageIndicatorWithFadeOutDemo(navigateBack: () -> Unit) {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    Box {
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
                    DemoPageContent(page = page, navigateBack = navigateBack)
                }
            }
        }
    }
}

@Composable
fun HorizontalPageIndicatorWhiteBackgroundDemo(navigateBack: () -> Unit) {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    Box(modifier = Modifier.background(Color.White)) {
        HorizontalPager(state = pagerState) { page ->
            DemoPageContent(page, Color.Black, navigateBack)
        }
        HorizontalPageIndicator(pagerState = pagerState)
    }
}

@Composable
fun VerticalPageIndicatorWithPagerOnLeftDemo() {
    AppScaffold {
        val pageCount = 9
        val pagerState = rememberPagerState { pageCount }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            VerticalPagerScaffold(
                pagerState = pagerState,
                pageIndicatorAnimationSpec = PagerScaffoldDefaults.FadeOutAnimationSpec,
            ) {
                VerticalPager(state = pagerState) { page ->
                    AnimatedPage(pageIndex = page, pagerState = pagerState) {
                        ScreenScaffold { DemoPageContent(page) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoPageContent(
    page: Int,
    textColor: Color = Color.White,
    navigateBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Page #$page", color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Swipe left and right", color = textColor)
        if (page == 0 && navigateBack != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = navigateBack) { Text("Exit") }
        }
    }
}
