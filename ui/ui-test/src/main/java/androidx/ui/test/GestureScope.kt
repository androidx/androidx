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

package androidx.ui.test

import androidx.annotation.FloatRange
import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.gesture.DoubleTapTimeout
import androidx.ui.core.gesture.LongPressTimeout
import androidx.ui.core.semantics.SemanticsNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.ui.test.InputDispatcher.Companion.eventPeriod
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.inMilliseconds
import androidx.compose.ui.unit.milliseconds
import androidx.compose.ui.util.lerp
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * The distance of a swipe's start position from the node's edge, in terms of the node's length.
 * We do not start the swipe exactly on the node's edge, but somewhat more inward, since swiping
 * from the exact edge may behave in an unexpected way (e.g. may open a navigation drawer).
 */
private const val edgeFuzzFactor = 0.083f

/**
 * The time between the last event of the first click and the first event of the second click in
 * a double click gesture. 145 milliseconds: both median and average of empirical data (33 samples)
 */
private val doubleClickDelay = 145.milliseconds

sealed class BaseGestureScope(node: SemanticsNode) {
    // TODO(b/133217292): Better error: explain which gesture couldn't be performed
    private var _semanticsNode: SemanticsNode? = node
    internal val semanticsNode
        get() = checkNotNull(_semanticsNode) {
            "Can't query SemanticsNode, (Partial)GestureScope has already been disposed"
        }

    // Convenience property
    @OptIn(ExperimentalLayoutNodeApi::class)
    private val owner get() = semanticsNode.componentNode.owner

    // TODO(b/133217292): Better error: explain which gesture couldn't be performed
    private var _inputDispatcher: InputDispatcher? =
        InputDispatcher.createInstance(checkNotNull(owner))
    internal val inputDispatcher
        get() = checkNotNull(_inputDispatcher) {
            "Can't send gesture, (Partial)GestureScope has already been disposed"
        }

    internal fun dispose() {
        inputDispatcher.saveState(owner)
        _semanticsNode = null
        _inputDispatcher = null
    }
}

/**
 * Returns the size of the node we're interacting with
 */
val BaseGestureScope.size: IntSize
    get() = semanticsNode.size

/**
 * Shorthand for `size.width`
 */
inline val BaseGestureScope.width: Int
    get() = size.width

/**
 * Shorthand for `size.height`
 */
inline val BaseGestureScope.height: Int
    get() = size.height

/**
 * Returns the x-coordinate for the left edge of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 */
@Suppress("unused")
inline val BaseGestureScope.left: Float
    get() = 0f

/**
 * Returns the y-coordinate for the bottom of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 */
@Suppress("unused")
inline val BaseGestureScope.top: Float
    get() = 0f

/**
 * Returns the x-coordinate for the center of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 */
inline val BaseGestureScope.centerX: Float
    get() = width / 2f

/**
 * Returns the y-coordinate for the center of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 */
inline val BaseGestureScope.centerY: Float
    get() = height / 2f

/**
 * Returns the x-coordinate for the right edge of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 * Note that, unless `width == 0`, `right != width`. In particular, `right == width - 1f`, because
 * pixels are 0-based. If `width == 0`, `right == 0` too.
 */
inline val BaseGestureScope.right: Float
    get() = width.let { if (it == 0) 0f else it - 1f }

/**
 * Returns the y-coordinate for the bottom of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 * Note that, unless `height == 0`, `bottom != height`. In particular, `bottom == height - 1f`,
 * because pixels are 0-based. If `height == 0`, `bottom == 0` too.
 */
inline val BaseGestureScope.bottom: Float
    get() = height.let { if (it == 0) 0f else it - 1f }

/**
 * Returns the top left corner of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node.
 */
@Suppress("unused")
val BaseGestureScope.topLeft: Offset
    get() = Offset(left, top)

/**
 * Returns the center of the top edge of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node.
 */
val BaseGestureScope.topCenter: Offset
    get() = Offset(centerX, top)

/**
 * Returns the top right corner of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node. Note that
 * `topRight.x != width`, see [right].
 */
val BaseGestureScope.topRight: Offset
    get() = Offset(right, top)

/**
 * Returns the center of the left edge of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 */
val BaseGestureScope.centerLeft: Offset
    get() = Offset(left, centerY)

/**
 * Returns the center of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node.
 */
val BaseGestureScope.center: Offset
    get() = Offset(centerX, centerY)

/**
 * Returns the center of the right edge of the node we're interacting with, in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 * Note that `centerRight.x != width`, see [right].
 */
val BaseGestureScope.centerRight: Offset
    get() = Offset(right, centerY)

/**
 * Returns the bottom left corner of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node. Note that
 * `bottomLeft.y != height`, see [bottom].
 */
val BaseGestureScope.bottomLeft: Offset
    get() = Offset(left, bottom)

