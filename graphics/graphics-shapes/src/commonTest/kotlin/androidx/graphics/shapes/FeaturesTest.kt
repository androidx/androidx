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

import androidx.kruth.assertThrows
import kotlin.test.Test

class FeaturesTest {
    @Test
    fun cannotBuildEmptyFeatures() {
        assertThrows(IllegalArgumentException::class) { Feature.buildConvexCorner(listOf()) }
        assertThrows(IllegalArgumentException::class) { Feature.buildConcaveCorner(listOf()) }
        assertThrows(IllegalArgumentException::class) { Feature.buildIgnorableFeature(listOf()) }
    }

    @Test
    fun cannotBuildNonContinuousFeatures() {
        val cubic1 = Cubic.straightLine(0f, 0f, 1f, 1f)
        val cubic2 = Cubic.straightLine(10f, 10f, 11f, 11f)

        assertThrows(IllegalArgumentException::class) {
            Feature.buildConvexCorner(listOf(cubic1, cubic2))
        }
        assertThrows(IllegalArgumentException::class) {
            Feature.buildConcaveCorner(listOf(cubic1, cubic2))
        }
        assertThrows(IllegalArgumentException::class) {
            Feature.buildIgnorableFeature(listOf(cubic1, cubic2))
        }
    }

    @Test
    fun buildsConcaveCorner() {
        val cubic = Cubic.straightLine(0f, 0f, 1f, 0f)
        val actual = Feature.buildConcaveCorner(listOf(cubic))
        val expected = Feature.Corner(listOf(cubic), false)
        assertFeaturesEqualish(expected, actual)
    }

    @Test
    fun buildsConvexCorner() {
        val cubic = Cubic.straightLine(0f, 0f, 1f, 0f)
        val actual = Feature.buildConvexCorner(listOf(cubic))
        val expected = Feature.Corner(listOf(cubic), true)
        assertFeaturesEqualish(expected, actual)
    }

    @Test
    fun buildsEdge() {
        val cubic = Cubic.straightLine(0f, 0f, 1f, 0f)
        val actual = Feature.buildEdge(cubic)
        val expected = Feature.Edge(listOf(cubic))
        assertFeaturesEqualish(expected, actual)
    }

    @Test
    fun buildsIgnorableAsEdge() {
        val cubic = Cubic.straightLine(0f, 0f, 1f, 0f)
        val actual = Feature.buildIgnorableFeature(listOf(cubic))
        val expected = Feature.Edge(listOf(cubic))
        assertFeaturesEqualish(expected, actual)
    }
}
