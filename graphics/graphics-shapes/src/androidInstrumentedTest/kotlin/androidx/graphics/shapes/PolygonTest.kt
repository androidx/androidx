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

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
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
        var min = Point(-1f, -1f)
        var max = Point(1f, 1f)
        assertInBounds(square.cubics, min, max)

        val doubleSquare = RoundedPolygon(4, 2f)
        min = min * 2f
        max = max * 2f
        assertInBounds(doubleSquare.cubics, min, max)

        val offsetSquare = RoundedPolygon(4, centerX = 1f, centerY = 2f)
        min = Point(0f, 1f)
        max = Point(2f, 3f)
        assertInBounds(offsetSquare.cubics, min, max)

        val squareCopy = RoundedPolygon(square)
        min = Point(-1f, -1f)
        max = Point(1f, 1f)
        assertInBounds(squareCopy.cubics, min, max)

        val p0 = Point(1f, 0f)
        val p1 = Point(0f, 1f)
        val p2 = Point(-1f, 0f)
        val p3 = Point(0f, -1f)
        val manualSquare = RoundedPolygon(floatArrayOf(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y,
            p3.x, p3.y))
        min = Point(-1f, -1f)
        max = Point(1f, 1f)
        assertInBounds(manualSquare.cubics, min, max)

        val offset = Point(1f, 2f)
        val p0Offset = p0 + offset
        val p1Offset = p1 + offset
        val p2Offset = p2 + offset
        val p3Offset = p3 + offset
        val manualSquareOffset = RoundedPolygon(
            vertices = floatArrayOf(p0Offset.x, p0Offset.y, p1Offset.x, p1Offset.y,
                p2Offset.x, p2Offset.y, p3Offset.x, p3Offset.y),
            centerX = offset.x, centerY = offset.y)
        min = Point(0f, 1f)
        max = Point(2f, 3f)
        assertInBounds(manualSquareOffset.cubics, min, max)
    }

    @Test
    fun boundsTest() {
        val bounds = square.calculateBounds()
        assertEqualish(-1f, bounds[0]) // Left
        assertEqualish(-1f, bounds[1]) // Top
        assertEqualish(1f, bounds[2]) // Right
        assertEqualish(1f, bounds[3]) // Bottom
    }

    @Test
    fun centerTest() {
        assertPointsEqualish(Point(0f, 0f), Point(square.centerX, square.centerY))
    }

    @Test
    fun transformTest() {
        // First, make sure the shape doesn't change when transformed by the identity
        val squareCopy = square.transformed(identityTransform())
        val n = square.cubics.size

        assertEquals(n, squareCopy.cubics.size)
        for (i in 0 until n) {
            assertCubicsEqualish(square.cubics[i], squareCopy.cubics[i])
        }

        // Now create a function which translates points by (1, 2) and make sure
        // the shape is translated similarly by it
        val offset = Point(1f, 2f)
        val squareCubics = square.cubics
        val translator = translateTransform(offset.x, offset.y)
        val translatedSquareCubics = square.transformed(translator).cubics

        for (i in squareCubics.indices) {
            assertPointsEqualish(Point(squareCubics[i].anchor0X,
                squareCubics[i].anchor0Y) + offset,
                Point(translatedSquareCubics[i].anchor0X, translatedSquareCubics[i].anchor0Y))
            assertPointsEqualish(Point(squareCubics[i].control0X,
                squareCubics[i].control0Y) + offset,
                Point(translatedSquareCubics[i].control0X, translatedSquareCubics[i].control0Y))
            assertPointsEqualish(Point(squareCubics[i].control1X,
                squareCubics[i].control1Y) + offset,
                Point(translatedSquareCubics[i].control1X, translatedSquareCubics[i].control1Y))
            assertPointsEqualish(Point(squareCubics[i].anchor1X,
                squareCubics[i].anchor1Y) + offset,
                Point(translatedSquareCubics[i].anchor1X, translatedSquareCubics[i].anchor1Y))
        }
    }

    @Test
    fun featuresTest() {
        val squareFeatures = square.features

        // Verify that cubics of polygon == cubics of features of that polygon
        assertTrue(square.cubics == squareFeatures.flatMap { it.cubics })

        // Same as above but with rounded corners
        val roundedSquare = RoundedPolygon(4, rounding = CornerRounding(.1f))
        val roundedFeatures = roundedSquare.features
        assertTrue(roundedSquare.cubics == roundedFeatures.flatMap { it.cubics })

        // Same as the first polygon test, but with a copy of that polygon
        val squareCopy = RoundedPolygon(square)
        val squareCopyFeatures = squareCopy.features
        assertTrue(squareCopy.cubics == squareCopyFeatures.flatMap { it.cubics })

        // Test other elements of Features
        val translator = translateTransform(1f, 2f)
        val features = square.features
        val preTransformVertices = mutableListOf<Point>()
        val preTransformCenters = mutableListOf<Point>()
        for (feature in features) {
            if (feature is Feature.Corner) {
                // Copy into new Point objects since the ones in the feature should transform
                preTransformVertices.add(Point(feature.vertex.x, feature.vertex.y))
                preTransformCenters.add(Point(feature.roundedCenter.x, feature.roundedCenter.y))
            }
        }
        val transformedFeatures = square.transformed(translator).features
        val postTransformVertices = mutableListOf<Point>()
        val postTransformCenters = mutableListOf<Point>()
        for (feature in transformedFeatures) {
            if (feature is Feature.Corner) {
                postTransformVertices.add(feature.vertex)
                postTransformCenters.add(feature.roundedCenter)
            }
        }
        assertNotEquals(preTransformVertices, postTransformVertices)
        assertNotEquals(preTransformCenters, postTransformCenters)
    }
}
