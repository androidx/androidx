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

package androidx.compose.foundation.pager

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class PagerContentTest {

    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun pageContent_makeSureContainerOwnsOutsideModifiers() {
        // Arrange
        lateinit var state: PagerState
        rule.setContent {
            HorizontalPager(
                state = rememberPagerState { 10 }.also { state = it },
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 4.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pager"),
                pageSize = PageSize.Fill
            ) { page ->
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .size(100.dp)
                        .testTag(page.toString())
                )
            }
        }

        rule.onNodeWithTag("pager").performTouchInput {
            swipe(bottomRight, bottomLeft) // swipe outside bounds of pages
        }

        rule.runOnIdle {
            assertTrue { state.currentPage != 0 }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun pageContent_makeSureInnerModifiersAreAppliedToPages() {
        // Arrange
        val colors = listOf(Color.Blue, Color.Green, Color.Red)
        rule.setContent {
            HorizontalPager(
                state = rememberPagerState { colors.size },
                modifier = Modifier
                    .width(6.dp)
                    .testTag(PagerTestTag),
                pageSize = PageSize.Fixed(2.dp)
            ) { page ->
                val color = colors[page]
                Box(
                    modifier = Modifier
                        .testTag(page.toString())
                        .width(2.dp)
                        .height(6.dp)
                        .zIndex(if (color == Color.Green) 1f else 0f)
                        .drawBehind {
                            drawRect(
                                color,
                                topLeft = Offset(-10.dp.toPx(), -10.dp.toPx()),
                                size = Size(20.dp.toPx(), 20.dp.toPx())
                            )
                        }
                )
            }
        }

        rule.onNodeWithTag(PagerTestTag)
            .captureToImage()
            .assertPixels { Color.Green }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun scrollableState_isScrollableWhenChangingPages() {
        val states = mutableMapOf<Int, ScrollState>()
        val pagerState = PagerStateImpl(
            initialPage = 0,
            initialPageOffsetFraction = 0.0f,
            updatedPageCount = { 2 })
        rule.setContent {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .testTag("pager")
                    .fillMaxSize()
            ) { page ->
                Column(
                    modifier = Modifier
                        .testTag("$page")
                        .verticalScroll(rememberScrollState().also {
                            states[page] = it
                        })
                ) {
                    repeat(100) {
                        Box(
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("0").performTouchInput {
            swipe(bottomCenter, topCenter)
        }
        rule.runOnIdle {
            Truth.assertThat(states[0]!!.value).isNotEqualTo(0f)
        }
        val previousScrollStateValue = states[0]!!.value
        rule.onNodeWithTag("pager").performTouchInput {
            swipe(centerRight, centerLeft)
        }

        rule.onNodeWithTag("pager").performTouchInput {
            swipe(centerLeft, centerRight)
        }
        rule.onNodeWithTag("0").performTouchInput {
            swipe(bottomCenter, topCenter)
        }
        rule.runOnIdle {
            Truth.assertThat(previousScrollStateValue).isNotEqualTo(states[0]!!.value)
        }
    }
}