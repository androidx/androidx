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

/**
 * Details object for callbacks that use [GestureDragStartCallback].
 *
 * See also:
 *
 *  * [DragGestureRecognizer.onStart], which uses [GestureDragStartCallback].
 *  * [DragDownDetails], the details for [GestureDragDownCallback].
 *  * [DragUpdateDetails], the details for [GestureDragUpdateCallback].
 *  * [DragEndDetails], the details for [GestureDragEndCallback].
 */
open class DragStartDetails(
    /**
     * Recorded timestamp of the source pointer event that triggered the drag
     * event.
     *
     * Could be null if triggered from proxied events such as accessibility.
     */
    val sourceTimeStamp: Duration? = null,
    /**
     * The global position at which the pointer contacted the screen.
     *
     * Defaults to the origin if not specified in the constructor.
     */
    val globalPosition: Offset = Offset.zero
) {

    // TODO(ianh): Expose the current position, so that you can have a no-jump
    // drag even when disambiguating (though of course it would lag the finger
    // instead).

    override fun toString(): String = "${runtimeType()}(${globalPosition})"
}

/**
 * Signature for when a pointer has contacted the screen and has begun to move.
 *
 * The `details` object provides the position of the touch when it first
 * touched the surface.
 *
 * See [DragGestureRecognizer.onStart].
 */
typealias GestureDragStartCallback = (details: DragStartDetails) -> Unit