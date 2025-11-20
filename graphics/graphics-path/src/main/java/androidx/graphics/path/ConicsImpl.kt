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

import androidx.collection.FloatFloatPair
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.sqrt

private const val MAX_CONIC_TO_QUAD_COUNT = 5

internal fun conicToQuadratics(
    conicPoints: FloatArray,
    offset: Int,
    weight: Float,
    tolerance: Float,
): FloatArray {
    val conic = conicPoints.toConic(offset, weight)
    val targetQuadraticCount = conic.computeQuadraticCount(tolerance)

    val quadraticPoints = ArrayList<Float>()
    conic.splitIntoQuadratics(quadraticPoints, targetQuadraticCount)

    return quadraticPoints.toFloatArray()
}

private typealias Point = FloatFloatPair

private val Point.x
    get() = first

private val Point.y
    get() = second

private fun Point.isFinite() = x.isFinite() && y.isFinite()

private fun ArrayList<Float>.add(p: Point) {
    add(p.x)
    add(p.y)
}

private fun FloatArray.toConic(offset: Int, weight: Float): Conic =
    Conic(
        arrayOf(
            Point(this[0 + offset], this[1 + offset]),
            Point(this[2 + offset], this[3 + offset]),
            Point(this[4 + offset], this[5 + offset]),
        ),
        weight,
    )

private operator fun Point.plus(b: Point) = Point(x + b.x, y + b.y)

private operator fun Point.times(b: Point) = Point(x * b.x, y * b.y)

private fun approxEquals(a: Float, b: Float): Boolean = abs(a - b) < 0.0001f

private fun approxEquals(a: Point, b: Point) = approxEquals(a.x, b.x) && approxEquals(a.y, b.y)

private fun between(a: Float, b: Float, c: Float): Boolean = (a - b) * (c - b) <= 0.0f

private fun subdivide(src: Conic, pts: ArrayList<Float>, level: Int) {
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

        val wp1 = ww * p1
        var m = (p0 + wp1 + wp1 + p2) * scale * Point(0.5f, 0.5f)
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
            Conic(arrayOf(p0, (p0 + wp1) * scale, m), newW),
            Conic(arrayOf(m, (wp1 + p2) * scale, p2), newW),
        )
    }

    fun commonFinitePointCheck(dstPoints: ArrayList<Float>, count: Int): Int {
        val quadCount = 1 shl count

        var isFinite = true
        for (p in dstPoints) {
            if (!p.isFinite()) {
                isFinite = false
                break
            }
        }

        if (!isFinite) {
            val pointCount = dstPoints.size / 2
            val p1 = points[1]
            for (i in 1..<pointCount) {
                val index = i * 2
                dstPoints[index] = p1.x
                dstPoints[index + 1] = p1.y
            }
        }

        return quadCount
    }

    fun splitIntoQuadratics(dstPoints: ArrayList<Float>, count: Int): Int {
        dstPoints.add(points[0])
        subdivide(this, dstPoints, count)
        return commonFinitePointCheck(dstPoints, count)
    }
}
