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

import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.InputEvent
import androidx.compose.ui.events.InputEventInit
import androidx.compose.ui.events.createMouseEvent
import androidx.compose.ui.events.createTouchEvent
import androidx.compose.ui.events.keyDownEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.sendFromScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.browser.document
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest

class TextInputTests : OnCanvasTests  {

    @Test
    fun keyboardEventPassedToTextField() = runTest {

        val textInputChannel = Channel<String>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val (firstFocusRequester, secondFocusRequester) = FocusRequester.createRefs()

        createComposeWindow {
            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(firstFocusRequester)
            )

            TextField(
                value = "",
                onValueChange = { value ->
                    textInputChannel.sendFromScope(value)
                },
                modifier = Modifier.focusRequester(secondFocusRequester)
            )

            SideEffect {
                secondFocusRequester.requestFocus()
                firstFocusRequester.requestFocus()
            }
        }

        assertNull(document.querySelector("textarea"))

        dispatchEvents(
            keyDownEvent("s"),
            keyDownEvent("t"),
            keyDownEvent("e"),
            keyDownEvent("p"),
            keyDownEvent("1")
        )


        assertEquals("step1", textInputChannel.receive())
        assertNull(document.querySelector("textarea"))

        // trigger virtual keyboard
        dispatchEvents(createTouchEvent("touchstart"))
        secondFocusRequester.requestFocus()

        assertNotNull(document.querySelector("textarea"))

        dispatchEvents(
            keyDownEvent("s"),
            keyDownEvent("t"),
            keyDownEvent("e"),
            keyDownEvent("p"),
            keyDownEvent("2")
        )

        assertEquals("step2", textInputChannel.receive())

        val backingField = document.querySelector("textarea")!!

        dispatchEvents(
            keyDownEvent("s"),
            keyDownEvent("t"),
            keyDownEvent("e"),
            keyDownEvent("p"),
            keyDownEvent("3")
        )

        assertEquals("step2step3", textInputChannel.receive())

        backingField.dispatchEvent(InputEvent("input", InputEventInit("insertText", "step4XX")))

        assertEquals("step2step3step4XX", textInputChannel.receive())

        backingField.dispatchEvent(InputEvent("input", InputEventInit("deleteContentBackward", "")))
        backingField.dispatchEvent(InputEvent("input", InputEventInit("deleteContentBackward", "")))
        assertEquals("step2step3step4", textInputChannel.receive())

        // trigger hardware keyboard
        dispatchEvents(createMouseEvent("mousedown"))
        firstFocusRequester.requestFocus()

        dispatchEvents(
            keyDownEvent("s"),
            keyDownEvent("t"),
            keyDownEvent("e"),
            keyDownEvent("p"),
            keyDownEvent("5")
        )

        assertEquals("step1step5", textInputChannel.receive())
    }
}