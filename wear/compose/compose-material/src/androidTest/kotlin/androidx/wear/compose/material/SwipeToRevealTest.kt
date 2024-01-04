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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.rememberRevealState
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearMaterialApi::class)
class SwipeToRevealActionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testTag_onChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testTag_onCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(modifier = Modifier.testTag(TEST_TAG))
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_testTag_onContent_onChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault()
        }

        rule.onNodeWithTag(CONTENT_TAG).assertExists()
    }

    @Test
    fun supports_testTag_onContent_onCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault()
        }

        rule.onNodeWithTag(CONTENT_TAG).assertExists()
    }

    @Test
    fun whenNotRevealed_actionsDoNotExist_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault()
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenNotRevealed_actionsDoNotExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault()
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenRevealing_actionsExist_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            )
        }
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealing_actionsExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            )
        }
        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertExists()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_undoActionExists_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_undoActionExists_inCard() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(UNDO_PRIMARY_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_actionsDoNotExist_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenRevealed_actionsDoNotExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(SECONDARY_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun onPrimaryActionClick_triggersOnClick_forChip() {
        var clicked = false
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                primaryAction = createPrimaryAction(
                    onClick = { clicked = true }
                )
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun onSecondaryActionClick_triggersOnClick_forChip() {
        var clicked = false
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                secondaryAction = createSecondaryAction(
                    onClick = { clicked = true }
                )
            )
        }

        rule.onNodeWithTag(SECONDARY_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun onPrimaryActionClickWithStateToRevealed_undoPrimaryActionCanBeClicked() {
        rule.setContentWithTheme {
            val revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                primaryAction = createPrimaryAction(
                    onClick = {
                        coroutineScope.launch { revealState.animateTo(RevealValue.Revealed) }
                    }
                ),
                undoSecondaryAction = null
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
            revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                primaryAction = createPrimaryAction(
                    onClick = {
                        coroutineScope.launch { revealState.animateTo(RevealValue.Revealed) }
                    }
                ),
                undoPrimaryAction = createUndoAction(
                    onClick = {
                        coroutineScope.launch {
                            // reset state when undo is clicked
                            revealState.animateTo(RevealValue.Covered)
                            revealState.lastActionType = RevealActionType.None
                        }
                    }
                ),
                undoSecondaryAction = null
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
            val revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                secodnaryAction = createSecondaryAction(
                    onClick = {
                        coroutineScope.launch { revealState.animateTo(RevealValue.Revealed) }
                    }
                ),
                undoPrimaryAction = null
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
            revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            val coroutineScope = rememberCoroutineScope()
            swipeToRevealCardDefault(
                revealState = revealState,
                secodnaryAction = createSecondaryAction(
                    onClick = {
                        coroutineScope.launch {
                            revealState.animateTo(RevealValue.Revealed)
                        }
                    }
                ),
                undoSecondaryAction = createUndoAction(
                    modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG),
                    onClick = {
                        coroutineScope.launch {
                            // reset state after undo is clicked
                            revealState.animateTo(RevealValue.Covered)
                            revealState.lastActionType = RevealActionType.None
                        }
                    }
                ),
                undoPrimaryAction = null
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

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verifyActionColors() {
        var primaryActionColor = Color.Yellow
        var secondaryActionColor = Color.Green
        rule.setContentWithTheme {
            primaryActionColor = MaterialTheme.colors.error
            secondaryActionColor = MaterialTheme.colors.surface
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(primaryActionColor, 50.0f)
        rule.onNodeWithTag(SECONDARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(secondaryActionColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun canOverrideActionColors() {
        val overridePrimaryActionColor = Color.Yellow
        val overrideSecondaryActionColor = Color.Green
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                colors = SwipeToRevealDefaults.actionColors(
                    primaryActionBackgroundColor = overridePrimaryActionColor,
                    secondaryActionBackgroundColor = overrideSecondaryActionColor
                )
            )
        }

        rule.onNodeWithTag(PRIMARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(overridePrimaryActionColor, 50.0f)
        rule.onNodeWithTag(SECONDARY_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(overrideSecondaryActionColor, 50.0f)
    }

    @Composable
    private fun swipeToRevealChipDefault(
        modifier: Modifier = Modifier,
        revealState: RevealState = rememberRevealState(),
        primaryAction: SwipeToRevealAction = createPrimaryAction(),
        secondaryAction: SwipeToRevealAction = createSecondaryAction(),
        undoPrimaryAction: SwipeToRevealAction? = createUndoAction(),
        undoSecondaryAction: SwipeToRevealAction? =
            createUndoAction(modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG)),
        colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
        content: @Composable () -> Unit = { createContent() }
    ) {
        SwipeToRevealChip(
            modifier = modifier,
            revealState = revealState,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
            undoPrimaryAction = undoPrimaryAction,
            undoSecondaryAction = undoSecondaryAction,
            colors = colors,
            content = content
        )
    }

    @Composable
    private fun swipeToRevealCardDefault(
        modifier: Modifier = Modifier,
        revealState: RevealState = rememberRevealState(),
        primaryAction: SwipeToRevealAction = createPrimaryAction(),
        secodnaryAction: SwipeToRevealAction = createSecondaryAction(),
        undoPrimaryAction: SwipeToRevealAction? = createUndoAction(),
        undoSecondaryAction: SwipeToRevealAction? =
            createUndoAction(modifier = Modifier.testTag(UNDO_SECONDARY_ACTION_TAG)),
        colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
        content: @Composable () -> Unit = { createContent() }
    ) {
        SwipeToRevealCard(
            modifier = modifier,
            revealState = revealState,
            primaryAction = primaryAction,
            secondaryAction = secodnaryAction,
            undoPrimaryAction = undoPrimaryAction,
            undoSecondaryAction = undoSecondaryAction,
            colors = colors,
            content = content
        )
    }

    @Composable
    private fun createPrimaryAction(
        icon: @Composable () -> Unit = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
        label: @Composable () -> Unit = { Text("Clear") },
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ): SwipeToRevealAction = SwipeToRevealDefaults.primaryAction(
        icon = icon,
        label = label,
        modifier = modifier.testTag(PRIMARY_ACTION_TAG),
        onClick = onClick
    )

    @Composable
    private fun createSecondaryAction(
        icon: @Composable () -> Unit = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") },
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ): SwipeToRevealAction = SwipeToRevealDefaults.secondaryAction(
        icon = icon,
        modifier = modifier.testTag(SECONDARY_ACTION_TAG),
        onClick = onClick
    )

    @Composable
    private fun createUndoAction(
        label: @Composable () -> Unit = { Text("Undo") },
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ) = SwipeToRevealDefaults.undoAction(
        label = label,
        modifier = modifier.testTag(UNDO_PRIMARY_ACTION_TAG),
        onClick = onClick
    )

    @Composable
    private fun createContent(
        modifier: Modifier = Modifier
    ) = Box(modifier = modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag(CONTENT_TAG))

    private val CONTENT_TAG = "Content"
    private val PRIMARY_ACTION_TAG = "Action"
    private val SECONDARY_ACTION_TAG = "AdditionalAction"
    private val UNDO_PRIMARY_ACTION_TAG = "UndoAction"
    private val UNDO_SECONDARY_ACTION_TAG = "UndoAdditionalAction"
}
