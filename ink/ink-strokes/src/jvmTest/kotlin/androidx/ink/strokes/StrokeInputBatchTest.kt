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

package androidx.ink.strokes

import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.InputToolType
import com.google.common.truth.Truth.assertThat
import java.lang.IllegalArgumentException
import kotlin.collections.listOf
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Tests [ImmutableStrokeInputBatch] and [MutableStrokeInputBatch]. */
@RunWith(JUnit4::class)
internal class StrokeInputBatchTest {

    private val builder = MutableStrokeInputBatch()

    private fun createStylusInputWithOptionals(count: Int): StrokeInput =
        StrokeInput.create(
            x = count * 1f,
            y = count * 2f,
            elapsedTimeMillis = count * 3L,
            toolType = InputToolType.STYLUS,
            strokeUnitLengthCm = 0.5f,
            pressure = 0.5f,
            tiltRadians = 0.5f,
            orientationRadians = 0.5f,
        )

    @Test
    fun build_returnsObjectWithNativePointer() {
        val batch = MutableStrokeInputBatch().toImmutable()
        assertThat(batch).isNotNull()
        assertThat(batch.nativePointer).isNotEqualTo(0)
    }

    @Test
    fun add_input() {
        val firstInput = createStylusInputWithOptionals(1)
        assertThat(builder.add(firstInput)).isEqualTo(builder)
        assertThat(builder.nativePointer).isNotEqualTo(0)
        assertThat(builder.size).isEqualTo(1)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.nativePointer).isNotEqualTo(0)
        assertThat(batch.size).isEqualTo(1)
        assertThat(batch.get(0)).isEqualTo(firstInput)
    }

    @Test
    fun add_input_withChainedCalls() {
        val firstInput = createStylusInputWithOptionals(1)
        val secondInput = createStylusInputWithOptionals(2)
        assertThat(builder.add(firstInput).add(secondInput).size).isEqualTo(2)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(2)
        assertThat(batch.get(0)).isEqualTo(firstInput)
        assertThat(batch.get(1)).isEqualTo(secondInput)
    }

    @Test
    fun add_input_withBadValues_throwsIllegalArgumentException() {
        // Bad stroke unit length.
        val badStrokeUnitLength =
            StrokeInput.create(
                x = 1f,
                y = 1f,
                elapsedTimeMillis = 1L,
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = Float.POSITIVE_INFINITY,
            )
        val strokeUnitLengthError =
            assertFailsWith<IllegalArgumentException> { builder.add(badStrokeUnitLength) }
        assertThat(strokeUnitLengthError)
            .hasMessageThat()
            .contains(
                "If present, `StrokeInput::stroke_unit_length` must be finite and strictly positive. Got: infcm"
            )
        assertThat(builder.size).isEqualTo(0)
        assertThat(builder.toImmutable().size).isEqualTo(0)

        // Bad pressure.
        val badPressure = StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, pressure = 10000f)
        val pressureError = assertFailsWith<IllegalArgumentException> { builder.add(badPressure) }
        assertThat(pressureError)
            .hasMessageThat()
            .contains("`StrokeInput::pressure` must be -1 or in the range [0, 1]. Got: 10000")
        assertThat(builder.size).isEqualTo(0)
        assertThat(builder.toImmutable().size).isEqualTo(0)

        // Bad tilt.
        val badTilt = StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, tiltRadians = 1000f)
        val tiltError = assertFailsWith<IllegalArgumentException> { builder.add(badTilt) }
        assertThat(tiltError)
            .hasMessageThat()
            .contains("`StrokeInput::tilt` must be -1 or in the range [0, pi / 2]. Got: 318.31π")
        assertThat(builder.size).isEqualTo(0)
        assertThat(builder.toImmutable().size).isEqualTo(0)

        // Bad orientation.
        val badOrientation =
            StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, orientationRadians = 10000f)
        val orientationError =
            assertFailsWith<IllegalArgumentException> { builder.add(badOrientation) }
        assertThat(orientationError)
            .hasMessageThat()
            .contains(
                "`StrokeInput::orientation` must be -1 or in the range [0, 2 * pi). Got: 3183.1π"
            )
        assertThat(builder.size).isEqualTo(0)
        assertThat(builder.toImmutable().size).isEqualTo(0)
    }

    @Test
    fun add_explodedInput() {
        val firstInput = createStylusInputWithOptionals(1)
        assertThat(builder.add(firstInput)).isEqualTo(builder)
        assertThat(builder.nativePointer).isNotEqualTo(0)
        assertThat(builder.size).isEqualTo(1)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.nativePointer).isNotEqualTo(0)
        assertThat(batch.size).isEqualTo(1)
        assertThat(batch.get(0)).isEqualTo(firstInput)
    }

    @Test
    fun add_explodedInput_withChainedCalls() {
        val firstInput = StrokeInput.create(1f, 2f, 3L, InputToolType.STYLUS, 0.5f, 0.5f, 0.5f)
        val secondInput = StrokeInput.create(2f, 4f, 6L, InputToolType.STYLUS, 0.5f, 0.5f, 0.5f)
        assertThat(
                builder
                    .add(InputToolType.STYLUS, 1f, 2f, 3L, 0.5f, 0.5f, 0.5f)
                    .add(InputToolType.STYLUS, 2f, 4f, 6L, 0.5f, 0.5f, 0.5f)
                    .size
            )
            .isEqualTo(2)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(2)
        assertThat(batch.get(0)).isEqualTo(firstInput)
        assertThat(batch.get(1)).isEqualTo(secondInput)
    }

    @Test
    fun add_explodedInput_withBadValues_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            // Bad tilt, pressure, and orientation.
            builder.add(InputToolType.STYLUS, 1f, 1f, 1L, 10000f, 1000f, 1000f)
        }
        assertThat(builder.size).isEqualTo(0)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(0)
    }

    @Test
    fun add_collectionOfStrokeInputs() {
        val firstInput = createStylusInputWithOptionals(1)
        val secondInput = createStylusInputWithOptionals(2)
        assertThat(builder.add(listOf(firstInput, secondInput))).isEqualTo(builder)

        // Check builder.
        assertThat(builder.size).isEqualTo(2)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(2)
        assertThat(batch.get(0)).isEqualTo(firstInput)
        assertThat(batch.get(1)).isEqualTo(secondInput)
    }

    @Test
    fun add_withBadInput_throwsIllegalArgumentException() {
        val input = createStylusInputWithOptionals(1)
        assertFailsWith<IllegalArgumentException> { builder.add(listOf(input, input)) }
        assertThat(builder.size).isEqualTo(0)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(0)
    }

    @Test
    fun add_withStrokeInputBatch() {
        val firstInput = createStylusInputWithOptionals(1)
        val secondInput = createStylusInputWithOptionals(2)
        val thirdInput = createStylusInputWithOptionals(3)
        val fourthInput = createStylusInputWithOptionals(4)
        assertThat(builder.add(listOf(firstInput, secondInput))).isEqualTo(builder)

        val extraBatch =
            MutableStrokeInputBatch().add(listOf(thirdInput, fourthInput)).toImmutable()

        assertThat(builder.add(extraBatch).size).isEqualTo(4)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(4)
        assertThat(batch.get(0)).isEqualTo(firstInput)
        assertThat(batch.get(1)).isEqualTo(secondInput)
        assertThat(batch.get(2)).isEqualTo(thirdInput)
        assertThat(batch.get(3)).isEqualTo(fourthInput)
    }

    @Test
    fun add_withStrokeInputBatch_withDifferentToolType_throwsIllegalArgumentException() {
        val firstInput = StrokeInput.create(1f, 2f, 3L, InputToolType.TOUCH, 0.5f, 0.5f, 0.5f)
        val secondInput = StrokeInput.create(2f, 4f, 6L, InputToolType.TOUCH, 0.5f, 0.5f, 0.5f)
        val thirdInput = StrokeInput.create(3f, 6f, 9L, InputToolType.STYLUS, 0.5f, 0.5f, 0.5f)
        val fourthInput = StrokeInput.create(4f, 8f, 12L, InputToolType.STYLUS, 0.5f, 0.5f, 0.5f)
        assertThat(builder.add(listOf(firstInput, secondInput))).isEqualTo(builder)

        val stylusBatch =
            MutableStrokeInputBatch().add(listOf(thirdInput, fourthInput)).toImmutable()

        assertFailsWith<IllegalArgumentException> { builder.add(stylusBatch) }

        // Check builder.
        assertThat(builder.size).isEqualTo(2)

        // Check batch.
        val touchBatch = builder.toImmutable()
        assertThat(touchBatch.size).isEqualTo(2)
        assertThat(touchBatch.get(0)).isEqualTo(firstInput)
        assertThat(touchBatch.get(1)).isEqualTo(secondInput)
    }

    @Test
    fun add_withBadInput_throwsAnIllegalArgumentException() {
        val badPressure = StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, pressure = 10000f)
        assertFailsWith<IllegalArgumentException> { builder.add(badPressure) }
        assertFailsWith<IllegalArgumentException> { builder.add(listOf(badPressure)) }
        assertFailsWith<IllegalArgumentException> {
            builder.add(InputToolType.STYLUS, 1f, 1f, 1L, pressure = 10000f)
        }
    }

    @Test
    fun add_withMismatchedBatch_throwsAnIllegalArgumentException() {
        builder.add(createStylusInputWithOptionals(1))
        assertThat(builder.size).isEqualTo(1)

        val noOptionalsInput = StrokeInput.create(1f, 1f, 10L, builder.getToolType())
        val noOptionalsBatch = MutableStrokeInputBatch().add(noOptionalsInput).toImmutable()
        assertFailsWith<IllegalArgumentException> { builder.add(noOptionalsBatch) }
        assertThat(builder.size).isEqualTo(1)
    }

    @Test
    fun getAndOverwrite_correctlyMapsEnumsAcrossJNI() {
        val unknownInput =
            StrokeInput.create(1f, 2f, 3L, InputToolType.UNKNOWN, 0.5f, 0.5f, 0.5f, 0.5f)
        val mouseInput = StrokeInput.create(2f, 3f, 4L, InputToolType.MOUSE, 0.6f, 0.6f, 0.6f, 0.6f)
        val stylusInput =
            StrokeInput.create(3f, 4f, 3L, InputToolType.STYLUS, 0.5f, 0.5f, 0.5f, 0.5f)
        val touchInput = StrokeInput.create(4f, 5f, 4L, InputToolType.TOUCH, 0.6f, 0.6f, 0.6f, 0.6f)
        val outInput = StrokeInput.create(0f, 0f, 0L, InputToolType.STYLUS, 0f, 0f, 0f, 0f)

        // Check unknown batch.
        val unknownBatch = builder.add(unknownInput).toImmutable()
        assertThat(unknownBatch.size).isEqualTo(1)
        unknownBatch.populate(0, outInput)
        assertThat(outInput).isEqualTo(unknownInput)

        // Check mouse batch.
        builder.clear()
        val mouseBatch = builder.add(mouseInput).toImmutable()
        assertThat(mouseBatch.size).isEqualTo(1)
        mouseBatch.populate(0, outInput)
        assertThat(outInput).isEqualTo(mouseInput)

        // Check stylus batch.
        builder.clear()
        val stylusBatch = builder.add(stylusInput).toImmutable()
        assertThat(stylusBatch.size).isEqualTo(1)
        stylusBatch.populate(0, outInput)
        assertThat(outInput).isEqualTo(stylusInput)

        // Check touch batch.
        builder.clear()
        val touchBatch = builder.add(touchInput).toImmutable()
        assertThat(touchBatch.size).isEqualTo(1)
        touchBatch.populate(0, outInput)
        assertThat(outInput).isEqualTo(touchInput)
    }

    @Test
    fun get_withBadIndex_throwsIllegalArgumentException() {

        // Check batch.
        val batch = builder.toImmutable()
        // Index greater than size.
        assertFailsWith<IllegalArgumentException> { assertThat(batch.get(1)) }
        // Index greater less than 0.
        assertFailsWith<IllegalArgumentException> { assertThat(batch.get(-1)) }
    }

    @Test
    fun size_returnsSizeOfBatch() {
        assertThat(builder.size).isEqualTo(0)
        assertThat(
                builder
                    .add(createStylusInputWithOptionals(1))
                    .add(createStylusInputWithOptionals(2))
                    .add(createStylusInputWithOptionals(3))
                    .size
            )
            .isEqualTo(3)
        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(3)
    }

    @Test
    fun isEmpty_returnsTrue() {
        assertThat(builder.isEmpty()).isTrue()

        // Check batch.
        val emptyBatch = builder.toImmutable()
        assertThat(emptyBatch.isEmpty()).isTrue()
    }

    @Test
    fun isEmpty_returnsFalse() {
        assertThat(builder.add(createStylusInputWithOptionals(1)).isEmpty()).isFalse()

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.isEmpty()).isFalse()
    }

    @Test
    fun toolType_returnsToolTypeOfInputs() {
        val stylusInput = createStylusInputWithOptionals(1)
        assertThat(builder.add(stylusInput).getToolType()).isEqualTo(InputToolType.STYLUS)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.getToolType()).isEqualTo(InputToolType.STYLUS)
    }

    @Test
    fun toolType_afterInputsChangeType_returnsToolTypeOfInputs() {
        val stylusInput = createStylusInputWithOptionals(1)
        val unknownInput = StrokeInput.create(1f, 2f, 3L, InputToolType.UNKNOWN)
        val mouseInput = StrokeInput.create(2f, 3f, 4L, InputToolType.MOUSE)
        val touchInput = StrokeInput.create(1f, 1f, 1L, InputToolType.TOUCH)
        assertThat(builder.add(stylusInput).getToolType()).isEqualTo(InputToolType.STYLUS)
        builder.clear()
        assertThat(builder.add(unknownInput).getToolType()).isEqualTo(InputToolType.UNKNOWN)
        builder.clear()
        assertThat(builder.add(mouseInput).getToolType()).isEqualTo(InputToolType.MOUSE)
        builder.clear()
        assertThat(builder.add(touchInput).getToolType()).isEqualTo(InputToolType.TOUCH)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.getToolType()).isEqualTo(InputToolType.TOUCH)
    }

    @Test
    fun strokeUnitLengthCm_returnsZeroIfUnset() {
        assertThat(builder.getStrokeUnitLengthCm()).isEqualTo(0f)
        assertThat(builder.toImmutable().hasStrokeUnitLength()).isFalse()
        assertThat(builder.toImmutable().getStrokeUnitLengthCm()).isEqualTo(0f)

        builder.add(InputToolType.MOUSE, 1f, 2f, 3L)
        assertThat(builder.getStrokeUnitLengthCm()).isEqualTo(0f)
        assertThat(builder.toImmutable().hasStrokeUnitLength()).isFalse()
        assertThat(builder.toImmutable().getStrokeUnitLengthCm()).isEqualTo(0f)
    }

    @Test
    fun strokeUnitLengthCm_returnsValueIfSet() {
        builder.add(InputToolType.MOUSE, 1f, 2f, 3L, strokeUnitLengthCm = 123f)
        assertThat(builder.getStrokeUnitLengthCm()).isEqualTo(123f)
        assertThat(builder.toImmutable().hasStrokeUnitLength()).isTrue()
        assertThat(builder.toImmutable().getStrokeUnitLengthCm()).isEqualTo(123f)
    }

    @Test
    @OptIn(ExperimentalInkCustomBrushApi::class)
    fun getNoiseSeed_returnsZeroIfUnset() {
        assertThat(builder.getNoiseSeed()).isEqualTo(0)
        assertThat(builder.toImmutable().getNoiseSeed()).isEqualTo(0)

        builder.add(InputToolType.MOUSE, 1f, 2f, 3L)
        assertThat(builder.getNoiseSeed()).isEqualTo(0)
        assertThat(builder.toImmutable().getNoiseSeed()).isEqualTo(0)
    }

    @Test
    @OptIn(ExperimentalInkCustomBrushApi::class)
    fun getNoiseSeed_returnsValueIfSet() {
        builder.setNoiseSeed(12345)
        assertThat(builder.getNoiseSeed()).isEqualTo(12345)
        assertThat(builder.toImmutable().getNoiseSeed()).isEqualTo(12345)

        builder.add(InputToolType.MOUSE, 1f, 2f, 3L)
        assertThat(builder.getNoiseSeed()).isEqualTo(12345)
        assertThat(builder.toImmutable().getNoiseSeed()).isEqualTo(12345)
    }

    @Test
    fun clear_removesAllInput() {
        assertThat(
                builder
                    .add(createStylusInputWithOptionals(1))
                    .add(createStylusInputWithOptionals(2))
                    .size
            )
            .isEqualTo(2)
        builder.clear()
        assertThat(builder.size).isEqualTo(0)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.size).isEqualTo(0)
    }

    @Test
    fun hasPressure_withPressure_returnsTrue() {
        val pressureInput = StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, pressure = 0.5f)
        assertThat(builder.add(pressureInput)).isEqualTo(builder)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.hasPressure()).isTrue()
    }

    @Test
    fun hasPressure_withoutPressure_returnsFalse() {
        val noPressureInput =
            StrokeInput.create(
                1f,
                1f,
                1L,
                InputToolType.STYLUS,
                tiltRadians = 0.5f,
                orientationRadians = 0.5f,
            )
        assertThat(builder.add(noPressureInput)).isEqualTo(builder)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.hasPressure()).isFalse()
    }

    @Test
    fun hasTilt_withTilt_returnsTrue() {
        val tiltInput = StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, tiltRadians = 0.5f)
        assertThat(builder.add(tiltInput)).isEqualTo(builder)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.hasTilt()).isTrue()
    }

    @Test
    fun hasTilt_withoutTilt_returnsFalse() {
        val noTiltInput =
            StrokeInput.create(
                1f,
                1f,
                1L,
                InputToolType.STYLUS,
                pressure = 0.5f,
                orientationRadians = 0.5f,
            )
        assertThat(builder.add(noTiltInput)).isEqualTo(builder)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.hasTilt()).isFalse()
    }

    @Test
    fun hasOrientation_withOrientation_returnsTrue() {
        val orientationInput =
            StrokeInput.create(1f, 1f, 1L, InputToolType.STYLUS, orientationRadians = 0.5f)
        assertThat(builder.add(orientationInput)).isEqualTo(builder)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.hasOrientation()).isTrue()
    }

    @Test
    fun hasOrientation_withoutOrientation_returnsFalse() {
        val noOrientationInput =
            StrokeInput.create(
                1f,
                1f,
                1L,
                InputToolType.STYLUS,
                pressure = 0.5f,
                tiltRadians = 0.5f,
            )
        assertThat(builder.add(noOrientationInput)).isEqualTo(builder)

        // Check batch.
        val batch = builder.toImmutable()
        assertThat(batch.hasOrientation()).isFalse()
    }

    @Test
    fun durationMillis_WhenEmptyBatch_shouldBeZero() {
        val batch = builder.toImmutable()
        assertThat(batch.getDurationMillis()).isEqualTo(0L)
    }

    @Test
    fun durationMillis_WhenMultipleInputBatch_shouldBeNonZero() {
        val batch =
            builder
                .add(createStylusInputWithOptionals(1))
                .add(createStylusInputWithOptionals(2))
                .add(createStylusInputWithOptionals(3))
                .toImmutable()
        assertThat(batch.getDurationMillis()).isEqualTo(6)
    }
}
