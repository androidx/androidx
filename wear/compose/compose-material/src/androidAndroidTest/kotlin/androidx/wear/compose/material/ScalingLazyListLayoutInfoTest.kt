/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
public class ScalingLazyListLayoutInfoTest {
    @get:Rule
    val rule = createComposeRule()

    private var itemSizePx: Int = 50
    private var itemSizeDp: Dp = Dp.Infinity
    private var defaultItemSpacingDp: Dp = 4.dp
    private var defaultItemSpacingPx = Int.MAX_VALUE

    @Before
    fun before() {
        with(rule.density) {
            itemSizeDp = itemSizePx.toDp()
            defaultItemSpacingPx = defaultItemSpacingDp.roundToPx()
        }
    }

    @Test
    fun visibleItemsAreCorrect() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 4)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrolling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Test
    fun visibleItemsAreCorrectNoScaling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 4)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterScrollNoScaling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollBy(-(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectAfterDispatchRawDeltaScrollNoScaling() {
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(
                    itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f
                ),
                scalingParams = ScalingLazyColumnDefaults.scalingParams(1.0f, 1.0f)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            runBlocking {
                state.dispatchRawDelta(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }

        rule.runOnIdle {
            runBlocking {
                state.dispatchRawDelta(-(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat()))
            }
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 0)
            assertThat(state.layoutInfo.visibleItemsInfo.first().offset).isEqualTo(0)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithCustomSpacing() {
        lateinit var state: ScalingLazyListState
        val spacing: Dp = 10.dp
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + spacing * 2.5f),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                items(5) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
        }

        rule.runOnIdle {
            val spacingPx = with(rule.density) {
                spacing.roundToPx()
            }
            state.layoutInfo.assertVisibleItems(
                count = 4,
                spacing = spacingPx
            )
        }
    }

    @Composable
    fun ObservingFun(
        state: ScalingLazyListState,
        currentInfo: StableRef<ScalingLazyListLayoutInfo?>
    ) {
        currentInfo.value = state.layoutInfo
    }

    @Test
    fun visibleItemsAreObservableWhenWeScroll() {
        lateinit var state: ScalingLazyListState
        val currentInfo = StableRef<ScalingLazyListLayoutInfo?>(null)
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingFun(state, currentInfo)
        }

        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.scrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Test
    fun visibleItemsAreObservableWhenWeDispatchRawDeltaScroll() {
        lateinit var state: ScalingLazyListState
        val currentInfo = StableRef<ScalingLazyListLayoutInfo?>(null)
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingFun(state, currentInfo)
        }

        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.dispatchRawDelta(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisibleItems(count = 4, startIndex = 1)
        }
    }

    @Composable
    fun ObservingIsScrollInProgressTrueFun(
        state: ScalingLazyListState,
        currentInfo: StableRef<Boolean?>
    ) {
        // If isScrollInProgress is ever true record it - otherwise leave the value as null
        if (state.isScrollInProgress) {
            currentInfo.value = true
        }
    }

    @Test
    fun isScrollInProgressIsObservableWhenWeScroll() {
        lateinit var state: ScalingLazyListState
        var scope: CoroutineScope? = null
        val currentInfo = StableRef<Boolean?>(null)
        rule.setContent {
            scope = rememberCoroutineScope()
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 3.5f + defaultItemSpacingDp * 2.5f)
            ) {
                items(6) {
                    Box(Modifier.requiredSize(itemSizeDp))
                }
            }
            ObservingIsScrollInProgressTrueFun(state, currentInfo)
        }

        scope!!.launch {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            state.animateScrollBy(itemSizePx.toFloat() + defaultItemSpacingPx.toFloat())
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            assertThat(currentInfo.value).isTrue()
        }
    }

    @Test
    fun visibleItemsAreObservableWhenResize() {
        lateinit var state: ScalingLazyListState
        var size by mutableStateOf(itemSizeDp * 2)
        var currentInfo: ScalingLazyListLayoutInfo? = null
        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it }
            ) {
                item {
                    Box(Modifier.requiredSize(size))
                }
            }
            observingFun()
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, unscaledSize = itemSizePx * 2)
            currentInfo = null
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisibleItems(count = 1, unscaledSize = itemSizePx)
        }
    }

    @Test
    fun viewportOffsetsAreCorrect() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                Modifier.requiredSize(sizeDp),
                state = rememberScalingLazyListState().also { state = it }
            ) {
                items(4) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
        }
    }

    @Test
    fun viewportOffsetsAreCorrectWithContentPadding() {
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val topPaddingDp = with(rule.density) { startPaddingPx.toDp() }
        val bottomPaddingDp = with(rule.density) { endPaddingPx.toDp() }
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                Modifier.requiredSize(sizeDp),
                contentPadding = PaddingValues(top = topPaddingDp, bottom = bottomPaddingDp),
                state = rememberScalingLazyListState().also { state = it }
            ) {
                items(4) {
                    Box(Modifier.requiredSize(sizeDp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        lateinit var state: ScalingLazyListState
        rule.setContent {
            ScalingLazyColumn(
                state = rememberScalingLazyListState().also { state = it }
            ) {
                items(count) {
                    Box(Modifier.requiredSize(10.dp))
                }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(10)
            count = 20
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.totalItemsCount).isEqualTo(20)
        }
    }

    fun ScalingLazyListLayoutInfo.assertVisibleItems(
        count: Int,
        startIndex: Int = 0,
        unscaledSize: Int = itemSizePx,
        spacing: Int = defaultItemSpacingPx
    ) {
        assertThat(visibleItemsInfo.size).isEqualTo(count)
        var currentIndex = startIndex
        var previousEndOffset = -1
        visibleItemsInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertThat(it.size).isEqualTo((unscaledSize * it.scale).roundToInt())
            currentIndex++
            if (previousEndOffset != -1) {
                assertThat(spacing).isEqualTo(it.offset - previousEndOffset)
            }
            previousEndOffset = it.offset + it.size
        }
    }
}

@Stable
class StableRef<T>(var value: T)