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

package androidx.compose.material3

import android.os.Build
import androidx.compose.animation.DeferredAnimatedContent
import androidx.compose.animation.core.DeferredTransitionState
import androidx.compose.animation.core.rememberDeferredTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialTransitionsTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun scrimColor_lightSurface_usesLightAlpha() {
        var resolvedScrimColor = Color.Unspecified

        rule.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(surface = Color.White, scrim = Color.Black)
            ) {
                resolvedScrimColor = MaterialTransitionDefaults.scrimColor
            }
        }

        assertThat(resolvedScrimColor.alpha).isWithin(0.001f).of(0.2f)
    }

    @Test
    fun scrimColor_darkSurface_usesDarkAlpha() {
        var resolvedScrimColor = Color.Unspecified

        rule.setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(surface = Color.Black, scrim = Color.Black)
            ) {
                resolvedScrimColor = MaterialTransitionDefaults.scrimColor
            }
        }

        assertThat(resolvedScrimColor.alpha).isWithin(0.001f).of(0.8f)
    }

    @Test
    fun horizontalSharedAxis_predictiveBack_scalesAndPositionsInitialAndTargetContent() {
        val state = DeferredTransitionState(2)
        val backEvent =
            NavigationEvent(
                swipeEdge = NavigationEvent.EDGE_LEFT,
                progress = 0.5f,
                touchX = 100f,
                touchY = 300f,
            )
        var initialBounds = Rect.Zero
        var targetBounds = Rect.Zero
        val boxSizeDp = 200.dp

        rule.setContent {
            val transition = rememberDeferredTransition(state)
            val density = LocalDensity.current
            val scrimColor = Color.Black.copy(alpha = 0.2f)
            transition.DeferredAnimatedContent(
                transitionSpec = { MaterialTransitions.HorizontalSharedAxis.backwardTransform() },
                mutableTransformSpec = {
                    MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                        backEvent = { backEvent },
                        density = density,
                        scrimColor = scrimColor,
                    )
                },
            ) { screen ->
                Box(
                    Modifier.size(boxSizeDp).onGloballyPositioned { coords ->
                        if (screen == 2) {
                            initialBounds = coords.boundsInRoot()
                        } else if (screen == 1) {
                            targetBounds = coords.boundsInRoot()
                        }
                    }
                )
            }
        }

        val fullSizePx = with(rule.density) { boxSizeDp.toPx() }
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()

        assertThat(initialBounds.width).isLessThan(fullSizePx)
        assertThat(targetBounds.width).isWithin(1f).of(initialBounds.width - 0.05f * fullSizePx)
        assertThat(targetBounds.center.x).isLessThan(initialBounds.center.x)
    }

    @Test
    fun horizontalSharedAxis_predictiveBack_leftEdgeShiftsInitialContentRightComparedToRightEdge() {
        val state = DeferredTransitionState(2)
        var backEvent by
            mutableStateOf(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_RIGHT,
                    progress = 0.5f,
                    touchX = 100f,
                    touchY = 300f,
                )
            )
        var initialBounds = Rect.Zero
        val boxSizeDp = 200.dp

        rule.setContent {
            val transition = rememberDeferredTransition(state)
            val density = LocalDensity.current
            transition.DeferredAnimatedContent(
                transitionSpec = { MaterialTransitions.HorizontalSharedAxis.backwardTransform() },
                mutableTransformSpec = {
                    MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                        backEvent = { backEvent },
                        density = density,
                        scrimColor = Color.Transparent,
                    )
                },
            ) { screen ->
                Box(
                    Modifier.size(boxSizeDp).onGloballyPositioned { coords ->
                        if (screen == 2) {
                            initialBounds = coords.boundsInRoot()
                        }
                    }
                )
            }
        }

        val fullSizePx = with(rule.density) { boxSizeDp.toPx() }
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val rightEdgeCenterX = initialBounds.center.x

        rule.runOnIdle {
            backEvent =
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 100f,
                    touchY = 300f,
                )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val leftEdgeCenterX = initialBounds.center.x

        assertThat(rightEdgeCenterX).isWithin(1f).of(fullSizePx / 2f)
        assertThat(leftEdgeCenterX).isGreaterThan(rightEdgeCenterX)
    }

    @Test
    fun horizontalSharedAxis_predictiveBack_tracksVerticalTouchDeltaRelativeToInitialTouchY() {
        val state = DeferredTransitionState(2)
        var backEvent by
            mutableStateOf(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 50f,
                    touchY = 300f,
                )
            )
        var initialBounds = Rect.Zero
        var targetBounds = Rect.Zero
        val boxSizeDp = 200.dp

        rule.setContent {
            val transition = rememberDeferredTransition(state)
            val density = LocalDensity.current
            transition.DeferredAnimatedContent(
                transitionSpec = { MaterialTransitions.HorizontalSharedAxis.backwardTransform() },
                mutableTransformSpec = {
                    MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                        backEvent = { backEvent },
                        density = density,
                        scrimColor = Color.Transparent,
                    )
                },
            ) { screen ->
                Box(
                    Modifier.size(boxSizeDp).onGloballyPositioned { coords ->
                        if (screen == 2) {
                            initialBounds = coords.boundsInRoot()
                        } else if (screen == 1) {
                            targetBounds = coords.boundsInRoot()
                        }
                    }
                )
            }
        }

        val centerY = with(rule.density) { boxSizeDp.toPx() } / 2f
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val baselineInitialCenterY = initialBounds.center.y
        val baselineTargetCenterY = targetBounds.center.y

        rule.runOnIdle {
            backEvent =
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 50f,
                    touchY = 500f,
                )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val downwardInitialCenterY = initialBounds.center.y
        val downwardTargetCenterY = targetBounds.center.y

        rule.runOnIdle {
            backEvent =
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 50f,
                    touchY = 100f,
                )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val upwardInitialCenterY = initialBounds.center.y
        val upwardTargetCenterY = targetBounds.center.y

        assertThat(baselineInitialCenterY).isWithin(1f).of(centerY)
        assertThat(baselineTargetCenterY).isWithin(1f).of(centerY)
        assertThat(downwardInitialCenterY).isGreaterThan(centerY)
        assertThat(downwardTargetCenterY).isGreaterThan(centerY)
        assertThat(upwardInitialCenterY).isLessThan(centerY)
        assertThat(upwardTargetCenterY).isLessThan(centerY)
    }

    @Test
    fun horizontalSharedAxis_predictiveBackCommit_handsOffContinuouslyAndCompletes() {
        val state = DeferredTransitionState(2)
        var backEvent by
            mutableStateOf(
                NavigationEvent(
                    swipeEdge = NavigationEvent.EDGE_LEFT,
                    progress = 0.5f,
                    touchX = 100f,
                    touchY = 300f,
                )
            )
        var initialBounds = Rect.Zero
        var targetBounds = Rect.Zero
        val boxSizeDp = 200.dp

        rule.setContent {
            val transition = rememberDeferredTransition(state)
            val density = LocalDensity.current
            transition.DeferredAnimatedContent(
                transitionSpec = {
                    if (targetState > initialState) {
                        MaterialTransitions.HorizontalSharedAxis.forwardTransform()
                    } else {
                        MaterialTransitions.HorizontalSharedAxis.backwardTransform()
                    }
                },
                mutableTransformSpec = {
                    MaterialTransitions.HorizontalSharedAxis.predictiveBackTransform(
                        backEvent = { backEvent },
                        density = density,
                        scrimColor = Color.Transparent,
                    )
                },
            ) { screen ->
                Box(
                    Modifier.size(boxSizeDp).testTag("screen_$screen").onGloballyPositioned { coords
                        ->
                        if (screen == 2) {
                            initialBounds = coords.boundsInRoot()
                        } else if (screen == 1) {
                            targetBounds = coords.boundsInRoot()
                        }
                    }
                )
            }
        }

        val fullSizePx = with(rule.density) { boxSizeDp.toPx() }
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val initialBoundsBeforeCommit = initialBounds
        val targetBoundsBeforeCommit = targetBounds

        rule.runOnIdle { state.animateTo(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val initialBoundsOnHandoffFrame = initialBounds

        rule.mainClock.advanceTimeBy(32L)
        rule.waitForIdle()
        val initialBoundsDuringCommit = initialBounds
        val targetBoundsDuringCommit = targetBounds

        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertThat(initialBoundsOnHandoffFrame.left).isWithin(1f).of(initialBoundsBeforeCommit.left)
        assertThat(initialBoundsDuringCommit.center.x)
            .isGreaterThan(initialBoundsBeforeCommit.center.x)
        assertThat(targetBoundsDuringCommit.center.x)
            .isGreaterThan(targetBoundsBeforeCommit.center.x)
        assertThat(targetBoundsDuringCommit.center.x).isLessThan(fullSizePx / 2f)
        rule.onNodeWithTag("screen_2").assertDoesNotExist()
        rule.onNodeWithTag("screen_1").assertIsDisplayed()
        assertThat(targetBounds.width).isWithin(1f).of(fullSizePx)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun fadeThrough_predictiveBack_scalesInitialContentAndHidesTargetContentDuringDefer() {
        val state = DeferredTransitionState(2)
        val backEvent =
            NavigationEvent(
                swipeEdge = NavigationEvent.EDGE_LEFT,
                progress = 0.5f,
                touchX = 100f,
                touchY = 300f,
            )
        val exitingTabColor = Color.Red
        val enteringTabColor = Color.Green
        var initialBounds = Rect.Zero
        var targetBounds = Rect.Zero
        val boxSizeDp = 200.dp

        rule.setContent {
            val transition = rememberDeferredTransition(state)
            val density = LocalDensity.current
            transition.DeferredAnimatedContent(
                modifier = Modifier.testTag("container"),
                transitionSpec = { MaterialTransitions.FadeThrough.transform() },
                mutableTransformSpec = {
                    MaterialTransitions.FadeThrough.predictiveBackTransform(
                        backEvent = { backEvent },
                        density = density,
                    )
                },
            ) { tab ->
                Box(
                    Modifier.size(boxSizeDp)
                        .background(if (tab == 1) enteringTabColor else exitingTabColor)
                        .testTag("tab_$tab")
                        .onGloballyPositioned { coords ->
                            if (tab == 2) {
                                initialBounds = coords.boundsInRoot()
                            } else if (tab == 1) {
                                targetBounds = coords.boundsInRoot()
                            }
                        }
                )
            }
        }

        val fullSizePx = with(rule.density) { boxSizeDp.toPx() }
        rule.mainClock.autoAdvance = false

        rule.runOnIdle { state.defer(1) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        val deferredInitialBounds = initialBounds
        // Entering tab (tab 1) is placed on top of exiting tab (tab 2), but has alpha = 0f during
        // defer so the center pixel shows the exiting tab.
        val deferredCenterColor =
            rule.onNodeWithTag("container").captureToImage().toPixelMap().let {
                it[it.width / 2, it.height / 2]
            }

        rule.runOnIdle { state.animateTo(1) }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        val settledCenterColor =
            rule.onNodeWithTag("container").captureToImage().toPixelMap().let {
                it[it.width / 2, it.height / 2]
            }

        assertThat(deferredInitialBounds.width).isLessThan(fullSizePx)
        assertThat(deferredInitialBounds.center.x).isWithin(1f).of(fullSizePx / 2f)
        assertThat(deferredCenterColor).isEqualTo(exitingTabColor)
        assertThat(settledCenterColor).isEqualTo(enteringTabColor)
        rule.onNodeWithTag("tab_2").assertDoesNotExist()
        rule.onNodeWithTag("tab_1").assertIsDisplayed()
        assertThat(targetBounds.width).isWithin(1f).of(fullSizePx)
    }
}
