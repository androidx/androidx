/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.runtime.math

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BoundingBoxTest {

    @Test
    fun boxCreation_fromMinMax_propertiesAreSetCorrectly() {
        val minVec = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec = Vector3(4.0f, 5.0f, 6.0f)
        val center = (minVec + maxVec) * 0.5f
        val halfExtents = FloatSize3d.fromVector3((maxVec - minVec) * 0.5f)
        val box = BoundingBox.fromMinMax(minVec, maxVec)

        assertThat(box.min).isEqualTo(minVec)
        assertThat(box.max).isEqualTo(maxVec)
        assertThat(box.center).isEqualTo(center)
        assertThat(box.halfExtents).isEqualTo(halfExtents)
    }

    @Test
    fun boxCreation_fromCenterAndHalfExtents_propertiesAreSetCorrectly() {
        val center = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtents = FloatSize3d(1.0f, 1.0f, 1.0f)
        val halfExtentsVector = Vector3(halfExtents.width, halfExtents.height, halfExtents.depth)
        val min = center - halfExtentsVector
        val max = center + halfExtentsVector
        val box = BoundingBox.fromCenterAndHalfExtents(center, halfExtents)

        assertThat(box.min).isEqualTo(min)
        assertThat(box.max).isEqualTo(max)
        assertThat(box.center).isEqualTo(center)
        assertThat(box.halfExtents).isEqualTo(halfExtents)
    }

    @Test
    fun equals_fromMinMax_withIdenticalBoxes_returnsTrue() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val box1 = BoundingBox.fromMinMax(minVec1, maxVec1)
        val box2 = BoundingBox.fromMinMax(minVec1, maxVec1)

        assertThat(box1).isEqualTo(box2)
    }

    @Test
    fun equals_fromCenterAndHalfExtents_withIdenticalBoxes_returnsTrue() {
        val center1 = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtents1 = FloatSize3d(1.0f, 1.0f, 1.0f)
        val box1 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)
        val box2 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)

        assertThat(box1).isEqualTo(box2)
    }

    @Test
    fun equals_fromMinMax_withDifferentMinVector_returnsFalse() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val minVec2 = Vector3(4.0f, 5.0f, 6.0f)
        val maxVec1 = Vector3(7.0f, 8.0f, 9.0f)
        val box1 = BoundingBox.fromMinMax(minVec1, maxVec1)
        val box2 = BoundingBox.fromMinMax(minVec2, maxVec1)

        assertThat(box1).isNotEqualTo(box2)
    }

    @Test
    fun equals_fromCenterAndHalfExtents_withDifferentCenterVector_returnsFalse() {
        val center1 = Vector3(1.0f, 2.0f, 3.0f)
        val center2 = Vector3(4.0f, 5.0f, 6.0f)
        val halfExtents1 = FloatSize3d(1.0f, 1.0f, 1.0f)
        val box1 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)
        val box2 = BoundingBox.fromCenterAndHalfExtents(center2, halfExtents1)

        assertThat(box1).isNotEqualTo(box2)
    }

    @Test
    fun equals_fromMinMax_withDifferentMaxVector_returnsFalse() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val maxVec2 = Vector3(10.0f, 11.0f, 12.0f)
        val box1 = BoundingBox.fromMinMax(minVec1, maxVec1)
        val box2 = BoundingBox.fromMinMax(minVec1, maxVec2)

        assertThat(box1).isNotEqualTo(box2)
    }

    @Test
    fun equals_fromCenterAndHalfExtents_withDifferentHalfExtents_returnsFalse() {
        val center1 = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtents1 = FloatSize3d(1.0f, 1.0f, 1.0f)
        val halfExtents2 = FloatSize3d(2.0f, 2.0f, 2.0f)
        val box1 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)
        val box2 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents2)

        assertThat(box1).isNotEqualTo(box2)
    }

    @Test
    fun hashCode_fromMinMax_isConsistentForEqualObjects() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val box1 = BoundingBox.fromMinMax(minVec1, maxVec1)
        val box2 = BoundingBox.fromMinMax(minVec1, maxVec1)

        assertThat(box1.hashCode()).isEqualTo(box2.hashCode())
    }

    @Test
    fun hashCode_fromCenterAndHalfExtents_isConsistentForEqualObjects() {
        val center1 = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtents1 = FloatSize3d(1.0f, 1.0f, 1.0f)
        val box1 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)
        val box2 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)

        assertThat(box1.hashCode()).isEqualTo(box2.hashCode())
    }

    @Test
    fun hashCode_fromMinMax_isDifferentForUnequalObjects() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val minVec2 = Vector3(7.0f, 8.0f, 9.0f)
        val maxVec2 = Vector3(10.0f, 11.0f, 12.0f)
        val box1 = BoundingBox.fromMinMax(minVec1, maxVec1)
        val box2 = BoundingBox.fromMinMax(minVec2, maxVec2)

        assertThat(box1.hashCode()).isNotEqualTo(box2.hashCode())
    }

    @Test
    fun hashCode_fromCenterAndHalfExtents_isDifferentForUnequalObjects() {
        val center1 = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtents1 = FloatSize3d(1.0f, 1.0f, 1.0f)
        val center2 = Vector3(4.0f, 5.0f, 6.0f)
        val halfExtents2 = FloatSize3d(2.0f, 2.0f, 2.0f)
        val box1 = BoundingBox.fromCenterAndHalfExtents(center1, halfExtents1)
        val box2 = BoundingBox.fromCenterAndHalfExtents(center2, halfExtents2)

        assertThat(box1.hashCode()).isNotEqualTo(box2.hashCode())
    }

    @Test
    fun toString_fromMinMax_returnsCorrectFormat() {
        val minVec = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec = Vector3(4.0f, 5.0f, 6.0f)
        val box = BoundingBox.fromMinMax(minVec, maxVec)
        val expectedString =
            "BoundingBox(min=[x=1.0, y=2.0, z=3.0], max=[x=4.0, y=5.0, z=6.0], " +
                "center=[x=2.5, y=3.5, z=4.5], halfExtents=[width=1.5, height=1.5, depth=1.5])"

        assertThat(box.toString()).isEqualTo(expectedString)
    }

    @Test
    fun toString_fromCenterAndHalfExtents_returnsCorrectFormat() {
        val center = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtents = FloatSize3d(1.0f, 1.0f, 1.0f)
        val box = BoundingBox.fromCenterAndHalfExtents(center, halfExtents)
        val expectedString =
            "BoundingBox(min=[x=0.0, y=1.0, z=2.0], max=[x=2.0, y=3.0, z=4.0], " +
                "center=[x=1.0, y=2.0, z=3.0], halfExtents=[width=1.0, height=1.0, depth=1.0])"

        assertThat(box.toString()).isEqualTo(expectedString)
    }
}
