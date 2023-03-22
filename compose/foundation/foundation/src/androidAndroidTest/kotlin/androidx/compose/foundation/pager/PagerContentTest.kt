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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import org.junit.Test

class PagerContentTest : SingleOrientationPagerTest(Orientation.Horizontal) {

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun pageContent_makeSureContainerOwnsOutsideModifiers() {
        // Arrange
        val state = PagerState()
        rule.setContent {
            HorizontalOrVerticalPager(
                pageCount = 10,
                state = state,
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
    fun pageContent_makeSureInnerModifiersAreAppliedToPages() {
        // Arrange
        val state = PagerState()
        val drawingList = mutableListOf<Int>()
        rule.setContent {
            HorizontalOrVerticalPager(
                pageCount = 10,
                state = state,
                modifier = Modifier
                    .width(100.dp)
                    .testTag("pager"),
                pageSize = PageSize.Fixed(10.dp)
            ) { page ->
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .size(100.dp)
                        .zIndex(if (page % 2 == 0) 100f else 50f)
                        .drawWithContent {
                            drawingList.add(page)
                        }
                        .testTag(page.toString())
                )
            }
        }

        rule.runOnIdle {
            assertContentEquals(drawingList, listOf(1, 3, 5, 7, 9, 0, 2, 4, 6, 8))
        }
    }
}