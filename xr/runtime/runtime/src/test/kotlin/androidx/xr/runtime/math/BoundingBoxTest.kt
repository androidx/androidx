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
    fun boxCreation_propertiesAreSetCorrectly() {
        val minVec = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec = Vector3(4.0f, 5.0f, 6.0f)
        val box = BoundingBox(minVec, maxVec)

        assertThat(box.min).isEqualTo(minVec)
        assertThat(box.max).isEqualTo(maxVec)
    }

    @Test
    fun equals_withIdenticalBoxes_returnsTrue() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val box1 = BoundingBox(minVec1, maxVec1)
        val box2 = BoundingBox(minVec1, maxVec1)

        assertThat(box1).isEqualTo(box2)
    }

    @Test
    fun equals_withDifferentMinVector_returnsFalse() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val minVec2 = Vector3(7.0f, 8.0f, 9.0f)
        val box1 = BoundingBox(minVec1, maxVec1)
        val box2 = BoundingBox(minVec2, maxVec1)

        assertThat(box1).isNotEqualTo(box2)
    }

    @Test
    fun equals_withDifferentMaxVector_returnsFalse() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val maxVec2 = Vector3(10.0f, 11.0f, 12.0f)
        val box1 = BoundingBox(minVec1, maxVec1)
        val box2 = BoundingBox(minVec1, maxVec2)

        assertThat(box1).isNotEqualTo(box2)
    }

    @Test
    fun hashCode_isConsistentForEqualObjects() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val box1 = BoundingBox(minVec1, maxVec1)
        val box2 = BoundingBox(minVec1, maxVec1)

        assertThat(box1.hashCode()).isEqualTo(box2.hashCode())
    }

    @Test
    fun hashCode_isDifferentForUnequalObjects() {
        val minVec1 = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec1 = Vector3(4.0f, 5.0f, 6.0f)
        val minVec2 = Vector3(7.0f, 8.0f, 9.0f)
        val maxVec2 = Vector3(10.0f, 11.0f, 12.0f)
        val box1 = BoundingBox(minVec1, maxVec1)
        val box2 = BoundingBox(minVec2, maxVec2)

        assertThat(box1.hashCode()).isNotEqualTo(box2.hashCode())
    }

    @Test
    fun toString_returnsCorrectFormat() {
        val minVec = Vector3(1.0f, 2.0f, 3.0f)
        val maxVec = Vector3(4.0f, 5.0f, 6.0f)
        val box = BoundingBox(minVec, maxVec)
        val expectedString = "BoundingBox(min=[x=1.0, y=2.0, z=3.0], max=[x=4.0, y=5.0, z=6.0])"

        assertThat(box.toString()).isEqualTo(expectedString)
    }
}
