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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class PageSizeTest(val config: ParamConfig) : BasePagerTest(config) {
    @Test
    fun pageSizeFill_onlySnappedItemIsDisplayed() {
        // Arrange

        // Act
        createPager(initialPage = 5, modifier = Modifier.fillMaxSize())

        // Assert
        rule.onNodeWithTag("4").assertDoesNotExist()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertDoesNotExist()
        confirmPageIsInCorrectPosition(5)
    }

    @Test
    fun pagerSizeCustom_visibleItemsAreWithinViewport() {
        // Arrange
        val pagerMode = object : PageSize {
            override fun Density.calculateMainAxisPageSize(
                availableSpace: Int,
                pageSpacing: Int
            ): Int {
                return 100.dp.roundToPx() + pageSpacing
            }
        }

        // Act
        createPager(
            initialPage = 5,
            modifier = Modifier.crossAxisSize(200.dp),
            beyondViewportPageCount = 0,
            pageSize = { pagerMode }
        )

        // Assert
        rule.runOnIdle {
            val visibleItems = pagerState.layoutInfo.visiblePagesInfo.size
            val pageCount = with(rule.density) {
                (pagerSize / (pageSize + config.pageSpacing.roundToPx()))
            } + 1
            Truth.assertThat(visibleItems).isEqualTo(pageCount)
        }

        for (pageIndex in 5 until pagerState.layoutInfo.visiblePagesInfo.size + 4) {
            confirmPageIsInCorrectPosition(5, pageIndex)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (pageSpacing in TestPageSpacing) {
                    add(
                        ParamConfig(
                            orientation = orientation,
                            pageSpacing = pageSpacing
                        )
                    )
                }
            }
        }
    }
}
