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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import androidx.compose.ui.window.*
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.browser.window
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.KeyboardEventInit

class CanvasBasedWindowTests {

    private val canvasId = "canvas1"

    @AfterTest
    fun cleanup() {
        document.getElementById(canvasId)?.remove()
    }

    @Test
    fun canCreate() {
        if (isHeadlessBrowser()) return
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)
        CanvasBasedWindow(canvasElementId = canvasId) {  }
    }

    @Test
    fun testPreventDefault() {
        if (isHeadlessBrowser()) return
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)

        val fr = FocusRequester()
        var changedValue = ""
        CanvasBasedWindow(canvasElementId = canvasId) {
            TextField(
                value = "",
                onValueChange = { changedValue = it },
                modifier = Modifier.fillMaxSize().focusRequester(fr)
            )
            SideEffect {
                fr.requestFocus()
            }
        }

        val stack = mutableListOf<Boolean>()
        canvasElement.addEventListener("keydown", { event ->
            stack.add(event.defaultPrevented)
        })

        // dispatchEvent synchronously invokes all the listeners
        canvasElement.dispatchEvent(createTypedEvent())
        assertEquals(1, stack.size)
        assertTrue(stack.last())

        canvasElement.dispatchEvent(createEventShouldNotBePrevented())
        assertEquals(2, stack.size)
        assertFalse(stack.last())

        // copy shortcut should not be prevented (we let browser create a corresponding event)
        canvasElement.dispatchEvent(createCopyKeyboardEvent())
        assertEquals(3, stack.size)
        assertFalse(stack.last())
    }

    @Test
    // https://github.com/JetBrains/compose-multiplatform/issues/3644
    fun keyMappingIsValid() {
        if (isHeadlessBrowser()) return

        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)

        val fr = FocusRequester()
        var mapping = ""
        var k: Key? = null
        CanvasBasedWindow(canvasElementId = canvasId) {
            Box(Modifier.size(1000.dp).background(Color.Red).focusRequester(fr).focusTarget().onKeyEvent {
                k = it.key
                mapping = it.key.toString()
                println(mapping)
                false
            }) {
                Text("Try to press different keys and look at the console...")
            }
            SideEffect {
                fr.requestFocus()
            }
        }

        val listOfKeys = listOf(
            Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G,
            Key.H, Key.I, Key.J, Key.K, Key.L, Key.M, Key.N,
            Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U,
            Key.V, Key.W, Key.X, Key.Y, Key.Z
        )

        ('a'..'z').forEachIndexed { index, c ->
            canvasElement.dispatchEvent(createTypedEvent(c))
            assertEquals(listOfKeys[index], k)
        }

        val listOfNumbers = listOf(
            Key.Zero, Key.One, Key.Two, Key.Three, Key.Four,
            Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine
        )

        ('0'..'9').forEachIndexed { index, c ->
            canvasElement.dispatchEvent(createTypedEvent(c))
            assertEquals(listOfNumbers[index], k)
        }
    }

    @Test
    // https://github.com/JetBrains/compose-multiplatform/issues/2296
    fun onPreviewKeyEventShouldWork() {
        if (isHeadlessBrowser()) return
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)

        val fr = FocusRequester()
        val textValue = mutableStateOf("")
        var lastKeyEvent: KeyEvent? = null
        var stopPropagation = true

        CanvasBasedWindow(canvasElementId = canvasId) {
            TextField(
                value = textValue.value,
                onValueChange = { textValue.value = it },
                modifier = Modifier.fillMaxSize().focusRequester(fr).onPreviewKeyEvent {
                    lastKeyEvent = it
                    return@onPreviewKeyEvent stopPropagation
                }
            )
            SideEffect {
                fr.requestFocus()
            }
        }

        canvasElement.dispatchEvent(createTypedEvent('t'))
        assertEquals(Key.T, lastKeyEvent!!.key)
        assertEquals("", textValue.value)

        stopPropagation = false
        canvasElement.dispatchEvent(createTypedEvent('t'))
        canvasElement.dispatchEvent(createTypedEvent('e'))
        canvasElement.dispatchEvent(createTypedEvent('s'))
        canvasElement.dispatchEvent(createTypedEvent('t'))
        canvasElement.dispatchEvent(createTypedEvent('x'))
        assertEquals(Key.X, lastKeyEvent!!.key)
        assertEquals("testx", textValue.value)
    }
}

internal external interface KeyboardEventInitExtended : KeyboardEventInit {
    var keyCode: Int?
}

internal fun KeyboardEventInit.keyDownEvent() = KeyboardEvent("keydown", this)
internal fun KeyboardEventInit.withKeyCode() = (this as KeyboardEventInitExtended).apply {
    keyCode = key!!.uppercase().first().code
}

internal fun createCopyKeyboardEvent(): KeyboardEvent =
    KeyboardEventInit(key = "c", code = "KeyC", ctrlKey = true, metaKey = true, cancelable = true)
        .withKeyCode()
        .keyDownEvent()

internal fun createTypedEvent(c: Char = 'c'): KeyboardEvent =
    KeyboardEventInit(key = "$c", code = "Key${c.uppercase()}", cancelable = true)
        .withKeyCode()
        .keyDownEvent()

internal fun createEventShouldNotBePrevented(): KeyboardEvent =
    KeyboardEventInit(ctrlKey = true, cancelable = true)
        .keyDownEvent()


// Unreliable heuristic, but it works for now
internal fun isHeadlessBrowser(): Boolean = window.navigator.userAgent.contains("Headless")