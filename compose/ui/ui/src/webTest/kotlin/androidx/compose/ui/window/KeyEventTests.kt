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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.events.keyDownEvent
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
import kotlin.test.assertEquals

class KeyEventTests : OnCanvasTests {
    @Test
    // https://github.com/JetBrains/compose-multiplatform/issues/3644
    fun keyMappingIsValid() {
        val fr = FocusRequester()
        var mapping = ""
        var k: Key? = null
        createComposeWindow {
            Box(
                Modifier.size(1000.dp).background(Color.Red).focusRequester(fr).focusTarget()
                    .onKeyEvent {
                        k = it.key
                        mapping = it.key.toString()
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
            dispatchEvents(keyDownEvent(c.toString()))
            assertEquals(listOfKeys[index], k)
        }

        val listOfNumbers = listOf(
            Key.Zero, Key.One, Key.Two, Key.Three, Key.Four,
            Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine
        )

        ('0'..'9').forEachIndexed { index, c ->
            val id = c.toString()
            dispatchEvents(keyDownEvent(id, code = "Digit${id}"))
            assertEquals(listOfNumbers[index], k)
        }
    }

    @Test
    // https://github.com/JetBrains/compose-multiplatform/issues/2296
    fun onPreviewKeyEventShouldWork() {

        val fr = FocusRequester()
        val textValue = mutableStateOf("")
        var lastKeyEvent: KeyEvent? = null
        var stopPropagation = true

        createComposeWindow {
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

        dispatchEvents(keyDownEvent("t"))
        assertEquals(Key.T, lastKeyEvent!!.key)
        assertEquals("", textValue.value)

        stopPropagation = false
        dispatchEvents(
            keyDownEvent("t"),
            keyDownEvent("e"),
            keyDownEvent("s"),
            keyDownEvent("t"),
            keyDownEvent("x")
        )
        assertEquals(Key.X, lastKeyEvent!!.key)
        assertEquals("testx", textValue.value)
    }
}