/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.pager

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class GlimmerPagerStateTest(private val config: GlimmerPagerParamConfig) :
    BaseParameterizedGlimmerPagerTest() {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun updateCurrentPage_updatesStateInsideScrollScope() = runTest {
        val state = GlimmerPagerState { 10 }

        rule.setContent {
            GlimmerHorizontalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
                Page("Page $page")
            }
        }

        runOnUiThread { state.scroll { with(state) { updateCurrentPage(4, 0.3f) } } }

        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(4)
        assertThat(state.currentPageOffsetFraction).isWithin(0.01f).of(0.3f)
    }

    @Test
    fun updateTargetPage_updatesTargetPageInsideScrollScope() = runTest {
        val state = GlimmerPagerState { 10 }
        var capturedTargetPage = -1

        rule.setContent {
            GlimmerHorizontalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
                Page("Page $page")
            }
        }

        runOnUiThread {
            state.scroll {
                with(state) {
                    updateTargetPage(7)
                    capturedTargetPage = targetPage
                }
            }
        }

        rule.waitForIdle()

        assertThat(capturedTargetPage).isEqualTo(7)
        assertThat(state.targetPage).isEqualTo(0)
    }

    @Test
    fun requestScrollToPage_changesCurrentPageOnNextMeasurement() = runTest {
        val state = GlimmerPagerState { 10 }

        rule.setContent {
            GlimmerParameterizedPager(
                config = config,
                modifier = Modifier.size(200.dp),
                state = state,
            ) { page ->
                Page("Page $page")
            }
        }

        rule.onNodeWithTag("Page 0").assertIsDisplayed()

        rule.runOnIdle { state.requestScrollToPage(4, 0.2f) }
        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(4)
        assertThat(state.currentPageOffsetFraction).isWithin(0.01f).of(0.2f)
    }

    @Test
    fun getOffsetDistanceInPages_calculatesCorrectly() = runTest {
        val state = GlimmerPagerState(currentPage = 2, pageCount = { 10 })

        assertThat(state.getOffsetDistanceInPages(5)).isEqualTo(3f)
        assertThat(state.getOffsetDistanceInPages(0)).isEqualTo(-2f)
        assertThat(state.getOffsetDistanceInPages(2)).isEqualTo(0f)
    }

    @Test
    fun dispatchPositiveRawDelta_consumesDeltaAndUpdatesOffset() = runTest {
        val state = GlimmerPagerState(currentPage = 5) { 10 }

        rule.setContent {
            GlimmerParameterizedPager(
                config = config,
                modifier = Modifier.size(200.dp),
                state = state,
            ) { page ->
                Page("Page $page")
            }
        }

        rule.onNodeWithTag("Page 5").assertIsDisplayed()

        val pageWidth = state.layoutInfo.pageSize
        assertThat(pageWidth).isGreaterThan(0)

        var consumed = 0f
        rule.runOnIdle {
            val delta = 10f
            consumed = state.dispatchRawDelta(delta)
        }
        rule.waitForIdle()

        assertThat(consumed).isGreaterThan(0f)
        assertThat(state.currentPageOffsetFraction).isGreaterThan(0f)
    }

    @Test
    fun dispatchNegativeRawDelta_consumesDeltaAndUpdatesOffset() = runTest {
        val state = GlimmerPagerState(currentPage = 5) { 10 }

        rule.setContent {
            GlimmerParameterizedPager(
                config = config,
                modifier = Modifier.size(200.dp),
                state = state,
            ) { page ->
                Page("Page $page")
            }
        }

        rule.onNodeWithTag("Page 5").assertIsDisplayed()

        val pageWidth = state.layoutInfo.pageSize
        assertThat(pageWidth).isGreaterThan(0)

        var consumed = 0f
        rule.runOnIdle {
            val delta = -10f
            consumed = state.dispatchRawDelta(delta)
        }
        rule.waitForIdle()

        assertThat(consumed).isLessThan(0f)
        assertThat(state.currentPageOffsetFraction).isLessThan(0f)
    }

    @Test
    fun scroll_withUserInputPriority_interruptsDefaultScroll() = runTest {
        val state = GlimmerPagerState { 10 }
        var defaultScrollInterrupted = false
        var userInputScrollCompleted = false

        rule.setContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.onNodeWithTag("Page 0").assertIsDisplayed()

        val scope = CoroutineScope(Dispatchers.Main)

        rule.runOnIdle {
            scope.launch {
                state.scroll(MutatePriority.Default) {
                    try {
                        delay(5000)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        defaultScrollInterrupted = true
                        throw e
                    }
                }
            }
        }

        // Allow some time for the first scroll to start and acquire the mutator
        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            scope.launch {
                state.scroll(MutatePriority.UserInput) { userInputScrollCompleted = true }
            }
        }

        rule.waitForIdle()

        assertThat(defaultScrollInterrupted).isTrue()
        assertThat(userInputScrollCompleted).isTrue()
    }

    @Test
    fun settledPage_updatesOnlyWhenScrollFinished() = runTest {
        val state = GlimmerPagerState { 10 }
        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            GlimmerParameterizedPager(
                config = config,
                modifier = Modifier.size(200.dp),
                state = state,
            ) { page ->
                Page("Page $page")
            }
        }

        rule.onNodeWithTag("Page 0").assertIsDisplayed()

        assertThat(state.settledPage).isEqualTo(0)

        rule.mainClock.autoAdvance = false

        scope.launch { state.animateScrollToPage(3) }

        rule.mainClock.advanceTimeBy(50) // Partially through animation

        assertThat(state.isScrollInProgress).isTrue()
        assertThat(state.currentPage).isGreaterThan(0)
        // Settled page must not update while scrolling
        assertThat(state.settledPage).isEqualTo(0)

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertThat(state.isScrollInProgress).isFalse()
        assertThat(state.currentPage).isEqualTo(3)
        // Settled page updates after idle
        assertThat(state.settledPage).isEqualTo(3)
    }

    @Test
    fun currentPageOffsetFraction_reflectsDragDistanceAccurately() = runTest {
        val state = GlimmerPagerState(currentPage = 5) { 10 }

        rule.setContent {
            GlimmerParameterizedPager(
                config = config,
                modifier = Modifier.size(200.dp),
                state = state,
            ) { page ->
                Page("Page $page")
            }
        }

        rule.onNodeWithTag("Page 5").assertIsDisplayed()

        val pageWidth = state.layoutInfo.pageSize
        assertThat(pageWidth).isGreaterThan(0)

        rule.mainClock.autoAdvance = false

        // Drag exactly 25% of the page width
        val fraction = 0.25f
        val delta = pageWidth.toFloat() * fraction

        rule.runOnIdle { state.dispatchRawDelta(delta) }

        rule.waitForIdle()

        assertThat(state.currentPageOffsetFraction.absoluteValue).isWithin(0.01f).of(fraction)
    }

    @Test
    fun targetPage_scrollBelowThreshold_revertsToCurrentPage() = runTest {
        val state = GlimmerPagerState(currentPage = 5) { 10 }

        rule.setContent {
            GlimmerParameterizedPager(
                config = config,
                modifier = Modifier.size(200.dp),
                state = state,
            ) { page ->
                Page("Page $page")
            }
        }

        rule.onNodeWithTag("Page 5").assertIsDisplayed()

        val pageWidth = state.layoutInfo.pageSize
        assertThat(pageWidth).isGreaterThan(0)

        // Simulate a tiny scroll forward and release
        val delta = pageWidth.toFloat() * 0.1f

        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.dispatchRawDelta(delta) }
        rule.waitForIdle()

        // Target page should still be 5 because 10% is below the typical snap threshold
        assertThat(state.targetPage).isEqualTo(5)
    }

    @Composable
    private fun Page(text: String, modifier: Modifier = Modifier) {
        Box(modifier.fillMaxSize().focusTarget().testTag(text)) { Text(text) }
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllGlimmerPagerTestParams
    }
}
