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

package androidx.compose.ui.layers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findFocusedUITextInput
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DialogInteractionTest {
    @Test
    fun testDialogDismissOnClickOutsideEnabled() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Dialog(
                onDismissRequest = {
                    dismissTriggered = true
                },
                properties = DialogProperties(dismissOnClickOutside = true)
            ) {
                Button(onClick = {}) {
                    Text("Dialog")
                }
            }
        }

        tap(outOfDialogBoundsPoint)

        waitForIdle()

        assertTrue(dismissTriggered)
    }

    @Test
    fun testDialogDismissOnClickOutsideDisabled() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Dialog(
                onDismissRequest = { dismissTriggered = true },
                properties = DialogProperties(dismissOnClickOutside = false)
            ) {
                Button(onClick = {}) {
                    Text("Dialog")
                }
            }
        }

        tap(outOfDialogBoundsPoint)
        waitForIdle()

        assertFalse(dismissTriggered)
    }

    @Test
    fun testManyDialogsDismissOnClickOutside() = runUIKitInstrumentedTest {
        val showDialog1 = mutableStateOf(true)
        val showDialog2 = mutableStateOf(true)
        setContent {
            if (showDialog1.value) {
                Dialog(
                    onDismissRequest = { showDialog1.value = false },
                    properties = DialogProperties(dismissOnClickOutside = true)
                ) {
                    Button(onClick = {}) {
                        Text("Dialog")
                    }
                }
            }
            if (showDialog2.value) {
                Dialog(
                    onDismissRequest = { showDialog2.value = false },
                    properties = DialogProperties(dismissOnClickOutside = true)
                ) {
                    Button(onClick = {}) {
                        Text("Dialog")
                    }
                }
            }
        }

        tap(outOfDialogBoundsPoint)
        waitForIdle()

        assertTrue(showDialog1.value)
        assertFalse(showDialog2.value)

        tap(outOfDialogBoundsPoint)
        waitForIdle()

        assertFalse(showDialog1.value)
        assertFalse(showDialog2.value)
    }

    @Test
    fun testTextInputFocusInDialog() = runUIKitInstrumentedTest {
        setContent {
            Dialog(onDismissRequest = {}) {
                TextField("", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").tap()

        waitForIdle()

        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)
    }

    @Test
    fun testKeyboardHideWhenDialogOpens() = runUIKitInstrumentedTest {
        val requester = FocusRequester()
        val showDialog = mutableStateOf(false)
        setContent {
            TextField("", {}, modifier = Modifier.focusRequester(requester))

            if (showDialog.value) {
                Dialog(onDismissRequest = {}) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red))
                }
            }
        }

        requester.requestFocus()
        waitForIdle()
        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)

        // Verify that the dialog temporarily removed focus from input
        showDialog.value = true
        waitForIdle()
        assertEquals(0.dp, keyboardHeight)

        // Verify that focus returns to the initial input
        showDialog.value = false
        waitForIdle()
        assertNotEquals(0.dp, keyboardHeight)
    }

    @Test
    fun testDialogWithPopupCoexistence() = runUIKitInstrumentedTest {
        var showPopup by mutableStateOf(true)
        var showDialog by mutableStateOf(true)
        setContent {
            if (showPopup) {
                Popup(
                    alignment = Alignment.Center,
                    onDismissRequest = { showPopup = false },
                    properties = PopupProperties(dismissOnClickOutside = true, focusable = true)
                ) {
                    Text("Popup")
                }
            }
            if (showDialog) {
                // Dialog is added after popup, so it sits above the popup in Z-order
                Dialog(
                    onDismissRequest = { showDialog = false },
                    properties = DialogProperties(dismissOnClickOutside = true)
                ) {
                    Text("Dialog")
                }
            }
        }

        // First tap: dialog is topmost, absorbs touch and dismisses; popup is shielded
        tap(outOfDialogBoundsPoint)
        waitForIdle()

        assertFalse(showDialog)
        assertTrue(showPopup)

        // Second tap: only the popup layer remains, it absorbs touch and dismisses
        tap(outOfDialogBoundsPoint)
        waitForIdle()

        assertFalse(showPopup)
    }

    @Test
    fun testDialogAbsorbsTouchesWhenDismissDisabled() = runUIKitInstrumentedTest {
        var backgroundButtonClicked = false
        setContent {
            Button(
                onClick = { backgroundButtonClicked = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Background Button")
            }
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(dismissOnClickOutside = false)
            ) {
                Box(modifier = Modifier.size(100.dp))
            }
        }

        tap(outOfDialogBoundsPoint)
        waitForIdle()

        assertFalse(backgroundButtonClicked)
    }

    private val UIKitInstrumentedTest.outOfDialogBoundsPoint: DpOffset
        get() = DpOffset(x = screenSize.width / 2, y = 100.dp)
}