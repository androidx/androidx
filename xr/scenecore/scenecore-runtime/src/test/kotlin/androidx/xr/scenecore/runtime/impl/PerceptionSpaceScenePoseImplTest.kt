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
package androidx.xr.scenecore.runtime.impl

import androidx.xr.runtime.math.Matrix4.Companion.fromTrs
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion.Companion.fromEulerAngles
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.ActivitySpace
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PerceptionSpaceScenePoseImplTest {
    private val activitySpace = mock<ActivitySpace>()

    private lateinit var perceptionSpaceScenePose: PerceptionSpaceScenePoseImpl

    @Before
    fun setUp() {
        perceptionSpaceScenePose = PerceptionSpaceScenePoseImpl(activitySpace)
        whenever(activitySpace.worldSpaceScale).thenReturn(Vector3.One)
    }

    @Test
    fun getActivitySpacePose_returnsInverseOfActivitySpaceMatrix() {
        val activitySpaceMatrix =
            fromTrs(
                Vector3(1.0f, 2.0f, 3.0f),
                fromEulerAngles(Vector3(0f, 0f, 90f)),
                Vector3(1.0f, 1.0f, 1.0f),
            )
        whenever(activitySpace.poseInOpenXrReferenceSpace).thenReturn(activitySpaceMatrix.toPose())

        val activitySpacePose = perceptionSpaceScenePose.activitySpacePose

        val expectedPose = activitySpaceMatrix.inverse.toPose()

        assertPose(activitySpacePose, expectedPose)
    }

    @Test
    fun transformPoseTo_returnsCorrectPose() {
        val activitySpaceMatrix =
            fromTrs(Vector3(4.0f, 5.0f, 6.0f), fromEulerAngles(Vector3(90f, 0f, 0f)), Vector3.One)
        whenever(activitySpace.poseInOpenXrReferenceSpace).thenReturn(activitySpaceMatrix.toPose())
        val entity = mock<BaseEntity>()
        whenever(entity.activitySpacePose).thenReturn(Pose.Identity)
        whenever(entity.activitySpaceScale).thenReturn(Vector3.One)

        val transformedPose = perceptionSpaceScenePose.transformPoseTo(Pose(), entity)

        val expectedPose = activitySpaceMatrix.inverse.toPose()

        assertPose(transformedPose, expectedPose)
    }

    @Test
    fun transformPoseTo_toScaledEntity_returnsCorrectPose() {
        val activitySpaceMatrix =
            fromTrs(Vector3(4.0f, 5.0f, 6.0f), fromEulerAngles(Vector3(90f, 0f, 0f)), Vector3.One)
        whenever(activitySpace.poseInOpenXrReferenceSpace).thenReturn(activitySpaceMatrix.toPose())

        val entity = mock<BaseEntity>()
        whenever(entity.activitySpacePose).thenReturn(Pose.Identity)
        whenever(entity.activitySpaceScale).thenReturn(Vector3(2.0f, 2.0f, 2.0f))

        val transformedPose = perceptionSpaceScenePose.transformPoseTo(Pose(), entity)

        val unscaledPose = activitySpaceMatrix.inverse.toPose()
        val expectedPose =
            Pose(unscaledPose.translation.scale(Vector3(0.5f, 0.5f, 0.5f)), unscaledPose.rotation)

        assertPose(transformedPose, expectedPose)
    }

    @Test
    fun getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() {
        val activitySpaceScale = 5f
        whenever(activitySpace.worldSpaceScale).thenReturn(Vector3.One * activitySpaceScale)

        assertVector3(perceptionSpaceScenePose.activitySpaceScale, Vector3.One / activitySpaceScale)
    }
}
