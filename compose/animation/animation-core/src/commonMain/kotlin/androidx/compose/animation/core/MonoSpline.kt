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

import kotlin.math.hypot

/**
 * This performs a spline interpolation in multiple dimensions
 * time is an array of all positions and y is a list of arrays each with the values at each point
 */
@ExperimentalAnimationSpecApi
internal class MonoSpline(time: FloatArray, y: List<FloatArray>) {
    private val timePoints: FloatArray
    private val values: ArrayList<FloatArray>
    private val tangents: ArrayList<FloatArray>
    private val isExtrapolate = true
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
        values = copyData(y)
        tangents = tangent
    }

    /**
     * @param a number of arrays
     * @param b dimension of Float arrays
     */
    private fun makeFloatArray(a: Int, b: Int): ArrayList<FloatArray> {
        val ret = ArrayList<FloatArray>() // new Float[a][b];
        for (i in 0 until a) {
            ret.add(FloatArray(b))
        }
        return ret
    }

    private fun copyData(y: List<FloatArray>): ArrayList<FloatArray> {
        val ret = ArrayList<FloatArray>()
        ret.addAll(y)
        return ret
    }

    /**
     * Get the values of the splines at t.
     * v is an array of all values
     */
    fun getPos(t: Float, v: FloatArray) {
        val n = timePoints.size
        val dim = values[0].size
        if (isExtrapolate) {
            if (t <= timePoints[0]) {
                getSlope(timePoints[0], slopeTemp)
                for (j in 0 until dim) {
                    v[j] = values[0][j] + (t - timePoints[0]) * slopeTemp[j]
                }
                return
            }
            if (t >= timePoints[n - 1]) {
                getSlope(timePoints[n - 1], slopeTemp)
                for (j in 0 until dim) {
                    v[j] = values[n - 1][j] + (t - timePoints[n - 1]) * slopeTemp[j]
                }
                return
            }
        } else {
            if (t <= timePoints[0]) {
                for (j in 0 until dim) {
                    v[j] = values[0][j]
                }
                return
            }
            if (t >= timePoints[n - 1]) {
                for (j in 0 until dim) {
                    v[j] = values[n - 1][j]
                }
                return
            }
        }
        for (i in 0 until n - 1) {
            if (t == timePoints[i]) {
                for (j in 0 until dim) {
                    v[j] = values[i][j]
                }
            }
            if (t < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = interpolate(h, x, y1, y2, t1, t2)
                }
                return
            }
        }
    }

    /**
     * get the value of the j'th spline at time t
     */
    fun getPos(t: Float, j: Int): Float {
        val n = timePoints.size
        if (isExtrapolate) {
            if (t <= timePoints[0]) {
                return values[0][j] + (t - timePoints[0]) * getSlope(timePoints[0], j)
            }
            if (t >= timePoints[n - 1]) {
                return values[n - 1][j] + (t - timePoints[n - 1]) * getSlope(timePoints[n - 1], j)
            }
        } else {
            if (t <= timePoints[0]) {
                return values[0][j]
            }
            if (t >= timePoints[n - 1]) {
                return values[n - 1][j]
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
                return interpolate(h, x, y1, y2, t1, t2)
            }
        }
        return 0.0f // should never reach here
    }

    /**
     * Populate the values of the spline at time [t] into the given AnimationVector [v].
     */
    fun getPos(t: Float, v: AnimationVector) {
        val n = timePoints.size
        val dim = values[0].size
        if (isExtrapolate) {
            if (t <= timePoints[0]) {
                getSlope(timePoints[0], slopeTemp)
                for (j in 0 until dim) {
                    v[j] = values[0][j] + (t - timePoints[0]) * slopeTemp[j]
                }
                return
            }
            if (t >= timePoints[n - 1]) {
                getSlope(timePoints[n - 1], slopeTemp)
                for (j in 0 until dim) {
                    v[j] = values[n - 1][j] + (t - timePoints[n - 1]) * slopeTemp[j]
                }
                return
            }
        } else {
            if (t <= timePoints[0]) {
                for (j in 0 until dim) {
                    v[j] = values[0][j]
                }
                return
            }
            if (t >= timePoints[n - 1]) {
                for (j in 0 until dim) {
                    v[j] = values[n - 1][j]
                }
                return
            }
        }
        for (i in 0 until n - 1) {
            if (t == timePoints[i]) {
                for (j in 0 until dim) {
                    v[j] = values[i][j]
                }
            }
            if (t < timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = interpolate(h, x, y1, y2, t1, t2)
                }
                return
            }
        }
    }

    /**
     * Get the differential of the value at time
     * fill an array of slopes for each spline
     */
    fun getSlope(time: Float, v: FloatArray) {
        var t = time
        val n = timePoints.size
        val dim = values[0].size
        if (t <= timePoints[0]) {
            t = timePoints[0]
        } else if (t >= timePoints[n - 1]) {
            t = timePoints[n - 1]
        }
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = diff(h, x, y1, y2, t1, t2) / h
                }
                break
            }
        }
        return
    }

    /**
     * Populate the differential values of the spline at the given [time] in to the given
     * AnimationVector [v].
     */
    fun getSlope(time: Float, v: AnimationVector) {
        var t = time
        val n = timePoints.size
        val dim = values[0].size
        if (t <= timePoints[0]) {
            t = timePoints[0]
        } else if (t >= timePoints[n - 1]) {
            t = timePoints[n - 1]
        }
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                for (j in 0 until dim) {
                    val y1 = values[i][j]
                    val y2 = values[i + 1][j]
                    val t1 = tangents[i][j]
                    val t2 = tangents[i + 1][j]
                    v[j] = diff(h, x, y1, y2, t1, t2) / h
                }
                break
            }
        }
        return
    }

    private fun getSlope(time: Float, j: Int): Float {
        var t = time
        val n = timePoints.size
        if (t < timePoints[0]) {
            t = timePoints[0]
        } else if (t >= timePoints[n - 1]) {
            t = timePoints[n - 1]
        }
        for (i in 0 until n - 1) {
            if (t <= timePoints[i + 1]) {
                val h = timePoints[i + 1] - timePoints[i]
                val x = (t - timePoints[i]) / h
                val y1 = values[i][j]
                val y2 = values[i + 1][j]
                val t1 = tangents[i][j]
                val t2 = tangents[i + 1][j]
                return diff(h, x, y1, y2, t1, t2) / h
            }
        }
        return 0.0f // should never reach here
    }

    /**
     * Cubic Hermite spline
     */
    private fun interpolate(
        h: Float,
        x: Float,
        y1: Float,
        y2: Float,
        t1: Float,
        t2: Float
    ): Float {
        val x2 = x * x
        val x3 = x2 * x
        return (-2 * x3 * y2 +
            3 * x2 * y2 +
            2 * x3 * y1 -
            3 * x2 * y1 +
            y1 + h * t2 * x3 +
            h * t1 * x3 -
            h * t2 * x2 -
            2 * h * t1 * x2 +
            h * t1 * x)
    }

    /**
     * Cubic Hermite spline slope differentiated
     */
    private fun diff(h: Float, x: Float, y1: Float, y2: Float, t1: Float, t2: Float): Float {
        val x2 = x * x
        return (-6 * x2 * y2 +
            6 * x * y2 +
            6 * x2 * y1 -
            6 * x * y1 +
            3 * h * t2 * x2 +
            3 * h * t1 * x2 -
            2 * h * t2 * x -
            4 * h * t1 * x + h * t1)
    }
}
