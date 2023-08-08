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

        val offsetSquare = RoundedPolygon(4, centerX = 1f, centerY = 2f)
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
        val manualSquare = RoundedPolygon(floatArrayOf(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y,
            p3.x, p3.y))
        min = PointF(-1f, -1f)
        max = PointF(1f, 1f)
        assertInBounds(manualSquare.toCubicShape(), min, max)

        val offset = PointF(1f, 2f)
        val p0Offset = p0 + offset
        val p1Offset = p1 + offset
        val p2Offset = p2 + offset
        val p3Offset = p3 + offset
        val manualSquareOffset = RoundedPolygon(
            vertices = floatArrayOf(p0Offset.x, p0Offset.y, p1Offset.x, p1Offset.y,
                p2Offset.x, p2Offset.y, p3Offset.x, p3Offset.y),
            centerX = offset.x, centerY = offset.y)
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
        assertPointsEqualish(PointF(0f, 0f), PointF(square.centerX, square.centerY))
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
            assertPointsEqualish(PointF(squareCopyCubics[i].anchor0X,
                squareCopyCubics[i].anchor0Y) + offset,
                PointF(squareCubics[i].anchor0X, squareCubics[i].anchor0Y))
            assertPointsEqualish(PointF(squareCopyCubics[i].control0X,
                squareCopyCubics[i].control0Y) + offset,
                PointF(squareCubics[i].control0X, squareCubics[i].control0Y))
            assertPointsEqualish(PointF(squareCopyCubics[i].control1X,
                squareCopyCubics[i].control1Y) + offset,
                PointF(squareCubics[i].control1X, squareCubics[i].control1Y))
            assertPointsEqualish(PointF(squareCopyCubics[i].anchor1X,
                squareCopyCubics[i].anchor1Y) + offset,
                PointF(squareCubics[i].anchor1X, squareCubics[i].anchor1Y))
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
