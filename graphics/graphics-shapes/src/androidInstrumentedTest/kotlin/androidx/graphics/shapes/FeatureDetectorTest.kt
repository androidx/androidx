/*
 * Copyright 2024 The Android Open Source Project
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

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureDetectorTest {
    @Test
    fun recognizesStraightness() {
        assertTrue(Cubic.straightLine(0f, 0f, 1f, 0f).straightIsh())
    }

    @Test
    fun recognizesStraightnessIsh() {
        val slightlyNotStraightCubic =
            Cubic(323.508f, 201.759f, 317.35f, 192.008f, 311.193f, 182.227f, 305.035f, 172.475f)
        assertTrue(slightlyNotStraightCubic.straightIsh())
    }

    @Test
    fun recognizesCurvature() {
        val roundCubic =
            Cubic(
                0f,
                0f,
                0.5f,
                0.5f,
                0.5f,
                0.5f,
                1f,
                0f,
            )
        assertFalse(roundCubic.straightIsh())
    }

    @Test
    fun recognizesSmoothnessForCurvedCubic() {
        val baseCubic = Cubic(0f, 0f, 0f, 10f, 10f, 10f, 10f, 0f)
        val smoothContinuation = Cubic(10f, 0f, 10f, -10f, 20f, -10f, 20f, 0f)

        assertTrue(baseCubic.smoothesIntoIsh(smoothContinuation))
    }

    @Test
    fun recognizesSmoothnessForStraightCubic() {
        val baseCubic = Cubic.straightLine(0f, 0f, 10f, 0f)
        val smoothContinuation = Cubic.straightLine(10f, 0f, 20f, 0f)

        assertTrue(baseCubic.smoothesIntoIsh(smoothContinuation))
    }

    @Test
    fun recognizesSmoothnessWithinRelativeTolerance() {
        // These two cubics are from the edge of an imported shape. Even though they don't
        // count as smooth within the absolute distance epsilon, relatively seen they should count.
        val baseCubic =
            Cubic(323.508f, 201.759f, 317.35f, 192.008f, 311.193f, 182.227f, 305.008f, 172.475f)
        val smoothContinuation =
            Cubic(305.008f, 172.475f, 290.812f, 149.962f, 276.617f, 127.42f, 262.422f, 104.907f)

        assertTrue(baseCubic.smoothesIntoIsh(smoothContinuation))
    }

    @Test
    fun emptyCubicsAreNotStraightIsh() {
        assertFalse(Cubic.empty(10f, 10f).straightIsh())
    }

    @Test
    fun recognizesAlignmentForStraightLines() {
        val baseCubic = Cubic.straightLine(0f, 0f, 10f, 0f)
        val smoothContinuation = Cubic.straightLine(10f, 0f, 20f, 0f)

        assertTrue(baseCubic.alignsIshWith(smoothContinuation))
    }

    @Test
    fun recognizesAlignmentWithinRelativeTolerance() {
        // These two cubics are from the edge of an imported shape. Even though the second edge
        // is very small within the given scale, it is not empty. However, even the length of
        // 0.027 is so relatively tiny in the given range of coordinates, that it should be seen as
        // an empty cubic. Therefore, the second can be seen as an extend of the first.
        val baseCubic =
            Cubic(323.508f, 201.759f, 317.35f, 192.008f, 311.193f, 182.227f, 305.035f, 172.475f)
        val smoothContinuation = Cubic.straightLine(305.035f, 172.475f, 305.008f, 172.475f)

        assertTrue(baseCubic.alignsIshWith(smoothContinuation))
    }

    @Test
    fun includesAlignmentForEmptyCubics() {
        val base = Cubic.straightLine(0f, 0f, 10f, 0f)
        val empty = Cubic.empty(10f, 0f)

        assertTrue(base.alignsIshWith(empty))
        assertTrue(empty.alignsIshWith(base))
    }

    @Test
    fun convertsStraightCubicToEdge() {
        val cubic = Cubic.straightLine(0f, 0f, 10f, 0f)
        val followingCubic = Cubic.straightLine(10f, 0f, 20f, 0f)

        val converted = cubic.asFeature(followingCubic)
        val expected = Feature.Edge(listOf(cubic))

        assertTrue(converted is Feature.Edge)
        assertFeaturesEqualish(expected, converted)
    }

    @Test
    fun convertsCurvedCubicToCorner() {
        val cubic =
            Cubic(
                0f,
                0f,
                0.5f,
                0.5f,
                0.5f,
                0.5f,
                1f,
                0f,
            )
        val followingCubic =
            Cubic(
                1f,
                0f,
                1.5f,
                1.5f,
                1.5f,
                1.5f,
                2f,
                0f,
            )

        val converted = cubic.asFeature(followingCubic)
        val expected = Feature.Corner(listOf(cubic), false)

        assertTrue(converted is Feature.Corner)
        assertFeaturesEqualish(expected, converted)
    }

    @Test
    fun convertsEmptyCubicToCorner() {
        val cubic = Cubic.empty(1f, 0f)
        val followingCubic =
            Cubic(
                1f,
                0f,
                1.5f,
                1.5f,
                1.5f,
                1.5f,
                2f,
                0f,
            )

        val converted = cubic.asFeature(followingCubic)
        val expected = Feature.Corner(listOf(cubic), false)

        assertTrue(converted is Feature.Corner)
        assertFeaturesEqualish(expected, converted)
    }

    @Test
    fun reconstructsPillStar() {
        val originalPolygon = RoundedPolygon.pillStar()
        val splitCubics = originalPolygon.cubics.flatMap { it.split(0.5f).toList() }

        val createdPolygon =
            RoundedPolygon(
                detectFeatures(splitCubics),
                originalPolygon.centerX,
                originalPolygon.centerY
            )

        // It's okay if the cubics' control points aren't the same, as long as the shape is the same
        assertEquals(originalPolygon.cubics.size, createdPolygon.cubics.size)
        createdPolygon.cubics.forEachIndexed { i, new ->
            val original = originalPolygon.cubics[i]

            // pillStar has no roundings, so the created cubics shouldn't be as well
            assertTrue(new.straightIsh())
            assertTrue(original.straightIsh())

            assertPointsEqualish(
                Point(new.anchor0X, new.anchor0Y),
                Point(original.anchor0X, original.anchor0Y)
            )
            assertPointsEqualish(
                Point(new.anchor1X, new.anchor1Y),
                Point(original.anchor1X, original.anchor1Y)
            )
        }

        // The order of the features can be different, as long as they describe the same shape
        assertEquals(originalPolygon.features.size, createdPolygon.features.size)
        assertEquals(
            originalPolygon.features.filterIsInstance<Feature.Corner>().size,
            createdPolygon.features.filterIsInstance<Feature.Corner>().size
        )
        assertEquals(
            originalPolygon.features.filterIsInstance<Feature.Edge>().size,
            createdPolygon.features.filterIsInstance<Feature.Edge>().size
        )
        assertTrue(
            createdPolygon.features.zipWithNext().all {
                it.first is Feature.Edge && it.second is Feature.Corner ||
                    it.first is Feature.Corner && it.second is Feature.Edge
            }
        )
        assertTrue(
            createdPolygon.features.filterIsInstance<Feature.Corner>().all {
                it.cubics.size == 1 && it.cubics.first().zeroLength()
            }
        )
    }

    @Test
    fun reconstructsRoundedPillStarCloseEnough() {
        // This test aims to ensure that our distance epsilon is not set too high that
        // the roundings of pill star gets pointy as they are small in the [0,1] space
        val originalPolygon = RoundedPolygon.pillStar(rounding = CornerRounding(0.2f))
        val createdPolygon =
            RoundedPolygon(
                detectFeatures(originalPolygon.cubics),
                originalPolygon.centerX,
                originalPolygon.centerY
            )

        assertEquals(originalPolygon.cubics.size, createdPolygon.cubics.size)
        // Allow up to one difference...
        assertEquals(abs(originalPolygon.features.size - createdPolygon.features.size), 1)
        // ...as long as the edge - corner pattern persists
        assertTrue(
            createdPolygon.features.zipWithNext().all {
                it.first is Feature.Edge && it.second is Feature.Corner ||
                    it.first is Feature.Corner && it.second is Feature.Edge
            }
        )
    }
}
