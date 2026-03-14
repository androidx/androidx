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
class MutableSegmentTest {

    @Test
    fun vec_whenPrimaryValuesAreUnchanged_returnsCorrectImmutableVec() {
        val segment = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))

        assertThat(segment.computeDisplacement()).isEqualTo(ImmutableVec(1f, 2f))
    }

    @Test
    fun vec_whenPrimaryValuesAreModified_returnsDifferentImmutableVec() {
        val segment = MutableSegment(MutableVec(10f, 50f), MutableVec(1f, 2f))

        segment.start.x = 0f
        segment.start.y = 0f
        assertThat(segment.computeDisplacement()).isEqualTo(ImmutableVec(1f, 2f))

        segment.end.x = -.005f
        segment.end.y = -456f
        assertThat(segment.computeDisplacement()).isEqualTo(ImmutableVec(-.005f, -456f))
    }

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val segment = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))

        assertThat(segment).isEqualTo(segment)
        assertThat(segment.hashCode()).isEqualTo(segment.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val segment = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))
        val listOfPoints = listOf(MutableVec(0f, 0f), MutableVec(1f, 2f))

        assertThat(segment).isNotEqualTo(listOfPoints)
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val segment = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))
        val other = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))

        assertThat(segment).isEqualTo(other)
        assertThat(segment.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenFlippedEndpoints_returnsFalse() {
        val segment = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))
        val other = MutableSegment(MutableVec(1f, 2f), MutableVec(0f, 0f))

        assertThat(segment).isNotEqualTo(other)
    }

    @Test
    fun equals_whenAnyPointCoordinateChanged_returnsFalse() {
        val segment = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 2f))
        val startXChange = MutableSegment(MutableVec(1f, 0f), MutableVec(1f, 2f))
        val startYChange = MutableSegment(MutableVec(0f, 1f), MutableVec(1f, 2f))
        val endXChange = MutableSegment(MutableVec(0f, 0f), MutableVec(10f, 2f))
        val endYChange = MutableSegment(MutableVec(0f, 0f), MutableVec(1f, 20f))

        assertThat(segment).isNotEqualTo(startXChange)
        assertThat(segment).isNotEqualTo(startYChange)
        assertThat(segment).isNotEqualTo(endXChange)
        assertThat(segment).isNotEqualTo(endYChange)
    }

    @Test
    fun toImmutable_returnsImmutableCopy() {
        val start = MutableVec(10f, 20f)
        val end = MutableVec(1f, 2f)
        val segment = MutableSegment(start, end)
        val output = segment.toImmutable()

        assertThat(output.start).isEqualTo(start)
        assertThat(output.end).isEqualTo(end)
    }

    @Test
    fun isAlmostEqual_usesToleranceToCompareValues() {
        val segment = MutableSegment(MutableVec(1f, 2f), MutableVec(3f, 4f))
        val other = MutableSegment(MutableVec(1.01f, 2.02f), MutableVec(3.03f, 4.04f))

        assertThat(segment.isAlmostEqual(other, 0.1f)).isTrue()
        assertThat(segment.isAlmostEqual(other, 0.02f)).isFalse()
    }

    @Test
    fun toString_correctlyReturnsString() {
        val segment = MutableSegment(MutableVec(10f, 20f), MutableVec(30f, 40f))
        val string = segment.toString()

        assertThat(string).contains("MutableSegment")
        assertThat(string).contains("MutableVec")
        assertThat(string).contains("10")
        assertThat(string).contains("20")
        assertThat(string).contains("30")
        assertThat(string).contains("40")
    }
}