/**
 * Returns the center of the bottom edge of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node. Note that
 * `bottomCenter.y != height`, see [bottom].
 */
val BaseGestureScope.bottomCenter: Offset
    get() = Offset(centerX, bottom)

/**
 * Returns the bottom right corner of the node we're interacting with, in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node. Note that
 * `bottomRight.x != width` and `bottomRight.y != height`, see [right] and [bottom].
 */
val BaseGestureScope.bottomRight: Offset
    get() = Offset(right, bottom)

/**
 * Creates an [Offset] relative to the size of the node we're interacting with. [x] and [y]
 * are fractions of the [width] and [height]. Note that `percentOffset(1f, 1f) != bottomRight`,
 * see [right] and [bottom].
 *
 * For example: `percentOffset(.5f, .5f)` is the same as the [center]; `centerLeft +
 * percentOffset(.1f, 0f)` is a point 10% inward from the middle of the left edge; and
 * `bottomRight - percentOffset(.2f, .1f)` is a point 20% to the left and 10% to the top of the
 * bottom right corner.
 */
fun BaseGestureScope.percentOffset(
    @FloatRange(from = -1.0, to = 1.0) x: Float = 0f,
    @FloatRange(from = -1.0, to = 1.0) y: Float = 0f
): Offset {
    return Offset(x * width, y * height)
}

/**
 * Transforms the [position] to global coordinates, as defined by
 * [LayoutCoordinates.localToGlobal][androidx.ui.core.LayoutCoordinates.localToGlobal]
 *
 * @param position A position in local coordinates
 */
fun BaseGestureScope.localToGlobal(position: Offset): Offset {
    @OptIn(ExperimentalLayoutNodeApi::class)
    return semanticsNode.componentNode.coordinates.localToGlobal(position)
}

/**
 * The receiver scope for injecting gestures on the [semanticsNode] identified by the
 * corresponding [SemanticsNodeInteraction]. Gestures can be injected by calling methods defined
 * on [GestureScope], such as [swipeUp]. The [SemanticsNodeInteraction] can be found by one
 * of the finder methods such as [onNodeWithTag].
 *
 * Example usage:
 * ```
 * onNodeWithTag("myWidget")
 *    .performGesture {
 *        sendSwipeUp()
 *    }
 * ```
 */
class GestureScope internal constructor(
    semanticsNode: SemanticsNode
) : BaseGestureScope(semanticsNode)

/**
 * Performs a click gesture at the given [position] on the associated node, or in the center
 * if the [position] is omitted. The [position] is in the node's local coordinate system,
 * where (0, 0) is the top left corner of the node. The default [position] is the
 * center of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If
 * omitted, the center position will be used.
 */
fun GestureScope.click(position: Offset = center) {
    inputDispatcher.sendClick(localToGlobal(position))
}

/**
 * Performs a long click gesture at the given [position] on the associated node, or in the
 * center if the [position] is omitted. By default, the [duration] of the press is
 * [LongPressTimeout] + 100 milliseconds. The [position] is in the node's local coordinate
 * system, where (0, 0) is the top left corner of the node.
 *
 * @param position The position of the long click, in the node's local coordinate system. If
 * omitted, the center position will be used.
 * @param duration The time between the down and the up event
 */
fun GestureScope.longClick(
    position: Offset = center,
    duration: Duration = LongPressTimeout + 100.milliseconds
) {
    require(duration >= LongPressTimeout) {
        "Long click must have a duration of at least ${LongPressTimeout.inMilliseconds()}ms"
    }
    swipe(position, position, duration)
}

/**
 * Performs a double click gesture at the given [position] on the associated node, or in the
 * center if the [position] is omitted. By default, the [delay] between the first and the second
 * click is 145 milliseconds (empirically established). The [position] is in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node.
 *
 * @param position The position of the double click, in the node's local coordinate system.
 * If omitted, the center position will be used.
 * @param delay The time between the up event of the first click and the down event of the second
 * click
 */
fun GestureScope.doubleClick(
    position: Offset = center,
    delay: Duration = doubleClickDelay
) {
    require(delay <= DoubleTapTimeout - 10.milliseconds) {
        "Time between clicks in double click can be at most ${DoubleTapTimeout - 10.milliseconds}ms"
    }
    val globalPosition = localToGlobal(position)
    inputDispatcher.sendClick(globalPosition)
    inputDispatcher.delay(delay)
    inputDispatcher.sendClick(globalPosition)
}

/**
 * Performs the swipe gesture on the associated node. The motion events are linearly
 * interpolated between [start] and [end]. The coordinates are in the node's local
 * coordinate system, where (0, 0) is the top left corner of the node. The default
 * duration is 200 milliseconds.
 *
 * @param start The start position of the gesture, in the node's local coordinate system
 * @param end The end position of the gesture, in the node's local coordinate system
 * @param duration The duration of the gesture
 */
