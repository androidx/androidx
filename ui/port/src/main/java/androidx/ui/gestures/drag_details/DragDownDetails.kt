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

import androidx.ui.engine.geometry.Offset
import androidx.ui.runtimeType

/**
 * Details object for callbacks that use [GestureDragDownCallback].
 *
 * See also:
 *
 *  * [DragGestureRecognizer.onDown], which uses [GestureDragDownCallback].
 *  * [DragStartDetails], the details for [GestureDragStartCallback].
 *  * [DragUpdateDetails], the details for [GestureDragUpdateCallback].
 *  * [DragEndDetails], the details for [GestureDragEndCallback].
 */
class DragDownDetails(
    /**
     * The global position at which the pointer contacted the screen.
     *
     * Defaults to the origin if not specified in the constructor.
     */
    val globalPosition: Offset = Offset.zero
) {
    override fun toString(): String = "${runtimeType()}(${globalPosition})"
}

/**
 * Signature for when a pointer has contacted the screen and might begin to
 * move.
 *
 * The `details` object provides the position of the touch.
 *
 * See [DragGestureRecognizer.onDown].
 */
typealias GestureDragDownCallback = (DragDownDetails) -> Unit