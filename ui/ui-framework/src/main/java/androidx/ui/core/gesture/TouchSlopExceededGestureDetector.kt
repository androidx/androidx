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
import androidx.compose.remember
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Direction
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerId
import androidx.ui.core.PointerInputChange
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.core.positionChange
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.Px

/**
 * This gesture filter detects when at least one pointer has moved far enough to exceed touch slop.
 *
 * The value of touch slop is currently defined internally as the constant [TouchSlop].
 *
 * @param onTouchSlopExceeded Lamba that will be called when touch slop by at least 1 pointer has
 * been exceeded in a supported direction. See [canDrag].
 * @param canDrag Set to limit the directions under which touch slop can be exceeded. Return true
 * if you want a drag to be started due to the touch slop being surpassed in the given [Direction].
 * If [canDrag] is not provided, touch slop will be able to be exceeded in all directions.
 */
@Composable
fun TouchSlopExceededGestureDetector(
    onTouchSlopExceeded: () -> Unit,
    canDrag: ((Direction) -> Boolean)? = null
): Modifier {
    val touchSlop = with(DensityAmbient.current) { TouchSlop.toPx() }
    val recognizer = remember { TouchSlopExceededGestureRecognizer(touchSlop) }
    recognizer.canDrag = canDrag
    recognizer.onTouchSlopExceeded = onTouchSlopExceeded
    return PointerInputModifier(recognizer)
}

// TODO(shepshapard): Shouldn't touchSlop be Px and not IntPx? What if the density bucket of the
//  device is not a whole number?
internal class TouchSlopExceededGestureRecognizer(
    private val touchSlop: Px
) : PointerInputFilter() {
    private val pointerTrackers: MutableMap<PointerId, PointerTrackingData> = mutableMapOf()
    private var passedSlop = false

    var canDrag: ((Direction) -> Boolean)? = null
    var onTouchSlopExceeded: () -> Unit = {}

    override val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass, _: IntPxSize ->
            if (pass == PointerEventPass.PostUp) {
                changes.forEach {
                    if (it.changedToUpIgnoreConsumed()) {
                        pointerTrackers.remove(it.id)
                    } else if (it.changedToDownIgnoreConsumed()) {
                        pointerTrackers[it.id] = PointerTrackingData()
                    }
                }

                if (!passedSlop) {
                    pointerTrackers.forEach {
                        it.value.dxForPass = 0f
                        it.value.dyForPass = 0f
                    }
                }
            }

            if (!passedSlop &&
                (pass == PointerEventPass.PostUp || pass == PointerEventPass.PostDown)
            ) {

                changes.filter { it.current.down && !it.changedToDownIgnoreConsumed() }.forEach {

                    if (!passedSlop) {

                        // TODO(shepshapard): handle the case that the pointerTrackingData is null,
                        //  either with an exception or a logged error, or something else. It should
                        //  only ever be able to be null at this point if we received a "move"
                        //  change for a pointer before we received an change that the pointer
                        //  became "down".
                        val pointerTracker: PointerTrackingData = pointerTrackers[it.id]!!

                        val positionChanged = it.positionChange()
                        val dx = positionChanged.x.value
                        val dy = positionChanged.y.value

                        // TODO(shepshapard): I believe the logic in this block could be simplified
                        //   to be much more clear.  Will need to revisit. The need to make
                        //   improvements may be rendered obsolete with upcoming changes however.

                        val directionX = when {
                            dx == 0f -> null
                            dx < 0f -> Direction.LEFT
                            else -> Direction.RIGHT
                        }
                        val directionY = when {
                            dy == 0f -> null
                            dy < 0f -> Direction.UP
                            else -> Direction.DOWN
                        }

                        val internalCanDrag = canDrag

                        val canDragX =
                            directionX != null &&
                                    (internalCanDrag == null || internalCanDrag.invoke(directionX))
                        val canDragY =
                            directionY != null &&
                                    (internalCanDrag == null || internalCanDrag.invoke(directionY))

                        if (pass == PointerEventPass.PostUp) {
                            pointerTracker.dxForPass = dx
                            pointerTracker.dyForPass = dy
                            pointerTracker.dxUnderSlop += dx
                            pointerTracker.dyUnderSlop += dy
                        } else {
                            pointerTracker.dxUnderSlop += dx - pointerTracker.dxForPass
                            pointerTracker.dyUnderSlop += dy - pointerTracker.dyForPass
                        }

                        val passedSlopX =
                            canDragX && Math.abs(pointerTracker.dxUnderSlop) > touchSlop.value
                        val passedSlopY =
                            canDragY && Math.abs(pointerTracker.dyUnderSlop) > touchSlop.value

                        if (passedSlopX || passedSlopY) {
                            passedSlop = true
                            onTouchSlopExceeded.invoke()
                        } else {
                            if (!canDragX &&
                                ((directionX == Direction.LEFT &&
                                        pointerTracker.dxUnderSlop < 0) ||
                                        (directionX == Direction.RIGHT &&
                                                pointerTracker.dxUnderSlop > 0))
                            ) {
                                pointerTracker.dxUnderSlop = 0f
                            }
                            if (!canDragY &&
                                ((directionY == Direction.LEFT &&
                                        pointerTracker.dyUnderSlop < 0) ||
                                        (directionY == Direction.DOWN &&
                                                pointerTracker.dyUnderSlop > 0))
                            ) {
                                pointerTracker.dyUnderSlop = 0f
                            }
                        }
                    }
                }
            }

            if (passedSlop &&
                pass == PointerEventPass.PostDown &&
                changes.all { it.changedToUpIgnoreConsumed() }
            ) {
                passedSlop = false
            }
            changes
        }

    override val cancelHandler = {
        pointerTrackers.clear()
        passedSlop = false
    }

    internal data class PointerTrackingData(
        var dxUnderSlop: Float = 0f,
        var dyUnderSlop: Float = 0f,
        var dxForPass: Float = 0f,
        var dyForPass: Float = 0f
    )
}