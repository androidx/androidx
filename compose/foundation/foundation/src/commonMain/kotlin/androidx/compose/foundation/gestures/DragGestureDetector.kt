/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.gestures

// Note, that there is a copy-paste version of this file (DragGestureDetectorCopy.kt), don't
// forget to change it too.
//
// We can't make *PointerSlop* functions public just yet because the new pointer API isn't ready.

// TODO(b/193549931): when the new pointer API will be ready we should make *PointerSlop*
//  functions public

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChangedIgnoreConsumed
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.coroutines.CancellationException

/**
 * Waits for drag motion to pass [touch slop][ViewConfiguration.touchSlop], using [pointerId] as the
 * pointer to examine. If [pointerId] is raised, another pointer from those that are down will be
 * chosen to lead the gesture, and if none are down, `null` is returned. If [pointerId] is not down
 * when [awaitTouchSlopOrCancellation] is called, then `null` is returned.
 *
 * [onTouchSlopReached] is called after [ViewConfiguration.touchSlop] motion in the any direction
 * with the change that caused the motion beyond touch slop and the [Offset] beyond touch slop that
 * has passed. [onTouchSlopReached] should consume the position change if it accepts the motion. If
 * it does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue.
 *
 * @return The [PointerInputChange] that was consumed in [onTouchSlopReached] or `null` if all
 *   pointers are raised before touch slop is detected or another gesture consumed the position
 *   change.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitDragOrCancellationSample
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitVerticalTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Offset) -> Unit,
): PointerInputChange? {
    return awaitPointerSlopOrCancellation(
        pointerId,
        PointerType.Touch,
        onPointerSlopReached = onTouchSlopReached,
        orientation = null,
    )
}

/**
 * Reads position change events for [pointerId] and calls [onDrag] for every change in position. If
 * [pointerId] is raised, a new pointer is chosen from those that are down and if none exist, the
 * method returns. This does not wait for touch slop.
 *
 * @return `true` if the drag completed normally or `false` if the drag motion was canceled by
 *   another gesture detector consuming position change events.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.DragSample
 * @see awaitTouchSlopOrCancellation
 * @see awaitDragOrCancellation
 * @see horizontalDrag
 * @see verticalDrag
 */
