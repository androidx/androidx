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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlin.test.Test
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import androidx.compose.ui.window.*
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
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
        val canvasElement = document.createElement("canvas") as HTMLCanvasElement
        canvasElement.setAttribute("id", canvasId)
        document.body!!.appendChild(canvasElement)
        CanvasBasedWindow(canvasElementId = canvasId) {  }
    }

    @Test
    fun testPreventDefault()  {
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
        canvasElement.dispatchEvent(createCopyKeyboardEvent())
        assertEquals(1, stack.size)
        assertTrue(stack.last())

        canvasElement.dispatchEvent(createTypedEvent())
        assertEquals(2, stack.size)
        assertTrue( stack.last())
        assertEquals("c", changedValue)

        canvasElement.dispatchEvent(createEventShouldNotBePrevented())
        assertEquals(3, stack.size)
        assertFalse(stack.last())
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

internal fun createTypedEvent(): KeyboardEvent =
    KeyboardEventInit(key = "c", code = "KeyC", cancelable = true)
        .withKeyCode()
        .keyDownEvent()

internal fun createEventShouldNotBePrevented(): KeyboardEvent =
    KeyboardEventInit(ctrlKey = true, cancelable = true)
        .keyDownEvent()