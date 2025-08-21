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
package androidx.graphics.path

import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.sqrt

private const val MAX_CONIC_TO_QUAD_COUNT = 5

internal fun conicToQuadratics(
    conicPoints: FloatArray,
    offset: Int,
    quadraticPoints: FloatArray,
    weight: Float,
    tolerance: Float,
): Int {
    val conic =
        Conic(
            arrayOf(
                Point(conicPoints[0 + offset], conicPoints[1 + offset]),
                Point(conicPoints[2 + offset], conicPoints[3 + offset]),
                Point(conicPoints[4 + offset], conicPoints[5 + offset]),
            ),
            weight,
        )

    val count = conic.computeQuadraticCount(tolerance)
    val quadraticCount = 1 shl count
    if (quadraticCount > quadraticPoints.size) {
        // Buffer not large enough; return necessary size to resize and try again
        return quadraticCount
    }

    val dstPoints = ArrayList<Point>()
    val finalCount = conic.splitIntoQuadratics(dstPoints, count)

    var index = 0
    for (p in dstPoints) {
        quadraticPoints[index++] = p.x
        quadraticPoints[index++] = p.y
    }

    return finalCount
}

private class Point(val x: Float, val y: Float) {
    fun isFinite() = x.isFinite() && y.isFinite()
}

private fun add(a: Point, b: Point) = Point(a.x + b.x, a.y + b.y)

private fun mul(a: Point, b: Point) = Point(a.x * b.x, a.y * b.y)

private fun approxEquals(a: Float, b: Float): Boolean = abs(a - b) < 0.0001f

private fun approxEquals(a: Point, b: Point) = approxEquals(a.x, b.x) && approxEquals(a.y, b.y)

private fun between(a: Float, b: Float, c: Float): Boolean = (a - b) * (c - b) <= 0.0f

private fun subdivide(src: Conic, pts: ArrayList<Point>, level: Int) {
    if (level == 0) {
        pts.add(src.points[1])
        pts.add(src.points[2])
    } else {
        val s = src.split()
        val startY = src.points[0].y
        val endY = src.points[2].y
        if (between(startY, src.points[1].y, endY)) {
            val midY = s.a.points[2].y
            if (!between(startY, midY, endY)) {
                val closerY = if (abs(midY - startY) < abs(midY - endY)) startY else endY
                s.a.points[2] = Point(s.a.points[2].x, closerY)
                s.b.points[0] = Point(s.b.points[0].x, closerY)
            }
            if (!between(startY, s.a.points[1].y, s.a.points[2].y)) {
                s.a.points[1] = Point(s.a.points[1].x, startY)
            }
            if (!between(s.b.points[0].y, s.b.points[1].y, endY)) {
                s.b.points[1] = Point(s.b.points[1].x, endY)
            }
        }
        subdivide(s.a, pts, level - 1)
        subdivide(s.b, pts, level - 1)
    }
}

private class Conic(val points: Array<Point>, val weight: Float) {
    fun computeQuadraticCount(tolerance: Float): Int {
        val a = weight - 1.0f
        val k = a / (4.0f * (2.0f + a))
        val x = k * (points[0].x - 2.0f * points[1].x + points[2].x)
        val y = k * (points[0].y - 2.0f * points[1].y + points[2].y)

        var error = sqrt(x * x + y * y)
        var count = 0
        while (count < MAX_CONIC_TO_QUAD_COUNT) {
            if (error <= tolerance) {
                break
            }
            error *= 0.25f
            count++
        }
        return count
    }

    class SplitResult(val a: Conic, val b: Conic)

    fun split(): SplitResult {
        val scale = Point(1.0f / (1.0f + weight), 1.0f / (1.0f + weight))
        val newW = sqrt(0.5f + weight * 0.5f)

        val p0 = points[0]
        val p1 = points[1]
        val p2 = points[2]
        val ww = Point(weight, weight)

        val wp1 = mul(ww, p1)
        var m = mul(mul(add(add(p0, add(wp1, wp1)), p2), scale), Point(0.5f, 0.5f))
        if (!m.isFinite()) {
            val wD = weight.toDouble()
            val w2 = wD * 2.0
            val scaleHalf = 1.0 / (1.0 + wD) * 0.5
            m =
                Point(
                    ((p0.x.toDouble() + w2 * p1.x.toDouble() + p2.x.toDouble()) * scaleHalf)
                        .toFloat(),
                    ((p0.y.toDouble() + w2 * p1.y.toDouble() + p2.y.toDouble()) * scaleHalf)
                        .toFloat(),
                )
        }
        return SplitResult(
            Conic(arrayOf(p0, mul(add(p0, wp1), scale), m), newW),
            Conic(arrayOf(m, mul(add(wp1, p2), scale), p2), newW),
        )
    }

    fun commonFinitePointCheck(dstPoints: ArrayList<Point>, count: Int): Int {
        val quadCount = 1 shl count
        val pointCount = 2 * quadCount + 1

        var isFinite = true
        for (p in dstPoints) {
            if (!p.isFinite()) {
                isFinite = false
                break
            }
        }

        if (!isFinite) {
            for (i in 1..<pointCount) {
                dstPoints[i] = points[1]
            }
        }

        return quadCount
    }

    fun splitIntoQuadratics(dstPoints: ArrayList<Point>, count: Int): Int {
        dstPoints.add(points[0])

        if (count > MAX_CONIC_TO_QUAD_COUNT) {
            val s = split()

            if (
                approxEquals(s.a.points[1], s.a.points[2]) &&
                    approxEquals(s.b.points[0], s.b.points[1])
            ) {
                dstPoints.add(s.a.points[1])
                dstPoints.add(s.a.points[1])
                dstPoints.add(s.a.points[1])
                dstPoints.add(s.b.points[2])
                return commonFinitePointCheck(dstPoints, 1)
            }
        }

        subdivide(this, dstPoints, count)
        return commonFinitePointCheck(dstPoints, count)
    }
}
