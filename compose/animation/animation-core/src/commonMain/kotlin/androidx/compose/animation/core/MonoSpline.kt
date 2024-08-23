/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.hypot

private const val MonoSplineIsExtrapolate = true

/**
 * This performs a spline interpolation in multiple dimensions time is an array of all positions and
 * y is a list of arrays each with the values at each point
 */
internal class MonoSpline(time: FloatArray, y: Array<FloatArray>, periodicBias: Float) {
    private val timePoints: FloatArray
    private val values: Array<FloatArray>
    private val tangents: Array<FloatArray>
    private val slopeTemp: FloatArray

    init {
        val n = time.size
        val dim = y[0].size
        slopeTemp = FloatArray(dim)
        val slope = makeFloatArray(n - 1, dim) // could optimize this out
        val tangent = makeFloatArray(n, dim)
        for (j in 0 until dim) {
            for (i in 0 until n - 1) {
                val dt = time[i + 1] - time[i]
                slope[i][j] = (y[i + 1][j] - y[i][j]) / dt
                if (i == 0) {
                    tangent[i][j] = slope[i][j]
                } else {
                    tangent[i][j] = (slope[i - 1][j] + slope[i][j]) * 0.5f
                }
            }
            tangent[n - 1][j] = slope[n - 2][j]
        }
        if (!periodicBias.isNaN()) {
            for (j in 0 until dim) {
                // Slope indicated by bias, where 0.0f is the last slope and 1f is the initial slope
                val adjustedSlope =
                    (slope[n - 2][j] * (1 - periodicBias)) + (slope[0][j] * periodicBias)
                slope[0][j] = adjustedSlope
                slope[n - 2][j] = adjustedSlope
                tangent[n - 1][j] = adjustedSlope
                tangent[0][j] = adjustedSlope
            }
        }
        for (i in 0 until n - 1) {
            for (j in 0 until dim) {
                if (slope[i][j] == 0.0f) {
                    tangent[i][j] = 0.0f
                    tangent[i + 1][j] = 0.0f
                } else {
                    val a = tangent[i][j] / slope[i][j]
                    val b = tangent[i + 1][j] / slope[i][j]
                    val h = hypot(a, b)
                    if (h > 9.0) {
                        val t = 3.0f / h
                        tangent[i][j] = t * a * slope[i][j]
                        tangent[i + 1][j] = t * b * slope[i][j]
                    }
                }
            }
        }
        timePoints = time
        values = y
        tangents = tangent
    }

    /**
     * @param a number of arrays
     * @param b dimension of Float arrays
     */
    private fun makeFloatArray(a: Int, b: Int): Array<FloatArray> = Array(a) { FloatArray(b) }

    /** get the value of the j'th spline at time t */
    fun getPos(t: Float, j: Int): Float {
        val values = values
        val tangents = tangents
        val n = timePoints.size
        val index = if (t <= timePoints[0]) 0 else (if (t >= timePoints[n - 1]) n - 1 else -1)
        if (MonoSplineIsExtrapolate) {
            if (index != -1) {
                return values[index][j] + (t - timePoints[index]) * getSlope(timePoints[index], j)
            }
        } else {
            if (index != -1) {
                return values[index][j]
            }
        }

        for (i in 0 until n - 1) {
            if (t == timePoints[i]) {
                return values[i][j]
            }
            if (t < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                val y1 = values[i][j]
                val y2 = values[i + 1][j]
                val t1 = tangents[i][j]
                val t2 = tangents[i + 1][j]
                return hermiteInterpolate(h, x, y1, y2, t1, t2)
            }
        }
        return 0.0f // should never reach here
    }

    /**
     * Populate the values of the spline at time [time] into the given AnimationVector [v].
     *
     * You may provide [index] to simplify searching for the correct keyframe for the given [time].
     */
    fun getPos(time: Float, v: AnimationVector, index: Int = 0) {
        val n = timePoints.size
        val dim = values[0].size
        val k = if (time <= timePoints[0]) 0 else (if (time >= timePoints[n - 1]) n - 1 else -1)
        if (MonoSplineIsExtrapolate) {
            if (k != -1) {
                getSlope(timePoints[k], slopeTemp)
                for (j in 0 until dim) {
                    v[j] = values[k][j] + (time - timePoints[k]) * slopeTemp[j]
                }
                return
            }
        } else {
            if (k != -1) {
                for (j in 0 until dim) {
                    v[j] = values[k][j]
                }
                return
            }
        }

        for (i in index until n - 1) {
            if (time == timePoints[i]) {
                for (j in 0 until dim) {
                    v[j] = values[i][j]
                }
                return
            }
            if (time < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (time - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = hermiteInterpolate(h, x, y1, y2, t1, t2)
                }
                return
            }
        }
    }

