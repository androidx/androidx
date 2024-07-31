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
class TriangleTest {

    fun signedArea_correctlyReturnsArea() {
        val triangle0 =
            ImmutableTriangle(ImmutableVec(-1f, -3f), ImmutableVec(3f, -3f), ImmutableVec(-3f, -1f))
        val triangle1 =
            MutableTriangle(ImmutableVec(1f, 1f), ImmutableVec(-5f, 4f), ImmutableVec(-1f, -2f))
        val triangle2 =
            ImmutableTriangle(ImmutableVec(-5f, 5f), ImmutableVec(2f, 4f), ImmutableVec(1f, -5f))
        val triangle3 =
            MutableTriangle(ImmutableVec(1f, -4f), ImmutableVec(3f, 1f), ImmutableVec(4f, 2f))

        assertThat(triangle0.signedArea).isWithin(1e-5f).of(4f)
        assertThat(triangle1.signedArea).isWithin(1e-5f).of(12f)
        assertThat(triangle2.signedArea).isWithin(1e-5f).of(-32f)
        assertThat(triangle3.signedArea).isWithin(1e-5f).of(-1.5f)
    }

    @Test
    fun signedArea_forDegenerateTriangle_correctlyReturnsArea() {
        val triangle0 =
            ImmutableTriangle(ImmutableVec(3f, 2f), ImmutableVec(5f, 2f), ImmutableVec(2f, 2f))
        val triangle1 =
            MutableTriangle(ImmutableVec(-1f, 2f), ImmutableVec(0f, 0f), ImmutableVec(1f, -2f))
        val triangle2 =
            ImmutableTriangle(ImmutableVec(0f, 1f), ImmutableVec(-2f, 3f), ImmutableVec(-2f, 3f))
        val triangle3 =
            MutableTriangle(ImmutableVec(5f, 2f), ImmutableVec(5f, 2f), ImmutableVec(5f, 2f))

        assertThat(triangle0.signedArea).isWithin(1e-5f).of(0f)
        assertThat(triangle1.signedArea).isWithin(1e-5f).of(0f)
        assertThat(triangle2.signedArea).isWithin(1e-5f).of(0f)
        assertThat(triangle3.signedArea).isWithin(1e-5f).of(0f)
    }

    @Test
    fun boundingBox_correctlyReturnsBoundingBox() {
        val triangle0 =
            MutableTriangle(ImmutableVec(1f, 1f), ImmutableVec(5f, 2f), ImmutableVec(2f, 2f))
        val triangle1 =
            ImmutableTriangle(ImmutableVec(-1f, -2f), ImmutableVec(0f, 0f), ImmutableVec(1f, -2f))
        val triangle2 =
            MutableTriangle(ImmutableVec(0f, 1f), ImmutableVec(-2f, 3f), ImmutableVec(-2f, 3f))
        val triangle3 =
            ImmutableTriangle(ImmutableVec(5f, 2f), ImmutableVec(5f, 2f), ImmutableVec(5f, 2f))

        assertThat(triangle0.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(1f, 1f), ImmutablePoint(5f, 2f)))
        assertThat(triangle1.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(-1f, -2f), ImmutablePoint(1f, 0f)))
        assertThat(triangle2.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(-2f, 1f), ImmutablePoint(0f, 3f)))
        assertThat(triangle3.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(5f, 2f), ImmutablePoint(5f, 2f)))
    }

    @Test
    fun boundingBox_forDegenerateTriangle_correctlyReturnsBoundingBox() {
        val triangle0 =
            MutableTriangle(ImmutableVec(3f, 2f), ImmutableVec(5f, 2f), ImmutableVec(2f, 2f))
        val triangle1 =
            MutableTriangle(ImmutableVec(-1f, 2f), ImmutableVec(0f, 0f), ImmutableVec(1f, -2f))
        val triangle2 =
            ImmutableTriangle(ImmutableVec(0f, 1f), ImmutableVec(-2f, 3f), ImmutableVec(-2f, 3f))
        val triangle3 =
            ImmutableTriangle(ImmutableVec(5f, 2f), ImmutableVec(5f, 2f), ImmutableVec(5f, 2f))

        assertThat(triangle0.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(2f, 2f), ImmutablePoint(5f, 2f)))
        assertThat(triangle1.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(-1f, -2f), ImmutablePoint(1f, 2f)))
        assertThat(triangle2.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(-2f, 1f), ImmutablePoint(0f, 3f)))
        assertThat(triangle3.boundingBox)
            .isEqualTo(ImmutableBox.fromTwoPoints(ImmutablePoint(5f, 2f), ImmutablePoint(5f, 2f)))
    }

    @Test
    fun populateBoundingBox_correctlyReturnsBoundingBox() {
        val triangle0 =
            MutableTriangle(ImmutableVec(1f, 1f), ImmutableVec(5f, 2f), ImmutableVec(2f, 2f))
        val triangle1 =
            ImmutableTriangle(ImmutableVec(-1f, -2f), ImmutableVec(0f, 0f), ImmutableVec(1f, -2f))
        val triangle2 =
            ImmutableTriangle(ImmutableVec(0f, 1f), ImmutableVec(-2f, 3f), ImmutableVec(-2f, 3f))
        val triangle3 =
            MutableTriangle(ImmutableVec(5f, 2f), ImmutableVec(5f, 2f), ImmutableVec(5f, 2f))
        val box0 = MutableBox()
        val box1 = MutableBox()
        val box2 = MutableBox()
        val box3 = MutableBox()

        triangle0.populateBoundingBox(box0)
        triangle1.populateBoundingBox(box1)
        triangle2.populateBoundingBox(box2)
        triangle3.populateBoundingBox(box3)

        assertThat(box0)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(1f, 1f), ImmutablePoint(5f, 2f))
            )
        assertThat(box1)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(-1f, -2f), ImmutablePoint(1f, 0f))
            )
        assertThat(box2)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(-2f, 1f), ImmutablePoint(0f, 3f))
            )
        assertThat(box3)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(5f, 2f), ImmutablePoint(5f, 2f))
            )
    }

    @Test
    fun populateBoundingBox_forDegenerateTriangle_correctlyReturnsBoundingBox() {
        val triangle0 =
            MutableTriangle(ImmutableVec(3f, 2f), ImmutableVec(5f, 2f), ImmutableVec(2f, 2f))
        val triangle1 =
            MutableTriangle(ImmutableVec(-1f, 2f), ImmutableVec(0f, 0f), ImmutableVec(1f, -2f))
        val triangle2 =
            ImmutableTriangle(ImmutableVec(0f, 1f), ImmutableVec(-2f, 3f), ImmutableVec(-2f, 3f))
        val triangle3 =
            ImmutableTriangle(ImmutableVec(5f, 2f), ImmutableVec(5f, 2f), ImmutableVec(5f, 2f))
        val box0 = MutableBox()
        val box1 = MutableBox()
        val box2 = MutableBox()
        val box3 = MutableBox()

        triangle0.populateBoundingBox(box0)
        triangle1.populateBoundingBox(box1)
        triangle2.populateBoundingBox(box2)
        triangle3.populateBoundingBox(box3)

        assertThat(box0)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(2f, 2f), ImmutablePoint(5f, 2f))
            )
        assertThat(box1)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(-1f, -2f), ImmutablePoint(1f, 2f))
            )
        assertThat(box2)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(-2f, 1f), ImmutablePoint(0f, 3f))
            )
        assertThat(box3)
            .isEqualTo(
                MutableBox().fillFromTwoPoints(ImmutablePoint(5f, 2f), ImmutablePoint(5f, 2f))
            )
    }
}
