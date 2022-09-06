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

package androidx.graphics.shapes

import android.graphics.Matrix
import android.graphics.PointF
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

@SmallTest
class CubicShapeTest {

    // ~circular arc from (1, 0) to (0, 1)
    val point0 = PointF(1f, 0f)
    val point1 = PointF(1f, .5f)
    val point2 = PointF(.5f, 1f)
    val point3 = PointF(0f, 1f)

    // ~circular arc from (0, 1) to (-1, 0)
    val point4 = PointF(0f, 1f)
    val point5 = PointF(-.5f, 1f)
    val point6 = PointF(-.5f, .5f)
    val point7 = PointF(-1f, 0f)

    val cubic0 = Cubic(point0, point1, point2, point3)
    val cubic1 = Cubic(point4, point5, point6, point7)

    fun getClosingCubic(first: Cubic, last: Cubic): Cubic {
        return Cubic(last.p3, last.p3, first.p0, first.p0)
    }

    @Test
    fun constructionTest() {
        var shape = CubicShape(listOf(cubic0, getClosingCubic(cubic0, cubic0)))
        assertNotNull(shape)

        shape = CubicShape(listOf(cubic0, cubic1, getClosingCubic(cubic0, cubic1)))
        assertNotNull(shape)
        val shape1 = CubicShape(shape)
        assertEquals(shape, shape1)
    }

    @Test
    fun pathTest() {
        val shape = CubicShape(listOf(cubic0, cubic1, getClosingCubic(cubic0, cubic1)))
        val path = shape.toPath()
        assertFalse(path.isEmpty)
    }

    @Test
    fun boundsTest() {
        val shape = CubicShape(listOf(cubic0, cubic1, getClosingCubic(cubic0, cubic1)))
        val bounds = shape.bounds
        assertPointsEqualish(PointF(-1f, 0f), PointF(bounds.left, bounds.top))
        assertPointsEqualish(PointF(1f, 1f), PointF(bounds.right, bounds.bottom))
    }

    @Test
    fun cubicsTest() {
        val shape = CubicShape(listOf(cubic0, cubic1, getClosingCubic(cubic0, cubic1)))
        val cubics = shape.cubics
        assertCubicsEqua1ish(cubic0, cubics[0])
        assertCubicsEqua1ish(cubic1, cubics[1])
    }

    @Test
    fun transformTest() {
        val shape = CubicShape(listOf(cubic0, getClosingCubic(cubic0, cubic0)))

        // First, make sure the shape doesn't change when transformed by the identity
        val identity = Matrix()
        shape.transform(identity)
        val cubics = shape.cubics
        assertCubicsEqua1ish(cubic0, cubics[0])

        // Now create a matrix which translates points by (1, 2) and make sure
        // the shape is translated similarly by it
        val translator = Matrix()
        translator.setTranslate(1f, 2f)
        val translatedPoints = floatArrayOf(point0.x, point0.y, point1.x, point1.y,
            point2.x, point2.y, point3.x, point3.y)
        translator.mapPoints(translatedPoints)
        shape.transform(translator)
        val cubic = shape.cubics[0]
        assertPointsEqualish(PointF(translatedPoints[0], translatedPoints[1]), cubic.p0)
        assertPointsEqualish(PointF(translatedPoints[2], translatedPoints[3]), cubic.p1)
        assertPointsEqualish(PointF(translatedPoints[4], translatedPoints[5]), cubic.p2)
        assertPointsEqualish(PointF(translatedPoints[6], translatedPoints[7]), cubic.p3)
    }
}