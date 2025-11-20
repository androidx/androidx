/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore.playservices

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import com.google.ar.core.Pose as ARCore1xPose
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PoseExtensionsTest {

    val tolerance = 1.0e-5f

    @Test
    fun toARCorePose_returnsCorrectARCorePose() {
        val arCorePose =
            ARCore1xPose(floatArrayOf(1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))

        assertPoseApproximatelyEquals(
            Pose(
                    translation = Vector3(1.0f, 2.0f, 3.0f),
                    rotation = Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
                )
                .toARCorePose(),
            arCorePose,
        )
    }

    @Test
    fun toRuntimePose_returnsCorrectRuntimePose() {
        val arCorePose =
            ARCore1xPose(floatArrayOf(1.0f, 2.0f, 3.0f), floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))

        assertPoseApproximatelyEquals(
            arCorePose.toRuntimePose(),
            Pose(
                translation = Vector3(1.0f, 2.0f, 3.0f),
                rotation = Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
            ),
        )
    }

    private fun assertPoseApproximatelyEquals(lhs: ARCore1xPose, rhs: ARCore1xPose) {
        assertThat(lhs.tx()).isWithin(tolerance).of(rhs.tx())
        assertThat(lhs.ty()).isWithin(tolerance).of(rhs.ty())
        assertThat(lhs.tz()).isWithin(tolerance).of(rhs.tz())
        assertThat(lhs.qx()).isWithin(tolerance).of(rhs.qx())
        assertThat(lhs.qy()).isWithin(tolerance).of(rhs.qy())
        assertThat(lhs.qz()).isWithin(tolerance).of(rhs.qz())
        assertThat(lhs.qw()).isWithin(tolerance).of(rhs.qw())
    }

    private fun assertPoseApproximatelyEquals(lhs: Pose, rhs: Pose) {
        assertPose(lhs, rhs, tolerance)
    }
}
