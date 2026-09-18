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

package androidx.wear.compose.material3

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PullToRefreshTest {

    @get:Rule val rule = createComposeRule()

    // ========================================================================
    // PullToRefreshBox Tests
    // ========================================================================

    @Test
    fun box_startRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContentWithTheme {
            PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {}
        }

        assertThat(state.distanceFraction).isEqualTo(1f)
    }

    @Test
    fun box_startNotRefreshing_updatesFraction() {
        val state = PullToRefreshState()
        rule.setContentWithTheme {
            PullToRefreshBox(isRefreshing = false, state = state, onRefresh = {}) {}
        }

        assertThat(state.distanceFraction).isEqualTo(0f)
    }

    @Test
    fun box_notVisible_notVisibleToSemantics() {
        val state = PullToRefreshState()
        rule.setContentWithTheme {
            PullToRefreshBox(
                modifier = Modifier.testTag(TEST_TAG),
                isRefreshing = false,
                state = state,
                onRefresh = {},
            ) {}
        }

        assertThat(state.distanceFraction).isEqualTo(0f)
        rule
            .onNodeWithTag(TEST_TAG)
            .onChild()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ProgressBarRangeInfo))
    }

    @Test
    fun box_visible_visibleToSemantics() {
        val state = PullToRefreshState()
        rule.setContentWithTheme {
            PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {}
        }

        assertThat(state.distanceFraction).isEqualTo(1f)
        rule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            .assertExists()
    }

    @Test
    fun box_startRefreshing_pull_isNoop() {
        val state = PullToRefreshState()
        rule.setContentWithTheme {
            PullToRefreshBox(isRefreshing = true, state = state, onRefresh = {}) {
                TestLazyList(Modifier.testTag(LAZY_TAG))
            }
        }

        rule.onNodeWithTag(LAZY_TAG).performTouchInput { swipeDownToThreshold() }
        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(1f) }
    }

    @Test
    fun box_startIdle_pull_triggersRefresh() {
        val state = PullToRefreshState()
        val isRefreshing = mutableStateOf(false)

        rule.setContentWithTheme {
            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                state = state,
                onRefresh = { isRefreshing.value = true },
            ) {
                TestLazyList(Modifier.testTag(LAZY_TAG))
            }
        }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(0f) }

        rule.onNodeWithTag(LAZY_TAG).performTouchInput { swipeDownToThreshold() }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(1f) }
    }

    @Test
    fun box_startIdle_pullBelowThreshold_doesNotTriggerRefresh() {
        val state = PullToRefreshState()
        var refreshCount = 0

        rule.setContentWithTheme {
            PullToRefreshBox(isRefreshing = false, state = state, onRefresh = { refreshCount++ }) {
                TestLazyList(Modifier.testTag(LAZY_TAG))
            }
        }

        rule.runOnIdle { assertThat(state.distanceFraction).isEqualTo(0f) }

        rule.onNodeWithTag(LAZY_TAG).performTouchInput {
            swipeDown(
                startY = top,
                endY = top + PullToRefreshDefaults.PositionalThreshold.toPx() / 2,
            )
        }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(0)
            assertThat(state.distanceFraction).isEqualTo(0f)
        }
    }

    @Test
    fun box_disabled_ignoresGesture() {
        val state = PullToRefreshState()
        var refreshCount = 0

        rule.setContentWithTheme {
            PullToRefreshBox(
                isRefreshing = false,
                state = state,
                enabled = false,
                onRefresh = { refreshCount++ },
            ) {
                TestLazyList(Modifier.testTag(LAZY_TAG))
            }
        }

        rule.onNodeWithTag(LAZY_TAG).performTouchInput { swipeDownToThreshold() }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(0)
            assertThat(state.distanceFraction).isEqualTo(0f)
        }
    }

    @Test
    fun box_customIndicator_isDisplayed() {
        val state = PullToRefreshState()
        rule.setContentWithTheme {
            PullToRefreshBox(
                isRefreshing = true,
                state = state,
                onRefresh = {},
                indicator = {
                    Text(
                        text = "CustomIndicator",
                        modifier = Modifier.testTag(CUSTOM_INDICATOR_TAG),
                    )
                },
            ) {}
        }

        rule.onNodeWithTag(CUSTOM_INDICATOR_TAG).assertExists()
    }

    @Test
    fun box_fling_isConsumed() {
        val state = PullToRefreshState()
        val isRefreshing = mutableStateOf(false)
        var remainingVelocity = Float.NaN

        rule.setContentWithTheme {
            PullToRefreshBox(
                isRefreshing = isRefreshing.value,
                state = state,
                onRefresh = { isRefreshing.value = true },
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .testTag("scrollable")
                        .verticalScroll(
                            state = rememberScrollState(),
                            flingBehavior =
                                object : FlingBehavior {
                                    override suspend fun ScrollScope.performFling(
                                        initialVelocity: Float
                                    ): Float {
                                        remainingVelocity = initialVelocity
                                        return initialVelocity
                                    }
                                },
                        )
                ) {
                    repeat(50) { Text("Lorem ipsum") }
                }
            }
        }

        rule.onNodeWithTag("scrollable").performTouchInput {
            // Do a swipe that doesn't trigger a refresh
            swipeDown(
                startY = top,
                endY = top + PullToRefreshDefaults.PositionalThreshold.toPx() / 2,
                100L,
            )
        }

        rule.runOnIdle {
            assertThat(state.distanceFraction).isEqualTo(0f)
            assertThat(abs(remainingVelocity)).isEqualTo(0f)
        }
    }

    @Test
    fun box_refreshingToNotRefreshing_resetsDistanceFraction() {
        val state = PullToRefreshState()
        val isRefreshing = mutableStateOf(true)

        rule.setContentWithTheme {
            PullToRefreshBox(isRefreshing = isRefreshing.value, state = state, onRefresh = {}) {
                TestLazyList(Modifier.testTag(LAZY_TAG))
            }
        }

        rule.waitForIdle()
        assertThat(state.distanceFraction).isEqualTo(1f)

        isRefreshing.value = false
        rule.waitForIdle()

        assertThat(state.distanceFraction).isEqualTo(0f)
    }

    // ========================================================================
    // PullToRefreshState Tests
    // ========================================================================

    @Test
    fun state_animateToThreshold_setsDistanceFractionTo1() {
        val state = PullToRefreshState()
        assertThat(state.distanceFraction).isEqualTo(0f)

        rule.setContent { LaunchedEffect(Unit) { state.animateToThreshold() } }
        rule.waitForIdle()

        assertThat(state.distanceFraction).isEqualTo(1f)
    }

    @Test
    fun state_animateToHidden_setsDistanceFractionTo0() {
        val state = PullToRefreshState()
        rule.setContent {
            LaunchedEffect(Unit) {
                state.snapTo(1f)
                state.animateToHidden()
            }
        }
        rule.waitForIdle()

        assertThat(state.distanceFraction).isEqualTo(0f)
    }

    @Test
    fun state_snapTo_setsDistanceFraction() {
        val state = PullToRefreshState()
        rule.setContent { LaunchedEffect(Unit) { state.snapTo(0.75f) } }
        rule.waitForIdle()

        assertThat(state.distanceFraction).isEqualTo(0.75f)
    }

    @Test
    fun state_refreshTrigger_onlyAfterThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state = PullToRefreshState()

        rule.setContentWithTheme {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            var isRefreshing by remember { mutableStateOf(false) }
            PullToRefreshBox(
                modifier = Modifier.testTag(TEST_TAG),
                isRefreshing = isRefreshing,
                state = state,
                onRefresh = {
                    isRefreshing = true
                    refreshCount++
                    isRefreshing = false
                },
            ) {
                TransformingLazyColumn { items(100) { Text("item $it") } }
            }
        }

        // Below threshold (accounting for DragMultiplier of 0.5f -> drag 2 * threshold)
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeDownFromContent(2 * positionalThreshold + touchSlop - 1f)
        }

        rule.runOnIdle {
            assertThat(refreshCount).isEqualTo(0)
            assertThat(state.distanceFraction).isEqualTo(0f)
        }

        // Past threshold
        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeDownFromContent(2 * positionalThreshold + touchSlop + 5f)
        }

        rule.runOnIdle { assertThat(refreshCount).isEqualTo(1) }
    }

    @Test
    fun state_progressAndVerticalOffset_scaleCorrectly_untilThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state = PullToRefreshState()

        rule.setContentWithTheme {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            PullToRefreshBox(
                modifier = Modifier.testTag(TEST_TAG),
                isRefreshing = false,
                onRefresh = { refreshCount++ },
                state = state,
            ) {
                TransformingLazyColumn { items(100) { Text("item $it") } }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(Offset(centerX, top))
            moveTo(Offset(centerX, top + positionalThreshold + touchSlop))
        }
        rule.waitForIdle()

        // Expected value given drag multiplier of 0.5f
        assertThat(state.distanceFraction).isWithin(0.01f).of(0.5f)
        assertThat(refreshCount).isEqualTo(0)

        rule.onNodeWithTag(TEST_TAG).performTouchInput { up() }
    }

    @Test
    fun state_progressAndPosition_scaleCorrectly_beyondThreshold() {
        var refreshCount = 0
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state = PullToRefreshState()

        rule.setContentWithTheme {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            PullToRefreshBox(
                modifier = Modifier.testTag(TEST_TAG),
                isRefreshing = false,
                onRefresh = { refreshCount++ },
                state = state,
            ) {
                TransformingLazyColumn { items(100) { Text("item $it") } }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(Offset(centerX, top))
            moveTo(Offset(centerX, top + positionalThreshold * 2 + touchSlop))
        }
        rule.waitForIdle()

        assertThat(state.distanceFraction).isWithin(0.01f).of(1f)
        assertThat(refreshCount).isEqualTo(0)

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            moveTo(Offset(centerX, top + 3 * positionalThreshold + touchSlop))
        }
        rule.waitForIdle()

        assertThat(state.distanceFraction).isWithin(0.1f).of(1.5f)

        rule.onNodeWithTag(TEST_TAG).performTouchInput { up() }
        rule.runOnIdle { assertThat(refreshCount).isEqualTo(1) }
    }

    @Test
    fun state_positionIsCapped() {
        var touchSlop = 0f
        var positionalThreshold = 0f
        val state = PullToRefreshState()

        rule.setContentWithTheme {
            touchSlop = LocalViewConfiguration.current.touchSlop
            positionalThreshold =
                with(LocalDensity.current) { PullToRefreshDefaults.PositionalThreshold.toPx() }
            PullToRefreshBox(
                modifier = Modifier.testTag(TEST_TAG),
                isRefreshing = false,
                onRefresh = {},
                state = state,
            ) {
                TransformingLazyColumn { items(100) { Text("item $it") } }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(Offset(centerX, top))
            moveTo(Offset(centerX, top + 10 * positionalThreshold + touchSlop))
        }
        rule.waitForIdle()

        assertThat(state.distanceFraction).isEqualTo(2f)

        rule.onNodeWithTag(TEST_TAG).performTouchInput { up() }
    }

    // ========================================================================
    // PullToRefreshIndicator Tests
    // ========================================================================

    @Test
    fun indicator_displayed_refreshing() {
        rule.setContentWithTheme(Modifier.fillMaxSize()) {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.testTag(INDICATOR_TAG),
                state = remember { PullToRefreshState() },
                isRefreshing = true,
            )
        }
        rule.onNodeWithTag(INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun indicator_displayed_notRefreshing() {
        rule.setContentWithTheme(Modifier.fillMaxSize()) {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.testTag(INDICATOR_TAG),
                state = remember { PullToRefreshState() },
                isRefreshing = false,
            )
        }
        rule.onNodeWithTag(INDICATOR_TAG).assertIsDisplayed()
    }

    @Test
    fun indicator_respects_changingOffset() {
        val containerSize = SpinnerContainerSize
        val verticalOffsetDp = 70.dp
        val state = PullToRefreshState()
        rule.setContentWithTheme(Modifier.fillMaxSize()) {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.testTag(INDICATOR_TAG),
                state = state,
                isRefreshing = true,
                maxDistance = verticalOffsetDp,
            )
        }

        runBlocking { state.snapTo(1f) }
        rule.waitForIdle()

        rule
            .onNodeWithTag(INDICATOR_TAG)
            .onChild()
            .assertTopPositionInRootIsEqualTo(verticalOffsetDp - containerSize)

        runBlocking { state.snapTo(0.5f) }
        rule.waitForIdle()

        rule
            .onNodeWithTag(INDICATOR_TAG)
            .onChild()
            .assertTopPositionInRootIsEqualTo(verticalOffsetDp * 0.5f - containerSize)
    }

    // Regression test for b/271777421
    @Test
    fun indicator_doesNotCapturePointerEvents() {
        var downEvent: PointerInputChange? = null

        rule.setContentWithTheme {
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture { downEvent = awaitFirstDown() }
                }
            )

            PullToRefreshDefaults.Indicator(
                modifier = Modifier.testTag(INDICATOR_TAG),
                state = remember { PullToRefreshState() },
                isRefreshing = true,
            )
        }

        rule.waitForIdle()

        rule.onNodeWithTag(INDICATOR_TAG).performClick()
        rule.runOnIdle {
            // The indicator should not have blocked its sibling (placed first, so below) from
            // seeing touch events.
            assertThat(downEvent).isNotNull()
        }
    }

    // ========================================================================
    // Test Helpers & Components
    // ========================================================================

    @Composable
    private fun TestLazyList(modifier: Modifier = Modifier) {
        TransformingLazyColumn(modifier) { items(50) { Text("Item $it") } }
    }

    private companion object {
        const val TEST_TAG = "PullToRefresh"
        const val LAZY_TAG = "lazy"
        const val CUSTOM_INDICATOR_TAG = "custom_indicator"
        const val INDICATOR_TAG = "pull-refresh-indicator"
    }
}

private fun TouchInjectionScope.swipeDownToThreshold() {
    val touchSlop = 18.dp
    swipeDown(
        startY = top,
        endY = top + PullToRefreshDefaults.PositionalThreshold.toPx() * 2 + touchSlop.toPx(),
    )
}

private fun TouchInjectionScope.swipeDownFromContent(distance: Float) {
    swipeDown(startY = top, endY = top + distance)
}
