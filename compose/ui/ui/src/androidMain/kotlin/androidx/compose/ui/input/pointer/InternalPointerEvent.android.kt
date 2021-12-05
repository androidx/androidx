/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import android.view.MotionEvent
import androidx.compose.ui.util.fastFirstOrNull

internal actual class InternalPointerEvent actual constructor(
    actual val changes: Map<PointerId, PointerInputChange>,
    val pointerInputEvent: PointerInputEvent
) {
    val motionEvent: MotionEvent
        get() = pointerInputEvent.motionEvent

    actual fun issuesEnterExitEvent(pointerId: PointerId): Boolean =
        pointerInputEvent.pointers.fastFirstOrNull {
            it.id == pointerId
        }?.issuesEnterExit ?: false

    actual var suppressMovementConsumption: Boolean = false
}