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
class ImmutableSegmentTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))

        assertThat(segment).isEqualTo(segment)
        assertThat(segment.hashCode()).isEqualTo(segment.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))
        val listOfPoints = listOf(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))

        assertThat(segment).isNotEqualTo(listOfPoints)
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))
        val other = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))

        assertThat(segment).isEqualTo(other)
        assertThat(segment.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenFlippedEndpoints_returnsFalse() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))
        val other = ImmutableSegment(ImmutableVec(1f, 2f), ImmutableVec(0f, 0f))

        assertThat(segment).isNotEqualTo(other)
    }

    @Test
    fun equals_whenAnyPointCoordinateChanged_returnsFalse() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))
        val startXChange = ImmutableSegment(ImmutableVec(1f, 0f), ImmutableVec(1f, 2f))
        val startYChange = ImmutableSegment(ImmutableVec(0f, 1f), ImmutableVec(1f, 2f))
        val endXChange = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(10f, 2f))
        val endYChange = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 20f))

        assertThat(segment).isNotEqualTo(startXChange)
        assertThat(segment).isNotEqualTo(startYChange)
        assertThat(segment).isNotEqualTo(endXChange)
        assertThat(segment).isNotEqualTo(endYChange)
    }

    @Test
    fun getters_returnCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))

        assertThat(segment.start).isEqualTo(ImmutableVec(0f, 0f))
        assertThat(segment.end).isEqualTo(ImmutableVec(1f, 2f))
    }

    @Test
    fun toImmutable_withSameValues_returnsSelf() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 2f))
        val output = segment.toImmutable()

        assertThat(output).isSameInstanceAs(segment)
    }

    @Test
    fun isAlmostEqual_usesToleranceToCompareValues() {
        val segment = ImmutableSegment(ImmutableVec(1f, 2f), ImmutableVec(3f, 4f))
        val other = ImmutableSegment(ImmutableVec(1.01f, 2.02f), ImmutableVec(3.03f, 4.04f))

        assertThat(segment.isAlmostEqual(other, 0.1f)).isTrue()
        assertThat(segment.isAlmostEqual(other, 0.02f)).isFalse()
    }

    @Test
    fun toString_correctlyReturnsString() {
        val segment = ImmutableSegment(ImmutableVec(10f, 20f), ImmutableVec(30f, 40f))
        val string = segment.toString()

        assertThat(string).contains("ImmutableSegment")
        assertThat(string).contains("ImmutableVec")
        assertThat(string).contains("10")
        assertThat(string).contains("20")
        assertThat(string).contains("30")
        assertThat(string).contains("40")
    }
}
