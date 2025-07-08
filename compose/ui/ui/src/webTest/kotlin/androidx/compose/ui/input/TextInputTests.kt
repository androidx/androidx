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

package androidx.compose.ui.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.TestInputState
import androidx.compose.ui.WebApplicationScope
import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.Event

abstract class TextInputTests : OnCanvasTests {

    internal abstract suspend fun createTestInputState(): TestInputState

    internal fun currentHtmlInput() = getShadowRoot().querySelector("textarea") as HTMLTextAreaElement

    internal suspend fun WebApplicationScope.createApplicationWithHolder(): TestInputState {
        val focusRequester = FocusRequester()
        val textFieldStateHolder = createTestInputState()

        createComposeWindow {
            textFieldStateHolder.createBasicTextField(focusRequester)
        }

        focusRequester.requestFocus()
        waitForHtmlInput()

        return textFieldStateHolder
    }


    private fun sendToHtmlInput(vararg events: Event) {
        dispatchEvents(currentHtmlInput(), *events)
    }

    // delay in web tests called directly will be completely ignored
    private suspend fun waitFor(millis: Long) {
        withContext(Dispatchers.Default) { delay(millis) }
    }

    internal suspend fun WebApplicationScope.waitForHtmlInput(): HTMLTextAreaElement {
        while (true) {
            val element = getShadowRoot().querySelector("textarea")
            if (element is HTMLTextAreaElement) {
                return element
            }
            yield()
        }
        awaitIdle()
    }

    @Test
    fun positionInput() = runApplicationTest {
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

        sendToHtmlInput(keyEvent("a"), keyEvent("b"), keyEvent("c"))

        inputHolder.awaitAndAssertTextEquals("abc")

        val clientRectInitial = currentHtmlInput().getBoundingClientRect()

        leftState = 50.dp
        focusRequester.requestFocus()
        awaitIdle()

        val clientRectUpdated = currentHtmlInput().getBoundingClientRect()

        assertEquals(50.0, clientRectUpdated.left - clientRectInitial.left, "left position updated")

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
        val deltaThreshold = 1.01
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

        sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        textFieldValue.awaitAndAssertTextEquals("step1")

        sendToHtmlInput(
            keyEvent("Backspace", code = "Backspace"),
            keyEvent("X"),
        )

        textFieldValue.awaitAndAssertTextEquals(
            "stepX",
            "Backspace should delete last symbol typed"
        )
    }

    @Test
    fun compositeInput() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        val backingTextField = getShadowRoot().querySelector("textarea")
        assertIs<HTMLTextAreaElement>(backingTextField)

        sendToHtmlInput(
            keyEvent("a"),
            compositionStart(),
            beforeInput("insertCompositionText", "a"),
            keyEvent("a", type = "keyup", isComposing = true),
            keyEvent("1", code = "Digit1", isComposing = true),
            beforeInput("insertCompositionText", "啊"),
            compositionEnd("啊"),
            keyEvent("1", code = "Digit1", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals("啊")

        sendToHtmlInput(
            keyEvent("x"),
            keyEvent("x", type = "keyup")
        )

        textFieldValue.awaitAndAssertTextEquals("啊x")
    }

    @Test
    fun compositeInputWebkit() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        val keyEvent = keyEvent("1", code = "Digit1")

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(50)

        sendToHtmlInput(
            compositionStart(),
            keyEvent("a", isComposing = true),
            keyEvent("a", type = "keyup", isComposing = true),
            beforeInput("deleteCompositionText", null),
            beforeInput("insertFromComposition", "啊"),
            compositionEnd("啊"),
            keyEvent,
            keyEvent("1", type = "keyup", code = "Digit1"),
        )

        textFieldValue.awaitAndAssertTextEquals("啊")

        // We can not change timestamp for js events, so we just add some delay to enforce it
        waitFor(100)

        sendToHtmlInput(
            keyEvent("b"),
            keyEvent("b", type = "keyup")
        )

        textFieldValue.awaitAndAssertTextEquals("啊b")
    }

