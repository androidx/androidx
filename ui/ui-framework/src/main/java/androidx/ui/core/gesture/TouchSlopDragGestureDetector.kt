/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.gesture

import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Direction
import androidx.ui.core.PxPosition

// TODO(shepshapard): Convert to functional component with effects once effects are ready.
/**
 * This gesture detector detects dragging in any direction.
 *
 * Dragging begins when the touch slop distance (currently defined by [TouchSlop]) is
 * surpassed in a supported direction (see [DragObserver.onDrag]).  When dragging begins,
 * [DragObserver.onStart] is called, followed immediately by a call to [DragObserver.onDrag].
 * [DragObserver.onDrag] is then continuously called whenever pointers have moved.
 * [DragObserver.onStop] is called when the dragging ends due to all of the pointers no longer
 * interacting with the DragGestureDetector (for example, the last finger has been lifted off
 * of the DragGestureDetector).
 *
 * When multiple pointers are touching the detector, the drag distance is taken as the average of
 * all of the pointers.
 *
 * @param canDrag Set to limit the directions under which touch slop can be exceeded. Return true
 * if you want a drag to be started due to the touch slop being surpassed in the given [Direction].
 * If [canDrag] is not provided, touch slop will be able to be exceeded in all directions.
 * @param dragObserver The callback interface to report all events related to dragging.
 */
@Composable
fun TouchSlopDragGestureDetector(
    dragObserver: DragObserver,
    canDrag: ((Direction) -> Boolean)? = null,
    children: @Composable() () -> Unit
) {
    val glue = +memo { TouchSlopDragGestureDetectorGlue() }
    glue.touchSlopDragObserver = dragObserver

    RawDragGestureDetector(glue.rawDragObserver, glue::dragEnabled) {
        TouchSlopExceededGestureDetector(glue::enableDrag, canDrag, children)
    }
}

/**
 * Glues together the logic of RawDragGestureDetector and TouchSlopExceededGestureDetector.
 */
private class TouchSlopDragGestureDetectorGlue {

    lateinit var touchSlopDragObserver: DragObserver
    var dragEnabled = false

    fun enableDrag() {
        dragEnabled = true
    }

    val rawDragObserver: DragObserver =
        object : DragObserver {
            override fun onStart(downPosition: PxPosition) {
                touchSlopDragObserver.onStart(downPosition)
            }

            override fun onDrag(dragDistance: PxPosition): PxPosition {
                return touchSlopDragObserver.onDrag(dragDistance)
            }

            override fun onStop(velocity: PxPosition) {
                touchSlopDragObserver.onStop(velocity)
                dragEnabled = false
            }
        }
}