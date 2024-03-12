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

package androidx.compose.ui.awt

import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

internal interface AwtEventFilter {
    fun shouldSendMouseEvent(event: MouseEvent): Boolean = true
    fun shouldSendKeyEvent(event: KeyEvent): Boolean = true

    companion object {
        val Empty = object : AwtEventFilter {
        }
    }
}

internal class AwtEventFilters(
    private vararg val filters: AwtEventFilter
) : AwtEventFilter {
    override fun shouldSendMouseEvent(event: MouseEvent): Boolean {
        return filters.all { it.shouldSendMouseEvent(event) }
    }

    override fun shouldSendKeyEvent(event: KeyEvent): Boolean {
        return filters.all { it.shouldSendKeyEvent(event) }
    }
}

/**
 * Filter out mouse events that report the primary button has changed state to pressed,
 * but aren't themselves a mouse press event. This is needed because on macOS, AWT sends
 * us spurious enter/exit events that report the primary button as pressed when resizing
 * the window by its corner/edge. This causes false-positives in detectTapGestures.
 * See https://github.com/JetBrains/compose-multiplatform/issues/2850 for more details.
*/
internal object OnlyValidPrimaryMouseButtonFilter : AwtEventFilter {
    private var isPrimaryButtonPressed = false

    override fun shouldSendMouseEvent(event: MouseEvent): Boolean {
        val eventReportsPrimaryButtonPressed =
            (event.modifiersEx and MouseEvent.BUTTON1_DOWN_MASK) != 0
        if ((event.button == MouseEvent.BUTTON1) &&
            ((event.id == MouseEvent.MOUSE_PRESSED) ||
                (event.id == MouseEvent.MOUSE_RELEASED))) {
            isPrimaryButtonPressed = eventReportsPrimaryButtonPressed  // Update state
        }
        if (eventReportsPrimaryButtonPressed && !isPrimaryButtonPressed) {
            return false  // Ignore such events
        }

        return true
    }
}