fun GestureScope.swipe(
    start: Offset,
    end: Offset,
    duration: Duration = 200.milliseconds
) {
    val globalStart = localToGlobal(start)
    val globalEnd = localToGlobal(end)
    inputDispatcher.sendSwipe(globalStart, globalEnd, duration)
}

/**
 * Performs a pinch gesture on the associated node.
 *
 * For each pair of start and end [Offset]s, the motion events are linearly interpolated. The
 * coordinates are in the node's local coordinate system where (0, 0) is the top left
 * corner of the node. The default duration is 400 milliseconds.
 *
 * @param start0 The start position of the first gesture in the node's local coordinate system
 * @param end0 The end position of the first gesture in the node's local coordinate system
 * @param start1 The start position of the second gesture in the node's local coordinate system
 * @param end1 The end position of the second gesture in the node's local coordinate system
 * @param duration the duration of the gesture
 */
fun GestureScope.pinch(
    start0: Offset,
    end0: Offset,
    start1: Offset,
    end1: Offset,
    duration: Duration = 400.milliseconds
) {
    val globalStart0 = localToGlobal(start0)
    val globalEnd0 = localToGlobal(end0)
    val globalStart1 = localToGlobal(start1)
    val globalEnd1 = localToGlobal(end1)
    val durationFloat = duration.inMilliseconds().toFloat()

    inputDispatcher.sendSwipes(
        listOf<(Long) -> Offset>(
            { lerp(globalStart0, globalEnd0, it / durationFloat) },
            { lerp(globalStart1, globalEnd1, it / durationFloat) }
        ),
        duration
    )
}

/**
 * Performs the swipe gesture on the associated node, such that the velocity when the
 * gesture is finished is roughly equal to [endVelocity]. The MotionEvents are linearly
 * interpolated between [start] and [end]. The coordinates are in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node. The
 * default duration is 200 milliseconds.
 *
 * Note that due to imprecisions, no guarantees can be made on the precision of the actual
 * velocity at the end of the gesture, but generally it is within 0.1% of the desired velocity.
 *
 * @param start The start position of the gesture, in the node's local coordinate system
 * @param end The end position of the gesture, in the node's local coordinate system
 * @param endVelocity The velocity of the gesture at the moment it ends. Must be positive.
 * @param duration The duration of the gesture. Must be long enough that at least 3 input events
 * are generated, which happens with a duration of 25ms or more.
 */
fun GestureScope.swipeWithVelocity(
    start: Offset,
    end: Offset,
    @FloatRange(from = 0.0) endVelocity: Float,
    duration: Duration = 200.milliseconds
) {
    require(endVelocity >= 0f) {
        "Velocity cannot be $endVelocity, it must be positive"
    }
    require(eventPeriod < 40.milliseconds.inMilliseconds()) {
        "InputDispatcher.eventPeriod must be smaller than 40ms in order to generate velocities"
    }
    val minimumDuration = ceil(2.5f * eventPeriod).roundToInt()
    require(duration >= minimumDuration.milliseconds) {
        "Duration must be at least ${minimumDuration}ms because " +
                "velocity requires at least 3 input events"
    }
    val globalStart = localToGlobal(start)
    val globalEnd = localToGlobal(end)

    // Decompose v into it's x and y components
    val delta = end - start
    val theta = atan2(delta.y, delta.x)
    // VelocityTracker internally calculates px/s, not px/ms
    val vx = cos(theta) * endVelocity / 1000
    val vy = sin(theta) * endVelocity / 1000

    // Note: it would be more precise to do `theta = atan2(-y, x)`, because atan2 expects a
    // coordinate system where positive y goes up and in our coordinate system positive y goes
    // down. However, in that case we would also have to inverse `vy` to convert the velocity
    // back to our own coordinate system. But then it's just a double negation, so we can skip
    // both conversions entirely.

    // To get the desired velocity, generate fx and fy such that VelocityTracker calculates
    // the right velocity. VelocityTracker makes a polynomial fit through the points
    // (-age, x) and (-age, y) for vx and vy respectively, which is accounted for in
    // f(Long, Long, Float, Float, Float).
    val durationMs = duration.inMilliseconds()
    val fx = createFunctionForVelocity(durationMs, globalStart.x, globalEnd.x, vx)
    val fy = createFunctionForVelocity(durationMs, globalStart.y, globalEnd.y, vy)

    inputDispatcher.sendSwipe({ t -> Offset(fx(t), fy(t)) }, duration)
}

/**
 * Performs a swipe up gesture on the associated node. The gesture starts slightly above the
 * bottom of the node and ends at the top.
 */
