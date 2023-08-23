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

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * This class has all internal methods, used by Polygon, Morph, etc.
 */

internal fun distance(x: Float, y: Float) = sqrt(x * x + y * y)

/**
 * Returns unit vector representing the direction to this point from (0, 0)
 */
internal fun directionVector(x: Float, y: Float): Point {
    val d = distance(x, y)
    require(d > 0f)
    return Point(x / d, y / d)
}

internal fun directionVector(angleRadians: Float) = Point(cos(angleRadians), sin(angleRadians))

internal fun angle(x: Float, y: Float) = ((atan2(y, x) + TwoPi) % TwoPi)

internal fun radialToCartesian(radius: Float, angleRadians: Float, center: Point = Zero) =
    directionVector(angleRadians) * radius + center

/**
 * These epsilon values are used internally to determine when two points are the same, within
 * some reasonable roundoff error. The distance epsilon is smaller, with the intention that the
 * roundoff should not be larger than a pixel on any reasonable sized display.
 */
internal const val DistanceEpsilon = 1e-4f
internal const val AngleEpsilon = 1e-6f

internal fun Point.rotate90() = Point(-y, x)

internal val Zero = Point(0f, 0f)

internal val FloatPi = Math.PI.toFloat()

internal val TwoPi: Float = 2 * Math.PI.toFloat()

internal fun square(x: Float) = x * x

/**
 * Linearly interpolate between [start] and [stop] with [fraction] fraction between them.
 */
internal fun interpolate(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

internal fun positiveModulo(num: Float, mod: Float) = (num % mod + mod) % mod

/*
 * Does a ternary search in [v0..v1] to find the parameter that minimizes the given function.
 * Stops when the search space size is reduced below the given tolerance.
 *
 * NTS: Does it make sense to split the function f in 2, one to generate a candidate, of a custom
 * type T (i.e. (Float) -> T), and one to evaluate it ( (T) -> Float )?
 */
internal fun findMinimum(
    v0: Float,
    v1: Float,
    tolerance: Float = 1e-3f,
    f: (Float) -> Float
): Float {
    var a = v0
    var b = v1
    while (b - a > tolerance) {
        val c1 = (2 * a + b) / 3
        val c2 = (2 * b + a) / 3
        if (f(c1) < f(c2)) {
            b = c2
        } else {
            a = c1
        }
    }
    return (a + b) / 2
}

internal fun verticesFromNumVerts(
    numVertices: Int,
    radius: Float,
    centerX: Float,
    centerY: Float
): FloatArray {
    val result = FloatArray(numVertices * 2)
    var arrayIndex = 0
    for (i in 0 until numVertices) {
        val vertex = radialToCartesian(radius, (FloatPi / numVertices * 2 * i)) +
            Point(centerX, centerY)
        result[arrayIndex++] = vertex.x
        result[arrayIndex++] = vertex.y
    }
    return result
}

internal fun starVerticesFromNumVerts(
    numVerticesPerRadius: Int,
    radius: Float,
    innerRadius: Float,
    centerX: Float,
    centerY: Float
): FloatArray {
    val result = FloatArray(numVerticesPerRadius * 4)
    var arrayIndex = 0
    for (i in 0 until numVerticesPerRadius) {
        var vertex = radialToCartesian(radius, (FloatPi / numVerticesPerRadius * 2 * i)) +
            Point(centerX, centerY)
        result[arrayIndex++] = vertex.x
        result[arrayIndex++] = vertex.y
        vertex = radialToCartesian(innerRadius, (FloatPi / numVerticesPerRadius * (2 * i + 1))) +
            Point(centerX, centerY)
        result[arrayIndex++] = vertex.x
        result[arrayIndex++] = vertex.y
    }
    return result
}

internal const val DEBUG = false

internal inline fun debugLog(tag: String, messageFactory: () -> String) {
    // TODO: Re-implement properly when the library goes KMP using expect/actual
    if (DEBUG) {
        println("$tag: ${messageFactory()}")
    }
}
