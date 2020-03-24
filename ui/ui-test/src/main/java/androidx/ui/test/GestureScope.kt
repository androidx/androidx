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
import androidx.ui.core.gesture.LongPressTimeout
import androidx.ui.unit.Duration
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import androidx.ui.unit.toPx
import androidx.ui.util.lerp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * An object that has an associated component in which one can inject gestures. The gestures can
 * be injected by calling methods defined on [GestureScope], such as [sendSwipeUp]. The associated
 * component is the [SemanticsNodeInteraction] found by one of the finder methods such as [findByTag].
 *
 * Example usage:
 * findByTag("myWidget")
 *    .doGesture {
 *        sendSwipeUp()
 *    }
 */
class GestureScope internal constructor(
    internal val semanticsNodeInteraction: SemanticsNodeInteraction
) {
    // TODO(b/133217292): Better error: explain which gesture couldn't be performed
    // TODO: Avoid calling this multiple times as it involves synchronization.
    internal inline val semanticsNode
        get() = semanticsNodeInteraction.fetchSemanticsNode("Failed to perform a gesture.")
    internal inline val semanticsTreeInteraction
        get() = semanticsNodeInteraction.semanticsTreeInteraction
}

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

/**
 * Returns the size of the component we're interacting with
 */
val GestureScope.size: IntPxSize
    get() = semanticsNode.size

/**
 * Returns the center of the component we're interacting with, in the component's local
 * coordinate system, where (0.px, 0.px) is the top left corner of the component.
 */
val GestureScope.center: PxPosition
    get() {
        return PxPosition(size.width / 2, size.height / 2)
    }

/**
 * Returns the global bounds of the component we're interacting with
 */
val GestureScope.globalBounds: PxBounds
    get() = semanticsNode.globalBounds

/**
 * Transforms the [position] to global coordinates, as defined by [globalBounds]
 *
 * @param position A position in local coordinates
 */
fun GestureScope.localToGlobal(position: PxPosition): PxPosition {
    val bounds = globalBounds
    return position + PxPosition(bounds.left, bounds.top)
}

/**
 * Performs a click gesture on the given [position] on the associated component. The [position]
 * is in the component's local coordinate system, where (0.px, 0.px) is the top left corner of
 * the component.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 *
 * @param position The position where to click, in the component's local coordinate system
 */
fun GestureScope.sendClick(position: PxPosition) {
    semanticsTreeInteraction.sendInput {
        it.sendClick(localToGlobal(position))
    }
}

/**
 * Performs a click gesture on the associated component. The click is done in the middle of the
 * component's bounds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendClick() {
    sendClick(center)
}

/**
 * Performs a long click gesture on the given [position] on the associated component. There will
 * be [LongPressTimeout] + 100 milliseconds time between the down and the up event. The
 * [position] is in the component's local coordinate system, where (0.px, 0.px) is the top left
 * corner of the component.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 *
 * @param position The position of the long click, in the component's local coordinate system
 */
fun GestureScope.sendLongClick(position: PxPosition) {
    // Keep down for 100ms more than needed, to allow the long press logic to trigger
    sendSwipe(position, position, LongPressTimeout + 100.milliseconds)
}

/**
 * Performs a long click gesture on the middle of the associated component. There will
 * be [LongPressTimeout] + 100 milliseconds time between the down and the up event.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendLongClick() {
    sendLongClick(center)
}

/**
 * Performs a double click gesture on the given [position] on the associated component. The
 * [position] is in the component's local coordinate system, where (0.px, 0.px) is the top left
 * corner of the component.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 *
 * @param position The position of the double click, in the component's local coordinate system
 */
fun GestureScope.sendDoubleClick(position: PxPosition) {
    val globalPosition = localToGlobal(position)
    semanticsTreeInteraction.sendInput {
        it.sendClick(globalPosition)
        it.delay(doubleClickDelay)
        it.sendClick(globalPosition)
    }
}

/**
 * Performs a double click gesture on the associated component. The clicks are done in the middle
 * of the component's bounds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendDoubleClick() {
    sendDoubleClick(center)
}

/**
 * Performs the swipe gesture on the associated component. The motion events are linearly
 * interpolated between [start] and [end]. The coordinates are in the component's local
 * coordinate system, where (0.px, 0.px) is the top left corner of the component. The default
 * duration is 200 milliseconds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 *
 * @param start The start position of the gesture, in the component's local coordinate system
 * @param end The end position of the gesture, in the component's local coordinate system
 * @param duration The duration of the gesture
 */