fun GestureScope.swipeUp() {
    val x = center.x
    val y0 = (size.height * (1 - edgeFuzzFactor)).roundToInt().toFloat()
    val y1 = 0.0f
    val start = Offset(x, y0)
    val end = Offset(x, y1)
    swipe(start, end, 200.milliseconds)
}

/**
 * Performs a swipe down gesture on the associated node. The gesture starts slightly below the
 * top of the node and ends at the bottom.
 */
fun GestureScope.swipeDown() {
    val x = center.x
    val y0 = (size.height * edgeFuzzFactor).roundToInt().toFloat()
    val y1 = size.height.toFloat()
    val start = Offset(x, y0)
    val end = Offset(x, y1)
    swipe(start, end, 200.milliseconds)
}

/**
 * Performs a swipe left gesture on the associated node. The gesture starts slightly left of
 * the right side of the node and ends at the left side.
 */
fun GestureScope.swipeLeft() {
    val x0 = (size.width * (1 - edgeFuzzFactor)).roundToInt().toFloat()
    val x1 = 0.0f
    val y = center.y
    val start = Offset(x0, y)
    val end = Offset(x1, y)
    swipe(start, end, 200.milliseconds)
}

/**
 * Performs a swipe right gesture on the associated node. The gesture starts slightly right of
 * the left side of the node and ends at the right side.
 */
fun GestureScope.swipeRight() {
    val x0 = (size.width * edgeFuzzFactor).roundToInt().toFloat()
    val x1 = size.width.toFloat()
    val y = center.y
    val start = Offset(x0, y)
    val end = Offset(x1, y)
    swipe(start, end, 200.milliseconds)
}

/**
 * Generate a function of the form `f(t) = a*(t-T)^2 + b*(t-T) + c` that satisfies
 * `f(0) = [start]`, `f([duration]) = [end]`, `T = [duration]` and `b = [velocity]`.
 *
 * Filling in `f([duration]) = [end]`, `T = [duration]` and `b = [velocity]` gives:
 * * `a * (duration - duration)^2 + velocity * (duration - duration) + c = end`
 * * `c = end`
 *
 * Filling in `f(0) = [start]`, `T = [duration]` and `b = [velocity]` gives:
 * * `a * (0 - duration)^2 + velocity * (0 - duration) + c = start`
 * * `a * duration^2 - velocity * duration + end = start`
 * * `a * duration^2 = start - end + velocity * duration`
 * * `a = (start - end + velocity * duration) / duration^2`
 *
 * @param duration The duration of the fling
 * @param start The start x or y position
 * @param end The end x or y position
 * @param velocity The desired velocity in the x or y direction at the [end] position
 */
private fun createFunctionForVelocity(
    duration: Long,
    start: Float,
    end: Float,
    velocity: Float
): (Long) -> Float {
    val a = (start - end + velocity * duration) / (duration * duration)
    val function = { t: Long ->
        val tMinusDuration = t - duration
        // `f(t) = a*(t-T)^2 + b*(t-T) + c`
        a * tMinusDuration * tMinusDuration + velocity * tMinusDuration + end
    }

    // High velocities often result in curves that start off in the wrong direction, like a bow
    // being strung to reach a high velocity at the end coordinate. For a gesture, that is not
    // desirable, and can be mitigated by using the fact that VelocityTracker only uses the last
    // 100 ms of the gesture. Anything before that doesn't need to follow the curve.

    // Does the function go in the correct direction at the start?
    if (sign(function(1) - start) == sign(end - start)) {
        return function
    } else {
        // If not, lerp between 0 and `duration - 100` in an attempt to prevent the function from
        // going in the wrong direction. This does not affect the velocity at f(duration), as
        // VelocityTracker only uses the last 100ms. This only works if f(duration - 100) is
        // between from and to, log a warning if this is not the case.
        val cutOffTime = duration - 100
        val cutOffValue = function(cutOffTime)
        require(sign(cutOffValue - start) == sign(end - start)) {
            "Creating a gesture between $start and $end with a duration of $duration and a " +
                    "resulting velocity of $velocity results in a movement that goes outside " +
                    "of the range [$start..$end]"
        }
        return { t ->
            if (t < cutOffTime) {
                lerp(start, cutOffValue, t / cutOffTime.toFloat())
            } else {
                function(t)
            }
        }
    }
}

/**
 * The receiver scope for injecting partial gestures on the [semanticsNode] identified by the
 * corresponding [SemanticsNodeInteraction]. Gestures can be injected by calling methods defined
 * on [PartialGestureScope], such as [down]. The [SemanticsNodeInteraction] can be found by
 * one of the finder methods such as [onNodeWithTag].
 *
 * Example usage:
 * ```
 * val position = Offset(10.px, 10.px)
 * onNodeWithTag("myWidget")
 *    .performPartialGesture { sendDown(position) }
 *    .assertIsDisplayed()
 *    .performPartialGesture { sendUp(position) }
 * ```
 */
