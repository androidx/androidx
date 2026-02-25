/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package androidx.xr.glimmer.benchmark.list

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.benchmark.sendIndirectSwipe
import androidx.xr.glimmer.list.ListState
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.testutils.NoFlingBehavior
import androidx.xr.glimmer.testutils.createGlimmerRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class GlimmerVerticalListScrollingBenchmark {

    @get:Rule(0) val benchmarkRule = ComposeBenchmarkRule()

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun verticalList_measureAndLayoutPhases_afterProgrammaticScroll() {
        val listState = ListState(firstVisibleItemIndex = 0)
        benchmarkRule.toggleStateBenchmark { ProgrammaticScrollTestCase(listState) }
    }

    @Test
    fun verticalList_measureAndLayoutPhases_afterIndirectScroll() {
        val listState = ListState(firstVisibleItemIndex = 0)
        benchmarkRule.toggleStateBenchmark { IndirectTouchScrollTestCase(listState) }
    }

    /**
     * Since the draw phase happens after the scroll has finished, the specific scroll method used
     * is irrelevant. Therefore, there is no need to test both types, as they will yield roughly the
     * same results.
     */
    @Test
    fun verticalList_drawPhase_afterScroll() {
        val listState = ListState(firstVisibleItemIndex = 0)
        benchmarkRule.toggleStateBenchmarkDraw { IndirectTouchScrollTestCase(listState) }
    }
}

internal class ProgrammaticScrollTestCase(listState: ListState) :
    ScrollableGlimmerListTestCase(listState) {
    override fun toggle() {
        // The `setUp()` and `tearDown()` methods are supposed to reset the scroll.
        // So there's no need to scroll it back inside `toggle()`.
        runBlocking { listState.scrollBy(scrollDistance) }
    }
}

@SuppressLint("VisibleForTests")
internal class IndirectTouchScrollTestCase(listState: ListState) :
    ScrollableGlimmerListTestCase(listState) {

    private var view: ViewRootForTest? = null

    @Composable
    override fun Content() {
        super.Content()
        view = LocalView.current as? ViewRootForTest
    }

    override fun toggle() {
        // The `setUp()` and `tearDown()` methods are supposed to reset the scroll.
        // So there's no need to scroll it back inside `toggle()`.
        requireNotNull(view)
            .sendIndirectSwipe(
                distance = scrollDistance,
                primaryAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            )
    }
}

internal abstract class ScrollableGlimmerListTestCase(val listState: ListState) :
    GlimmerListBenchmarkTestCase, LayeredComposeTestCase() {

    private var expectedFocusedItemIndex = -1
    private var focusedItemIndex = -2 // choose a value different from [expectedFocusedItemIndex].
    private var mainThreadId = -1L
    protected var scrollDistance: Float = Float.NaN

    @Composable
    override fun Content() {
        val scrollBy = 75.dp
        scrollDistance = with(LocalDensity.current) { scrollBy.toPx() }
        expectedFocusedItemIndex = (scrollBy / (ItemHeight + SpacedBy)).toInt()
        mainThreadId = LocalView.current.handler.looper.thread.id
        Box(Modifier.fillMaxSize()) { MeasuredContent() }
    }

    override fun setUp() {
        assertMainThread()
        runBlocking { listState.scrollToItem(0, 0) }
    }

    override fun beforeToggleCheck() {
        assertMainThread()
        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals(0, listState.firstVisibleItemScrollOffset)
    }

    override fun afterToggleCheck() {
        assertMainThread()
        assertEquals(expectedFocusedItemIndex, focusedItemIndex)
    }

    override fun tearDown() {
        // no-op
    }

    protected fun assertMainThread() {
        assertEquals(
            "Test case methods are expected to be called from the main thread.",
            mainThreadId,
            Thread.currentThread().id,
        )
    }

    @Composable
    override fun MeasuredContent() {
        VerticalList(
            state = listState,
            modifier = Modifier.requiredHeight(ListHeight).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacedBy),
            contentPadding = PaddingValues(16.dp),
            flingBehavior = NoFlingBehavior,
        ) {
            items(300) { index -> FocusableItem(index) }
        }
    }

    @Composable
    private fun FocusableItem(index: Int) {
        Text(
            text = "Item-$index",
            modifier =
                Modifier.height(ItemHeight)
                    .fillMaxWidth()
                    .onFocusChanged { if (it.hasFocus) focusedItemIndex = index }
                    .focusable(),
        )
    }

    companion object {
        private val SpacedBy = 12.dp
        private val ItemHeight = 50.dp
        private val ListHeight = 400.dp
    }
}