suspend fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): Boolean {
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrCancellation(pointer) ?: return false

        if (change.changedToUpIgnoreConsumed()) {
            return true
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Reads pointer input events until a drag is detected or all pointers are up. When the final
 * pointer is raised, the up event is returned. When a drag event is detected, the drag change will
 * be returned. Note that if [pointerId] has been raised, another pointer that is down will be used,
 * if available, so the returned [PointerInputChange.id] may differ from [pointerId]. If the
 * position change in the any direction has been consumed by the [PointerEventPass.Main] pass, then
 * the drag is considered canceled and `null` is returned. If [pointerId] is not down when
 * [awaitDragOrCancellation] is called, then `null` is returned.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitDragOrCancellationSample
 * @see awaitVerticalDragOrCancellation
 * @see awaitHorizontalDragOrCancellation
 * @see drag
 */
suspend fun AwaitPointerEventScope.awaitDragOrCancellation(
    pointerId: PointerId
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val change = awaitDragOrUp(pointerId) { it.positionChangedIgnoreConsumed() }
    return if (change?.isConsumed == false) change else null
}

/**
 * Gesture detector that waits for pointer down and touch slop in any direction and then calls
 * [onDrag] for each drag event. It follows the touch slop detection of
 * [awaitTouchSlopOrCancellation] but will consume the position change automatically once the touch
 * slop has been crossed. @see [detectDragGestures] with orientation lock for a fuller set of
 * capabilities.
 *
 * [onDragStart] called when the touch slop has been passed and includes an [Offset] representing
 * the last known pointer position relative to the containing element. The [Offset] can be outside
 * the actual bounds of the element itself meaning the numbers can be negative or larger than the
 * element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up and [onDragCancel] is called if another gesture
 * has consumed pointer input, canceling this gesture.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.DetectDragGesturesSample
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 * @see detectDragGesturesAfterLongPress to detect gestures after long press
 */
@OptIn(ExperimentalFoundationApi::class)
suspend fun PointerInputScope.detectDragGestures(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) =
    detectDragGestures(
        onDragStart = { _, slopTriggerChange, _ -> onDragStart(slopTriggerChange.position) },
        onDragEnd = { onDragEnd.invoke() },
        onDragCancel = onDragCancel,
        shouldAwaitTouchSlop = { true },
        orientationLock = null,
        onDrag = onDrag,
    )

/**
 * A Gesture detector that waits for pointer down and touch slop in the direction specified by
 * [orientationLock] and then calls [onDrag] for each drag event. It follows the touch slop
 * detection of [awaitTouchSlopOrCancellation] but will consume the position change automatically
 * once the touch slop has been crossed, the amount of drag over the touch slop is reported as the
 * first drag event [onDrag] after the slop is crossed. If [shouldAwaitTouchSlop] returns true the
 * touch slop recognition phase will be ignored and the drag gesture will be recognized
 * immediately.The first [onDrag] in this case will report an [Offset.Zero].
 *
 * [onDragStart] is called when the touch slop has been passed and includes an [Offset] representing
 * the last known pointer position relative to the containing element as well as the initial down
 * event that triggered this gesture detection cycle. The [Offset] can be outside the actual bounds
 * of the element itself meaning the numbers can be negative or larger than the element bounds if
 * the touch target is smaller than the [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up with the event change of the up event and
 * [onDragCancel] is called if another gesture has consumed pointer input, canceling this gesture.
 *
 * @param orientationLock Optionally locks detection to this orientation, this means, when this is
 *   provided, touch slop detection and drag event detection will be conditioned to the given
 *   orientation axis. [onDrag] will still dispatch events on with information in both axis, but if
 *   orientation lock is provided, only events that happen on the given orientation will be
 *   considered. This also means that if no event in the orientation is detected we will not
 *   dispatch [onDrag] calls. If no value is provided (i.e. null) touch slop and drag detection will
 *   happen on an "any" orientation basis, that is, touch slop will be detected if crossed in either
 *   direction and drag events will be dispatched if present in either direction.
 * @param onDragStart A lambda to be called when the drag gesture starts, it contains information
 *   about the last known [PointerInputChange] relative to the containing element and the post slop
 *   delta, slopTriggerChange. It also contains information about the down event where this gesture
 *   started and the overSlopOffset.
 * @param onDragEnd A lambda to be called when the gesture ends. It contains information about the
 *   up [PointerInputChange] that finished the gesture.
 * @param onDragCancel A lambda to be called when the gesture is cancelled either by an error or
 *   when it was consumed.
 * @param shouldAwaitTouchSlop Indicates if touch slop detection should be skipped.
 * @param onDrag A lambda to be called for each delta event in the gesture. It contains information
 *   about the [PointerInputChange] and the movement offset.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.DetectDragGesturesSample
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 * @see detectDragGesturesAfterLongPress to detect gestures after long press
 */
@OptIn(ExperimentalFoundationApi::class)
suspend fun PointerInputScope.detectDragGestures(
    orientationLock: Orientation?,
    onDragStart:
        (
            down: PointerInputChange, slopTriggerChange: PointerInputChange, overSlopOffset: Offset,
        ) -> Unit =
        { _, _, _ ->
        },
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    shouldAwaitTouchSlop: () -> Boolean = { true },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    var overSlop: Offset

    awaitEachGesture {
        val initialDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val awaitTouchSlop = shouldAwaitTouchSlop()

        if (!awaitTouchSlop) {
            initialDown.consume()
        }
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        overSlop = Offset.Zero
        if (awaitTouchSlop) {
            do {
                drag =
                    awaitPointerSlopOrCancellation(
                        down.id,
                        down.type,
                        orientation = orientationLock,
                    ) { change, over ->
                        change.consume()
                        overSlop = over
                    }
            } while (drag != null && !drag.isConsumed)
        } else {
            drag = initialDown
        }

        // if the pointer is still down, keep reading events in case we need to pick up the gesture.
        while (drag == null && currentEvent.changes.fastAny { it.pressed }) {
            var event: PointerEvent
            do {
                // use final pass so we only pick up a gesture if it was really ignored by
                // everyone else
                event = awaitPointerEvent(pass = PointerEventPass.Final)
            } while (
                event.changes.fastAny { it.isConsumed } && event.changes.fastAny { it.pressed }
            )

            // an event was not consumed and there's still a pointer in the screen
            if (event.changes.fastAny { it.pressed }) {
                // await touch slop again, using the initial down as starting point.
                // For most cases this should return immediately since we probably moved
                // far enough from the initial down event.
                val initialPositionChange =
                    (event.changes.firstOrNull()?.position ?: Offset.Zero) - down.position
                drag =
                    awaitPointerSlopOrCancellation(
                        down.id,
                        down.type,
                        orientation = orientationLock,
                        initialPositionChange = initialPositionChange,
                    ) { change, _ ->
                        change.consume()
                        // the triggering event will be used as over slop
                        overSlop = change.positionChange()
                    }
            }
        }

        if (drag != null) {
            onDragStart.invoke(down, drag, overSlop)
            onDrag(drag, overSlop)
            val upEvent =
                drag(
                    pointerId = drag.id,
                    onDrag = {
                        onDrag(it, it.positionChange())
                        it.consume()
                    },
                    // once drag starts we want to capture drags in any direction, though
                    // they will be propagated on the correct direction above we want to
                    // consume any new drag to avoid the cases where we start dragging
                    // on a given direction and then change directions.
                    orientation = null,
                    motionConsumed = { it.isConsumed },
                )
            if (upEvent == null) {
                onDragCancel()
            } else {
                onDragEnd(upEvent)
            }
        }
    }
}

/**
 * Gesture detector that waits for pointer down and long press, after which it calls [onDrag] for
 * each drag event.
 *
 * [onDragStart] called when a long press is detected and includes an [Offset] representing the last
 * known pointer position relative to the containing element. The [Offset] can be outside the actual
 * bounds of the element itself meaning the numbers can be negative or larger than the element
 * bounds if the touch target is smaller than the [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up and [onDragCancel] is called if another gesture
 * has consumed pointer input, canceling this gesture. This function will automatically consume all
 * the position change after the long press.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.DetectDragWithLongPressGesturesSample
 * @see detectVerticalDragGestures
 * @see detectHorizontalDragGestures
 * @see detectDragGestures
 */
suspend fun PointerInputScope.detectDragGesturesAfterLongPress(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        try {
            val down = awaitFirstDown(requireUnconsumed = false)
            val drag = awaitLongPressOrCancellation(down.id)
            if (drag != null) {
                onDragStart.invoke(drag.position)

                if (
                    drag(drag.id) {
                        onDrag(it, it.positionChange())
                        it.consume()
                    }
                ) {
                    // consume up if we quit drag gracefully with the up
                    currentEvent.changes.fastForEach { if (it.changedToUp()) it.consume() }
                    onDragEnd()
                } else {
                    onDragCancel()
                }
            }
        } catch (c: CancellationException) {
            onDragCancel()
            throw c
        }
    }
}

/**
 * Waits for vertical drag motion to pass [touch slop][ViewConfiguration.touchSlop], using
 * [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from those that
 * are down will be chosen to lead the gesture, and if none are down, `null` is returned. If
 * [pointerId] is not down when [awaitVerticalTouchSlopOrCancellation] is called, then `null` is
 * returned.
 *
 * [onTouchSlopReached] is called after [ViewConfiguration.touchSlop] motion in the vertical
 * direction with the change that caused the motion beyond touch slop and the pixels beyond touch
 * slop. [onTouchSlopReached] should consume the position change if it accepts the motion. If it
 * does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue.
 *
 * @return The [PointerInputChange] that was consumed in [onTouchSlopReached] or `null` if all
 *   pointers are raised before touch slop is detected or another gesture consumed the position
 *   change.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitVerticalDragOrCancellationSample
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitVerticalTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit,
) =
    awaitPointerSlopOrCancellation(
        pointerId = pointerId,
        pointerType = PointerType.Touch,
        onPointerSlopReached = { change, overSlop -> onTouchSlopReached(change, overSlop.y) },
        orientation = Orientation.Vertical,
    )

/**
 * Waits for vertical drag motion to pass [pointerType]'s touch slop using [pointerId] as the
 * pointer to examine. If [pointerId] is raised, another pointer from those that are down will be
 * chosen to lead the gesture, and if none are down, `null` is returned. If [pointerId] is not down
 * when [awaitVerticalPointerSlopOrCancellation] is called, then `null` is returned.
 *
 * [onPointerSlopReached] is called after [ViewConfiguration.touchSlop] motion in the vertical
 * direction with the change that caused the motion beyond touch slop and the pixels beyond touch
 * slop. [onPointerSlopReached] should consume the position change if it accepts the motion. If it
 * does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue.
 *
 * @return The [PointerInputChange] that was consumed in [onPointerSlopReached] or `null` if all
 *   pointers are raised before touch slop is detected or another gesture consumed the position
 *   change.
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitVerticalPointerSlopOrCancellation(
    pointerId: PointerId,
    pointerType: PointerType,
    onPointerSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit,
) =
    awaitPointerSlopOrCancellation(
        pointerId = pointerId,
        pointerType = pointerType,
        onPointerSlopReached = { change, overSlop -> onPointerSlopReached(change, overSlop.y) },
        orientation = Orientation.Vertical,
    )

/**
 * Reads vertical position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if none
 * exist, the method returns. This does not wait for touch slop
 *
 * @return `true` if the vertical drag completed normally or `false` if the drag motion was canceled
 *   by another gesture detector consuming position change events.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.VerticalDragSample
 * @see awaitVerticalTouchSlopOrCancellation
 * @see awaitVerticalDragOrCancellation
 * @see horizontalDrag
 * @see drag
 */
suspend fun AwaitPointerEventScope.verticalDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): Boolean =
    drag(
        pointerId = pointerId,
        onDrag = onDrag,
        orientation = Orientation.Vertical,
        motionConsumed = { it.isConsumed },
    ) != null

