/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MutableTriangleTest {

    private val p0 = MutableVec(1f, 2f)
    private val p1 = MutableVec(5f, 2f)
    private val p2 = MutableVec(5f, 6f)

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val triangle = MutableTriangle(p0, p1, p2)

        // Ensure test coverage of the same-instance case, but call .equals directly for lint.
        assertThat(triangle.equals(triangle)).isTrue()
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val triangle = MutableTriangle(p0, p1, p2)
        val other =
            MutableTriangle(MutableVec(p0.x, p0.y), MutableVec(p1.x, p1.y), MutableVec(p2.x, p2.y))

        assertThat(triangle).isEqualTo(other)
        assertThat(triangle.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenPermutedEndpoints_returnsFalse() {
        val triangle = MutableTriangle(p0, p1, p2)
        val clockWisePermutation = MutableTriangle(p1, p2, p0)
        val counterClockWisePermutation = MutableTriangle(p2, p0, p1)

        assertThat(triangle).isNotEqualTo(clockWisePermutation)
        assertThat(triangle).isNotEqualTo(counterClockWisePermutation)
    }

    @Test
    fun equals_whenP0different_returnsFalse() {
        val triangle = MutableTriangle(MutableVec(1f, 2f), p1, p2)
        val p0XChange = MutableTriangle(MutableVec(1.23f, 2f), p1, p2)
        val p0YChange = MutableTriangle(MutableVec(1f, 21.1f), p1, p2)

        assertThat(triangle).isNotEqualTo(p0XChange)
        assertThat(triangle).isNotEqualTo(p0YChange)
    }

    @Test
    fun equals_whenP1different_returnsFalse() {
        val triangle = MutableTriangle(p0, MutableVec(3f, 4f), p2)
        val p1XChange = MutableTriangle(p0, MutableVec(41.21f, 4f), p2)
        val p1YChange = MutableTriangle(p0, MutableVec(3f, -6.77f), p2)

        assertThat(triangle).isNotEqualTo(p1XChange)
        assertThat(triangle).isNotEqualTo(p1YChange)
    }

    @Test
    fun equals_whenP2different_returnsFalse() {
        val triangle = MutableTriangle(p0, p1, MutableVec(5f, 6f))
        val p2XChange = MutableTriangle(p0, p1, MutableVec(-0.43f, 6f))
        val p2YChange = MutableTriangle(p0, p1, MutableVec(5f, -10f))

        assertThat(triangle).isNotEqualTo(p2XChange)
        assertThat(triangle).isNotEqualTo(p2YChange)
    }

    @Test
    fun populateFrom_correctlyCopiesValues() {
        val triangle = MutableTriangle(p0, p1, p2)
        val other =
            ImmutableTriangle(
                ImmutableVec(10f, 11f),
                ImmutableVec(12f, 13f),
                ImmutableVec(14f, 15f),
            )

        triangle.populateFrom(other)

        assertThat(triangle.p0).isEqualTo(MutableVec(other.p0.x, other.p0.y))
        assertThat(triangle.p1).isEqualTo(MutableVec(other.p1.x, other.p1.y))
        assertThat(triangle.p2).isEqualTo(MutableVec(other.p2.x, other.p2.y))
    }

    @Test
    fun contains_forContainedPoint_returnsTrue() {
        val triangle = MutableTriangle(p0, p1, p2)
        val point = MutableVec(4f, 3f)

        assertThat(triangle.contains(point)).isTrue()
    }

    @Test
    fun contains_forExternalPoint_returnsFalse() {
        val triangle = MutableTriangle(p0, p1, p2)
        val point = MutableVec(6f, 3f)

        assertThat(triangle.contains(point)).isFalse()
    }

    @Test
    fun edge_returnsCorrectSegment() {
        val triangle = MutableTriangle(p0, p1, p2)

        assertThat(triangle.computeEdge(0)).isEqualTo(MutableSegment(p0, p1))
        assertThat(triangle.computeEdge(1)).isEqualTo(MutableSegment(p1, p2))
        assertThat(triangle.computeEdge(2)).isEqualTo(MutableSegment(p2, p0))
        assertThat(triangle.computeEdge(3)).isEqualTo(MutableSegment(p0, p1))
        assertThat(triangle.computeEdge(4)).isEqualTo(MutableSegment(p1, p2))
        assertThat(triangle.computeEdge(5)).isEqualTo(MutableSegment(p2, p0))
    }

    @Test
    fun populateEdge_zeroIndex_correctlyPopulatesSegment() {
        val triangle = MutableTriangle(p0, p1, p2)
        val segment0 = MutableSegment()
        val segment6 = MutableSegment()

        triangle.computeEdge(0, segment0)
        triangle.computeEdge(6, segment6)

        assertThat(segment0).isEqualTo(MutableSegment(p0, p1))
        assertThat(segment6).isEqualTo(MutableSegment(p0, p1))
    }

    @Test
    fun populateEdge_oneIndex_correctlyPopulatesSegment() {
        val triangle = MutableTriangle(p0, p1, p2)
        val segment1 = MutableSegment()
        val segment7 = MutableSegment()

        triangle.computeEdge(1, segment1)
        triangle.computeEdge(7, segment7)

        assertThat(segment1).isEqualTo(MutableSegment(p1, p2))
        assertThat(segment7).isEqualTo(MutableSegment(p1, p2))
    }

    @Test
    fun populateEdge_twoIndex_correctlyPopulatesSegment() {
        val triangle = MutableTriangle(p0, p1, p2)
        val segment2 = MutableSegment()
        val segment8 = MutableSegment()

        triangle.computeEdge(2, segment2)
        triangle.computeEdge(8, segment8)

        assertThat(segment2).isEqualTo(MutableSegment(p2, p0))
        assertThat(segment8).isEqualTo(MutableSegment(p2, p0))
    }

    @Test
    fun toImmutable_returnsImmutableCopy() {
        val triangle = MutableTriangle(p0, p1, p2)
        val output = triangle.toImmutable()

        assertThat(output.p0).isEqualTo(p0)
        assertThat(output.p1).isEqualTo(p1)
        assertThat(output.p2).isEqualTo(p2)
    }

    @Test
    fun isAlmostEqual_usesTolereneceToCompareValues() {
        val triangle = MutableTriangle(MutableVec(1f, 2f), MutableVec(3f, 4f), MutableVec(5f, 6f))
        val other =
            MutableTriangle(
                MutableVec(1.01f, 2.02f),
                MutableVec(3.03f, 4.04f),
                MutableVec(5.05f, 6.06f),
            )

        assertThat(triangle.isAlmostEqual(other, 0.1f)).isTrue()
        assertThat(triangle.isAlmostEqual(other, 0.02f)).isFalse()
    }

    @Test
    fun toString_correctlyReturnsString() {
        val triangle = MutableTriangle(p0, p1, p2)

        val string = triangle.toString()

        assertThat(string).contains("MutableTriangle")
        assertThat(string).contains("MutableVec")
        assertThat(string).contains("1")
        assertThat(string).contains("2")
        assertThat(string).contains("5")
        assertThat(string).contains("6")
    }
}
