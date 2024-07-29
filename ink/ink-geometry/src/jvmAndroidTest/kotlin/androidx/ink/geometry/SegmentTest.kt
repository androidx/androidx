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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SegmentTest {

    @Test
    fun length_returnsCorrectValue() {
        assertThat(ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 1f)).length)
            .isEqualTo(sqrt(2f))
        assertThat(ImmutableSegment(ImmutableVec(-4f, 2f), ImmutableVec(0f, 5f)).length)
            .isEqualTo(5f)
        assertThat(ImmutableSegment(ImmutableVec(0f, 1f), ImmutableVec(-1f, 3f)).length)
            .isEqualTo(sqrt(5f))
        assertThat(ImmutableSegment(ImmutableVec(3f, 4f), ImmutableVec(-1f, -1f)).length)
            .isEqualTo(sqrt(41f))
    }

    @Test
    fun length_whenSegmentIsHorizontal_returnsCorrectValue() {
        assertThat(ImmutableSegment(ImmutableVec(1f, 1f), ImmutableVec(1f, -3f)).length)
            .isEqualTo(4f)
        assertThat(ImmutableSegment(ImmutableVec(3f, -2f), ImmutableVec(3f, 4f)).length)
            .isEqualTo(6f)
    }

    @Test
    fun length_whenSegmentIsVertical_returnsCorrectValue() {
        assertThat(ImmutableSegment(ImmutableVec(4f, 1f), ImmutableVec(5f, 1f)).length)
            .isEqualTo(1f)
        assertThat(ImmutableSegment(ImmutableVec(-1f, -5f), ImmutableVec(-3f, -5f)).length)
            .isEqualTo(2f)
    }

    @Test
    fun length_whenSegmentIsDegenerate_returnsZero() {
        assertThat(ImmutableSegment(ImmutableVec(4f, 1f), ImmutableVec(4f, 1f)).length)
            .isEqualTo(0f)
        assertThat(ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f)).length)
            .isEqualTo(0f)
    }

    @Test
    fun vec_fillsCorrectValues() {
        assertThat(
                ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 1f))
                    .vec
                    .isAlmostEqual(ImmutableVec(1f, 1f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(-4f, 2f), ImmutableVec(0f, 5f))
                    .vec
                    .isAlmostEqual(ImmutableVec(4f, 3f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(0f, 1f), ImmutableVec(-1f, 3f))
                    .vec
                    .isAlmostEqual(ImmutableVec(-1f, 2f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(3f, 4f), ImmutableVec(-1f, -1f))
                    .vec
                    .isAlmostEqual(ImmutableVec(-4f, -5f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(0.6f, 1.9f), ImmutableVec(-1.2f, 3.3f))
                    .vec
                    .isAlmostEqual(ImmutableVec(-1.8f, 1.4f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun vec_whenSegmentIsHorizontal_fillsCorrectValues() {
        assertThat(
                ImmutableSegment(ImmutableVec(1f, 1f), ImmutableVec(1f, -3f))
                    .vec
                    .isAlmostEqual(ImmutableVec(0f, -4f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(3f, -2f), ImmutableVec(3f, 4f))
                    .vec
                    .isAlmostEqual(ImmutableVec(0f, 6f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun vec_whenSegmentIsVertical_fillsCorrectValues() {
        assertThat(
                ImmutableSegment(ImmutableVec(4f, 1f), ImmutableVec(5f, 1f))
                    .vec
                    .isAlmostEqual(ImmutableVec(1f, 0f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(-1f, -5f), ImmutableVec(-3f, -5f))
                    .vec
                    .isAlmostEqual(ImmutableVec(-2f, 0f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun vec_whenSegmentIsDegenerate_fillsZeroes() {
        assertThat(
                ImmutableSegment(ImmutableVec(1f, -5f), ImmutableVec(1f, -5f))
                    .vec
                    .isAlmostEqual(ImmutableVec(0f, 0f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f))
                    .vec
                    .isAlmostEqual(ImmutableVec(0f, 0f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun populateVec_fillsCorrectValues() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 1f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(1f, 1f))

        ImmutableSegment(ImmutableVec(-4f, 2f), ImmutableVec(0f, 5f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(4f, 3f))

        ImmutableSegment(ImmutableVec(0f, 1f), ImmutableVec(-1f, 3f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-1f, 2f))

        ImmutableSegment(ImmutableVec(3f, 4f), ImmutableVec(-1f, -1f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-4f, -5f))

        ImmutableSegment(ImmutableVec(0.6f, 1.9f), ImmutableVec(-1.2f, 3.3f))
            .populateVec(mutableVec)
        assertThat(mutableVec.isAlmostEqual(MutableVec(-1.8f, 1.4f), 0.000001f)).isTrue()
    }

    @Test
    fun populateVec_whenSegmentIsHorizontal_fillsCorrectValues() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(1f, 1f), ImmutableVec(1f, -3f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(0f, -4f))

        ImmutableSegment(ImmutableVec(3f, -2f), ImmutableVec(3f, 4f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(0f, 6f))
    }

    @Test
    fun populateVec_whenSegmentIsVertical_fillsCorrectValues() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(4f, 1f), ImmutableVec(5f, 1f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(1f, 0f))

        ImmutableSegment(ImmutableVec(-1f, -5f), ImmutableVec(-3f, -5f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-2f, 0f))
    }

    @Test
    fun populateVec_whenSegmentIsDegenerate_fillsZeroes() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(1f, -5f), ImmutableVec(1f, -5f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(0f, 0f))

        ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f)).populateVec(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(0f, 0f))
    }

    @Test
    fun midpoint_fillsCorrectValues() {
        assertThat(
                ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 1f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(.5f, .5f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(-4f, 2f), ImmutableVec(0f, 5f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(-2f, 3.5f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(0f, 1f), ImmutableVec(-1f, 3f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(-.5f, 2f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(3f, 4f), ImmutableVec(-1f, -1f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(1f, 1.5f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(0.6f, 1.9f), ImmutableVec(-1.2f, 3.3f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(-.3f, 2.6f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun midpoint_whenSegmentIsHorizontal_fillsCorrectValues() {
        assertThat(
                ImmutableSegment(ImmutableVec(1f, 1f), ImmutableVec(1f, -3f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(1f, -1f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(3f, -2f), ImmutableVec(3f, 4f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(3f, 1f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun midpoint_whenSegmentIsVertical_fillsCorrectValues() {
        assertThat(
                ImmutableSegment(ImmutableVec(4f, 1f), ImmutableVec(5f, 1f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(4.5f, 1f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(-1f, -5f), ImmutableVec(-3f, -5f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(-2f, -5f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun midpoint_whenSegmentIsDegenerate_fillsZeroes() {
        assertThat(
                ImmutableSegment(ImmutableVec(1f, -5f), ImmutableVec(1f, -5f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(1f, -5f), 0.000001f)
            )
            .isTrue()

        assertThat(
                ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f))
                    .midpoint
                    .isAlmostEqual(ImmutableVec(0f, 0f), 0.000001f)
            )
            .isTrue()
    }

    @Test
    fun populateMidpoint_fillsCorrectValues() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 1f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(.5f, .5f))

        ImmutableSegment(ImmutableVec(-4f, 2f), ImmutableVec(0f, 5f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-2f, 3.5f))

        ImmutableSegment(ImmutableVec(0f, 1f), ImmutableVec(-1f, 3f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-.5f, 2f))

        ImmutableSegment(ImmutableVec(3f, 4f), ImmutableVec(-1f, -1f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(1f, 1.5f))

        ImmutableSegment(ImmutableVec(0.6f, 1.9f), ImmutableVec(-1.2f, 3.3f))
            .populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-.3f, 2.6f))
    }

    @Test
    fun populateMidpoint_whenSegmentIsHorizontal_fillsCorrectValues() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(1f, 1f), ImmutableVec(1f, -3f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(1f, -1f))

        ImmutableSegment(ImmutableVec(3f, -2f), ImmutableVec(3f, 4f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(3f, 1f))
    }

    @Test
    fun populateMidpoint_whenSegmentIsVertical_fillsCorrectValues() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(4f, 1f), ImmutableVec(5f, 1f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(4.5f, 1f))

        ImmutableSegment(ImmutableVec(-1f, -5f), ImmutableVec(-3f, -5f))
            .populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(-2f, -5f))
    }

    @Test
    fun populateMidpoint_whenSegmentIsDegenerate_fillsZeroes() {
        val mutableVec = MutableVec(0f, 0f)
        ImmutableSegment(ImmutableVec(1f, -5f), ImmutableVec(1f, -5f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(1f, -5f))

        ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f)).populateMidpoint(mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(0f, 0f))
    }

    @Test
    fun boundingBox_correctlyReturnsBoundingBox() {
        val segment0 = MutableSegment(ImmutableVec(1f, 1f), ImmutableVec(5f, 2f))
        val segment1 = ImmutableSegment(ImmutableVec(-1f, 2f), ImmutableVec(0f, 0f))

        assertThat(segment0.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(1f, 1f), ImmutablePoint(5f, 2f)))
        assertThat(segment1.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(-1f, 0f), ImmutablePoint(0f, 2f)))
    }

    @Test
    fun boundingBox_forDegenerateSegment_correctlyReturnsBoundingBox() {
        val segment0 = MutableSegment(ImmutableVec(3f, 2f), ImmutableVec(3f, 2f))
        val segment1 = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f))

        assertThat(segment0.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(3f, 2f), ImmutablePoint(3f, 2f)))
        assertThat(segment1.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(0f, 0f), ImmutablePoint(0f, 0f)))
    }

    @Test
    fun populateBoundingBox_correctlyReturnsBoundingBox() {
        val segment0 = MutableSegment(ImmutableVec(1f, 1f), ImmutableVec(5f, 2f))
        val segment1 = ImmutableSegment(ImmutableVec(-1f, 2f), ImmutableVec(0f, 0f))
        val box0 = MutableBox()
        val box1 = MutableBox()

        segment0.populateBoundingBox(box0)
        segment1.populateBoundingBox(box1)

        assertThat(box0)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1f, 1f), ImmutablePoint(5f, 2f))
            )
        assertThat(box1)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(-1f, 0f), ImmutablePoint(0f, 2f))
            )
    }

    @Test
    fun populateBoundingBox_forDegenerateSegment_correctlyReturnsBoundingBox() {
        val segment0 = MutableSegment(ImmutableVec(3f, 2f), ImmutableVec(3f, 2f))
        val segment1 = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(0f, 0f))
        val box0 = MutableBox()
        val box1 = MutableBox()

        segment0.populateBoundingBox(box0)
        segment1.populateBoundingBox(box1)

        assertThat(box0)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(3f, 2f), ImmutablePoint(3f, 2f))
            )
        assertThat(box1)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(0f, 0f), ImmutablePoint(0f, 0f))
            )
    }

    @Test
    fun lerpPoint_withZeroOrOneRatio_fillsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(6f, 3f), ImmutableVec(8f, -5f))

        assertThat(segment.lerpPoint(0.0f).isAlmostEqual(ImmutableVec(6f, 3f), 0.000001f)).isTrue()

        assertThat(segment.lerpPoint(1.0f).isAlmostEqual(ImmutableVec(8f, -5f), 0.000001f)).isTrue()
    }

    @Test
    fun lerpPoint_withRatioBetweenZeroAndOne_fillsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(6f, 3f), ImmutableVec(8f, -5f))

        assertThat(segment.lerpPoint(0.2f).isAlmostEqual(ImmutableVec(6.4f, 1.4f), 0.000001f))
            .isTrue()

        assertThat(segment.lerpPoint(0.5f).isAlmostEqual(ImmutableVec(7f, -1f), 0.000001f)).isTrue()

        assertThat(segment.lerpPoint(0.9f).isAlmostEqual(ImmutableVec(7.8f, -4.2f), 0.000001f))
            .isTrue()
    }

    @Test
    fun lerpPoint_withRatioOutsideZeroAndOne_fillsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(6f, 3f), ImmutableVec(8f, -5f))

        assertThat(segment.lerpPoint(-1f).isAlmostEqual(ImmutableVec(4f, 11f), 0.000001f)).isTrue()

        assertThat(segment.lerpPoint(1.3f).isAlmostEqual(ImmutableVec(8.6f, -7.4f), 0.000001f))
            .isTrue()
    }

    @Test
    fun populateLerpPoint_withZeroOrOneRatio_fillsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(6f, 3f), ImmutableVec(8f, -5f))
        val mutableVec = MutableVec(0f, 0f)

        segment.populateLerpPoint(0.0f, mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(6f, 3f))

        segment.populateLerpPoint(1.0f, mutableVec)
        assertThat(mutableVec).isEqualTo(MutableVec(8f, -5f))
    }

    @Test
    fun populateLerpPoint_withRatioBetweenZeroAndOne_fillsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(6f, 3f), ImmutableVec(8f, -5f))
        val mutableVec = MutableVec(0f, 0f)

        segment.populateLerpPoint(0.2f, mutableVec)
        assertThat(mutableVec.isAlmostEqual(MutableVec(6.4f, 1.4f), .000001f)).isTrue()

        segment.populateLerpPoint(0.5f, mutableVec)
        assertThat(mutableVec.isAlmostEqual(MutableVec(7f, -1f), .000001f)).isTrue()

        segment.populateLerpPoint(0.9f, mutableVec)
        assertThat(mutableVec.isAlmostEqual(MutableVec(7.8f, -4.2f), .000001f)).isTrue()
    }

    @Test
    fun populateLerpPoint_withRatioOutsideZeroAndOne_fillsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(6f, 3f), ImmutableVec(8f, -5f))
        val mutableVec = MutableVec(0f, 0f)

        segment.populateLerpPoint(-1f, mutableVec)
        assertThat(mutableVec.isAlmostEqual(MutableVec(4f, 11f), .000001f)).isTrue()

        segment.populateLerpPoint(1.3f, mutableVec)
        assertThat(mutableVec.isAlmostEqual(MutableVec(8.6f, -7.4f), .000001f)).isTrue()
    }

    @Test
    fun project_returnsCorrectValues() {
        val segment = ImmutableSegment(ImmutableVec(0f, 0f), ImmutableVec(1f, 1f))

        // On the endpoints.
        assertThat(segment.project(MutableVec(0f, 0f))).isEqualTo(0f)
        assertThat(segment.project(ImmutableVec(1f, 1f))).isEqualTo(1f)

        // On the segment.
        assertThat(segment.project(ImmutableVec(0.1f, 0.1f))).isEqualTo(0.1f)
        assertThat(segment.project(ImmutableVec(0.6f, 0.6f))).isEqualTo(0.6f)

        // On the line, but past the ends of the segment.
        assertThat(segment.project(ImmutableVec(-1f, -1f))).isEqualTo(-1.0f)
        assertThat(segment.project(ImmutableVec(2f, 2f))).isEqualTo(2.0f)
        assertThat(segment.project(ImmutableVec(-10f, -10f))).isEqualTo(-10f)
        assertThat(segment.project(ImmutableVec(50f, 50f))).isEqualTo(50f)

        // Off to the side of the line.
        assertThat(segment.project(ImmutableVec(0f, 1f))).isEqualTo(0.5f)
        assertThat(segment.project(ImmutableVec(1f, 0f))).isEqualTo(0.5f)
        assertThat(segment.project(ImmutableVec(0.7f, 0.2f))).isEqualTo(0.45f)
    }

    @Test
    fun project_degenerateSegment_throwsError() {
        // Degenerate segment.
        assertFailsWith<IllegalArgumentException> {
            ImmutableSegment(ImmutableVec(2f, 3f), MutableVec(2f, 3f)).project(ImmutableVec(1f, 1f))
        }

        // This segment is technically not degenerate, as the endpoints are different.
        // However, it's so small that it's squared length underflows to zero.
        assertFailsWith<IllegalArgumentException> {
            ImmutableSegment(ImmutableVec(0f, 0f), MutableVec(1e-23f, 1e-23f))
                .project(ImmutableVec(1f, 1f))
        }

        // Throws error for degenerate segments even if pointToProject is one of the endpoints.
        assertFailsWith<IllegalArgumentException> {
            ImmutableSegment(ImmutableVec(2f, 3f), MutableVec(2f, 3f)).project(ImmutableVec(2f, 3f))
        }
        assertFailsWith<IllegalArgumentException> {
            ImmutableSegment(ImmutableVec(0f, 0f), MutableVec(1e-23f, 1e-23f))
                .project(ImmutableVec(0f, 0f))
        }
        assertFailsWith<IllegalArgumentException> {
            ImmutableSegment(ImmutableVec(0f, 0f), MutableVec(1e-23f, 1e-23f))
                .project(ImmutableVec(1e-23f, 1e-23f))
        }
    }
}
