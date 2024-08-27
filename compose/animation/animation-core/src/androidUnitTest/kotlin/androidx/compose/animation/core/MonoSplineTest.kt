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

import java.util.Arrays
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MonoSplineTest {
    @Test
    fun testCurveFit01() {
        val points = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f), floatArrayOf(2f, 0f))
        val time = floatArrayOf(0f, 5f, 10f)
        val spline = MonoSpline(time, points, Float.NaN)
        var value = spline.getPos(5f, 0).toDouble()
        assertEquals(1.0, value, 0.001)
        value = spline.getPos(7f, 0).toDouble()
        assertEquals(1.4, value, 0.001)
        value = spline.getPos(7f, 1).toDouble()
        assertEquals(0.744, value, 0.001)
    }

    @Test
    fun testMonoSpline() {
        val points =
            arrayOf(
                floatArrayOf(0f, 0f),
                floatArrayOf(1f, 1f),
                floatArrayOf(1f, 0f),
                floatArrayOf(2f, 0f),
                floatArrayOf(2f, 0f),
                floatArrayOf(3f, 0f)
            )
        val time = floatArrayOf(0f, 1f, 2f, 3f, 4f, 5f)
        val mspline = MonoSpline(time, points, Float.NaN)
        assertEquals(1.0f, mspline.getPos(1f, 0), 0.001f)
        assertEquals(1.0f, mspline.getPos(1.1f, 0), 0.001f)
        assertEquals(1.0f, mspline.getPos(1.3f, 0), 0.001f)
        assertEquals(1.0f, mspline.getPos(1.6f, 0), 0.001f)
        assertEquals(1.0f, mspline.getPos(1.9f, 0), 0.001f)
        assertEquals(2.0f, mspline.getPos(3.5f, 0), 0.001f)
        val s = plotMonoSpline(mspline, 0, 0f, 5f)
        val expect =
            """
                |***                                                         | 0.0
                |   **                                                       |
                |     **                                                     |
                |       **                                                   |
                |         ****************                                   |
                |                          **                                | 1.071
                |                            **                              |
                |                              **                            |
                |                                **                          |
                |                                  ****************          |
                |                                                   **       | 2.143
                |                                                     **     |
                |                                                       **   |
                |                                                         ** |
                |                                                           *| 3.0
                0.0                                                        5.0
                """
                .trimIndent()
        assertEquals(expect, s)
    }
}

private fun plotMonoSpline(spline: MonoSpline, splineNo: Int, start: Float, end: Float): String {
    val count = 60
    val x = FloatArray(count)
    val y = FloatArray(count)
    var c = 0
    for (i in 0 until count) {
        val t = start + (end - start) * i / (count - 1)
        x[c] = t
        y[c] = spline.getPos(t, splineNo)
        c++
    }
    return drawTextGraph(count, count / 4, x, y, false)
}

internal fun drawTextGraph(
    dimx: Int,
    dimy: Int,
    x: FloatArray,
    y: FloatArray,
    flip: Boolean
): String {
    var minX = x[0]
    var maxX = x[0]
    var minY = y[0]
    var maxY = y[0]
    var ret = ""
    for (i in x.indices) {
        minX = Math.min(minX, x[i])
        maxX = Math.max(maxX, x[i])
        minY = Math.min(minY, y[i])
        maxY = Math.max(maxY, y[i])
    }
    val c = Array(dimy) { CharArray(dimx) }
    for (i in 0 until dimy) {
        Arrays.fill(c[i], ' ')
    }
    val dimx1 = dimx - 1
    val dimy1 = dimy - 1
    for (j in x.indices) {
        val xp = (dimx1 * (x[j] - minX) / (maxX - minX)).toInt()
        val yp = (dimy1 * (y[j] - minY) / (maxY - minY)).toInt()
        c[if (flip) dimy - yp - 1 else yp][xp] = '*'
    }
    for (i in c.indices) {
        var v: Float =
            if (flip) {
                (minY - maxY) * (i / (c.size - 1.0f)) + maxY
            } else {
                (maxY - minY) * (i / (c.size - 1.0f)) + minY
            }
        v = (v * 1000 + 0.5).toInt() / 1000f
        ret +=
            if (i % 5 == 0 || i == c.size - 1) {
                "|" + String(c[i]) + "| " + v + "\n"
            } else {
                "|" + String(c[i]) + "|\n"
            }
    }
    val minStr = ((minX * 1000 + 0.5).toInt() / 1000f).toString()
    val maxStr = ((maxX * 1000 + 0.5).toInt() / 1000f).toString()
    var s = minStr + String(CharArray(dimx) { ' ' })
    s = s.substring(0, dimx - maxStr.length + 2) + maxStr + '\n'
    return (ret + s).trimIndent()
}