class PartialGestureScope internal constructor(
    semanticsNode: SemanticsNode
) : BaseGestureScope(semanticsNode)

/**
 * Sends a down event for the pointer with the given [pointerId] at [position] on the associated
 * node. The [position] is in the node's local coordinate system, where (0, 0) is
 * the top left corner of the node.
 *
 * If no pointers are down yet, this will start a new partial gesture. If a partial gesture is
 * already in progress, this event is sent with at the same timestamp as the last event. If the
 * given pointer is already down, an [IllegalArgumentException] will be thrown.
 *
 * This gesture is considered _partial_, because the entire gesture can be spread over several
 * invocations of [performPartialGesture]. An entire gesture starts with a [down][down] event,
 * followed by several down, move or up events, and ends with an [up][up] or a
 * [cancel][cancel] event. Movement can be expressed with [moveTo] and [moveBy] to
 * move a single pointer at a time, or [movePointerTo] and [movePointerBy] to move multiple
 * pointers at a time. The `movePointer[To|By]` methods do not send the move event directly, use
 * [move] to send the move event. Some other methods can send a move event as well. All
 * events, regardless the method used, will always contain the current position of _all_ pointers.
 *
 * Down and up events are sent at the same time as the previous event, but will send an extra
 * move event just before the down or up event if [movePointerTo] or [movePointerBy] has been
 * called and no move event has been sent yet. This does not happen for cancel events, but the
 * cancel event will contain the up to date position of all pointers. Move and cancel events will
 * advance the event time by 10 milliseconds.
 *
 * Because partial gestures don't have to be defined all in the same [performPartialGesture] block,
 * keep in mind that while the gesture is not complete, all code you execute in between
 * blocks that progress the gesture, will be executed while imaginary fingers are actively
 * touching the screen.
 *
 * In the context of testing, it is not necessary to complete a gesture with an up or cancel
 * event, if the test ends before it expects the finger to be lifted from the screen.
 *
 * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
 * @param position The position of the down event, in the node's local coordinate system
 */
fun PartialGestureScope.down(pointerId: Int, position: Offset) {
    val globalPosition = localToGlobal(position)
    inputDispatcher.sendDown(pointerId, globalPosition)
}

/**
 * Sends a down event for the default pointer at [position] on the associated node. The
 * [position] is in the node's local coordinate system, where (0, 0) is the top left
 * corner of the node. The default pointer has `pointerId = 0`.
 *
 * If no pointers are down yet, this will start a new partial gesture. If a partial gesture is
 * already in progress, this event is sent with at the same timestamp as the last event. If the
 * default pointer is already down, an [IllegalArgumentException] will be thrown.
 *
 * @param position The position of the down event, in the node's local coordinate system
 */
fun PartialGestureScope.down(position: Offset) {
    down(0, position)
}

/**
 * Sends a move event on the associated node, with the position of the pointer with the
 * given [pointerId] updated to [position]. The [position] is in the node's local coordinate
 * system, where (0, 0) is the top left corner of the node.
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param pointerId The id of the pointer to move, as supplied in [down]
 * @param position The new position of the pointer, in the node's local coordinate system
 */
fun PartialGestureScope.moveTo(pointerId: Int, position: Offset) {
    movePointerTo(pointerId, position)
    move()
}

/**
 * Sends a move event on the associated node, with the position of the default pointer
 * updated to [position]. The [position] is in the node's local coordinate system, where
 * (0, 0) is the top left corner of the node. The default pointer has `pointerId = 0`.
 *
 * If the default pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param position The new position of the pointer, in the node's local coordinate system
 */
fun PartialGestureScope.moveTo(position: Offset) {
    moveTo(0, position)
}

/**
 * Updates the position of the pointer with the given [pointerId] to the given [position], but
 * does not send a move event. The move event can be sent with [move]. The [position] is in
 * the node's local coordinate system, where (0.px, 0.px) is the top left corner of the
 * node.
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param pointerId The id of the pointer to move, as supplied in [down]
 * @param position The new position of the pointer, in the node's local coordinate system
 */
fun PartialGestureScope.movePointerTo(pointerId: Int, position: Offset) {
    val globalPosition = localToGlobal(position)
    inputDispatcher.movePointer(pointerId, globalPosition)
}

/**
 * Sends a move event on the associated node, with the position of the pointer with the
 * given [pointerId] moved by the given [delta].
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param pointerId The id of the pointer to move, as supplied in [down]
 * @param delta The position for this move event, relative to the last sent position of the
 * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's last
 * x-position, and subtract 10.px from the pointer's last y-position.
 */
fun PartialGestureScope.moveBy(pointerId: Int, delta: Offset) {
    movePointerBy(pointerId, delta)
    move()
}