/**
 * Reads pointer input events until a vertical drag is detected or all pointers are up. When the
 * final pointer is raised, the up event is returned. When a drag event is detected, the drag change
 * will be returned. Note that if [pointerId] has been raised, another pointer that is down will be
 * used, if available, so the returned [PointerInputChange.id] may differ from [pointerId]. If the
 * position change has been consumed by the [PointerEventPass.Main] pass, then the drag is
 * considered canceled and `null` is returned. If [pointerId] is not down when
 * [awaitVerticalDragOrCancellation] is called, then `null` is returned.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitVerticalDragOrCancellationSample
 * @see awaitHorizontalDragOrCancellation
 * @see awaitDragOrCancellation
 * @see verticalDrag
 */
suspend fun AwaitPointerEventScope.awaitVerticalDragOrCancellation(
    pointerId: PointerId
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val change = awaitDragOrUp(pointerId) { it.positionChangeIgnoreConsumed().y != 0f }
    return if (change?.isConsumed == false) change else null
}

/**
 * Gesture detector that waits for pointer down and touch slop in the vertical direction and then
 * calls [onVerticalDrag] for each vertical drag event. It follows the touch slop detection of
 * [awaitVerticalTouchSlopOrCancellation], but will consume the position change automatically once
 * the touch slop has been crossed.
 *
 * [onDragStart] called when the touch slop has been passed and includes an [Offset] representing
 * the last known pointer position relative to the containing element. The [Offset] can be outside
 * the actual bounds of the element itself meaning the numbers can be negative or larger than the
 * element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up and [onDragCancel] is called if another gesture
 * has consumed pointer input, canceling this gesture.
 *
 * This gesture detector will coordinate with [detectHorizontalDragGestures] and
 * [awaitHorizontalTouchSlopOrCancellation] to ensure only vertical or horizontal dragging is
 * locked, but not both.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.DetectVerticalDragGesturesSample
 * @see detectDragGestures
 * @see detectHorizontalDragGestures
 */
