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

package androidx.compose.foundation.text.selection

import android.view.InputDevice
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastAll

internal actual fun PointerEvent.isMouseOrTouchPad(): Boolean {
    // There isn't a reliable way to check if the event is from a touchpad device.
    // On Android, touchpad events are disguised as MotionEvent.TOOL_TYPE_FINGER
    // and InputDevice.SOURCE_MOUSE events. However, its source is not reported as
    // InputDevice.SOURCE_TOUCHPAD in most of the cases.
    // The check here is an implementation detail, but NOT a well established behavior.
    // And the Android platform might change this behavior later.
    return this.changes.fastAll { it.type == PointerType.Mouse } ||
        this.motionEvent?.isFromSource(InputDevice.SOURCE_MOUSE) == true ||
        this.motionEvent?.isFromSource(InputDevice.SOURCE_TOUCHPAD) == true
}
