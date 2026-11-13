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

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.activity.BackEventCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.foundation.LocalReduceMotion
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class DialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            Dialog(visible = true, modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dialogContent_composedOnce() {
        var recomposeCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(false) }
            Button(modifier = Modifier.testTag(SHOW_BUTTON_TAG), onClick = { visible = true }) {}

            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            ) {
                recomposeCounter++
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(1, recomposeCounter)
    }

    @Test
    fun displays_content() {
        rule.setContentWithTheme {
            Dialog(visible = true, onDismissRequest = {}) {
                Text("Text", modifier = Modifier.testTag(TEST_TAG))
            }
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
            ) {}
        }

        rule.swipeToDismiss()
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @Test
    fun swipeToDismissDialog_supports_swipeToDismiss() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            SwipeToDismissDialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
            ) {}
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight() }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_scalesParentScaffold_andTransformsDialog_duringProgress() {
        var scaffoldState: AppScaffoldState? = null
        rule.setContentWithTheme {
            AppScaffold {
                scaffoldState = LocalScaffoldState.current as? AppScaffoldState
                Dialog(
                    visible = true,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {}
            }
        }
        rule.waitForIdle()
        Assert.assertEquals(0.85f, scaffoldState!!.parentScale.floatValue, 0.01f)
        val initialBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.0f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.5f, BackEventCompat.EDGE_LEFT)
            )
        }
        rule.waitForIdle()

        // At progress = 0.5f, parentScale is lerp(0.85f, 1.0f, 0.5f) = 0.925f
        Assert.assertEquals(0.925f, scaffoldState!!.parentScale.floatValue, 0.01f)

        // Dialog should be translated right (0.5f * width) and scaled down around center
        // (lerp(1f, 0.8f, 0.5f) = 0.9f, which shifts top-left by 0.05f * size).
        val progressedBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        Assert.assertEquals(
            initialBounds.width.value * 0.55f,
            progressedBounds.left.value,
            2f,
        )
        Assert.assertEquals(
            initialBounds.height.value * 0.05f,
            progressedBounds.top.value,
            2f,
        )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_cancelsAndRestoresState_whenCancelled() {
        var scaffoldState: AppScaffoldState? = null
        var dismissCounter = 0
        rule.setContentWithTheme {
            AppScaffold {
                scaffoldState = LocalScaffoldState.current as? AppScaffoldState
                Dialog(
                    visible = true,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = { dismissCounter++ },
                ) {}
            }
        }
        rule.waitForIdle()
        val initialBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.1f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.6f, BackEventCompat.EDGE_LEFT)
            )
        }
        rule.waitForIdle()
        Assert.assertTrue(scaffoldState!!.parentScale.floatValue > 0.85f)

        rule.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        rule.waitForIdle()

        // Dialog must remain visible, not invoke onDismissRequest, and restore bounds & parentScale
        rule.onNodeWithTag(TEST_TAG).assertExists()
        Assert.assertEquals(0, dismissCounter)
        Assert.assertEquals(0.85f, scaffoldState!!.parentScale.floatValue, 0.01f)
        val restoredBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        Assert.assertEquals(initialBounds.left.value, restoredBounds.left.value, 1f)
        Assert.assertEquals(initialBounds.width.value, restoredBounds.width.value, 1f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_canDismissAgain_afterReopeningDialog() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            Button(
                modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                onClick = { visible = true },
            ) {}
            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
            ) {}
        }

        val initialBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()

        // First predictive back dismissal
        rule.swipeToDismiss()
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)

        // Reopen the dialog
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
        val reopenedBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        Assert.assertEquals(initialBounds.left.value, reopenedBounds.left.value, 1f)
        Assert.assertEquals(initialBounds.width.value, reopenedBounds.width.value, 1f)

        // Second predictive back dismissal
        rule.swipeToDismiss()
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(2, dismissCounter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_respectsDismissOnBackPressFalse() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            Dialog(
                visible = true,
                modifier = Modifier.testTag(TEST_TAG),
                properties = DialogProperties(dismissOnBackPress = false),
                onDismissRequest = { dismissCounter++ },
            ) {}
        }

        rule.swipeToDismiss()
        rule.waitForIdle()
        rule.onNodeWithTag(TEST_TAG).assertExists()
        Assert.assertEquals(0, dismissCounter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_clipsDialogToCircle_onRoundDevice() {
        val dialogColor = Color.Blue
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE, isRound = true) {
                Dialog(
                    visible = true,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(dialogColor))
                }
            }
        }
        rule.waitForIdle()

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.0f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.2f, BackEventCompat.EDGE_LEFT)
            )
        }
        rule.waitForIdle()

        val pixelMap = rule.onNodeWithTag(TEST_TAG).captureToImage().toPixelMap()
        // At vertical center near the left edge, the pixel is inside the clipped circle.
        Assert.assertEquals(dialogColor, pixelMap[5, pixelMap.height / 2])
        // At the top-left corner of the dialog bounds, the pixel is outside the clipped circle.
        Assert.assertNotEquals(dialogColor, pixelMap[2, 2])
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_clipsDialogToCircle_onlyWhenSwiping_onRoundDevice() {
        val dialogColor = Color.Blue
        rule.setContentWithTheme {
            ScreenConfiguration(SCREEN_SIZE_LARGE, isRound = true) {
                Dialog(
                    visible = true,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(dialogColor))
                }
            }
        }
        rule.waitForIdle()

        // Before swiping, the top-left corner of the dialog should not be clipped.
        val initialPixelMap = rule.onNodeWithTag(TEST_TAG).captureToImage().toPixelMap()
        Assert.assertEquals(dialogColor, initialPixelMap[2, 2])

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.0f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.2f, BackEventCompat.EDGE_LEFT)
            )
        }
        rule.waitForIdle()

        // While swiping, the top-left corner of the dialog should be clipped to a circle.
        val swipingPixelMap = rule.onNodeWithTag(TEST_TAG).captureToImage().toPixelMap()
        Assert.assertEquals(dialogColor, swipingPixelMap[5, swipingPixelMap.height / 2])
        Assert.assertNotEquals(dialogColor, swipingPixelMap[2, 2])

        rule.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        rule.waitForIdle()

        // After cancelling the swipe and returning to rest, the top-left corner should not be
        // clipped.
        val restoredPixelMap = rule.onNodeWithTag(TEST_TAG).captureToImage().toPixelMap()
        Assert.assertEquals(dialogColor, restoredPixelMap[2, 2])
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_commitNotCancelled_bySubsequentBackGesture() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            var visible by remember { mutableStateOf(true) }
            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
            ) {}
        }
        rule.waitForIdle()

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.1f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.5f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.onBackPressed()
        }
        rule.mainClock.advanceTimeBy(48)

        // Start and cancel a new back gesture while the commit settle animation is in flight.
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.1f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackCancelled()
        }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_doesNotCallOnDismissRequest_whenHiddenMidGesture() {
        var visible by mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            Dialog(
                visible = visible,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {
                    visible = false
                    dismissCounter++
                },
            ) {}
        }
        rule.waitForIdle()

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.1f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.5f, BackEventCompat.EDGE_LEFT)
            )
        }
        rule.mainClock.advanceTimeByFrame()

        // Hide the dialog mid-gesture (e.g. a ConfirmationDialog timeout).
        visible = false
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle { dispatcher.onBackPressed() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(0, dismissCounter)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    @Test
    fun predictiveBack_snapsOnCancelAndCommit_whenReduceMotionEnabled() {
        var dismissCounter = 0
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                var visible by remember { mutableStateOf(true) }
                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {
                        visible = false
                        dismissCounter++
                    },
                ) {}
            }
        }
        rule.waitForIdle()
        val initialBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()

        val dispatcher = rule.dialogOnBackPressedDispatcher(TEST_TAG)
        rule.mainClock.autoAdvance = false

        // Cancel with reduce motion enabled should snap back to initial bounds immediately.
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.1f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.6f, BackEventCompat.EDGE_LEFT)
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { dispatcher.dispatchOnBackCancelled() }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        val restoredBounds = rule.onNodeWithTag(TEST_TAG).getUnclippedBoundsInRoot()
        Assert.assertEquals(initialBounds.left.value, restoredBounds.left.value, 1f)
        Assert.assertEquals(0, dismissCounter)

        // Commit with reduce motion enabled should snap and dismiss immediately.
        rule.runOnIdle {
            dispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1f, 0.1f, 0.1f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1f, 0.1f, 0.6f, BackEventCompat.EDGE_LEFT)
            )
            dispatcher.onBackPressed()
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        Assert.assertEquals(1, dismissCounter)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onDismissRequest_not_called_when_hidden() {
        val visible = mutableStateOf(true)
        var dismissCounter = 0
        rule.setContentWithTheme {
            Dialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { dismissCounter++ },
                visible = visible.value,
            ) {}
        }
        rule.waitForIdle()
        visible.value = false
        rule.waitUntilDoesNotExist(hasTestTag(TEST_TAG))
        Assert.assertEquals(0, dismissCounter)
    }

    @Test
    fun hides_dialog_when_visible_false() {
        rule.setContentWithTheme {
            Dialog(modifier = Modifier.testTag(TEST_TAG), onDismissRequest = {}, visible = false) {}
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun shrink_background_when_dialog_is_shown() {
        var scaffoldState: AppScaffoldState? = null
        rule.setContentWithTheme {
            AppScaffold {
                scaffoldState = LocalScaffoldState.current as? AppScaffoldState
                var visible by remember { mutableStateOf(false) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = true },
                ) {}

                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {}
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        assert(scaffoldState!!.parentScale.floatValue < 1f)
    }

    @Test
    fun expand_background_when_dialog_is_hidden() {
        var scaffoldState: AppScaffoldState? = null
        rule.setContentWithTheme {
            AppScaffold {
                scaffoldState = LocalScaffoldState.current as? AppScaffoldState
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false },
                ) {}

                Dialog(
                    visible = visible,
                    modifier = Modifier.testTag(TEST_TAG),
                    onDismissRequest = {},
                ) {}
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(scaffoldState!!.parentScale.floatValue, 1f, 0.01f)
    }

    @Test
    fun expand_background_when_dialog_is_removed() {
        var scaffoldState: AppScaffoldState? = null
        rule.setContentWithTheme {
            AppScaffold {
                scaffoldState = LocalScaffoldState.current as? AppScaffoldState
                var visible by remember { mutableStateOf(true) }
                Button(
                    modifier = Modifier.testTag(SHOW_BUTTON_TAG),
                    onClick = { visible = false },
                ) {}

                if (visible) {
                    Dialog(
                        visible = visible,
                        modifier = Modifier.testTag(TEST_TAG),
                        onDismissRequest = {},
                    ) {}
                }
            }
        }
        rule.onNodeWithTag(SHOW_BUTTON_TAG).performClick()
        rule.waitForIdle()
        Assert.assertEquals(scaffoldState!!.parentScale.floatValue, 1f, 0.01f)
    }

    @Test
    fun dialogProperties_whenStatusBarEnabled_forcesEdgeToEdgeAndPreservesProperties() {
        var window: Window? = null
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides true) {
                Dialog(
                    visible = true,
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            windowTitle = "CustomTitle",
                            decorFitsSystemWindows = true,
                            usePlatformDefaultWidth = true,
                        ),
                ) {
                    var parent = LocalView.current.parent
                    while (parent != null && parent !is DialogWindowProvider) {
                        parent = parent.parent
                    }
                    window = (parent as? DialogWindowProvider)?.window
                }
            }
        }
        rule.waitForIdle()
        Assert.assertNotNull(window)
        Assert.assertEquals("CustomTitle", window!!.attributes.title)
        val flags = window!!.attributes.flags
        Assert.assertTrue(
            "Expected FLAG_LAYOUT_IN_SCREEN to be set when decorFitsSystemWindows is forced to false",
            (flags and WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN) != 0,
        )
    }

    @Test
    fun dialogProperties_whenStatusBarDisabled_preservesPassedProperties() {
        var window: Window? = null
        rule.setContentWithTheme {
            CompositionLocalProvider(LocalStatusBarEnabledForTest provides false) {
                Dialog(
                    visible = true,
                    onDismissRequest = {},
                    properties =
                        DialogProperties(
                            windowTitle = "CustomTitle",
                            securePolicy = SecureFlagPolicy.SecureOn,
                        ),
                ) {
                    var parent = LocalView.current.parent
                    while (parent != null && parent !is DialogWindowProvider) {
                        parent = parent.parent
                    }
                    window = (parent as? DialogWindowProvider)?.window
                }
            }
        }
        rule.waitForIdle()
        Assert.assertNotNull(window)
        Assert.assertEquals("CustomTitle", window!!.attributes.title)
        val flags = window!!.attributes.flags
        Assert.assertTrue(
            "Expected FLAG_SECURE to be set when securePolicy is SecureOn",
            (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0,
        )
    }

    @Test
    fun plainDialog_withoutScaffold_providesLocalScaffoldStatusBarStateDisabled() {
        var observedState: ScaffoldStatusBarState? = null
        rule.setContentWithTheme {
            WithMockStatusBarInsets {
                Dialog(visible = true, onDismissRequest = {}) {
                    observedState = LocalScaffoldStatusBarState.current
                }
            }
        }
        rule.waitForIdle()
        Assert.assertNotNull(observedState)
        Assert.assertEquals(false, observedState?.showStatusBar?.value)
    }

    @Test
    fun plainDialog_withInnerScreenScaffoldInherit_inheritsDisabled() {
        var innerShow: Boolean? = null
        val observedPaddings = mutableListOf<Dp>()
        rule.setContentWithTheme {
            WithMockStatusBarInsets {
                Dialog(visible = true, onDismissRequest = {}) {
                    ScreenScaffold(contentPadding = PaddingValues(0.dp)) { contentPadding ->
                        Box(
                            modifier =
                                Modifier.fillMaxSize().layout { measurable, constraints ->
                                    observedPaddings.add(contentPadding.calculateTopPadding())
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(0, 0)
                                    }
                                }
                        )
                        innerShow = LocalScaffoldStatusBarState.current?.showStatusBar?.value
                    }
                }
            }
        }
        rule.waitForIdle()
        Assert.assertEquals(false, innerShow)
        Assert.assertTrue("Observed at least one measure pass", observedPaddings.isNotEmpty())
        Assert.assertTrue(
            "Inner ScreenScaffold inside Dialog must inherit disabled status bar and collapse padding to 0.dp across all frames, but observed: $observedPaddings",
            observedPaddings.all { it == 0.dp },
        )
    }

    @Test
    fun compositingStrategy_atRest_usesAuto() {
        Assert.assertEquals(
            CompositingStrategy.Auto,
            dialogContentCompositingStrategy(
                isTransitionRunning = false,
                isGestureInProgress = isSwipeInProgress(0f),
            ),
        )
    }

    @Test
    fun compositingStrategy_uninitializedSwipeOffset_usesAuto() {
        Assert.assertEquals(
            CompositingStrategy.Auto,
            dialogContentCompositingStrategy(
                isTransitionRunning = false,
                isGestureInProgress = isSwipeInProgress(Float.NaN),
            ),
        )
    }

    @Test
    fun compositingStrategy_transitionRunning_usesOffscreen() {
        Assert.assertEquals(
            CompositingStrategy.Offscreen,
            dialogContentCompositingStrategy(
                isTransitionRunning = true,
                isGestureInProgress = isSwipeInProgress(0f),
            ),
        )
        Assert.assertEquals(
            CompositingStrategy.Offscreen,
            dialogContentCompositingStrategy(
                isTransitionRunning = true,
                isGestureInProgress = isSwipeInProgress(Float.NaN),
            ),
        )
    }

    @Test
    fun compositingStrategy_swipeInProgress_usesOffscreen() {
        Assert.assertEquals(
            CompositingStrategy.Offscreen,
            dialogContentCompositingStrategy(
                isTransitionRunning = false,
                isGestureInProgress = isSwipeInProgress(0.5f),
            ),
        )
        Assert.assertEquals(
            CompositingStrategy.Offscreen,
            dialogContentCompositingStrategy(
                isTransitionRunning = false,
                isGestureInProgress = isSwipeInProgress(200f),
            ),
        )
    }

    @Test
    fun compositingStrategy_swipeInProgressDuringTransition_usesOffscreen() {
        Assert.assertEquals(
            CompositingStrategy.Offscreen,
            dialogContentCompositingStrategy(
                isTransitionRunning = true,
                isGestureInProgress = isSwipeInProgress(200f),
            ),
        )
    }
}

private const val SHOW_BUTTON_TAG = "show-button"

@Suppress("UNCHECKED_CAST")
private val LocalStatusBarEnabledForTest: ProvidableCompositionLocal<Boolean>
    get() = LocalStatusBarEnabled as ProvidableCompositionLocal<Boolean>
