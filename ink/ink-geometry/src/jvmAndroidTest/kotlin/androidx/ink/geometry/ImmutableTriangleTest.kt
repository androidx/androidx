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
class ImmutableTriangleTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val triangle = ImmutableTriangle(P0, P1, P2)

        // Ensure test coverage of the same-instance case, but call .equals directly for lint.
        assertThat(triangle.equals(triangle)).isTrue()
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val other = ImmutableTriangle(P0, P1, P2)

        assertThat(triangle).isEqualTo(other)
        assertThat(triangle.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenPermutedEndpoints_returnsFalse() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val clockWisePermutation = ImmutableTriangle(P1, P0, P2)
        val counterClockWisePermutation = ImmutableTriangle(P2, P0, P1)

        assertThat(triangle).isNotEqualTo(clockWisePermutation)
        assertThat(triangle).isNotEqualTo(counterClockWisePermutation)
    }

    @Test
    fun equals_whenP0different_returnsFalse() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val p0XChange = ImmutableTriangle(ImmutableVec(1.23f, 2f), P1, P2)
        val p0YChange = ImmutableTriangle(ImmutableVec(1f, 21.1f), P1, P2)

        assertThat(triangle).isNotEqualTo(p0XChange)
        assertThat(triangle).isNotEqualTo(p0YChange)
    }

    @Test
    fun equals_whenP1different_returnsFalse() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val p1XChange = ImmutableTriangle(P0, ImmutableVec(41.21f, 4f), P2)
        val p1YChange = ImmutableTriangle(P0, ImmutableVec(3f, -6.77f), P2)

        assertThat(triangle).isNotEqualTo(p1XChange)
        assertThat(triangle).isNotEqualTo(p1YChange)
    }

    @Test
    fun equals_whenP2different_returnsFalse() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val p2XChange = ImmutableTriangle(P0, P1, ImmutableVec(-0.43f, 6f))
        val p2YChange = ImmutableTriangle(P0, P1, ImmutableVec(5f, -10f))

        assertThat(triangle).isNotEqualTo(p2XChange)
        assertThat(triangle).isNotEqualTo(p2YChange)
    }

    @Test
    fun getters_returnCorrectValues() {
        val triangle = ImmutableTriangle(P0, P1, P2)

        assertThat(triangle.p0).isEqualTo(P0)
        assertThat(triangle.p1).isEqualTo(P1)
        assertThat(triangle.p2).isEqualTo(P2)
    }

    @Test
    fun edge_returnsCorrectSegment() {
        val triangle = ImmutableTriangle(P0, P1, P2)

        assertThat(triangle.computeEdge(0)).isEqualTo(ImmutableSegment(P0, P1))
        assertThat(triangle.computeEdge(1)).isEqualTo(ImmutableSegment(P1, P2))
        assertThat(triangle.computeEdge(2)).isEqualTo(ImmutableSegment(P2, P0))
        assertThat(triangle.computeEdge(3)).isEqualTo(ImmutableSegment(P0, P1))
        assertThat(triangle.computeEdge(4)).isEqualTo(ImmutableSegment(P1, P2))
        assertThat(triangle.computeEdge(5)).isEqualTo(ImmutableSegment(P2, P0))
    }

    @Test
    fun contains_forContainedPoint_returnsTrue() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val point = ImmutableVec(4f, 3f)

        assertThat(triangle.contains(point)).isTrue()
    }

    @Test
    fun contains_forExternalPoint_returnsFalse() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val point = ImmutableVec(6f, 3f)

        assertThat(triangle.contains(point)).isFalse()
    }

    @Test
    fun populateEdge_zeroIndex_correctlyPopulatesSegment() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val segment0 = MutableSegment()
        val segment6 = MutableSegment()

        triangle.computeEdge(0, segment0)
        triangle.computeEdge(6, segment6)

        assertThat(segment0).isEqualTo(ImmutableSegment(P0, P1))
        assertThat(segment6).isEqualTo(ImmutableSegment(P0, P1))
    }

    @Test
    fun populateEdge_oneIndex_correctlyPopulatesSegment() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val segment1 = MutableSegment()
        val segment7 = MutableSegment()

        triangle.computeEdge(1, segment1)
        triangle.computeEdge(7, segment7)

        assertThat(segment1).isEqualTo(ImmutableSegment(P1, P2))
        assertThat(segment7).isEqualTo(ImmutableSegment(P1, P2))
    }

    @Test
    fun populateEdge_twoIndex_correctlyPopulatesSegment() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val segment2 = MutableSegment()
        val segment8 = MutableSegment()

        triangle.computeEdge(2, segment2)
        triangle.computeEdge(8, segment8)

        assertThat(segment2).isEqualTo(ImmutableSegment(P2, P0))
        assertThat(segment8).isEqualTo(ImmutableSegment(P2, P0))
    }

    @Test
    fun toImmutable_returnsSelf() {
        val triangle = ImmutableTriangle(P0, P1, P2)
        val output = triangle.toImmutable()

        assertThat(output).isSameInstanceAs(triangle)
    }

    @Test
    fun isAlmostEqual_usesToleranceToCompareValues() {
        val triangle =
            ImmutableTriangle(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f), ImmutableVec(5f, 6f))
        val other =
            ImmutableTriangle(
                ImmutableVec(1.01f, 2.02f),
                ImmutableVec(3.03f, 4.04f),
                ImmutableVec(5.05f, 6.06f),
            )

        assertThat(triangle.isAlmostEqual(other, 0.1f)).isTrue()
        assertThat(triangle.isAlmostEqual(other, 0.02f)).isFalse()
    }

    @Test
    fun toString_correctlyReturnsString() {
        val triangle = ImmutableTriangle(P0, P1, P2)

        val string = triangle.toString()

        assertThat(string).contains("ImmutableTriangle")
        assertThat(string).contains("ImmutableVec")
        assertThat(string).contains("1")
        assertThat(string).contains("2")
        assertThat(string).contains("5")
        assertThat(string).contains("6")
    }

    companion object {
        private val P0 = ImmutableVec(1f, 2f)

        private val P1 = ImmutableVec(5f, 2f)

        private val P2 = ImmutableVec(5f, 6f)
    }
}
