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
class ImmutablePointTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val point = ImmutablePoint(1f, 2f)

        assertThat(point).isEqualTo(point)
        assertThat(point.hashCode()).isEqualTo(point.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val point = ImmutablePoint(1f, 2f)
        val other = ImmutableBox.fromTwoPoints(ImmutablePoint(1F, 2F), ImmutablePoint(3F, 4F))
        assertThat(point).isNotEqualTo(other)
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val point = ImmutablePoint(-3f, 1.2f)
        val other = ImmutablePoint(-3f, 1.2f)

        assertThat(point).isEqualTo(other)
        assertThat(point.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenFlippedValues_returnsFalse() {
        val point = ImmutablePoint(10f, 2134f)
        val other = ImmutablePoint(2134f, 10f)

        assertThat(point).isNotEqualTo(other)
    }

    @Test
    fun getters_returnCorrectValues() {
        val point = ImmutablePoint(10f, 2134f)

        assertThat(point.x).isEqualTo(10f)
        assertThat(point.y).isEqualTo(2134f)
    }

    @Test
    fun newMutable_returnsCorrectMutablePoint() {
        val point = ImmutablePoint(2.1f, 2134f)

        assertThat(point.newMutable()).isEqualTo(MutablePoint(2.1f, 2134f))
    }

    @Test
    fun fillMutable_correctlyModifiesMutablePoint() {
        val point = ImmutablePoint(2.1f, 2134f)
        val output = MutablePoint()

        point.fillMutable(output)

        assertThat(output).isEqualTo(MutablePoint(2.1f, 2134f))
    }

    @Test
    fun getVec_correctlyModifiesMutableVec() {
        val point = ImmutablePoint(65.26f, -9228f)
        val output = MutableVec()

        point.getVec(output)

        assertThat(output).isEqualTo(MutableVec(65.26f, -9228f))
    }

    @Test
    fun copy_withNoArguments_returnsThis() {
        val point = ImmutablePoint(1f, 2f)

        assertThat(point.copy()).isSameInstanceAs(point)
    }

    @Test
    fun copy_withArguments_makesCopy() {
        val x = 1f
        val y = 2f
        val point = ImmutablePoint(x, y)
        val differentX = 3f
        val differentY = 4f

        // Change both x and y.
        assertThat(point.copy(x = differentX, y = differentY))
            .isEqualTo(ImmutablePoint(differentX, differentY))

        // Change x.
        assertThat(point.copy(x = differentX)).isEqualTo(ImmutablePoint(differentX, y))

        // Change y.
        assertThat(point.copy(y = differentY)).isEqualTo(ImmutablePoint(x, differentY))
    }

    @Test
    fun add_withPointThenVec_correctlyAddsAndFillsMutablePoint() {
        val point = ImmutablePoint(10f, 40f)
        val vec = ImmutableVec(5f, -2f)
        val output = MutablePoint()

        Point.add(point, vec, output)

        assertThat(output).isEqualTo(MutablePoint(15f, 38f))
    }

    @Test
    fun add_withVecThenPoint_correctlyAddsAndFillsMutablePoint() {
        val point = ImmutablePoint(10f, 40f)
        val vec = ImmutableVec(5f, -2f)
        val output = MutablePoint()

        Point.add(vec, point, output)

        assertThat(output).isEqualTo(MutablePoint(15f, 38f))
    }

    @Test
    fun subtract_pointMinusVec_correctlySubtractsAndFillsMutablePoint() {
        val point = ImmutablePoint(10f, 40f)
        val vec = ImmutableVec(5f, -2f)
        val output = MutablePoint()

        Point.subtract(point, vec, output)

        assertThat(output).isEqualTo(MutablePoint(5f, 42f))
    }

    @Test
    fun subtract_pointMinusPoint_correctlySubtractsAndFillsMutableVec() {
        val lhsPoint = ImmutablePoint(10f, 40f)
        val rhsPoint = ImmutablePoint(5f, -2f)
        val output = MutableVec()

        Point.subtract(lhsPoint, rhsPoint, output)

        assertThat(output).isEqualTo(MutableVec(5f, 42f))
    }
}
