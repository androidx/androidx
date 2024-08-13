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
class ParallelogramInterfaceTest {

    @Test
    fun normalizeAndRun_withNegativeWidth_normalizesWidthHeightAndRotation() {
        val expectedWidth = 5f
        val expectedHeight = -3f
        val expectedRotation = Angle.QUARTER_TURN_RADIANS + Angle.HALF_TURN_RADIANS
        val assertExpectedValues: (Float, Float, Float) -> TestParallelogram =
            { normalizedWidth: Float, normalizedHeight: Float, normalizedRotation: Float ->
                assertThat(normalizedWidth).isEqualTo(expectedWidth)
                assertThat(normalizedHeight).isEqualTo(expectedHeight)
                assertThat(normalizedRotation).isWithin(tolerance).of(expectedRotation)
                TestParallelogram(
                    ImmutableVec(0f, 0f),
                    expectedWidth,
                    expectedHeight,
                    expectedRotation,
                    0f
                )
            }
        Parallelogram.normalizeAndRun(
            width = -5f,
            height = 3f,
            rotation = Angle.QUARTER_TURN_RADIANS,
            runBlock = assertExpectedValues,
        )
    }

    @Test
    fun normalizeAndRun_withHighRotation_normalizesRotation() {
        val expectedWidth = 5f
        val expectedHeight = 3f
        val expectedRotation = Angle.QUARTER_TURN_RADIANS // 5 Pi normalized to range [0, 2*pi]
        val assertExpectedValues: (Float, Float, Float) -> TestParallelogram =
            { normalizedWidth: Float, normalizedHeight: Float, normalizedRotation: Float ->
                assertThat(normalizedWidth).isEqualTo(expectedWidth)
                assertThat(normalizedHeight).isEqualTo(expectedHeight)
                assertThat(normalizedRotation).isWithin(tolerance).of(expectedRotation)
                TestParallelogram(
                    ImmutableVec(0f, 0f),
                    expectedWidth,
                    expectedHeight,
                    expectedRotation,
                    0f
                )
            }

        Parallelogram.normalizeAndRun(
            width = 5f,
            height = 3f,
            rotation = 5 * Angle.QUARTER_TURN_RADIANS,
            runBlock = assertExpectedValues,
        )
    }

    @Test
    fun signedArea_calculatesArea() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                runBlock = TestParallelogram.makeTestParallelogram,
            )
        assertThat(parallelogram.signedArea()).isEqualTo(15f)
    }

    private class TestParallelogram(
        override val center: ImmutableVec,
        override val width: Float,
        override val height: Float,
        override val rotation: Float,
        override val shearFactor: Float,
    ) : Parallelogram {
        companion object {
            val makeTestParallelogram = { w: Float, h: Float, r: Float ->
                TestParallelogram(ImmutableVec(0f, 0f), w, h, r, 0f)
            }
        }
    }

    private val tolerance = 0.000001f
}
