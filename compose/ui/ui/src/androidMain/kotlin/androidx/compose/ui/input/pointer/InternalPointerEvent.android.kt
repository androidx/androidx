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
import androidx.collection.LongSparseArray
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.util.fastFirstOrNull

internal actual class InternalPointerEvent
actual constructor(
    actual val changes: LongSparseArray<PointerInputChange>,
    val pointerInputEvent: PointerInputEvent,
) {
    val motionEvent: MotionEvent?
        get() = pointerInputEvent.motionEvent

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    actual fun activeHoverEvent(pointerId: PointerId): Boolean =
        (pointerInputEvent.pointers.fastFirstOrNull { it.id == pointerId }?.activeHover ?: false) ||
            // During a trackpad pan gesture, the fake finger touch stream pointer has activeHover =
            // false (since it is processed as a touch down/move event). However, for Compose's
            // hover tracking, we want to treat this pan pointer as actively hovering so that
            // hover/exit layout logic still works under the stationary cursor position.
            (ComposeUiFlags.isTrackpadPanHoverFixEnabled &&
                activeGesture == PointerClassification.Pan)

    actual var suppressMovementConsumption: Boolean = false

    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    actual val activeGesture: PointerClassification
        get() {
            if (pointerInputEvent.activeGesture != PointerClassification.None) {
                return pointerInputEvent.activeGesture
            }
            val event = motionEvent
            if (event == null || android.os.Build.VERSION.SDK_INT < 29) {
                return PointerClassification.None
            }
            return when (event.classification) {
                MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE -> {
                    if (
                        android.os.Build.VERSION.SDK_INT >= 34 &&
                            ComposeUiFlags.isTrackpadPanHoverFixEnabled
                    ) {
                        PointerClassification.Pan
                    } else {
                        PointerClassification.None
                    }
                }
                MotionEvent.CLASSIFICATION_PINCH -> {
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        PointerClassification.Pinch
                    } else {
                        PointerClassification.None
                    }
                }
                MotionEvent.CLASSIFICATION_AMBIGUOUS_GESTURE -> PointerClassification.Ambiguous
                MotionEvent.CLASSIFICATION_DEEP_PRESS -> PointerClassification.DeepPress
                else -> PointerClassification.None
            }
        }

    actual val isGestureStart: Boolean
        get() {
            val event = motionEvent
            return event != null &&
                activeGesture != PointerClassification.None &&
                event.actionMasked == MotionEvent.ACTION_DOWN
        }

    actual val isGestureEnd: Boolean
        get() {
            val event = motionEvent
            return event != null &&
                activeGesture != PointerClassification.None &&
                event.actionMasked == MotionEvent.ACTION_UP
        }
}
