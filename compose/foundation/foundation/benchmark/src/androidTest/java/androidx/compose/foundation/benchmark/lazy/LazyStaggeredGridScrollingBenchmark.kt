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

package androidx.compose.foundation.benchmark.lazy

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.staggeredgrid.LazyHorizontalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyStaggeredGridScrollingBenchmark(
    private val testCase: LazyStaggeredGridScrollingTestCase
) {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun scrollProgrammatically_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollProgrammatically_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollProgrammatically_newItemComposed_up() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                firstItemIndex = 100,
                scrollUp = true,
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = false
            )
        }
    }

    @Test
    fun scrollViaPointerInput_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true
            )
        }
    }

    @Test
    fun scrollViaPointerInput_newItemComposed_up() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                firstItemIndex = 100,
                scrollUp = true,
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true
            )
        }
    }

    @Test
    @Ignore("b/300472956")
    fun scrollViaPointerInput_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true
            )
        }
    }

    @Test
    fun drawAfterScroll_noNewItems() {
        // this test makes sense only when run on the Android version which supports RenderNodes
        // as this tests how efficiently we move RenderNodes.
        Assume.assumeTrue(supportsRenderNode || supportsMRenderNode)
        benchmarkRule.toggleStateBenchmarkDraw {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun drawAfterScroll_newItemComposed() {
        // this test makes sense only when run on the Android version which supports RenderNodes
        // as this tests how efficiently we move RenderNodes.
        Assume.assumeTrue(supportsRenderNode || supportsMRenderNode)
        benchmarkRule.toggleStateBenchmarkDraw {
            StaggeredGridRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<LazyStaggeredGridScrollingTestCase> =
            arrayOf(
                Vertical,
                Horizontal
            )

        // Copied from AndroidComposeTestCaseRunner
        private val supportsRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        private val supportsMRenderNode = Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}

@OptIn(ExperimentalFoundationApi::class)
class LazyStaggeredGridScrollingTestCase(
    private val name: String,
    val isVertical: Boolean,
    val content: @Composable StaggeredGridRemeasureTestCase.(LazyStaggeredGridState) -> Unit
) {
    override fun toString(): String {
        return name
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val Vertical = LazyStaggeredGridScrollingTestCase(
    "Vertical",
    isVertical = true
) { state ->
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state,
        modifier = Modifier
            .requiredHeight(400.dp)
            .fillMaxWidth(),
        flingBehavior = NoFlingBehavior
    ) {
        items(2) {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val Horizontal = LazyStaggeredGridScrollingTestCase(
    "Horizontal",
    isVertical = false
) { state ->
    LazyHorizontalStaggeredGrid(
        rows = StaggeredGridCells.Fixed(2),
        state = state,
        modifier = Modifier
            .requiredWidth(400.dp)
            .fillMaxHeight(),
        flingBehavior = NoFlingBehavior
    ) {
        items(2) {
            FirstLargeItem()
        }
        items(items) {
            RegularItem()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
class StaggeredGridRemeasureTestCase(
    val firstItemIndex: Int = 0,
    val scrollUp: Boolean = false,
    val addNewItemOnToggle: Boolean,
    val content: @Composable StaggeredGridRemeasureTestCase.(LazyStaggeredGridState) -> Unit,
    val isVertical: Boolean,
    val usePointerInput: Boolean = false
) : LazyBenchmarkTestCase(isVertical, usePointerInput) {

    val items = List(300) { LazyItem(it) }

    private lateinit var state: LazyStaggeredGridState
    private var targetItemOffset = 0

    @Composable
    fun FirstLargeItem() {
        Box(Modifier.requiredSize(30.dp))
    }

    @Composable
    fun RegularItem() {
        Box(
            Modifier
                .requiredSize(20.dp)
                .background(Color.Red, RoundedCornerShape(8.dp))
        )
    }

    @Composable
    override fun Content() {
        val scrollBy = if (addNewItemOnToggle) {
            with(LocalDensity.current) { 15.dp.roundToPx() }
        } else {
            5
        } * if (scrollUp) -1 else 1
        targetItemOffset = if (scrollUp) {
            with(LocalDensity.current) { 20.dp.roundToPx() + scrollBy }
        } else {
            scrollBy
        }

        InitializeScrollHelper(scrollAmount = scrollBy)
        state = rememberLazyStaggeredGridState()
        content(state)
    }

    override fun beforeToggleCheck() {
        assertEquals(firstItemIndex, state.firstVisibleItemIndex)
        assertEquals(0, state.firstVisibleItemScrollOffset)
    }

    override fun afterToggleCheck() {
        assertEquals(
            if (scrollUp) firstItemIndex - 2 else firstItemIndex,
            state.firstVisibleItemIndex
        )
        assertEquals(targetItemOffset, state.firstVisibleItemScrollOffset)
    }

    override suspend fun programmaticScroll(amount: Int) {
        runBlocking {
            state.scrollBy(amount.toFloat())
        }
    }

    override fun setUp() {
        runBlocking {
            state.scrollToItem(firstItemIndex, 0)
        }
    }

    override fun tearDown() {
        // No Op
    }
}
