/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("Utils")

package androidx.graphics.shapes

import android.graphics.PointF
import androidx.core.graphics.div
import androidx.core.graphics.plus
import androidx.core.graphics.times
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * This class has all internal methods, used by Polygon, Morph, etc.
 */

internal fun interpolate(start: Float, stop: Float, fraction: Float) =
    (start * (1 - fraction) + stop * fraction)

internal fun interpolate(start: PointF, stop: PointF, fraction: Float): PointF {
    return PointF(
        interpolate(start.x, stop.x, fraction),
        interpolate(start.y, stop.y, fraction)
    )
}

/**
 * Non-allocating version of interpolate; values are stored in [result]
 */
internal fun interpolate(start: PointF, stop: PointF, result: PointF, fraction: Float): PointF {
    result.x = interpolate(start.x, stop.x, fraction)
    result.y = interpolate(start.y, stop.y, fraction)
    return result
}

internal fun PointF.getDistance() = sqrt(x * x + y * y)

internal fun PointF.dotProduct(other: PointF) = x * other.x + y * other.y

/**
 * Returns unit vector representing the direction to this point from (0, 0)
 */
internal fun PointF.getDirection() = run {
    val d = this.getDistance()
    require(d > 0f)
    this / d
}

// These epsilon values are used internally to determine when two points are the same, within
// some reasonable roundoff error. The distance episilon is smaller, with the intention that the
// roundoff should not be larger than a pixel on any reasonable sized display.
internal const val DistanceEpsilon = 1e-4f
internal const val AngleEpsilon = 1e-6f

internal fun PointF.rotate90() = PointF(-y, x)

internal val Zero = PointF(0f, 0f)

internal val FloatPi = Math.PI.toFloat()

internal fun Float.toRadians(): Float {
    return this / 360f * TwoPi
}

internal val TwoPi: Float = 2 * Math.PI.toFloat()

internal fun directionVector(angleRadians: Float) = PointF(cos(angleRadians), sin(angleRadians))

internal fun square(x: Float) = x * x

internal fun PointF.copy(x: Float = Float.NaN, y: Float = Float.NaN) =
    PointF(if (x.isNaN()) this.x else x, if (y.isNaN()) this.y else y)

internal fun PointF.angle() = ((atan2(y, x) + TwoPi) % TwoPi)

internal fun radialToCartesian(radius: Float, angleRadians: Float, center: PointF = Zero) =
    directionVector(angleRadians) * radius + center