suspend fun PointerInputScope.detectVerticalDragGestures(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onVerticalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var overSlop = 0f
        val drag =
            awaitVerticalPointerSlopOrCancellation(down.id, down.type) { change, over ->
                change.consume()
                overSlop = over
            }
        if (drag != null) {
            onDragStart.invoke(drag.position)
            onVerticalDrag.invoke(drag, overSlop)
            if (
                verticalDrag(drag.id) {
                    onVerticalDrag(it, it.positionChange().y)
                    it.consume()
                }
            ) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/**
 * Waits for horizontal drag motion to pass [touch slop][ViewConfiguration.touchSlop], using
 * [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from those that
 * are down will be chosen to lead the gesture, and if none are down, `null` is returned.
 *
 * [onTouchSlopReached] is called after [ViewConfiguration.touchSlop] motion in the horizontal
 * direction with the change that caused the motion beyond touch slop and the pixels beyond touch
 * slop. [onTouchSlopReached] should consume the position change if it accepts the motion. If it
 * does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue. If [pointerId] is not down when [awaitHorizontalTouchSlopOrCancellation] is called,
 * then `null` is returned.
 *
 * @return The [PointerInputChange] that was consumed in [onTouchSlopReached] or `null` if all
 *   pointers are raised before touch slop is detected or another gesture consumed the position
 *   change.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitHorizontalDragOrCancellationSample
 * @see awaitVerticalTouchSlopOrCancellation
 * @see awaitTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitHorizontalTouchSlopOrCancellation(
    pointerId: PointerId,
    onTouchSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit,
) =
    awaitPointerSlopOrCancellation(
        pointerId = pointerId,
        pointerType = PointerType.Touch,
        onPointerSlopReached = { change, overSlop -> onTouchSlopReached(change, overSlop.x) },
        orientation = Orientation.Horizontal,
    )

/**
 * Waits for horizontal drag motion to pass [pointerType]'s touch slop, using [pointerId] as the
 * pointer to examine. If [pointerId] is raised, another pointer from those that are down will be
 * chosen to lead the gesture, and if none are down, `null` is returned. If [pointerId] is not down
 * when [awaitHorizontalPointerSlopOrCancellation] is called, then `null` is returned.
 *
 * [onPointerSlopReached] is called after [pointerType]'s touch slop motion in the horizontal
 * direction with the change that caused the motion beyond touch slop and the pixels beyond touch
 * slop. [onPointerSlopReached] should consume the position change if it accepts the motion. If it
 * does, then the method returns that [PointerInputChange]. If not, touch slop detection will
 * continue.
 *
 * @return The [PointerInputChange] that was consumed in [onPointerSlopReached] or `null` if all
 *   pointers are raised before touch slop is detected or another gesture consumed the position
 *   change.
 * @see awaitVerticalTouchSlopOrCancellation
 * @see awaitTouchSlopOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitHorizontalPointerSlopOrCancellation(
    pointerId: PointerId,
    pointerType: PointerType,
    onPointerSlopReached: (change: PointerInputChange, overSlop: Float) -> Unit,
) =
    awaitPointerSlopOrCancellation(
        pointerId = pointerId,
        pointerType = pointerType,
        onPointerSlopReached = { change, overSlop -> onPointerSlopReached(change, overSlop.x) },
        orientation = Orientation.Horizontal,
    )

/**
 * Reads horizontal position change events for [pointerId] and calls [onDrag] for every change in
 * position. If [pointerId] is raised, a new pointer is chosen from those that are down and if none
 * exist, the method returns. This does not wait for touch slop.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.HorizontalDragSample
 * @see awaitHorizontalTouchSlopOrCancellation
 * @see awaitDragOrCancellation
 * @see verticalDrag
 * @see drag
 */
suspend fun AwaitPointerEventScope.horizontalDrag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): Boolean =
    drag(
        pointerId = pointerId,
        onDrag = onDrag,
        orientation = Orientation.Horizontal,
        motionConsumed = { it.isConsumed },
    ) != null

/**
 * Reads pointer input events until a horizontal drag is detected or all pointers are up. When the
 * final pointer is raised, the up event is returned. When a drag event is detected, the drag change
 * will be returned. Note that if [pointerId] has been raised, another pointer that is down will be
 * used, if available, so the returned [PointerInputChange.id] may differ from [pointerId]. If the
 * position change has been consumed by the [PointerEventPass.Main] pass, then the drag is
 * considered canceled and `null` is returned. If [pointerId] is not down when
 * [awaitHorizontalDragOrCancellation] is called, then `null` is returned.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitHorizontalDragOrCancellationSample
 * @see horizontalDrag
 * @see awaitVerticalDragOrCancellation
 * @see awaitDragOrCancellation
 */
suspend fun AwaitPointerEventScope.awaitHorizontalDragOrCancellation(
    pointerId: PointerId
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val change = awaitDragOrUp(pointerId) { it.positionChangeIgnoreConsumed().x != 0f }
    return if (change?.isConsumed == false) change else null
}

/**
 * Gesture detector that waits for pointer down and touch slop in the horizontal direction and then
 * calls [onHorizontalDrag] for each horizontal drag event. It follows the touch slop detection of
 * [awaitHorizontalTouchSlopOrCancellation], but will consume the position change automatically once
 * the touch slop has been crossed.
 *
 * [onDragStart] called when the touch slop has been passed and includes an [Offset] representing
 * the last known pointer position relative to the containing element. The [Offset] can be outside
 * the actual bounds of the element itself meaning the numbers can be negative or larger than the
 * element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 *
 * [onDragEnd] is called after all pointers are up and [onDragCancel] is called if another gesture
 * has consumed pointer input, canceling this gesture.
 *
 * This gesture detector will coordinate with [detectVerticalDragGestures] and
 * [awaitVerticalTouchSlopOrCancellation] to ensure only vertical or horizontal dragging is locked,
 * but not both.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.DetectHorizontalDragGesturesSample
 * @see detectVerticalDragGestures
 * @see detectDragGestures
 */
suspend fun PointerInputScope.detectHorizontalDragGestures(
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onHorizontalDrag: (change: PointerInputChange, dragAmount: Float) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var overSlop = 0f
        val drag =
            awaitHorizontalPointerSlopOrCancellation(down.id, down.type) { change, over ->
                change.consume()
                overSlop = over
            }
        if (drag != null) {
            onDragStart.invoke(drag.position)
            onHorizontalDrag(drag, overSlop)
            if (
                horizontalDrag(drag.id) {
                    onHorizontalDrag(it, it.positionChange().x)
                    it.consume()
                }
            ) {
                onDragEnd()
            } else {
                onDragCancel()
            }
        }
    }
}