    @Test
    fun mobileInput() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            mobileKeyDown(),
            compositionStart(),
            beforeInput("insertCompositionText", "a"),
            mobileKeyUp(),
            mobileKeyDown(),
            beforeInput("insertCompositionText", "ab"),
            mobileKeyUp(),
            mobileKeyDown(),
            beforeInput("insertCompositionText", "abc"),
            mobileKeyUp()
        )

        textFieldValue.awaitAndAssertTextEquals("abc")
    }

    @Ignore
    @Test
    fun repeatedAccent() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput("insertText", "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput("insertText", "c"),
            keyEvent("c", type = "keyup")
        )

        // TODO: this does not behave as desktop, ideally we should have "abc" here
        textFieldValue.awaitAndAssertTextEquals(
            "bc",
            "Repeat mode should be resolved as Accent Dialogue"
        )

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            beforeInput("insertText", "b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            beforeInput("insertText", "c"),
            keyEvent("c", type = "keyup")
        )

    }

    @Test
    fun repeatedDefault() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            beforeInput("insertText", "a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("b"),
            keyEvent("c")
        )


        textFieldValue.awaitAndAssertTextMatches( Regex("a+bc"), "Repeat mode should be resolved as Default")
    }

    @Test
    fun repeatedAccentMenuPressed() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            keyEvent("1", code = "Digit1"),
            beforeInput(inputType = "insertText", data = "à"),
            keyEvent("1", code = "Digit1", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals("à", "Choose symbol from Accent Menu")
    }

    @Test
    fun repeatedAccentMenuIgnoreNonTyped() = runApplicationTest {
        val textFieldValue = createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("ArrowLeft", code = "ArrowLeft"),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", repeat = true),
            keyEvent("ArrowLeft", code = "ArrowLeft", type = "keyup"),
            keyEvent("a"),
            keyEvent("a", type = "keyup"),
            keyEvent("b"),
            keyEvent("b", type = "keyup"),
            keyEvent("c"),
            keyEvent("c", type = "keyup"),
        )

        textFieldValue.awaitAndAssertTextEquals("abc", "XXX")
    }

    @Test
    fun repeatedAccentMenuClicked() = runApplicationTest {
        val textFieldValue =  createApplicationWithHolder()

        sendToHtmlInput(
            keyEvent("a"),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", repeat = true),
            keyEvent("a", type = "keyup"),
            beforeInput(inputType = "insertText", data = "æ"),
        )

        textFieldValue.awaitAndAssertTextEquals("æ", "Choose symbol from Accent Menu")
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

        sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("1")
        )

        inputHolder1.awaitAndAssertTextEquals("step1")

        focusRequester2.requestFocus()
        waitForHtmlInput()

        sendToHtmlInput(
            keyEvent("s"),
            keyEvent("t"),
            keyEvent("e"),
            keyEvent("p"),
            keyEvent("2")
        )

        inputHolder2.awaitAndAssertTextEquals("step2")
    }
}

private fun compositionStart(data: String = "") =
    CompositionEvent("compositionstart", CompositionEventInit(data = data))

private fun compositionEnd(data: String) =
    CompositionEvent("compositionend", CompositionEventInit(data = data))

private fun beforeInput(inputType: String, data: String?) =
    InputEvent("beforeinput", InputEventInit(inputType = inputType, data = data))

private fun mobileKeyDown() = keyEvent(type = "keydown", key = "Unidentified", code = "")
private fun mobileKeyUp() = keyEvent(type = "keydown", key = "Unidentified", code = "")

class BasicTextFieldTests : TextInputTests() {

    private class TextFieldValueHolder(private val textFieldValue: MutableState<TextFieldValue>) : TestInputState {
        override val text: String
            get() = textFieldValue.value.text

        @Composable
        override fun createBasicTextField(focusRequester: FocusRequester) {
            BasicTextField(
                value = textFieldValue.value,
                onValueChange = { value ->
                    textFieldValue.value = value
                },
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }

    override suspend fun createTestInputState(): TestInputState = TextFieldValueHolder(mutableStateOf(TextFieldValue()))
}

class BasicTextFieldTests2 : TextInputTests() {
    private class TextFieldStateHolder(private val textFieldState: TextFieldState) : TestInputState {
        override val text: CharSequence
            get() = textFieldState.text

        @Composable
        override fun createBasicTextField(focusRequester: FocusRequester) {
            BasicTextField(
                state = textFieldState,
                modifier = Modifier.focusRequester(focusRequester)
            )
        }
    }

    override suspend fun createTestInputState(): TestInputState = TextFieldStateHolder(TextFieldState())
}