/**
 * Sends a move event on the associated node, with the position of the default pointer
 * moved by the given [delta]. The default pointer has `pointerId = 0`.
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param delta The position for this move event, relative to the last sent position of the
 * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's last
 * x-position, and subtract 10.px from the pointer's last y-position.
 */
fun PartialGestureScope.moveBy(delta: Offset) {
    moveBy(0, delta)
}

/**
 * Moves the position of the pointer with the given [pointerId] by the given [delta], but does
 * not send a move event. The move event can be sent with [move].
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param pointerId The id of the pointer to move, as supplied in [down]
 * @param delta The position for this move event, relative to the last sent position of the
 * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's last
 * x-position, and subtract 10.px from the pointer's last y-position.
 */
fun PartialGestureScope.movePointerBy(pointerId: Int, delta: Offset) {
    // Ignore currentPosition of null here, let movePointer generate the error
    val globalPosition =
        (inputDispatcher.getCurrentPosition(pointerId) ?: Offset.Zero) + delta
    inputDispatcher.movePointer(pointerId, globalPosition)
}

/**
 * Sends a move event without updating any of the pointer positions. This can be useful when
 * batching movement of multiple pointers together, which can be done with [movePointerTo] and
 * [movePointerBy].
 */
fun PartialGestureScope.move() {
    inputDispatcher.sendMove()
}

/**
 * Sends an up event for the pointer with the given [pointerId], or the default pointer if
 * [pointerId] is omitted, on the associated node. If any pointers have been moved with
 * [movePointerTo] or [movePointerBy] and no move event has been sent yet, a move event will be
 * sent right before the up event.
 *
 * @param pointerId The id of the pointer to lift up, as supplied in [down]
 */
fun PartialGestureScope.up(pointerId: Int = 0) {
    inputDispatcher.sendUp(pointerId)
}

/**
 * Sends a cancel event to cancel the current partial gesture. The cancel event contains the
 * current position of all active pointers.
 */
fun PartialGestureScope.cancel() {
    inputDispatcher.sendCancel()
}

// DEPRECATED APIs SECTION

/**
 * Performs a click gesture at the given [position] on the associated component, or in the center
 * if the [position] is omitted. The [position] is in the component's local coordinate system,
 * where (0, 0) is the top left corner of the component. The default [position] is the
 * center of the component.
 *
 * @param position The position where to click, in the component's local coordinate system. If
 * omitted, the center position will be used.
 */
@Deprecated("Renamed to click",
    replaceWith = ReplaceWith("click(position)"))
fun GestureScope.sendClick(position: Offset = center) = click(position)

/**
 * Performs a long click gesture at the given [position] on the associated component, or in the
 * center if the [position] is omitted. By default, the [duration] of the press is
 * [LongPressTimeout] + 100 milliseconds. The [position] is in the component's local coordinate
 * system, where (0, 0) is the top left corner of the component.
 *
 * @param position The position of the long click, in the component's local coordinate system. If
 * omitted, the center position will be used.
 * @param duration The time between the down and the up event
 */
@Deprecated("Renamed to longClick",
    replaceWith = ReplaceWith("longClick(position, duration)"))
fun GestureScope.sendLongClick(
    position: Offset = center,
    duration: Duration = LongPressTimeout + 100.milliseconds
) = longClick(position, duration)

/**
 * Performs a double click gesture at the given [position] on the associated component, or in the
 * center if the [position] is omitted. By default, the [delay] between the first and the second
 * click is 145 milliseconds (empirically established). The [position] is in the component's
 * local coordinate system, where (0, 0) is the top left corner of the component.
 *
 * @param position The position of the double click, in the component's local coordinate system.
 * If omitted, the center position will be used.
 * @param delay The time between the up event of the first click and the down event of the second
 * click
 */
@Deprecated("Renamed to doubleClick",
    replaceWith = ReplaceWith("doubleClick(position, delay)"))
fun GestureScope.sendDoubleClick(
    position: Offset = center,
    delay: Duration = doubleClickDelay
) = doubleClick(position, delay)

/**
 * Performs the swipe gesture on the associated component. The motion events are linearly
 * interpolated between [start] and [end]. The coordinates are in the component's local
 * coordinate system, where (0, 0) is the top left corner of the component. The default
 * duration is 200 milliseconds.
 *
 * @param start The start position of the gesture, in the component's local coordinate system
 * @param end The end position of the gesture, in the component's local coordinate system
 * @param duration The duration of the gesture
 */
@Deprecated("Renamed to swipe",
    replaceWith = ReplaceWith("swipe(start, end, duration)"))
fun GestureScope.sendSwipe(
    start: Offset,
    end: Offset,
    duration: Duration = 200.milliseconds
) = swipe(start, end, duration)

