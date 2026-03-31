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

package androidx.xr.scenecore.runtime.impl

import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.scenecore.runtime.ActivitySpace
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.testing.FakeActivitySpace
import androidx.xr.scenecore.testing.FakeEntity
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.ParameterizedRobolectricTestRunner

/** Test for common behavior for ScenePoses whose world position is retrieved from OpenXr. */
@RunWith(ParameterizedRobolectricTestRunner::class)
class OpenXrScenePoseTest(private val testScenePoseType: OpenXrScenePoseType) {

    private val executor = FakeScheduledExecutorService()
    private val activitySpace = FakeActivitySpace()

    private var testScenePose: BaseScenePose? = null

    enum class OpenXrScenePoseType {
        PERCEPTION_POSE_ACTIVITY_POSE
    }

    companion object {
        /** Creates and return list of OpenXrScenePoseType values. */
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data(): List<Any> {
            return listOf(OpenXrScenePoseType.PERCEPTION_POSE_ACTIVITY_POSE)
        }
    }

    /** Creates an OpenXrActivityPose instance. */
    private fun createOpenXrScenePose(activitySpace: ActivitySpace, pose: Pose): OpenXrScenePose {
        return OpenXrScenePose(activitySpace, pose)
    }

    private fun createTestScenePose(
        pose: Pose,
        testActivitySpace: ActivitySpace = this.activitySpace,
    ): BaseScenePose {
        when (testScenePoseType) {
            OpenXrScenePoseType.PERCEPTION_POSE_ACTIVITY_POSE ->
                return createOpenXrScenePose(testActivitySpace, pose)
        }
    }

    /** Creates a generic entity. */
    private fun createEntity(): Entity {
        return FakeEntity()
    }

    @Test
    fun getActivitySpacePose_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)

        Assert.assertNotNull(testScenePose)
        assertPose(testScenePose!!.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Companion.fromPose(pose))

        assertPose(testScenePose!!.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpacePose_returnsDifferencePose() {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Companion.Identity)

        Assert.assertNotNull(testScenePose)
        assertPose(testScenePose!!.activitySpacePose, pose)
    }

    @Test
    fun getActivitySpacePose_withRotatedPerceptionPose_returnsDifferencePose() {
        val perceptionQuaternion = Quaternion.Companion.fromEulerAngles(Vector3(0f, 0f, 90f))
        val pose = Pose(Vector3(0f, 0f, 0f), perceptionQuaternion)
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.Companion.fromTrs(
                Vector3(0f, 0f, 0f),
                Quaternion.Companion.Identity,
                /* scale= */ Vector3(1f, 1f, 1f),
            )
        )

        // If the activitySpace has an identity rotation, then there shouldn't be any change
        val expectedPose = Pose(Vector3(0f, 0f, 0f), perceptionQuaternion)

        assertPose(testScenePose!!.activitySpacePose, expectedPose)
    }

    @Test
    fun getActivitySpacePose_withRotatedActivitySpace_returnsDifferencePose() {
        val activitySpaceQuaternion = Quaternion.Companion.fromEulerAngles(Vector3(0f, 0f, 90f))
        val pose = Pose(Vector3(0f, 0f, 0f), Quaternion.Companion.Identity)
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.Companion.fromTrs(
                Vector3(0f, 0f, 0f),
                activitySpaceQuaternion,
                /* scale= */ Vector3(1f, 1f, 1f),
            )
        )
        // If perception pose is identity, then rotation should be the inverse of the activity
        // space.
        val expectedPose =
            Pose(Vector3(0f, 0f, 0f), Quaternion.Companion.fromEulerAngles(Vector3(0f, 0f, -90f)))

        assertPose(testScenePose!!.activitySpacePose, expectedPose)
    }

    // TODO: Add tests with children of these entities

    @Test
    fun transformPoseTo_withActivitySpace_returnsTransformedPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Companion.Identity)
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Companion.Identity)

        val userHeadSpaceOffset =
            Pose(Vector3(10f, 0f, 0f), Quaternion.Companion.fromEulerAngles(Vector3(0f, 0f, 90f)))
        val transformedPose = testScenePose!!.transformPoseTo(userHeadSpaceOffset, activitySpace)

        assertPose(
            transformedPose,
            Pose(Vector3(11f, 2f, 3f), Quaternion.Companion.fromEulerAngles(Vector3(0f, 0f, 90f))),
        )
    }

    @Test
    fun hitTest_callsActivitySpaceHitTestRelativeToActivityPose() = runBlocking {
        val pose =
            Pose(Vector3(1f, 1f, 1f), Quaternion.Companion.fromEulerAngles(Vector3(90f, 0f, 0f)))
        val testActivitySpace = mock<ActivitySpace>()
        val testScenePose = createTestScenePose(pose, testActivitySpace = testActivitySpace)
        val origin = Vector3(1.0f, 2.0f, 3.0f)
        val direction = Vector3(0.0f, 1.0f, 0.0f)
        val filter = ScenePose.HitTestFilter.SELF_SCENE

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                testScenePose.hitTest(origin, direction, filter)
            }
        executor.runAll()

        verify(testActivitySpace)
            .hitTestRelativeToActivityPose(origin, direction, filter, testScenePose)
        assert(true) // Test doesn't run without an assert at the end.
    }
}
