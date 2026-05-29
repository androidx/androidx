/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.interaction

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.test.MockAppDelegate
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.test.utils.beginPress
import androidx.compose.ui.test.utils.cancel
import androidx.compose.ui.test.utils.release
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSStringFromSelector
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIKeyModifierShift
import platform.UIKit.UIPress
import platform.UIKit.UIPressTypeMenu
import platform.UIKit.UIPressTypeSelect
import platform.UIKit.UIPressTypeUpArrow
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
class KeyboardEventsTest {

    private class PressTrackingView : UIView(frame = CGRectZero.readValue()) {
        val began = mutableListOf<platform.UIKit.UIPressType>()
        val changed = mutableListOf<platform.UIKit.UIPressType>()
        val ended = mutableListOf<platform.UIKit.UIPressType>()
        val cancelled = mutableListOf<platform.UIKit.UIPressType>()

        override fun canBecomeFirstResponder(): Boolean = true

        override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
            presses.forEach { began += (it as UIPress).type }
        }

        override fun pressesChanged(presses: Set<*>, withEvent: UIPressesEvent?) {
            presses.forEach { changed += (it as UIPress).type }
        }

        override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
            presses.forEach { ended += (it as UIPress).type }
        }

        override fun pressesCancelled(presses: Set<*>, withEvent: UIPressesEvent?) {
            presses.forEach { cancelled += (it as UIPress).type }
        }
    }

    @Test
    fun selectPressDispatchesBeganAndEnded() = withPressTrackingView { trackingView, window ->
        val event = window.beginPress(UIPressTypeSelect)
        assertEquals(listOf(UIPressTypeSelect), trackingView.began)
        assertTrue(trackingView.ended.isEmpty())

        event.release()
        assertEquals(listOf(UIPressTypeSelect), trackingView.began)
        assertEquals(listOf(UIPressTypeSelect), trackingView.ended)
    }

    @Test
    fun cancelPressDispatchesCancelled() = withPressTrackingView { trackingView, window ->
        val event = window.beginPress(UIPressTypeSelect)
        event.cancel()

        assertEquals(listOf(UIPressTypeSelect), trackingView.began)
        assertEquals(listOf(UIPressTypeSelect), trackingView.cancelled)
        assertTrue(trackingView.ended.isEmpty())
    }

    private fun withPressTrackingView(
        block: (PressTrackingView, UIWindow) -> Unit
    ) {
        val appDelegate = MockAppDelegate()
        val trackingView = PressTrackingView()
        val viewController = UIViewController(nibName = null, bundle = null).apply {
            view.addSubview(trackingView)
        }
        try {
            appDelegate.setUpWindow(viewController)
            UIKitInstrumentedTest.waitUntil(
                conditionDescription = "Tracking view attached to window"
            ) { trackingView.window != null }
            val didBecomeFirstResponder = trackingView.becomeFirstResponder()
            assertTrue(didBecomeFirstResponder, "Tracking view failed to become first responder")
            assertTrue(trackingView.isFirstResponder(), "Tracking view should be first responder")

            val window = appDelegate.window() ?: error("MockAppDelegate has no window")

            UIKitInstrumentedTest.delay(1)

            block(trackingView, window)
        } finally {
            appDelegate.cleanUp()
        }
    }

    @Test
    fun composeFocusableReadsKeyInputs() = runUIKitInstrumentedTest {
        val requester = FocusRequester()
        val keyEvents = mutableListOf<Pair<KeyEventType, Key>>()

        setContent {
            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(requester)
                    .focusable()
                    .onKeyEvent { event ->
                        keyEvents += event.type to event.key
                        true
                    }
            )
        }

        waitForIdle()

        keystroke(UIPressTypeSelect)
        keystroke(UIPressTypeMenu)
        keystroke(UIPressTypeUpArrow)

        waitForIdle()

        assertEquals(
            listOf(
                KeyEventType.KeyDown to Key.DirectionCenter,
                KeyEventType.KeyUp to Key.DirectionCenter,
                KeyEventType.KeyDown to Key.Menu,
                KeyEventType.KeyUp to Key.Menu,
                KeyEventType.KeyDown to Key.DirectionUp,
                KeyEventType.KeyUp to Key.DirectionUp,
            ),
            keyEvents,
        )
    }

    @Test
    fun textFieldTypesHelloFromPresses() = runUIKitInstrumentedTest {
        val requester = FocusRequester()
        var value by mutableStateOf("")

        setContent {
            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(requester),
            )
        }

        typeWithKeyboard("Hello")

        assertEquals("Hello", value)
    }

    @Test
    fun simulateInterruptedKeyPressEvent() = runUIKitInstrumentedTest {
        val requester1 = FocusRequester()
        val requester2 = FocusRequester()
        val keyEvents = mutableListOf<Pair<KeyEventType, Key>>()

        setContent {
            LaunchedEffect(Unit) {
                requester1.requestFocus()
            }
            Column(
                modifier = Modifier.focusable().onPreviewKeyEvent { event ->
                    keyEvents += event.type to event.key
                    true
                }
            ) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(requester1)
                )
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.focusRequester(requester2)
                )
            }
        }

        // Press 'x' and verify key down is dispatched to the onKeyEvent modifier
        val xPress = beginKeyPress('x')
        waitForIdle()
        assertEquals(listOf(KeyEventType.KeyDown to Key.X), keyEvents)
        keyEvents.clear()

        // Remove focus from text field while 'x' is still held down
        requester2.requestFocus()
        waitForIdle()

        // Key up must be received when focus is removed (press is cancelled by UIKit)
        assertEquals(listOf(KeyEventType.KeyUp to Key.X), keyEvents)
        keyEvents.clear()

        // Release the physical key — no additional event should arrive
        xPress.release()
        waitForIdle()
        assertTrue(keyEvents.isEmpty(), "Expected no additional key events after physical release, got: $keyEvents")
    }

    @Test
    fun testMultipleKeyDownSimultaneously() = runUIKitInstrumentedTest {
        val requester = FocusRequester()
        val keyEvents = mutableListOf<Pair<KeyEventType, Key>>()

        setContent {
            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(requester)
                    .focusable()
                    .onKeyEvent { event ->
                        keyEvents += event.type to event.key
                        true
                    }
            )
        }

        fun assertReceived(typeWithEvent: Pair<KeyEventType, Key>) {
            assertEquals(listOf(typeWithEvent), keyEvents)
            keyEvents.clear()
        }

        // Press Shift
        val shiftEvent = beginModifierKeyPress(UIKeyModifierShift)
        waitForIdle()
        assertReceived(KeyEventType.KeyDown to Key.ShiftLeft)

        // Press Cmd
        val cmdEvent = beginModifierKeyPress(
            UIKeyModifierCommand,
            currentModifiers = UIKeyModifierShift,
        )
        waitForIdle()
        assertReceived(KeyEventType.KeyDown to Key.MetaLeft)

        // Press 'a'
        val aEvent = beginKeyPress('a', modifierFlags = UIKeyModifierShift or UIKeyModifierCommand)
        waitForIdle()
        assertReceived(KeyEventType.KeyDown to Key.A)

        // Release Cmd
        cmdEvent.release()
        waitForIdle()
        assertReceived(KeyEventType.KeyUp to Key.MetaLeft)

        // Release 'a'
        aEvent.release()
        waitForIdle()
        assertReceived(KeyEventType.KeyUp to Key.A)

        // Release Shift
        shiftEvent.release()
        waitForIdle()
        assertReceived(KeyEventType.KeyUp to Key.ShiftLeft)
    }

    @Test
    fun copyEventPropagatedToNativeView() = runUIKitInstrumentedTest {
        val view = FocusableView()

        setContent {
            UIKitView(
                factory = { view },
                modifier = Modifier.fillMaxSize()
            )
        }

        view.becomeFirstResponder()

        keystroke('c', modifierFlags = UIKeyModifierCommand)
        waitForIdle()

        assertTrue(view.isCopyCalled)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class FocusableView: UIView(frame = CGRectZero.readValue()) {
    var isCopyCalled = false

    override fun canBecomeFirstResponder(): Boolean = true

    override fun canPerformAction(action: COpaquePointer?, withSender: Any?): Boolean =
        NSStringFromSelector(action) == "copy:"

    override fun copy(sender: Any?) {
        isCopyCalled = true
    }
}