/**
 * Performs a pinch gesture on the associated component.
 *
 * For each pair of start and end [Offset]s, the motion events are linearly interpolated. The
 * coordinates are in the component's local coordinate system where (0, 0) is the top left
 * corner of the component. The default duration is 400 milliseconds.
 *
 * @param start0 The start position of the first gesture in the component's local coordinate system
 * @param end0 The end position of the first gesture in the component's local coordinate system
 * @param start1 The start position of the second gesture in the component's local coordinate system
 * @param end1 The end position of the second gesture in the component's local coordinate system
 * @param duration the duration of the gesture
 */
@Deprecated("Renamed to pinch",
    replaceWith = ReplaceWith("pinch(start0, end0, start1, end0, duration)"))
fun GestureScope.sendPinch(
    start0: Offset,
    end0: Offset,
    start1: Offset,
    end1: Offset,
    duration: Duration = 400.milliseconds
) = pinch(start0, end0, start1, end1, duration)

/**
 * Performs the swipe gesture on the associated component, such that the velocity when the
 * gesture is finished is roughly equal to [endVelocity]. The MotionEvents are linearly
 * interpolated between [start] and [end]. The coordinates are in the component's
 * local coordinate system, where (0, 0) is the top left corner of the component. The
 * default duration is 200 milliseconds.
 *
 * Note that due to imprecisions, no guarantees can be made on the precision of the actual
 * velocity at the end of the gesture, but generally it is within 0.1% of the desired velocity.
 *
 * @param start The start position of the gesture, in the component's local coordinate system
 * @param end The end position of the gesture, in the component's local coordinate system
 * @param endVelocity The velocity of the gesture at the moment it ends. Must be positive.
 * @param duration The duration of the gesture. Must be long enough that at least 3 input events
 * are generated, which happens with a duration of 25ms or more.
 */
@Deprecated("Renamed to swipeWithVelocity",
    replaceWith = ReplaceWith("swipeWithVelocity(start, end, endVelocity, duration)"))
fun GestureScope.sendSwipeWithVelocity(
    start: Offset,
    end: Offset,
    @FloatRange(from = 0.0) endVelocity: Float,
    duration: Duration = 200.milliseconds
) = swipeWithVelocity(start, end, endVelocity, duration)

/**
 * Performs a swipe up gesture on the associated component. The gesture starts slightly above the
 * bottom of the component and ends at the top.
 */
@Deprecated("Renamed to swipeUp",
    replaceWith = ReplaceWith("swipeUp()"))
fun GestureScope.sendSwipeUp() = swipeUp()

/**
 * Performs a swipe down gesture on the associated component. The gesture starts slightly below the
 * top of the component and ends at the bottom.
 */
@Deprecated("Renamed to swipeDown",
    replaceWith = ReplaceWith("swipeDown()"))
fun GestureScope.sendSwipeDown() = swipeDown()

/**
 * Performs a swipe left gesture on the associated component. The gesture starts slightly left of
 * the right side of the component and ends at the left side.
 */
@Deprecated("Renamed to swipeLeft",
    replaceWith = ReplaceWith("swipeLeft()"))
fun GestureScope.sendSwipeLeft() = swipeLeft()

/**
 * Performs a swipe right gesture on the associated component. The gesture starts slightly right of
 * the left side of the component and ends at the right side.
 */
@Deprecated("Renamed to swipeRight",
    replaceWith = ReplaceWith("swipeRight()"))
fun GestureScope.sendSwipeRight() = swipeRight()

/**
 * Sends a down event for the pointer with the given [pointerId] at [position] on the associated
 * component. The [position] is in the component's local coordinate system, where (0, 0) is
 * the top left corner of the component.
 *
 * If no pointers are down yet, this will start a new partial gesture. If a partial gesture is
 * already in progress, this event is sent with at the same timestamp as the last event. If the
 * given pointer is already down, an [IllegalArgumentException] will be thrown.
 *
 * This gesture is considered _partial_, because the entire gesture can be spread over several
 * invocations of [performPartialGesture]. An entire gesture starts with a [down][sendDown] event,
 * followed by several down, move or up events, and ends with an [up][sendUp] or a
 * [cancel][sendCancel] event. Movement can be expressed with [sendMoveTo] and [sendMoveBy] to
 * move a single pointer at a time, or [movePointerTo] and [movePointerBy] to move multiple
 * pointers at a time. The `movePointer[To|By]` methods do not send the move event directly, use
 * [sendMove] to send the move event. Some other methods can send a move event as well. All
 * events, regardless the method used, will always contain the current position of _all_ pointers.
 *
 * Down and up events are sent at the same time as the previous event, but will send an extra
 * move event just before the down or up event if [movePointerTo] or [movePointerBy] has been
 * called and no move event has been sent yet. This does not happen for cancel events, but the
 * cancel event will contain the up to date position of all pointers. Move and cancel events will
 * advance the event time by 10 milliseconds.
 *
 * Because partial gestures don't have to be defined all in the same [performPartialGesture] block,
 * keep in mind that while the gesture is not complete, all code you execute in between
 * blocks that progress the gesture, will be executed while imaginary fingers are actively
 * touching the screen.
 *
 * In the context of testing, it is not necessary to complete a gesture with an up or cancel
 * event, if the test ends before it expects the finger to be lifted from the screen.
 *
 * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
 * @param position The position of the down event, in the component's local coordinate system
 */
