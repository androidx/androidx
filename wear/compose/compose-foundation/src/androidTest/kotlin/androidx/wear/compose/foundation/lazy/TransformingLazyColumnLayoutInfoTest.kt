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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TransformingLazyColumnLayoutInfoTest {
    @get:Rule val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity

    @Before
    fun before() {
        with(rule.density) { itemSizeDp = itemSizePx.toDp() }
    }

    @Test
    fun visibleItemsAreCorrect() {
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                // Viewport take 4 items, item 0 is exactly above the center and there is space for
                // two more items below the center line.
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items((0..5).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportSize.height).isEqualTo(itemSizePx * 5)
            // Start offset compensates for the layout where the first item is exactly above the
            // center line.
            state.layoutInfo.assertVisibleItems(count = 3, startOffset = itemSizePx * 2)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithSpacing() {
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                // Viewport take 4 items, item 0 is exactly above the center and there is space for
                // two more items below the center line.
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
                verticalArrangement = Arrangement.spacedBy(itemSizeDp),
            ) {
                items((0..5).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportSize.height).isEqualTo(itemSizePx * 5)
            // Start offset compensates for the layout where the first item is exactly above the
            // center line.
            state.layoutInfo.assertVisibleItems(
                count = 2,
                spacing = itemSizePx,
                startOffset = itemSizePx * 2,
            )
        }
    }

    @Composable
    fun ObservingFun(
        state: TransformingLazyColumnState,
        currentInfo: StableRef<TransformingLazyColumnLayoutInfo?>
    ) {
        currentInfo.value = state.layoutInfo
    }

    @Test
    fun visibleItemsAreObservableWhenWeScroll() {
        lateinit var state: TransformingLazyColumnState
        val currentInfo = StableRef<TransformingLazyColumnLayoutInfo?>(null)
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.requiredSize(itemSizeDp * 3f)
            ) {
                items((0..5).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
            ObservingFun(state, currentInfo)
        }

        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking { state.scrollToItem(1) }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisibleItems(count = 3, startIndex = 0)
        }
    }

    @Test
    fun visibleItemsAreObservableWhenResize() {
        lateinit var state: TransformingLazyColumnState
        var size by mutableStateOf(itemSizeDp * 2)
        var currentInfo: TransformingLazyColumnLayoutInfo? = null
        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 4f)
            ) {
                item { Box(Modifier.requiredSize(size)) }
            }
            observingFun()
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(
                count = 1,
                expectedSize = itemSizePx * 2,
                startOffset = itemSizePx,
            )
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(
                count = 1,
                expectedSize = itemSizePx,
                startOffset = itemSizePx * 3 / 2,
            )
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it }
            ) {
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
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            TransformingLazyColumn(
                Modifier.height(sizeDp).width(sizeDp * 2),
                state = rememberTransformingLazyColumnState().also { state = it }
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
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
                state = rememberTransformingLazyColumnState().also { state = it },
            ) {
                items(2, contentType = { it }) { Box(Modifier.requiredSize(itemSizeDp)) }
                item { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.visibleItems.size).isEqualTo(3)
            assertThat(state.layoutInfo.visibleItems.map { it.contentType })
                .isEqualTo(listOf(0, 1, null))
        }
    }

    @Test
    fun keyIsCorrect() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            TransformingLazyColumn(
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
                state = rememberTransformingLazyColumnState().also { state = it },
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

    @Test
    fun visibleItemsAreCorrectAfterScroll() {
        lateinit var state: TransformingLazyColumnState
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items((0..6).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(2, scrollOffset = -10) } }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1, startOffset = -10)
        }
    }

    private fun TransformingLazyColumnLayoutInfo.assertVisibleItems(
        count: Int,
        startIndex: Int = 0,
        startOffset: Int = 0,
        expectedSize: Int = itemSizePx,
        spacing: Int = 0,
    ) {
        assertThat(visibleItems.size).isEqualTo(count)
        var currentIndex = startIndex
        var currentOffset = startOffset
        visibleItems.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertWithMessage("Offset of item $currentIndex")
                .that(it.offset)
                .isEqualTo(currentOffset)
            assertThat(it.transformedHeight).isEqualTo(expectedSize)
            currentIndex++
            currentOffset += it.transformedHeight + spacing
        }
    }
}
