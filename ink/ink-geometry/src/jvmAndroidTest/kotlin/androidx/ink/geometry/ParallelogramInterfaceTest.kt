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
        val assertExpectedValues: (Float, Float, Float) -> Parallelogram =
            { normalizedWidth: Float, normalizedHeight: Float, normalizedRotation: Float ->
                assertThat(normalizedWidth).isEqualTo(expectedWidth)
                assertThat(normalizedHeight).isEqualTo(expectedHeight)
                assertThat(normalizedRotation).isWithin(tolerance).of(expectedRotation)
                ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                    ImmutableVec(0f, 0f),
                    expectedWidth,
                    expectedHeight,
                    expectedRotation,
                    0f,
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
        val assertExpectedValues: (Float, Float, Float) -> Parallelogram =
            { normalizedWidth: Float, normalizedHeight: Float, normalizedRotation: Float ->
                assertThat(normalizedWidth).isEqualTo(expectedWidth)
                assertThat(normalizedHeight).isEqualTo(expectedHeight)
                assertThat(normalizedRotation).isWithin(tolerance).of(expectedRotation)
                ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                    ImmutableVec(0f, 0f),
                    expectedWidth,
                    expectedHeight,
                    expectedRotation,
                    0f,
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
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        0f,
                    )
                },
            )
        assertThat(parallelogram.computeSignedArea()).isEqualTo(15f)
    }

    @Test
    fun computeBoundingBox_returnsCorrectBoundingBoxNoShear() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        0f,
                    )
                },
            )
        assertThat(
                parallelogram
                    .computeBoundingBox()
                    .isAlmostEqual(
                        ImmutableBox.fromTwoPoints(
                            ImmutableVec(-1.5f, -2.5f),
                            ImmutableVec(1.5f, 2.5f),
                        ),
                        tolerance,
                    )
            )
            .isTrue()
    }

    @Test
    fun computeBoundingBox_populatesBoundingBoxNoShear() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.QUARTER_TURN_RADIANS,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        0f,
                    )
                },
            )
        val box = MutableBox()
        parallelogram.computeBoundingBox(box)
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(
                        ImmutableVec(-1.5f, -2.5f),
                        ImmutableVec(1.5f, 2.5f),
                    ),
                    tolerance,
                )
            )
            .isTrue()
    }

    @Test
    fun computeBoundingBox_returnsCorrectBoundingBoxWithShear() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        assertThat(
                parallelogram
                    .computeBoundingBox()
                    .isAlmostEqual(
                        ImmutableBox.fromTwoPoints(
                            ImmutableVec(-5.5f, -1.5f),
                            ImmutableVec(5.5f, 1.5f),
                        ),
                        tolerance,
                    )
            )
            .isTrue()
    }

    @Test
    fun computeBoundingBox_populatesBoundingBoxWithShear() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val box = MutableBox()
        parallelogram.computeBoundingBox(box)
        assertThat(
                box.isAlmostEqual(
                    ImmutableBox.fromTwoPoints(
                        ImmutableVec(-5.5f, -1.5f),
                        ImmutableVec(5.5f, 1.5f),
                    ),
                    tolerance,
                )
            )
            .isTrue()
    }

    @Test
    fun computeSemiAxes_returnsCorrectSemiAxes() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val axes = parallelogram.computeSemiAxes()
        assertThat(axes.size).isEqualTo(2)
        assertThat(axes.get(0).isAlmostEqual(ImmutableVec(2.5f, 0f), tolerance)).isTrue()
        assertThat(axes.get(1).isAlmostEqual(ImmutableVec(3f, 1.5f), tolerance)).isTrue()
    }

    @Test
    fun computeSemiAxes_populatesSemiAxes() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val axis1 = MutableVec()
        val axis2 = MutableVec()
        parallelogram.computeSemiAxes(axis1, axis2)
        assertThat(axis1.isAlmostEqual(ImmutableVec(2.5f, 0f), tolerance)).isTrue()
        assertThat(axis2.isAlmostEqual(ImmutableVec(3f, 1.5f), tolerance)).isTrue()
    }

    @Test
    fun computeCorners_returnsCorrectCorners() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val corners = parallelogram.computeCorners()
        assertThat(corners.size).isEqualTo(4)
        assertThat(corners.get(0).isAlmostEqual(ImmutableVec(-5.5f, -1.5f), tolerance)).isTrue()
        assertThat(corners.get(1).isAlmostEqual(ImmutableVec(-0.5f, -1.5f), tolerance)).isTrue()
        assertThat(corners.get(2).isAlmostEqual(ImmutableVec(5.5f, 1.5f), tolerance)).isTrue()
        assertThat(corners.get(3).isAlmostEqual(ImmutableVec(0.5f, 1.5f), tolerance)).isTrue()
    }

    @Test
    fun isAlmostEqual_withToleranceGiven_returnsCorrectValue() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val corner1 = MutableVec()
        val corner2 = MutableVec()
        val corner3 = MutableVec()
        val corner4 = MutableVec()
        parallelogram.computeCorners(corner1, corner2, corner3, corner4)

        assertThat(corner1.isAlmostEqual(ImmutableVec(-5.5f, -1.5f), tolerance)).isTrue()
        assertThat(corner2.isAlmostEqual(ImmutableVec(-0.5f, -1.5f), tolerance)).isTrue()
        assertThat(corner3.isAlmostEqual(ImmutableVec(5.5f, 1.5f), tolerance)).isTrue()
        assertThat(corner4.isAlmostEqual(ImmutableVec(0.5f, 1.5f), tolerance)).isTrue()
    }

    @Test
    fun contains_returnsCorrectValue() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        // Center of the parallelogram
        assertThat(parallelogram.contains(ImmutableVec(0f, 0f))).isTrue()
        // On one of the lines
        assertThat(parallelogram.contains(ImmutableVec(2f, 1.5f))).isTrue()
        // Outside the parallelogram
        assertThat(parallelogram.contains(ImmutableVec(2f, 2f))).isFalse()
    }

    @Test
    fun isAlmostEqual_withinToleranceReturnsTrue() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val other =
            Parallelogram.normalizeAndRun(
                width = 5.0000009f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        assertThat(parallelogram.isAlmostEqual(other, tolerance)).isTrue()
    }

    @Test
    fun isAlmostEqual_outsideToleranceReturnsFalse() {
        val parallelogram =
            Parallelogram.normalizeAndRun(
                width = 5f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        val other =
            Parallelogram.normalizeAndRun(
                width = 5.000009f,
                height = 3f,
                rotation = Angle.ZERO,
                runBlock = { w: Float, h: Float, r: Float ->
                    ImmutableParallelogram.fromCenterDimensionsRotationAndShear(
                        ImmutableVec(0f, 0f),
                        w,
                        h,
                        r,
                        2f,
                    )
                },
            )
        assertThat(parallelogram.isAlmostEqual(other, tolerance)).isFalse()
    }

    private val tolerance = 0.000001f
}
