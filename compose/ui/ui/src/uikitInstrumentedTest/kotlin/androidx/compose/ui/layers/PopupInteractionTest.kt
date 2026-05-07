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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.findFocusedUITextInput
import androidx.compose.ui.test.findNodeWithLabel
import androidx.compose.ui.test.findNodeWithLabelOrNull
import androidx.compose.ui.test.findNodeWithTag
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.center
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PopupInteractionTest {
    @Test
    fun testPopupDismissOnClickOutsideEnabled() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = {
                    dismissTriggered = true
                },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) {
                Text("Popup")
            }
        }

        tap(outOfPopupBoundsPoint)

        waitForIdle()

        assertTrue(dismissTriggered)
    }

    @Test
    fun testPopupDismissOnClickOutsideDisabled() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismissTriggered = true },
                properties = PopupProperties(dismissOnClickOutside = false)
            ) {
                Text("Popup")
            }
        }

        tap(outOfPopupBoundsPoint)
        waitForIdle()

        assertFalse(dismissTriggered)
    }

    @Test
    fun testPopupDismissOnClickOutsideEnabledAndFocusable() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismissTriggered = true },
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    focusable = true
                )
            ) {
                Text("Popup")
            }
        }

        tap(outOfPopupBoundsPoint)
        waitForIdle()

        assertTrue(dismissTriggered)
    }

    @Test
    fun testPopupDismissOnClickOutsideDisabledAndFocusable() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismissTriggered = true },
                properties = PopupProperties(
                    dismissOnClickOutside = false,
                    focusable = true
                )
            ) {
                Text("Popup")
            }
        }

        tap(outOfPopupBoundsPoint)
        waitForIdle()

        assertFalse(dismissTriggered)
    }

    @Test
    fun testManyPopupsDismissOnClickOutside() = runUIKitInstrumentedTest {
        var dismiss1Triggered = false
        var dismiss2Triggered = false
        var dismiss3Triggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismiss1Triggered = true },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) { Text("Popup 1") }
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismiss2Triggered = true },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) { Text("Popup 2") }
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismiss3Triggered = true },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) { Text("Popup 3") }
        }

        tap(outOfPopupBoundsPoint)
        waitForIdle()

        assertTrue(dismiss1Triggered)
        assertTrue(dismiss2Triggered)
        assertTrue(dismiss3Triggered)
    }

    @Test
    fun testManyPopupsBelowFocusableDismissOnClickOutside() = runUIKitInstrumentedTest {
        var dismiss1Triggered = false
        var dismiss2Triggered = false
        var dismiss3Triggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismiss1Triggered = true },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) { Text("Popup 1") }
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismiss2Triggered = true },
                properties = PopupProperties(
                    dismissOnClickOutside = true,
                    focusable = true
                )
            ) { Text("Popup 2") }
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismiss3Triggered = true },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) { Text("Popup 3") }
        }

        tap(outOfPopupBoundsPoint)
        waitForIdle()

        assertFalse(dismiss1Triggered)
        assertTrue(dismiss2Triggered)
        assertTrue(dismiss3Triggered)
    }

    @Test
    fun testFocusablePopupInteraction() = runUIKitInstrumentedTest {
        var contentButtonClicked = false
        var popupButtonClicked = false
        setContent {
            Button(
                onClick = { contentButtonClicked = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Content Button")
            }
            Popup(
                properties = PopupProperties(dismissOnClickOutside = false, focusable = true)
            ) {
                Button({ popupButtonClicked = true }) {
                    Text("Popup Button")
                }
            }
        }

        findNodeWithLabel("Popup Button").tap()
        waitForIdle()
        assertTrue(popupButtonClicked)

        val contentButtonAccessible = findNodeWithLabelOrNull("Content Button") != null
        assertFalse(contentButtonAccessible)

        tap(outOfPopupBoundsPoint)
        waitForIdle()
        assertFalse(contentButtonClicked)
    }

    @Test
    fun testNonFocusablePopupInteraction() = runUIKitInstrumentedTest {
        var contentButtonClicked = false
        var popupButtonClicked = false
        setContent {
            Button(
                onClick = { contentButtonClicked = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Content Button")
            }
            Popup(
                properties = PopupProperties(dismissOnClickOutside = false, focusable = false)
            ) {
                Button({ popupButtonClicked = true }) {
                    Text("Popup Button")
                }
            }
        }

        findNodeWithLabel("Popup Button").tap()
        waitForIdle()
        assertTrue(popupButtonClicked)

        findNodeWithLabel("Content Button").tap()
        waitForIdle()
        assertTrue(contentButtonClicked)
    }

    @Test
    fun testBlockingNonFocusablePopupBlocksContentInteraction() = runUIKitInstrumentedTest {
        var contentButtonClicked = false
        var popupButtonClicked = false
        setContent {
            Button(
                onClick = { contentButtonClicked = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Content Button")
            }
            @OptIn(ExperimentalComposeUiApi::class)
            Popup(
                properties = PopupProperties(
                    dismissOnClickOutside = false,
                    focusable = false,
                    consumePointerInputOutside = true,
                )
            ) {
                Button({ popupButtonClicked = true }) {
                    Text("Popup Button")
                }
            }
        }

        findNodeWithLabel("Popup Button").tap()
        waitForIdle()
        assertTrue(popupButtonClicked)

        tap(outOfPopupBoundsPoint)
        waitForIdle()
        assertFalse(contentButtonClicked)
    }

    @Test
    fun testNonBlockingFocusablePopupAllowsContentInteraction() = runUIKitInstrumentedTest {
        var contentButtonClicked = false
        var popupButtonClicked = false
        setContent {
            Button(
                onClick = { contentButtonClicked = true },
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Content Button")
            }
            Popup(
                properties = PopupProperties(
                    dismissOnClickOutside = false,
                    focusable = true,
                    consumePointerInputOutside = false,
                )
            ) {
                Button({ popupButtonClicked = true }) {
                    Text("Popup Button")
                }
            }
        }

        findNodeWithLabel("Popup Button").tap()
        waitForIdle()
        assertTrue(popupButtonClicked)

        tap(outOfPopupBoundsPoint)
        waitForIdle()
        assertTrue(contentButtonClicked)
    }

    @Test
    fun testTextInputFocusInPopup() = runUIKitInstrumentedTest {
        setContent {
            Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = true)) {
                TextField("", {}, modifier = Modifier.testTag("TextField"))
            }
        }

        findNodeWithTag("TextField").tap()

        waitForIdle()

        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)
    }

    @Test
    fun testKeyboardHidesWhenFocusablePopupOpens() = runUIKitInstrumentedTest {
        val requester = FocusRequester()
        val showDialog = mutableStateOf(false)
        setContent {
            TextField("", {}, modifier = Modifier.focusRequester(requester))

            if (showDialog.value) {
                Popup(properties = PopupProperties(focusable = true)) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red))
                }
            }
        }

        requester.requestFocus()
        waitForIdle()
        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)

        // Verify that the popup temporarily removed focus from input
        showDialog.value = true
        waitForIdle()
        assertEquals(0.dp, keyboardHeight)

        // Verify that focus returns to the initial input
        showDialog.value = false
        waitForIdle()
        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)
    }

    @Test
    fun testKeyboardNotHidesWhenNonFocusablePopupOpens() = runUIKitInstrumentedTest {
        val requester = FocusRequester()
        val showDialog = mutableStateOf(false)
        setContent {
            TextField("", {}, modifier = Modifier.focusRequester(requester))

            if (showDialog.value) {
                Popup(properties = PopupProperties(focusable = false)) {
                    Box(modifier = Modifier.size(10.dp).background(Color.Red))
                }
            }
        }

        requester.requestFocus()
        waitForIdle()
        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)

        // Verify that the non-focusable popup does not remove focus from input
        showDialog.value = true
        waitForIdle()
        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)

        // Verify that focus keeps on the initial input
        showDialog.value = false
        waitForIdle()
        assertNotNull(findFocusedUITextInput())
        assertNotEquals(0.dp, keyboardHeight)
    }

    @Test
    fun testPopupDismissByClickingOnAnotherPopup() = runUIKitInstrumentedTest {
        var showPopup1 by mutableStateOf(true)
        var showPopup2 by mutableStateOf(false)
        setContent {
            if (showPopup1) {
                Popup(
                    onDismissRequest = { showPopup1 = false }, properties = PopupProperties(
                        focusable = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Column(modifier = Modifier.background(Color.Blue).size(100.dp)) {
                        androidx.compose.material3.Text(
                            text = "Popup 1",
                            modifier = Modifier.testTag("Popup 1")
                        )
                    }

                    if (showPopup2) {
                        Popup(
                            alignment = Alignment.BottomCenter,
                            onDismissRequest = { showPopup2 = false },
                            properties = PopupProperties(
                                focusable = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            Column(modifier = Modifier.background(Color.Red)) {
                                androidx.compose.material3.Text(
                                    text = "Popup 2",
                                    modifier = Modifier.testTag("Popup 2")
                                )
                            }
                        }
                    }
                }
            }
        }

        val popupCenter = findNodeWithTag("Popup 1").frame?.center()
            ?: error("Popup 1 does not have a valid frame")

        showPopup2 = true
        waitForIdle()
        findNodeWithTag("Popup 2") // Ensure the second popup is opened

        tap(popupCenter)
        waitForIdle()

        assertFalse(showPopup2)
        assertTrue(showPopup1)
    }

    @Test
    fun testTapInsidePopupDoesNotDismiss() = runUIKitInstrumentedTest {
        var dismissTriggered = false
        setContent {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { dismissTriggered = true },
                properties = PopupProperties(dismissOnClickOutside = true)
            ) {
                Text("Popup Content", modifier = Modifier.testTag("PopupContent"))
            }
        }

        findNodeWithTag("PopupContent").tap()
        waitForIdle()

        assertFalse(dismissTriggered)
    }

    @Test
    fun testNonFocusablePopupDoesNotBlockFocusablePopupDismiss() = runUIKitInstrumentedTest {
        var dismissFocusableTriggered = false
        var dismissNonFocusableTriggered = false
        setContent {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismissFocusableTriggered = true },
                properties = PopupProperties(dismissOnClickOutside = true, focusable = true)
            ) {
                Text("Focusable Popup")
            }
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { dismissNonFocusableTriggered = true },
                properties = PopupProperties(dismissOnClickOutside = false, focusable = false)
            ) {
                Text("Non-Focusable Popup")
            }
        }

        tap(outOfPopupBoundsPoint)
        waitForIdle()

        assertTrue(dismissFocusableTriggered)
        assertFalse(dismissNonFocusableTriggered)
    }

    private val UIKitInstrumentedTest.outOfPopupBoundsPoint: DpOffset
        get() = DpOffset(x = screenSize.width / 2, y = 100.dp)
}
