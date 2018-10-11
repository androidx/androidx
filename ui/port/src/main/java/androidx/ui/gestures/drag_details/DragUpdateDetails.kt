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

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.runtimeType

// / Details object for callbacks that use [GestureDragUpdateCallback].
// /
// / See also:
// /
// /  * [DragGestureRecognizer.onUpdate], which uses [GestureDragUpdateCallback].
// /  * [DragDownDetails], the details for [GestureDragDownCallback].
// /  * [DragStartDetails], the details for [GestureDragStartCallback].
// /  * [DragEndDetails], the details for [GestureDragEndCallback].
class DragUpdateDetails(
    // / Recorded timestamp of the source pointer event that triggered the drag
    // / event.
    // /
    // / Could be null if triggered from proxied events such as accessibility.
    val sourceTimeStamp: Duration? = null,
    // / The amount the pointer has moved since the previous update.
    // /
    // / If the [GestureDragUpdateCallback] is for a one-dimensional drag (e.g.,
    // / a horizontal or vertical drag), then this offset contains only the delta
    // / in that direction (i.e., the coordinate in the other direction is zero).
    // /
    // / Defaults to zero if not specified in the constructor.
    val delta: Offset = Offset.zero,
    // / The amount the pointer has moved along the primary axis since the previous
    // / update.
    // /
    // / If the [GestureDragUpdateCallback] is for a one-dimensional drag (e.g.,
    // / a horizontal or vertical drag), then this value contains the component of
    // / [delta] along the primary axis (e.g., horizontal or vertical,
    // / respectively). Otherwise, if the [GestureDragUpdateCallback] is for a
    // / two-dimensional drag (e.g., a pan), then this value is null.
    // /
    // / Defaults to null if not specified in the constructor.
    // /
    // / If non-null, then its value must match one of the coordinates of [delta] and the other
    // / coordinate must be zero.
    val primaryDelta: Double? = null,
    // / The pointer's global position when it triggered this update.
    val globalPosition: Offset
) {
    init {
        assert(
            primaryDelta == null
                    || (primaryDelta == delta.dx && delta.dy == 0.0)
                    || (primaryDelta == delta.dy && delta.dx == 0.0)
        )
    }

    override fun toString() = "${runtimeType()}(${delta})"
}

// / Signature for when a pointer that is in contact with the screen and moving
// / has moved again.
// /
// / The `details` object provides the position of the touch and the distance it
// / has travelled since the last update.
// /
// / See [DragGestureRecognizer.onUpdate].
typealias GestureDragUpdateCallback = (details: DragUpdateDetails) -> Unit