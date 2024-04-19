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

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * This provides a curve fit system that stitches the x,y path together with
 * quarter ellipses.
 *
 * @param arcModes Array of arc mode values. Expected to be of size n - 1.
 * @param timePoints Array of timestamps. Expected to be of size n. Seconds preferred.
 * @param y Array of values (of size n), where each value is spread on a [FloatArray] for each of
 * its dimensions, expected to be of even size since two values are needed to interpolate arcs.
 */
@ExperimentalAnimationSpecApi
internal class ArcSpline(
    arcModes: IntArray,
    timePoints: FloatArray,
    y: Array<FloatArray>
) {
    private val arcs: Array<Array<Arc>>
    private val isExtrapolate = true

    init {
        var mode = StartVertical
        var last = StartVertical

        arcs = Array(timePoints.size - 1) { i ->
            when (arcModes[i]) {
                ArcStartVertical -> {
                    mode = StartVertical
                    last = mode
                }

                ArcStartHorizontal -> {
                    mode = StartHorizontal
                    last = mode
                }

                ArcStartFlip -> {
                    mode = if (last == StartVertical) StartHorizontal else StartVertical
                    last = mode
                }

                ArcStartLinear -> mode = StartLinear
                ArcAbove -> mode = UpArc
                ArcBelow -> mode = DownArc
            }
            val dim = y[i].size / 2 + y[i].size % 2
            Array(dim) { j ->
                val k = j * 2
                Arc(
                    mode = mode,
                    time1 = timePoints[i],
                    time2 = timePoints[i + 1],
                    x1 = y[i][k],
                    y1 = y[i][k + 1],
                    x2 = y[i + 1][k],
                    y2 = y[i + 1][k + 1]
                )
            }
        }
    }

    /**
     * get the values of the at t point in time.
     */
    fun getPos(time: Float, v: FloatArray) {
        var t = time
        if (isExtrapolate) {
            if (t < arcs[0][0].time1 || t > arcs[arcs.size - 1][0].time2) {
                val p: Int
                val t0: Float
                if (t > arcs[arcs.size - 1][0].time2) {
                    p = arcs.size - 1
                    t0 = arcs[arcs.size - 1][0].time2
                } else {
                    p = 0
                    t0 = arcs[0][0].time1
                }
                val dt = t - t0

                var i = 0
                var j = 0
                while (i < v.size) {
                    if (arcs[p][j].isLinear) {
                        v[i] = arcs[p][j].getLinearX(t0) + dt * arcs[p][j].getLinearDX()
                        v[i + 1] = arcs[p][j].getLinearY(t0) + dt * arcs[p][j].getLinearDY()
                    } else {
                        arcs[p][j].setPoint(t0)
                        v[i] = arcs[p][j].calcX() + dt * arcs[p][j].calcDX()
                        v[i + 1] = arcs[p][j].calcY() + dt * arcs[p][j].calcDY()
                    }
                    i += 2
                    j++
                }
                return
            }
        } else {
            if (t < arcs[0][0].time1) {
                t = arcs[0][0].time1
            }
            if (t > arcs[arcs.size - 1][0].time2) {
                t = arcs[arcs.size - 1][0].time2
            }
        }

        // TODO: Consider passing the index from the caller to improve performance
        var populated = false
        for (i in arcs.indices) {
            var k = 0
            var j = 0
            while (j < v.size) {
                if (t <= arcs[i][k].time2) {
                    if (arcs[i][k].isLinear) {
                        v[j] = arcs[i][k].getLinearX(t)
                        v[j + 1] = arcs[i][k].getLinearY(t)
                        populated = true
                    } else {
                        arcs[i][k].setPoint(t)
                        v[j] = arcs[i][k].calcX()
                        v[j + 1] = arcs[i][k].calcY()
                        populated = true
                    }
                }
                j += 2
                k++
            }
            if (populated) {
                return
            }
        }
    }

    /**
     * Get the differential which of the curves at point t
     */
    fun getSlope(time: Float, v: FloatArray) {
        var t = time
        if (t < arcs[0][0].time1) {
            t = arcs[0][0].time1
        } else if (t > arcs[arcs.size - 1][0].time2) {
            t = arcs[arcs.size - 1][0].time2
        }
        var populated = false
        // TODO: Consider passing the index from the caller to improve performance
        for (i in arcs.indices) {
            var j = 0
            var k = 0
            while (j < v.size) {
                if (t <= arcs[i][k].time2) {
                    if (arcs[i][k].isLinear) {
                        v[j] = arcs[i][k].getLinearDX()
                        v[j + 1] = arcs[i][k].getLinearDY()
                        populated = true
                    } else {
                        arcs[i][k].setPoint(t)
                        v[j] = arcs[i][k].calcDX()
                        v[j + 1] = arcs[i][k].calcDY()
                        populated = true
                    }
                }
                j += 2
                k++
            }
            if (populated) {
                return
            }
        }
    }

    class Arc internal constructor(
        mode: Int,
        val time1: Float,
        val time2: Float,
        private val x1: Float,
        private val y1: Float,
        private val x2: Float,
        private val y2: Float
    ) {
        private var arcDistance = 0f
        private var tmpSinAngle = 0f
        private var tmpCosAngle = 0f

        private val lut: FloatArray
        private val oneOverDeltaTime: Float
        private val ellipseA: Float
        private val ellipseB: Float
        private val ellipseCenterX: Float // also used to cache the slope in the unused center
        private val ellipseCenterY: Float // also used to cache the slope in the unused center
        private val arcVelocity: Float
        private val isVertical: Boolean

        val isLinear: Boolean

        init {
            val dx = x2 - x1
            val dy = y2 - y1
            isVertical = when (mode) {
                StartVertical -> true
                UpArc -> dy < 0
                DownArc -> dy > 0
                else -> false
            }
            oneOverDeltaTime = 1 / (this.time2 - this.time1)

            var isLinear = false
            if (StartLinear == mode) {
                isLinear = true
            }
            if (isLinear || abs(dx) < Epsilon || abs(dy) < Epsilon) {
                isLinear = true
                arcDistance = hypot(dy, dx)
                arcVelocity = arcDistance * oneOverDeltaTime
                ellipseCenterX =
                    dx / (this.time2 - this.time1) // cache the slope in the unused center
                ellipseCenterY =
                    dy / (this.time2 - this.time1) // cache the slope in the unused center
                lut = FloatArray(101)
                ellipseA = Float.NaN
                ellipseB = Float.NaN
            } else {
                lut = FloatArray(101)
                ellipseA = dx * if (isVertical) -1 else 1
                ellipseB = dy * if (isVertical) 1 else -1
                ellipseCenterX = if (isVertical) x2 else x1
                ellipseCenterY = if (isVertical) y1 else y2
                buildTable(x1, y1, x2, y2)
                arcVelocity = arcDistance * oneOverDeltaTime
            }
            this.isLinear = isLinear
        }

        fun setPoint(time: Float) {
            val percent = (if (isVertical) time2 - time else time - time1) * oneOverDeltaTime
            val angle = Math.PI.toFloat() * 0.5f * lookup(percent)
            tmpSinAngle = sin(angle)
            tmpCosAngle = cos(angle)
        }

        fun calcX(): Float {
            return ellipseCenterX + ellipseA * tmpSinAngle
        }

        fun calcY(): Float {
            return ellipseCenterY + ellipseB * tmpCosAngle
        }

        fun calcDX(): Float {
            val vx = ellipseA * tmpCosAngle
            val vy = -ellipseB * tmpSinAngle
            val norm = arcVelocity / hypot(vx, vy)
            return if (isVertical) -vx * norm else vx * norm
        }

        fun calcDY(): Float {
            val vx = ellipseA * tmpCosAngle
            val vy = -ellipseB * tmpSinAngle
            val norm = arcVelocity / hypot(vx, vy)
            return if (isVertical) -vy * norm else vy * norm
        }

        fun getLinearX(time: Float): Float {
            var t = time
            t = (t - time1) * oneOverDeltaTime
            return x1 + t * (x2 - x1)
        }

        fun getLinearY(time: Float): Float {
            var t = time
            t = (t - time1) * oneOverDeltaTime
            return y1 + t * (y2 - y1)
        }

        fun getLinearDX(): Float {
            return ellipseCenterX
        }

        fun getLinearDY(): Float {
            return ellipseCenterY
        }

        private fun lookup(v: Float): Float {
            if (v <= 0) {
                return 0.0f
            }
            if (v >= 1) {
                return 1.0f
            }
            val pos = v * (lut.size - 1)
            val iv = pos.toInt()
            val off = pos - pos.toInt()
            return lut[iv] + off * (lut[iv + 1] - lut[iv])
        }

        private fun buildTable(x1: Float, y1: Float, x2: Float, y2: Float) {
            val a = x2 - x1
            val b = y1 - y2
            var lx = 0f
            var ly = 0f
            var dist = 0f
            for (i in ourPercent.indices) {
                val angle = Math.toRadians(90.0 * i / (ourPercent.size - 1)).toFloat()
                val s = sin(angle)
                val c = cos(angle)
                val px = a * s
                val py = b * c
                if (i > 0) {
                    dist += hypot((px - lx), (py - ly))
                    ourPercent[i] = dist
                }
                lx = px
                ly = py
            }
            arcDistance = dist
            for (i in ourPercent.indices) {
                ourPercent[i] /= dist
            }
            for (i in lut.indices) {
                val pos = i / (lut.size - 1).toFloat()
                val index = ourPercent.binarySearch(pos)
                if (index >= 0) {
                    lut[i] = index / (ourPercent.size - 1).toFloat()
                } else if (index == -1) {
                    lut[i] = 0f
                } else {
                    val p1 = -index - 2
                    val p2 = -index - 1
                    val ans =
                        (p1 + (pos - ourPercent[p1]) / (ourPercent[p2] - ourPercent[p1])) /
                            (ourPercent.size - 1)
                    lut[i] = ans
                }
            }
        }

        companion object {
            private var _ourPercent: FloatArray? = null
            private val ourPercent: FloatArray
                get() {
                    if (_ourPercent != null) {
                        return _ourPercent!!
                    }
                    _ourPercent = FloatArray(91)
                    return _ourPercent!!
                }
            private const val Epsilon = 0.001f
        }
    }

    companion object {
        const val ArcStartVertical = 1
        const val ArcStartHorizontal = 2
        const val ArcStartFlip = 3
        const val ArcBelow = 4
        const val ArcAbove = 5
        const val ArcStartLinear = 0
        private const val StartVertical = 1
        private const val StartHorizontal = 2
        private const val StartLinear = 3
        private const val DownArc = 4
        private const val UpArc = 5
    }
}
