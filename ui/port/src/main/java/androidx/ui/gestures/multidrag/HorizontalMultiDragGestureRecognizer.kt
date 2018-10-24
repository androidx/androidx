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
import kotlin.math.absoluteValue

/**
 * Recognizes movement in the horizontal direction on a per-pointer basis.
 *
 * In contrast to [HorizontalDragGestureRecognizer],
 * [HorizontalMultiDragGestureRecognizer] watches each pointer separately,
 * which means multiple drags can be recognized concurrently if multiple
 * pointers are in contact with the screen.
 *
 * See also:
 *
 *  * [HorizontalDragGestureRecognizer], a gesture recognizer that just
 *    looks at horizontal movement.
 *  * [ImmediateMultiDragGestureRecognizer], a similar recognizer, but without
 *    the limitation that the drag must start horizontally.
 *  * [VerticalMultiDragGestureRecognizer], which only recognizes drags that
 *    start vertically.
 */
class HorizontalMultiDragGestureRecognizer(debugOwner: Any) :
    MultiDragGestureRecognizer<MultiDragPointerState>(debugOwner) {

    override fun createNewPointerState(event: PointerDownEvent): MultiDragPointerState {
        return HorizontalPointerState(event.position)
    }

    override val debugDescription = "horizontal multidrag"
}

private class HorizontalPointerState(initialPosition: Offset) :
    MultiDragPointerState(initialPosition) {

    override fun checkForResolutionAfterMove() {
        assert(pendingDelta != null)
        if (pendingDelta!!.dx.absoluteValue > kTouchSlop)
            resolve(GestureDisposition.accepted)
    }

    override fun accepted(starter: GestureMultiDragStartCallback) {
        starter(initialPosition)
    }
}