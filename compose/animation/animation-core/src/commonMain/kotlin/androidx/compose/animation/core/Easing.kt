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

package androidx.compose.animation.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.computeCubicVerticalBounds
import androidx.compose.ui.graphics.evaluateCubic
import androidx.compose.ui.graphics.findFirstCubicRoot
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.max

/**
 * Easing is a way to adjust an animation’s fraction. Easing allows transitioning elements to speed
 * up and slow down, rather than moving at a constant rate.
 *
 * Fraction is a value between 0 and 1.0 indicating our current point in the animation where 0
 * represents the start and 1.0 represents the end.
 *
 * An [Easing] must map fraction=0.0 to 0.0 and fraction=1.0 to 1.0.
 */
@Stable
public fun interface Easing {
    public fun transform(fraction: Float): Float
}

/**
 * Elements that begin and end at rest use this standard easing. They speed up quickly and slow down
 * gradually, in order to emphasize the end of the transition.
 *
 * Standard easing puts subtle attention at the end of an animation, by giving more time to
 * deceleration than acceleration. It is the most common form of easing.
 *
 * This is equivalent to the Android `FastOutSlowInInterpolator`
 */
public val FastOutSlowInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

/**
 * Incoming elements are animated using deceleration easing, which starts a transition at peak
 * velocity (the fastest point of an element’s movement) and ends at rest.
 *
 * This is equivalent to the Android `LinearOutSlowInInterpolator`
 */
public val LinearOutSlowInEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

/**
 * Elements exiting a screen use acceleration easing, where they start at rest and end at peak
 * velocity.
 *
 * This is equivalent to the Android `FastOutLinearInInterpolator`
 */
public val FastOutLinearInEasing: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

/**
 * It returns fraction unmodified. This is useful as a default value for cases where a [Easing] is
 * required but no actual easing is desired.
 */
public val LinearEasing: Easing = Easing { fraction -> fraction }

// This is equal to 1f.ulp or 1f.nextUp() - 1f, but neither ulp nor nextUp() are part of all KMP
// targets, only JVM and native
private const val OneUlpAt1 = 1.1920929e-7f

/**
 * A cubic polynomial easing.
 *
 * The [CubicBezierEasing] class implements third-order Bézier curves.
 *
 * This is equivalent to the Android `PathInterpolator` when a single cubic Bézier curve is
 * specified.
 *
 * Note: [CubicBezierEasing] instances are stateless and can be used concurrently from multiple
 * threads.
 *
 * Rather than creating a new instance, consider using one of the common cubic [Easing]s:
 *
 * @param a The x coordinate of the first control point. The line through the point (0, 0) and the
 *   first control point is tangent to the easing at the point (0, 0).
 * @param b The y coordinate of the first control point. The line through the point (0, 0) and the
 *   first control point is tangent to the easing at the point (0, 0).
 * @param c The x coordinate of the second control point. The line through the point (1, 1) and the
 *   second control point is tangent to the easing at the point (1, 1).
 * @param d The y coordinate of the second control point. The line through the point (1, 1) and the
 *   second control point is tangent to the easing at the point (1, 1).
 * @see FastOutSlowInEasing
 * @see LinearOutSlowInEasing
 * @see FastOutLinearInEasing
 */
@Immutable
public class CubicBezierEasing(
    private val a: Float,
    private val b: Float,
    private val c: Float,
    private val d: Float
) : Easing {
    private val min: Float
    private val max: Float

    init {
        requirePrecondition(!a.isNaN() && !b.isNaN() && !c.isNaN() && !d.isNaN()) {
            "Parameters to CubicBezierEasing cannot be NaN. Actual parameters are: $a, $b, $c, $d."
        }
        val roots = FloatArray(5)
        val extrema = computeCubicVerticalBounds(0.0f, b, d, 1.0f, roots, 0)
        min = extrema.first
        max = extrema.second
    }

    /**
     * Transforms the specified [fraction] in the range 0..1 by this cubic Bézier curve. To solve
     * the curve, [fraction] is used as the x coordinate along the curve, and the corresponding y
     * coordinate on the curve is returned. If no solution exists, this method throws an
     * [IllegalArgumentException].
     *
     * @throws IllegalArgumentException If the cubic Bézier curve cannot be solved
     */
    override fun transform(fraction: Float): Float {
        return if (fraction > 0f && fraction < 1f) {
            // We translate the coordinates by the fraction when calling findFirstCubicRoot,
            // but we need to make sure the translation can be done at 1.0f so we take at
            // least 1 ulp at 1.0f
            val f = max(fraction, OneUlpAt1)
            val t =
                findFirstCubicRoot(
                    0.0f - f,
                    a - f,
                    c - f,
                    1.0f - f,
                )

            // No root, the cubic curve has no solution
            if (t.isNaN()) {
                throwNoSolution(fraction)
            }

            // Don't clamp the values since the curve might be used to over- or under-shoot
            // The test above that checks if fraction is in ]0..1[ will ensure we start and
            // end at 0 and 1 respectively
            evaluateCubic(b, d, t).fastCoerceIn(min, max)
        } else {
            fraction
        }
    }

    private fun throwNoSolution(fraction: Float) {
        throw IllegalArgumentException(
            "The cubic curve with parameters ($a, $b, $c, $d) has no solution at $fraction"
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is CubicBezierEasing &&
            a == other.a &&
            b == other.b &&
            c == other.c &&
            d == other.d
    }

    override fun hashCode(): Int {
        return ((a.hashCode() * 31 + b.hashCode()) * 31 + c.hashCode()) * 31 + d.hashCode()
    }

    override fun toString(): String = "CubicBezierEasing(a=$a, b=$b, c=$c, d=$d)"
}
