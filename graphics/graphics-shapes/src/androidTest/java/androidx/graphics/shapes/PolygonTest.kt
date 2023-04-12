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
import androidx.core.graphics.plus
import androidx.core.graphics.times
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@SmallTest
class PolygonTest {

    val square = RoundedPolygon(4)

    @Test
    fun constructionTest() {
        // We can't be too specific on how exactly the square is constructed, but
        // we can at least test whether all points are within the unit square
        var min = PointF(-1f, -1f)
        var max = PointF(1f, 1f)
        assertInBounds(square.toCubicShape(), min, max)

        val doubleSquare = RoundedPolygon(4, 2f)
        min = min * 2f
        max = max * 2f
        assertInBounds(doubleSquare.toCubicShape(), min, max)

        val offsetSquare = RoundedPolygon(4, center = PointF(1f, 2f))
        min = PointF(0f, 1f)
        max = PointF(2f, 3f)
        assertInBounds(offsetSquare.toCubicShape(), min, max)

        val squareCopy = RoundedPolygon(square)
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(squareCopy.toCubicShape(), min, max)

        val p0 = PointF(1f, 0f)
        val p1 = PointF(0f, 1f)
        val p2 = PointF(-1f, 0f)
        val p3 = PointF(0f, -1f)
        val manualSquare = RoundedPolygon(listOf(p0, p1, p2, p3))
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(manualSquare.toCubicShape(), min, max)

        val offset = PointF(1f, 2f)
        val manualSquareOffset = RoundedPolygon(
            vertices = listOf(p0 + offset, p1 + offset, p2 + offset, p3 + offset), center = offset)
        min = PointF(0f, 1f)
        max = PointF(2f, 3f)
        assertInBounds(manualSquareOffset.toCubicShape(), min, max)
    }

    @Test
    fun pathTest() {
        val shape = square.toCubicShape()
        val path = shape.toPath()
        assertFalse(path.isEmpty)
    }

    @Test
    fun boundsTest() {
        val shape = square.toCubicShape()
        val bounds = shape.bounds
        assertPointsEqualish(PointF(-1f, 1f), PointF(bounds.left, bounds.bottom))
        assertPointsEqualish(PointF(1f, -1f), PointF(bounds.right, bounds.top))
    }

    @Test
    fun centerTest() {
        assertPointsEqualish(PointF(0f, 0f), square.center)
    }

    @Test
    fun transformTest() {
        // First, make sure the shape doesn't change when transformed by the identity
        val squareCopy = RoundedPolygon(square)
        val identity = Matrix()
        square.transform(identity)
        assertEquals(square, squareCopy)

        // Now create a matrix which translates points by (1, 2) and make sure
        // the shape is translated similarly by it
        val translator = Matrix()
        val offset = PointF(1f, 2f)
        translator.setTranslate(offset.x, offset.y)
        square.transform(translator)
        val squareCubics = square.toCubicShape().cubics
        val squareCopyCubics = squareCopy.toCubicShape().cubics
        for (i in 0 until squareCubics.size) {
            assertPointsEqualish(squareCopyCubics[i].p0 + offset, squareCubics[i].p0)
            assertPointsEqualish(squareCopyCubics[i].p1 + offset, squareCubics[i].p1)
            assertPointsEqualish(squareCopyCubics[i].p2 + offset, squareCubics[i].p2)
            assertPointsEqualish(squareCopyCubics[i].p3 + offset, squareCubics[i].p3)
        }
    }

    @Test
    fun featuresTest() {
        val squareFeatures = square.features

        // Verify that cubics of polygon == cubics of features of that polygon
        assertTrue(square.toCubicShape().cubics == squareFeatures.flatMap { it.cubics })

        // Same as above but with rounded corners
        val roundedSquare = RoundedPolygon(4, rounding = CornerRounding(.1f))
        val roundedFeatures = roundedSquare.features
        assertTrue(roundedSquare.toCubicShape().cubics == roundedFeatures.flatMap { it.cubics })

        // Same as the first polygon test, but with a copy of that polygon
        val squareCopy = RoundedPolygon(square)
        val squareCopyFeatures = squareCopy.features
        assertTrue(squareCopy.toCubicShape().cubics == squareCopyFeatures.flatMap { it.cubics })

        // Test other elements of Features
        val copy = RoundedPolygon(square)
        val matrix = Matrix()
        matrix.setTranslate(1f, 2f)
        val features = copy.features
        val preTransformVertices = mutableListOf<PointF>()
        val preTransformCenters = mutableListOf<PointF>()
        for (feature in features) {
            if (feature is RoundedPolygon.Corner) {
                // Copy into new Point objects since the ones in the feature should transform
                preTransformVertices.add(PointF(feature.vertex.x, feature.vertex.y))
                preTransformCenters.add(PointF(feature.roundedCenter.x, feature.roundedCenter.y))
            }
        }
        copy.transform(matrix)
        val postTransformVertices = mutableListOf<PointF>()
        val postTransformCenters = mutableListOf<PointF>()
        for (feature in features) {
            if (feature is RoundedPolygon.Corner) {
                postTransformVertices.add(feature.vertex)
                postTransformCenters.add(feature.roundedCenter)
            }
        }
        assertNotEquals(preTransformVertices, postTransformVertices)
        assertNotEquals(preTransformCenters, postTransformCenters)
    }
}