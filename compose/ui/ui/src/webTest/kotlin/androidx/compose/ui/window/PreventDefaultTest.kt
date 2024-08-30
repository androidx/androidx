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

package androidx.compose.ui.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.keyDownEvent
import androidx.compose.ui.events.keyDownEventUnprevented
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreventDefaultTest : OnCanvasTests {

    @Test
    fun testPreventDefault() {
        val fr = FocusRequester()
        var changedValue = ""
        composableContent {
            TextField(
                value = "",
                onValueChange = { changedValue = it },
                modifier = Modifier.fillMaxSize().focusRequester(fr)
            )
            SideEffect {
                fr.requestFocus()
            }
        }

        var stack = mutableListOf<Boolean>()

        getCanvas().addEventListener("keydown", { event ->
            stack.add(event.defaultPrevented)
        })

        // dispatchEvent synchronously invokes all the listeners
        dispatchEvents(keyDownEvent("c"))
        assertEquals(1, stack.size)
        assertTrue(stack.last())

        dispatchEvents(keyDownEventUnprevented())
        assertEquals(2, stack.size)
        assertFalse(stack.last())

        assertEquals(changedValue, "c")

        // copy shortcut should not be prevented (we let browser create a corresponding event)
        dispatchEvents(keyDownEvent("c", metaKey = true, ctrlKey = true))
        assertEquals(3, stack.size)
        assertFalse(stack.last())

        assertEquals(changedValue, "c")

    }
}