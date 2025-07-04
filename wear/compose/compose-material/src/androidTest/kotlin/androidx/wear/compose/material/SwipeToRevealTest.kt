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

package androidx.wear.compose.material

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.material.SwipeToRevealDefaults.createRevealAnchors
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalWearMaterialApi::class)
class SwipeToRevealTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testTag_onChip() {
        rule.setContentWithTheme { swipeToRevealChipDefault(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testTag_onCard() {
        rule.setContentWithTheme { swipeToRevealCardDefault(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testTag_onContent_onChip() {
        rule.setContentWithTheme { swipeToRevealChipDefault() }

        rule.onNodeWithTag(CONTENT_TAG).assertExists()
    }

    @Test
    fun supports_testTag_onContent_onCard() {
        rule.setContentWithTheme { swipeToRevealCardDefault() }

        rule.onNodeWithTag(CONTENT_TAG).assertExists()
    }

    @Test
    fun whenNotRevealed_actionsDoNotExist_inChip() {
        rule.setContentWithTheme { swipeToRevealChipDefault() }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenNotRevealed_actionsDoNotExist_inCard() {
        rule.setContentWithTheme { swipeToRevealCardDefault() }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenRevealing_actionsExist_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            )
        }
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealing_actionsExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            )
        }
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_undoActionExists_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealed)
            )
        }

        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_undoActionExists_inCard() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealed)
            )
        }

        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_actionsDoNotExist_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealed)
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenRevealed_actionsDoNotExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealed)
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun onPrimaryActionClick_triggersOnClick_forChip() {
        var clicked = false
        rule.setContentWithTheme {
            val revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            swipeToRevealChipDefault(
                revealState = revealState,
                primaryAction = {
                    CreatePrimaryAction(revealState = revealState) { clicked = true }
                },
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun onSecondaryActionClick_triggersOnClick_forChip() {
        var clicked = false
        rule.setContentWithTheme {
            val revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            swipeToRevealChipDefault(
                revealState = revealState,
                secondaryAction = {
                    CreateSecondaryAction(revealState = revealState) { clicked = true }
                },
            )
        }

        rule.onNodeWithTag(SECONDARY_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun onPrimaryActionClickWithStateToRevealed_undoPrimaryActionCanBeClicked() {
        rule.setContentWithTheme {
            val revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                primaryAction = {
                    CreatePrimaryAction(revealState = revealState) {
                        coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
                    }
                },
                undoSecondaryAction = null,
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists().assertHasClickAction()
        rule.onNodeWithTag(UNDO_SECONDARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun onPrimaryActionClickAndPrimaryUndoClicked_stateChangesToCovered() {
        lateinit var revealState: RevealState
        rule.setContentWithTheme {
            revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                primaryAction = {
                    CreatePrimaryAction(revealState = revealState) {
                        coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
                    }
                },
                undoPrimaryAction = {
                    CreateUndoAction(revealState = revealState) {
                        coroutineScope.launch {
                            // reset state when undo is clicked
                            revealState.animateTo(RevealValue.Covered)
                            revealState.lastActionType = RevealActionType.None
                        }
                    }
                },
                undoSecondaryAction = null,
            )
        }
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(RevealValue.Covered, revealState.currentValue)
            assertEquals(RevealActionType.None, revealState.lastActionType)
        }
    }

    @Test
    fun onSecondaryActionClickWithStateToRevealed_undoSecondaryActionCanBeClicked() {
        rule.setContentWithTheme {
            val revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                secondaryAction = {
                    CreateSecondaryAction(revealState = revealState) {
                        coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
                    }
                },
                undoPrimaryAction = null,
            )
        }

        rule.onNodeWithTag(SECONDARY_ACTION_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(UNDO_SECONDARY_ACTION_TAG).assertExists().assertHasClickAction()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun onSecondaryActionClickAndUndoSecondaryClicked_stateChangesToCovered() {
        lateinit var revealState: RevealState
        rule.setContentWithTheme {
            revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                secondaryAction = {
                    CreateSecondaryAction(revealState = revealState) {
                        coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
                    }
                },
                undoSecondaryAction = {
                    CreateUndoAction(
                        revealState = revealState,
                        modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG),
                    ) {
                        coroutineScope.launch {
                            // reset state after undo is clicked
                            revealState.animateTo(RevealValue.Covered)
                            revealState.lastActionType = RevealActionType.None
                        }
                    }
                },
                undoPrimaryAction = null,
            )
        }
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(UNDO_SECONDARY_ACTION_TAG).performClick()
        rule.waitForIdle()

        rule.runOnIdle {
            assertEquals(RevealValue.Covered, revealState.currentValue)
            assertEquals(RevealActionType.None, revealState.lastActionType)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verifyActionColors() {
        var primaryActionColor = Color.Yellow
        var secondaryActionColor = Color.Green
        rule.setContentWithTheme {
            primaryActionColor = MaterialTheme.colors.error
            secondaryActionColor = MaterialTheme.colors.surface
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            )
        }

        rule
            .onNodeWithTag(PRIMARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(primaryActionColor, 50.0f)
        rule
            .onNodeWithTag(SECONDARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(secondaryActionColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun canOverrideActionColors() {
        val overridePrimaryActionColor = Color.Yellow
        val overrideSecondaryActionColor = Color.Green
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                colors =
                    SwipeToRevealDefaults.actionColors(
                        primaryActionBackgroundColor = overridePrimaryActionColor,
                        secondaryActionBackgroundColor = overrideSecondaryActionColor,
                    ),
            )
        }

        rule
            .onNodeWithTag(PRIMARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(overridePrimaryActionColor, 50.0f)
        rule
            .onNodeWithTag(SECONDARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(overrideSecondaryActionColor, 50.0f)
    }

    @Test
    fun supports_testTag() {
        rule.setContent { swipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onStartWithDefaultState_keepsContentToRight() {
        rule.setContent {
            swipeToRevealWithDefaults(
                content = { getBoxContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun onZeroOffset_doesNotDrawActions() {
        rule.setContent {
            swipeToRevealWithDefaults(
                primaryAction = { actionContent(modifier = Modifier.testTag(TEST_TAG)) }
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun onRevealing_drawsAction() {
        rule.setContent {
            swipeToRevealWithDefaults(
                state = rememberRevealState(initialValue = RevealValue.RightRevealing),
                primaryAction = { actionContent(modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun onSwipe_drawsAction() {
        val s2rTag = "S2RTag"
        rule.setContent {
            swipeToRevealWithDefaults(
                modifier = Modifier.testTag(s2rTag),
                primaryAction = { actionContent(modifier = Modifier.testTag(TEST_TAG)) },
            )
        }

        rule.onNodeWithTag(s2rTag).performTouchInput {
            down(center)
            // Move the pointer by quarter of the screen width, don't move up the pointer
            moveBy(delta = Offset(x = -(centerX / 4), y = 0f))
        }

        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun noSwipe_onFullSwipeRight() {
        var onFullSwipeTriggered = false

        verifyGesture(
            revealValue = RevealValue.Covered,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeRight() },
        )

        assertEquals(false, onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_onFullSwipeLeft() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RevealValue.RightRevealed,
            onFullSwipe = { onFullSwipeTriggered = true },
            gesture = { swipeLeft() },
        )

        assertEquals(true, onFullSwipeTriggered)
    }

    @Test
    fun stateToSwiped_onFullSwipeRight() {
        var onFullSwipeTriggered = false
        verifyGesture(
            revealValue = RevealValue.LeftRevealed,
            onFullSwipe = { onFullSwipeTriggered = true },
            revealDirection = RevealDirection.Both,
            gesture = { swipeRight() },
        )

        assertEquals(true, onFullSwipeTriggered)
    }

    @Test
    fun stateToRevealing_onAboveVelocityThresholdSmallDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(endX = right - 65, durationMillis = 30L) },
        )
    }

    @Test
    fun noSwipe_onBelowVelocityThresholdSmallDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.Covered,
            gesture = { swipeLeft(endX = right - 65, durationMillis = 1000L) },
        )
    }

    @Test
    fun stateToRevealing_onAboveVelocityThresholdLongDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(endX = right - 300, durationMillis = 100L) },
        )
    }

    @Test
    fun stateToRevealing_onBelowVelocityThresholdLongDistanceSwipe() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(endX = right - 300, durationMillis = 1000L) },
        )
    }

    @Test
    fun stateToCovered_singleDirectionRevealing_onFullSwipeRight() {
        var onSwipeToDismissBoxDismissed = false

        verifyGesture(
            initialValue = RevealValue.RightRevealing,
            revealValue = RevealValue.Covered,
            gesture = { swipeRight() },
            wrappedInSwipeToDismissBox = true,
            onSwipeToDismissBoxDismissed = { onSwipeToDismissBoxDismissed = true },
        )

        assertFalse(onSwipeToDismissBoxDismissed)
    }

    @Test
    fun stateToIconsVisible_onPartialSwipeLeft() {
        verifyGesture(
            revealValue = RevealValue.RightRevealing,
            gesture = { swipeLeft(startX = width / 2f, endX = 0f) },
        )
    }

    @Test
    fun onSwipe_whenNotAllowed_doesNotSwipe() {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState =
                rememberRevealState(
                    confirmValueChange = { revealValue ->
                        revealValue != RevealValue.RightRevealing
                    }
                )
            swipeToRevealWithDefaults(state = revealState, modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft(startX = width / 2f, endX = 0f) }

        rule.runOnIdle { assertEquals(RevealValue.Covered, revealState.currentValue) }
    }

    @Test
    fun onMultiSwipe_whenNotAllowed_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo =
                rememberRevealState(
                    confirmValueChange = { revealValue ->
                        revealValue != RevealValue.RightRevealing
                    }
                )
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        // swipe the second S2R to a reveal value which is not allowed
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        rule.runOnIdle {
            assertEquals(RevealValue.RightRevealing, revealStateOne.currentValue)
            assertEquals(RevealValue.Covered, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onMultiSwipe_whenAllowed_resetsLastState() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R to Revealing state
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        // swipe the second S2R to a reveal value
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        rule.runOnIdle {
            assertEquals(RevealValue.Covered, revealStateOne.currentValue)
            assertEquals(RevealValue.RightRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onMultiSwipe_whenLastStateRevealed_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R to Revealed (full screen swipe)
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeLeft(startX = width.toFloat(), endX = 0f)
        }

        // swipe the second S2R to a reveal value
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeLeft(startX = width / 2f, endX = 0f)
        }

        rule.runOnIdle {
            // assert that state does not reset when it is fully swiped then another S2R is swiped
            assertEquals(RevealValue.RightRevealed, revealStateOne.currentValue)
            assertEquals(RevealValue.RightRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onMultiSwipeBiDirection_whenLastStateRevealed_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val testTagOne = "testTagOne"
        val testTagTwo = "testTagTwo"
        rule.setContent {
            revealStateOne =
                rememberRevealState(
                    anchors = createRevealAnchors(revealDirection = RevealDirection.Both)
                )
            revealStateTwo =
                rememberRevealState(
                    anchors = createRevealAnchors(revealDirection = RevealDirection.Both)
                )
            Column {
                swipeToRevealWithDefaults(
                    state = revealStateOne,
                    modifier = Modifier.testTag(testTagOne),
                )
                swipeToRevealWithDefaults(
                    state = revealStateTwo,
                    modifier = Modifier.testTag(testTagTwo),
                )
            }
        }

        // swipe the first S2R to Left Revealed (full screen swipe)
        rule.onNodeWithTag(testTagOne).performTouchInput {
            swipeRight(startX = 0f, endX = width.toFloat())
        }

        // swipe the second S2R to a reveal value
        rule.onNodeWithTag(testTagTwo).performTouchInput {
            swipeRight(startX = 0f, endX = width / 2f)
        }

        rule.runOnIdle {
            // assert that state does not reset when it is fully swiped then another S2R is swiped
            assertEquals(RevealValue.LeftRevealed, revealStateOne.currentValue)
            assertEquals(RevealValue.LeftRevealing, revealStateTwo.currentValue)
        }
    }

    @Test
    fun onSnapForDifferentStates_lastOneGetsReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            swipeToRevealWithDefaults(state = revealStateOne)
            swipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                // First change
                revealStateOne.snapTo(RevealValue.RightRevealing)
                // Second change, in a different state
                revealStateTwo.snapTo(RevealValue.RightRevealing)
            }
        }

        rule.waitForIdle()

        // assert that state resets when it is partial swiped then another S2R is swiped
        rule.runOnIdle { assertEquals(RevealValue.Covered, revealStateOne.currentValue) }
    }

    @Test
    fun onSnapForDifferentStates_biDirection_lastOneGetsReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        rule.setContent {
            revealStateOne =
                rememberRevealState(
                    anchors = createRevealAnchors(revealDirection = RevealDirection.Both)
                )
            revealStateTwo =
                rememberRevealState(
                    anchors = createRevealAnchors(revealDirection = RevealDirection.Both)
                )
            ScreenConfiguration(SCREEN_SIZE_LARGE) {
                // Make sure S2R does not overlap
                Column {
                    swipeToRevealWithDefaults(state = revealStateOne)
                    swipeToRevealWithDefaults(state = revealStateTwo)
                }
            }

            LaunchedEffect(Unit) {
                // First change
                revealStateOne.snapTo(RevealValue.LeftRevealing)
                // Second change, in a different state
                revealStateTwo.snapTo(RevealValue.LeftRevealing)
            }
        }

        rule.waitForIdle()

        // assert that state resets when it is partial swiped then another S2R is swiped
        rule.runOnIdle { assertEquals(RevealValue.Covered, revealStateOne.currentValue) }
    }

    @Test
    fun onMultiSnapOnSameState_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val lastValue = RevealValue.RightRevealed
        rule.setContent {
            revealStateOne = rememberRevealState()
            revealStateTwo = rememberRevealState()
            swipeToRevealWithDefaults(state = revealStateOne)
            swipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                revealStateOne.snapTo(RevealValue.RightRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same state
            }
        }

        rule.waitForIdle()

        rule.runOnIdle { assertEquals(lastValue, revealStateOne.currentValue) }
    }

    @Test
    fun onMultiSnapOnSameState_biDirection_doesNotReset() {
        lateinit var revealStateOne: RevealState
        lateinit var revealStateTwo: RevealState
        val lastValue = RevealValue.LeftRevealed
        rule.setContent {
            revealStateOne =
                rememberRevealState(
                    anchors = createRevealAnchors(revealDirection = RevealDirection.Both)
                )
            revealStateTwo =
                rememberRevealState(
                    anchors = createRevealAnchors(revealDirection = RevealDirection.Both)
                )
            swipeToRevealWithDefaults(state = revealStateOne)
            swipeToRevealWithDefaults(state = revealStateTwo)

            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch {
                revealStateOne.snapTo(RevealValue.LeftRevealing) // First change
                revealStateOne.snapTo(lastValue) // Second change, same state
            }
        }

        rule.waitForIdle()

        rule.runOnIdle { assertEquals(lastValue, revealStateOne.currentValue) }
    }

    @Test
    fun onSecondaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = RevealActionType.SecondaryAction,
            initialRevealValue = RevealValue.RightRevealing,
            secondaryActionModifier = Modifier.testTag(TEST_TAG),
        )

    @Test
    fun onPrimaryActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = RevealActionType.PrimaryAction,
            initialRevealValue = RevealValue.RightRevealing,
            primaryActionModifier = Modifier.testTag(TEST_TAG),
        )

    @Test
    fun onUndoActionClick_setsLastClickAction() =
        verifyLastClickAction(
            expectedClickType = RevealActionType.UndoAction,
            initialRevealValue = RevealValue.RightRevealed,
            undoActionModifier = Modifier.testTag(TEST_TAG),
        )

    @Test
    fun onRightSwipe_dispatchEventsToParent() {
        var onPreScrollDispatch = 0f
        rule.setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        onPreScrollDispatch = available.x
                        return available
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                swipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }

        assert(onPreScrollDispatch > 0)
    }

    @Test
    fun onLeftSwipe_dispatchEventsToParent() {
        var onPreScrollDispatch = 0f
        rule.setContent {
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        onPreScrollDispatch = available.x
                        return available
                    }
                }
            }
            Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
                swipeToRevealWithDefaults(modifier = Modifier.testTag(TEST_TAG))
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeLeft() }

        assert(onPreScrollDispatch < 0) // Swiping left means the dispatch will be negative
    }

    @Test()
    fun onRtl_animateToLeftRevealing() {
        verifyIllegalAnimateTo(RevealValue.LeftRevealing)
    }

    @Test()
    fun onRtl_animateToLeftRevealed() {
        verifyIllegalAnimateTo(RevealValue.LeftRevealed)
    }

    private fun verifyIllegalAnimateTo(targetValue: RevealValue) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState()
            swipeToRevealWithDefaults(state = revealState)
        }

        assertThrows(IllegalStateException::class.java) {
            // If use coroutineScope.launch, below block will run in parallel with test code, and we
            // won't able to catch exception.
            runBlocking { revealState.animateTo(targetValue) }
        }
    }

    private fun verifyLastClickAction(
        expectedClickType: RevealActionType,
        initialRevealValue: RevealValue,
        primaryActionModifier: Modifier = Modifier,
        secondaryActionModifier: Modifier = Modifier,
        undoActionModifier: Modifier = Modifier,
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState = rememberRevealState(initialRevealValue)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealWithDefaults(
                state = revealState,
                primaryAction = {
                    actionContent(
                        modifier =
                            primaryActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.snapTo(RevealValue.Covered)
                                    revealState.lastActionType = RevealActionType.PrimaryAction
                                }
                            }
                    )
                },
                secondaryAction = {
                    actionContent(
                        modifier =
                            secondaryActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.snapTo(RevealValue.Covered)
                                    revealState.lastActionType = RevealActionType.SecondaryAction
                                }
                            }
                    )
                },
                undoAction = {
                    actionContent(
                        modifier =
                            undoActionModifier.clickable {
                                coroutineScope.launch {
                                    revealState.animateTo(RevealValue.Covered)
                                    revealState.lastActionType = RevealActionType.UndoAction
                                }
                            }
                    )
                },
            )
        }
        rule.onNodeWithTag(TEST_TAG).performClick()
        rule.runOnIdle { assertEquals(expectedClickType, revealState.lastActionType) }
    }

    private fun verifyGesture(
        initialValue: RevealValue = RevealValue.Covered,
        revealValue: RevealValue,
        gesture: TouchInjectionScope.() -> Unit,
        onFullSwipe: () -> Unit = {},
        revealDirection: RevealDirection = RevealDirection.RightToLeft,
        wrappedInSwipeToDismissBox: Boolean = false,
        onSwipeToDismissBoxDismissed: () -> Unit = {},
    ) {
        lateinit var revealState: RevealState
        rule.setContent {
            revealState =
                rememberRevealState(
                    initialValue = initialValue,
                    anchors = createRevealAnchors(revealDirection = revealDirection),
                )
            if (!wrappedInSwipeToDismissBox) {
                swipeToRevealWithDefaults(
                    state = revealState,
                    onFullSwipe = onFullSwipe,
                    modifier = Modifier.testTag(TEST_TAG),
                )
            } else {
                BasicSwipeToDismissBox(
                    onDismissed = onSwipeToDismissBoxDismissed,
                    state = androidx.wear.compose.foundation.rememberSwipeToDismissBoxState(),
                ) { isBackground ->
                    if (isBackground) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Red))
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            swipeToRevealWithDefaults(
                                state = revealState,
                                onFullSwipe = onFullSwipe,
                                modifier = Modifier.testTag(TEST_TAG),
                            )
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(gesture)

        rule.runOnIdle { assertEquals(revealValue, revealState.currentValue) }
    }

    @Composable
    private fun swipeToRevealWithDefaults(
        primaryAction: @Composable () -> Unit = { getAction() },
        state: RevealState = rememberRevealState(),
        modifier: Modifier = Modifier,
        secondaryAction: (@Composable () -> Unit)? = null,
        undoAction: (@Composable () -> Unit)? = null,
        onFullSwipe: () -> Unit = {},
        content: @Composable () -> Unit = { getBoxContent() },
    ) {
        SwipeToReveal(
            primaryAction = primaryAction,
            modifier = modifier,
            onFullSwipe = onFullSwipe,
            state = state,
            secondaryAction = secondaryAction,
            undoAction = undoAction,
            content = content,
        )
    }

    @Composable
    private fun getBoxContent(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
        Box(
            modifier =
                modifier.background(Color.Red).size(width = 200.dp, height = 50.dp).clickable {
                    onClick()
                }
        ) {}
    }

    @Composable
    private fun actionContent(modifier: Modifier = Modifier) {
        Box(modifier = modifier.background(Color.Green).size(50.dp)) {}
    }

    @Composable
    private fun getAction(
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit = { actionContent(modifier) },
    ) {
        Box(modifier = modifier.clickable { onClick() }) { content() }
    }

    @Composable
    private fun swipeToRevealChipDefault(
        modifier: Modifier = Modifier,
        revealState: RevealState = rememberRevealState(),
        primaryAction: @Composable () -> Unit = { CreatePrimaryAction(revealState) },
        secondaryAction: @Composable () -> Unit = { CreateSecondaryAction(revealState) },
        undoPrimaryAction: (@Composable () -> Unit)? = { CreateUndoAction(revealState) },
        undoSecondaryAction: (@Composable () -> Unit)? = {
            CreateUndoAction(revealState, modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG))
        },
        onFullSwipe: () -> Unit = {},
        colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
        content: @Composable () -> Unit = { CreateContent() },
    ) {
        SwipeToRevealChip(
            modifier = modifier,
            revealState = revealState,
            onFullSwipe = onFullSwipe,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
            undoPrimaryAction = undoPrimaryAction,
            undoSecondaryAction = undoSecondaryAction,
            colors = colors,
            content = content,
        )
    }

    @Composable
    private fun swipeToRevealCardDefault(
        modifier: Modifier = Modifier,
        revealState: RevealState = rememberRevealState(),
        primaryAction: @Composable () -> Unit = { CreatePrimaryAction(revealState) },
        secondaryAction: @Composable () -> Unit = { CreateSecondaryAction(revealState) },
        undoPrimaryAction: (@Composable () -> Unit)? = { CreateUndoAction(revealState) },
        undoSecondaryAction: (@Composable () -> Unit)? = {
            CreateUndoAction(revealState, modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG))
        },
        onFullSwipe: () -> Unit = {},
        colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
        content: @Composable () -> Unit = { CreateContent() },
    ) {
        SwipeToRevealCard(
            modifier = modifier,
            revealState = revealState,
            onFullSwipe = onFullSwipe,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
            undoPrimaryAction = undoPrimaryAction,
            undoSecondaryAction = undoSecondaryAction,
            colors = colors,
            content = content,
        )
    }

    @Composable
    private fun CreatePrimaryAction(
        revealState: RevealState,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
        label: @Composable () -> Unit = { Text("Clear") },
        onClick: () -> Unit = {},
    ) {
        SwipeToRevealPrimaryAction(
            revealState = revealState,
            icon = icon,
            label = label,
            modifier = modifier.testTag(PRIMARY_ACTION_TAG),
            onClick = onClick,
        )
    }

    @Composable
    private fun CreateSecondaryAction(
        revealState: RevealState,
        modifier: Modifier = Modifier,
        icon: @Composable () -> Unit = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") },
        onClick: () -> Unit = {},
    ) {
        SwipeToRevealSecondaryAction(
            revealState = revealState,
            content = icon,
            modifier = modifier.testTag(SECONDARY_ACTION_TAG),
            onClick = onClick,
        )
    }

    @Composable
    private fun CreateUndoAction(
        revealState: RevealState,
        modifier: Modifier = Modifier,
        label: @Composable () -> Unit = { Text("Undo") },
        onClick: () -> Unit = {},
    ) {
        SwipeToRevealUndoAction(
            revealState = revealState,
            label = label,
            modifier = modifier.testTag(UNDO_PRIMARY_ACTION_TAG),
            onClick = onClick,
        )
    }

    @Composable
    private fun CreateContent(modifier: Modifier = Modifier) =
        Box(modifier = modifier.fillMaxWidth().height(50.dp).testTag(CONTENT_TAG))

    private val CONTENT_TAG = "Content"
    private val PRIMARY_ACTION_TAG = "Action"
    private val SECONDARY_ACTION_TAG = "AdditionalAction"
    private val UNDO_PRIMARY_ACTION_TAG = "UndoAction"
    private val UNDO_SECONDARY_ACTION_TAG = "UndoAdditionalAction"
}
