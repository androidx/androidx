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

package androidx.ui.gestures.multidrag

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.kTouchSlop

/**
 * Recognizes movement both horizontally and vertically on a per-pointer basis.
 *
 * In contrast to [PanGestureRecognizer], [ImmediateMultiDragGestureRecognizer]
 * watches each pointer separately, which means multiple drags can be
 * recognized concurrently if multiple pointers are in contact with the screen.
 *
 * See also:
 *
 *  * [PanGestureRecognizer], which recognizes only one drag gesture at a time,
 *    regardless of how many fingers are involved.
 *  * [HorizontalMultiDragGestureRecognizer], which only recognizes drags that
 *    start horizontally.
 *  * [VerticalMultiDragGestureRecognizer], which only recognizes drags that
 *    start vertically.
 *  * [DelayedMultiDragGestureRecognizer], which only recognizes drags that
 *    start after a long-press gesture.
 */
class ImmediateMultiDragGestureRecognizer(debugOwner: Any?) :
    MultiDragGestureRecognizer<MultiDragPointerState>(debugOwner) {

    override fun createNewPointerState(event: PointerDownEvent): MultiDragPointerState {
        return ImmediatePointerState(event.position)
    }

    override val debugDescription = "multidrag"
}

private class ImmediatePointerState(initialPosition: Offset) :
    MultiDragPointerState(initialPosition) {

    override fun checkForResolutionAfterMove() {
        assert(pendingDelta != null)
        if (pendingDelta!!.getDistance() > kTouchSlop)
            resolve(GestureDisposition.accepted)
    }

    override fun accepted(starter: GestureMultiDragStartCallback) {
        starter(initialPosition)
    }
}