/**
 * Continues to read drag events until all pointers are up or the drag event is canceled. The
 * initial pointer to use for driving the drag is [pointerId]. [onDrag] is called whenever the
 * pointer moves. The up event is returned at the end of the drag gesture.
 *
 * @param pointerId The pointer where that is driving the gesture.
 * @param onDrag Callback for every new drag event.
 * @param motionConsumed If the PointerInputChange should be considered as consumed.
 * @return The last pointer input event change when gesture ended with all pointers up and null when
 *   the gesture was canceled.
 */
internal suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
    orientation: Orientation?,
    motionConsumed: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    var pointer = pointerId
    while (true) {
        val change =
            awaitDragOrUp(pointer) {
                val positionChange = it.positionChangeIgnoreConsumed()
                val motionChange =
                    if (orientation == null) {
                        positionChange.getDistance()
                    } else {
                        if (orientation == Orientation.Vertical) positionChange.y
                        else positionChange.x
                    }
                motionChange != 0.0f
            } ?: return null

        if (motionConsumed(change)) {
            return null
        }

        if (change.changedToUpIgnoreConsumed()) {
            return change
        }

        onDrag(change)
        pointer = change.id
    }
}

/**
 * Waits for a single drag in one axis, final pointer up, or all pointers are up. When [pointerId]
 * has lifted, another pointer that is down is chosen to be the finger governing the drag. When the
 * final pointer is lifted, that [PointerInputChange] is returned. When a drag is detected, that
 * [PointerInputChange] is returned. A drag is only detected when [hasDragged] returns `true`.
 *
 * `null` is returned if there was an error in the pointer input stream and the pointer that was
 * down was dropped before the 'up' was received.
 */
