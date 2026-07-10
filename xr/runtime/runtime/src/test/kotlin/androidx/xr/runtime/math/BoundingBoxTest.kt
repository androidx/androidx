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
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BoundingBoxTest {

    @Test
    fun boxCreation_fromNanMin_causesIllegalArgumentException() {
        val minVecNanX = Vector3(Float.NaN, 2.0f, 3.0f)
        val minVecNanY = Vector3(1.0f, Float.NaN, 3.0f)
        val minVecNanZ = Vector3(1.0f, 2.0f, Float.NaN)
        val maxVec = Vector3(4.0f, 5.0f, 6.0f)

        var exception =
            assertFailsWith<IllegalArgumentException> { BoundingBox.fromMinMax(minVecNanX, maxVec) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("min [x=NaN, y=2.0, z=3.0] must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> { BoundingBox.fromMinMax(minVecNanY, maxVec) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("min [x=1.0, y=NaN, z=3.0] must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> { BoundingBox.fromMinMax(minVecNanZ, maxVec) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("min [x=1.0, y=2.0, z=NaN] must not contain NaN")
    }

    @Test
    fun boxCreation_fromNanMax_causesIllegalArgumentException() {
        val minVec = Vector3(1.0f, 2.0f, 3.0f)
        val maxVecNanX = Vector3(Float.NaN, 5.0f, 6.0f)
        val maxVecNanY = Vector3(4.0f, Float.NaN, 6.0f)
        val maxVecNanZ = Vector3(4.0f, 5.0f, Float.NaN)

        var exception =
            assertFailsWith<IllegalArgumentException> { BoundingBox.fromMinMax(minVec, maxVecNanX) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("max [x=NaN, y=5.0, z=6.0] must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> { BoundingBox.fromMinMax(minVec, maxVecNanY) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("max [x=4.0, y=NaN, z=6.0] must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> { BoundingBox.fromMinMax(minVec, maxVecNanZ) }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("max [x=4.0, y=5.0, z=NaN] must not contain NaN")
    }

    @Test
    fun boxCreation_fromMaxLessThanMin_causesIllegalArgumentException() {
        val minVec = Vector3(1.0f, 2.0f, 3.0f)
        val maxVecLessX = Vector3(0f, 5.0f, 6.0f)
        val maxVecLessY = Vector3(4.0f, 0f, 6.0f)
        val maxVecLessZ = Vector3(4.0f, 5.0f, 0f)

        var exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromMinMax(minVec, maxVecLessX)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("min.x (1.0) must be less than or equal to max.x (0.0)")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromMinMax(minVec, maxVecLessY)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("min.y (2.0) must be less than or equal to max.y (0.0)")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromMinMax(minVec, maxVecLessZ)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("min.z (3.0) must be less than or equal to max.z (0.0)")
    }

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
    fun boxCreation_fromNanCenter_causesIllegalArgumentException() {
        val centerNanX = Vector3(Float.NaN, 2.0f, 3.0f)
        val centerNanY = Vector3(1.0f, Float.NaN, 3.0f)
        val centerNanZ = Vector3(1.0f, 2.0f, Float.NaN)
        val halfExtents = FloatSize3d(1.0f, 1.0f, 1.0f)

        var exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(centerNanX, halfExtents)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("center [x=NaN, y=2.0, z=3.0] must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(centerNanY, halfExtents)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("center [x=1.0, y=NaN, z=3.0] must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(centerNanZ, halfExtents)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("center [x=1.0, y=2.0, z=NaN] must not contain NaN")
    }

    @Test
    fun boxCreation_fromNanHalfExtents_causesIllegalArgumentException() {
        val center = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtentsNanWidth = FloatSize3d(Float.NaN, 1.0f, 1.0f)
        val halfExtentsNanHeight = FloatSize3d(1.0f, Float.NaN, 1.0f)
        val halfExtentsNanDepth = FloatSize3d(1.0f, 1.0f, Float.NaN)

        var exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(center, halfExtentsNanWidth)
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("w NaN x h 1.0 x d 1.0 must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(center, halfExtentsNanHeight)
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("w 1.0 x h NaN x d 1.0 must not contain NaN")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(center, halfExtentsNanDepth)
            }

        assertThat(exception)
            .hasMessageThat()
            .contains("w 1.0 x h 1.0 x d NaN must not contain NaN")
    }

    @Test
    fun boxCreation_fromHalfExtentsLessThanZero_causesIllegalArgumentException() {
        val center = Vector3(1.0f, 2.0f, 3.0f)
        val halfExtentsNegativeWidth = FloatSize3d(-1.0f, 1.0f, 1.0f)
        val halfExtentsNegativeHeight = FloatSize3d(1.0f, -1.0f, 1.0f)
        val halfExtentsNegativeDepth = FloatSize3d(1.0f, 1.0f, -1.0f)

        var exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(center, halfExtentsNegativeWidth)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("halfExtents.width (-1.0) must be greater than or equal to 0")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(center, halfExtentsNegativeHeight)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("halfExtents.height (-1.0) must be greater than or equal to 0")

        exception =
            assertFailsWith<IllegalArgumentException> {
                BoundingBox.fromCenterAndHalfExtents(center, halfExtentsNegativeDepth)
            }

        assertThat(exception)
            .hasMessageThat()
            .isEqualTo("halfExtents.depth (-1.0) must be greater than or equal to 0")
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
