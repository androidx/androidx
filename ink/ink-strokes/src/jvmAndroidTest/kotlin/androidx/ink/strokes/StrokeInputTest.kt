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

import androidx.ink.brush.InputToolType
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StrokeInputTest {

    @Test
    fun toString_withValues() {
        val input = StrokeInput()
        input.update(
            x = 2f,
            y = 3f,
            elapsedTimeMillis = 4L,
            toolType = InputToolType.STYLUS,
            strokeUnitLengthCm = 0.1f,
            pressure = 0.2f,
            tiltRadians = 0.3f,
            orientationRadians = 0.4f,
        )
        assertThat(input.toString())
            .isEqualTo(
                "StrokeInput(x=2.0, y=3.0, elapsedTimeMillis=4, toolType=InputToolType.STYLUS, strokeUnitLengthCm=0.1, pressure=0.2, tiltRadians=0.3, orientationRadians=0.4)"
            )
    }

    @Test
    fun overwrite_shouldReassignValues() {
        val input = StrokeInput()
        input.update(1f, 2f, 3L, InputToolType.TOUCH)
        input.update(2f, 3f, 4L, InputToolType.STYLUS, 0.1f, 0.2f, 0.3f, 0.4f)
        assertThat(input).isNotNull()
        assertThat(input.x).isEqualTo(2f)
        assertThat(input.y).isEqualTo(3f)
        assertThat(input.elapsedTimeMillis).isEqualTo(4L)
        assertThat(input.toolType).isEqualTo(InputToolType.STYLUS)
        assertThat(input.strokeUnitLengthCm).isEqualTo(0.1f)
        assertThat(input.pressure).isEqualTo(0.2f)
        assertThat(input.tiltRadians).isEqualTo(0.3f)
        assertThat(input.orientationRadians).isEqualTo(0.4f)
    }

    @Test
    fun overwrite_withDefaultValues_shouldReassignValues() {
        val input = StrokeInput()
        input.update(
            x = 1f,
            y = 2f,
            elapsedTimeMillis = 3L,
            toolType = InputToolType.STYLUS,
            strokeUnitLengthCm = 0.4f,
            pressure = 0.5f,
            tiltRadians = 0.7f,
            orientationRadians = 0.9F,
        )
        input.update(2f, 3f, 4L, InputToolType.TOUCH)
        assertThat(input).isNotNull()
        assertThat(input.x).isEqualTo(2f)
        assertThat(input.y).isEqualTo(3f)
        assertThat(input.elapsedTimeMillis).isEqualTo(4L)
        assertThat(input.toolType).isEqualTo(InputToolType.TOUCH)
        assertThat(input.strokeUnitLengthCm).isEqualTo(StrokeInput.NO_STROKE_UNIT_LENGTH)
        assertThat(input.pressure).isEqualTo(StrokeInput.NO_PRESSURE)
        assertThat(input.tiltRadians).isEqualTo(StrokeInput.NO_TILT)
        assertThat(input.orientationRadians).isEqualTo(StrokeInput.NO_ORIENTATION)
    }

    @Test
    fun equals_whenSame_shouldReturnTrueAndHaveSameHashCode() {
        val input1 =
            StrokeInput.create(
                x = 1F,
                y = 2F,
                elapsedTimeMillis = 3L,
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = 4F,
                pressure = 5F,
                tiltRadians = 6F,
                orientationRadians = 7F,
            )
        val input2 =
            StrokeInput.create(
                x = 1F,
                y = 2F,
                elapsedTimeMillis = 3L,
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = 4F,
                pressure = 5F,
                tiltRadians = 6F,
                orientationRadians = 7F,
            )

        // Same instance.
        assertThat(input1).isEqualTo(input1)
        assertThat(input2).isEqualTo(input2)
        assertThat(input1.hashCode()).isEqualTo(input1.hashCode())
        assertThat(input2.hashCode()).isEqualTo(input2.hashCode())
        // Different instance, same values.
        assertThat(input2).isEqualTo(input1)
        assertThat(input1.hashCode()).isEqualTo(input2.hashCode())
    }

    @Test
    fun equals_whenOneValueDifferent_shouldReturnFalse() {
        val input =
            StrokeInput.create(
                x = 1F,
                y = 2F,
                elapsedTimeMillis = 3L,
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = 4F,
                pressure = 5F,
                tiltRadians = 6F,
                orientationRadians = 7F,
            )

        assertThat(
                StrokeInput.create(
                    x = 999F,
                    y = 2F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 4F,
                    pressure = 5F,
                    tiltRadians = 6F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 999F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 4F,
                    pressure = 5F,
                    tiltRadians = 6F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 2F,
                    elapsedTimeMillis = 999L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 4F,
                    pressure = 5F,
                    tiltRadians = 6F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 2F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.MOUSE,
                    strokeUnitLengthCm = 4F,
                    pressure = 5F,
                    tiltRadians = 6F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 2F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 999F,
                    pressure = 5F,
                    tiltRadians = 6F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 2F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 4F,
                    pressure = 999F,
                    tiltRadians = 6F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 2F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 4F,
                    pressure = 5F,
                    tiltRadians = 999F,
                    orientationRadians = 7F,
                )
            )
            .isNotEqualTo(input)

        assertThat(
                StrokeInput.create(
                    x = 1F,
                    y = 2F,
                    elapsedTimeMillis = 3L,
                    toolType = InputToolType.STYLUS,
                    strokeUnitLengthCm = 4F,
                    pressure = 5F,
                    tiltRadians = 6F,
                    orientationRadians = 999F,
                )
            )
            .isNotEqualTo(input)
    }
}
