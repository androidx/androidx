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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.animation.core

import androidx.compose.ui.util.fastCoerceIn
import kotlin.jvm.JvmField
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * This provides a curve fit system that stitches the x,y path together with quarter ellipses.
 *
 * @param arcModes Array of arc mode values. Expected to be of size n - 1.
 * @param timePoints Array of timestamps. Expected to be of size n. Seconds preferred.
 * @param y Array of values (of size n), where each value is spread on a [FloatArray] for each of
 *   its dimensions, expected to be of even size since two values are needed to interpolate arcs.
 */
internal class ArcSpline(arcModes: IntArray, timePoints: FloatArray, y: Array<FloatArray>) {
    private val arcs: Array<Array<Arc>>
    private val isExtrapolate = true

    init {
        var mode = StartVertical
        var last = StartVertical

        arcs =
            Array(timePoints.size - 1) { i ->
                when (arcModes[i]) {
                    ArcSplineArcStartVertical -> {
                        mode = StartVertical
                        last = mode
                    }
                    ArcSplineArcStartHorizontal -> {
                        mode = StartHorizontal
                        last = mode
                    }
                    ArcSplineArcStartFlip -> {
                        mode = if (last == StartVertical) StartHorizontal else StartVertical
                        last = mode
                    }
                    ArcSplineArcStartLinear -> mode = StartLinear
                    ArcSplineArcAbove -> mode = UpArc
                    ArcSplineArcBelow -> mode = DownArc
                }

                val yArray = y[i]
                val yArray1 = y[i + 1]
                val timeArray = timePoints[i]
                val timeArray1 = timePoints[i + 1]

                val dim = yArray.size / 2 + yArray.size % 2
                Array(dim) { j ->
                    val k = j * 2
                    Arc(
                        mode = mode,
                        time1 = timeArray,
                        time2 = timeArray1,
                        x1 = yArray[k],
                        y1 = yArray[k + 1],
                        x2 = yArray1[k],
                        y2 = yArray1[k + 1]
                    )
                }
            }
    }

    /** get the values of the at t point in time. */
    fun getPos(time: Float, v: FloatArray) {
        var t = time
        val arcs = arcs
        val lastIndex = arcs.size - 1
        val start = arcs[0][0].time1
        val end = arcs[lastIndex][0].time2
        val size = v.size

        if (isExtrapolate) {
            if (t < start || t > end) {
                val p: Int
                val t0: Float
                if (t > end) {
                    p = lastIndex
                    t0 = end
                } else {
                    p = 0
                    t0 = start
                }
                val dt = t - t0

                var i = 0
                var j = 0
                while (i < size - 1) {
                    val arc = arcs[p][j]
                    if (arc.isLinear) {
                        v[i] = arc.getLinearX(t0) + dt * arc.linearDX
                        v[i + 1] = arc.getLinearY(t0) + dt * arc.linearDY
                    } else {
                        arc.setPoint(t0)
                        v[i] = arc.calcX() + dt * arc.calcDX()
                        v[i + 1] = arc.calcY() + dt * arc.calcDY()
                    }
                    i += 2
                    j++
                }
                return
            }
        } else {
            t = max(t, start)
            t = min(t, end)
        }

        // TODO: Consider passing the index from the caller to improve performance
        var populated = false
        for (i in arcs.indices) {
            var k = 0
            var j = 0
            while (j < size - 1) {
                val arc = arcs[i][k]
                if (t <= arc.time2) {
                    if (arc.isLinear) {
                        v[j] = arc.getLinearX(t)
                        v[j + 1] = arc.getLinearY(t)
                    } else {
                        arc.setPoint(t)
                        v[j] = arc.calcX()
                        v[j + 1] = arc.calcY()
                    }
                    populated = true
                }
                j += 2
                k++
            }
            if (populated) {
                return
            }
        }
    }

    /** Get the differential which of the curves at point t */
    fun getSlope(time: Float, v: FloatArray) {
        val arcs = arcs
        val t = time.fastCoerceIn(arcs[0][0].time1, arcs[arcs.size - 1][0].time2)
        val size = v.size

        var populated = false
        // TODO: Consider passing the index from the caller to improve performance
        for (i in arcs.indices) {
            var j = 0
            var k = 0
            while (j < size - 1) {
                val arc = arcs[i][k]
                if (t <= arc.time2) {
                    if (arc.isLinear) {
                        v[j] = arc.linearDX
                        v[j + 1] = arc.linearDY
                    } else {
                        arc.setPoint(t)
                        v[j] = arc.calcDX()
                        v[j + 1] = arc.calcDY()
                    }
                    populated = true
                }
                j += 2
                k++
            }
            if (populated) {
                return
            }
        }
    }

