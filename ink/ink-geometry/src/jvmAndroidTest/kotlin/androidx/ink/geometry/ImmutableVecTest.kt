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
import kotlin.math.sqrt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImmutableVecTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val vec = ImmutableVec(1f, 2f)

        assertThat(vec).isEqualTo(vec)
        assertThat(vec.hashCode()).isEqualTo(vec.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val vec = ImmutableVec(1f, 2f)
        val point = ImmutablePoint(1f, 2f)

        assertThat(vec).isNotEqualTo(point)
    }

    @Test
    fun equals_whenSameInterface_returnsTrue() {
        val vec = ImmutableVec(1f, 2f)
        val other = MutableVec(1f, 2f)
        assertThat(vec).isEqualTo(other)
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val vec = ImmutableVec(-3f, 1.2f)
        val other = ImmutableVec(-3f, 1.2f)

        assertThat(vec).isEqualTo(other)
        assertThat(vec.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenFlippedValues_returnsFalse() {
        val vec = ImmutableVec(10f, 2134f)
        val other = ImmutableVec(2134f, 10f)

        assertThat(vec).isNotEqualTo(other)
    }

    @Test
    fun getters_returnCorrectValues() {
        val vec = ImmutableVec(10f, 2134f)

        assertThat(vec.x).isEqualTo(10f)
        assertThat(vec.y).isEqualTo(2134f)
    }

    @Test
    fun newMutable_returnsCorrectMutableVec() {
        val vec = ImmutableVec(2.1f, 2134f)

        assertThat(vec.newMutable()).isEqualTo(MutableVec(2.1f, 2134f))
    }

    @Test
    fun fillMutable_correctlyModifiesMutableVec() {
        val vec = ImmutableVec(2.1f, 2134f)
        val output = MutableVec()

        vec.fillMutable(output)

        assertThat(output).isEqualTo(MutableVec(2.1f, 2134f))
    }

    @Test
    fun orthogonal_returnsCorrectValue() {
        assertThat(ImmutableVec(3f, 1f).orthogonal).isEqualTo(ImmutableVec(-1f, 3f))
        assertThat(ImmutableVec(-395f, .005f).orthogonal).isEqualTo(ImmutableVec(-.005f, -395f))
        assertThat(ImmutableVec(-.2f, -.66f).orthogonal).isEqualTo(ImmutableVec(.66f, -.2f))
        assertThat(ImmutableVec(123f, -987f).orthogonal).isEqualTo(ImmutableVec(987f, 123f))
    }

    @Test
    fun populateOrthogonal_populatesCorrectValue() {
        val mutableVec = MutableVec()
        ImmutableVec(3f, 1f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-1f, 3f))
        ImmutableVec(-395f, .005f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-.005f, -395f))
        ImmutableVec(-.2f, -.66f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(.66f, -.2f))
        ImmutableVec(123f, -987f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(987f, 123f))
    }

    @Test
    fun negation_returnsCorrectValue() {
        assertThat(ImmutableVec(3f, 1f).negation).isEqualTo(ImmutableVec(-3f, -1f))
        assertThat(ImmutableVec(-395f, .005f).negation).isEqualTo(ImmutableVec(395f, -.005f))
        assertThat(ImmutableVec(-.2f, -.66f).negation).isEqualTo(ImmutableVec(.2f, .66f))
        assertThat(ImmutableVec(123f, -987f).negation).isEqualTo(ImmutableVec(-123f, 987f))
    }

    @Test
    fun populateNegation_populatesCorrectValue() {
        val mutableVec = MutableVec()
        ImmutableVec(3f, 1f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-3f, -1f))
        ImmutableVec(-395f, .005f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(395f, -.005f))
        ImmutableVec(-.2f, -.66f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(.2f, .66f))
        ImmutableVec(123f, -987f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-123f, 987f))
    }

    @Test
    fun magnitude_returnsCorrectValue() {
        assertThat(ImmutableVec(1f, 1f).magnitude).isEqualTo(sqrt(2f))
        assertThat(ImmutableVec(-3f, 4f).magnitude).isEqualTo(5f)
        assertThat(ImmutableVec(0f, 0f).magnitude).isEqualTo(0f)
        assertThat(ImmutableVec(0f, 17f).magnitude).isEqualTo(17f)
    }

    @Test
    fun magnitudeSquared_returnsCorrectValue() {
        assertThat(ImmutableVec(1f, 1f).magnitudeSquared).isEqualTo(2f)
        assertThat(ImmutableVec(3f, -4f).magnitudeSquared).isEqualTo(25f)
        assertThat(ImmutableVec(0f, 0f).magnitudeSquared).isEqualTo(0f)
        assertThat(ImmutableVec(15f, 0f).magnitudeSquared).isEqualTo(225f)
    }

    @Test
    fun asImmutableVal_returnsThis() {
        val vec = ImmutableVec(1f, 2f)

        assertThat(vec.asImmutable).isSameInstanceAs(vec)
    }

    @Test
    fun asImmutableFun_withNoArguments_returnsThis() {
        val vec = ImmutableVec(1f, 2f)

        assertThat(vec.asImmutable()).isSameInstanceAs(vec)
    }

    @Test
    fun asImmutableFun_withArguments_returnsCorrectNewImmutableVec() {
        val vec = ImmutableVec(1f, 2f)

        assertThat(vec.asImmutable(x = 10f)).isEqualTo(ImmutableVec(10f, 2f))
        assertThat(vec.asImmutable(10f)).isEqualTo(ImmutableVec(10f, 2f))
        assertThat(vec.asImmutable(y = 20f)).isEqualTo(ImmutableVec(1f, 20f))
        assertThat(vec.asImmutable(x = 10f, y = 20f)).isEqualTo(ImmutableVec(10f, 20f))
        assertThat(vec.asImmutable(10f, 20f)).isEqualTo(ImmutableVec(10f, 20f))
    }

    @Test
    fun toString_doesNotCrash() {
        assertThat(ImmutableVec(1F, 2F).toString()).isNotEmpty()
    }

    @Test
    fun fromDirectionAndMagnitude_returnsCorrectValue() {
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(0f, 5f).isAlmostEqual(ImmutableVec(5f, 0f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(90f), 5f)
                    .isAlmostEqual(ImmutableVec(0f, 5f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(180f), 5f)
                    .isAlmostEqual(ImmutableVec(-5f, 0f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(270f), 5f)
                    .isAlmostEqual(ImmutableVec(0f, -5f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(360f), 5f)
                    .isAlmostEqual(ImmutableVec(5f, 0f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(45f), sqrt(50f))
                    .isAlmostEqual(ImmutableVec(5f, 5f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(135f), sqrt(50f))
                    .isAlmostEqual(ImmutableVec(-5f, 5f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(225f), sqrt(50f))
                    .isAlmostEqual(ImmutableVec(-5f, -5f))
            )
            .isTrue()
        assertThat(
                ImmutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(315f), sqrt(50f))
                    .isAlmostEqual(ImmutableVec(5f, -5f))
            )
            .isTrue()
    }
}
