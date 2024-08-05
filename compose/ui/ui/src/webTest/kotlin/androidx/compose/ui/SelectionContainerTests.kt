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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit

class SelectionContainerTests : OnCanvasTests {

    @BeforeTest
    fun setup() {
        resetCanvas()
    }

    private fun HTMLCanvasElement.doClick() {
        dispatchEvent(MouseEvent("mousedown", MouseEventInit(5, 5, 5, 5, buttons = 1, button = 1)))
        dispatchEvent(MouseEvent("mouseup", MouseEventInit(5, 5, 5, 5, buttons = 0, button = 1)))
    }

    @Test
    fun canSelectOneWordUsingDoubleClick() = runTest {
        val syncChannel = Channel<Selection?>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        var viewConfiguration: ViewConfiguration? = null

        createComposeWindow {
            var selection by remember { mutableStateOf<Selection?>(null) }

            androidx.compose.foundation.text.selection.SelectionContainer(
                modifier = Modifier.fillMaxSize(),
                selection = selection,
                onSelectionChange = {
                    selection = it
                    syncChannel.sendFromScope(it)
                },
                children = {
                    Column {
                        Text("qwerty uiopasdfghjklzxcvbnm")
                        Text("mnbvcxzlkjhgfdsapoiuytrewq")
                    }

                    viewConfiguration = LocalViewConfiguration.current
                }
            )
        }

        val canvas = getCanvas()
        canvas.dispatchEvent(MouseEvent("mouseenter"))

        // single click - no selection expected
        canvas.doClick()

        var selection = syncChannel.receive()
        assertFalse(selection.exists())

        // delay to prevent interpreting next single click as a second click of the previous click,
        // so we make sure it won't appear as a double click
        withContext(Dispatchers.Default) {
            delay(viewConfiguration!!.doubleTapTimeoutMillis)
        }

        // now double click:
        canvas.doClick()
        canvas.doClick()

        selection = syncChannel.receive()
        assertTrue(selection.exists())
        assertEquals(0, selection!!.start.offset)
        assertEquals(6, selection!!.end.offset)

        withContext(Dispatchers.Default) {
            delay(viewConfiguration!!.doubleTapTimeoutMillis)
        }
        // reset selection by clicking
        canvas.doClick()
        selection = syncChannel.receive()
        assertFalse(selection.exists())
    }

    @Test
    fun canSelectOneLineUsingTripleClick() = runTest {
        val syncChannel = Channel<Selection?>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        var viewConfiguration: ViewConfiguration? = null

        createComposeWindow {
            var selection by remember { mutableStateOf<Selection?>(null) }

            androidx.compose.foundation.text.selection.SelectionContainer(
                modifier = Modifier.fillMaxSize(),
                selection = selection,
                onSelectionChange = {
                    selection = it
                    syncChannel.sendFromScope(it)
                },
                children = {
                    Column {
                        Text("012345 uiopasdfghjklzxcvbnm")
                        Text("mnbvcxzlkjhgfdsapoiuytrewq")
                    }

                    viewConfiguration = LocalViewConfiguration.current
                }
            )
        }

        val canvas = getCanvas()
        canvas.dispatchEvent(MouseEvent("mouseenter"))

        // triple click
        canvas.doClick()
        canvas.doClick()
        canvas.doClick()

        var selection = syncChannel.receive()
        assertTrue(selection.exists())
        assertEquals(0, selection!!.start.offset)
        assertEquals(27, selection!!.end.offset)

        withContext(Dispatchers.Default) {
            delay(viewConfiguration!!.doubleTapTimeoutMillis)
        }
        // reset selection by clicking
        canvas.doClick()
        selection = syncChannel.receive()
        assertFalse(selection.exists())
    }

    @Test
    fun twoSingleClicksDoNotTriggerSelection() = runTest {
        val syncChannel = Channel<Selection?>(
            5, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        var selectionCallbackCounter = 0

        var viewConfiguration: ViewConfiguration? = null

        createComposeWindow {
            var selection by remember { mutableStateOf<Selection?>(null) }

            androidx.compose.foundation.text.selection.SelectionContainer(
                modifier = Modifier.fillMaxSize(),
                selection = selection,
                onSelectionChange = {
                    selection = it
                    syncChannel.sendFromScope(it)
                    selectionCallbackCounter++
                },
                children = {
                    Column {
                        Text("asdfgh uiopasdfghjklzxcvbnm")
                        Text("mnbvcxzlkjhgfdsapoiuytrewq")
                    }

                    viewConfiguration = LocalViewConfiguration.current
                }
            )
        }

        val canvas = getCanvas()
        canvas.dispatchEvent(MouseEvent("mouseenter"))

        // first single click
        canvas.doClick()
        // pause to ensure no double-click
        withContext(Dispatchers.Default) {
            delay(viewConfiguration!!.doubleTapTimeoutMillis)
        }
        // second single click
        canvas.doClick()
        withContext(Dispatchers.Default) {
            delay(viewConfiguration!!.doubleTapTimeoutMillis)
        }

        repeat(selectionCallbackCounter) {
            val selection = syncChannel.receive()
            val actualSelectionLength =
                (selection?.end?.offset ?: 0) - (selection?.start?.offset ?: 0)
            assertEquals(0, actualSelectionLength)
        }
    }
}

private fun Selection?.exists() = (this !== null) && !this.toTextRange().collapsed
