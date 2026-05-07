/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test.util

import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.test.InputEventWithAdditionalInformation

internal class InputEventRecorder {

    private val _events = mutableListOf<InputEvent>()
    val events
        get() = _events as List<InputEvent>

    fun disposeEvents() {
        _events.removeAll {
            if (it is MotionEvent) it.recycle()
            true
        }
    }

    /**
     * [InputEvent] recorder which can record events of type [MotionEvent] and [KeyEvent]. Note: By
     * default this doesn't save the extra device information passed to it. This is used for
     * indirect pointer events (where you can not set all device information on a MotionEvent in a
     * testing scenario). The touch and other tests don't need that. If they do in the future, you
     * can change the lists in InputEventRecorder to use [InputEventWithAdditionalInformation]
     * instead, but you will need to refactor all tests using it to call down to the contained
     * [InputEvent].
     */
    fun recordEvent(eventWithDeviceInfo: InputEventWithAdditionalInformation) {
        when (val event = eventWithDeviceInfo.inputEvent) {
            is KeyEvent -> _events.add(KeyEvent(event))
            is MotionEvent -> _events.add(MotionEvent.obtain(event))
            else ->
                throw IllegalArgumentException(
                    "Given InputEvent must be a MotionEvent or KeyEvent" +
                        " not ${event::class.simpleName}"
                )
        }
    }
}
