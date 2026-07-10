/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.scenecore.testing.internal

import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeTrackableComponentTest {

    private lateinit var underTest: FakeTrackableComponent
    private lateinit var poseFlow: MutableStateFlow<Pose>

    @Before
    fun setUp() {
        val initialPose = Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1f))
        poseFlow = MutableStateFlow(initialPose)

        underTest = FakeTrackableComponent(poseFlow = poseFlow)
    }

    @Test
    fun stateFlowUpdates_onAttach_updatesPerceptionPose() {
        val entity = FakeEntity()
        val expectedActivitySpacePose = Pose(Vector3(10f, 20f, 30f), Quaternion(0f, 0f, 0f, 1f))

        assertThat(entity.addComponent(underTest)).isTrue()

        poseFlow.value = expectedActivitySpacePose

        // The pose is updated after onAttach().
        assertThat(underTest.perceptionPose).isEqualTo(expectedActivitySpacePose)

        entity.removeComponent(underTest)

        poseFlow.value = Pose.Identity

        // The pose should not be updated after onDetach()
        assertThat(underTest.perceptionPose).isEqualTo(expectedActivitySpacePose)
    }
}
