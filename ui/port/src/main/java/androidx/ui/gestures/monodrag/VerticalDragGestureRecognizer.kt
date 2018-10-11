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

package androidx.ui.gestures.monodrag

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.kMinFlingVelocity
import androidx.ui.gestures.kTouchSlop
import androidx.ui.gestures.velocity_tracker.VelocityEstimate
import kotlin.math.absoluteValue

// / Recognizes movement in the vertical direction.
// /
// / Used for vertical scrolling.
// /
// / See also:
// /
// /  * [HorizontalDragGestureRecognizer], for a similar recognizer but for
// /    horizontal movement.
// /  * [MultiDragGestureRecognizer], for a family of gesture recognizers that
// /    track each touch point independently.
// TODO(Migration/shepshapard): Needs tests, which rely on some Mixin stuff.
class VerticalDragGestureRecognizer(debugOwner: Any? = null) : DragGestureRecognizer(debugOwner) {

    override fun _isFlingGesture(estimate: VelocityEstimate): Boolean {
        val minVelocity = minFlingVelocity ?: kMinFlingVelocity
        val minDistance = minFlingDistance ?: kTouchSlop
        return estimate.pixelsPerSecond.dy.absoluteValue > minVelocity &&
                estimate.offset.dy.absoluteValue > minDistance
    }

    override fun _hasSufficientPendingDragDeltaToAccept() =
        (_pendingDragOffset!!.dy.absoluteValue) > kTouchSlop

    override fun _getDeltaForDetails(delta: Offset) = Offset(0.0, delta.dy)

    override fun _getPrimaryValueFromOffset(value: Offset) = value.dy

    override val debugDescription = "vertical drag"
}