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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
                // Viewport takes 4 items, item 0 is exactly above the center and there is space for
                // two more items below the center line.
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items((0..5).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportSize.height).isEqualTo(itemSizePx * 5)
            state.layoutInfo.assertVisibleItems(count = 5)
        }
    }

    @Test
    fun visibleItemsAreCorrectWithSpacing() {
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                // Viewport takes 4 items, item 0 is exactly above the center and there is space for
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
            state.layoutInfo.assertVisibleItems(count = 3, spacing = itemSizePx)
        }
    }

    @Composable
    fun ObservingFun(
        state: TransformingLazyColumnState,
        currentInfo: StableRef<TransformingLazyColumnLayoutInfo?>,
    ) {
        LaunchedEffect(Unit) {
            snapshotFlow { state.layoutInfo }.collect { currentInfo.value = it }
        }
    }

    @Test
    fun visibleItemsAreObservableWhenWeScroll() {
        lateinit var state: TransformingLazyColumnState
        val currentInfo = StableRef<TransformingLazyColumnLayoutInfo?>(null)
        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.requiredSize(itemSizeDp * 3f),
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
        val currentInfo = StableRef<TransformingLazyColumnLayoutInfo?>(null)

        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                modifier = Modifier.requiredSize(itemSizeDp * 4f),
            ) {
                item { Box(Modifier.requiredSize(size)) }
            }
            ObservingFun(state, currentInfo)
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value?.assertVisibleItems(count = 1, expectedSize = itemSizePx * 2)
            size = itemSizeDp
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value?.assertVisibleItems(count = 1, expectedSize = itemSizePx)
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
                state = rememberTransformingLazyColumnState().also { state = it },
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
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items((0..6).toList()) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(2, scrollOffset = 10) } }
        rule.runOnIdle {
            state.layoutInfo.assertVisibleItems(count = 4, startIndex = 1, startOffset = -10)
        }
    }

    @Test
    fun reverseLayout_isReportedCorrectly() {
        lateinit var state: TransformingLazyColumnState
        var reverseLayout by mutableStateOf(false)

        rule.setContent {
            TransformingLazyColumn(
                state = rememberTransformingLazyColumnState().also { state = it },
                reverseLayout = reverseLayout,
            ) {
                items(5) { Box(Modifier.requiredSize(itemSizeDp)) }
            }
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.reverseLayout).isFalse()
            reverseLayout = true
        }

        rule.runOnIdle { assertThat(state.layoutInfo.reverseLayout).isTrue() }
    }

    @Test
    fun layoutInfo_reflectsDynamicPadding() {
        lateinit var state: TransformingLazyColumnState
        val minimumVerticalContentPaddingPx = 50
        val minimumVerticalContentPaddingDp =
            with(rule.density) { minimumVerticalContentPaddingPx.toDp() }
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
            ) {
                items(100) { index ->
                    Box(
                        Modifier.requiredSize(itemSizeDp)
                            .minimumVerticalContentPadding(minimumVerticalContentPaddingDp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding)
                .isEqualTo(minimumVerticalContentPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(99) } }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding)
                .isEqualTo(minimumVerticalContentPaddingPx)
        }
    }

    @Test
    fun layoutInfo_reflectsDynamicPaddingWithAnimation() {
        lateinit var state: TransformingLazyColumnState
        val minimumVerticalContentPaddingPx = 50
        val minimumVerticalContentPaddingDp =
            with(rule.density) { minimumVerticalContentPaddingPx.toDp() }
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
            ) {
                items(100) { index ->
                    Box(
                        Modifier.requiredSize(itemSizeDp)
                            .animateItem()
                            .minimumVerticalContentPadding(minimumVerticalContentPaddingDp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding)
                .isEqualTo(minimumVerticalContentPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(99) } }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding)
                .isEqualTo(minimumVerticalContentPaddingPx)
        }
    }

    @Test
    fun layoutInfo_reflectsDynamicPaddingWithAnimationAfter() {
        lateinit var state: TransformingLazyColumnState
        val minimumVerticalContentPaddingPx = 50
        val minimumVerticalContentPaddingDp =
            with(rule.density) { minimumVerticalContentPaddingPx.toDp() }
        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.requiredSize(itemSizeDp * 5f),
            ) {
                items(100) { index ->
                    Box(
                        Modifier.requiredSize(itemSizeDp)
                            .minimumVerticalContentPadding(minimumVerticalContentPaddingDp)
                            .animateItem()
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding)
                .isEqualTo(minimumVerticalContentPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(10) } }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(0)
        }

        rule.runOnIdle { runBlocking { state.scrollToItem(99) } }
        rule.runOnIdle {
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(0)
            assertThat(state.layoutInfo.afterContentPadding)
                .isEqualTo(minimumVerticalContentPaddingPx)
        }
    }

    @Test
    fun emptyList_withReverseLayout_reportsReverseLayoutTrue() {
        lateinit var state: TransformingLazyColumnState

        rule.setContent {
            state = rememberTransformingLazyColumnState()
            TransformingLazyColumn(state = state, reverseLayout = true) {}
        }

        rule.runOnIdle { assertThat(state.layoutInfo.reverseLayout).isTrue() }
    }

    @Test
    fun layoutDirectionChange_updatesStartEndContentPadding() {
        var direction by mutableStateOf(LayoutDirection.Ltr)

        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                TransformingLazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 0.dp)) {
                    item { Box(Modifier.fillMaxWidth().height(40.dp).testTag("item")) }
                }
            }
        }

        rule.onNodeWithTag("item").assertLeftPositionInRootIsEqualTo(20.dp)

        rule.runOnIdle { direction = LayoutDirection.Rtl }

        rule.onNodeWithTag("item").assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun densityChange_updatesEdgeClickThreshold() {
        var densityState by mutableStateOf(Density(1f))
        var clickedIndex = -1

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides densityState) {
                val containerHeightDp = with(LocalDensity.current) { 125.toDp() }
                val itemHeightDp = with(LocalDensity.current) { 50.toDp() }
                TransformingLazyColumn(
                    modifier =
                        Modifier.fillMaxWidth().requiredHeight(containerHeightDp).testTag("list"),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(5) { index ->
                        Box(
                            Modifier.fillMaxWidth().height(itemHeightDp).clickable {
                                clickedIndex = index
                            }
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag("list").performTouchInput { click(Offset(centerX, 112f)) }
        rule.runOnIdle {
            assertThat(clickedIndex).isEqualTo(2)
            clickedIndex = -1
            densityState = Density(2f)
        }

        rule.onNodeWithTag("list").performTouchInput { click(Offset(centerX, 112f)) }
        rule.runOnIdle { assertThat(clickedIndex).isEqualTo(-1) }
    }

    @Test
    fun stateChange_updatesPointerInputAndAnimator() {
        val state1 = TransformingLazyColumnState()
        val state2 = TransformingLazyColumnState()
        var activeState by mutableStateOf(state1)
        var clickedIndex = -1
        val progresses = mutableMapOf<Int, TransformingLazyColumnItemScrollProgress>()

        rule.setContent {
            TransformingLazyColumn(
                state = activeState,
                modifier = Modifier.fillMaxWidth().requiredHeight(125.dp).testTag("list"),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                items(5, key = { it }) { index ->
                    Box(
                        Modifier.fillMaxWidth()
                            .height(50.dp)
                            .graphicsLayer { progresses[index] = scrollProgress }
                            .clickable { clickedIndex = index }
                    )
                }
            }
        }

        rule.runOnIdle {
            runBlocking { state1.scrollBy(with(rule.density) { 40.dp.toPx() }) }
        }
        val tapY = with(rule.density) { 5.dp.toPx() }
        rule.onNodeWithTag("list").performTouchInput { click(Offset(centerX, tapY)) }
        rule.runOnIdle {
            assertThat(clickedIndex).isEqualTo(-1)
            assertThat(progresses[0]?.topOffsetFraction).isWithin(0.02f).of(-0.32f)
            activeState = state2
        }

        rule.onNodeWithTag("list").performTouchInput { click(Offset(centerX, tapY)) }
        rule.runOnIdle {
            assertThat(clickedIndex).isEqualTo(0)
            assertThat(progresses[0]?.topOffsetFraction).isWithin(0.01f).of(0f)
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
