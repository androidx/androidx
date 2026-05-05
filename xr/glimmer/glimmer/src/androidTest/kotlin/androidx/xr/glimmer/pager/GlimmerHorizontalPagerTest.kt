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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.performIndirectMove
import androidx.xr.glimmer.performIndirectPress
import androidx.xr.glimmer.performIndirectRelease
import androidx.xr.glimmer.performIndirectSwipe
import androidx.xr.glimmer.setGlimmerThemeContent
import androidx.xr.glimmer.testutils.captureToImage
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@RunWith(Parameterized::class)
class GlimmerHorizontalPagerTest(private val config: GlimmerPagerParamConfig) :
    BaseParameterizedGlimmerPagerTest() {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun zeroPages_displaysNothing() {
        val state = GlimmerPagerState { 0 }
        rule.setGlimmerThemeContent { GlimmerParameterizedPager(config = config, state = state) {} }

        assertThat(rule.onNodeWithTag("pager").getBoundsInRoot().size).isEqualTo(DpSize.Zero)
        assertThat(state.pageCount).isEqualTo(0)
    }

    @Test
    fun singlePage_displaysFirstPage() {
        val state = GlimmerPagerState { 1 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { Page("Single Page") }
        }

        rule.onNodeWithText("Single Page").assertIsDisplayed()
        assertThat(state.currentPage).isEqualTo(0)
    }

    @Test
    fun multiplePages_displaysFirstPage() {
        val state = GlimmerPagerState { 5 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.onNodeWithText("Page 0").assertIsDisplayed()
        rule.onNodeWithText("Page 1").assertIsNotDisplayed()
        assertThat(state.currentPage).isEqualTo(0)
    }

    @Test
    fun multiplePages_customInitialPage_displaysRequestedPage() {
        val state = GlimmerPagerState(currentPage = 1) { 5 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.onNodeWithText("Page 0").assertIsNotDisplayed()
        rule.onNodeWithText("Page 1").assertIsDisplayed()
        rule.onNodeWithText("Page 2").assertIsNotDisplayed()
        assertThat(state.currentPage).isEqualTo(1)
    }

    @Test
    fun stateChanges_updatesPageCount() = runTest {
        var pageCount by mutableStateOf(3)
        val state = GlimmerPagerState { pageCount }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.runOnIdle { assertThat(state.pageCount).isEqualTo(3) }

        rule.runOnIdle { pageCount = 5 }

        rule.runOnIdle { assertThat(state.pageCount).isEqualTo(5) }
    }

    @Test
    fun stateChanges_decreasesPageCountBelowCurrentPage() = runTest {
        var pageCount by mutableStateOf(5)
        val state = GlimmerPagerState { pageCount }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        // Scroll to the last page
        runOnUiThread { state.scrollToPage(4) }
        rule.waitForIdle()
        assertThat(state.currentPage).isEqualTo(4)

        // Decrease page count to 3. Current page should be updated to 2.
        rule.runOnIdle { pageCount = 3 }
        rule.waitForIdle()

        assertThat(state.pageCount).isEqualTo(3)
        assertThat(state.currentPage).isEqualTo(2)
    }

    @Test
    fun scrollToPage_displaysCorrectPage() = runTest {
        val state = GlimmerPagerState { 5 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.onNodeWithText("Page 0").assertIsDisplayed()

        runOnUiThread { state.scrollToPage(2) }
        rule.waitForIdle()

        rule.onNodeWithText("Page 0").assertIsNotDisplayed()
        rule.onNodeWithText("Page 2").assertIsDisplayed()
        assertThat(state.currentPage).isEqualTo(2)
    }

    @Test
    fun userScrollEnabledIsOff_shouldNotAllowGestureScroll() {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state, userScrollEnabled = false) {
                page ->
                Page("Page $page")
            }
        }

        rule.waitForIdle()
        val pageSize = state.layoutInfo.pageSize
        assertThat(pageSize).isGreaterThan(0)

        rule.onRoot().performIndirectSwipe(rule, pageSize * 1.5f * config.scrollSign)

        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(0)
    }

    @Test
    fun userScrollEnabledIsOff_shouldAllowAnimationScroll() = runTest {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state, userScrollEnabled = false) {
                page ->
                Page("Page $page")
            }
        }

        runOnUiThread { state.scrollToPage(2) }

        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(2)
    }

    @Test
    fun userScrollEnabledIsOn_shouldAllowGestureScroll() {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }

        rule.waitForIdle()
        val pageSize = state.layoutInfo.pageSize
        assertThat(pageSize).isGreaterThan(0)

        rule.onRoot().performIndirectSwipe(rule, pageSize.toFloat() * config.scrollSign)

        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(1)
    }

    @Test
    fun stateRestoration_restoresParameters() {
        val restorationTester = StateRestorationTester(rule)
        var currentPage by mutableStateOf<Int?>(null)
        var pageCount by mutableStateOf(10)
        lateinit var state: GlimmerPagerState

        restorationTester.setContent {
            state = rememberGlimmerPagerState(initialPage = 0) { pageCount }

            GlimmerParameterizedPager(
                modifier = Modifier.size(100.dp),
                state = state,
                config = config,
            ) { page ->
                Page("Page $page")
            }

            LaunchedEffect(currentPage) { currentPage?.let { state.scrollToPage(it) } }
        }

        // Verify the initial state
        rule.onNodeWithText("Page 0").assertIsDisplayed()
        rule.onNodeWithText("Page 1").assertIsNotDisplayed()
        assertThat(state.currentPage).isEqualTo(0)

        // Scroll to a new page and set new page count
        rule.runOnIdle {
            currentPage = 3
            pageCount = 11
        }
        rule.waitForIdle()

        rule.onNodeWithText("Page 2").assertIsNotDisplayed()
        rule.onNodeWithText("Page 3").assertIsDisplayed()
        assertThat(state.currentPage).isEqualTo(3)
        assertThat(state.pageCount).isEqualTo(11)

        // Simulate recreation
        restorationTester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        // Verify the restored state
        rule.onNodeWithText("Page 2").assertIsNotDisplayed()
        rule.onNodeWithText("Page 3").assertIsDisplayed()
        assertThat(state.currentPage).isEqualTo(3)
        assertThat(state.pageCount).isEqualTo(11)
    }

    @Test
    fun swipeForward_largeDistance_stopsAtNextItem() {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }
        assertThat(state.currentPage).isEqualTo(0)
        val pageSize = state.layoutInfo.pageSize

        rule
            .onRoot()
            .performIndirectSwipe(rule = rule, distance = pageSize * 3f * config.scrollSign)
        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(1)
        rule.onNodeWithText("Page 0").assertIsNotDisplayed()
        rule.onNodeWithText("Page 1").assertIsDisplayed()
    }

    @Test
    fun swipeBackward_largeDistance_stopsAtPreviousItem() {
        val state = GlimmerPagerState(currentPage = 3) { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }
        assertThat(state.currentPage).isEqualTo(3)
        val pageSize = state.layoutInfo.pageSize

        rule
            .onRoot()
            .performIndirectSwipe(rule = rule, distance = -pageSize * 3f * config.scrollSign)
        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(2)
        rule.onNodeWithText("Page 3").assertIsNotDisplayed()
        rule.onNodeWithText("Page 2").assertIsDisplayed()
    }

    @Test
    fun flingForward_highVelocity_stopsAtNextItem() {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }
        assertThat(state.currentPage).isEqualTo(0)
        val pageSize = state.layoutInfo.pageSize

        // Fling forward with a fast swipe (short duration)
        rule
            .onRoot()
            .performIndirectSwipe(
                rule = rule,
                distance = pageSize * 2f * config.scrollSign,
                moveDuration = FlingDuration,
            )
        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(1)
        rule.onNodeWithText("Page 0").assertIsNotDisplayed()
        rule.onNodeWithText("Page 1").assertIsDisplayed()
    }

    @Test
    fun flingBackward_highVelocity_stopsAtPreviousItem() = runTest {
        val state = GlimmerPagerState(currentPage = 3) { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(config = config, state = state) { page -> Page("Page $page") }
        }
        assertThat(state.currentPage).isEqualTo(3)
        val pageSize = state.layoutInfo.pageSize

        // Fling backward with a fast swipe (short duration)
        rule
            .onRoot()
            .performIndirectSwipe(
                rule = rule,
                distance = -pageSize * 2f * config.scrollSign,
                moveDuration = FlingDuration,
            )
        rule.waitForIdle()

        assertThat(state.currentPage).isEqualTo(2)
        rule.onNodeWithText("Page 3").assertIsNotDisplayed()
        rule.onNodeWithText("Page 2").assertIsDisplayed()
    }

    @Test
    fun interactionSource_emitsDragInteractions() {
        val state = GlimmerPagerState(0) { 10 }
        lateinit var scope: CoroutineScope
        rule.setContent {
            scope = rememberCoroutineScope()
            GlimmerParameterizedPager(
                modifier = Modifier.size(100.dp),
                state = state,
                config = config,
            ) { page ->
                Page("Page $page")
            }
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { state.interactionSource.interactions.collect { interactions.add(it) } }

        rule.waitForIdle()
        val pageSize = state.layoutInfo.pageSize
        assertThat(pageSize).isGreaterThan(0)
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onRoot().performIndirectSwipe(rule, pageSize.toFloat() * config.scrollSign)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
        }
    }

    @Test
    fun edgeScrim_indirectPointerDrag_showsAndHidesScrim() {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(
                config = config,
                state = state,
                modifier = Modifier.background(Color.Red),
            ) {
                Box(Modifier.focusable().fillMaxSize().background(Color.Green))
            }
        }
        val x = 0
        val y = 0

        rule.waitForIdle()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the page's color before drag")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }

        val pressMotionEvent = rule.onRoot().performIndirectPress(rule)

        val moveDistance = state.layoutInfo.pageSize * 0.5f * config.scrollSign
        val moveMotionEvent =
            rule
                .onRoot()
                .performIndirectMove(
                    rule = rule,
                    distancePx = moveDistance,
                    previousMotionEvent = pressMotionEvent,
                )

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val scrimEdgePixelColor = pixels[x, y]

            assertWithMessage("Pixel should have the scrim's color during drag")
                .that(scrimEdgePixelColor.red)
                .isGreaterThan(0.9f)
            assertWithMessage("Pixel should have the scrim's color during drag")
                .that(scrimEdgePixelColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on drag")
                .that(scrimEdgePixelColor.blue)
                .isZero()
        }

        rule.onRoot().performIndirectRelease(rule = rule, moveMotionEvent)

        rule.waitForIdle()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the page's color after release")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }
    }

    @Test
    fun edgeScrim_touchDrag_showsAndHidesScrim() {
        val state = GlimmerPagerState { 10 }
        rule.setGlimmerThemeContent {
            GlimmerParameterizedPager(
                config = config,
                state = state,
                modifier = Modifier.background(Color.Red),
            ) {
                Box(Modifier.focusable().fillMaxSize().background(Color.Green))
            }
        }
        val x = 0
        val y = 0

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the page's color before drag")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }

        val moveDistance = state.layoutInfo.pageSize * 0.5f * config.scrollSign

        rule.onRoot().performTouchInput {
            down(Offset(x = 0f, y = centerY))
            moveTo(Offset(x = -moveDistance, y = centerY))
        }

        rule.waitForIdle()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            val scrimTopPixelColor = pixels[x, y]
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on drag")
                .that(scrimTopPixelColor.red)
                .isGreaterThan(0.9f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on drag")
                .that(scrimTopPixelColor.green)
                .isLessThan(0.1f)
            assertWithMessage("Pixel at ($x, $y) should have the scrim's color on drag")
                .that(scrimTopPixelColor.blue)
                .isZero()
        }

        rule.onRoot().performTouchInput { up() }

        rule.waitForIdle()

        rule.onRoot().captureToImage().run {
            val pixels = toPixelMap()
            assertWithMessage("Pixel at ($x, $y) should have the page's color after release")
                .that(pixels[x, y])
                .isEqualTo(Color.Green)
        }
    }

    @Composable
    private fun Page(text: String, modifier: Modifier = Modifier) {
        Box(modifier.fillMaxSize().focusable().testTag(text)) { Text(text) }
    }

    suspend fun runOnUiThread(action: suspend () -> Unit) {
        rule.waitForIdle()
        withContext(Dispatchers.Main) { action() }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun params() = AllGlimmerPagerTestParams
    }
}

/** A short swipe duration to trigger a fling. */
private const val FlingDuration: Long = 50L
