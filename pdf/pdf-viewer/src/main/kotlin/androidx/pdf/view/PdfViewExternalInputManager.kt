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

package androidx.pdf.view

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/**
 * This manager acts as a central dispatcher for external input. It inspects each event to determine
 * if an action, such as scrolling, should be performed. If so, it delegates the action to a
 * [PdfViewExternalInputHandler].
 *
 * @param pdfView The view on which the actions are to performed.
 */
internal class PdfViewExternalInputManager(pdfView: PdfView) {

    private val keyboardActionHandler: PdfViewKeyboardActionHandler =
        PdfViewKeyboardActionHandler(pdfView)
    private val mouseActionHandler: PdfViewMouseActionHandler = PdfViewMouseActionHandler(pdfView)

    /**
     * Handles a [KeyEvent] and triggers the corresponding action if it's a recognized key press.
     *
     * This method only processes `ACTION_DOWN` events for supported key codes.
     *
     * @param event The [KeyEvent] to process.
     * @return `true` if the key event was handled, `false` otherwise.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                keyboardActionHandler.scrollUp()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                keyboardActionHandler.scrollDown()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                keyboardActionHandler.scrollLeft()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                keyboardActionHandler.scrollRight()
                true
            }
            else -> false
        }
    }

    /**
     * Handles a [MotionEvent] and triggers the corresponding action if it's a recognized mouse
     * shortcut.
     *
     * @param event The [MotionEvent] to process.
     * @return `true` if the key event was handled, `false` otherwise.
     */
    fun handleMouseEvent(event: MotionEvent): Boolean {
        if (event.source != InputDevice.SOURCE_MOUSE) {
            return false
        }

        if (event.action == MotionEvent.ACTION_SCROLL) {
            val vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            when {
                vscroll > 0 -> mouseActionHandler.scrollUp()
                vscroll < 0 -> mouseActionHandler.scrollDown()
            }

            val hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            when {
                hscroll > 0 -> mouseActionHandler.scrollRight()
                hscroll < 0 -> mouseActionHandler.scrollLeft()
            }

            return (hscroll != 0f || vscroll != 0f)
        }
        return false
    }
}
