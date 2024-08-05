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

@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class LazyListScrollingBenchmark(private val testCase: LazyListScrollingTestCase) {
    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun scrollProgrammatically_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollProgrammatically_useStickyHeader() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                useStickyHeader = true
            )
        }
    }

    @Test
    fun scrollProgrammatically_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    @Test
    fun scrollProgrammatically_noNewItems_withoutKeys() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                useKeys = false
            )
        }
    }

    @Test
    fun scrollProgrammatically_newItemComposed_withoutKeys() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical,
                useKeys = false
            )
        }
    }

    @Test
    fun scrollViaPointerInput_noNewItems() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true
            )
        }
    }

    @Test
    fun scrollViaPointerInput_useStickyHeader() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
                addNewItemOnToggle = false,
                content = testCase.content,
                isVertical = testCase.isVertical,
                usePointerInput = true,
                useStickyHeader = true
            )
        }
    }

    @Test
    fun scrollViaPointerInput_newItemComposed() {
        benchmarkRule.toggleStateBenchmark {
            ListRemeasureTestCase(
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
            ListRemeasureTestCase(
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
            ListRemeasureTestCase(
                addNewItemOnToggle = true,
                content = testCase.content,
                isVertical = testCase.isVertical
            )
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<LazyListScrollingTestCase> = arrayOf(LazyColumn, LazyRow)

        // Copied from AndroidComposeTestCaseRunner
        private val supportsRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        private val supportsMRenderNode =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}

class LazyListScrollingTestCase(
    private val name: String,
    val isVertical: Boolean,
    val content:
        @Composable
        ListRemeasureTestCase.(LazyListState, useKeys: Boolean, useStickyHeader: Boolean) -> Unit
) {
    override fun toString(): String {
        return name
    }
}

private val LazyColumn =
    LazyListScrollingTestCase("LazyColumn", isVertical = true) { state, useKeys, useStickyHeader ->
        LazyColumn(
            state = state,
            modifier = Modifier.requiredHeight(400.dp).fillMaxWidth(),
            flingBehavior = NoFlingBehavior
        ) {
            if (useStickyHeader) {
                stickyHeader(key = if (useKeys) "header" else null) { FirstLargeItem() }
            } else {
                item(key = if (useKeys) "header" else null) { FirstLargeItem() }
            }

            items(
                items,
                key =
                    if (useKeys) {
                        { it.index }
                    } else {
                        null
                    }
            ) {
                RegularItem()
            }
        }
    }

private val LazyRow =
    LazyListScrollingTestCase("LazyRow", isVertical = false) { state, useKeys, useStickyHeader ->
        LazyRow(
            state = state,
            modifier = Modifier.requiredWidth(400.dp).fillMaxHeight(),
            flingBehavior = NoFlingBehavior
        ) {
            if (useStickyHeader) {
                stickyHeader(key = if (useKeys) "header" else null) { FirstLargeItem() }
            } else {
                item(key = if (useKeys) "header" else null) { FirstLargeItem() }
            }
            items(
                items,
                key =
                    if (useKeys) {
                        { it.index }
                    } else {
                        null
                    }
            ) {
                RegularItem()
            }
        }
    }

class ListRemeasureTestCase(
    val addNewItemOnToggle: Boolean,
    val content:
        @Composable
        ListRemeasureTestCase.(LazyListState, useKeys: Boolean, useStickyHeader: Boolean) -> Unit,
    val isVertical: Boolean,
    val usePointerInput: Boolean = false,
    val useKeys: Boolean = true,
    val useStickyHeader: Boolean = false
) : LazyBenchmarkTestCase(isVertical, usePointerInput) {

    val items = List(100) { LazyItem(it) }

    private lateinit var listState: LazyListState

    @Composable
    fun FirstLargeItem() {
        Box(Modifier.requiredSize(30.dp))
    }

    @Composable
    override fun Content() {
        val scrollBy =
            if (addNewItemOnToggle) {
                with(LocalDensity.current) { 15.dp.roundToPx() }
            } else {
                5
            }
        InitializeScrollHelper(scrollAmount = scrollBy)
        listState = rememberLazyListState()
        content(listState, useKeys, useStickyHeader)
    }

    @Composable
    fun RegularItem() {
        Box(Modifier.requiredSize(20.dp).background(Color.Red, RoundedCornerShape(8.dp)))
    }

    override fun beforeToggleCheck() {
        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals(0, listState.firstVisibleItemScrollOffset)
    }

    override fun afterToggleCheck() {
        assertEquals(0, listState.firstVisibleItemIndex)
        assertEquals(scrollingHelper.scrollAmount, listState.firstVisibleItemScrollOffset)
    }

    override suspend fun programmaticScroll(amount: Int) {
        runBlocking { listState.scrollBy(amount.toFloat()) }
    }

    override fun setUp() {
        runBlocking { listState.scrollToItem(0, 0) }
    }

    override fun tearDown() {
        // N/A
    }
}