@Deprecated("Renamed to down",
    replaceWith = ReplaceWith("down(pointerId, position)"))
fun PartialGestureScope.sendDown(pointerId: Int, position: Offset) = down(pointerId, position)

/**
 * Sends a down event for the default pointer at [position] on the associated component. The
 * [position] is in the component's local coordinate system, where (0, 0) is the top left
 * corner of the component. The default pointer has `pointerId = 0`.
 *
 * If no pointers are down yet, this will start a new partial gesture. If a partial gesture is
 * already in progress, this event is sent with at the same timestamp as the last event. If the
 * default pointer is already down, an [IllegalArgumentException] will be thrown.
 *
 * @param position The position of the down event, in the component's local coordinate system
 */
@Deprecated("Renamed to down",
    replaceWith = ReplaceWith("down(position)"))
fun PartialGestureScope.sendDown(position: Offset) = down(position)

/**
 * Sends a move event on the associated component, with the position of the pointer with the
 * given [pointerId] updated to [position]. The [position] is in the component's local coordinate
 * system, where (0, 0) is the top left corner of the component.
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param pointerId The id of the pointer to move, as supplied in [sendDown]
 * @param position The new position of the pointer, in the component's local coordinate system
 */
@Deprecated("Renamed to moveTo",
    replaceWith = ReplaceWith("moveTo(pointerId, position)"))
fun PartialGestureScope.sendMoveTo(pointerId: Int, position: Offset) = moveTo(pointerId, position)

/**
 * Sends a move event on the associated component, with the position of the default pointer
 * updated to [position]. The [position] is in the component's local coordinate system, where
 * (0, 0) is the top left corner of the component. The default pointer has `pointerId = 0`.
 *
 * If the default pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param position The new position of the pointer, in the component's local coordinate system
 */
@Deprecated("Renamed to moveTo",
    replaceWith = ReplaceWith("moveTo(position)"))
fun PartialGestureScope.sendMoveTo(position: Offset) = moveTo(position)

/**
 * Sends a move event on the associated component, with the position of the pointer with the
 * given [pointerId] moved by the given [delta].
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param pointerId The id of the pointer to move, as supplied in [sendDown]
 * @param delta The position for this move event, relative to the last sent position of the
 * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's last
 * x-position, and subtract 10.px from the pointer's last y-position.
 */
@Deprecated("Renamed to moveBy",
    replaceWith = ReplaceWith("moveBy(pointerId, delta)"))
fun PartialGestureScope.sendMoveBy(pointerId: Int, delta: Offset) = moveBy(pointerId, delta)

/**
 * Sends a move event on the associated component, with the position of the default pointer
 * moved by the given [delta]. The default pointer has `pointerId = 0`.
 *
 * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
 *
 * @param delta The position for this move event, relative to the last sent position of the
 * pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's last
 * x-position, and subtract 10.px from the pointer's last y-position.
 */
@Deprecated("Renamed to moveBy",
    replaceWith = ReplaceWith("moveBy(delta)"))
fun PartialGestureScope.sendMoveBy(delta: Offset) = moveBy(delta)

/**
 * Sends a move event without updating any of the pointer positions. This can be useful when
 * batching movement of multiple pointers together, which can be done with [movePointerTo] and
 * [movePointerBy].
 */
@Deprecated("Renamed to move",
    replaceWith = ReplaceWith("move()"))
fun PartialGestureScope.sendMove() = move()

/**
 * Sends an up event for the pointer with the given [pointerId], or the default pointer if
 * [pointerId] is omitted, on the associated component. If any pointers have been moved with
 * [movePointerTo] or [movePointerBy] and no move event has been sent yet, a move event will be
 * sent right before the up event.
 *
 * @param pointerId The id of the pointer to lift up, as supplied in [sendDown]
 */
@Deprecated("Renamed to up",
    replaceWith = ReplaceWith("up(pointerId)"))
fun PartialGestureScope.sendUp(pointerId: Int = 0) = up(pointerId)

/**
 * Sends a cancel event to cancel the current partial gesture. The cancel event contains the
 * current position of all active pointers.
 */
@Deprecated("Renamed to cancel",
    replaceWith = ReplaceWith("cancel()"))
fun PartialGestureScope.sendCancel() = cancel()