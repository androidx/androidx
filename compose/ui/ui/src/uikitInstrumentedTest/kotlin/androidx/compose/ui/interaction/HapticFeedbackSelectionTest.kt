/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.interaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.hold
import androidx.compose.ui.test.utils.up
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HapticFeedbackSelectionTest {

    private class TestHapticFeedback : HapticFeedback {
        val performedHaptics = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            if (hapticFeedbackType == HapticFeedbackType.TextHandleMove) {
                // Ignored for iOS
                return
            }

            performedHaptics.add(hapticFeedbackType)
        }

        fun assertLongPressHapticPerformed() {
            assertTrue(
                performedHaptics.contains(HapticFeedbackType.LongPress),
                "Expected LongPress haptic feedback, but got: $performedHaptics"
            )
            assertTrue(
                performedHaptics.none { it != HapticFeedbackType.LongPress },
                "Expected LongPress haptic feedback, but got: $performedHaptics"
            )
        }

        fun assertNoHaptic() {
            assertTrue(
                performedHaptics.isEmpty(),
                "Did not expect LongPress haptic feedback, but got: $performedHaptics"
            )
        }
    }

    @Composable
    private fun WithTestHapticFeedback(
        hapticFeedback: TestHapticFeedback,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(LocalHapticFeedback provides hapticFeedback) {
            content()
        }
    }

    @Test
    fun testBasicTextFieldValue_LongPress_TriggersHapticFeedback() = runUIKitInstrumentedTest {
        val hapticFeedback = TestHapticFeedback()
        var textFieldValue by mutableStateOf(TextFieldValue("Hello World"))

        setContent {
            WithTestHapticFeedback(hapticFeedback) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("TextField")
                            .padding(16.dp)
                    )
                }
            }
        }

        // Perform long press
        val textFieldNode = findNodeWithTag("TextField")
        val touch = textFieldNode.touchDown()
        delay(600) // Wait for long press timeout
        touch.hold()
        delay(100)
        touch.up()

        waitForIdle()

        // Verify that LongPress haptic feedback was triggered
        hapticFeedback.assertLongPressHapticPerformed()
    }

    @Test
    fun testBasicTextFieldValue_DoubleTap_DoesNotTriggerHaptic() = runUIKitInstrumentedTest {
        val hapticFeedback = TestHapticFeedback()
        var textFieldValue by mutableStateOf(TextFieldValue("Hello-LongLongLongLongLongLong-text"))
        val focusRequester = FocusRequester()

        setContent {
            WithTestHapticFeedback(hapticFeedback) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("TextField")
                            .padding(16.dp)
                            .focusRequester(focusRequester)
                    )
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        // Perform double tap
        findNodeWithTag("TextField").doubleTap()

        waitForIdle()
        // Verify that haptic feedback was NOT triggered
        hapticFeedback.assertNoHaptic()

        assertFalse(textFieldValue.selection.collapsed)
    }

    @Test
    fun testBasicTextFieldState_LongPress_TriggersHapticFeedback() = runUIKitInstrumentedTest {
        val hapticFeedback = TestHapticFeedback()
        val textFieldState = TextFieldState("Hello World")

        setContent {
            WithTestHapticFeedback(hapticFeedback) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("TextField")
                            .padding(16.dp)
                    )
                }
            }
        }

        // Perform long press
        val textFieldNode = findNodeWithTag("TextField")
        val touch = textFieldNode.touchDown()
        delay(600) // Wait for long press timeout
        touch.hold()
        delay(100)
        touch.up()

        waitForIdle()

        // Verify that LongPress haptic feedback was triggered
        hapticFeedback.assertLongPressHapticPerformed()
    }

    @Test
    fun testBasicTextFieldState_DoubleTap_DoesNotTriggerHaptic() = runUIKitInstrumentedTest {
        val hapticFeedback = TestHapticFeedback()
        val textFieldState = TextFieldState("Hello-LongLongLongLongLongLong-text")
        val focusRequester = FocusRequester()

        setContent {
            WithTestHapticFeedback(hapticFeedback) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        state = textFieldState,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("TextField")
                            .padding(16.dp)
                            .focusRequester(focusRequester)
                    )
                }
            }
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        findNodeWithTag("TextField").doubleTap()

        waitForIdle()
        // Verify that haptic feedback was NOT triggered
        hapticFeedback.assertNoHaptic()

        assertFalse(textFieldState.selection.collapsed)
    }

    @Test
    fun testSelectionContainer_LongPress_TriggersHapticFeedback() = runUIKitInstrumentedTest {
        val hapticFeedback = TestHapticFeedback()

        setContent {
            WithTestHapticFeedback(hapticFeedback) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SelectionContainer(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("SelectionContainer")
                    ) {
                        Text(
                            text = "Hello World",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Perform long press
        val textNode = findNodeWithTag("SelectionContainer")
        val touch = textNode.touchDown()
        delay(600) // Wait for long press timeout
        touch.hold()
        delay(100)
        touch.up()

        waitForIdle()

        // Verify that LongPress haptic feedback was triggered
        hapticFeedback.assertLongPressHapticPerformed()
    }

    @Test
    fun testSelectionContainer_DoubleTap_DoesNotTriggerHaptic() = runUIKitInstrumentedTest {
        val hapticFeedback = TestHapticFeedback()

        setContent {
            WithTestHapticFeedback(hapticFeedback) {
                Box(modifier = Modifier.fillMaxSize()) {
                    SelectionContainer(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("SelectionContainer")
                    ) {
                        Text(
                            text = "Hello World",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        findNodeWithTag("SelectionContainer").doubleTap()

        waitForIdle()

        // Verify that haptic feedback was NOT triggered
        hapticFeedback.assertNoHaptic()
    }
}
