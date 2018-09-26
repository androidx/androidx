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

package androidx.ui.gestures.drag_details

import androidx.ui.gestures.velocity_tracker.Velocity
import androidx.ui.runtimeType

// / Details object for callbacks that use [GestureDragEndCallback].
// /
// / See also:
// /
// /  * [DragGestureRecognizer.onEnd], which uses [GestureDragEndCallback].
// /  * [DragDownDetails], the details for [GestureDragDownCallback].
// /  * [DragStartDetails], the details for [GestureDragStartCallback].
// /  * [DragUpdateDetails], the details for [GestureDragUpdateCallback].
class DragEndDetails(
    // / The velocity the pointer was moving when it stopped contacting the screen.
    // /
    // / Defaults to zero if not specified in the constructor.
    val velocity: Velocity = Velocity.zero,
    // / The velocity the pointer was moving along the primary axis when it stopped
    // / contacting the screen, in logical pixels per second.
    // /
    // / If the [GestureDragEndCallback] is for a one-dimensional drag (e.g., a
    // / horizontal or vertical drag), then this value contains the component of
    // / [velocity] along the primary axis (e.g., horizontal or vertical,
    // / respectively). Otherwise, if the [GestureDragEndCallback] is for a
    // / two-dimensional drag (e.g., a pan), then this value is null.
    // /
    // / Defaults to null if not specified in the constructor.
    val primaryVelocity: Double? = null
) {
    init {
        assert(
            primaryVelocity == null
                    || primaryVelocity == velocity.pixelsPerSecond.dx
                    || primaryVelocity == velocity.pixelsPerSecond.dy
        )
    }

    override fun toString() = "${runtimeType()}(${velocity})"
}