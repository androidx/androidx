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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun HorizontalPageIndicatorSample() {
    val pageCount = 9
    var selectedPage by remember { mutableStateOf(0) }

    val animatedSelectedPage by
        animateFloatAsState(
            targetValue = selectedPage.toFloat(),
            label = "animateSelectedPage",
        )

    Box(modifier = Modifier.fillMaxSize()) {
        Slider(
            modifier = Modifier.align(Alignment.Center),
            value = selectedPage,
            valueProgression = 0 until pageCount,
            onValueChange = { selectedPage = it }
        )
        HorizontalPageIndicator(
            pageCount = pageCount,
            currentPage = selectedPage,
            currentPageOffsetFraction = { animatedSelectedPage - selectedPage },
        )
    }
}

@Sampled
@Composable
fun HorizontalPageIndicatorWithPagerSample(swipeState: SwipeToDismissBoxState) {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    Box {
        HorizontalPager(
            modifier = Modifier.fillMaxSize().edgeSwipeToDismiss(swipeState),
            state = pagerState,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                Text(modifier = Modifier.align(Alignment.Center), text = "Page #$page")
            }
        }
        HorizontalPageIndicator(
            pageCount = pageCount,
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = { pagerState.currentPageOffsetFraction },
        )
    }
}
