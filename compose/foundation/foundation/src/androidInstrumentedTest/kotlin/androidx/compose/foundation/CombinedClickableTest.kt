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

package androidx.compose.foundation

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.CLASSIFICATION_DEEP_PRESS
import android.view.MotionEvent.CLASSIFICATION_NONE
import android.view.View
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.first
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class CombinedClickableTest {

    @get:Rule val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    // TODO(b/267253920): Add a compose test API to set/reset InputMode.
    @After
    fun resetTouchMode() =
        with(InstrumentationRegistry.getInstrumentation()) {
            if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
        }

    @Test
    fun defaultSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").combinedClickable {},
                )
            }
        }

        rule
            .onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsEnabled()
            .assertHasClickAction()
    }

    @Test
    fun disabledSemantics() {
        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").combinedClickable(enabled = false) {},
                )
            }
        }

        rule
            .onNodeWithTag("myClickable")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assertIsNotEnabled()
            .assertHasClickAction()
    }

    @Test
    fun longClickSemantics() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            onLongClick = onLongClick
                        ) {},
                )
            }
        }

        rule
            .onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").performSemanticsAction(SemanticsActions.OnLongClick)

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun changingLongClickSemantics() {
        var counter = 0
        var onLongClick: (() -> Unit)? by mutableStateOf(null)

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            onLongClick = onLongClick
                        ) {},
                )
            }
        }

        rule
            .onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))

        rule.runOnIdle {
            // Add a no-op long click
            onLongClick = { /* no-op */ }
        }

        rule
            .onNodeWithTag("myClickable")
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            .performSemanticsAction(SemanticsActions.OnLongClick)

        rule.runOnIdle {
            // no-op long click handler
            assertThat(counter).isEqualTo(0)
            // Change to mutate counter
            onLongClick = { ++counter }
        }

        rule.onNodeWithTag("myClickable").performSemanticsAction(SemanticsActions.OnLongClick)

        rule.runOnIdle {
            // Changes should now be applied
            assertThat(counter).isEqualTo(1)
            // Make onLongClick null
            onLongClick = null
        }

        rule
            .onNodeWithTag("myClickable")
            .assertIsEnabled()
            // Long click action should be removed
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
    }

    @Test
    fun click() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier = Modifier.testTag("myClickable").combinedClickable(onClick = onClick),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    fun click_withIndirectTouchEvent() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(onClick = onClick),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent()

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    fun clickOnChildBasicText() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box(modifier = Modifier.combinedClickable(onClick = onClick)) {
                BasicText("Foo")
                BasicText("Bar")
            }
        }

        rule.onNodeWithText("Foo", substring = true).assertExists()
        rule.onNodeWithText("Bar", substring = true).assertExists()

        rule.onNodeWithText("Foo", substring = true).performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithText("Bar", substring = true).performClick()

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    @LargeTest
    fun longClick() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(onLongClick = onClick) {},
                )
            }
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.runOnIdle { assertThat(counter).isEqualTo(2) }
    }

    @Test
    @LargeTest
    fun longClick_hapticFeedbackEnabled() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val performedHaptics = mutableListOf<HapticFeedbackType>()

        val hapticFeedback: HapticFeedback =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    performedHaptics += hapticFeedbackType
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                Box {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable").combinedClickable(
                                onLongClick = onClick,
                                hapticFeedbackEnabled = true,
                            ) {},
                    )
                }
            }
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Advance a small amount of time
        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        // Releasing the press before the long click timeout shouldn't trigger haptic feedback
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
        rule.runOnIdle { assertThat(performedHaptics).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Advance past the long press timeout
        rule.mainClock.advanceTimeBy(1000)

        // Long press haptic feedback should be invoked
        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
        rule.runOnIdle {
            assertThat(performedHaptics).containsExactly(HapticFeedbackType.LongPress)
        }
    }

    @Test
    @LargeTest
    fun longClick_hapticFeedbackDisabled() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val performedHaptics = mutableListOf<HapticFeedbackType>()

        val hapticFeedback: HapticFeedback =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    performedHaptics += hapticFeedbackType
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                Box {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable").combinedClickable(
                                onLongClick = onClick,
                                hapticFeedbackEnabled = false,
                            ) {},
                    )
                }
            }
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Advance a small amount of time
        rule.mainClock.advanceTimeBy(100)

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
        rule.runOnIdle { assertThat(performedHaptics).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Advance past the long press timeout
        rule.mainClock.advanceTimeBy(1000)

        // Long press should be invoked, without any haptics
        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
        rule.runOnIdle { assertThat(performedHaptics).isEmpty() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun longClickWithEnterKeyThenDPadCenter_triggersListenerTwice() {
        var clickCounter = 0
        var longClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onLongClick = { ++longClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
            longPressKey(Key.Enter)
            longPressKey(Key.DirectionCenter)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(2)
            assertThat(clickCounter).isEqualTo(0)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun longClickWithEnterKeyConcurrentlyWithDPadCenter_triggersListenerForEach() {
        var clickCounter = 0
        var longClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onLongClick = { ++longClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
            // The press duration is 100ms longer than the minimum required for a long press.
            val durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
            keyDown(Key.Enter)
            advanceEventTime(durationMillis / 2)
            keyDown(Key.DirectionCenter)
            advanceEventTime(durationMillis / 2)
            keyUp(Key.Enter)
            advanceEventTime(durationMillis / 2)
            keyUp(Key.DirectionCenter)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(2)
            assertThat(clickCounter).isEqualTo(0)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun longClickWithEnterKeyConcurrentlyWithShortClickDPadCenter_triggersListenerForEach() {
        var clickCounter = 0
        var longClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onLongClick = { ++longClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
            // The press duration is 100ms longer than the minimum required for a long press.
            val durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
            keyDown(Key.Enter)
            advanceEventTime(durationMillis / 2)
            keyDown(Key.DirectionCenter)
            advanceEventTime(durationMillis / 2)
            keyUp(Key.Enter)
            keyUp(Key.DirectionCenter)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    fun click_withLongClick() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performTouchInput { click() }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun click_withDoubleClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(onDoubleClick = onDoubleClick, onClick = onClick),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { doubleClick() }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun click_withDoubleClick_andLongClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(
                                onDoubleClick = onDoubleClick,
                                onLongClick = onLongClick,
                                onClick = onClick,
                            ),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { doubleClick() }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    fun doubleClick_withinTimeout_aboveMinimumDuration() {
        var clickCounter = 0
        var doubleClickCounter = 0
        rule.setContent {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
            advanceEventTime(doubleTapDelay)
            down(center)
            up()
        }

        // Double click should not trigger click, and the double click should be immediately invoked
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun doubleClick_withinTimeout_belowMinimumDuration() {
        var clickCounter = 0
        var doubleClickCounter = 0
        rule.setContent {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }

        var doubleTapTimeoutDelay: Long = 0

        rule.onNodeWithTag("myClickable").performTouchInput {
            doubleTapTimeoutDelay = viewConfiguration.doubleTapTimeoutMillis + 100
            down(center)
            up()
            // Send a second press below the minimum time required for a double tap
            val minimumDuration = viewConfiguration.doubleTapMinTimeMillis
            advanceEventTime(minimumDuration / 2)
            down(center)
            up()
        }

        // Because the second tap was below the timeout, it is ignored, and so no click is invoked /
        // we are still waiting for a second tap to trigger the double click
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        // After the timeout has run out, the first click will be invoked, and no double click will
        // be invoked
        rule.mainClock.advanceTimeBy(doubleTapTimeoutDelay)
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }
    }

    @Test
    fun doubleClick_outsideTimeout() {
        var clickCounter = 0
        var doubleClickCounter = 0
        rule.setContent {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }

        var delay: Long = 0

        rule.onNodeWithTag("myClickable").performTouchInput {
            // Delay slightly past the timeout
            delay = viewConfiguration.doubleTapTimeoutMillis + 100
            down(center)
            up()
        }

        // The click should not be invoked until the timeout has run out
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        // After the timeout has run out, the click will be invoked
        rule.mainClock.advanceTimeBy(delay)
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        // Perform a second click, after the timeout has elapsed - this should not trigger a double
        // click
        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
        }

        // The second click should not be invoked until the timeout has run out
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        // After the timeout has run out, the second click will be invoked, and no double click will
        // be invoked
        rule.mainClock.advanceTimeBy(delay)
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(2)
            assertThat(doubleClickCounter).isEqualTo(0)
        }
    }

    @Test
    fun doubleClick_secondClickIsALongClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        rule.setContent {
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                            onLongClick = { ++longClickCounter },
                        ),
            )
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
            advanceEventTime(doubleTapDelay)
            down(center)
        }

        // Wait for the long click
        rule.mainClock.advanceTimeBy(1000)

        // Long click should cancel double click and click
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun interactionSource_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // No scrollable container, so there should be no delay and we should instantly appear
        // pressed
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(currentTime = 0L)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent(currentTime = 16L)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateRelease_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
        }

        // Press finished so we should see both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateRelease_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager

        rule.mainClock.autoAdvance = false

        val focusRequester = FocusRequester()

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current

            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent(16L)

        // Press finished so we should see both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateCancel_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            cancel()
        }

        // We are not in a scrollable container, so we should see a press and immediate cancel
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateCancel_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchCancelEvent(sendMoveEvents = false)

        // We are not in a scrollable container, so we should see a press and immediate cancel
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateDrag_noScrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .draggable(
                                state = rememberDraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(centerLeft)
            moveTo(centerRight)
        }

        // The press should fire, and then the drag should instantly cancel it
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateDrag_noScrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchMoveEvents(
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

        // The press should fire, and then the drag should instantly cancel it
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)

        val halfTapIndicationDelay = TapIndicationDelay / 2

        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        // Haven't reached the tap delay yet, so we shouldn't have started a press
        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Advance past the tap delay
        rule.mainClock.advanceTimeBy(halfTapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchReleaseEvent(halfTapIndicationDelay + 16L)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateRelease_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
        }

        // We haven't reached the tap delay, but we have finished a press so we should have
        // emitted both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateRelease_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L)
        rule.onNodeWithTag("myClickable").sendIndirectTouchReleaseEvent(16L)

        // We haven't reached the tap delay, but we have finished a press so we should have
        // emitted both press and release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_immediateCancel_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            cancel()
        }

        // We haven't reached the tap delay, and a cancel was emitted, so no press should ever be
        // shown
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_immediateCancel_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectTouchCancelEvent(sendMoveEvents = false)

        // We haven't reached the tap delay, and a cancel was emitted, so no press should ever be
        // shown
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_immediateDrag_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .draggable(
                                state = rememberDraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(centerLeft)
            moveTo(centerRight)
        }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_immediateDrag_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)
        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L, pressPosition)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchMoveEvents(
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_dragAfterTimeout_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .draggable(
                                state = rememberDraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(centerLeft) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { moveTo(centerRight) }

        // The drag should cancel the press
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_dragAfterTimeout_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)
        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L, pressPosition)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectTouchMoveEvents(
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )

        // The drag should cancel the press
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_cancelledGesture_scrollableContainer() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { cancel() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_cancelledGesture_scrollableContainer_indirectTouch() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box(Modifier.verticalScroll(rememberScrollState())) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val pressPosition = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f)
        rule.onNodeWithTag("myClickable").sendIndirectTouchPressEvent(0L, pressPosition)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("myClickable").sendIndirectTouchCancelEvent(sendMoveEvents = false)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitClickableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitClickableText) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable").combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose clickable
        rule.runOnIdle { emitClickableText = false }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_hover() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[1] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_hover_and_press() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput {
            enter(center)
            click()
            exit(Offset(-1f, -1f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions[0]).isInstanceOf(HoverInteraction.Enter::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[3]).isInstanceOf(HoverInteraction.Exit::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
            assertThat((interactions[3] as HoverInteraction.Exit).enter).isEqualTo(interactions[0])
        }
    }

    @Test
    fun interactionSource_focus_inTouchMode() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Touch) }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Touch mode by default, so we shouldn't be focused
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun interactionSource_focus_inKeyboardMode() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var focusManager: FocusManager
        lateinit var inputModeManager: InputModeManager
        rule.setFocusableContent {
            scope = rememberCoroutineScope()
            focusManager = LocalFocusManager.current
            inputModeManager = LocalInputModeManager.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }
        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.runOnIdle { focusRequester.requestFocus() }

        // Keyboard mode, so we should now be focused and see an interaction
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
        }

        rule.runOnIdle { focusManager.clearFocus() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[1] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }
    }

    // TODO: b/202871171 - add test for changing between keyboard mode and touch mode, making sure
    // it resets existing focus

    /**
     * Regression test for b/186223077
     *
     * Tests that if a long click causes the long click lambda to change instances, we will still
     * correctly wait for the up event and emit [PressInteraction.Release].
     */
    @Test
    @LargeTest
    fun longClick_interactionSource_continuesTrackingPressAfterLambdasChange() {
        val interactionSource = MutableInteractionSource()

        var onLongClick by mutableStateOf({})
        val finalLongClick = {}
        val initialLongClick = { onLongClick = finalLongClick }
        // Simulate the long click causing a recomposition, and changing the lambda instance
        onLongClick = initialLongClick

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            onLongClick = onLongClick,
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Simulate a long click
        rule.mainClock.advanceTimeBy(1000)
        // Run another frame to trigger recomposition caused by the long click
        rule.mainClock.advanceTimeByFrame()

        // We should have a press interaction, with no release, even though the lambda instance
        // has changed
        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(onLongClick).isEqualTo(finalLongClick)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        // The up should now cause a release
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    /**
     * Regression test for b/186223077
     *
     * Tests that if a long click causes the long click lambda to become null, we will emit
     * [PressInteraction.Cancel].
     */
    @Test
    @LargeTest
    fun longClick_interactionSource_cancelsIfLongClickBecomesNull() {
        val interactionSource = MutableInteractionSource()

        var onLongClick: (() -> Unit)? by mutableStateOf(null)
        val initialLongClick = { onLongClick = null }
        // Simulate the long click causing a recomposition, and changing the lambda to be null
        onLongClick = initialLongClick

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            onLongClick = onLongClick,
                            interactionSource = interactionSource,
                            indication = null,
                        ) {},
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Initial press
        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        // Long click
        rule.mainClock.advanceTimeBy(1000)
        // Run another frame to trigger recomposition caused by the long click
        rule.mainClock.advanceTimeByFrame()

        // The new onLongClick lambda should be null, and so we should cancel the existing press.
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
            assertThat(onLongClick).isNull()
        }
    }

    @Test
    @LargeTest
    fun click_withDoubleClick_andLongClick_disabled() {
        val enabled = mutableStateOf(false)
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(
                                enabled = enabled.value,
                                onDoubleClick = onDoubleClick,
                                onLongClick = onLongClick,
                                onClick = onClick,
                            ),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { doubleClick() }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { doubleClick() }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun clicks_consumedWhenDisabled() {
        val enabled = mutableStateOf(false)
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }
        var outerClickCounter = 0
        var outerDoubleClickCounter = 0
        var outerLongClickCounter = 0
        val outerOnClick: () -> Unit = { ++outerClickCounter }
        val outerOnDoubleClick: () -> Unit = { ++outerDoubleClickCounter }
        val outerOnLongClick: () -> Unit = { ++outerLongClickCounter }

        rule.setContent {
            Box(
                Modifier.combinedClickable(
                    onDoubleClick = outerOnDoubleClick,
                    onLongClick = outerOnLongClick,
                    onClick = outerOnClick,
                )
            ) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(
                                enabled = enabled.value,
                                onDoubleClick = onDoubleClick,
                                onLongClick = onLongClick,
                                onClick = onClick,
                            ),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performClick()

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { doubleClick() }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
            enabled.value = true
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { doubleClick() }

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.mainClock.advanceTimeUntil { longClickCounter == 1 }

        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(1)
            assertThat(outerDoubleClickCounter).isEqualTo(0)
            assertThat(outerLongClickCounter).isEqualTo(0)
            assertThat(outerClickCounter).isEqualTo(0)
        }
    }

    @Test
    @LargeTest
    fun noHover_whenDisabled() {
        val interactionSource = MutableInteractionSource()

        lateinit var scope: CoroutineScope
        val enabled = mutableStateOf(true)

        rule.setContent {
            scope = rememberCoroutineScope()
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .combinedClickable(
                            enabled = enabled.value,
                            onClick = {},
                            interactionSource = interactionSource,
                            indication = null,
                        ),
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle {
            interactions.clear()
            enabled.value = false
        }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }

        rule.runOnIdle { enabled.value = true }

        rule.onNodeWithTag("myClickable").performMouseInput { enter(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(HoverInteraction.Enter::class.java)
        }

        rule.onNodeWithTag("myClickable").performMouseInput { exit(Offset(-1f, -1f)) }
    }

    @Test
    fun noFocus_whenDisabled() {
        val requester = FocusRequester()
        // Force clickable to always be in non-touch mode, so it should be focusable
        val keyboardMockManager =
            object : InputModeManager {
                override val inputMode = Keyboard

                override fun requestInputMode(inputMode: InputMode) = true
            }

        val enabled = mutableStateOf(true)
        lateinit var focusState: FocusState

        rule.setContent {
            CompositionLocalProvider(LocalInputModeManager provides keyboardMockManager) {
                Box {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable")
                                .focusRequester(requester)
                                .onFocusEvent { focusState = it }
                                .combinedClickable(enabled = enabled.value) {},
                    )
                }
            }
        }

        rule.runOnIdle {
            requester.requestFocus()
            assertThat(focusState.isFocused).isTrue()
        }

        rule.runOnIdle { enabled.value = false }

        rule.runOnIdle {
            assertThat(focusState.isFocused).isFalse()
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    /** Test for b/269319898 */
    @Test
    fun noFocusPropertiesSet_whenDisabled() {
        val requester = FocusRequester()
        // Force clickable to always be in non-touch mode, so it should be focusable
        val keyboardMockManager =
            object : InputModeManager {
                override val inputMode = Keyboard

                override fun requestInputMode(inputMode: InputMode) = true
            }

        val enabled = mutableStateOf(true)
        lateinit var focusState: FocusState

        rule.setContent {
            CompositionLocalProvider(LocalInputModeManager provides keyboardMockManager) {
                Box(Modifier.combinedClickable(enabled = enabled.value, onClick = {})) {
                    Box(
                        Modifier.size(10.dp)
                            // If clickable is setting canFocus to true without a focus target, then
                            // that would override this property
                            .focusProperties { canFocus = false }
                            .focusRequester(requester)
                            .onFocusEvent { focusState = it }
                            .focusable()
                    )
                }
            }
        }

        // b/314129026 we can't read canFocus, so instead try and request focus and make sure
        // that we are not focused
        rule.runOnIdle {
            // Clickable is enabled, it should correctly apply properties to its focus node
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }

        rule.runOnIdle { enabled.value = false }

        rule.runOnIdle {
            // Clickable is disabled, it should not apply properties down the tree
            requester.requestFocus()
            assertThat(focusState.isFocused).isFalse()
        }
    }

    @Test
    fun testInspectorValue_noIndicationOverload() {
        val onClick: () -> Unit = {}
        rule.setContent {
            val modifier = Modifier.combinedClickable(onClick = onClick) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("combinedClickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "interactionSource",
                    "indicationNodeFactory",
                    "enabled",
                    "onClickLabel",
                    "role",
                    "onClick",
                    "onDoubleClick",
                    "onLongClick",
                    "onLongClickLabel",
                    "hapticFeedbackEnabled",
                )
        }
    }

    @Test
    fun testInspectorValue_fullParamsOverload() {
        val onClick: () -> Unit = {}
        rule.setContent {
            val modifier =
                Modifier.combinedClickable(
                        onClick = onClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    )
                    .first() as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("combinedClickable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "enabled",
                    "onClickLabel",
                    "onClick",
                    "role",
                    "onDoubleClick",
                    "onLongClick",
                    "onLongClickLabel",
                    "indicationNodeFactory",
                    "interactionSource",
                    "hapticFeedbackEnabled",
                )
        }
    }

    @Test
    fun clickInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier.requiredHeight(20.dp)
                    .requiredWidth(20.dp)
                    .clipToBounds()
                    .combinedClickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performTouchInput { click(Offset(-1f, -1f)) }

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    fun clickInVerticalTargetInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier.requiredHeight(50.dp)
                    .requiredWidth(20.dp)
                    .clipToBounds()
                    .combinedClickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(50.dp)
            .assertTouchHeightIsEqualTo(50.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performTouchInput { click(Offset(-1f, 0f)) }

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    fun clickInHorizontalTargetInMinimumTouchArea() {
        var clicked by mutableStateOf(false)
        val tag = "my clickable"
        rule.setContent {
            Box(
                Modifier.requiredHeight(20.dp)
                    .requiredWidth(50.dp)
                    .clipToBounds()
                    .combinedClickable { clicked = true }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(50.dp)
            .performTouchInput { click(Offset(0f, -1f)) }

        rule.runOnIdle { assertThat(clicked).isTrue() }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun otherKey_doesNotEmitIndication() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {},
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").performKeyInput { pressKey(Key.Backspace) }
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @Test
    fun localIndication_interactionSource_eagerlyCreated() {
        val interactionSource = MutableInteractionSource()
        var created = false
        val indication = TestIndicationNodeFactory { _, _ -> created = true }
        rule.setContent {
            CompositionLocalProvider(LocalIndication provides indication) {
                Box(Modifier.padding(10.dp)) {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("clickable").combinedClickable(
                                interactionSource = interactionSource
                            ) {},
                    )
                }
            }
        }
        rule.runOnIdle { assertThat(created).isTrue() }
    }

    // Regression test for b/332814226
    @Test
    fun movableContentWithSubcomposition_updatingSemanticsShouldNotCrash() {
        var moveContent by mutableStateOf(false)
        rule.setContent {
            val content = remember {
                movableContentOf {
                    BoxWithConstraints {
                        BasicText(
                            "ClickableText",
                            modifier =
                                Modifier.testTag("clickable").combinedClickable(
                                    role = if (moveContent) Role.Button else Role.Checkbox,
                                    onClickLabel = moveContent.toString(),
                                    onLongClick = {},
                                    onLongClickLabel = moveContent.toString(),
                                ) {},
                        )
                    }
                }
            }

            key(moveContent) { content() }
        }

        rule
            .onNodeWithTag("clickable")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertOnClickLabelMatches("false")
            .assertOnLongClickLabelMatches("false")

        rule.runOnIdle { moveContent = true }

        rule
            .onNodeWithTag("clickable")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertOnClickLabelMatches("true")
            .assertOnLongClickLabelMatches("true")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun longClick_deepPress() {
        lateinit var view: View
        var clicks = 0
        var longClicks = 0
        var doubleClicks = 0
        val onClick: () -> Unit = { ++clicks }
        val onLongClick: () -> Unit = { ++longClicks }
        val onDoubleClick: () -> Unit = { ++doubleClicks }
        rule.setContent {
            view = LocalView.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(
                                onClick = onClick,
                                onLongClick = onLongClick,
                                onDoubleClick = onDoubleClick,
                            ),
                )
            }
        }

        val pointerProperties =
            arrayOf(
                MotionEvent.PointerProperties().also {
                    it.id = 0
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            )

        val downEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 0,
                /* action = */ ACTION_DOWN,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = 5f
                        y = 5f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_NONE,
            )

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(0)
            assertThat(doubleClicks).isEqualTo(0)
        }

        val deepPressMoveEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 50,
                /* action = */ ACTION_MOVE,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = 10f
                        y = 10f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_DEEP_PRESS,
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        // Even though the timeout didn't pass, the deep press should immediately trigger the long
        // click
        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(1)
            assertThat(doubleClicks).isEqualTo(0)
        }
    }

    /** Detect the second deep press as long click. */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun secondTapLongClick_deepPress() {
        lateinit var view: View
        var clicks = 0
        var longClicks = 0
        var doubleClicks = 0
        val onClick: () -> Unit = { ++clicks }
        val onLongClick: () -> Unit = { ++longClicks }
        val onDoubleClick: () -> Unit = { ++doubleClicks }
        rule.setContent {
            view = LocalView.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .combinedClickable(
                                onClick = onClick,
                                onLongClick = onLongClick,
                                onDoubleClick = onDoubleClick,
                            ),
                )
            }
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(0, Offset(5f, 5f))
            up(0)
        }
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(0)
            assertThat(doubleClicks).isEqualTo(0)
        }

        val pointerProperties =
            arrayOf(
                MotionEvent.PointerProperties().also {
                    it.id = 0
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            )

        val downEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 50,
                /* action = */ ACTION_DOWN,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = 5f
                        y = 5f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_NONE,
            )

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(0)
            assertThat(doubleClicks).isEqualTo(0)
        }

        val deepPressMoveEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 100,
                /* action = */ ACTION_MOVE,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    MotionEvent.PointerCoords().apply {
                        x = 10f
                        y = 10f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_DEEP_PRESS,
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        // Even though the timeout didn't pass, the deep press should immediately trigger the long
        // press
        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(1)
            assertThat(doubleClicks).isEqualTo(0)
        }
    }
}

private fun SemanticsNodeInteraction.assertOnLongClickLabelMatches(
    expectedValue: String
): SemanticsNodeInteraction {
    return assert(
        SemanticsMatcher("onLongClickLabel = '$expectedValue'") {
            it.config.getOrElseNullable(SemanticsActions.OnLongClick) { null }?.label ==
                expectedValue
        }
    )
}

private fun KeyInjectionScope.longPressKey(key: Key) {
    // The press duration is 100ms longer than the minimum required for a long press.
    val durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
    pressKey(key, durationMillis)
}
