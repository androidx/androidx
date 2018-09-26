/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.long_press

import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.kLongPressTimeout
import androidx.ui.gestures.recognizer.PrimaryPointerGestureRecognizer

// / Signature for when a pointer has remained in contact with the screen at the
// / same location for a long period of time.
typealias GestureLongPressCallback = () -> Unit

// / Recognizes when the user has pressed down at the same location for a long
// / period of time.
// / Consider assigning the [onLongPress] callback after creating this object.
// TODO(Migration/shepshapard): Needs tests, which rely on some Mixin stuff.
open class LongPressGestureRecognizer(
    debugOwner: Any?
) : PrimaryPointerGestureRecognizer(
    deadline = kLongPressTimeout, debugOwner = debugOwner
) {

    // / Called when a long-press is recognized.
    var onLongPress: GestureLongPressCallback? = null

    override fun didExceedDeadline() {
        this.resolve(GestureDisposition.accepted)
        if (onLongPress != null) {
            invokeCallback("onLongPress", ::onLongPress)
        }
    }

    override fun handlePrimaryPointer(event: PointerEvent) {
        if (event is PointerUpEvent) {
            this.resolve(GestureDisposition.rejected)
        }
    }

    override val debugDescription: String
        get() = "long press"
}