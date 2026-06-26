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

package androidx.compose.ui.input

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.background
import androidx.compose.ui.events.keyEvent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.isClearFocusOnMouseDownEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.yield
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import org.w3c.dom.pointerevents.PointerEventInit

class TextFieldFocusTest : OnCanvasTests {

    @Test
    fun canMoveFocusForwardAndBackUsingTab() = runApplicationTest {
        val focusRequester = FocusRequester()

        suspend fun waitForSingleLineHtmlInput(): HTMLInputElement {
            while (true) {
                val element = getShadowRoot().querySelector("input")
                if (element is HTMLInputElement) {
                    return element
                }
                yield()
            }
        }

        var firstTextFieldFocusState: FocusState? = null
        var secondTextFieldFocusState: FocusState? = null

        createComposeWindow {
            Column {
                TextField(
                    state = rememberTextFieldState(initialText = "Hello"),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged({
                            firstTextFieldFocusState = it
                        }),
                    lineLimits = TextFieldLineLimits.SingleLine
                )

                TextField(
                    state = rememberTextFieldState(initialText = "World"),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    modifier = Modifier.onFocusChanged({
                        secondTextFieldFocusState = it
                    })
                )
            }
        }

        var lastKeydownEventOnRoot: Event? = null

        focusRequester.requestFocus()

        val htmlInput1 = waitForSingleLineHtmlInput()
        assertNotNull(firstTextFieldFocusState)
        assertNotNull(secondTextFieldFocusState)
        assertEquals(true, firstTextFieldFocusState.isFocused)
        assertEquals(false, secondTextFieldFocusState.isFocused)

        getShadowRoot().addEventListener("keydown", {
            lastKeydownEventOnRoot = it
        })

        val tabKeyDown = keyEvent(
            key = "Tab",
            type = "keydown",
            keyCode = Key.Tab.keyCode.toInt(),
            code = "Tab"
        )
        htmlInput1.dispatchEvent(tabKeyDown)
        awaitAnimationFrame()
        assertNotNull(lastKeydownEventOnRoot)
        assertEquals("Tab", (lastKeydownEventOnRoot as KeyboardEvent).key)
        assertFalse((lastKeydownEventOnRoot as KeyboardEvent).shiftKey)
        assertTrue(lastKeydownEventOnRoot!!.defaultPrevented)
        lastKeydownEventOnRoot = null

        assertEquals(false, firstTextFieldFocusState.isFocused)
        assertEquals(true, secondTextFieldFocusState.isFocused)

        /* Now move focus back using Tab+Shift */

        val htmlInput2 = waitForSingleLineHtmlInput()
        val tabKeyDownWithShift = keyEvent(
            key = "Tab",
            type = "keydown",
            keyCode = Key.Tab.keyCode.toInt(),
            code = "Tab",
            shiftKey = true
        )

        htmlInput2.dispatchEvent(tabKeyDownWithShift)
        awaitAnimationFrame()

        assertEquals(true, firstTextFieldFocusState.isFocused)
        assertEquals(false, secondTextFieldFocusState.isFocused)

        assertNotNull(lastKeydownEventOnRoot)
        assertEquals("Tab", (lastKeydownEventOnRoot as KeyboardEvent).key)
        assertTrue((lastKeydownEventOnRoot as KeyboardEvent).shiftKey)
        assertTrue(lastKeydownEventOnRoot!!.defaultPrevented)
    }

    private fun mouseDownPointerEvent(
        x: Int, y: Int,
    ): PointerEvent = PointerEvent(
        "pointerdown",
        PointerEventInit(
            clientX = x, clientY = y,
            button = 0, buttons = 1,
            pointerType = "mouse"
        )
    )

    @Test
    fun mouseClickOutsideClearsFocusWithClearFocusOnMouseDownEnabled() {
        val prevClearFocusOnMouseDownEnabled = ComposeUiFlags.isClearFocusOnMouseDownEnabled
        ComposeUiFlags.isClearFocusOnMouseDownEnabled = true
        try {
            runApplicationTest {
                val focusRequester = FocusRequester()
                var focusState: FocusState? = null

                createComposeWindow {
                    Column(Modifier.size(300.dp, 400.dp)) {
                        Box(Modifier.testTag("box").size(100.dp).background(Color.Gray))
                        BasicTextField(
                            state = rememberTextFieldState(),
                            modifier = Modifier
                                .testTag("textField")
                                .focusRequester(focusRequester)
                                .onFocusChanged {
                                    focusState = it
                                }
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    }
                }
                assertTrue(focusState!!.isFocused, "Expected to be focused after requestFocus")

                dispatchEvents(mouseDownPointerEvent(50, 50))
                awaitIdle()
                assertFalse(focusState!!.isFocused, "Expected to lose focus after clicking outside")
            }
        } finally {
            ComposeUiFlags.isClearFocusOnMouseDownEnabled = prevClearFocusOnMouseDownEnabled
        }
    }

    @Test
    fun mouseClickOutsideDoesntClearsFocusWhenDisabled() = runApplicationTest {
        val focusRequester = FocusRequester()
        var focusState: FocusState? = null

        createComposeWindow(
            configure = {
                isClearFocusOnMouseDownEnabled = false
            }
        ) {
            Column(Modifier.size(300.dp, 400.dp)) {
                Box(Modifier.testTag("box").size(100.dp).background(Color.Gray))
                BasicTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier
                        .testTag("textField")
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            focusState = it
                        }
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }
        assertTrue(focusState!!.isFocused, "Expected to be focused after requestFocus")

        dispatchEvents(mouseDownPointerEvent(50, 50))
        awaitIdle()
        assertTrue(focusState!!.isFocused, "Expected to keep focus despite clicking outside")
    }


}