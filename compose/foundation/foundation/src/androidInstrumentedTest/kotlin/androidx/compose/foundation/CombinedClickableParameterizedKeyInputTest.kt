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

@file:OptIn(ExperimentalTestApi::class)

package androidx.compose.foundation

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.InputMode.Companion.Keyboard
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InjectionScope
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Common parameterized tests for key input with the different supported keys
 * ([androidx.compose.foundation.isClick]). Non-parameterized key input tests are in
 * [CombinedClickableTest]
 */
@MediumTest
@RunWith(Parameterized::class)
class CombinedClickableParameterizedKeyInputTest(keyCode: Long) {
    private val key: Key = Key(keyCode)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "keyCode={0}")
        // @Parameterized doesn't handle value classes correctly, which is why we use key codes.
        fun parameters() =
            listOf(
                Key.Enter.keyCode,
                Key.NumPadEnter.keyCode,
                Key.DirectionCenter.keyCode,
                Key.Spacebar.keyCode
            )
    }

    @get:Rule val rule = createComposeRule()

    @Test
    fun clickWithKey() {
        var counter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            BasicText(
                "ClickableText",
                modifier =
                    Modifier.testTag("myClickable")
                        .focusRequester(focusRequester)
                        .combinedClickable { counter++ }
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(key) }

        rule.runOnIdle { assertThat(counter).isEqualTo(0) }

        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(key) }

        rule.runOnIdle { assertThat(counter).isEqualTo(1) }
    }

    @Test
    fun clickWithKey_notInvokedIfFocusIsLostWhilePressed() {
        var counter = 0
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp).focusRequester(outerFocusRequester).focusTarget()) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(clickableFocusRequester)
                            .combinedClickable { counter++ }
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            clickableFocusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            // Remove focus from the clickable
            outerFocusRequester.requestFocus()
        }

        // (clickable won't see this event as it is no longer focused, but emit for clarity)
        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(key) }

        // The clickable should never see the up event, so it should never invoke onClick
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
    }

    @Test
    fun clickWithKey_notInvokedIfCorrespondingDownEventWasNotReceived() {
        var counter = 0
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(
                Modifier.testTag("outerBox")
                    .padding(10.dp)
                    .focusRequester(outerFocusRequester)
                    .focusTarget()
            ) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(clickableFocusRequester)
                            .combinedClickable { counter++ }
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            outerFocusRequester.requestFocus()
        }

        // Press down on the outer box
        rule.onNodeWithTag("outerBox").performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            // Focus the clickable, while still pressing down
            clickableFocusRequester.requestFocus()
        }

        // Release the key
        rule.onNodeWithTag("myClickable").performKeyInput { keyUp(key) }

        // The clickable should not invoke onClick because it only saw the up event, not the
        // corresponding down, and hence should not be considered pressed
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
    }

    @Test
    fun longClickWithKey() {
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
                            onClick = { ++clickCounter }
                        )
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
            pressKey(key, durationMillis)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(0)
        }
    }

    @Test
    @LargeTest
    fun longClickWithKey_doesNotTriggerHapticFeedback() {
        var clickCounter = 0
        var longClickCounter = 0
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        val performedHaptics = mutableListOf<HapticFeedbackType>()

        val hapticFeedback: HapticFeedback =
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                    performedHaptics += hapticFeedbackType
                }
            }
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                onLongClick = { ++longClickCounter },
                                onClick = { ++clickCounter },
                                hapticFeedbackEnabled = true
                            )
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
            // The press duration is 100ms longer than the minimum required for a long press.
            val durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
            pressKey(key, durationMillis)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(0)
            assertThat(performedHaptics).isEmpty()
        }
    }

    @Test
    fun longClickWithKey_notInvokedIfFocusIsLostWhilePressed() {
        var counter = 0
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp).focusRequester(outerFocusRequester).focusTarget()) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(clickableFocusRequester)
                            .combinedClickable(onLongClick = { counter++ }) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            clickableFocusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(counter).isEqualTo(0)
            // Remove focus from the clickable
            outerFocusRequester.requestFocus()
        }

        // Advance a small amount to allow the coroutine to be cancelled
        rule.mainClock.advanceTimeBy(100)

        // Advance past the long press timeout
        rule.mainClock.advanceTimeBy(1000)

        // We should dispose the long click when we lost focus, so onLongClick should not be invoked
        rule.runOnIdle { assertThat(counter).isEqualTo(0) }
    }

    @Test
    fun doubleClickWithKey_withinTimeout_aboveMinimumDuration() {
        var clickCounter = 0
        var doubleClickCounter = 0
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
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter }
                        )
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            keyDown(key)
            keyUp(key)
            advanceEventTime(doubleTapDelay)
            keyDown(key)
            keyUp(key)
        }

        // Double click should not trigger click, and the double click should be immediately invoked
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun doubleClickWithKey_withinTimeout_belowMinimumDuration() {
        var clickCounter = 0
        var doubleClickCounter = 0
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
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter }
                        )
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        var doubleTapTimeoutDelay: Long = 0

        rule.onNodeWithTag("myClickable").performKeyInput {
            doubleTapTimeoutDelay = viewConfiguration.doubleTapTimeoutMillis + 100
            keyDown(key)
            keyUp(key)
            // Send a second press below the minimum time required for a double tap
            val minimumDuration = viewConfiguration.doubleTapMinTimeMillis
            advanceEventTime(minimumDuration / 2)
            keyDown(key)
            keyUp(key)
        }

        // Because the second tap was below the timeout, this should instead be treated as two
        // clicks, but the second click won't be invoked until after the timeout
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(1)
            assertThat(doubleClickCounter).isEqualTo(0)
        }

        // After the timeout has run out, the second click will be invoked, and no double click will
        // be invoked
        rule.mainClock.advanceTimeBy(doubleTapTimeoutDelay)
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(2)
            assertThat(doubleClickCounter).isEqualTo(0)
        }
    }

    @Test
    fun doubleClickWithKey_outsideTimeout() {
        var clickCounter = 0
        var doubleClickCounter = 0
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
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter }
                        )
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        var delay: Long = 0

        rule.onNodeWithTag("myClickable").performKeyInput {
            // Delay slightly past the timeout
            delay = viewConfiguration.doubleTapTimeoutMillis + 100
            keyDown(key)
            keyUp(key)
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
        rule.onNodeWithTag("myClickable").performKeyInput {
            keyDown(key)
            keyUp(key)
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
    fun doubleClickWithKey_secondClickIsALongClick() {
        var clickCounter = 0
        var doubleClickCounter = 0
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
                            onDoubleClick = { ++doubleClickCounter },
                            onClick = { ++clickCounter },
                            onLongClick = { ++longClickCounter }
                        )
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            keyDown(key)
            keyUp(key)
            advanceEventTime(doubleTapDelay)
            keyDown(key)
            // Advance past long click timeout
            advanceEventTime(1000)
        }

        // Long click should cancel double click and click
        rule.runOnIdle {
            assertThat(clickCounter).isEqualTo(0)
            assertThat(doubleClickCounter).isEqualTo(0)
            assertThat(longClickCounter).isEqualTo(1)
        }
    }

    @Test
    fun keyPress_emitsInteraction() {
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
                                indication = null
                            ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }
        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun keyPress_emitsCancelInteractionWhenFocusIsRemovedWhilePressed() {
        val interactionSource = MutableInteractionSource()
        val outerFocusRequester = FocusRequester()
        val clickableFocusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp).focusRequester(outerFocusRequester).focusTarget()) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable")
                            .focusRequester(clickableFocusRequester)
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            clickableFocusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            // Remove focus from the clickable, while it is still 'pressed'
            outerFocusRequester.requestFocus()
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            // We should cancel the existing press, since the clickable is no longer focused
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Cancel::class.java)
            // We should be unfocused
            assertThat(interactions[2]).isInstanceOf(FocusInteraction.Unfocus::class.java)
        }
    }

    @Test
    fun doubleKeyPress_emitsFurtherInteractions() {
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
                                indication = null
                            ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        val clickableNode = rule.onNodeWithTag("clickable")

        clickableNode.performKeyInput { pressKey(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
        }

        clickableNode.performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(3)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
        }

        clickableNode.performKeyInput { keyUp(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(4)
            assertThat(interactions[0]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            assertThat(interactions[2]).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions[3]).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun repeatKeyEvents_doNotEmitFurtherInteractions() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        lateinit var scope: CoroutineScope
        lateinit var inputModeManager: InputModeManager
        var repeatCounter = 0
        rule.setContent {
            scope = rememberCoroutineScope()
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.padding(10.dp)) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("clickable")
                            .focusRequester(focusRequester)
                            .onKeyEvent {
                                if (it.nativeKeyEvent.repeatCount != 0) repeatCounter++
                                false
                            }
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = null,
                            ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.onNodeWithTag("clickable").performKeyInput {
            keyDown(key)

            advanceEventTime(500) // First repeat
            advanceEventTime(50) // Second repeat
        }

        rule.runOnIdle {
            // Ensure that expected number of repeats occurred and did not cause press interactions.
            assertThat(repeatCounter).isEqualTo(2)
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("clickable").performKeyInput { keyUp(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(interactions.last()).isInstanceOf(PressInteraction.Release::class.java)
        }
    }

    @Test
    fun interruptedKeyClick_emitsCancelInteraction() {
        val interactionSource = MutableInteractionSource()
        val focusRequester = FocusRequester()
        val enabled = mutableStateOf(true)
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
                                enabled = enabled.value
                            ) {}
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        val interactions = mutableListOf<Interaction>()
        scope.launch { interactionSource.interactions.collect { interactions.add(it) } }

        val clickableNode = rule.onNodeWithTag("clickable")

        clickableNode.performKeyInput { keyDown(key) }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        enabled.value = false

        clickableNode.assertIsNotEnabled()

        rule.runOnIdle {
            // Filter out focus interactions.
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }

        // Key releases should not result in interactions.
        clickableNode.performKeyInput { keyUp(key) }

        // Make sure nothing has changed.
        rule.runOnIdle {
            val pressInteractions = interactions.filterIsInstance<PressInteraction>()
            assertThat(pressInteractions).hasSize(2)
            assertThat(pressInteractions.first()).isInstanceOf(PressInteraction.Press::class.java)
            assertThat(pressInteractions.last()).isInstanceOf(PressInteraction.Cancel::class.java)
        }
    }

    @Test
    fun updateOnLongClickListenerBetweenKeyDownAndUp_callsNewListener() {
        var clickCounter = 0
        var longClickCounter = 0
        var newLongClickCounter = 0
        var mutableOnLongClick: () -> Unit by mutableStateOf({ ++longClickCounter })
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
                            onLongClick = mutableOnLongClick,
                            onClick = { ++clickCounter }
                        )
            )
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
            keyDown(key)
            advanceEventTime(100)
        }
        mutableOnLongClick = { ++newLongClickCounter }
        rule.waitForIdle()
        rule.onNodeWithTag("myClickable").performKeyInput {
            // The press duration is 100ms longer than the minimum required for a long press.
            val durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100
            advanceEventTime(durationMillis)
            keyUp(key)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(newLongClickCounter).isEqualTo(1)
            assertThat(clickCounter).isEqualTo(0)
        }
    }

    @Test
    fun modifierReusedBetweenKeyDownAndKeyUp_doesNotCallListeners() {
        var clickCounter = 0
        var longClickCounter = 0
        var reuseKey by mutableStateOf(0)
        val focusRequester = FocusRequester()
        lateinit var inputModeManager: InputModeManager
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            ReusableContent(reuseKey) {
                BasicText(
                    "ClickableText",
                    modifier =
                        Modifier.testTag("myClickable")
                            .focusRequester(focusRequester)
                            .combinedClickable(
                                onLongClick = { ++longClickCounter },
                                onClick = { ++clickCounter }
                            )
                )
            }
        }
        rule.runOnIdle {
            inputModeManager.requestInputMode(Keyboard)
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("myClickable").performKeyInput {
            assertThat(inputModeManager.inputMode).isEqualTo(Keyboard)
            keyDown(key)
            // Press the key down for 100ms less than the required long press duration.
            val durationMillis: Long = viewConfiguration.longPressTimeoutMillis - 100
            advanceEventTime(durationMillis)
        }
        rule.runOnIdle { reuseKey = 1 }
        rule.waitForIdle()
        rule.onNodeWithTag("myClickable").performKeyInput {
            // Press the key down for another 200ms to reach the required long press duration.
            advanceEventTime(200)
            keyUp(key)
        }

        rule.runOnIdle {
            assertThat(longClickCounter).isEqualTo(0)
            assertThat(clickCounter).isEqualTo(0)
        }
    }
}

/** Average of the min time and timeout for the delay between clicks for a double click */
internal val InjectionScope.doubleTapDelay
    get() = with(viewConfiguration) { (doubleTapMinTimeMillis + doubleTapTimeoutMillis) / 2 }
