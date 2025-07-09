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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealState.SingleSwipeCoordinator
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import androidx.wear.compose.materialcore.screenWidthDp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class SwipeToRevealScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    @Before
    fun setUp() {
        SingleSwipeCoordinator.lastUpdatedState.set(null)
    }

    @Test
    fun swipeToReveal_showsPrimaryAction(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                            { Text("Clear") },
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                ) {
                    Button({}, Modifier.fillMaxWidth()) {
                        Text("This text should be partially visible.")
                    }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsPrimaryAndSecondaryActions(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                            { Text("Clear") },
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    secondaryAction = {
                        SecondaryActionButton(
                            {}, /* Empty for testing */
                            { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                        )
                    },
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                ) {
                    Button({}, Modifier.fillMaxWidth()) {
                        Text("This text should be partially visible.")
                    }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsUndoPrimaryAction(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    undoPrimaryAction = {
                        UndoActionButton({} /* Empty for testing */, { Text("Undo Primary") })
                    },
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealed),
                ) {
                    Button({}) { Text(/* Empty for testing */ "") }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_hasDefaultUndoHeightForLongLabels(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    undoPrimaryAction = {
                        UndoActionButton({} /* Empty for testing */, { Text("Undo") })
                    },
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealed),
                ) {
                    Button({}) {
                        Text(
                            "This Button has an extremely long label so that the undo " +
                                "action must truncate its label and should stay standard height"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_hasDefaultHeightForLongLabels(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    secondaryAction = {
                        SecondaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                ) {
                    Button({}) {
                        Text(
                            "This Button has an extremely long label, but the primary " +
                                "and secondary actions should still have default button height"
                        )
                    }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsUndoPrimaryAction_singleLineTruncated() {
        verifyScreenshotForSize(ScreenSize.SMALL) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    undoPrimaryAction = {
                        UndoActionButton(
                            {}, /* Empty for testing */
                            {
                                Text(
                                    "Undo Delete action with an extremely long label that should truncate."
                                )
                            },
                        )
                    },
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealed),
                ) {
                    Button({}) { Text(/* Empty for testing */ "") }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsUndoSecondaryAction(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    secondaryAction = {
                        SecondaryActionButton(
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    undoPrimaryAction = {
                        UndoActionButton({}, /* Empty for testing */ {} /* Empty for testing */)
                    },
                    undoSecondaryAction = { UndoActionButton({}, { Text("Undo Secondary") }) },
                    revealState =
                        rememberRevealState(initialValue = RevealValue.RightRevealed).apply {
                            lastActionType = RevealActionType.SecondaryAction
                        },
                ) {
                    Button({}) { Text(/* Empty for testing */ "") }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsUndoSecondaryAction_singleLineTruncated() {
        verifyScreenshotForSize(ScreenSize.SMALL) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    secondaryAction = {
                        SecondaryActionButton(
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    undoPrimaryAction = {
                        UndoActionButton({}, /* Empty for testing */ {} /* Empty for testing */)
                    },
                    undoSecondaryAction = {
                        UndoActionButton(
                            {},
                            {
                                Text(
                                    "Undo Delete action with an extremely long label that should truncate."
                                )
                            },
                        )
                    },
                    revealState =
                        rememberRevealState(initialValue = RevealValue.RightRevealed).apply {
                            lastActionType = RevealActionType.SecondaryAction
                        },
                ) {
                    Button({}) { Text(/* Empty for testing */ "") }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsContent(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            {}, /* Empty for testing */
                            {}, /* Empty for testing */
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                ) {
                    Button({}, Modifier.fillMaxWidth()) {
                        Text("This content should be fully visible.")
                    }
                }
            }
        }
    }

    @Test
    fun swipeToRevealCard_showsLargePrimaryAction(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                            { Text("Clear") },
                            Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                ) {
                    Card({}, Modifier.fillMaxWidth()) {
                        Text("This content should be partially visible.")
                    }
                }
            }
        }
    }

    @Test
    fun swipeToRevealCard_showsLargePrimaryAndSecondaryActions(
        @TestParameter screenSize: ScreenSize
    ) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                            { Text("Clear") },
                            Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    secondaryAction = {
                        SecondaryActionButton(
                            {},
                            { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                            Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                        )
                    },
                    revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
                ) {
                    Card({}, Modifier.fillMaxWidth()) {
                        Text("This content should be partially visible.")
                    }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_showsPrimaryAndSecondaryActionsLeft(@TestParameter screenSize: ScreenSize) {
        verifyScreenshotForSize(screenSize) {
            Box(modifier = Modifier.fillMaxSize()) {
                SwipeToReveal(
                    primaryAction = {
                        PrimaryActionButton(
                            onClick = {}, /* Empty for testing */
                            { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                            { Text("Clear") },
                        )
                    },
                    onSwipePrimaryAction = {}, /* Empty for testing */
                    modifier = Modifier.testTag(TEST_TAG),
                    secondaryAction = {
                        SecondaryActionButton(
                            {}, /* Empty for testing */
                            { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                        )
                    },
                    revealState = rememberRevealState(initialValue = RevealValue.LeftRevealing),
                    revealDirection = Bidirectional,
                ) {
                    Button({}, Modifier.fillMaxWidth()) {
                        Text("This text should be partially visible.")
                    }
                }
            }
        }
    }

    @Test
    fun swipeToReveal_beforeButtonVisibleThreshold_doesNotShowActions(
        @TestParameter screenSize: ScreenSize
    ) {
        val swipeScreenPercent = 0.05f

        verifyScreenshotAfterSwipe(screenSize, testName.goldenIdentifier(), swipeScreenPercent)
    }

    @Test
    fun swipeToReveal_beforeButtonFadeInThreshold_showsActions(
        @TestParameter screenSize: ScreenSize
    ) {
        val swipeScreenPercent = 0.11f

        verifyScreenshotAfterSwipe(screenSize, testName.goldenIdentifier(), swipeScreenPercent)
    }

    @Test
    fun swipeToReveal_showsPrimaryActionWithLabel(@TestParameter screenSize: ScreenSize) {
        val swipeScreenPercent = 0.85f

        verifyScreenshotAfterSwipe(screenSize, testName.goldenIdentifier(), swipeScreenPercent)
    }

    private fun verifyScreenshotAfterSwipe(
        screenSize: ScreenSize,
        goldenIdentifier: String,
        swipeScreenPercent: Float,
    ) {
        var screenWidthPx: Float? = null
        rule.setContentWithTheme {
            ScreenConfiguration(screenSize.size) {
                screenWidthPx = with(LocalDensity.current) { screenWidthDp().dp.toPx() }
                CustomTouchSlopProvider(newTouchSlop = 0f) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SwipeToReveal(
                            primaryAction = {
                                PrimaryActionButton(
                                    onClick = {}, /* Empty for testing */
                                    { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                                    { Text("Clear") },
                                )
                            },
                            onSwipePrimaryAction = {}, /* Empty for testing */
                            modifier = Modifier.testTag(TEST_TAG),
                            secondaryAction = {
                                SecondaryActionButton(
                                    {},
                                    { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                                )
                            },
                        ) {
                            Button({}, Modifier.fillMaxWidth()) { Text("Some text.") }
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput {
            down(center)
            moveTo(Offset(center.x - (screenWidthPx!! * swipeScreenPercent), center.y))
        }

        rule.verifyScreenshot(testName, screenshotRule)
    }

    private fun verifyScreenshotForSize(screenSize: ScreenSize, content: @Composable () -> Unit) {
        rule.verifyScreenshot(screenshotRule = screenshotRule, testName = testName) {
            ScreenConfiguration(screenSize.size) { content() }
        }
    }
}
