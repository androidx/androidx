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
class MutableVecTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val vec = MutableVec(1f, 2f)

        assertThat(vec).isEqualTo(vec)
        assertThat(vec.hashCode()).isEqualTo(vec.hashCode())
    }

    @Test
    fun equals_whenDifferentType_returnsFalse() {
        val vec = MutableVec(1f, 2f)
        val point = MutablePoint(1f, 2f)

        assertThat(vec).isNotEqualTo(point)
    }

    @Test
    fun equals_whenSameInterface_returnsTrue() {
        val vec = MutableVec(1f, 2f)
        val other = ImmutableVec(1f, 2f)
        assertThat(vec).isEqualTo(other)
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val vec = MutableVec(-3f, 1.2f)
        val other = MutableVec(-3f, 1.2f)

        assertThat(vec).isEqualTo(other)
        assertThat(vec.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun equals_whenFlippedValues_returnsFalse() {
        val vec = MutableVec(10f, 2134f)
        val other = MutableVec(2134f, 10f)

        assertThat(vec).isNotEqualTo(other)
    }

    @Test
    fun getters_returnCorrectValues() {
        val vec = MutableVec(10f, 2134f)

        assertThat(vec.x).isEqualTo(10f)
        assertThat(vec.y).isEqualTo(2134f)
    }

    @Test
    fun setters_gettersReturnNewValues() {
        val vec = MutableVec(99f, 1234f)

        vec.x = 10f
        vec.y = 2134f

        assertThat(vec.x).isEqualTo(10f)
        assertThat(vec.y).isEqualTo(2134f)
    }

    @Test
    fun x_modifiesValue() {
        val testVec = MutableVec(10f, 25f)

        testVec.x(999f)

        assertThat(testVec).isEqualTo(MutableVec(999f, 25f))
    }

    @Test
    fun y_modifiesValue() {
        val testVec = MutableVec(10f, 25f)

        testVec.y(999f)

        assertThat(testVec).isEqualTo(MutableVec(10f, 999f))
    }

    @Test
    fun populateFrom_modifiesValue() {
        val testVec = MutableVec(10f, 25f)

        testVec.populateFrom(ImmutableVec(999f, 999f))

        assertThat(testVec).isEqualTo(MutableVec(999f, 999f))
    }

    @Test
    fun orthogonal_returnsCorrectValue() {
        assertThat(MutableVec(3f, 1f).orthogonal).isEqualTo(ImmutableVec(-1f, 3f))
        assertThat(MutableVec(-395f, .005f).orthogonal).isEqualTo(ImmutableVec(-.005f, -395f))
        assertThat(MutableVec(-.2f, -.66f).orthogonal).isEqualTo(ImmutableVec(.66f, -.2f))
        assertThat(MutableVec(123f, -987f).orthogonal).isEqualTo(ImmutableVec(987f, 123f))
    }

    @Test
    fun orthogonal_whenMutableVecIsModified_returnsCorrectValue() {
        val vec = MutableVec(3f, 1f)
        assertThat(vec.orthogonal).isEqualTo(ImmutableVec(-1f, 3f))
        vec.x = 10f
        vec.y = 2134f
        assertThat(vec.orthogonal).isEqualTo(ImmutableVec(-2134f, 10f))
    }

    @Test
    fun populateOrthogonal_populatesCorrectValue() {
        val mutableVec = MutableVec()
        MutableVec(3f, 1f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-1f, 3f))
        MutableVec(-395f, .005f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(-.005f, -395f))
        MutableVec(-.2f, -.66f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(.66f, -.2f))
        MutableVec(123f, -987f).populateOrthogonal(mutableVec)
        assertThat(mutableVec).isEqualTo(ImmutableVec(987f, 123f))
    }

    @Test
    fun populateOrthogonal_whenMutableVecIsModified_populatesCorrectValue() {
        val inputVec = MutableVec(3f, 1f)
        val outputVec = MutableVec()
        inputVec.populateOrthogonal(outputVec)
        assertThat(outputVec).isEqualTo(ImmutableVec(-1f, 3f))
        inputVec.x = -9956f
        inputVec.y = -.001f
        inputVec.populateOrthogonal(outputVec)
        assertThat(outputVec).isEqualTo(ImmutableVec(.001f, -9956f))
    }

    @Test
    fun negation_returnsCorrectValue() {
        assertThat(MutableVec(3f, 1f).negation).isEqualTo(MutableVec(-3f, -1f))
        assertThat(MutableVec(-395f, .005f).negation).isEqualTo(MutableVec(395f, -.005f))
        assertThat(MutableVec(-.2f, -.66f).negation).isEqualTo(MutableVec(.2f, .66f))
        assertThat(MutableVec(123f, -987f).negation).isEqualTo(MutableVec(-123f, 987f))
    }

    @Test
    fun populateNegation_populatesCorrectValue() {
        val mutableVec = MutableVec()
        MutableVec(3f, 1f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-3f, -1f))
        MutableVec(-395f, .005f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(395f, -.005f))
        MutableVec(-.2f, -.66f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(.2f, .66f))
        MutableVec(123f, -987f).populateNegation(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-123f, 987f))
    }

    @Test
    fun negation_whenMutableVecIsModified_returnsCorrectValue() {
        val vec = MutableVec(3f, 1f)
        assertThat(vec.negation).isEqualTo(MutableVec(-3f, -1f))
        vec.x = 10f
        vec.y = 2134f
        assertThat(vec.negation).isEqualTo(MutableVec(-10f, -2134f))
    }

    @Test
    fun populateNegation_whenMutableVecIsModified_populatesCorrectValue() {
        val inputVec = MutableVec(3f, 1f)
        val outputVec = MutableVec()
        inputVec.populateNegation(outputVec)
        assertThat(outputVec).isEqualTo(MutableVec(-3f, -1f))
        inputVec.x = -9956f
        inputVec.y = -.001f
        inputVec.populateNegation(outputVec)
        assertThat(outputVec).isEqualTo(MutableVec(9956f, .001f))
    }

    @Test
    fun magnitude_returnsCorrectValue() {
        assertThat(MutableVec(1f, 1f).magnitude).isEqualTo(sqrt(2f))
        assertThat(MutableVec(-3f, 4f).magnitude).isEqualTo(5f)
        assertThat(MutableVec(0f, 0f).magnitude).isEqualTo(0f)
        assertThat(MutableVec(0f, 17f).magnitude).isEqualTo(17f)
    }

    @Test
    fun magnitude_whenMutableVecIsModified_returnsCorrectValue() {
        val vec = MutableVec(-3f, 4f)
        assertThat(vec.magnitude).isEqualTo(5f)
        vec.x = 5f
        vec.y = 12f
        assertThat(vec.magnitude).isEqualTo(13f)
    }

    @Test
    fun magnitudeSquared_returnsCorrectValue() {
        assertThat(MutableVec(1f, 1f).magnitudeSquared).isEqualTo(2f)
        assertThat(MutableVec(3f, -4f).magnitudeSquared).isEqualTo(25f)
        assertThat(MutableVec(0f, 0f).magnitudeSquared).isEqualTo(0f)
        assertThat(MutableVec(15f, 0f).magnitudeSquared).isEqualTo(225f)
    }

    @Test
    fun magnitudeSquared_whenMutableVecIsModified_returnsCorrectValue() {
        val vec = MutableVec(-3f, 4f)
        assertThat(vec.magnitudeSquared).isEqualTo(25f)
        vec.x = 5f
        vec.y = 12f
        assertThat(vec.magnitudeSquared).isEqualTo(169f)
    }

    @Test
    fun asImmutableVal_returnsNewEquivalentImmutableVec() {
        val vec = MutableVec(1f, 2f)

        assertThat(vec.asImmutable).isNotSameInstanceAs(vec)
        assertThat(vec.asImmutable).isEqualTo(vec)
    }

    @Test
    fun asImmutableFun_withNoArguments_returnsNewEquivalentImmutableVec() {
        val vec = MutableVec(1f, 2f)

        assertThat(vec.asImmutable()).isNotSameInstanceAs(vec)
        assertThat(vec.asImmutable()).isEqualTo(vec)
    }

    @Test
    fun asImmutableFun_withArguments_returnsCorrectNewImmutableVec() {
        val vec = MutableVec(1f, 2f)

        assertThat(vec.asImmutable(x = 10f)).isEqualTo(ImmutableVec(10f, 2f))
        assertThat(vec.asImmutable(10f)).isEqualTo(ImmutableVec(10f, 2f))
        assertThat(vec.asImmutable(y = 20f)).isEqualTo(ImmutableVec(1f, 20f))
        assertThat(vec.asImmutable(x = 10f, y = 20f)).isEqualTo(ImmutableVec(10f, 20f))
        assertThat(vec.asImmutable(10f, 20f)).isEqualTo(ImmutableVec(10f, 20f))
    }

    @Test
    fun unitVec_whenModified_returnsCorrectValue() {
        val vec = MutableVec(4f, 0f)
        assertThat(vec.unitVec).isEqualTo(ImmutableVec(1f, 0f))
        vec.x = 0f
        vec.y = -.05f
        assertThat(vec.unitVec).isEqualTo(ImmutableVec(0f, -1f))
    }

    @Test
    fun add_whenGivenAMutableVecAsBothInputAndOutput_populatesCorrectValue() {
        val a = MutableVec(3f, 0f)
        val b = MutableVec(-1f, .3f)
        val c = MutableVec(2.7f, 4f)

        Vec.add(a, b, a)
        assertThat(a.isAlmostEqual(ImmutableVec(2f, .3f))).isTrue()

        Vec.add(b, c, b)
        assertThat(b.isAlmostEqual(ImmutableVec(1.7f, 4.3f))).isTrue()

        Vec.add(c, c, c)
        assertThat(c.isAlmostEqual(ImmutableVec(5.4f, 8f))).isTrue()
    }

    @Test
    fun multiply_whenGivenAMutableVecAsBothInputAndOutput_populatesCorrectValue() {
        val a = MutableVec(.7f, -3f)
        val b = MutableVec(3f, 5f)

        Vec.multiply(a, 2f, a)
        assertThat(a.isAlmostEqual(ImmutableVec(1.4f, -6f))).isTrue()

        Vec.multiply(-.3f, b, b)
        assertThat(b.isAlmostEqual(ImmutableVec(-0.9f, -1.5f))).isTrue()
    }

    @Test
    fun divide_whenGivenAMutableVecAsBothInputAndOutput_populatesCorrectValue() {
        val a = MutableVec(7f, .9f)
        val b = MutableVec(-4.5f, -2f)

        Vec.divide(a, -.1f, a)
        assertThat(a.isAlmostEqual(ImmutableVec(-70f, -9f))).isTrue()

        Vec.divide(b, 5f, b)
        assertThat(b.isAlmostEqual(ImmutableVec(-.9f, -.4f))).isTrue()
    }

    @Test
    fun toString_doesNotCrash() {
        assertThat(MutableVec(1F, 2F).toString()).isNotEmpty()
    }

    @Test
    fun fromDirectionAndMagnitude_returnsCorrectValue() {
        assertThat(MutableVec.fromDirectionAndMagnitude(0f, 5f).isAlmostEqual(MutableVec(5f, 0f)))
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(90f), 5f)
                    .isAlmostEqual(MutableVec(0f, 5f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(180f), 5f)
                    .isAlmostEqual(MutableVec(-5f, 0f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(270f), 5f)
                    .isAlmostEqual(MutableVec(0f, -5f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(360f), 5f)
                    .isAlmostEqual(MutableVec(5f, 0f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(45f), sqrt(50f))
                    .isAlmostEqual(MutableVec(5f, 5f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(135f), sqrt(50f))
                    .isAlmostEqual(MutableVec(-5f, 5f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(225f), sqrt(50f))
                    .isAlmostEqual(MutableVec(-5f, -5f))
            )
            .isTrue()
        assertThat(
                MutableVec.fromDirectionAndMagnitude(Angle.degreesToRadians(315f), sqrt(50f))
                    .isAlmostEqual(MutableVec(5f, -5f))
            )
            .isTrue()
    }
}
