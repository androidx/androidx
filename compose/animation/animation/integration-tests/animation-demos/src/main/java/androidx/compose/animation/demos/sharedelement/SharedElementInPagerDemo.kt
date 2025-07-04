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

package androidx.compose.animation.demos.sharedelement

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun SharedElementInPagerDemo() {
    var selectedCat by remember { mutableStateOf<Cat?>(null) }
    val pagerState = rememberPagerState { listCats.size }
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(selectedCat) {
            if (it == null) {
                HorizontalPager(
                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                    state = pagerState,
                    pageSize = TwoPagesPerViewport,
                    pageSpacing = 8.dp,
                    snapPosition = SnapPosition.Center,
                    flingBehavior =
                        PagerDefaults.flingBehavior(
                            state = pagerState,
                            pagerSnapDistance = PagerSnapDistance.atMost(3),
                        ),
                ) {
                    val cat = listCats[it]
                    CatItem(
                        cat = cat,
                        onClick = { selectedCat = cat },
                        scope = this@AnimatedContent,
                    )
                }
            } else {
                CatDetails(cat = it, this@AnimatedContent, onConfirmClick = { selectedCat = null })
            }
        }
    }
}

private val TwoPagesPerViewport =
    object : PageSize {
        override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
            return (availableSpace - 2 * pageSpacing) / 2
        }
    }
