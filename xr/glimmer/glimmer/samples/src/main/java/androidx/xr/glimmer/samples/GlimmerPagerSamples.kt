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

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.LocalTextStyle
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.pager.GlimmerHorizontalPager
import androidx.xr.glimmer.pager.GlimmerPagerState
import androidx.xr.glimmer.pager.rememberGlimmerPagerState
import kotlinx.coroutines.launch

@Sampled
@Composable
fun GlimmerHorizontalPagerSample() {
    val pagerState = rememberGlimmerPagerState(pageCount = { 10 })

    GlimmerHorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Page: $page",
                style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
            )
        }
    }
}

@Sampled
@Composable
fun GlimmerPagerStateAnimateScrollToPageSample() {
    val state = rememberGlimmerPagerState(initialPage = 5, pageCount = { 10 })
    val scope = rememberCoroutineScope()
    Column {
        Button(onClick = { scope.launch { state.animateScrollToPage(1) } }) {
            Text(text = "Scroll to Page 1")
        }

        GlimmerHorizontalPager(modifier = Modifier.fillMaxSize(), state = state) { page ->
            Card {
                Text(
                    text = "Page: $page",
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                )
            }
        }
    }
}

@Sampled
@Composable
fun GlimmerPagerStateScrollToPageSample() {
    val state = rememberGlimmerPagerState(initialPage = 5, pageCount = { 10 })
    val scope = rememberCoroutineScope()
    Column {
        Button(onClick = { scope.launch { state.scrollToPage(1) } }) {
            Text(text = "Scroll to Page 1")
        }

        GlimmerHorizontalPager(modifier = Modifier.fillMaxSize(), state = state) { page ->
            Card {
                Text(
                    text = "Page: $page",
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                )
            }
        }
    }
}

@Sampled
@Composable
fun GlimmerPagerStateCustomAnimateScrollToPageSample() {
    suspend fun GlimmerPagerState.customAnimateScrollToPage(page: Int) {
        val preJumpPosition =
            if (page > currentPage) {
                (page - 1).coerceAtLeast(0)
            } else {
                (page + 1).coerceAtMost(pageCount - 1)
            }
        scroll {
            // Update the target page
            updateTargetPage(page)

            // pre-jump to 1 page before our target page
            updateCurrentPage(preJumpPosition, pageOffsetFraction = 0.0f)
            val targetPageDiff = page - currentPage
            val distance = targetPageDiff * layoutInfo.pageSize.toFloat()
            var previousValue = 0.0f
            animate(0f, distance) { currentValue, _ ->
                previousValue += scrollBy(currentValue - previousValue)
            }
        }
    }

    val state = rememberGlimmerPagerState(initialPage = 5, pageCount = { 10 })
    val scope = rememberCoroutineScope()

    Column {
        Button(onClick = { scope.launch { state.customAnimateScrollToPage(1) } }) {
            Text(text = "Scroll to Page 1")
        }

        GlimmerHorizontalPager(modifier = Modifier.fillMaxSize(), state = state) { page ->
            Card {
                Text(
                    text = "Page: $page",
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                )
            }
        }
    }
}

@Preview
@Composable
fun GlimmerHorizontalPagerSamplePreview() {
    GlimmerTheme {
        Box(
            modifier = Modifier.background(GlimmerTheme.colors.background).fillMaxSize(),
            contentAlignment = Alignment.BottomStart,
        ) {
            GlimmerHorizontalPagerSample()
        }
    }
}