fun GestureScope.sendSwipe(
    start: PxPosition,
    end: PxPosition,
    duration: Duration = 200.milliseconds
) {
    val globalStart = localToGlobal(start)
    val globalEnd = localToGlobal(end)
    semanticsTreeInteraction.sendInput {
        it.sendSwipe(globalStart, globalEnd, duration)
    }
}

/**
 * Performs the swipe gesture on the associated component, such that the velocity when the
 * gesture is finished is roughly equal to [endVelocity]. The MotionEvents are linearly
 * interpolated between [start] and [end]. The coordinates are in the component's
 * local coordinate system, where (0.px, 0.px) is the top left corner of the component. The
 * default duration is 200 milliseconds.
 *
 * Note that due to imprecisions, no guarantees can be made on the precision of the actual
 * velocity at the end of the gesture, but generally it is within 0.1% of the desired velocity.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 *
 * @param start The start position of the gesture, in the component's local coordinate system
 * @param end The end position of the gesture, in the component's local coordinate system
 * @param endVelocity The velocity of the gesture at the moment it ends. Must be positive.
 * @param duration The duration of the gesture. Must be long enough that at least 3 input events
 * are generated, which happens with a duration of 25ms or more.
 */
fun GestureScope.sendSwipeWithVelocity(
    start: PxPosition,
    end: PxPosition,
    @FloatRange(from = 0.0) endVelocity: Float,
    duration: Duration = 200.milliseconds
) {
    require(endVelocity >= 0f) {
        "Velocity cannot be $endVelocity, it must be positive"
    }
    // TODO(146551983): require that duration >= 2.5 * eventPeriod
    // TODO(146551983): check that eventPeriod < 40 milliseconds
    require(duration >= 25.milliseconds) {
        "Duration must be at least 25ms because velocity requires at least 3 input events"
    }
    val globalStart = localToGlobal(start)
    val globalEnd = localToGlobal(end)

    // Decompose v into it's x and y components
    val delta = end - start
    val theta = atan2(delta.y.value, delta.x.value)
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
    val fx = createFunctionForVelocity(durationMs, globalStart.x.value, globalEnd.x.value, vx)
    val fy = createFunctionForVelocity(durationMs, globalStart.y.value, globalEnd.y.value, vy)

    semanticsTreeInteraction.sendInput {
        it.sendSwipe({ t -> PxPosition(fx(t).px, fy(t).px) }, duration)
    }
}

/**
 * Performs a swipe up gesture on the associated component. The gesture starts slightly above the
 * bottom of the component and ends at the top.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeUp() {
    val x = center.x
    val y0 = size.height * (1 - edgeFuzzFactor)
    val y1 = 0.px
    val start = PxPosition(x, y0.toPx())
    val end = PxPosition(x, y1)
    sendSwipe(start, end, 200.milliseconds)
}

/**
 * Performs a swipe down gesture on the associated component. The gesture starts slightly below the
 * top of the component and ends at the bottom.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeDown() {
    val x = center.x
    val y0 = size.height * edgeFuzzFactor
    val y1 = size.height
    val start = PxPosition(x, y0.toPx())
    val end = PxPosition(x, y1.toPx())
    sendSwipe(start, end, 200.milliseconds)
}

/**
 * Performs a swipe left gesture on the associated component. The gesture starts slightly left of
 * the right side of the component and ends at the left side.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeLeft() {
    val x0 = size.width * (1 - edgeFuzzFactor)
    val x1 = 0.px
    val y = center.y
    val start = PxPosition(x0.toPx(), y)
    val end = PxPosition(x1, y)
    sendSwipe(start, end, 200.milliseconds)
}

/**
 * Performs a swipe right gesture on the associated component. The gesture starts slightly right of
 * the left side of the component and ends at the right side.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeRight() {
    val x0 = size.width * edgeFuzzFactor
    val x1 = size.width
    val y = center.y
    val start = PxPosition(x0.toPx(), y)
    val end = PxPosition(x1.toPx(), y)
    sendSwipe(start, end, 200.milliseconds)
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
