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

package androidx.xr.scenecore

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SpatialModeChangeEventTest {

    private val pose1 = Pose.Identity
    private val pose2 = Pose(Vector3(1f, 2f, 3f))

    @Test
    fun equals_sameObject_returnsTrue() {
        val underTest = SpatialModeChangeEvent(pose1, 1.0f)
        assertThat(underTest.equals(underTest)).isTrue()
    }

    @Test
    fun equals_differentObjectsSameValues_returnsTrue() {
        val underTest1 = SpatialModeChangeEvent(pose1, 1.0f)
        val underTest2 = SpatialModeChangeEvent(pose1, 1.0f)
        assertThat(underTest1.equals(underTest2)).isTrue()
    }

    @Test
    fun equals_differentObjectsDifferentPose_returnsFalse() {
        val underTest1 = SpatialModeChangeEvent(pose1, 1.0f)
        val underTest2 = SpatialModeChangeEvent(pose2, 1.0f)
        assertThat(underTest1.equals(underTest2)).isFalse()
    }

    @Test
    fun equals_differentObjectsDifferentScale_returnsFalse() {
        val underTest1 = SpatialModeChangeEvent(pose1, 1.0f)
        val underTest2 = SpatialModeChangeEvent(pose1, 2.0f)
        assertThat(underTest1.equals(underTest2)).isFalse()
    }

    @Test
    fun equals_null_returnsFalse() {
        val underTest = SpatialModeChangeEvent(pose1, 1.0f)
        assertThat(underTest.equals(null)).isFalse()
    }

    @Test
    fun equals_differentType_returnsFalse() {
        val underTest = SpatialModeChangeEvent(pose1, 1.0f)
        val otherObject = "Not a SpatialModeChangeEvent"
        assertThat(underTest.equals(otherObject)).isFalse()
    }

    @Test
    fun hashCode_differentObjectsSameValues_returnsSameHashCode() {
        val underTest1 = SpatialModeChangeEvent(pose1, 1.0f)
        val underTest2 = SpatialModeChangeEvent(pose1, 1.0f)
        assertThat(underTest1.hashCode()).isEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentObjectsDifferentPose_returnsDifferentHashCodes() {
        val underTest1 = SpatialModeChangeEvent(pose1, 1.0f)
        val underTest2 = SpatialModeChangeEvent(pose2, 1.0f)
        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    @Test
    fun hashCode_differentObjectsDifferentScale_returnsDifferentHashCodes() {
        val underTest1 = SpatialModeChangeEvent(pose1, 1.0f)
        val underTest2 = SpatialModeChangeEvent(pose1, 2.0f)
        assertThat(underTest1.hashCode()).isNotEqualTo(underTest2.hashCode())
    }

    @Test
    fun toString_returnsCorrectStringRepresentation() {
        val underTest = SpatialModeChangeEvent(pose1, 1.0f)
        val expected = "SpatialModeChangeEvent(recommendedPose=$pose1, recommendedScale=1.0)"
        assertThat(underTest.toString()).isEqualTo(expected)
    }
}
