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

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), P2.newMutable())

        // Ensure test coverage of the same-instance case, but call .equals directly for lint.
        assertThat(triangle.equals(triangle)).isTrue()
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), P2.newMutable())
        val other = MutableTriangle(P0.newMutable(), P1.newMutable(), P2.newMutable())

        assertThat(triangle).isEqualTo(other)
        assertThat(triangle.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenPermutedEndpoints_returnsFalse() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), P2.newMutable())
        val clockWisePermutation =
            MutableTriangle(P1.newMutable(), P2.newMutable(), P0.newMutable())
        val counterClockWisePermutation =
            MutableTriangle(P2.newMutable(), P0.newMutable(), P1.newMutable())

        assertThat(triangle).isNotEqualTo(clockWisePermutation)
        assertThat(triangle).isNotEqualTo(counterClockWisePermutation)
    }

    @Test
    fun equals_whenP0different_returnsFalse() {
        val triangle = MutableTriangle(MutableVec(1f, 2f), P1.newMutable(), P2.newMutable())
        val p0XChange = MutableTriangle(MutableVec(1.23f, 2f), P1.newMutable(), P2.newMutable())
        val p0YChange = MutableTriangle(MutableVec(1f, 21.1f), P1.newMutable(), P2.newMutable())

        assertThat(triangle).isNotEqualTo(p0XChange)
        assertThat(triangle).isNotEqualTo(p0YChange)
    }

    @Test
    fun equals_whenP1different_returnsFalse() {
        val triangle = MutableTriangle(P0.newMutable(), MutableVec(3f, 4f), P2.newMutable())
        val p1XChange = MutableTriangle(P0.newMutable(), MutableVec(41.21f, 4f), P2.newMutable())
        val p1YChange = MutableTriangle(P0.newMutable(), MutableVec(3f, -6.77f), P2.newMutable())

        assertThat(triangle).isNotEqualTo(p1XChange)
        assertThat(triangle).isNotEqualTo(p1YChange)
    }

    @Test
    fun equals_whenP2different_returnsFalse() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), MutableVec(5f, 6f))
        val p2XChange = MutableTriangle(P0.newMutable(), P1.newMutable(), MutableVec(-0.43f, 6f))
        val p2YChange = MutableTriangle(P0.newMutable(), P1.newMutable(), MutableVec(5f, -10f))

        assertThat(triangle).isNotEqualTo(p2XChange)
        assertThat(triangle).isNotEqualTo(p2YChange)
    }

    @Test
    fun p0_correctlyModifiesP0Value() {
        val triangle = MutableTriangle(MutableVec(1f, 2f), P1.newMutable(), P2.newMutable())

        triangle.p0(MutableVec(1.5f, 21.6f))

        assertThat(triangle.p0).isEqualTo(MutableVec(1.5f, 21.6f))
    }

    @Test
    fun p0_withXYArgs_correctlyModifiesP0Value() {
        val triangle = MutableTriangle(MutableVec(1f, 2f), P1.newMutable(), P2.newMutable())

        triangle.p0(x = 1.5f, y = 21.6f)

        assertThat(triangle.p0).isEqualTo(MutableVec(1.5f, 21.6f))
    }

    @Test
    fun p1_correctlyModifiesP1Value() {
        val triangle = MutableTriangle(P0.newMutable(), MutableVec(3f, 4f), P2.newMutable())

        triangle.p1(MutableVec(20.9f, 513f))

        assertThat(triangle.p1).isEqualTo(MutableVec(20.9f, 513f))
    }

    @Test
    fun p1_withXYArgs_correctlyModifiesP1Value() {
        val triangle = MutableTriangle(P0.newMutable(), MutableVec(3f, 4f), P2.newMutable())

        triangle.p1(x = 20.9f, y = 513f)

        assertThat(triangle.p1).isEqualTo(MutableVec(20.9f, 513f))
    }

    @Test
    fun p2_correctlyModifiesP2Value() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), MutableVec(5f, 6f))

        triangle.p2(MutableVec(600f, 900f))

        assertThat(triangle.p2).isEqualTo(MutableVec(600f, 900f))
    }

    @Test
    fun p2_withXYArgs_correctlyModifiesP2Value() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), MutableVec(5f, 6f))

        triangle.p2(x = 600f, y = 900f)

        assertThat(triangle.p2).isEqualTo(MutableVec(600f, 900f))
    }

    @Test
    fun populateFrom_correctlyCopiesValues() {
        val triangle = MutableTriangle(P0.newMutable(), P1.newMutable(), P2.newMutable())
        val other =
            ImmutableTriangle(
                ImmutableVec(10f, 11f),
                ImmutableVec(12f, 13f),
                ImmutableVec(14f, 15f)
            )

        triangle.populateFrom(other)

        assertThat(triangle.p0).isEqualTo(MutableVec(other.p0.x, other.p0.y))
        assertThat(triangle.p1).isEqualTo(MutableVec(other.p1.x, other.p1.y))
        assertThat(triangle.p2).isEqualTo(MutableVec(other.p2.x, other.p2.y))
    }

    @Test
    fun contains_forContainedPoint_returnsTrue() {
        val triangle = MutableTriangle(P0, P1, P2)
        val point = MutableVec(4f, 3f)

        assertThat(triangle.contains(point)).isTrue()
    }

    @Test
    fun contains_forExternalPoint_returnsFalse() {
        val triangle = MutableTriangle(P0, P1, P2)
        val point = MutableVec(6f, 3f)

        assertThat(triangle.contains(point)).isFalse()
    }

    @Test
    fun edge_returnsCorrectSegment() {
        val triangle = MutableTriangle(P0, P1, P2)

        assertThat(triangle.edge(0)).isEqualTo(MutableSegment(P0, P1))
        assertThat(triangle.edge(1)).isEqualTo(MutableSegment(P1, P2))
        assertThat(triangle.edge(2)).isEqualTo(MutableSegment(P2, P0))
        assertThat(triangle.edge(3)).isEqualTo(MutableSegment(P0, P1))
        assertThat(triangle.edge(4)).isEqualTo(MutableSegment(P1, P2))
        assertThat(triangle.edge(5)).isEqualTo(MutableSegment(P2, P0))
    }

    @Test
    fun populateEdge_zeroIndex_correctlyPopulatesSegment() {
        val triangle = MutableTriangle(P0, P1, P2)
        val segment0 = MutableSegment()
        val segment6 = MutableSegment()

        triangle.populateEdge(0, segment0)
        triangle.populateEdge(6, segment6)

        assertThat(segment0).isEqualTo(MutableSegment(P0, P1))
        assertThat(segment6).isEqualTo(MutableSegment(P0, P1))
    }

    @Test
    fun populateEdge_oneIndex_correctlyPopulatesSegment() {
        val triangle = MutableTriangle(P0, P1, P2)
        val segment1 = MutableSegment()
        val segment7 = MutableSegment()

        triangle.populateEdge(1, segment1)
        triangle.populateEdge(7, segment7)

        assertThat(segment1).isEqualTo(MutableSegment(P1, P2))
        assertThat(segment7).isEqualTo(MutableSegment(P1, P2))
    }

    @Test
    fun populateEdge_twoIndex_correctlyPopulatesSegment() {
        val triangle = MutableTriangle(P0, P1, P2)
        val segment2 = MutableSegment()
        val segment8 = MutableSegment()

        triangle.populateEdge(2, segment2)
        triangle.populateEdge(8, segment8)

        assertThat(segment2).isEqualTo(MutableSegment(P2, P0))
        assertThat(segment8).isEqualTo(MutableSegment(P2, P0))
    }

    @Test
    fun asImmutable_returnsImmutableCopy() {
        val triangle = MutableTriangle(P0, P1, P2)
        val output = triangle.asImmutable()

        assertThat(output.p0).isEqualTo(P0)
        assertThat(output.p1).isEqualTo(P1)
        assertThat(output.p2).isEqualTo(P2)
    }

    @Test
    fun asImmutable_withNewValues_ReturnsNewImmutable() {
        val triangle = MutableTriangle(P0, P1, P2)
        val p0 = ImmutableVec(10f, 20f)
        val p1 = ImmutableVec(30f, 40f)
        val p2 = ImmutableVec(50f, 60f)
        val output = triangle.asImmutable(p0, p1, p2)

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
                MutableVec(5.05f, 6.06f)
            )

        assertThat(triangle.isAlmostEqual(other, 0.1f)).isTrue()
        assertThat(triangle.isAlmostEqual(other, 0.02f)).isFalse()
    }

    @Test
    fun toString_correctlyReturnsString() {
        val triangle = MutableTriangle(P0, P1, P2)

        val string = triangle.toString()

        assertThat(string).contains("MutableTriangle")
        assertThat(string).contains("MutableVec")
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
