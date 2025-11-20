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
import kotlin.test.assertEquals
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

class FeatureSerializerTest {
    @Test
    fun throwsForEmptyParse() {
        val serialized = ""

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun throwsForBlankParse() {
        val serialized = "                    "

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun throwsForOnlyVersionNoTags() {
        val serialized = "V1                    "

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun throwsForInsufficientCoordinateCount() {
        val serialized0 = "V1c"
        val serialized1 = "V1o1,1,2,2"
        val serialized2 = "V1o1,1,2,2,3,3,4,4,5,5,6,6"
        val serialized3 = "V1o1,1,2,2,3,3,4,4n4,4"

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized0) }
        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized1) }
        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized2) }
        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized3) }
    }

    @Test
    fun throwsForWrongSeparator() {
        val serialized = "V1o1 1 2 2 3 3 4 4"

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun throwsForNonNumbers() {
        val serialized = "V1o1,1,two,2,3,three,4,4"

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun throwsWhenTagNotFirst() {
        val serialized = "V11,1,2,2,3,4,4,4o"

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun throwsWhenCoordinatesAndTagSeparated() {
        val serialized = "V1o,1,1,2,2,3,4,4,4"

        assertThrows(IllegalArgumentException::class) { FeatureSerializer.parse(serialized) }
    }

    @Test
    fun treatsUnknownFeatureTagAsEdge() {
        val serialized = "V1 h1,1,2,2,3,3,4,4"
        val expected = Feature.Edge(listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f)))

        assertFeaturesEqualish(expected, FeatureSerializer.parse(serialized).first())
    }

    @Test
    fun treatsNoVersionAsV1() {
        val serializedNoVersion = "n1,1,2,2,3,3,4,4x4,4,5,5,6,6,7,7o7,7,8,8,9,9,10,10"
        val serializedV1 = "V1 n1,1,2,2,3,3,4,4x4,4,5,5,6,6,7,7o7,7,8,8,9,9,10,10"

        val noVersionParse = FeatureSerializer.parse(serializedNoVersion)
        val v1Parse = FeatureSerializer.parse(serializedV1)

        assertEquals(noVersionParse.size, v1Parse.size)
        for (index in v1Parse.indices) {
            assertFeaturesEqualish(v1Parse[index], noVersionParse[index])
        }
    }

    @Test
    fun treatsUnknownVersionsAsV1() {
        val serializedV999 = "V999 n1,1,2,2,3,3,4,4x4,4,5,5,6,6,7,7o7,7,8,8,9,9,10,10"
        val serializedV1 = "V1 n1,1,2,2,3,3,4,4x4,4,5,5,6,6,7,7o7,7,8,8,9,9,10,10"

        val v999Parse = FeatureSerializer.parse(serializedV999)
        val v1Parse = FeatureSerializer.parse(serializedV1)

        assertEquals(v999Parse.size, v1Parse.size)

        for (index in v1Parse.indices) {
            assertFeaturesEqualish(v1Parse[index], v999Parse[index])
        }
    }

    @Test
    fun ignoresExcessSpaces() {
        val serialized = "V1       n  1 ,  1  ,2  ,  2 , 3  ,  3  , 4 , 4   "
        val expected = Feature.Edge(listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f)))

        assertFeaturesEqualish(expected, FeatureSerializer.parse(serialized).first())
    }

    @Test
    fun parsesEdgeWithSingleCubic() {
        val serialized = "V1 n1,1,2,2,3,3,4,4"
        val expected = Feature.Edge(listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f)))

        assertFeaturesEqualish(expected, FeatureSerializer.parse(serialized).first())
    }

    @Test
    fun parsesEdgeWithSingleCubicAndFloats() {
        val serialized = "V1n1.1,1.1,2.12,2.12,3.123,3.123,4.1234,4.1234"
        val expected =
            Feature.Edge(listOf(Cubic(1.1f, 1.1f, 2.12f, 2.12f, 3.123f, 3.123f, 4.1234f, 4.1234f)))

        assertFeaturesEqualish(expected, FeatureSerializer.parse(serialized).first())
    }

    @Test
    fun parsesConvexCornerWithSingleCubic() {
        val serialized = "V1x1,1,2,2,3,3,4,4"
        val expected = Feature.Corner(listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f)), true)

        assertFeaturesEqualish(expected, FeatureSerializer.parse(serialized).first())
    }

    @Test
    fun parsesConcaveCornerWithSingleCubic() {
        val serialized = "V1o1,1,2,2,3,3,4,4"
        val expected = Feature.Corner(listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f)), false)

        assertFeaturesEqualish(expected, FeatureSerializer.parse(serialized).first())
    }

    @Test
    fun parsesEdgeWithMultipleCubics() {
        val serialized = "V1n1,1,2,2,3,3,4,4,5,5,6,6,7,7"
        val expected =
            Feature.Edge(
                listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f), Cubic(4f, 4f, 5f, 5f, 6f, 6f, 7f, 7f))
            )

        val actual = FeatureSerializer.parse(serialized)

        assertFeaturesEqualish(expected, actual.first())
    }

    @Test
    fun parsesConvexCornerWithMultipleCubics() {
        val serialized = "V1x1,1,2,2,3,3,4,4,5,5,6,6,7,7"
        val expected =
            Feature.Corner(
                listOf(
                    Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f),
                    Cubic(4f, 4f, 5f, 5f, 6f, 6f, 7f, 7f),
                ),
                true,
            )

        val actual = FeatureSerializer.parse(serialized)

        assertFeaturesEqualish(expected, actual.first())
    }

    @Test
    fun parsesConcaveCornerWithMultipleCubics() {
        val serialized = "V1o1,1,2,2,3,3,4,4,5,5,6,6,7,7"
        val expected =
            Feature.Corner(
                listOf(
                    Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f),
                    Cubic(4f, 4f, 5f, 5f, 6f, 6f, 7f, 7f),
                ),
                false,
            )

        val actual = FeatureSerializer.parse(serialized)

        assertFeaturesEqualish(expected, actual.first())
    }

    @Test
    fun parsesConvexCornerWithALotOfCubics() {
        val serialized =
            "V1x1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16"
        val expected =
            Feature.Corner(
                listOf(
                    Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f),
                    Cubic(4f, 4f, 5f, 5f, 6f, 6f, 7f, 7f),
                    Cubic(7f, 7f, 8f, 8f, 9f, 9f, 10f, 10f),
                    Cubic(10f, 10f, 11f, 11f, 12f, 12f, 13f, 13f),
                    Cubic(13f, 13f, 14f, 14f, 15f, 15f, 16f, 16f),
                ),
                true,
            )

        val actual = FeatureSerializer.parse(serialized)

        assertFeaturesEqualish(expected, actual.first())
    }

    @Test
    fun parsesMultipleFeaturesWithMultipleCubics() {
        val serialized = "V1n1,1,2,2,3,3,4,4x4,4,5,5,6,6,7,7o7,7,8,8,9,9,10,10"
        val expected =
            listOf(
                Feature.Edge(listOf(Cubic(1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f))),
                Feature.Corner(listOf(Cubic(4f, 4f, 5f, 5f, 6f, 6f, 7f, 7f)), true),
                Feature.Corner(listOf(Cubic(7f, 7f, 8f, 8f, 9f, 9f, 10f, 10f)), false),
            )

        val actual = FeatureSerializer.parse(serialized)

        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertFeaturesEqualish(expected[index], actual[index])
        }
    }

    @Test
    fun serializesEdgeWithSingleCubic() {
        val expected = "V1n1,1,2,2,3,3,4,4"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @IgnoreJsTarget // FIXME Due to float rounding issue it produces different string
    @IgnoreWasmTarget // FIXME Due to float rounding issue it produces different string
    @Test
    fun serializesEdgeWithSingleCubicAndFloats() {
        val expected = "V1n1.1,1.1,2.12,2.12,3.123,3.123,4.1234,4.1234"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @Test
    fun serializesConvexCornerWithSingleCubic() {
        val expected = "V1x1,1,2,2,3,3,4,4"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @Test
    fun serializesConcaveCornerWithSingleCubic() {
        val expected = "V1o1,1,2,2,3,3,4,4"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @Test
    fun serializesEdgeWithMultipleCubics() {
        val expected = "V1n1,1,2,2,3,3,4,4,5,5,6,6,7,7"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @Test
    fun serializesConvexCornerWithMultipleCubics() {
        val expected = "V1x1,1,2,2,3,3,4,4,5,5,6,6,7,7"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @Test
    fun serializesConcaveCornerWithMultipleCubics() {
        val expected = "V1o1,1,2,2,3,3,4,4,5,5,6,6,7,7"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @IgnoreJsTarget // FIXME Due to float rounding issue it produces different string
    @Test
    fun serializesConvexCornerWithALotOfCubics() {
        val expected =
            "V1x1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13,14,14,15,15,16,16"
        val feature = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(feature)

        assertEquals(expected, actual)
    }

    @IgnoreJsTarget // FIXME Due to float rounding issue it produces different string
    @Test
    fun serializesMultipleFeaturesWithMultipleCubics() {
        val expected = "V1n1,1,2,2,3,3,4,4x4,4,5,5,6,6,7,7o7,7,8,8,9,9,10,10"
        val features = FeatureSerializer.parse(expected)
        val actual = FeatureSerializer.serialize(features)

        assertEquals(expected, actual)
        for (index in expected.indices) {
            assertEquals(expected[index], actual[index])
        }
    }
}
