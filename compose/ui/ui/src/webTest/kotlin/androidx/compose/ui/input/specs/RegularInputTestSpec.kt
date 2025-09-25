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

package androidx.compose.ui.input.specs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.events.beforeInput
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.browser.window

internal interface RegularInputTestSpec : TextFieldTestSpec {

    @Test
    fun positionInput() = runApplicationTest {
        val deltaThreshold = 1.01

        val focusRequester = FocusRequester()
        val inputHolder = createTestInputState()

        var leftState by mutableStateOf(0.dp)
        var topState by mutableStateOf(0.dp)

        createComposeWindow {
            Box(modifier = Modifier.padding(horizontal = leftState, vertical = topState)) {
                inputHolder.createBasicTextField(focusRequester)
            }
        }

        focusRequester.requestFocus()
        waitForHtmlInput()

        sendStandardKeyboardSequence("abc")

        inputHolder.awaitAndAssertTextEquals("abc")

        val clientRectInitial = currentHtmlInput().getBoundingClientRect()

        leftState = 50.dp
        focusRequester.requestFocus()
        awaitIdle()

        val clientRectUpdated = currentHtmlInput().getBoundingClientRect()

        // in Windows/Chrome we need to consider delta
        assertTrue((clientRectUpdated.left - clientRectInitial.left - 50.0).absoluteValue < deltaThreshold, "left position updated")

        focusRequester.requestFocus()
        awaitIdle()

        // intentionally huge, will never grow over viewport nevertheless
        topState = 10000000.dp

        focusRequester.requestFocus()
        awaitIdle()

        var clientRectSticky= currentHtmlInput().getBoundingClientRect()
        val expectedTopValue = window.innerHeight - clientRectSticky.height

        // TODO: In Firefox there's a 0.5 delta - may be this can be accounted precisely somehow
        val topDelta = clientRectSticky.top - expectedTopValue
        assertTrue(topDelta.absoluteValue < deltaThreshold, "top position sticky $topDelta")

        // intentionally huge, will never grow over viewport nevertheless
        leftState = 10000000.dp

        focusRequester.requestFocus()
        awaitIdle()

        clientRectSticky= currentHtmlInput().getBoundingClientRect()
        val expectedLeftValue = window.innerWidth - clientRectSticky.width
        val leftDelta = clientRectSticky.left - expectedLeftValue
        assertTrue(leftDelta.absoluteValue < deltaThreshold, "left position sticky $leftDelta")
    }


    @Test
    fun regularInput() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendStandardKeyboardSequence("step1")
        textFieldValue.awaitAndAssertTextEquals("step1")

        sendToHtmlInput(
            keyEvent("Backspace", code = "Backspace"),
            keyEvent("X"),
            beforeInput(inputType = "insertText", data = "X"),
        )

        textFieldValue.awaitAndAssertTextEquals(
            "stepX",
            "Backspace should delete last symbol typed"
        )
    }

    @Test
    fun keyboardEventPassedToTextField() = runApplicationTest {
        val focusRequester1 = FocusRequester()
        val focusRequester2 = FocusRequester()

        val inputHolder1 = createTestInputState()
        val inputHolder2 = createTestInputState()

        createComposeWindow {
            inputHolder1.createBasicTextField(focusRequester1)
            inputHolder2.createBasicTextField(focusRequester2)
        }

        focusRequester1.requestFocus()
        waitForHtmlInput()

        sendStandardKeyboardSequence("step1")
        inputHolder1.awaitAndAssertTextEquals("step1")

        focusRequester2.requestFocus()
        waitForHtmlInput()

        sendStandardKeyboardSequence("step2")
        inputHolder2.awaitAndAssertTextEquals("step2")
    }
}