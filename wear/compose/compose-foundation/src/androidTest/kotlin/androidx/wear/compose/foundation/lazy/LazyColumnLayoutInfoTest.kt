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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyColumnLayoutInfoTest {
    @get:Rule val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    fun visibleItemsAreCorrect() {
        lateinit var state: LazyColumnState

        rule.setContent {
            LazyColumn(
                state = rememberLazyColumnState().also { state = it },
                // Viewport take 4 items, item 0 is exactly above the center and there is space for
                // two more items below the center line.
                modifier = Modifier.requiredSize(itemSizeDp * 4f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items((0..5).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportSize.height).isEqualTo(itemSizePx * 4)
            // Start offset compensates for the layout where the first item is exactly above the
            // center line.
            state.layoutInfo.assertVisibleItems(count = 3, startOffset = itemSizePx)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithSpacing() {
        lateinit var state: LazyColumnState

        rule.setContent {
            LazyColumn(
                state = rememberLazyColumnState().also { state = it },
                // Viewport take 4 items, item 0 is exactly above the center and there is space for
                // two more items below the center line.
                modifier = Modifier.requiredSize(itemSizeDp * 4f),
                verticalArrangement = Arrangement.spacedBy(itemSizeDp),
            ) {
                items((0..5).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportSize.height).isEqualTo(itemSizePx * 4)
            // Start offset compensates for the layout where the first item is exactly above the
            // center line.
            state.layoutInfo.assertVisibleItems(
                count = 2,
                spacing = itemSizePx,
                startOffset = itemSizePx
            )
        }
    }

    @Test
    fun visibleItemsAreObservableWhenResize() {
        lateinit var state: LazyColumnState
        var size by mutableStateOf(itemSizeDp * 2)
        var currentInfo: LazyColumnLayoutInfo? = null
        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }
        rule.setContent {
            LazyColumn(
                state = rememberLazyColumnState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 4f)
            ) {
                item { Box(Modifier.requiredSize(size)) }
            }
            observingFun()
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, expectedSize = itemSizePx * 2)
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, expectedSize = itemSizePx)
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: LazyColumnState
        rule.setContent {
            LazyColumn(state = rememberLazyColumnState().also { state = it }) {
                items((0 until count).toList()) { Box(Modifier.requiredSize(10.dp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(10)
            count = 20
        }

        rule.runOnIdle { assertThat(state.layoutInfo.totalItemsCount).isEqualTo(20) }
    }

    @Test
    fun viewportOffsetsAndSizeAreCorrect() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        lateinit var state: LazyColumnState
        rule.setContent {
            LazyColumn(
                Modifier.height(sizeDp).width(sizeDp * 2),
                state = rememberLazyColumnState().also { state = it }
            ) {
                items((0..3).toList()) { Box(Modifier.requiredSize(sizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportSize).isEqualTo(IntSize(sizePx * 2, sizePx))
        }
    }

    @Test
    fun contentTypeIsCorrect() {
        lateinit var state: LazyColumnState
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(itemSizeDp * 3f),
                state = rememberLazyColumnState().also { state = it },
            ) {
                items(2, contentType = { it }) { Box(Modifier.requiredSize(itemSizeDp)) }
                item { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItems.map { it.contentType })
                .isEqualTo(listOf(0, 1, null))
        }
    }

    @Test
    fun keyIsCorrect() {
        lateinit var state: LazyColumnState
        rule.setContent {
            LazyColumn(
                modifier = Modifier.requiredSize(itemSizeDp * 3f),
                state = rememberLazyColumnState().also { state = it },
            ) {
                items(2, key = { it }) { Box(Modifier.requiredSize(itemSizeDp)) }
                item { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItems.map { it.key }.size).isEqualTo(3)
            assertThat(state.layoutInfo.visibleItems.map { it.key }.subList(0, 2))
                .isEqualTo(listOf(0, 1)) // Last key is autogenerated.
        }
    }

    private fun LazyColumnLayoutInfo.assertVisibleItems(
        count: Int,
        startIndex: Int = 0,
        startOffset: Int = 0,
        expectedSize: Int = itemSizePx,
        spacing: Int = 0
    ) {
        assertThat(visibleItems.size).isEqualTo(count)
        var currentIndex = startIndex
        var currentOffset = startOffset
        visibleItems.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertWithMessage("Offset of item $currentIndex")
                .that(it.offset)
                .isEqualTo(currentOffset)
            assertThat(it.height).isEqualTo(expectedSize)
            currentIndex++
            currentOffset += it.height + spacing
        }
    }
}
