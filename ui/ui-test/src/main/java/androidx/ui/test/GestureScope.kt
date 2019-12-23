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
import androidx.ui.core.Duration
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.inMilliseconds
import androidx.ui.core.milliseconds
import androidx.ui.lerp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * An object that has an associated component in which one can inject gestures. The gestures can
 * be injected by calling methods defined on [GestureScope], such as [sendSwipeUp]. The associated
 * component is the [SemanticsTreeNode] found by one of the finder methods such as [findByTag].
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
    internal inline val semanticsTreeNode
        get() = semanticsNodeInteraction.semanticsTreeNode
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
 * Performs a click gesture on the given coordinate on the associated component. The coordinate
 * ([x], [y]) is in the component's local coordinate system.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendClick(x: Float, y: Float) {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
    val xOffset = globalRect.left
    val yOffset = globalRect.top

    semanticsTreeInteraction.sendInput {
        it.sendClick(x + xOffset, y + yOffset)
    }
}

/**
 * Performs a click gesture on the associated component. The click is done in the middle of the
 * component's bounds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendClick() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
    val x = globalRect.width / 2
    val y = globalRect.height / 2

    sendClick(x, y)
}

/**
 * Performs the swipe gesture on the associated component. The MotionEvents are linearly
 * interpolated between ([x0], [y0]) and ([x1], [y1]). The coordinates are in the component's local
 * coordinate system, i.e. (0, 0) is the top left corner of the component. The default duration is
 * 200 milliseconds.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipe(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    duration: Duration = 200.milliseconds
) {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val xOffset = globalRect.left
    val yOffset = globalRect.top

    val x0Global = x0 + xOffset
    val x1Global = x1 + xOffset
    val y0Global = y0 + yOffset
    val y1Global = y1 + yOffset

    semanticsTreeInteraction.sendInput {
        it.sendSwipe(x0Global, y0Global, x1Global, y1Global, duration)
    }
}

/**
 * Performs the swipe gesture on the associated component, such that the velocity when the
 * gesture is finished is roughly equal to [endVelocity]. The MotionEvents are linearly
 * interpolated between ([x0], [y0]) and ([x1], [y1]). The coordinates are in the component's
 * local coordinate system, i.e. (0, 0) is the top left corner of the component. The default
 * duration is 200 milliseconds.
 *
 * Note that due to imprecisions, no guarantees can be made on the precision of the actual
 * velocity at the end of the gesture, but generally it is within 0.1% of the desired velocity.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeWithVelocity(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
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
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val xOffset = globalRect.left
    val yOffset = globalRect.top

    val x0Global = x0 + xOffset
    val x1Global = x1 + xOffset
    val y0Global = y0 + yOffset
    val y1Global = y1 + yOffset

    // Decompose v into it's x and y components
    val theta = atan2(y1 - y0, x1 - x0)
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
    val fx = createFunctionForVelocity(durationMs, x0Global, x1Global, vx)
    val fy = createFunctionForVelocity(durationMs, y0Global, y1Global, vy)

    semanticsTreeInteraction.sendInput {
        it.sendSwipe(fx, fy, duration)
    }
}

/**
 * Performs a swipe up gesture on the associated component. The gesture starts slightly above the
 * bottom of the component and ends at the top.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeUp() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x = globalRect.width / 2
    val y0 = globalRect.height * (1 - edgeFuzzFactor)
    val y1 = 0f

    sendSwipe(x, y0, x, y1, 200.milliseconds)
}

/**
 * Performs a swipe down gesture on the associated component. The gesture starts slightly below the
 * top of the component and ends at the bottom.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeDown() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x = globalRect.width / 2
    val y0 = globalRect.height * edgeFuzzFactor
    val y1 = globalRect.height

    sendSwipe(x, y0, x, y1, 200.milliseconds)
}

/**
 * Performs a swipe left gesture on the associated component. The gesture starts slightly left of
 * the right side of the component and ends at the left side.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeLeft() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x0 = globalRect.width * (1 - edgeFuzzFactor)
    val x1 = 0f
    val y = globalRect.height / 2

    sendSwipe(x0, y, x1, y, 200.milliseconds)
}

/**
 * Performs a swipe right gesture on the associated component. The gesture starts slightly right of
 * the left side of the component and ends at the right side.
 *
 * Throws [AssertionError] when the component doesn't have a bounding rectangle set
 */
fun GestureScope.sendSwipeRight() {
    val globalRect = semanticsTreeNode.globalRect
        ?: throw AssertionError("Semantic Node has no child layout to perform swipe on!")
    val x0 = globalRect.width * edgeFuzzFactor
    val x1 = globalRect.width
    val y = globalRect.height / 2

    sendSwipe(x0, y, x1, y, 200.milliseconds)
}

/**
 * Generate a function of the form `f(t) = a*(t-T)^2 + b*(t-T) + c` that satisfies `f(0) = [from]`,
 * `f([duration]) = [to]`, `T = [duration]` and `b = [velocity]`.
 *
 * Filling in `f([duration]) = [to]`, `T = [duration]` and `b = [velocity]` gives:
 * * `a * (duration - duration)^2 + velocity * (duration - duration) + c = to`
 * * `c = to`
 *
 * Filling in `f(0) = [from]`, `T = [duration]` and `b = [velocity]` gives:
 * * `a * (0 - duration)^2 + velocity * (0 - duration) + c = from`
 * * `a * duration^2 - velocity * duration + to = from`
 * * `a * duration^2 = from - to + velocity * duration`
 * * `a = (from - to + velocity * duration) / duration^2`
 *
 * @param duration The duration of the fling
 * @param from The start x or y coordinate
 * @param to The end x or y coordinate
 * @param velocity The desired velocity in the x or y direction at the [to] coordinate
 */
private fun createFunctionForVelocity(
    duration: Long,
    from: Float,
    to: Float,
    velocity: Float
): (Long) -> Float {
    val a = (from - to + velocity * duration) / (duration * duration)
    val function = { t: Long ->
        val tMinusDuration = t - duration
        // `f(t) = a*(t-T)^2 + b*(t-T) + c`
        a * tMinusDuration * tMinusDuration + velocity * tMinusDuration + to
    }

    // High velocities often result in curves that start off in the wrong direction, like a bow
    // being strung to reach a high velocity at the end coordinate. For a gesture, that is not
    // desirable, and can be mitigated by using the fact that VelocityTracker only uses the last
    // 100 ms of the gesture. Anything before that doesn't need to follow the curve.

    // Does the function go in the correct direction at the start?
    if (sign(function(1) - from) == sign(to - from)) {
        return function
    } else {
        // If not, lerp between 0 and `duration - 100` in an attempt to prevent the function from
        // going in the wrong direction. This does not affect the velocity at f(duration), as
        // VelocityTracker only uses the last 100ms. This only works if f(duration - 100) is
        // between from and to, log a warning if this is not the case.
        val cutOffTime = duration - 100
        val cutOffValue = function(cutOffTime)
        require(sign(cutOffValue - from) == sign(to - from)) {
            "Creating a gesture between $from and $to with a duration of $duration and a " +
                    "resulting velocity of $velocity results in a movement that goes outside of " +
                    "the range [$from..$to]"
        }
        return { t ->
            if (t < cutOffTime) {
                lerp(from, cutOffValue, t / cutOffTime.toFloat())
            } else {
                function(t)
            }
        }
    }
}
