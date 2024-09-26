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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealScope
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.rememberRevealState
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalWearMaterialApi::class)
class SwipeToRevealScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Test
    fun swipeToRevealCard_singleAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            swipeToRevealCard(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                secondaryAction = null
            )
        }
    }

    @Test
    fun swipeToRevealChip_singleAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            swipeToRevealChip(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                secondaryAction = null
            )
        }
    }

    @Test
    fun swipeToRevealCard_twoActions() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            swipeToRevealCard(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            )
        }
    }

    @Test
    fun swipeToRevealChip_twoActions() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            swipeToRevealChip(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealing)
            )
        }
    }

    @Test
    fun swipeToRevealChip_undoPrimaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            swipeToRevealChip(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealed)
            )
        }
    }

    @Test
    fun swipeToRevealCard_undoPrimaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            swipeToRevealCard(
                revealState = rememberRevealState(initialValue = RevealValue.RightRevealed)
            )
        }
    }

    @Test
    fun swipeToRevealChip_undoSecondaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            val revealState = rememberRevealState()
            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
            revealState.lastActionType = RevealActionType.SecondaryAction
            swipeToRevealChip(revealState = revealState)
        }
    }

    @Test
    fun swipeToRevealCard_undoSecondaryAction() {
        rule.verifyScreenshot(screenshotRule = screenshotRule, methodName = testName.methodName) {
            val revealState = rememberRevealState()
            val coroutineScope = rememberCoroutineScope()
            coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
            revealState.lastActionType = RevealActionType.SecondaryAction
            swipeToRevealCard(revealState = revealState)
        }
    }

    @Composable
    private fun swipeToRevealCard(
        revealState: RevealState = rememberRevealState(),
        secondaryAction: (@Composable RevealScope.() -> Unit)? = {
            SwipeToRevealSecondaryAction(
                revealState = revealState,
                onClick = {},
                content = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") }
            )
        },
        undoPrimaryAction: (@Composable RevealScope.() -> Unit)? = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                onClick = {},
                label = { Text("Undo") }
            )
        },
        undoSecondaryAction: (@Composable RevealScope.() -> Unit)? = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                onClick = {},
                label = { Text("Undo") }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            SwipeToRevealCard(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    SwipeToRevealPrimaryAction(
                        revealState = revealState,
                        onClick = {},
                        icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
                        label = { Text("Delete") }
                    )
                },
                onFullSwipe = {},
                secondaryAction = secondaryAction,
                undoPrimaryAction = undoPrimaryAction,
                undoSecondaryAction = undoSecondaryAction,
                revealState = revealState
            ) {
                TitleCard(
                    onClick = { /*TODO*/ },
                    title = { Text("Title of card") },
                    time = { Text("now") },
                ) {
                    Text("Swipe To Reveal - Card")
                }
            }
        }
    }

    @Composable
    private fun swipeToRevealChip(
        revealState: RevealState = rememberRevealState(),
        secondaryAction: (@Composable RevealScope.() -> Unit)? = {
            SwipeToRevealSecondaryAction(
                revealState = revealState,
                onClick = {},
                content = { Icon(SwipeToRevealDefaults.MoreOptions, "More Options") }
            )
        },
        undoPrimaryAction: (@Composable RevealScope.() -> Unit)? = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                onClick = {},
                label = { Text("Undo") }
            )
        },
        undoSecondaryAction: (@Composable RevealScope.() -> Unit)? = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                onClick = {},
                label = { Text("Undo") }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            SwipeToRevealChip(
                modifier = Modifier.testTag(TEST_TAG),
                primaryAction = {
                    SwipeToRevealPrimaryAction(
                        revealState = revealState,
                        onClick = {},
                        icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
                        label = { Text("Delete") }
                    )
                },
                onFullSwipe = {},
                secondaryAction = secondaryAction,
                undoPrimaryAction = undoPrimaryAction,
                undoSecondaryAction = undoSecondaryAction,
                revealState = revealState
            ) {
                Chip(
                    onClick = { /* onClick handler for chip */ },
                    colors = ChipDefaults.primaryChipColors(),
                    border = ChipDefaults.outlinedChipBorder()
                ) {
                    Text("Swipe To Reveal - Chip")
                }
            }
        }
    }
}