private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
    hasDragged: (PointerInputChange) -> Boolean,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else if (hasDragged(dragEvent)) {
            return dragEvent
        }
    }
}

/**
 * Waits for drag motion and uses [orientation] to detect the direction of touch slop detection. It
 * passes [pointerId] as the pointer to examine. If [pointerId] is raised, another pointer from
 * those that are down will be chosen to lead the gesture, and if none are down, `null` is returned.
 * If [pointerId] is not down when [awaitPointerSlopOrCancellation] is called, then `null` is
 * returned.
 *
 * When pointer slop is detected, [onPointerSlopReached] is called with the change and the distance
 * beyond the pointer slop. If [onPointerSlopReached] does not consume the position change, pointer
 * slop will not have been considered detected and the detection will continue or, if it is
 * consumed, the [PointerInputChange] that was consumed will be returned.
 *
 * This works with [awaitTouchSlopOrCancellation] for the other axis to ensure that only horizontal
 * or vertical dragging is done, but not both. It also works for dragging in two ways when using
 * [awaitTouchSlopOrCancellation]
 *
 * We use [initialPositionChange] to consider any amount of initial movement in this gesture before
 * the slop detector is called.
 *
 * @return The [PointerInputChange] of the event that was consumed in [onPointerSlopReached] or
 *   `null` if all pointers are raised or the position change was consumed by another gesture
 *   detector.
 */
