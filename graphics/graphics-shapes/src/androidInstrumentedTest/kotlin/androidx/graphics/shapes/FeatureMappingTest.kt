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

import org.junit.Test

class FeatureMappingTest {
    private val triangleWithRoundings = RoundedPolygon(3, rounding = CornerRounding(0.2f))
    private val triangle = RoundedPolygon(3)
    private val square = RoundedPolygon(4)
    private val squareRotated = RoundedPolygon(4).transformed(pointRotator(45f))

    @Test
    fun featureMappingTriangles() =
        verifyMapping(triangleWithRoundings, triangle) { distances ->
            distances.forEach { assert(it < 0.1f) }
        }

    @Test
    fun featureMappingTriangleToSquare() =
        verifyMapping(triangle, square) { distances ->
            // We have one exact match (both have points at 0 degrees), and 2 close ones
            assert(distances.size == 3)
            assertEqualish(distances[0], distances[1])
            assert(distances[0] < 0.3f)
            assert(distances[2] < 1e-6f)
        }

    @Test
    fun featureMappingSquareToTriangle() =
        verifyMapping(square, triangle) { distances ->
            // We have one exact match (both have points at 0 degrees), and 2 close ones
            assert(distances.size == 3)

            assertEqualish(distances[0], distances[1])
            assert(distances[0] < 0.3f)
            assert(distances[2] < 1e-6f)
        }

    @Test
    fun featureMappingSquareRotatedToTriangle() =
        verifyMapping(squareRotated, triangle) { distances ->
            // We have a very bad mapping (the triangle vertex just in the middle of one of the
            // square's sides) and 2 decent ones.
            assert(distances.size == 3)

            assert(distances[0] > 0.5f)
            assertEqualish(distances[1], distances[2])
            assert(distances[2] < 0.1f)
        }

    @Test
    fun featureMappingDoesntCrash() {
        // Verify that complicated shapes can me matched (this used to crash before).
        val checkmark =
            RoundedPolygon(
                    floatArrayOf(
                        400f,
                        -304f,
                        240f,
                        -464f,
                        296f,
                        -520f,
                        400f,
                        -416f,
                        664f,
                        -680f,
                        720f,
                        -624f,
                        400f,
                        -304f,
                    )
                )
                .normalized()
        val verySunny =
            RoundedPolygon.star(
                    numVerticesPerRadius = 8,
                    innerRadius = .65f,
                    rounding = CornerRounding(radius = .15f),
                )
                .normalized()
        verifyMapping(checkmark, verySunny) {
            // Most vertices on the checkmark map to a feature in the second shape.
            assert(it.size >= 6)

            // And they are close enough
            assert(it[0] < 0.15f)
        }
    }

    private fun verifyMapping(
        p1: RoundedPolygon,
        p2: RoundedPolygon,
        validator: (List<Float>) -> Unit,
    ) {
        val f1 = MeasuredPolygon.measurePolygon(LengthMeasurer(), p1).features
        val f2 = MeasuredPolygon.measurePolygon(LengthMeasurer(), p2).features

        // Maps progress in p1 to progress in p2
        val map = doMapping(f1, f2)

        // See which features where actually mapped and the distance between their representative
        // points
        val distances = buildList {
            map.forEach { (progress1, progress2) ->
                val feature1 = f1.find { it.progress == progress1 }!!
                val feature2 = f2.find { it.progress == progress2 }!!
                add(featureDistSquared(feature1.feature, feature2.feature))
            }
        }

        validator(distances.sortedDescending())
    }
}
