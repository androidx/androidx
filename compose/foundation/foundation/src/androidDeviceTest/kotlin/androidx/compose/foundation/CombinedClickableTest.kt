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
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.CLASSIFICATION_DEEP_PRESS
import android.view.MotionEvent.CLASSIFICATION_NONE
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ComposeFoundationFlags.isDelayPressesUsingGestureConsumptionEnabled
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.elementFor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.first
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputMode.Companion.Touch
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputModifierNode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CombinedClickableTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

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
    fun click_withIndirectPointerEvent() {
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

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

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
    fun longClick_withIndirectPointerEvent() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(onLongClick = onLongClick) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
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
    fun longClick_hapticFeedbackEnabled_indirectPointer() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val performedHaptics = mutableListOf<HapticFeedbackType>()

        val hapticFeedback: HapticFeedback =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    performedHaptics += hapticFeedbackType
                }
            }

        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                Box {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable")
                                .focusRequester(focusRequester)
                                .combinedClickable(
                                    onLongClick = onClick,
                                    hapticFeedbackEnabled = true,
                                ) {},
                    )
                }
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        // Advance a small amount of time
        rule.mainClock.advanceTimeBy(100)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        // Releasing the press before the long click timeout shouldn't trigger haptic feedback
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
        rule.runOnIdle { assertThat(performedHaptics).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

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
    @LargeTest
    fun longClick_hapticFeedbackDisabled_indirectPointer() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val performedHaptics = mutableListOf<HapticFeedbackType>()

        val hapticFeedback: HapticFeedback =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    performedHaptics += hapticFeedbackType
                }
            }

        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                Box {
                    BasicText(
                        "ClickableText",
                        modifier =
                            Modifier.testTag("myClickable")
                                .focusRequester(focusRequester)
                                .combinedClickable(
                                    onLongClick = onClick,
                                    hapticFeedbackEnabled = false,
                                ) {},
                    )
                }
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        // Advance a small amount of time
        rule.mainClock.advanceTimeBy(100)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
        rule.runOnIdle { assertThat(performedHaptics).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        // Advance past the long press timeout
        rule.mainClock.advanceTimeBy(1000)

        // Long press should be invoked, without any haptics
        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
        rule.runOnIdle { assertThat(performedHaptics).isEmpty() }
    }

    @Test
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
    @LargeTest
    fun longClick_consumesEventsAfterLongClick() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<PointerEvent>()

        rule.setContent {
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        receivedEvents += event
                                    }
                                }
                            }
                            .combinedClickable(onLongClick = onLongClick) {},
                )
            }
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            moveBy(Offset(1f, 1f))
        }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Press)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis + 100
            advanceEventTime(longPressTimeout)
            moveBy(Offset(1f, 1f))
            up()
        }

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Release)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    @Test
    @LargeTest
    fun longClick_consumesEventsAfterLongClick_outOfBounds() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<PointerEvent>()
        val clickableSize = 100
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier =
                    Modifier.testTag("myClickable")
                        .size(with(LocalDensity.current) { clickableSize.toDp() })
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    receivedEvents += event
                                }
                            }
                        }
                        .combinedClickable(onLongClick = onLongClick) {}
            )
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            moveBy(Offset(1f, 1f))
        }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Press)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        val longPressTimeout = viewConfiguration.longPressTimeoutMillis + 100
        rule.mainClock.advanceTimeBy(longPressTimeout)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            // Move outside the touch bounds - normally this would cancel input, but since we
            // already triggered a long click, we still want to consume events until all pointers
            // are up (even out of bounds)
            moveBy(Offset(clickableSize * 2f, clickableSize * 2f))
            up()
        }

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Release)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    /**
     * Test case to make sure that after a long press is triggered, we consume _all_ pointer events,
     * even if a child consumed an event after the long press was triggered
     */
    @Test
    @LargeTest
    fun longClick_consumesEventsAfterLongClick_childConsumesFirst() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<PointerEvent>()
        var consumeEventsInChild = false
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier =
                    Modifier.size(100.dp)
                        .testTag("myClickable")
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    receivedEvents += awaitPointerEvent()
                                }
                            }
                        }
                        .combinedClickable(onLongClick = onLongClick) {}
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (consumeEventsInChild) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
            )
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            moveBy(Offset(1f, 1f))
        }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Press)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        val longPressTimeout = viewConfiguration.longPressTimeoutMillis + 100
        rule.mainClock.advanceTimeBy(longPressTimeout)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(0)
            // Start consuming events in the child
            consumeEventsInChild = true
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            // Move - this move will be consumed by the child
            moveBy(Offset(1f, 1f))
        }

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(1)
            // The move will be consumed by the child
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            receivedEvents.clear()
            // Stop consuming events in the child
            consumeEventsInChild = false
        }

        rule.onNodeWithTag("myClickable").performTouchInput {
            // Move again
            moveBy(Offset(1f, 1f))
            up()
        }

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up, even though the child consumed
            // an event before this
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Release)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    @Test
    fun longClick_childConsumesUp_handlesNextClick() {
        var clickCounter = 0
        var longClickCounter = 0
        var consumeEventsInChild = false
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier =
                    Modifier.testTag("myClickable")
                        .size(100.dp)
                        .combinedClickable(
                            onClick = { ++clickCounter },
                            onLongClick = { ++longClickCounter },
                        )
                        // The modifier order here places this pointerInput deeper in the node tree.
                        // During the Main pass, it will intercept and consume events before
                        // combinedClickable.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (consumeEventsInChild) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
            )
        }

        // Start a long press gesture
        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        val longPressTimeout = viewConfiguration.longPressTimeoutMillis + 100
        rule.mainClock.advanceTimeBy(longPressTimeout)

        rule.runOnIdle {
            // Ensure the long click triggered properly
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(0)

            // Instruct the child to start swallowing events before the UP event fires
            consumeEventsInChild = true
        }

        // Release the pointer
        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle {
            // Stop consuming events so we can attempt a normal click
            consumeEventsInChild = false
        }

        // Attempt a normal click
        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun longClick_consumesEventsAfterLongClick_indirectPointer() {
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<IndirectPointerEvent>()
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        val indirectEventCapturingNode =
            object : IndirectPointerInputModifierNode, Modifier.Node() {
                override fun onIndirectPointerEvent(
                    event: IndirectPointerEvent,
                    pass: PointerEventPass,
                ) {
                    if (pass == PointerEventPass.Main) {
                        receivedEvents.add(event)
                    }
                }

                override fun onCancelIndirectPointerInput() {}
            }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .elementFor(indirectEventCapturingNode)
                            .combinedClickable(onLongClick = onClick) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent =
            rule
                .onNodeWithTag("myClickable")
                .sendIndirectPointerPressEvent(rule, currentValue = Offset.Zero)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 16L,
                currentValue = Offset.Zero,
                delayTimeMills = 16L,
                stepSize = Offset(1f, 1f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Press).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(0)
        }

        // Move again to trigger consumption check
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 32L,
                currentValue = Offset(1f, 1f),
                delayTimeMills = 16L,
                stepSize = Offset(1f, 1f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type == IndirectPointerEventType.Release).isTrue()
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    @Test
    @LargeTest
    fun longClick_consumesEventsAfterLongClick_outOfBounds_indirectPointer() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<IndirectPointerEvent>()
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        val indirectEventCapturingNode =
            object : IndirectPointerInputModifierNode, Modifier.Node() {
                override fun onIndirectPointerEvent(
                    event: IndirectPointerEvent,
                    pass: PointerEventPass,
                ) {
                    if (pass == PointerEventPass.Main) {
                        receivedEvents.add(event)
                    }
                }

                override fun onCancelIndirectPointerInput() {}
            }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .size(100.dp)
                            .focusRequester(focusRequester)
                            .elementFor(indirectEventCapturingNode)
                            .combinedClickable(onLongClick = onLongClick) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent =
            rule
                .onNodeWithTag("myClickable")
                .sendIndirectPointerPressEvent(rule, currentValue = Offset.Zero)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 16L,
                currentValue = Offset.Zero,
                delayTimeMills = 16L,
                stepSize = Offset(1f, 1f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Press).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(0)
        }

        val moveAmount = viewConfiguration.touchSlop * 2

        // Move past touch slop - normally this would cancel input, but since we
        // already triggered a long click, we still want to consume events until all pointers
        // are up (even out of bounds)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 32L,
                currentValue = Offset(moveAmount, moveAmount),
                delayTimeMills = 16L,
                stepSize = Offset(moveAmount, moveAmount),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type == IndirectPointerEventType.Release).isTrue()
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    /**
     * Test case to make sure that after a long press is triggered, we consume _all_ indirect
     * pointer events, even if a child consumed an event after the long press was triggered
     */
    @Test
    @LargeTest
    fun longClick_consumesEventsAfterLongClick_childConsumesFirst_indirectPointer() {
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<IndirectPointerEvent>()
        var consumeEventsInChild = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        val indirectEventCapturingNode =
            object : IndirectPointerInputModifierNode, Modifier.Node() {
                override fun onIndirectPointerEvent(
                    event: IndirectPointerEvent,
                    pass: PointerEventPass,
                ) {
                    if (pass == PointerEventPass.Main) {
                        receivedEvents.add(event)
                    }
                }

                override fun onCancelIndirectPointerInput() {}
            }

        val childConsumingNode =
            object : IndirectPointerInputModifierNode, Modifier.Node() {
                override fun onIndirectPointerEvent(
                    event: IndirectPointerEvent,
                    pass: PointerEventPass,
                ) {
                    if (consumeEventsInChild && pass == PointerEventPass.Main) {
                        event.changes.forEach { it.consume() }
                    }
                }

                override fun onCancelIndirectPointerInput() {}
            }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .elementFor(indirectEventCapturingNode)
                            .combinedClickable(onLongClick = onLongClick) {}
                            .elementFor(childConsumingNode)
                            .focusRequester(focusRequester)
                            .focusTarget(),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent =
            rule
                .onNodeWithTag("myClickable")
                .sendIndirectPointerPressEvent(rule, currentValue = Offset.Zero)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 16L,
                currentValue = Offset.Zero,
                delayTimeMills = 16L,
                stepSize = Offset(1f, 1f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Press).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(0)
        }

        rule.runOnIdle { consumeEventsInChild = true }

        // Move - this move will be consumed by the child
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 32L,
                currentValue = Offset(1f, 1f),
                delayTimeMills = 16L,
                stepSize = Offset(1f, 1f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(1)
            // The move will be consumed by the child
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            receivedEvents.clear()
            // Stop consuming events in the child
            consumeEventsInChild = false
        }

        // Move again
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 1,
                currentTime = 48L,
                currentValue = Offset(2f, 2f),
                delayTimeMills = 16L,
                stepSize = Offset(1f, 1f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                previousEvent = downEvent,
            )

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up, even though the child consumed
            // an event before this
            assertThat(receivedEvents[0].type == IndirectPointerEventType.Move).isTrue()
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type == IndirectPointerEventType.Release).isTrue()
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    @Test
    fun longClick_childConsumesUp_handlesNextClick_indirectPointer() {
        var clickCounter = 0
        var longClickCounter = 0
        var consumeEventsInChild = false
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        val childConsumingNode =
            object : IndirectPointerInputModifierNode, Modifier.Node() {
                override fun onIndirectPointerEvent(
                    event: IndirectPointerEvent,
                    pass: PointerEventPass,
                ) {
                    if (consumeEventsInChild && pass == PointerEventPass.Main) {
                        event.changes.forEach { it.consume() }
                    }
                }

                override fun onCancelIndirectPointerInput() {}
            }

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box(
                modifier =
                    Modifier.testTag("myClickable")
                        .size(100.dp)
                        .combinedClickable(
                            onClick = { ++clickCounter },
                            onLongClick = { ++longClickCounter },
                        )
                        // The modifier order here places this node deeper in the node tree.
                        // During the Main pass, it will intercept and consume events before
                        // combinedClickable.
                        .elementFor(childConsumingNode)
                        .focusRequester(focusRequester)
                        .focusTarget()
            )
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        // Start a long press gesture
        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        val longPressTimeout = viewConfiguration.longPressTimeoutMillis + 100
        rule.mainClock.advanceTimeBy(longPressTimeout)

        rule.runOnIdle {
            // Ensure the long click triggered properly
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(0)

            // Instruct the child to start swallowing events before the UP event fires
            consumeEventsInChild = true
        }

        // Release the pointer
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle {
            // Stop consuming events so we can attempt a normal click
            consumeEventsInChild = false
        }

        // Attempt a normal click
        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    /**
     * Integration test to make sure that a scrollable parent cannot scroll after a long click when
     * using indirect pointer events.
     */
    @Test
    fun longClick_consumesEventsAfterLongClick_scrollableContainer_indirectPointer() {
        var longClickCounter = 0
        val onLongClick: () -> Unit = { ++longClickCounter }
        val scrollState = ScrollState(0)
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box(Modifier.size(100.dp).verticalScroll(scrollState)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(onLongClick = onLongClick) {},
                )
                Box(Modifier.height(1000.dp))
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent =
            rule
                .onNodeWithTag("myClickable")
                .sendIndirectPointerPressEvent(rule, currentValue = Offset.Zero)

        // Advance past the long press timeout
        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle { assertThat(longClickCounter).isEqualTo(1) }

        // Move by a large amount
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                stepCount = 5,
                currentTime = 16L,
                currentValue = Offset.Zero,
                delayTimeMills = 16L,
                stepSize = Offset(0f, 100f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.Y,
                previousEvent = downEvent,
            )

        rule.runOnIdle {
            // Long click should consume all the events, so no scrolling should happen
            assertThat(scrollState.value).isEqualTo(0)
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
    fun click_withLongClick_indirectPointer() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
        }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun click_afterLongClick() {
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

        rule.onNodeWithTag("myClickable").performTouchInput { longClick() }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").performClick()

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun click_afterLongClick_indirectPointer() {
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)
        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(1)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

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
    fun click_withDoubleClick_indirectPointer() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }

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
                            .combinedClickable(onDoubleClick = onDoubleClick, onClick = onClick),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)
        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

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
    @LargeTest
    fun click_withDoubleClick_andLongClick_indirectPointer() {
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                onDoubleClick = onDoubleClick,
                                onLongClick = onLongClick,
                                onClick = onClick,
                            ),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        rule.mainClock.advanceTimeUntil { clickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)
        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        rule.mainClock.advanceTimeUntil { doubleClickCounter == 1 }
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(1)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(1)
        }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

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
    fun doubleClick_withinTimeout_aboveMinimumDuration_indirectPointer() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule, time = 0)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPressReleaseEvent(
                rule,
                time = viewConfiguration.doubleTapMinTimeMillis + 10,
            )

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
    fun doubleClick_withinTimeout_belowMinimumDuration_indirectPointer() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val doubleTapTimeoutDelay = viewConfiguration.doubleTapTimeoutMillis + 100

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule, time = 0)
        // Send a second press below the minimum time required for a double tap
        val minimumDuration = viewConfiguration.doubleTapMinTimeMillis
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPressReleaseEvent(rule, time = minimumDuration / 2)

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
    fun doubleClick_outsideTimeout_indirectPointer() {
        var clickCounter = 0
        var doubleClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                        ),
            )
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val delay = viewConfiguration.doubleTapTimeoutMillis + 100

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

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
        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

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
    fun doubleClick_secondClickIsALongClick_indirectPointer() {
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration
        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                            onLongClick = { ++longClickCounter },
                        ),
            )
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule, 0L)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerPressEvent(rule, viewConfiguration.doubleTapMinTimeMillis + 10)

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
    fun interactionSource_noScrollableContainer_indirectPointer() {
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

        val downEvent =
            rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, currentTime = 0L)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, currentTime = 16L, previousEvent = downEvent)

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
    fun interactionSource_immediateRelease_noScrollableContainer_indirectPointer() {
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

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, 0L)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, 16L, previousEvent = downEvent)

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
    fun interactionSource_immediateCancel_noScrollableContainer_indirectPointer() {
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

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerCancelEvent(rule, sendMoveEvents = false)

        // We are not in a scrollable container, so we should see a press and immediate cancel
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun interactionSource_immediateDrag_insideDraggable() {
        Assume.assumeTrue(isDelayPressesUsingGestureConsumptionEnabled)
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

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        // We started a drag before the timeout, so no press should be emitted
        rule.runOnIdle { assertThat(interactions).isEmpty() }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun interactionSource_immediateDrag_noScrollableContainer_doNotUseGestureNode() {
        Assume.assumeFalse(isDelayPressesUsingGestureConsumptionEnabled)
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
    fun interactionSource_scrollableContainer_indirectPointer() {
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

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, 0L)

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
            .sendIndirectPointerReleaseEvent(
                rule,
                halfTapIndicationDelay + 16L,
                previousEvent = downEvent,
            )

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
    fun interactionSource_immediateRelease_scrollableContainer_indirectPointer() {
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

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, 0L)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, 16L, previousEvent = downEvent)

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
    fun interactionSource_immediateCancel_scrollableContainer_indirectPointer() {
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

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerCancelEvent(rule, sendMoveEvents = false)

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
    fun interactionSource_immediateDrag_scrollableContainer_indirectPointer() {
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
        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, 0L, pressPosition)
        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
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
    fun interactionSource_dragAfterTimeout_scrollableContainer_indirectPointer() {
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
        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, 0L, pressPosition)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule,
                3,
                16L,
                pressPosition,
                16L,
                Offset(50f, 0f),
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
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
    fun interactionSource_cancelledGesture_scrollableContainer_indirectPointer() {
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
        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule, 0L, pressPosition)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerCancelEvent(rule, sendMoveEvents = false)

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
    fun interactionSource_resetWhenDisposed_indirectPointer() {
        val interactionSource = MutableInteractionSource()
        var emitClickableText by mutableStateOf(true)

        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box {
                if (emitClickableText) {
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
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        rule.mainClock.advanceTimeBy(TapIndicationDelay)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // Dispose clickable
        rule.runOnIdle { emitClickableText = false }

        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
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
            advanceEventTime()
            click()
            advanceEventTime()
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

    @Test
    @LargeTest
    fun longClick_interactionSource_continuesTrackingPressAfterLambdasChange_indirectPointer() {
        val interactionSource = MutableInteractionSource()

        var onLongClick by mutableStateOf({})
        val finalLongClick = {}
        val initialLongClick = { onLongClick = finalLongClick }
        // Simulate the long click causing a recomposition, and changing the lambda instance
        onLongClick = initialLongClick

        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager

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
                                onLongClick = onLongClick,
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

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

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

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

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
    fun longClick_interactionSource_cancelsIfLongClickBecomesNull_indirectPointer() {
        val interactionSource = MutableInteractionSource()

        var onLongClick: (() -> Unit)? by mutableStateOf(null)
        val initialLongClick = { onLongClick = null }
        // Simulate the long click causing a recomposition, and changing the lambda to be null
        onLongClick = initialLongClick

        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager

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
                                onLongClick = onLongClick,
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

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
            assertThat(onLongClick).isEqualTo(initialLongClick)
        }

        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

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
    fun longClick_interactionSource_cancelsIfBecomesDisabled() {
        val interactionSource = MutableInteractionSource()

        var counter = 0
        var enabled by mutableStateOf(true)

        lateinit var scope: CoroutineScope

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable").combinedClickable(
                            enabled = enabled,
                            onLongClick = { counter++ },
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
            assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        // Initial press
        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(counter).isEqualTo(0)
        }

        // Long click
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle { enabled = false }
        rule.mainClock.advanceTimeByFrame()

        // We should now be disabled, and so we should cancel the existing press.
        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
            assertThat(counter).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun longClick_interactionSource_cancelsIfBecomesDisabled_indirectPointer() {
        val interactionSource = MutableInteractionSource()

        var counter = 0
        var enabled by mutableStateOf(true)

        lateinit var scope: CoroutineScope
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager

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
                                enabled = enabled,
                                onLongClick = { counter++ },
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

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
            assertThat(counter).isEqualTo(0)
        }

        rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        // Initial press
        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(counter).isEqualTo(0)
        }

        // Long click
        rule.mainClock.advanceTimeBy(1000)

        rule.runOnIdle { enabled = false }
        rule.mainClock.advanceTimeByFrame()

        // We should now be disabled, and so we should cancel the existing press.
        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
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
    fun click_withDoubleClick_andLongClick_disabledMidGesture() {
        val enabled = mutableStateOf(true)
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

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.runOnIdle { enabled.value = false }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        // No gestures should be triggered since we became disabled mid-gesture
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }
    }

    @Test
    @LargeTest
    fun click_withDoubleClick_andLongClick_disabledMidGesture_indirectPointer() {
        val enabled = mutableStateOf(true)
        var clickCounter = 0
        var doubleClickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }

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
                            .combinedClickable(
                                enabled = enabled.value,
                                onDoubleClick = onDoubleClick,
                                onLongClick = onLongClick,
                                onClick = onClick,
                            ),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val downEvent = rule.onNodeWithTag("myClickable").sendIndirectPointerPressEvent(rule)

        rule.runOnIdle { enabled.value = false }

        // Process gestures
        rule.mainClock.advanceTimeBy(1000)

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(rule, previousEvent = downEvent)

        // No gestures should be triggered since we became disabled mid-gesture
        rule.runOnIdle {
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
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

        val downEvent = obtainMotionEvent(eventTime = 0, action = ACTION_DOWN, x = 5f, y = 5f)

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(0)
            assertThat(doubleClicks).isEqualTo(0)
        }

        val deepPressMoveEvent =
            obtainMotionEvent(
                eventTime = 50,
                action = ACTION_MOVE,
                x = 10f,
                y = 10f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        val upEvent = obtainMotionEvent(eventTime = 100, action = ACTION_UP, x = 10f, y = 10f)

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(upEvent)
        rule.mainClock.advanceTimeBy(50)

        // Even though the timeout didn't pass, the deep press should immediately trigger the long
        // click. No other callbacks should be triggered.
        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(1)
            assertThat(doubleClicks).isEqualTo(0)
        }
    }

    /** Regression test for b/483931967 */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun longClick_deepPress_triggersOnce_multipleDeepPressEvents() {
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

        val downEvent = obtainMotionEvent(eventTime = 0, action = ACTION_DOWN, x = 5f, y = 5f)

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(0)
            assertThat(doubleClicks).isEqualTo(0)
        }

        val deepPressMoveEvent1 =
            obtainMotionEvent(
                eventTime = 50,
                action = ACTION_MOVE,
                x = 10f,
                y = 10f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        val deepPressMoveEvent2 =
            obtainMotionEvent(
                eventTime = 100,
                action = ACTION_MOVE,
                x = 15f,
                y = 15f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        val upEvent = obtainMotionEvent(eventTime = 150, action = ACTION_UP, x = 15f, y = 15f)

        view.dispatchTouchEvent(deepPressMoveEvent1)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(deepPressMoveEvent2)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(upEvent)
        rule.mainClock.advanceTimeBy(50)

        // Multiple deep presses should still only trigger one long click
        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(1)
            assertThat(doubleClicks).isEqualTo(0)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun longClick_consumesEventsAfterLongClick_deepPress() {
        lateinit var view: View
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<PointerEvent>()

        rule.setContent {
            view = LocalView.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        receivedEvents += event
                                    }
                                }
                            }
                            .combinedClickable(onLongClick = onClick) {},
                )
            }
        }

        val downEvent = obtainMotionEvent(eventTime = 0, action = ACTION_DOWN, x = 5f, y = 5f)

        val moveEvent = obtainMotionEvent(eventTime = 50, action = ACTION_MOVE, x = 10f, y = 10f)

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(moveEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Press)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        val deepPressMoveEvent =
            obtainMotionEvent(
                eventTime = 100,
                action = ACTION_MOVE,
                x = 15f,
                y = 15f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        val postDeepPressMoveEvent =
            obtainMotionEvent(eventTime = 150, action = ACTION_MOVE, x = 20f, y = 20f)

        val upEvent = obtainMotionEvent(eventTime = 200, action = ACTION_UP, x = 20f, y = 20f)

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(postDeepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(upEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(3)
            // Both the deep press event and subsequent move and up should be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[2].type).isEqualTo(PointerEventType.Release)
            assertThat(receivedEvents[2].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun longClick_deepPress_consumesEventsAfterLongClick_outOfBounds() {
        lateinit var view: View
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<PointerEvent>()
        val clickableSize = 100

        rule.setContent {
            view = LocalView.current
            Box(
                modifier =
                    Modifier.testTag("myClickable")
                        .size(with(LocalDensity.current) { clickableSize.toDp() })
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    receivedEvents += event
                                }
                            }
                        }
                        .combinedClickable(onLongClick = onLongClick) {}
            )
        }

        val downEvent = obtainMotionEvent(eventTime = 0, action = ACTION_DOWN, x = 5f, y = 5f)

        val moveEvent = obtainMotionEvent(eventTime = 50, action = ACTION_MOVE, x = 10f, y = 10f)

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(moveEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Press)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        val deepPressMoveEvent =
            obtainMotionEvent(
                eventTime = 100,
                action = ACTION_MOVE,
                x = 15f,
                y = 15f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(1)
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            receivedEvents.clear()
        }

        val outOfBoundsMoveEvent =
            obtainMotionEvent(
                eventTime = 150,
                action = ACTION_MOVE,
                x = clickableSize * 2f,
                y = clickableSize * 2f,
            )

        val upEvent =
            obtainMotionEvent(
                eventTime = 200,
                action = ACTION_UP,
                x = clickableSize * 2f,
                y = clickableSize * 2f,
            )

        // Move outside the touch bounds - normally this would cancel input, but since we
        // already triggered a long click, we still want to consume events until all pointers
        // are up (even out of bounds)
        view.dispatchTouchEvent(outOfBoundsMoveEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(upEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click should consume the subsequent move and up
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Release)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
        }
    }

    /**
     * Test case to make sure that after a deep press is triggered, we consume _all_ pointer events,
     * even if a child consumed an event after the long press was triggered
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun longClick_deepPress_consumesEventsAfterLongClick_childConsumesFirst() {
        lateinit var view: View
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val receivedEvents = mutableListOf<PointerEvent>()
        var consumeEventsInChild = false

        rule.setContent {
            view = LocalView.current
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        receivedEvents += awaitPointerEvent()
                                    }
                                }
                            }
                            .combinedClickable(onLongClick = onLongClick) {}
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (consumeEventsInChild) {
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            },
                )
            }
        }

        val downEvent = obtainMotionEvent(eventTime = 0, action = ACTION_DOWN, x = 5f, y = 5f)

        val moveEvent = obtainMotionEvent(eventTime = 50, action = ACTION_MOVE, x = 10f, y = 10f)

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(moveEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            assertThat(receivedEvents.size).isEqualTo(2)
            // Long click has not triggered yet, so the first move should not be consumed
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Press)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isFalse()
            receivedEvents.clear()
        }

        val deepPressMoveEvent =
            obtainMotionEvent(
                eventTime = 100,
                action = ACTION_MOVE,
                x = 15f,
                y = 15f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            // Long click will now have triggered
            assertThat(counter).isEqualTo(1)
            assertThat(receivedEvents.size).isEqualTo(1)
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            receivedEvents.clear()
            // Start consuming events in the child
            consumeEventsInChild = true
        }

        val postDeepPressMoveEvent1 =
            obtainMotionEvent(eventTime = 150, action = ACTION_MOVE, x = 20f, y = 20f)

        // Move - this move will be consumed by the child
        view.dispatchTouchEvent(postDeepPressMoveEvent1)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(1)
            // The move will be consumed by the child
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            receivedEvents.clear()
            // Stop consuming events in the child
            consumeEventsInChild = false
        }

        val postDeepPressMoveEvent2 =
            obtainMotionEvent(eventTime = 200, action = ACTION_MOVE, x = 25f, y = 25f)

        val upEvent = obtainMotionEvent(eventTime = 250, action = ACTION_UP, x = 25f, y = 25f)

        view.dispatchTouchEvent(postDeepPressMoveEvent2)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(upEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(receivedEvents.size).isEqualTo(2)
            // Deep press long click should consume the subsequent move and up, even though the
            // child consumed an event before this
            assertThat(receivedEvents[0].type).isEqualTo(PointerEventType.Move)
            assertThat(receivedEvents[0].changes.fastAll { it.isConsumed }).isTrue()
            assertThat(receivedEvents[1].type).isEqualTo(PointerEventType.Release)
            assertThat(receivedEvents[1].changes.fastAll { it.isConsumed }).isTrue()
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

        val downEvent = obtainMotionEvent(eventTime = 50, action = ACTION_DOWN, x = 5f, y = 5f)

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(0)
            assertThat(doubleClicks).isEqualTo(0)
        }

        val deepPressMoveEvent =
            obtainMotionEvent(
                eventTime = 100,
                action = ACTION_MOVE,
                x = 10f,
                y = 10f,
                classification = CLASSIFICATION_DEEP_PRESS,
            )

        val upEvent = obtainMotionEvent(eventTime = 150, action = ACTION_UP, x = 10f, y = 10f)

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)
        view.dispatchTouchEvent(upEvent)
        rule.mainClock.advanceTimeBy(50)

        // Even though the timeout didn't pass, the deep press should immediately trigger the long
        // click. No other callbacks should be triggered.
        rule.runOnIdle {
            assertThat(clicks).isEqualTo(0)
            assertThat(longClicks).isEqualTo(1)
            assertThat(doubleClicks).isEqualTo(0)
        }
    }

    @Test
    fun indirectPointerDrag_cancelsPressInteraction() {
        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

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
                                indication = null,
                                interactionSource = interactionSource,
                            ) {},
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerPressEvent(
                rule = rule,
                currentTime = 0L,
                currentValue = Offset.Zero,
            )

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule = rule,
                stepCount = 3,
                currentTime = 16L,
                currentValue = Offset.Zero,
                delayTimeMills = 16L,
                stepSize = Offset(50f, 0f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
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
    fun immediateIndirectDrag_doesNotTriggerCallbacks() {
        var clickCounter = 0

        val interactionSource = MutableInteractionSource()
        lateinit var inputModeManager: InputModeManager
        val focusRequester = FocusRequester()

        lateinit var scope: CoroutineScope

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            scope = rememberCoroutineScope()
            Box {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(onClick = { clickCounter++ }),
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerPressEvent(
                rule = rule,
                currentTime = 0L,
                currentValue = Offset.Zero,
            )

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerMoveEvents(
                rule = rule,
                stepCount = 3,
                currentTime = 16L,
                currentValue = Offset.Zero,
                delayTimeMills = 16L,
                stepSize = Offset(50f, 0f),
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            )

        rule
            .onNodeWithTag("myClickable")
            .sendIndirectPointerReleaseEvent(
                rule,
                currentTime = 48L,
                currentValue = Offset(150f, 0f),
                primaryAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            )

        rule.runOnIdle { assertThat(clickCounter).isEqualTo(0) }
    }

    @Test
    fun childConsumesIndirectPointerEvent_cancelsPress() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    interactionSource = interactionSource,
                )
            ) {
                Box(
                    Modifier.elementFor(
                            object : IndirectPointerInputModifierNode, Modifier.Node() {
                                override fun onIndirectPointerEvent(
                                    event: IndirectPointerEvent,
                                    pass: PointerEventPass,
                                ) {
                                    if (
                                        pass == PointerEventPass.Main &&
                                            event.type == IndirectPointerEventType.Move
                                    ) {
                                        // Consume moves in the main pass
                                        event.changes.forEach { it.consume() }
                                    }
                                }

                                override fun onCancelIndirectPointerInput() {}
                            }
                        )
                        .size(100.dp)
                        .focusRequester(focusRequester)
                        .focusTarget()
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val downEvent =
            rule
                .onRoot()
                .sendIndirectPointerPressEvent(rule, currentTime = 0L, currentValue = Offset.Zero)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // The move should be consumed by the child, which should cancel the click in the main pass
        val (_, _, lastMove) =
            rule
                .onRoot()
                .sendIndirectPointerMoveEvents(
                    rule,
                    stepCount = 1,
                    currentTime = 16L,
                    currentValue = Offset.Zero,
                    delayTimeMills = 16L,
                    stepSize = Offset(1f, 1f),
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                    previousEvent = downEvent,
                )

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        // The up will not be consumed
        rule
            .onRoot()
            .sendIndirectPointerReleaseEvent(
                rule,
                currentTime = 32L,
                currentValue = Offset.Zero,
                previousEvent = lastMove,
            )

        // The child consumed the move, so the click should be canceled and not triggered by the up
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun parentConsumesIndirectPointerEvent_cancelsPress() {
        val interactionSource = MutableInteractionSource()
        lateinit var scope: CoroutineScope
        var clickCounter = 0
        var longClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onLongClick: () -> Unit = { ++longClickCounter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier.elementFor(
                    object : IndirectPointerInputModifierNode, Modifier.Node() {
                        override fun onIndirectPointerEvent(
                            event: IndirectPointerEvent,
                            pass: PointerEventPass,
                        ) {
                            if (
                                pass == PointerEventPass.Main &&
                                    event.type == IndirectPointerEventType.Move
                            ) {
                                // Consume moves in the main pass
                                event.changes.forEach { it.consume() }
                            }
                        }

                        override fun onCancelIndirectPointerInput() {}
                    }
                )
            ) {
                Box(
                    Modifier.size(100.dp)
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                            interactionSource = interactionSource,
                        )
                )
            }
        }

        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val downEvent =
            rule
                .onRoot()
                .sendIndirectPointerPressEvent(rule, currentTime = 0L, currentValue = Offset.Zero)

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        // The move should be consumed by the parent (in the main pass), which should cancel the
        // click in the final pass (since the move will be consumed after the clickable sees it in
        // the main pass)
        val (_, _, lastMove) =
            rule
                .onRoot()
                .sendIndirectPointerMoveEvents(
                    rule,
                    stepCount = 1,
                    currentTime = 16L,
                    currentValue = Offset.Zero,
                    delayTimeMills = 16L,
                    stepSize = Offset(1f, 1f),
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                    previousEvent = downEvent,
                )

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }

        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        // The up will not be consumed
        rule
            .onRoot()
            .sendIndirectPointerReleaseEvent(
                rule,
                currentTime = 32L,
                currentValue = Offset.Zero,
                previousEvent = lastMove,
            )

        // The parent consumed the move, so the click should be canceled and not triggered by the up
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(0)
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            assertThat((interactions[1] as PressInteraction.Cancel).press)
                .isEqualTo(interactions[0])
        }
    }

    /**
     * Test to ensure that indirect pointer cancellation (triggered when we lose focus, such as when
     * a clickable loses focus when moving to touch mode) doesn't also cancel ongoing clicks from
     * pointer input.
     */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun switchingToTouchModeFromNonTouchMode_doesNotCancelOngoingClick() {
        val interactionSource = MutableInteractionSource()
        var counter = 0
        val onClick: () -> Unit = { ++counter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var focusManager: FocusManager

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier.size(100.dp)
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(onClick = onClick, interactionSource = interactionSource)
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Start in keyboard mode and request focus
        rule.runOnIdle { assertThat(inputModeManager.requestInputMode(Keyboard)).isTrue() }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").assertIsFocused()

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
        }

        // b/438742567 - currently touch mode isn't reset when injecting touch input. Resetting
        // touch mode through InstrumentationRegistry doesn't seem to work here mid-activity,
        // so instead we manually clear focus to simulate this. (this will move focus to the root
        // box). In the future this test should actually move to touch mode.
        rule.runOnIdle { focusManager.clearFocus() }

        // The clickable should no longer be focused
        rule.onNodeWithTag("myClickable").assertIsNotFocused()

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[2] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }

        // No click should be invoked yet
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        // Perform an up event - this should trigger the click
        rule.onNodeWithTag("myClickable").performTouchInput { up() }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[2] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
            assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[3] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun switchingToTouchModeFromNonTouchMode_doesNotCancelOngoingClick_delayedTap() {
        val interactionSource = MutableInteractionSource()
        var clickCounter = 0
        var doubleClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var focusManager: FocusManager
        lateinit var viewConfiguration: ViewConfiguration

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier.size(100.dp)
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onClick = onClick,
                            onDoubleClick = onDoubleClick,
                            interactionSource = interactionSource,
                        )
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Start in keyboard mode and request focus
        rule.runOnIdle { assertThat(inputModeManager.requestInputMode(Keyboard)).isTrue() }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").assertIsFocused()

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
        }

        // We've tapped once, but since we have a double click listener, we shouldn't have clicked
        // yet
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
        }

        // b/438742567 - currently touch mode isn't reset when injecting touch input. Resetting
        // touch mode through InstrumentationRegistry doesn't seem to work here mid-activity,
        // so instead we manually clear focus to simulate this. (this will move focus to the root
        // box). In the future this test should actually move to touch mode.
        rule.runOnIdle { focusManager.clearFocus() }

        // The clickable should no longer be focused
        rule.onNodeWithTag("myClickable").assertIsNotFocused()

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
            assertThat(interactions[3]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[3] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }

        // Wait for the timeout
        rule.mainClock.advanceTimeBy(viewConfiguration.doubleTapTimeoutMillis + 100)

        // The click should be invoked
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun switchingToTouchModeFromNonTouchMode_doesNotCancelOngoingLongClick() {
        val interactionSource = MutableInteractionSource()
        var counter = 0
        val onLongClick: () -> Unit = { ++counter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var focusManager: FocusManager
        lateinit var viewConfiguration: ViewConfiguration

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier.size(100.dp)
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onLongClick = onLongClick,
                            interactionSource = interactionSource,
                        ) {}
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Start in keyboard mode and request focus
        rule.runOnIdle { assertThat(inputModeManager.requestInputMode(Keyboard)).isTrue() }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").assertIsFocused()

        rule.onNodeWithTag("myClickable").performTouchInput { down(center) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
        }

        // b/438742567 - currently touch mode isn't reset when injecting touch input. Resetting
        // touch mode through InstrumentationRegistry doesn't seem to work here mid-activity,
        // so instead we manually clear focus to simulate this. (this will move focus to the root
        // box). In the future this test should actually move to touch mode.
        rule.runOnIdle { focusManager.clearFocus() }

        // The clickable should no longer be focused
        rule.onNodeWithTag("myClickable").assertIsNotFocused()

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[2] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }

        // No long click should be invoked yet
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        // Wait for the long click
        rule.mainClock.advanceTimeBy(viewConfiguration.longPressTimeoutMillis + 100)

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun switchingToTouchModeFromNonTouchMode_doesNotCancelOngoingDoubleTap() {
        val interactionSource = MutableInteractionSource()
        var clickCounter = 0
        var doubleClickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = { ++doubleClickCounter }
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var focusManager: FocusManager
        lateinit var viewConfiguration: ViewConfiguration

        lateinit var scope: CoroutineScope

        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            focusManager = LocalFocusManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box(Modifier.focusTarget()) {
                Box(
                    Modifier.size(100.dp)
                        .testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable(
                            onClick = onClick,
                            onDoubleClick = onDoubleClick,
                            interactionSource = interactionSource,
                        )
                )
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        // Start in keyboard mode and request focus
        rule.runOnIdle { assertThat(inputModeManager.requestInputMode(Keyboard)).isTrue() }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag("myClickable").assertIsFocused()

        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
        }

        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
        }

        // b/438742567 - currently touch mode isn't reset when injecting touch input. Resetting
        // touch mode through InstrumentationRegistry doesn't seem to work here mid-activity,
        // so instead we manually clear focus to simulate this. (this will move focus to the root
        // box). In the future this test should actually move to touch mode.
        rule.runOnIdle { focusManager.clearFocus() }

        // The clickable should no longer be focused
        rule.onNodeWithTag("myClickable").assertIsNotFocused()

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
            assertThat(interactions[3]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[3] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
        }

        // Send a second tap after the min timeout
        rule.onNodeWithTag("myClickable").performTouchInput {
            val minimumDuration = viewConfiguration.doubleTapMinTimeMillis
            advanceEventTime(minimumDuration + 100)
            down(center)
            up()
        }

        // The double click should be invoked
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(1)
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(6)
            assertThat(interactions.first()).isInstanceOf(FocusInteraction.Focus::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[2] as PressInteraction.Release).press)
                .isEqualTo(interactions[1])
            assertThat(interactions[3]).isInstanceOf(FocusInteraction.Unfocus::class.java)
            assertThat((interactions[3] as FocusInteraction.Unfocus).focus)
                .isEqualTo(interactions[0])
            assertThat(interactions[4]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[5]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat((interactions[5] as PressInteraction.Release).press)
                .isEqualTo(interactions[4])
        }
    }

    @Test
    fun doubleClick_pointerInputCanceled_cancelsPendingTap() {
        var addModifier by mutableStateOf(true)
        var clickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = {}
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Box(
                Modifier.size(100.dp)
                    .testTag("myClickable")
                    .then(
                        if (addModifier) {
                            Modifier.combinedClickable(
                                onClick = onClick,
                                onDoubleClick = onDoubleClick,
                            )
                        } else Modifier
                    )
            )
        }

        // Click
        rule.onNodeWithTag("myClickable").performTouchInput {
            down(center)
            up()
        }

        // Cancel pointer input by removing the modifier
        rule.runOnIdle { addModifier = false }

        // Wait for double tap timeout
        rule.mainClock.advanceTimeBy(viewConfiguration.doubleTapTimeoutMillis + 100)

        // Click should not be invoked
        rule.runOnIdle { assertThat(clickCounter).isEqualTo(0) }
    }

    @Test
    fun doubleClick_indirectPointerInputCanceled_cancelsPendingTap() {
        var addModifier by mutableStateOf(true)
        var clickCounter = 0
        val onClick: () -> Unit = { ++clickCounter }
        val onDoubleClick: () -> Unit = {}
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        lateinit var viewConfiguration: ViewConfiguration

        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            viewConfiguration = LocalViewConfiguration.current
            Box(
                Modifier.size(100.dp)
                    .testTag("myClickable")
                    .focusRequester(focusRequester)
                    .then(
                        if (addModifier) {
                            Modifier.combinedClickable(
                                onClick = onClick,
                                onDoubleClick = onDoubleClick,
                            )
                        } else Modifier
                    )
            )
        }

        // Start in Keyboard mode
        rule.runOnIdle { inputModeManager.requestInputMode(Keyboard) }
        rule.runOnIdle { focusRequester.requestFocus() }

        // Tap Indirect
        rule.onNodeWithTag("myClickable").sendIndirectPressReleaseEvent(rule)

        // Cancel indirect pointer input by removing the modifier
        rule.runOnIdle { addModifier = false }

        // Wait for double tap timeout
        rule.mainClock.advanceTimeBy(viewConfiguration.doubleTapTimeoutMillis + 100)

        // Click should not be invoked
        rule.runOnIdle { assertThat(clickCounter).isEqualTo(0) }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun obtainMotionEvent(
    eventTime: Int,
    action: Int,
    x: Float,
    y: Float,
    classification: Int = CLASSIFICATION_NONE,
): MotionEvent {
    val pointerProperties =
        arrayOf(
            MotionEvent.PointerProperties().also {
                it.id = 0
                it.toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        )
    val pointerCoords =
        arrayOf(
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
            }
        )

    return MotionEvent.obtain(
        /* downTime = */ 0,
        /* eventTime = */ eventTime.toLong(),
        /* action = */ action,
        /* pointerCount = */ 1,
        /* pointerProperties = */ pointerProperties,
        /* pointerCoords = */ pointerCoords,
        /* metaState = */ 0,
        /* buttonState = */ 0,
        /* xPrecision = */ 0f,
        /* yPrecision = */ 0f,
        /* deviceId = */ 0,
        /* edgeFlags = */ 0,
        /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
        /* displayId = */ 0,
        /* flags = */ 0,
        /* classification = */ classification,
    )!!
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
