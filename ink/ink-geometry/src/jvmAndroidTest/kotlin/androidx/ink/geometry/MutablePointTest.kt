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
class MutablePointTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val point = MutablePoint(1f, 2f)

        assertThat(point).isEqualTo(point)
        assertThat(point.hashCode()).isEqualTo(point.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val point = MutablePoint(1f, 2f)
        val vec = ImmutableVec(1f, 2f)

        assertThat(point).isNotEqualTo(vec)
    }

    @Test
    fun equals_whenSameInterface_returnsTrue() {
        val point = MutablePoint(1f, 2f)
        val other = ImmutablePoint(1f, 2f)

        assertThat(point).isEqualTo(other)
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val point = MutablePoint(-3f, 1.2f)
        val other = MutablePoint(-3f, 1.2f)

        assertThat(point).isEqualTo(other)
        assertThat(point.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenFlippedValues_returnsFalse() {
        val point = MutablePoint(10f, 2134f)
        val other = MutablePoint(2134f, 10f)

        assertThat(point).isNotEqualTo(other)
    }

    @Test
    fun getters_returnCorrectValues() {
        val point = MutablePoint(10f, 2134f)

        assertThat(point.x).isEqualTo(10f)
        assertThat(point.y).isEqualTo(2134f)
    }

    @Test
    fun setters_gettersReturnNewValues() {
        val point = MutablePoint(99f, 1234f)

        point.x = 10f
        point.y = 2134f

        assertThat(point.x).isEqualTo(10f)
        assertThat(point.y).isEqualTo(2134f)
    }

    @Test
    fun build_returnsPointWithSameValues() {
        val point = MutablePoint(10f, 2134f)

        val builtPoint = point.build()
        assertThat(builtPoint).isEqualTo(ImmutablePoint(10f, 2134f))
    }

    @Test
    fun add_withPointThenVec_correctlyAddsAndFillsAndDoesntMutateInputs() {
        val point = MutablePoint(10f, 40f)
        val vec = MutableVec(5f, -2f)
        val output = MutablePoint()

        Point.add(point, vec, output)

        assertThat(output).isEqualTo(MutablePoint(15f, 38f))
        assertThat(point).isEqualTo(MutablePoint(10f, 40f))
        assertThat(vec).isEqualTo(MutableVec(5f, -2f))
    }

    @Test
    fun add_withVecThenPoint_correctlyAddsAndFillsAndDoesntMutateInputs() {
        val point = MutablePoint(10f, 40f)
        val vec = MutableVec(5f, -2f)
        val output = MutablePoint()

        Point.add(vec, point, output)

        assertThat(output).isEqualTo(MutablePoint(15f, 38f))
        assertThat(point).isEqualTo(MutablePoint(10f, 40f))
        assertThat(vec).isEqualTo(MutableVec(5f, -2f))
    }

    @Test
    fun subtract_pointMinusVec_correctlySubtractsAndFillsAndDoesntMutateInputs() {
        val point = MutablePoint(10f, 40f)
        val vec = MutableVec(5f, -2f)
        val output = MutablePoint()

        Point.subtract(point, vec, output)

        assertThat(output).isEqualTo(MutablePoint(5f, 42f))
        assertThat(point).isEqualTo(MutablePoint(10f, 40f))
        assertThat(vec).isEqualTo(MutableVec(5f, -2f))
    }

    @Test
    fun subtract_pointMinusPoint_correctlySubtractsAndFillsAndDoesntMutateInputs() {
        val lhsPoint = MutablePoint(10f, 40f)
        val rhsPoint = MutablePoint(5f, -2f)
        val output = MutableVec()

        Point.subtract(lhsPoint, rhsPoint, output)

        assertThat(output).isEqualTo(MutableVec(5f, 42f))
        assertThat(lhsPoint).isEqualTo(MutablePoint(10f, 40f))
        assertThat(rhsPoint).isEqualTo(MutablePoint(5f, -2f))
    }
}
