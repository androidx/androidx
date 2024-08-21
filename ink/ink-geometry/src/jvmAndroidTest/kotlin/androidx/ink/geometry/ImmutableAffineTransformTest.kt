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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ImmutableAffineTransformTest {

    @Test
    fun equals_whenSameInstance_returnsTrueAndSameHashCode() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        // Ensure test coverage of the same-instance case, but call .equals directly for lint.
        assertThat(affineTransform.equals(affineTransform)).isTrue()
        assertThat(affineTransform.hashCode()).isEqualTo(affineTransform.hashCode())
    }

    @Test
    fun equals_whenSameValues_returnsTrueAndSameHashCode() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        assertThat(affineTransform).isEqualTo(otherTransform)
        assertThat(affineTransform.hashCode()).isEqualTo(otherTransform.hashCode())
    }

    @Test
    fun equals_whenSameInterfacePropertiesAndDifferentType_returnsTrue() {
        val immutable = ImmutableAffineTransform(A, B, C, D, E, F)
        val mutable = MutableAffineTransform(A, B, C, D, E, F)

        assertThat(immutable).isEqualTo(mutable)
    }

    @Test
    fun equals_whenDifferentA_returnsFalse() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(DIFFERENT_A, B, C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentB_returnsFalse() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(A, DIFFERENT_B, C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentC_returnsFalse() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(A, B, DIFFERENT_C, D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentD_returnsFalse() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(A, B, C, DIFFERENT_D, E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentE_returnsFalse() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(A, B, C, D, DIFFERENT_E, F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun equals_whenDifferentF_returnsFalse() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)
        val otherTransform = ImmutableAffineTransform(A, B, C, D, E, DIFFERENT_F)

        assertThat(affineTransform).isNotEqualTo(otherTransform)
    }

    @Test
    fun translate_returnsCorrectImmutableAffineTransform() {
        val translate = ImmutableAffineTransform.translate(ImmutableVec(4.12f, -19.9f))
        val expected = ImmutableAffineTransform(1f, 0f, 4.12f, 0f, 1f, -19.9f)

        assertThat(translate).isEqualTo(expected)
    }

    @Test
    fun scale_callsJniAndReturnsCorrectValue() {
        val scale = ImmutableAffineTransform.scale(2.9f)
        val expected = ImmutableAffineTransform(2.9f, 0f, 0f, 0f, 2.9f, 0f)

        assertThat(scale).isEqualTo(expected)
    }

    @Test
    fun scale_withTwoArguments_callsJniAndReturnsCorrectValue() {
        val scale = ImmutableAffineTransform.scale(-7.13f, 19.71f)
        val expected = ImmutableAffineTransform(-7.13f, 0f, 0f, 0f, 19.71f, 0f)

        assertThat(scale).isEqualTo(expected)
    }

    @Test
    fun scaleX_callsJniAndReturnsCorrectValue() {
        val scale = ImmutableAffineTransform.scaleX(100.54f)
        val expected = ImmutableAffineTransform(100.54f, 0f, 0f, 0f, 1f, 0f)

        assertThat(scale).isEqualTo(expected)
    }

    @Test
    fun scaleY_callsJniAndReturnsCorrectValue() {
        val scale = ImmutableAffineTransform.scaleY(12f)
        val expected = ImmutableAffineTransform(1f, 0f, 0f, 0f, 12f, 0f)

        assertThat(scale).isEqualTo(expected)
    }

    @Test
    fun populateInverse_correctlyModifiesOutput() {
        val identityTransform = AffineTransform.IDENTITY
        val identityOutput = MutableAffineTransform()

        identityTransform.computeInverse(identityOutput)
        assertThat(identityOutput).isEqualTo(AffineTransform.IDENTITY)

        val scaleTransform = ImmutableAffineTransform.scale(4f, 10f)
        val scaleOutput = MutableAffineTransform()

        scaleTransform.computeInverse(scaleOutput)
        assertThat(scaleOutput).isEqualTo(ImmutableAffineTransform.scale(0.25f, 0.1f))

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(5f, 10f))
        val translateOutput = MutableAffineTransform()

        translateTransform.computeInverse(translateOutput)
        assertThat(translateOutput)
            .isEqualTo(ImmutableAffineTransform.translate(ImmutableVec(-5f, -10f)))

        val shearXTransform = ImmutableAffineTransform(1f, 5F, 0f, 0f, 1f, 0f)
        val shearXOutput = MutableAffineTransform()

        shearXTransform.computeInverse(shearXOutput)
        assertThat(shearXOutput).isEqualTo(ImmutableAffineTransform(1f, -5f, 0f, 0f, 1f, 0f))

        val shearYTransform = ImmutableAffineTransform(1f, 0F, 0f, 5f, 1f, 0f)
        val shearYOutput = MutableAffineTransform()

        shearYTransform.computeInverse(shearYOutput)
        assertThat(shearYOutput).isEqualTo(ImmutableAffineTransform(1f, 0f, 0f, -5f, 1f, 0f))
    }

    @Test
    fun populateInverse_whenAppliedToItself_correctlyModifiesItself() {
        // This is equivalent to ImmutableAffineTransform.scale(4f, 10f)
        val testTransform = MutableAffineTransform(4f, 0f, 0f, 0f, 10f, 0f)

        testTransform.computeInverse(testTransform)
        assertThat(testTransform).isEqualTo(ImmutableAffineTransform.scale(0.25f, 0.1f))
    }

    @Test
    fun populateInverse_whenAppliedToTransformWithDeterminantOfZero_correctlyThrows() {
        val zeroesTransform = MutableAffineTransform(0f, 0f, 0f, 0f, 0f, 0f)

        assertFailsWith<IllegalArgumentException> {
            zeroesTransform.computeInverse(zeroesTransform)
        }

        // Determinant = a * e - b * d = 2 * 16 - 4 * 8 = 0
        val determinantOfZeroTransform = MutableAffineTransform(2f, 4f, 0f, 8f, 16f, 0f)

        assertFailsWith<IllegalArgumentException> {
            determinantOfZeroTransform.computeInverse(determinantOfZeroTransform)
        }
    }

    @Test
    fun applyTransform_whenAppliedToAVec_correctlyModifiesVec() {
        val testVec = ImmutableVec(4F, 6F)

        val identityTransform = AffineTransform.IDENTITY
        val identityVec = MutableVec()
        identityTransform.applyTransform(testVec, identityVec)
        assertThat(identityVec).isEqualTo(MutableVec(4F, 6F))

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(3F, -20F))
        val translateVec = MutableVec()
        translateTransform.applyTransform(testVec, translateVec)
        assertThat(translateVec).isEqualTo(MutableVec(7F, -14F))

        val scaleBy2ValuesTransform = ImmutableAffineTransform.scale(2.5F, -.5F)
        val scaleBy2ValuesVec = MutableVec()
        scaleBy2ValuesTransform.applyTransform(testVec, scaleBy2ValuesVec)
        assertThat(scaleBy2ValuesVec).isEqualTo(MutableVec(10F, -3F))

        val scaleBy1ValueTransform = ImmutableAffineTransform.scale(2.5F)
        val scaleBy1ValueVec = MutableVec()
        scaleBy1ValueTransform.applyTransform(testVec, scaleBy1ValueVec)
        assertThat(scaleBy1ValueVec).isEqualTo(MutableVec(10F, 15F))

        val scaleXTransform = ImmutableAffineTransform.scaleX(2.5F)
        val scaleXVec = MutableVec()
        scaleXTransform.applyTransform(testVec, scaleXVec)
        assertThat(scaleXVec).isEqualTo(MutableVec(10F, 6F))

        val scaleYTransform = ImmutableAffineTransform.scaleY(2.5F)
        val scaleYVec = MutableVec()
        scaleYTransform.applyTransform(testVec, scaleYVec)
        assertThat(scaleYVec).isEqualTo(MutableVec(4F, 15F))
    }

    @Test
    fun applyTransform_whenAppliedToAMutableVec_canModifyInputAsOutput() {
        val testMutableVec = MutableVec(4F, 6F)

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(3F, -20F))
        translateTransform.applyTransform(testMutableVec, testMutableVec)
        assertThat(testMutableVec).isEqualTo(MutableVec(7F, -14F))
    }

    @Test
    fun applyTransform_whenAppliedToASegment_correctlyModifiesSegment() {
        val testSegment = ImmutableSegment(ImmutableVec(4F, 6F), ImmutableVec(40F, 60F))

        val identityTransform = AffineTransform.IDENTITY
        val identitySegment = MutableSegment()
        identityTransform.applyTransform(testSegment, identitySegment)
        assertThat(identitySegment)
            .isEqualTo(MutableSegment(MutableVec(4F, 6F), MutableVec(40F, 60F)))

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(3F, -20F))
        val translateSegment = MutableSegment()
        translateTransform.applyTransform(testSegment, translateSegment)
        assertThat(translateSegment)
            .isEqualTo(MutableSegment(MutableVec(7F, -14F), MutableVec(43F, 40F)))

        val scaleBy2ValuesTransform = ImmutableAffineTransform.scale(2.5F, -.5F)
        val scaleBy2ValuesSegment = MutableSegment()
        scaleBy2ValuesTransform.applyTransform(testSegment, scaleBy2ValuesSegment)
        assertThat(scaleBy2ValuesSegment)
            .isEqualTo(MutableSegment(MutableVec(10F, -3F), MutableVec(100F, -30F)))

        val scaleBy1ValueTransform = ImmutableAffineTransform.scale(2.5F)
        val scaleBy1ValueSegment = MutableSegment()
        scaleBy1ValueTransform.applyTransform(testSegment, scaleBy1ValueSegment)
        assertThat(scaleBy1ValueSegment)
            .isEqualTo(MutableSegment(MutableVec(10F, 15F), MutableVec(100F, 150F)))

        val scaleXTransform = ImmutableAffineTransform.scaleX(2.5F)
        val scaleXSegment = MutableSegment()
        scaleXTransform.applyTransform(testSegment, scaleXSegment)
        assertThat(scaleXSegment)
            .isEqualTo(MutableSegment(MutableVec(10F, 6F), MutableVec(100F, 60F)))

        val scaleYTransform = ImmutableAffineTransform.scaleY(2.5F)
        val scaleYSegment = MutableSegment()
        scaleYTransform.applyTransform(testSegment, scaleYSegment)
        assertThat(scaleYSegment)
            .isEqualTo(MutableSegment(MutableVec(4F, 15F), MutableVec(40F, 150F)))
    }

    @Test
    fun applyTransform_whenAppliedToAMutableSegment_canModifyInputAsOutput() {
        val testMutableSegment = MutableSegment(MutableVec(4F, 6F), MutableVec(40F, 60F))

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(3F, -20F))
        translateTransform.applyTransform(testMutableSegment, testMutableSegment)
        assertThat(testMutableSegment)
            .isEqualTo(MutableSegment(MutableVec(7F, -14F), MutableVec(43F, 40F)))
    }

    @Test
    fun applyTransform_whenAppliedToATriangle_correctlyModifiesTriangle() {
        val testTriangle =
            ImmutableTriangle(ImmutableVec(1F, 2F), ImmutableVec(6F, -3F), ImmutableVec(-4F, -6F))

        val identityTransform = AffineTransform.IDENTITY
        val identityTriangle = MutableTriangle()
        identityTransform.applyTransform(testTriangle, identityTriangle)
        assertThat(identityTriangle)
            .isEqualTo(
                MutableTriangle(MutableVec(1F, 2F), MutableVec(6F, -3F), MutableVec(-4F, -6F))
            )

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(3F, -20F))
        val translateTriangle = MutableTriangle()
        translateTransform.applyTransform(testTriangle, translateTriangle)
        assertThat(translateTriangle)
            .isEqualTo(
                MutableTriangle(MutableVec(4F, -18F), MutableVec(9F, -23F), MutableVec(-1F, -26F))
            )

        val scaleBy2ValuesTransform = ImmutableAffineTransform.scale(2.5F, -.5F)
        val scaleBy2ValuesTriangle = MutableTriangle()
        scaleBy2ValuesTransform.applyTransform(testTriangle, scaleBy2ValuesTriangle)
        assertThat(scaleBy2ValuesTriangle)
            .isEqualTo(
                MutableTriangle(MutableVec(2.5F, -1F), MutableVec(15F, 1.5F), MutableVec(-10F, 3F))
            )

        val scaleBy1ValueTransform = ImmutableAffineTransform.scale(2.5F)
        val scaleBy1ValueTriangle = MutableTriangle()
        scaleBy1ValueTransform.applyTransform(testTriangle, scaleBy1ValueTriangle)
        assertThat(scaleBy1ValueTriangle)
            .isEqualTo(
                MutableTriangle(
                    MutableVec(2.5F, 5F),
                    MutableVec(15F, -7.5F),
                    MutableVec(-10F, -15F)
                )
            )

        val scaleXTransform = ImmutableAffineTransform.scaleX(2.5F)
        val scaleXTriangle = MutableTriangle()
        scaleXTransform.applyTransform(testTriangle, scaleXTriangle)
        assertThat(scaleXTriangle)
            .isEqualTo(
                MutableTriangle(MutableVec(2.5F, 2F), MutableVec(15F, -3F), MutableVec(-10F, -6F))
            )

        val scaleYTransform = ImmutableAffineTransform.scaleY(2.5F)
        val scaleYTriangle = MutableTriangle()
        scaleYTransform.applyTransform(testTriangle, scaleYTriangle)
        assertThat(scaleYTriangle)
            .isEqualTo(
                MutableTriangle(MutableVec(1F, 5F), MutableVec(6F, -7.5F), MutableVec(-4F, -15F))
            )
    }

    @Test
    fun applyTransform_whenAppliedToAMutableTriangle_canModifyInputAsOutput() {
        val testMutableTriangle =
            MutableTriangle(MutableVec(1F, 2F), MutableVec(6F, -3F), MutableVec(-4F, -6F))

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(3F, -20F))
        translateTransform.applyTransform(testMutableTriangle, testMutableTriangle)
        assertThat(testMutableTriangle)
            .isEqualTo(
                MutableTriangle(MutableVec(4F, -18F), MutableVec(9F, -23F), MutableVec(-1F, -26F))
            )
    }

    @Test
    fun applyTransform_whenAppliedToABox_correctlyModifiesParallelogram() {
        val testBox = ImmutableBox.fromCenterAndDimensions(ImmutableVec(4f, 1f), 6f, 8f)

        val identityTransform = AffineTransform.IDENTITY
        val identityParallelogram = MutableParallelogram()
        identityTransform.applyTransform(testBox, identityParallelogram)
        assertThat(identityParallelogram)
            .isEqualTo(MutableParallelogram.fromCenterAndDimensions(MutableVec(4f, 1f), 6f, 8f))

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(1F, 3F))
        val translateParallelogram = MutableParallelogram()
        translateTransform.applyTransform(testBox, translateParallelogram)
        assertThat(translateParallelogram)
            .isEqualTo(MutableParallelogram.fromCenterAndDimensions(MutableVec(5f, 4f), 6f, 8f))

        val scaleBy2ValuesTransform = ImmutableAffineTransform.scale(2.5F, -.5F)
        val scaleBy2ValuesParallelogram = MutableParallelogram()
        scaleBy2ValuesTransform.applyTransform(testBox, scaleBy2ValuesParallelogram)
        assertThat(scaleBy2ValuesParallelogram)
            .isEqualTo(
                MutableParallelogram.fromCenterAndDimensions(MutableVec(10f, -0.5f), 15f, -4f)
            )

        val scaleBy1ValueTransform = ImmutableAffineTransform.scale(2.5F)
        val scaleBy1ValueParallelogram = MutableParallelogram()
        scaleBy1ValueTransform.applyTransform(testBox, scaleBy1ValueParallelogram)
        assertThat(scaleBy1ValueParallelogram)
            .isEqualTo(
                MutableParallelogram.fromCenterAndDimensions(MutableVec(10f, 2.5f), 15f, 20f)
            )

        val scaleXTransform = ImmutableAffineTransform.scaleX(2.5F)
        val scaleXParallelogram = MutableParallelogram()
        scaleXTransform.applyTransform(testBox, scaleXParallelogram)
        assertThat(scaleXParallelogram)
            .isEqualTo(MutableParallelogram.fromCenterAndDimensions(MutableVec(10f, 1f), 15f, 8f))

        val scaleYTransform = ImmutableAffineTransform.scaleY(2.5F)
        val scaleYParallelogram = MutableParallelogram()
        scaleYTransform.applyTransform(testBox, scaleYParallelogram)
        assertThat(scaleYParallelogram)
            .isEqualTo(MutableParallelogram.fromCenterAndDimensions(MutableVec(4f, 2.5f), 6f, 20f))

        val shearXTransform = ImmutableAffineTransform(1f, 2.5F, 0f, 0f, 1f, 0f)
        val shearXParallelogram = MutableParallelogram()
        shearXTransform.applyTransform(testBox, shearXParallelogram)
        assertThat(shearXParallelogram)
            .isEqualTo(
                MutableParallelogram.fromCenterDimensionsRotationAndShear(
                    MutableVec(6.5f, 1f),
                    6f,
                    8f,
                    0.0f,
                    2.5f,
                )
            )

        val sinPi = sin(Angle.HALF_TURN_RADIANS)
        val cosPi = cos(Angle.HALF_TURN_RADIANS)
        val rotateTransform = ImmutableAffineTransform(cosPi, -sinPi, 0f, sinPi, cosPi, 0f)
        val rotateParallelogram = MutableParallelogram()
        rotateTransform.applyTransform(testBox, rotateParallelogram)
        assertThat(
                Parallelogram.areNear(
                    rotateParallelogram,
                    MutableParallelogram.fromCenterDimensionsAndRotation(
                        MutableVec(-4f, -1f),
                        6f,
                        8f,
                        Angle.HALF_TURN_RADIANS,
                    ),
                )
            )
            .isTrue()
    }

    @Test
    fun applyTransform_whenAppliedToAParallelogram_correctlyModifiesParallelogram() {
        val testParallelogram =
            ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                ImmutableVec(4f, 1f),
                6f,
                8f,
                Angle.QUARTER_TURN_RADIANS,
                0.5f,
            )

        val identityTransform = AffineTransform.IDENTITY
        val identityParallelogram = MutableParallelogram()
        identityTransform.applyTransform(testParallelogram, identityParallelogram)
        assertThat(identityParallelogram)
            .isEqualTo(
                MutableParallelogram.fromCenterDimensionsRotationAndShear(
                    MutableVec(4f, 1f),
                    6f,
                    8f,
                    Angle.QUARTER_TURN_RADIANS,
                    0.5f,
                )
            )

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(1F, 3F))
        val translateParallelogram = MutableParallelogram()
        translateTransform.applyTransform(testParallelogram, translateParallelogram)
        assertThat(translateParallelogram)
            .isEqualTo(
                MutableParallelogram.fromCenterDimensionsRotationAndShear(
                    MutableVec(5f, 4f),
                    6f,
                    8f,
                    Angle.QUARTER_TURN_RADIANS,
                    0.5f,
                )
            )

        val scaleBy2ValuesTransform = ImmutableAffineTransform.scale(2.5F, -.5F)
        val scaleBy2ValuesParallelogram = MutableParallelogram()
        scaleBy2ValuesTransform.applyTransform(testParallelogram, scaleBy2ValuesParallelogram)
        assertThat(
                Parallelogram.areNear(
                    scaleBy2ValuesParallelogram,
                    MutableParallelogram.fromCenterDimensionsRotationAndShear(
                        MutableVec(10f, -0.5f),
                        3f,
                        -20f,
                        Angle.QUARTER_TURN_RADIANS + Angle.HALF_TURN_RADIANS,
                        -0.1f,
                    ),
                    tolerance = 0.0001f,
                )
            )
            .isTrue()

        val scaleBy1ValueTransform = ImmutableAffineTransform.scale(2.5F)
        val scaleBy1ValueParallelogram = MutableParallelogram()
        scaleBy1ValueTransform.applyTransform(testParallelogram, scaleBy1ValueParallelogram)
        assertThat(
                Parallelogram.areNear(
                    scaleBy1ValueParallelogram,
                    MutableParallelogram.fromCenterDimensionsRotationAndShear(
                        MutableVec(10f, 2.5f),
                        15f,
                        20f,
                        Angle.QUARTER_TURN_RADIANS,
                        0.5f,
                    ),
                    tolerance = 0.0001f,
                )
            )
            .isTrue()

        val scaleXTransform = ImmutableAffineTransform.scaleX(2.5F)
        val scaleXParallelogram = MutableParallelogram()
        scaleXTransform.applyTransform(testParallelogram, scaleXParallelogram)
        assertThat(
                Parallelogram.areNear(
                    scaleXParallelogram,
                    MutableParallelogram.fromCenterDimensionsRotationAndShear(
                        MutableVec(10f, 1f),
                        6f,
                        20f,
                        Angle.QUARTER_TURN_RADIANS,
                        0.2f,
                    ),
                    tolerance = 0.0001f,
                )
            )
            .isTrue()

        val scaleYTransform = ImmutableAffineTransform.scaleY(2.5F)
        val scaleYParallelogram = MutableParallelogram()
        scaleYTransform.applyTransform(testParallelogram, scaleYParallelogram)
        assertThat(
                Parallelogram.areNear(
                    scaleYParallelogram,
                    MutableParallelogram.fromCenterDimensionsRotationAndShear(
                        MutableVec(4f, 2.5f),
                        15f,
                        8f,
                        Angle.QUARTER_TURN_RADIANS,
                        1.25f,
                    ),
                    tolerance = 0.0001f,
                )
            )
            .isTrue()

        val sinPi = sin(Angle.HALF_TURN_RADIANS)
        val cosPi = cos(Angle.HALF_TURN_RADIANS)
        val rotateTransform = ImmutableAffineTransform(cosPi, -sinPi, 0f, sinPi, cosPi, 0f)
        val rotateParallelogram = MutableParallelogram()
        rotateTransform.applyTransform(testParallelogram, rotateParallelogram)
        assertThat(
                Parallelogram.areNear(
                    rotateParallelogram,
                    MutableParallelogram.fromCenterDimensionsRotationAndShear(
                        MutableVec(-4f, -1f),
                        6f,
                        8f,
                        Angle.HALF_TURN_RADIANS + Angle.QUARTER_TURN_RADIANS,
                        0.5f,
                    ),
                )
            )
            .isTrue()
    }

    @Test
    fun applyTransform_whenAppliedToAMutableParallelogram_canModifyInputAsOutput() {
        val testMutableParallelogram =
            MutableParallelogram.fromCenterDimensionsRotationAndShear(
                MutableVec(4f, 1f),
                6f,
                8f,
                Angle.QUARTER_TURN_RADIANS,
                0.5f,
            )

        val translateTransform = ImmutableAffineTransform.translate(ImmutableVec(1F, 3F))
        translateTransform.applyTransform(testMutableParallelogram, testMutableParallelogram)
        assertThat(testMutableParallelogram)
            .isEqualTo(
                MutableParallelogram.fromCenterDimensionsRotationAndShear(
                    MutableVec(5f, 4f),
                    6f,
                    8f,
                    Angle.QUARTER_TURN_RADIANS,
                    0.5f,
                )
            )
    }

    @Test
    fun constructWithValuesAndGetValues_shouldRoundTrip() {
        val affineTransform = ImmutableAffineTransform(1F, 2F, 3F, 4F, 5F, 6F)

        val outValues = FloatArray(6)
        affineTransform.getValues(outValues)

        assertThat(outValues)
            .usingExactEquality()
            .containsExactly(floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F))
    }

    @Test
    fun constructWithArrayAndGetValues_shouldRoundTrip() {
        val values = floatArrayOf(1F, 2F, 3F, 4F, 5F, 6F)
        val affineTransform = ImmutableAffineTransform(values)

        val outValues = FloatArray(6)
        affineTransform.getValues(outValues)

        assertThat(outValues).usingExactEquality().containsExactly(values)
    }

    @Test
    fun constructWithValues_shouldMatchConstructedWithFactoryFunctions() {
        assertThat(ImmutableAffineTransform(7F, 0F, 0F, 0F, 7F, 0F))
            .isEqualTo(ImmutableAffineTransform.scale(7F))

        assertThat(ImmutableAffineTransform(3F, 0F, 0F, 0F, 5F, 0F))
            .isEqualTo(ImmutableAffineTransform.scale(3F, 5F))

        assertThat(ImmutableAffineTransform(4F, 0F, 0F, 0F, 1F, 0F))
            .isEqualTo(ImmutableAffineTransform.scaleX(4F))

        assertThat(ImmutableAffineTransform(1F, 0F, 0F, 0F, 2F, 0F))
            .isEqualTo(ImmutableAffineTransform.scaleY(2F))

        assertThat(ImmutableAffineTransform(1F, 0F, 8F, 0F, 1F, 9F))
            .isEqualTo(ImmutableAffineTransform.translate(ImmutableVec(8F, 9F)))
    }

    @Test
    fun asImmutable_returnsSelf() {
        val affineTransform = ImmutableAffineTransform(A, B, C, D, E, F)

        val output = affineTransform.asImmutable()

        assertThat(output).isEqualTo(ImmutableAffineTransform(A, B, C, D, E, F))
        assertThat(output).isSameInstanceAs(affineTransform)
    }

    companion object {
        private const val A = 1f
        private const val B = 2f
        private const val C = 3f
        private const val D = 4f
        private const val E = 5f
        private const val F = 6f
        private const val DIFFERENT_A = -1f
        private const val DIFFERENT_B = -2f
        private const val DIFFERENT_C = -3f
        private const val DIFFERENT_D = -4f
        private const val DIFFERENT_E = -5f
        private const val DIFFERENT_F = -6f
    }
}
