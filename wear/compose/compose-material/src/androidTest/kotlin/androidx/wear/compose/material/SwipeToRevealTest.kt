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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.rememberRevealState
import junit.framework.TestCase.assertEquals
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

        rule.onNodeWithTag(ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNDO_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenNotRevealed_actionsDoNotExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault()
        }

        rule.onNodeWithTag(ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(UNDO_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenRevealing_actionsExist_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            )
        }
        rule.onNodeWithTag(ACTION_TAG).assertExists()
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealing_actionsExist_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            )
        }
        rule.onNodeWithTag(ACTION_TAG).assertExists()
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_undoActionExists_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(UNDO_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_undoActionExists_inCard() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(UNDO_ACTION_TAG).assertExists()
    }

    @Test
    fun whenRevealed_actionsDoesNotExists_inChip() {
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun whenRevealed_actionsDoesNotExists_inCard() {
        rule.setContentWithTheme {
            swipeToRevealCardDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            )
        }

        rule.onNodeWithTag(ACTION_TAG).assertDoesNotExist()
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).assertDoesNotExist()
    }

    @Test
    fun onActionClick_triggersOnClick_forChip() {
        var clicked = false
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                action = createAction(
                    onClick = { clicked = true }
                )
            )
        }

        rule.onNodeWithTag(ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun onAdditionalActionClick_triggersOnClick_forChip() {
        var clicked = false
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                additionalAction = createAdditionalAction(
                    onClick = { clicked = true }
                )
            )
        }

        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(true, clicked) }
    }

    @Test
    fun onActionClick_stateChangesToRevealed_forChip() {
        lateinit var revealState: RevealState
        rule.setContentWithTheme {
            revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            swipeToRevealChipDefault(
                revealState = revealState,
            )
        }

        rule.onNodeWithTag(ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(RevealValue.Revealed, revealState.currentValue) }
    }

    @Test
    fun onAdditionalActionClick_stateChangesToRevealed_forChip() {
        lateinit var revealState: RevealState
        rule.setContentWithTheme {
            revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            swipeToRevealChipDefault(
                revealState = revealState,
            )
        }

        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(RevealValue.Revealed, revealState.currentValue) }
    }

    @Test
    fun onUndoActionClick_stateChangesToCovered_forChip() {
        lateinit var revealState: RevealState
        rule.setContentWithTheme {
            revealState = rememberRevealState(initialValue = RevealValue.Revealed)
            swipeToRevealChipDefault(
                revealState = revealState,
            )
        }

        rule.onNodeWithTag(UNDO_ACTION_TAG).performClick()
        rule.runOnIdle { assertEquals(RevealValue.Covered, revealState.currentValue) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun verifyActionColors() {
        var actionColor = Color.Yellow
        var additionalActionColor = Color.Green
        rule.setContentWithTheme {
            actionColor = MaterialTheme.colors.error
            additionalActionColor = MaterialTheme.colors.surface
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing)
            )
        }

        rule.onNodeWithTag(ACTION_TAG)
            .captureToImage()
            .assertContainsColor(actionColor, 50.0f)
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(additionalActionColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun canOverrideActionColors() {
        val overrideActionColor = Color.Yellow
        val overrideAdditionalActionColor = Color.Green
        rule.setContentWithTheme {
            swipeToRevealChipDefault(
                revealState = rememberRevealState(initialValue = RevealValue.Revealing),
                colors = SwipeToRevealDefaults.actionColors(
                    actionBackgroundColor = overrideActionColor,
                    additionalActionBackgroundColor = overrideAdditionalActionColor
                )
            )
        }

        rule.onNodeWithTag(ACTION_TAG)
            .captureToImage()
            .assertContainsColor(overrideActionColor, 50.0f)
        rule.onNodeWithTag(ADDITIONAL_ACTION_TAG)
            .captureToImage()
            .assertContainsColor(overrideAdditionalActionColor, 50.0f)
    }

    @Composable
    private fun swipeToRevealChipDefault(
        modifier: Modifier = Modifier,
        revealState: RevealState = rememberRevealState(),
        action: SwipeToRevealAction = createAction(),
        additionalAction: SwipeToRevealAction = createAdditionalAction(),
        undoAction: SwipeToRevealAction = createUndoAction(),
        colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
        content: @Composable () -> Unit = { createContent() }
    ) {
        SwipeToRevealChip(
            modifier = modifier,
            revealState = revealState,
            action = action,
            additionalAction = additionalAction,
            undoAction = undoAction,
            colors = colors,
            content = content
        )
    }

    @Composable
    private fun swipeToRevealCardDefault(
        modifier: Modifier = Modifier,
        revealState: RevealState = rememberRevealState(),
        action: SwipeToRevealAction = createAction(),
        additionalAction: SwipeToRevealAction = createAdditionalAction(),
        undoAction: SwipeToRevealAction = createUndoAction(),
        colors: SwipeToRevealActionColors = SwipeToRevealDefaults.actionColors(),
        content: @Composable () -> Unit = { createContent() }
    ) {
        SwipeToRevealCard(
            modifier = modifier,
            revealState = revealState,
            action = action,
            additionalAction = additionalAction,
            undoAction = undoAction,
            colors = colors,
            content = content
        )
    }

    @Composable
    private fun createAction(
        icon: @Composable () -> Unit = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
        label: @Composable () -> Unit = { Text("Clear") },
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ): SwipeToRevealAction = SwipeToRevealDefaults.action(
        icon = icon,
        label = label,
        modifier = modifier.testTag(ACTION_TAG),
        onClick = onClick
    )

    @Composable
    private fun createAdditionalAction(
        icon: @Composable () -> Unit = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") },
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {},
    ): SwipeToRevealAction = SwipeToRevealDefaults.additionalAction(
        icon = icon,
        modifier = modifier.testTag(ADDITIONAL_ACTION_TAG),
        onClick = onClick
    )

    @Composable
    private fun createUndoAction(
        label: @Composable () -> Unit = { Text("Undo") },
        modifier: Modifier = Modifier
    ) = SwipeToRevealDefaults.undoAction(
        label = label,
        modifier = modifier.testTag(UNDO_ACTION_TAG)
    )

    @Composable
    private fun createContent(
        modifier: Modifier = Modifier
    ) = Box(modifier = modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag(CONTENT_TAG))

    private val CONTENT_TAG = "Content"
    private val ACTION_TAG = "Action"
    private val ADDITIONAL_ACTION_TAG = "AdditionalAction"
    private val UNDO_ACTION_TAG = "UndoAction"
}
