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
import androidx.ui.gestures.kPanSlop
import androidx.ui.gestures.kTouchSlop
import androidx.ui.gestures.velocity_tracker.VelocityEstimate

// / Recognizes movement both horizontally and vertically.
// /
// / See also:
// /
// /  * [ImmediateMultiDragGestureRecognizer], for a similar recognizer that
// /    tracks each touch point independently.
// /  * [DelayedMultiDragGestureRecognizer], for a similar recognizer that
// /    tracks each touch point independently, but that doesn't start until
// /    some time has passed.
// TODO(Migration/shepshapard): Needs tests, which rely on some Mixin stuff.
class PanGestureRecognizer(debugOwner: Any?) : DragGestureRecognizer(debugOwner = debugOwner) {

    override fun _isFlingGesture(estimate: VelocityEstimate): Boolean {
        val minVelocity = minFlingVelocity ?: kMinFlingVelocity
        val minDistance = minFlingDistance ?: kTouchSlop
        return estimate.pixelsPerSecond.getDistanceSquared() > minVelocity * minVelocity &&
                estimate.offset.getDistanceSquared() > minDistance * minDistance
    }

    override fun _hasSufficientPendingDragDeltaToAccept() =
        _pendingDragOffset!!.getDistance() > kPanSlop

    override fun _getDeltaForDetails(delta: Offset) = delta

    override fun _getPrimaryValueFromOffset(value: Offset) = null

    override val debugDescription = "pan"
}