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

package androidx.wear.compose.material3

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.RevealActionType.Companion.None
import androidx.wear.compose.material3.RevealActionType.Companion.PrimaryAction
import androidx.wear.compose.material3.RevealActionType.Companion.SecondaryAction
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealDirection.Companion.RightToLeft
import androidx.wear.compose.material3.RevealState.SingleSwipeCoordinator
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealed
import androidx.wear.compose.material3.RevealValue.Companion.LeftRevealing
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealed
import androidx.wear.compose.material3.RevealValue.Companion.RightRevealing
import androidx.wear.compose.material3.SwipeToRevealDefaults.DoubleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.SingleActionAnchorWidth
import androidx.wear.compose.material3.SwipeToRevealDefaults.bidirectionalGestureInclusion
import androidx.wear.compose.material3.SwipeToRevealDefaults.gestureInclusion
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import androidx.wear.compose.materialcore.LARGE_SCREEN_WIDTH_DP
import androidx.wear.widget.SwipeDismissFrameLayout
import com.google.common.truth.StringSubject
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SwipeToRevealTest {
    @get:Rule val rule = createComposeRule()

    @Before
    fun setUp() {
        SingleSwipeCoordinator.lastUpdatedState.set(null)
    }

    @Test
    fun onStateChangeToRevealed_performsHaptics() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))
        lateinit var revealState: RevealState
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            revealState = rememberRevealState(initialValue = RightRevealing)
            coroutineScope = rememberCoroutineScope()
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = revealState,
                )
            }
        }

        rule.runOnIdle { assertThat(results).isEmpty() }

        rule.runOnIdle { coroutineScope.launch { revealState.animateTo(RightRevealed) } }

        rule.runOnIdle {
            assertThat(results).hasSize(1)
            assertThat(results).containsKey(HapticFeedbackType.GestureThresholdActivate)
            assertThat(results[HapticFeedbackType.GestureThresholdActivate]).isEqualTo(1)
        }
    }

    @Test
    fun onStateChangeToLeftRevealed_performsHaptics() {
        val results = mutableMapOf<HapticFeedbackType, Int>()
        val haptics = hapticFeedback(collectResultsFromHapticFeedback(results))
        lateinit var revealState: RevealState
        lateinit var coroutineScope: CoroutineScope

        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                revealState = rememberRevealState(initialValue = LeftRevealing)
                coroutineScope = rememberCoroutineScope()
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = revealState,
                    revealDirection = Bidirectional,
                )
            }
        }

        rule.runOnIdle { assertThat(results).isEmpty() }

        rule.runOnIdle { coroutineScope.launch { revealState.animateTo(LeftRevealed) } }

        rule.runOnIdle {
            assertThat(results).hasSize(1)
            assertThat(results).containsKey(HapticFeedbackType.GestureThresholdActivate)
            assertThat(results[HapticFeedbackType.GestureThresholdActivate]).isEqualTo(1)
        }
    }

    @Test
    fun onStart_defaultState_keepsContentToRight() {
        rule.setContent {
            SwipeToRevealWithDefaults { DefaultContent(modifier = Modifier.testTag(TEST_TAG)) }
        }

        rule.onNodeWithTag(TEST_TAG).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun onCovered_doesNotDrawActions() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(TEST_TAG))
                }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun onRightRevealing_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(TEST_TAG))
                },
                revealState = rememberRevealState(initialValue = RightRevealing),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onRightRevealing_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                revealState = rememberRevealState(initialValue = RightRevealing),
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onLeftRevealing_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(TEST_TAG))
                },
                revealDirection = Bidirectional,
                revealState = rememberRevealState(initialValue = LeftRevealing),
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onLeftRevealing_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                revealState = rememberRevealState(initialValue = LeftRevealing),
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipe_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipe_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipeRight_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = centerX / 4, y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onSwipeRight_twoActions_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(modifier = Modifier.testTag(PRIMARY_ACTION_TAG))
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(modifier = Modifier.testTag(SECONDARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = centerX / 4, y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onFullSwipe_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                undoPrimaryAction = {
                    DefaultUndoActionButton(modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG))
                },
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        rule.waitForIdle()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onFullSwipeRight_drawsAction() {
        rule.setContent {
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                undoPrimaryAction = {
                    DefaultUndoActionButton(modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG))
                },
                revealDirection = Bidirectional,
                enableTouchSlop = false,
            )
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        rule.waitForIdle()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun onFullSwipeRight_noSwipe() {
        verifyGesture(expectedRevealValue = Covered) { swipeRight() }
    }

    @Test
    fun onFullSwipeRight_bidirectionalGestureInclusion_noSwipe() {
        verifyGesture(expectedRevealValue = Covered, bidirectionalGestureInclusion = true) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeLeft_stateToSwiped() {
        verifyGesture(expectedRevealValue = RightRevealed) { swipeLeft() }
    }

    @Test
    fun onFullSwipeLeft_bidirectionalGestureInclusion_stateToSwiped() {
        verifyGesture(expectedRevealValue = RightRevealed, bidirectionalGestureInclusion = true) {
            swipeLeft()
        }
    }

    @Test
    fun onFullSwipeRight_bidirectionalNonBidirectionalGestureInclusion_noSwipe() {
        verifyGesture(
            expectedRevealValue = Covered,
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeRight_bidirectional_stateToSwiped() {
        verifyGesture(expectedRevealValue = LeftRevealed, revealDirection = Bidirectional) {
            swipeRight()
        }
    }

    @Test
    fun onPartialSwipeRight_bidirectionalNonBidirectionalGestureInclusion_stateToSwiped() {
        verifyGesture(
            expectedRevealValue = LeftRevealed,
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        ) { density ->
            swipeRight(startX = LARGE_SCREEN_WIDTH_DP * density / 4f)
        }
    }

    @Test
    fun onPartialSwipeRight_bidirectional_stateToSwiped() {
        verifyGesture(expectedRevealValue = LeftRevealed, revealDirection = Bidirectional) { density
            ->
            swipeRight(startX = LARGE_SCREEN_WIDTH_DP * density / 4f)
        }
    }

    @Test
    fun onFullSwipeLeft_bidirectionalNonBidirectionalGestureInclusion_stateToSwiped() {
        verifyGesture(
            expectedRevealValue = RightRevealed,
            revealDirection = Bidirectional,
            bidirectionalGestureInclusion = false,
        ) {
            swipeLeft()
        }
    }

    @Test
    fun onFullSwipeLeft_bidirectional_stateToSwiped() {
        verifyGesture(expectedRevealValue = RightRevealed, revealDirection = Bidirectional) {
            swipeLeft()
        }
    }

    @Test
    fun onAboveVelocityThresholdSmallDistanceSwipe_stateToRevealing() {
        verifyGesture(expectedRevealValue = RightRevealing, enableTouchSlop = false) { density ->
            swipeLeft(endX = (LARGE_SCREEN_WIDTH_DP - 32) * density, durationMillis = 30L)
        }
    }

    @Test
    fun onBelowVelocityThresholdSmallDistanceSwipe_noSwipe() {
        verifyGesture(expectedRevealValue = Covered, enableTouchSlop = false) { density ->
            swipeLeft(endX = (LARGE_SCREEN_WIDTH_DP - 32) * density, durationMillis = 1000L)
        }
    }

    @Test
    fun onAboveVelocityThresholdLongDistanceSwipe_stateToRevealing() {
        verifyGesture(expectedRevealValue = RightRevealing, enableTouchSlop = false) { density ->
            swipeLeft(endX = (LARGE_SCREEN_WIDTH_DP - 64) * density, durationMillis = 30L)
        }
    }

    @Test
    fun onBelowVelocityThresholdLongDistanceSwipe_stateToRevealing() {
        verifyGesture(expectedRevealValue = RightRevealing, enableTouchSlop = false) { density ->
            swipeLeft(endX = (LARGE_SCREEN_WIDTH_DP - 64) * density, durationMillis = 1000L)
        }
    }

    @Test
    fun onPartialSwipe_lastStateRevealing_resetsLastState() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }
            },
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onPartialSwipe_whenLastStateRevealed_doesNotReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealed (full screen swipe)
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput { swipeLeft() }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }
            },
            assertions = { revealStateOne, revealStateTwo ->
                // assert that state does not reset
                assertEquals(RightRevealed, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onPartialSwipeRight_lastStateRevealing_resetsLastState() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onPartialSwipeRight_whenLastStateRevealed_doesNotReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealed (full screen swipe)
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput { swipeRight() }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                // assert that state does not reset
                assertEquals(LeftRevealed, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onPartialSwipeRightAndLeft_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onPartialSwipeLeftAndRight_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actions = { revealStateOne, revealStateTwo, density ->
                // swipe the first S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
                    swipeLeftToRevealing(density)
                }

                // swipe the second S2R to Revealing state
                rule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
                    swipeRightToRevealing(density)
                }
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnap_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(RightRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(RightRevealing)
            },
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnap_sameComponents_doesNotReset() {
        val lastValue = RightRevealed
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                revealStateOne.snapTo(RightRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same component
            },
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(lastValue, revealStateOne.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnapRight_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(LeftRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(LeftRevealing)
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnapRight_sameComponents_doesNotReset() {
        val lastValue = LeftRevealed
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                revealStateOne.snapTo(LeftRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same component
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(lastValue, revealStateOne.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnapRightAndLeft_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(RightRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(LeftRevealing)
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(LeftRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onMultiSnapLeftAndRight_differentComponents_lastOneGetsReset() {
        verifyStateMultipleSwipeToReveal(
            actionsSuspended = { revealStateOne, revealStateTwo, density ->
                // First change
                revealStateOne.snapTo(LeftRevealing)
                // Second change, in a different component
                revealStateTwo.snapTo(RightRevealing)
            },
            revealDirection = Bidirectional,
            assertions = { revealStateOne, revealStateTwo ->
                assertEquals(Covered, revealStateOne.currentValue)
                assertEquals(RightRevealing, revealStateTwo.currentValue)
            },
        )
    }

    @Test
    fun onSecondaryActionClick_hasUndo_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = SecondaryAction,
            initialRevealValue = RightRevealing,
            nodeTagToPerformClick = SECONDARY_ACTION_TAG,
        )

    @Test
    fun onPrimaryActionClick_hasUndo_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = PrimaryAction,
            initialRevealValue = RightRevealing,
        )

    @Test
    fun onSecondaryActionClick_withoutUndo_setsLastClickActionNone() =
        verifyLastClickAction(
            initialRevealValue = RightRevealing,
            nodeTagToPerformClick = SECONDARY_ACTION_TAG,
            hasSecondaryUndoAction = false,
            expectedClickType = None,
        )

    @Test
    fun onPrimaryActionClick_withoutUndo_setsLastClickActionPrimary() =
        verifyLastClickAction(
            initialRevealValue = RightRevealing,
            hasPrimaryUndoAction = false,
            expectedClickType = PrimaryAction,
        )

    @Test
    fun onFullSwipe_withoutUndo_setsLastClickActionPrimary() {
        verifyLastClickAction(
            initialRevealValue = Covered,
            shouldFullySwipe = true,
            hasPrimaryUndoAction = false,
            expectedClickType = PrimaryAction,
        )
    }

    @Test
    fun onFullSwipe_hasUndo_setsLastClickActionPrimary() {
        verifyLastClickAction(
            initialRevealValue = Covered,
            shouldFullySwipe = true,
            hasPrimaryUndoAction = true,
            expectedClickType = PrimaryAction,
        )
    }

    @Test
    fun onUndoActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = None,
            initialRevealValue = RightRevealed,
            nodeTagToPerformClick = UNDO_PRIMARY_ACTION_TAG,
        )

    @Test
    fun onUndoActionClick_setsCorrectCurrentValue() {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(RightRevealed)
            SwipeToRevealWithDefaults(
                revealState = revealState,
                undoPrimaryAction = {
                    DefaultUndoActionButton(modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG))
                },
            )
        }
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(Covered, revealState.currentValue) }
    }

    @Test
    fun onFullSwipeRight_wrappedInSwipeToDismissBox_navigationSwipe() {
        verifyGesture(
            expectedRevealValue = Covered,
            wrappedInSwipeToDismissBox = true,
            expectedSwipeToDismissBoxDismissed = true,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeRight_wrappedInSTDBBidirectionalGI_noSwipe() {
        verifyGesture(
            expectedRevealValue = Covered,
            bidirectionalGestureInclusion = true,
            wrappedInSwipeToDismissBox = true,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onFullSwipeRight_wrappedInSwipeToDismissBoxAndRightRevealing_stateToCovered() {
        verifyGesture(
            initialRevealValue = RightRevealing,
            expectedRevealValue = Covered,
            wrappedInSwipeToDismissBox = true,
        ) {
            swipeRight()
        }
    }

    @Test
    fun onSwipeRightFromOutsideEdge_wrappedInSwipeToDismissBoxAndRightRevealing_stateToCovered() {
        verifyGesture(
            initialRevealValue = RightRevealing,
            expectedRevealValue = Covered,
            wrappedInSwipeToDismissBox = true,
            enableTouchSlop = false,
        ) {
            swipeRight(startX = width / 2f)
        }
    }

    @Test
    fun onSwipeRight_withPartiallyRevealedState_stateToCovered() {
        verifyGesture(initialRevealValue = RightRevealing, expectedRevealValue = Covered) {
            swipeRight(startX = width / 2f)
        }
    }

    @Test
    fun onPrimaryActionClick_doesNotTriggerOnSwipePrimaryAction() {
        var onPrimaryActionClick = false
        var onSwipePrimaryAction = false
        lateinit var revealState: RevealState
        var density = 0f
        rule.setContent {
            with(LocalDensity.current) { density = this.density }
            ScreenConfiguration(screenSizeDp = LARGE_SCREEN_WIDTH_DP) {
                revealState = rememberRevealState(Covered)
                SwipeToRevealWithDefaults(
                    modifier = Modifier.testTag(TEST_TAG),
                    onSwipePrimaryAction = { onSwipePrimaryAction = true },
                    primaryAction = {
                        DefaultPrimaryActionButton(
                            modifier = Modifier.testTag(PRIMARY_ACTION_TAG),
                            onClick = { onPrimaryActionClick = true },
                        )
                    },
                    revealState = revealState,
                )
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeftToRevealing(density) }
        rule.waitForIdle()
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).performClick()

        rule.runOnIdle {
            assertTrue(onPrimaryActionClick)
            assertFalse(onSwipePrimaryAction)
        }
    }

    @Test
    fun onFullSwipe_doesNotTriggerPrimaryActionClick() {
        var onPrimaryActionClick = false
        var onSwipePrimaryAction = false
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(Covered)
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                onSwipePrimaryAction = { onSwipePrimaryAction = true },
                primaryAction = {
                    DefaultPrimaryActionButton(onClick = { onPrimaryActionClick = true })
                },
                revealState = revealState,
            )
        }
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertTrue(onSwipePrimaryAction)
            assertFalse(onPrimaryActionClick)
        }
    }

    @Test
    fun onPartiallySwipe_primaryActionIsBigger_actionsCenteredVerticallyAligned() {
        verifyActionCenterVerticalAligned(70.dp, 60.dp)
    }

    @Test
    fun onPartiallySwipe_secondaryActionIsBigger_actionsCenteredVerticallyAligned() {
        verifyActionCenterVerticalAligned(60.dp, 70.dp)
    }

    private fun verifyActionCenterVerticalAligned(
        primaryActionHeight: Dp,
        secondaryActionHeight: Dp,
    ) {
        rule.setContent {
            SwipeToRevealWithDefaults(
                primaryAction = {
                    DefaultPrimaryActionButton(
                        modifier =
                            Modifier.heightIn(primaryActionHeight).testTag(PRIMARY_ACTION_TAG)
                    )
                },
                secondaryAction = {
                    DefaultSecondaryActionButton(
                        modifier =
                            Modifier.heightIn(secondaryActionHeight).testTag(SECONDARY_ACTION_TAG)
                    )
                },
                revealState = rememberRevealState(initialValue = RightRevealing),
            ) {
                DefaultContent(modifier = Modifier.height(100.dp))
            }
        }

        rule.waitForIdle()
        // get centerY of actions
        val primaryActionCenterY =
            with(rule.onNodeWithTag(PRIMARY_ACTION_TAG).getUnclippedBoundsInRoot()) {
                    top + (bottom - top) / 2f
                }
                .value
        val secondaryActionCenterY =
            with(rule.onNodeWithTag(SECONDARY_ACTION_TAG).getUnclippedBoundsInRoot()) {
                    top + (bottom - top) / 2f
                }
                .value

        // actions are center vertical aligned when their centerY
        assertEquals(0f, abs(primaryActionCenterY - secondaryActionCenterY), 0.1f)
    }

    @Test
    fun onCreation_rtlWithLeftRevealing_throwsHelpfulMessage() {
        assertThrowsHelpfulMessage(initialValue = LeftRevealing)
    }

    @Test
    fun onCreation_rtlWithLeftRevealed_throwsHelpfulMessage() {
        assertThrowsHelpfulMessage(initialValue = LeftRevealed)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun onSwipeLeft_withSlcInSwipeToDismissLayout_ableToSwipeRightToCovered() {
        val swipeItemText = "SWIPE"

        val androidTestRule =
            rule as AndroidComposeTestRule<ActivityScenarioRule<Activity>, Activity>
        lateinit var revealState: RevealState
        var density = 0f
        androidTestRule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(context = activity)
            val swipeLayout = SwipeDismissFrameLayout(activity)
            swipeLayout.tag = swipeItemText
            swipeLayout.addView(composeView)
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                with(LocalDensity.current) { density = this.density }
                revealState = rememberRevealState()
                ScalingLazyColumn(
                    state = rememberScalingLazyListState(initialCenterItemIndex = 1)
                ) {
                    item { SwipeToRevealWithTouchSlop() }
                    item { SwipeToRevealWithTouchSlop(TEST_TAG, revealState) }
                    item { SwipeToRevealWithTouchSlop() }
                }
            }

            activity.setContentView(swipeLayout)
        }
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            swipeLeftToRevealing(density, DoubleActionAnchorWidth)
        }

        rule.waitForIdle()

        rule.runOnIdle { assertEquals(RightRevealing, revealState.currentValue) }

        // Swipe on a view rather than node in order to simulate user behavior. Swiping on a node
        // skips a big part of touch logic, specifically global swipe to dismiss
        onView(withTagValue(equalTo(swipeItemText)))
            .perform(
                actionWithAssertions(
                    GeneralSwipeAction(
                        Swipe.FAST,
                        GeneralLocation.translate(GeneralLocation.CENTER_LEFT, 0.2f, 0f),
                        GeneralLocation.CENTER_RIGHT,
                        Press.FINGER,
                    )
                )
            )

        rule.runOnIdle { assertEquals(Covered, revealState.currentValue) }
    }

    @Test()
    fun onRtl_withPartiallyRevealedState_animateToLeftRevealing_throwsException() {
        verifyAnimateToIllegalState(LeftRevealing)
    }

    @Test()
    fun onRtl_withPartiallyRevealedState_animateToLeftRevealed_throwsException() {
        verifyAnimateToIllegalState(LeftRevealed)
    }

    @Test()
    fun onRtl_withNoPartiallyRevealedState_animateToRightRevealing_throwsException() {
        verifyAnimateToIllegalState(targetValue = RightRevealing, hasPartiallyRevealedState = false)
    }

    @Test()
    fun onBiDirectional_withNoPartiallyRevealedState_animateToLeftRevealing_throwsException() {
        verifyAnimateToIllegalState(
            targetValue = LeftRevealing,
            revealDirection = Bidirectional,
            hasPartiallyRevealedState = false,
        )
    }

    @Test()
    fun onBiDirectional_withNoPartiallyRevealedState_animateToRightRevealing_throwsException() {
        verifyAnimateToIllegalState(
            targetValue = RightRevealing,
            revealDirection = Bidirectional,
            hasPartiallyRevealedState = false,
        )
    }

    private fun verifyAnimateToIllegalState(
        targetValue: RevealValue,
        revealDirection: RevealDirection = RightToLeft,
        hasPartiallyRevealedState: Boolean = true,
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState()
            SwipeToRevealWithDefaults(
                revealState = revealState,
                revealDirection = revealDirection,
                hasPartiallyRevealedState = hasPartiallyRevealedState,
            )
        }

        assertThrows(IllegalStateException::class.java) {
            // If use coroutineScope.launch, below block will run in parallel with test code, and we
            // won't able to catch exception.
            runBlocking { revealState.animateTo(targetValue) }
        }
    }

    @Composable
    fun SwipeToRevealWithTouchSlop(
        text: String = "other-item",
        revealState: RevealState = rememberRevealState(),
    ) {
        CustomTouchSlopProvider(32f) {
            SwipeToReveal(
                modifier = Modifier.semantics { testTag = text },
                primaryAction = {
                    PrimaryActionButton(
                        onClick = {},
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = {},
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = {},
                        icon = { Icon(Icons.Filled.MoreVert, contentDescription = "Duplicate") },
                    )
                },
                undoPrimaryAction = {
                    UndoActionButton(onClick = {}, text = { Text("Undo Delete") })
                },
                revealDirection = RightToLeft,
                revealState = revealState,
            ) {
                Button({}, Modifier.fillMaxWidth().padding(horizontal = 4.dp)) { Text(text) }
            }
        }
    }

    private fun assertThrowsHelpfulMessage(initialValue: RevealValue) {
        val assertionError =
            assertThrows(IllegalArgumentException::class.java) {
                rule.setContent {
                    SwipeToRevealWithDefaults(
                        revealState = rememberRevealState(initialValue = initialValue)
                    )
                }
            }

        fun StringSubject.containsAll(vararg strings: CharSequence) = strings.forEach(::contains)

        assertThat(assertionError)
            .hasMessageThat()
            .containsAll("LeftRevealing", "LeftRevealed", "RightToLeft")
    }

    private fun verifyLastClickAction(
        expectedClickType: RevealActionType,
        initialRevealValue: RevealValue,
        nodeTagToPerformClick: String = PRIMARY_ACTION_TAG,
        shouldFullySwipe: Boolean = false,
        hasPrimaryUndoAction: Boolean = true,
        hasSecondaryUndoAction: Boolean = true,
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(initialRevealValue)
            SwipeToRevealWithDefaults(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    DefaultPrimaryActionButton(
                        modifier = Modifier.testTag(PRIMARY_ACTION_TAG),
                        onClick = {},
                    )
                },
                revealState = revealState,
                secondaryAction = {
                    DefaultSecondaryActionButton(
                        modifier = Modifier.testTag(SECONDARY_ACTION_TAG),
                        onClick = {},
                    )
                },
                undoPrimaryAction =
                    if (hasPrimaryUndoAction) {
                        @Composable {
                            DefaultUndoActionButton(
                                modifier = Modifier.testTag(UNDO_PRIMARY_ACTION_TAG),
                                onClick = {},
                            )
                        }
                    } else null,
                undoSecondaryAction =
                    if (hasSecondaryUndoAction) {
                        @Composable {
                            DefaultUndoActionButton(
                                modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG),
                                onClick = {},
                            )
                        }
                    } else null,
            )
        }
        rule.waitForIdle()
        if (shouldFullySwipe) {
            rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }
        } else {
            rule.onNodeWithTag(nodeTagToPerformClick).performClick()
        }
        rule.runOnIdle { assertEquals(expectedClickType, revealState.lastActionType) }
    }

    private fun verifyGesture(
        initialRevealValue: RevealValue = Covered,
        expectedRevealValue: RevealValue,
        expectedFullSwipeTriggered: Boolean =
            (expectedRevealValue == RightRevealed || expectedRevealValue == LeftRevealed),
        revealDirection: RevealDirection = RightToLeft,
        bidirectionalGestureInclusion: Boolean = revealDirection == Bidirectional,
        enableTouchSlop: Boolean = true,
        wrappedInSwipeToDismissBox: Boolean = false,
        expectedSwipeToDismissBoxDismissed: Boolean = false,
        gesture: TouchInjectionScope.(density: Float) -> Unit,
    ) {
        var onFullSwipeTriggerCounter = 0
        var onSwipeToDismissBoxDismissed = false
        lateinit var revealState: RevealState
        var density = 0f

        rule.setContent {
            revealState = rememberRevealState(initialValue = initialRevealValue)

            val content =
                @Composable {
                    with(LocalDensity.current) { density = this.density }

                    ScreenConfiguration(screenSizeDp = LARGE_SCREEN_WIDTH_DP) {
                        SwipeToRevealWithDefaults(
                            modifier = Modifier.testTag(TEST_TAG),
                            onSwipePrimaryAction = { onFullSwipeTriggerCounter++ },
                            revealState = revealState,
                            revealDirection = revealDirection,
                            gestureInclusion =
                                if (bidirectionalGestureInclusion) {
                                    SwipeToRevealDefaults.bidirectionalGestureInclusion
                                } else {
                                    gestureInclusion(revealState)
                                },
                            enableTouchSlop = enableTouchSlop,
                        )
                    }
                }

            if (!wrappedInSwipeToDismissBox) {
                content()
            } else {
                BasicSwipeToDismissBox(
                    onDismissed = { onSwipeToDismissBoxDismissed = true },
                    state = rememberSwipeToDismissBoxState(),
                ) { isBackground ->
                    if (isBackground) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Red))
                    } else {
                        Box(contentAlignment = Alignment.Center) { content() }
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(block = { gesture(density) })

        rule.runOnIdle { assertEquals(expectedRevealValue, revealState.currentValue) }

        assertEquals(if (expectedFullSwipeTriggered) 1 else 0, onFullSwipeTriggerCounter)
        assertEquals(expectedSwipeToDismissBoxDismissed, onSwipeToDismissBoxDismissed)
    }

    private fun verifyStateMultipleSwipeToReveal(
        actions:
            ((revealStateOne: RevealState, revealStateTwo: RevealState, density: Float) -> Unit)? =
            null,
        actionsSuspended:
            (suspend (
                revealStateOne: RevealState, revealStateTwo: RevealState, density: Float,
            ) -> Unit)? =
            null,
        revealDirection: RevealDirection = RightToLeft,
        assertions: (revealStateOne: RevealState, revealStateTwo: RevealState) -> Unit,
    ) {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        var density = 0f
        rule.setContent {
            with(LocalDensity.current) { density = this.density }
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            ScreenConfiguration(screenSizeDp = LARGE_SCREEN_WIDTH_DP) {
                CustomTouchSlopProvider(newTouchSlop = 0f) {
                    Column {
                        SwipeToRevealWithDefaults(
                            modifier = Modifier.testTag(SWIPE_TO_REVEAL_TAG),
                            revealState = revealStateOne,
                            revealDirection = revealDirection,
                        )
                        SwipeToRevealWithDefaults(
                            modifier = Modifier.testTag(SWIPE_TO_REVEAL_SECOND_TAG),
                            revealState = revealStateTwo,
                            revealDirection = revealDirection,
                        )
                    }
                }
            }

            LaunchedEffect(Unit) {
                launch { actionsSuspended?.invoke(revealStateOne, revealStateTwo, density) }
            }
        }

        actions?.invoke(revealStateOne, revealStateTwo, density)

        rule.runOnIdle { assertions(revealStateOne, revealStateTwo) }
    }

    @Composable
    fun SwipeToRevealWithDefaults(
        modifier: Modifier = Modifier,
        onSwipePrimaryAction: () -> Unit = {},
        primaryAction: @Composable SwipeToRevealScope.() -> Unit =
            @Composable { DefaultPrimaryActionButton(onClick = onSwipePrimaryAction) },
        secondaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
        undoPrimaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
        undoSecondaryAction: (@Composable SwipeToRevealScope.() -> Unit)? = null,
        revealState: RevealState = rememberRevealState(),
        revealDirection: RevealDirection = RightToLeft,
        hasPartiallyRevealedState: Boolean = true,
        gestureInclusion: GestureInclusion =
            if (revealDirection == Bidirectional) {
                bidirectionalGestureInclusion
            } else {
                gestureInclusion(revealState)
            },
        enableTouchSlop: Boolean = true,
        content: @Composable () -> Unit = @Composable { DefaultContent() },
    ) {
        val swipeToRevealContent =
            @Composable {
                SwipeToReveal(
                    primaryAction = primaryAction,
                    onSwipePrimaryAction = onSwipePrimaryAction,
                    modifier = modifier,
                    secondaryAction = secondaryAction,
                    undoPrimaryAction = undoPrimaryAction,
                    undoSecondaryAction = undoSecondaryAction,
                    revealState = revealState,
                    revealDirection = revealDirection,
                    hasPartiallyRevealedState = hasPartiallyRevealedState,
                    gestureInclusion = gestureInclusion,
                    content = content,
                )
            }
        if (enableTouchSlop) {
            swipeToRevealContent()
        } else {
            CustomTouchSlopProvider(newTouchSlop = 0f, content = swipeToRevealContent)
        }
    }

    @Composable
    private fun SwipeToRevealScope.DefaultPrimaryActionButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) {
        PrimaryActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
            text = { Text("Delete") },
            modifier = modifier,
        )
    }

    @Composable
    private fun SwipeToRevealScope.DefaultSecondaryActionButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) {
        SecondaryActionButton(
            onClick = onClick,
            icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
            modifier = modifier,
        )
    }

    @Composable
    private fun SwipeToRevealScope.DefaultUndoActionButton(
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) {
        UndoActionButton(onClick = onClick, text = { Text("Undo Delete") }, modifier = modifier)
    }

    @Composable
    private fun DefaultContent(modifier: Modifier = Modifier) {
        Button({}, modifier.fillMaxWidth()) { Text("Swipe me!") }
    }

    private fun TouchInjectionScope.swipeLeftToRevealing(
        density: Float,
        anchorWidth: Dp = SingleActionAnchorWidth,
    ) {
        val widthPx = anchorWidth.value * density
        swipeLeft(startX = right, endX = right - widthPx)
    }

    private fun TouchInjectionScope.swipeRightToRevealing(
        density: Float,
        anchorWidth: Dp = SingleActionAnchorWidth,
    ) {
        val widthPx = anchorWidth.value * density
        swipeRight(startX = left, endX = left + widthPx)
    }

    companion object {
        private const val SWIPE_TO_REVEAL_TAG = TEST_TAG
        private const val SWIPE_TO_REVEAL_SECOND_TAG = "SWIPE_TO_REVEAL_SECOND_TAG"
        private const val PRIMARY_ACTION_TAG = "PRIMARY_ACTION_TAG"
        private const val SECONDARY_ACTION_TAG = "SECONDARY_ACTION_TAG"
        private const val UNDO_PRIMARY_ACTION_TAG = "UNDO_PRIMARY_ACTION_TAG"
        private const val UNDO_SECONDARY_ACTION_TAG = "UNDO_SECONDARY_ACTION_TAG"
    }
}
