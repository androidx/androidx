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

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.viewinterop.InteropViewGroup
import java.awt.event.FocusEvent

internal class InteropFocusSwitcher(
    private val group: InteropViewGroup,
    private val focusManager: FocusManager,
) {
    private val backwardTracker = Tracker {
        val component = group.focusTraversalPolicy.getFirstComponent(group)
        if (component != null) {
            component.requestFocus(FocusEvent.Cause.TRAVERSAL_FORWARD)
        } else {
            moveForward()
        }
    }

    private val forwardTracker = Tracker {
        val component = group.focusTraversalPolicy.getLastComponent(group)
        if (component != null) {
            component.requestFocus(FocusEvent.Cause.TRAVERSAL_BACKWARD)
        } else {
            moveBackward()
        }
    }

    val backwardTrackerModifier: Modifier
        get() = backwardTracker.modifier

    val forwardTrackerModifier: Modifier
        get() = forwardTracker.modifier

    fun moveBackward() {
        backwardTracker.requestFocusWithoutEvent()
        focusManager.moveFocus(FocusDirection.Previous)
    }

    fun moveForward() {
        forwardTracker.requestFocusWithoutEvent()
        focusManager.moveFocus(FocusDirection.Next)
    }

    /**
     * A helper class that can help:
     * - to prevent recursive focus events
     *   (a case when we focus the same element inside `onFocusEvent`)
     * - to prevent triggering `onFocusEvent` while requesting focus somewhere else
     */
    private class Tracker(
        private val onNonRecursiveFocused: () -> Unit
    ) {
        private val requester = FocusRequester()

        private var isRequestingFocus = false
        private var isHandlingFocus = false

        fun requestFocusWithoutEvent() {
            try {
                isRequestingFocus = true
                requester.requestFocus()
            } finally {
                isRequestingFocus = false
            }
        }

        val modifier = Modifier
            .focusRequester(requester)
            .onFocusEvent {
                if (!isRequestingFocus && !isHandlingFocus && it.isFocused) {
                    try {
                        isHandlingFocus = true
                        onNonRecursiveFocused()
                    } finally {
                        isHandlingFocus = false
                    }
                }
            }
            .focusTarget()
    }
}