    /** Get the differential of the value at time fill an array of slopes for each spline */
    private fun getSlope(time: Float, v: FloatArray) {
        val dim = values[0].size
        val n = timePoints.size
        val t = time.fastCoerceIn(timePoints[0], timePoints[n - 1])

        if (v.size < dim) return
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = hermiteDifferential(h, x, y1, y2, t1, t2) / h
                }
                break
            }
        }
    }

    /**
     * Populate the differential values of the spline at the given [time] in to the given
     * AnimationVector [v].
     *
     * You may provide [index] to simplify searching for the correct keyframe for the given [time].
     */
    fun getSlope(time: Float, v: AnimationVector, index: Int = 0) {
        val timePoints = timePoints
        val values = values
        val tangents = tangents

        val n = timePoints.size
        val dim = values[0].size

        // If time is 0, max or out of range we directly return the corresponding slope value
        val tangentIndex =
            if (time <= timePoints[0]) 0 else (if (time >= timePoints[n - 1]) n - 1 else -1)
        if (tangentIndex != -1) {
            val tangent = tangents[tangentIndex]
            // Help ART eliminate bound checks
            if (tangent.size < dim) return
            for (j in 0 until dim) {
                v[j] = tangent[j]
            }
            return
        }

        // Otherwise, calculate interpolated velocity
        for (i in index until n - 1) {
            if (time <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (time - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = hermiteDifferential(h, x, y1, y2, t1, t2) / h
                }
                break
            }
        }
    }

    private fun getSlope(time: Float, j: Int): Float {
        val timePoints = timePoints
        val values = values
        val tangents = tangents
        val n = timePoints.size
        val t = time.fastCoerceIn(timePoints[0], timePoints[n - 1])
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val y1 = values[i][j]
                val y2 = values[i + 1][j]
                val t1 = tangents[i][j]
                val t2 = tangents[i + 1][j]
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                return hermiteDifferential(h, x, y1, y2, t1, t2) / h
            }
        }
        return 0.0f // should never reach here
    }
}

/** Cubic Hermite spline */
internal fun hermiteInterpolate(
    h: Float,
    x: Float,
    y1: Float,
    y2: Float,
    t1: Float,
    t2: Float
): Float {
    val x2 = x * x
    val x3 = x2 * x
    // The exact formula is as follows:
    //
    //     -2 * x3 * y2 + 3 * x2 * y2 + 2 * x3 * y1 - 3 * x2 * y1 +
    //     y1 +
    //     h * t2 * x3 +
    //     h * t1 * x3 - h * t2 * x2 - 2 * h * t1 * x2 + h * t1 * x)
    //
    // The code below is equivalent but factored to go from 30 down to 20 instructions
    // on aarch64 devices
    return h * t1 * (x - 2 * x2 + x3) + h * t2 * (x3 - x2) + y1 - (3 * x2 - 2 * x3) * (y1 - y2)
}

/** Cubic Hermite spline slope differentiated */
internal fun hermiteDifferential(
    h: Float,
    x: Float,
    y1: Float,
    y2: Float,
    t1: Float,
    t2: Float
): Float {
    // The exact formula is as follows:
    //
    //    -6 * x2 * y2 + 6 * x * y2 + 6 * x2 * y1 - 6 * x * y1 +
    //     3 * h * t2 * x2 +
    //     3 * h * t1 * x2 - 2 * h * t2 * x - 4 * h * t1 * x + h * t1
    //
    // The code below is equivalent but factored to go from 33 down to 19 instructions
    // on aarch64 devices
    val x2 = x * x
    return h * (t1 - 2 * x * (2 * t1 + t2) + 3 * (t1 + t2) * x2) - 6 * (x - x2) * (y1 - y2)
}