    class Arc
    internal constructor(
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
        private val arcVelocity: Float
        private val vertical: Float

        @JvmField internal val ellipseA: Float
        @JvmField internal val ellipseB: Float

        @JvmField internal val isLinear: Boolean

        // also used to cache the slope in the unused center
        @JvmField internal val ellipseCenterX: Float
        // also used to cache the slope in the unused center
        @JvmField internal val ellipseCenterY: Float

        internal inline val linearDX: Float
            get() = ellipseCenterX

        internal inline val linearDY: Float
            get() = ellipseCenterY

        init {
            val dx = x2 - x1
            val dy = y2 - y1
            val isVertical =
                when (mode) {
                    StartVertical -> true
                    UpArc -> dy < 0
                    DownArc -> dy > 0
                    else -> false
                }
            vertical = if (isVertical) -1.0f else 1.0f
            oneOverDeltaTime = 1 / (this.time2 - this.time1)
            lut = FloatArray(LutSize)

            var isLinear = mode == StartLinear
            if (isLinear || abs(dx) < Epsilon || abs(dy) < Epsilon) {
                isLinear = true
                arcDistance = hypot(dy, dx)
                arcVelocity = arcDistance * oneOverDeltaTime
                ellipseCenterX = dx * oneOverDeltaTime // cache the slope in the unused center
                ellipseCenterY = dy * oneOverDeltaTime // cache the slope in the unused center
                ellipseA = Float.NaN
                ellipseB = Float.NaN
            } else {
                ellipseA = dx * vertical
                ellipseB = dy * -vertical
                ellipseCenterX = if (isVertical) x2 else x1
                ellipseCenterY = if (isVertical) y1 else y2
                buildTable(x1, y1, x2, y2)
                arcVelocity = arcDistance * oneOverDeltaTime
            }
            this.isLinear = isLinear
        }

        fun setPoint(time: Float) {
            val angle = calcAngle(time)
            tmpSinAngle = sin(angle)
            tmpCosAngle = cos(angle)
        }

        private inline fun calcAngle(time: Float): Float {
            val percent = (if (vertical == -1.0f) time2 - time else time - time1) * oneOverDeltaTime
            return HalfPi * lookup(percent)
        }

        inline fun calcX(): Float {
            return ellipseCenterX + ellipseA * tmpSinAngle
        }

        inline fun calcY(): Float {
            return ellipseCenterY + ellipseB * tmpCosAngle
        }

        fun calcDX(): Float {
            val vx = ellipseA * tmpCosAngle
            val vy = -ellipseB * tmpSinAngle
            val norm = arcVelocity / hypot(vx, vy)
            return vx * vertical * norm
        }

        fun calcDY(): Float {
            val vx = ellipseA * tmpCosAngle
            val vy = -ellipseB * tmpSinAngle
            val norm = arcVelocity / hypot(vx, vy)
            return vy * vertical * norm
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

        private fun lookup(v: Float): Float {
            if (v <= 0) {
                return 0.0f
            }
            if (v >= 1) {
                return 1.0f
            }
            val pos = v * (LutSize - 1)
            val iv = pos.toInt()
            val off = pos - pos.toInt()
            return lut[iv] + off * (lut[iv + 1] - lut[iv])
        }

        // Internal to prevent inlining from R8
        @Suppress("MemberVisibilityCanBePrivate")
        internal fun buildTable(x1: Float, y1: Float, x2: Float, y2: Float) {
            val a = x2 - x1
            val b = y1 - y2
            var lx = 0f
            var ly = b // == b * cos(0), because we skip index 0 in the loops below
            var dist = 0f

            val ourPercent = OurPercentCache
            val lastIndex = ourPercent.size - 1
            val lut = lut

            for (i in 1..lastIndex) {
                val angle = toRadians(90.0 * i / lastIndex).toFloat()
                val s = sin(angle)
                val c = cos(angle)
                val px = a * s
                val py = b * c
                dist += hypot((px - lx), (py - ly)) // we don't want to compute and store dist
                ourPercent[i] = dist // for i == 0
                lx = px
                ly = py
            }

            arcDistance = dist
            for (i in 1..lastIndex) {
                ourPercent[i] /= dist
            }

            val lutLastIndex = (LutSize - 1).toFloat()
            for (i in lut.indices) {
                val pos = i / lutLastIndex
                val index = binarySearch(ourPercent, pos)
                if (index >= 0) {
                    lut[i] = index / lutLastIndex
                } else if (index == -1) {
                    lut[i] = 0f
                } else {
                    val p1 = -index - 2
                    val p2 = -index - 1
                    val ans =
                        (p1 + (pos - ourPercent[p1]) / (ourPercent[p2] - ourPercent[p1])) /
                            lastIndex
                    lut[i] = ans
                }
            }
        }
    }
}

internal const val ArcSplineArcStartLinear = 0
internal const val ArcSplineArcStartVertical = 1
internal const val ArcSplineArcStartHorizontal = 2
internal const val ArcSplineArcStartFlip = 3
internal const val ArcSplineArcBelow = 4
internal const val ArcSplineArcAbove = 5

private const val StartVertical = 1
private const val StartHorizontal = 2
private const val StartLinear = 3
private const val DownArc = 4
private const val UpArc = 5
private const val LutSize = 101

private const val Epsilon = 0.001f
private const val HalfPi = (PI * 0.5).toFloat()

private val OurPercentCache: FloatArray = FloatArray(91)

internal expect inline fun toRadians(value: Double): Double

internal expect inline fun binarySearch(array: FloatArray, position: Float): Int
