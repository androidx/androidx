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

import androidx.compose.remember
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Direction
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.composed
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.positionChange
import androidx.ui.geometry.Offset
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition

/**
 * This gesture filter detects when the average distance change of all pointers surpasses touch
 * slop.
 *
 * The value of touch slop is currently defined internally as the constant [TouchSlop].
 *
 * @param onDragSlopExceeded Called when touch slop is exceeded in a supported direction. See
 * [canDrag].
 * @param canDrag Set to limit the directions under which touch slop can be exceeded. Return true
 * if you want a drag to be started due to the touch slop being surpassed in the given [Direction].
 * If [canDrag] is not provided, touch slop will be able to be exceeded in all directions.
 */
fun Modifier.dragSlopExceededGestureFilter(
    onDragSlopExceeded: () -> Unit,
    canDrag: ((Direction) -> Boolean)? = null
): Modifier = composed {
    val touchSlop = with(DensityAmbient.current) { TouchSlop.toPx() }
    val filter = remember { DragSlopExceededGestureFilter(touchSlop) }
    filter.canDrag = canDrag
    filter.onDragSlopExceeded = onDragSlopExceeded
    PointerInputModifierImpl(filter)
}

internal class DragSlopExceededGestureFilter(
    private val touchSlop: Float
) : PointerInputFilter() {
    private var dxForPass = 0f
    private var dyForPass = 0f
    private var dxUnderSlop = 0f
    private var dyUnderSlop = 0f
    private var passedSlop = false

    var canDrag: ((Direction) -> Boolean)? = null
    var onDragSlopExceeded: () -> Unit = {}

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {

        if (!passedSlop &&
            (pass == PointerEventPass.PostUp || pass == PointerEventPass.PostDown)
        ) {
            // Get current average change.
            val averagePositionChange = getAveragePositionChange(changes)
            val dx = averagePositionChange.dx
            val dy = averagePositionChange.dy

            // Track changes during postUp and during postDown.  This allows for fancy dragging
            // due to a parent being dragged and will likely be removed.
            // TODO(b/157087973): Likely remove this two pass complexity.
            if (pass == PointerEventPass.PostUp) {
                dxForPass = dx
                dyForPass = dy
                dxUnderSlop += dx
                dyUnderSlop += dy
            } else {
                dxUnderSlop += dx - dxForPass
                dyUnderSlop += dy - dyForPass
            }

            // Map the distance to the direction enum for a call to canDrag.
            val directionX = averagePositionChange.horizontalDirection()
            val directionY = averagePositionChange.verticalDirection()

            val canDragX = directionX != null && canDrag?.invoke(directionX) ?: true
            val canDragY = directionY != null && canDrag?.invoke(directionY) ?: true

            val passedSlopX = canDragX && Math.abs(dxUnderSlop) > touchSlop
            val passedSlopY = canDragY && Math.abs(dyUnderSlop) > touchSlop

            if (passedSlopX || passedSlopY) {
                passedSlop = true
                onDragSlopExceeded.invoke()
            } else {
                // If we have passed slop in a direction that we can't drag in, we should reset
                // our tracking back to zero so that a user doesn't have to later scroll the slop
                // + the extra distance they scrolled in the wrong direction.
                if (!canDragX &&
                    ((directionX == Direction.LEFT && dxUnderSlop < 0) ||
                            (directionX == Direction.RIGHT && dxUnderSlop > 0))
                ) {
                    dxUnderSlop = 0f
                }
                if (!canDragY &&
                    ((directionY == Direction.UP && dyUnderSlop < 0) ||
                            (directionY == Direction.DOWN && dyUnderSlop > 0))
                ) {
                    dyUnderSlop = 0f
                }
            }
        }

        if (pass == PointerEventPass.PostDown &&
            changes.all { it.changedToUpIgnoreConsumed() }
        ) {
            reset()
        }
        return changes
    }

    override fun onCancel() {
        reset()
    }

    private fun reset() {
        passedSlop = false
        dxForPass = 0f
        dyForPass = 0f
        dxUnderSlop = 0f
        dyUnderSlop = 0f
    }
}

/**
 * Get's the average distance change of all pointers as an Offset.
 */
private fun getAveragePositionChange(changes: List<PointerInputChange>): Offset {
    val sum = changes.fold(PxPosition.Origin) { sum, change ->
        sum + change.positionChange()
    }
    val sizeAsFloat = changes.size.toFloat()
    // TODO(b/148980115): Once PxPosition is removed, sum will be an Offset, and this line can
    //  just be straight division.
    return Offset(sum.x / sizeAsFloat, sum.y / sizeAsFloat)
}

/**
 * Maps an [Offset] value to a horizontal [Direction].
 */
private fun Offset.horizontalDirection() =
    when {
        this.dx < 0f -> Direction.LEFT
        this.dx > 0f -> Direction.RIGHT
        else -> null
    }

/**
 * Maps a [Offset] value to a vertical [Direction].
 */
private fun Offset.verticalDirection() =
    when {
        this.dy < 0f -> Direction.UP
        this.dy > 0f -> Direction.DOWN
        else -> null
    }