internal suspend inline fun AwaitPointerEventScope.awaitPointerSlopOrCancellation(
    pointerId: PointerId,
    pointerType: PointerType,
    orientation: Orientation?,
    initialPositionChange: Offset = Offset.Zero,
    onPointerSlopReached: (PointerInputChange, Offset) -> Unit,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }

    val touchSlop = viewConfiguration.pointerSlop(pointerType)
    var pointer: PointerId = pointerId
    val touchSlopDetector = TouchSlopDetector(orientation, initialPositionChange)
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.isConsumed) {
            return null
        } else if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            val postSlopOffset = touchSlopDetector.addPointerInputChange(dragEvent, touchSlop)
            if (postSlopOffset.isSpecified) {
                onPointerSlopReached(dragEvent, postSlopOffset)
                if (dragEvent.isConsumed) {
                    return dragEvent
                } else {
                    touchSlopDetector.reset()
                }
            } else {
                // verify that nothing else consumed the drag event
                awaitPointerEvent(PointerEventPass.Final)
                if (dragEvent.isConsumed) {
                    return null
                }
            }
        }
    }
}

/**
 * Similar to [awaitAllPointersUp], but additionally tracks if during current gesture there was any
 * dragging without consuming it.
 */
internal suspend fun AwaitPointerEventScope.awaitAllPointersUpWithSlopDetection(
    initialPositionChange: PointerInputChange,
    pass: PointerEventPass = PointerEventPass.Main,
): Boolean {
    if (allPointersUp()) {
        return false
    }

    var pointer: PointerId = initialPositionChange.id
    var pointerSlopReached = false
    val touchSlop = viewConfiguration.pointerSlop(initialPositionChange.type)
    val touchSlopDetector = TouchSlopDetector()
    do {
        val event = awaitPointerEvent(pass)
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer }
        if (dragEvent == null || dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return pointerSlopReached
            } else {
                pointer = otherDown.id
            }
        } else {
            val postSlopOffset = touchSlopDetector.addPointerInputChange(dragEvent, touchSlop)
            if (postSlopOffset.isSpecified) {
                pointerSlopReached = true
            }
        }
    } while (event.changes.fastAny { it.pressed })
    return pointerSlopReached
}

/**
 * Detects if touch slop has been crossed after adding a series of [PointerInputChange]. For every
 * new [PointerInputChange] one should add it to this detector using [addPointerInputChange]. If the
 * position change causes the touch slop to be crossed, [addPointerInputChange] will return true.
 */
internal class TouchSlopDetector(
    var orientation: Orientation? = null,
    initialPositionChange: Offset = Offset.Zero,
) {

    fun Offset.mainAxis() = if (orientation == Orientation.Horizontal) x else y

    fun Offset.crossAxis() = if (orientation == Orientation.Horizontal) y else x

    /** The accumulation of drag deltas in this detector. */
    private var totalPositionChange: Offset = initialPositionChange

    /**
     * Adds [dragEvent] to this detector. If the accumulated position changes crosses the touch slop
     * provided by [touchSlop], this method will return the post slop offset, that is the total
     * accumulated delta change minus the touch slop value, otherwise this should return null.
     */
    fun addPointerInputChange(dragEvent: PointerInputChange, touchSlop: Float): Offset {
        val currentPosition = dragEvent.position
        val previousPosition = dragEvent.previousPosition
        val positionChange = currentPosition - previousPosition
        totalPositionChange += positionChange

        val inDirection =
            if (orientation == null) {
                totalPositionChange.getDistance()
            } else {
                totalPositionChange.mainAxis().absoluteValue
            }

        val hasCrossedSlop = inDirection >= touchSlop

        return if (hasCrossedSlop) {
            calculatePostSlopOffset(touchSlop)
        } else {
            Offset.Unspecified
        }
    }

    /**
     * Resets the accumulator associated with this detector.
     *
     * @param initialPositionAccumulator Use to initialize the position change accumulator, for
     *   instance in cases where slop detection may happen "mid-gesture", that is, the slop
     *   detection didn't start from the first down event but somewhere after.
     */
    fun reset(initialPositionAccumulator: Offset = Offset.Zero) {
        totalPositionChange = initialPositionAccumulator
    }

    private fun calculatePostSlopOffset(touchSlop: Float): Offset {
        return if (orientation == null) {
            val touchSlopOffset =
                totalPositionChange / totalPositionChange.getDistance() * touchSlop
            // update postSlopOffset
            totalPositionChange - touchSlopOffset
        } else {
            val finalMainAxisChange =
                totalPositionChange.mainAxis() - (sign(totalPositionChange.mainAxis()) * touchSlop)
            val finalCrossAxisChange = totalPositionChange.crossAxis()
            if (orientation == Orientation.Horizontal) {
                Offset(finalMainAxisChange, finalCrossAxisChange)
            } else {
                Offset(finalCrossAxisChange, finalMainAxisChange)
            }
        }
    }
}

