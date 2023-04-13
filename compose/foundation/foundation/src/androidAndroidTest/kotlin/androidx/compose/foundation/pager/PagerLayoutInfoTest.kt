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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(Parameterized::class)
class PagerLayoutInfoTest(private val param: ParamConfig) : BasePagerTest(param) {

    private var pageSizeDp: Dp = 200.dp
    private var pageSizePx: Int = 0

    @Before
    fun setUp() {
        pageSizePx = with(rule.density) { pageSizeDp.roundToPx() }
    }

    @Test
    fun visiblePagesAreCorrect() {
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier.requiredSize(pageSizeDp * 3.5f),
            pageCount = { 5 },
            pageSize = { PageSize.Fixed(pageSizeDp) }
        ) {
            Box(Modifier.requiredSize(pageSizeDp))
        }
        rule.runOnIdle {
            state.layoutInfo.assertVisiblePages(count = 4)
        }
    }

    @Test
    fun visiblePagesAreCorrectAfterScroll() {
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier.requiredSize(pageSizeDp * 3.5f),
            pageCount = { 5 },
            pageSize = { PageSize.Fixed(pageSizeDp) }
        ) {
            Box(Modifier.requiredSize(pageSizeDp))
        }

        rule.runOnIdle {
            runBlocking {
                state.scrollToPage(1)
                state.scrollBy(10f)
            }

            state.layoutInfo.assertVisiblePages(
                count = 4,
                startIndex = 1,
                startOffset = -10
            )
        }
    }

    @Test
    fun visiblePagesAreCorrectWithSpacing() {
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier.requiredSize(pageSizeDp * 3.5f),
            pageCount = { 5 },
            pageSpacing = pageSizeDp,
            pageSize = { PageSize.Fixed(pageSizeDp) }
        ) {
            Box(Modifier.requiredSize(pageSizeDp))
        }

        rule.runOnIdle {
            state.layoutInfo.assertVisiblePages(count = 2, spacing = pageSizePx)
        }
    }

    @Test
    fun visiblePagesAreObservableWhenWeScroll() {
        val state = PagerState()
        val currentInfo = StableRef<PagerLayoutInfo?>(null)
        createPager(
            state,
            modifier = Modifier.requiredSize(pageSizeDp * 3.5f),
            pageCount = { 5 },
            pageSize = { PageSize.Fixed(pageSizeDp) },
            additionalContent = {
                LaunchedEffect(key1 = state) {
                    snapshotFlow { state.layoutInfo }.collect {
                        currentInfo.value = it
                    }
                }
            }
        ) {
            Box(Modifier.requiredSize(pageSizeDp))
        }

        rule.runOnIdle {
            // empty it here and scrolling should invoke observingFun again
            currentInfo.value = null
            runBlocking {
                state.scrollToPage(1)
            }
        }

        rule.runOnIdle {
            assertThat(currentInfo.value).isNotNull()
            currentInfo.value!!.assertVisiblePages(count = 4, startIndex = 1)
        }
    }

    @Test
    fun visiblePagesAreObservableWhenResize() {
        val state = PagerState()
        var pageSize by mutableStateOf(PageSize.Fixed(pageSizeDp * 2))
        var currentInfo: PagerLayoutInfo? = null

        @Composable
        fun observingFun() {
            currentInfo = state.layoutInfo
        }

        createPager(
            state,
            pageCount = { 1 },
            pageSize = { pageSize },
            additionalContent = { observingFun() }
        ) {
            Box(Modifier.requiredSize(pageSizeDp * 2))
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisiblePages(
                count = 1,
                pageSize = with(rule.density) { pageSizeDp.roundToPx() * 2 })
            currentInfo = null
            pageSize = PageSize.Fixed(pageSizeDp)
        }

        rule.runOnIdle {
            assertThat(currentInfo).isNotNull()
            currentInfo!!.assertVisiblePages(count = 1, pageSize = pageSizePx)
        }
    }

    @Test
    fun totalCountIsCorrect() {
        var count by mutableStateOf(10)
        val state = PagerState()

        createPager(
            state,
            pageCount = { count },
            pageSize = { PageSize.Fixed(10.dp) }
        ) {
            Box(Modifier.requiredSize(10.dp))
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.pagesCount).isEqualTo(10)
            count = 20
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.pagesCount).isEqualTo(20)
        }
    }

    @Test
    fun viewportOffsetsAndSizeAreCorrect() {
        val sizePx = 45
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier
                .mainAxisSize(sizeDp)
                .crossAxisSize(sizeDp * 2),
            pageCount = { 3 },
            pageSize = { PageSize.Fixed(sizeDp) }
        ) {
            Box(Modifier.requiredSize(sizeDp))
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(0)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun viewportOffsetsAndSizeAreCorrectWithContentPadding() {
        val reverseLayout = param.reverseLayout
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val beforeContentPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val afterContentPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier
                .mainAxisSize(sizeDp)
                .crossAxisSize(sizeDp * 2),
            pageCount = { 3 },
            pageSize = { PageSize.Fixed(sizeDp) },
            contentPadding = PaddingValues(
                beforeContent = beforeContentPaddingDp,
                afterContent = afterContentPaddingDp,
                beforeContentCrossAxis = 2.dp,
                afterContentCrossAxis = 2.dp
            )
        ) {
            Box(Modifier.requiredSize(sizeDp))
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun emptyPagesInVisiblePagesInfo() {
        val state = PagerState()

        createPager(
            state,
            pageCount = { 2 },
            pageSize = { PageSize.Fixed(pageSizeDp) }
        ) {
            Box(Modifier)
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.visiblePagesInfo.size).isEqualTo(2)
            assertThat(state.layoutInfo.visiblePagesInfo.first().index).isEqualTo(0)
            assertThat(state.layoutInfo.visiblePagesInfo.last().index).isEqualTo(1)
        }
    }

    @Test
    fun emptyContent() {
        val reverseLayout = param.reverseLayout
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val beforeContentPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val afterContentPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier
                .mainAxisSize(sizeDp)
                .crossAxisSize(sizeDp * 2),
            pageCount = { 0 },
            pageSize = { PageSize.Fixed(sizeDp) },
            contentPadding = PaddingValues(
                beforeContent = beforeContentPaddingDp,
                afterContent = afterContentPaddingDp,
                beforeContentCrossAxis = 2.dp,
                afterContentCrossAxis = 2.dp
            )
        ) {}

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun viewportIsLargerThenTheContent() {
        val reverseLayout = param.reverseLayout
        val sizePx = 45
        val startPaddingPx = 10
        val endPaddingPx = 15
        val sizeDp = with(rule.density) { sizePx.toDp() }
        val beforeContentPaddingDp = with(rule.density) {
            if (!reverseLayout) startPaddingPx.toDp() else endPaddingPx.toDp()
        }
        val afterContentPaddingDp = with(rule.density) {
            if (!reverseLayout) endPaddingPx.toDp() else startPaddingPx.toDp()
        }
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier
                .mainAxisSize(sizeDp)
                .crossAxisSize(sizeDp * 2),
            pageCount = { 1 },
            pageSize = { PageSize.Fixed(sizeDp) },
            contentPadding = PaddingValues(
                beforeContent = beforeContentPaddingDp,
                afterContent = afterContentPaddingDp,
                beforeContentCrossAxis = 2.dp,
                afterContentCrossAxis = 2.dp
            )
        ) {
            Box(Modifier.size(sizeDp / 2))
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.viewportStartOffset).isEqualTo(-startPaddingPx)
            assertThat(state.layoutInfo.viewportEndOffset).isEqualTo(sizePx - startPaddingPx)
            assertThat(state.layoutInfo.beforeContentPadding).isEqualTo(startPaddingPx)
            assertThat(state.layoutInfo.afterContentPadding).isEqualTo(endPaddingPx)
            assertThat(state.layoutInfo.viewportSize).isEqualTo(
                if (vertical) IntSize(sizePx * 2, sizePx) else IntSize(sizePx, sizePx * 2)
            )
        }
    }

    @Test
    fun reverseLayoutIsCorrect() {
        val state = PagerState()

        createPager(
            state,
            modifier = Modifier.requiredSize(pageSizeDp * 3.5f),
            pageCount = { 5 },
            pageSize = { PageSize.Fixed(pageSizeDp) }
        ) {
            Box(Modifier.requiredSize(pageSizeDp))
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.reverseLayout).isEqualTo(param.reverseLayout)
        }
    }

    @Test
    fun orientationIsCorrect() {

        val state = PagerState()

        createPager(
            state,
            modifier = Modifier.requiredSize(pageSizeDp * 3.5f),
            pageCount = { 5 },
            pageSize = { PageSize.Fixed(pageSizeDp) }
        ) {
            Box(Modifier.requiredSize(pageSizeDp))
        }

        rule.runOnIdle {
            assertThat(state.layoutInfo.orientation)
                .isEqualTo(if (vertical) Orientation.Vertical else Orientation.Horizontal)
        }
    }

    private fun PagerLayoutInfo.assertVisiblePages(
        count: Int,
        startIndex: Int = 0,
        startOffset: Int = 0,
        spacing: Int = 0,
        pageSize: Int = pageSizePx
    ) {
        assertThat(this.pageSize).isEqualTo(pageSize)
        assertThat(visiblePagesInfo.size).isEqualTo(count)
        var currentIndex = startIndex
        var currentOffset = startOffset
        visiblePagesInfo.forEach {
            assertThat(it.index).isEqualTo(currentIndex)
            assertWithMessage("Offset of page $currentIndex").that(it.offset)
                .isEqualTo(currentOffset)
            currentIndex++
            currentOffset += pageSize + spacing
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = mutableListOf<ParamConfig>().apply {
            for (orientation in TestOrientation) {
                for (reverseLayout in TestReverseLayout) {
                    add(ParamConfig(orientation = orientation, reverseLayout = reverseLayout))
                }
            }
        }
    }
}

@Stable
class StableRef<T>(var value: T)