/**
 * Waits for a long press by examining [pointerId].
 *
 * If that [pointerId] is raised (that is, the user lifts their finger), but another finger
 * ([PointerId]) is down at that time, another pointer will be chosen as the lead for the gesture,
 * and if none are down, `null` is returned.
 *
 * @return The latest [PointerInputChange] associated with a long press or `null` if all pointers
 *   are raised before a long press is detected or another gesture consumed the change.
 *
 * Example Usage:
 *
 * @sample androidx.compose.foundation.samples.AwaitLongPressOrCancellationSample
 */
suspend fun AwaitPointerEventScope.awaitLongPressOrCancellation(
    pointerId: PointerId
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the long press is cancelled.
    }

    val initialDown = currentEvent.changes.fastFirstOrNull { it.id == pointerId } ?: return null

    var longPress: PointerInputChange? = null
    var currentDown = initialDown
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    return try {
        var deepPress = false
        // wait for first tap up or long press
        withTimeout(longPressTimeout) {
            var finished = false
            while (!finished) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                    // All pointers are up
                    finished = true
                }

                if (
                    event.changes.fastAny {
                        it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
                    }
                ) {
                    finished = true // Canceled
                }

                if (event.isDeepPress) {
                    deepPress = true
                    finished = true
                }

                // Check for cancel by position consumption. We can look on the Final pass of
                // the existing pointer event because it comes after the Main pass we checked
                // above.
                val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                if (consumeCheck.changes.fastAny { it.isConsumed }) {
                    finished = true
                }
                if (event.isPointerUp(currentDown.id)) {
                    val newPressed = event.changes.fastFirstOrNull { it.pressed }
                    if (newPressed != null) {
                        currentDown = newPressed
                        longPress = currentDown
                    } else {
                        // should technically never happen as we checked it above
                        finished = true
                    }
                    // Pointer (id) stayed down.
                } else {
                    longPress = event.changes.fastFirstOrNull { it.id == currentDown.id }
                }
            }
        }
        // If we finished early because of a deep press, return the relevant change as this counts
        // as a long press
        if (deepPress) {
            longPress ?: initialDown
        } else {
            null
        }
    } catch (_: PointerEventTimeoutCancellationException) {
        longPress ?: initialDown
    }
}

private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true

// This value was determined using experiments and common sense.
// We can't use zero slop, because some hypothetical desktop/mobile devices can send
// pointer events with a very high precision (but I haven't encountered any that send
// events with less than 1px precision)
private val mouseSlop = 0.125.dp
private val defaultTouchSlop = 18.dp // The default touch slop on Android devices
private val mouseToTouchSlopRatio = mouseSlop / defaultTouchSlop

// TODO(demin): consider this as part of ViewConfiguration class after we make *PointerSlop*
//  functions public (see the comment at the top of the file).
//  After it will be a public API, we should get rid of `touchSlop / 144` and return absolute
//  value 0.125.dp.toPx(). It is not possible right now, because we can't access density.
internal fun ViewConfiguration.pointerSlop(pointerType: PointerType): Float {
    return when (pointerType) {
        PointerType.Mouse -> touchSlop * mouseToTouchSlopRatio
        else -> touchSlop
    }
}
