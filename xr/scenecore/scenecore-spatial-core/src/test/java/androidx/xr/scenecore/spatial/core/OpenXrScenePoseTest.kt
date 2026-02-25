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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.HitTestResult
import androidx.xr.scenecore.runtime.ScenePose
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import androidx.xr.scenecore.runtime.impl.BaseScenePose
import androidx.xr.scenecore.testing.FakeGltfFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.Vec3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.Robolectric

/** Test for common behavior for ScenePoses whose world position is retrieved from OpenXr. */
@RunWith(ParameterizedRobolectricTestRunner::class)
class OpenXrScenePoseTest(private val testScenePoseType: OpenXrScenePoseType) {
    private val xrExtensions: XrExtensions? = XrExtensionsProvider.getXrExtensions()
    private val executor = FakeScheduledExecutorService()
    private val entityManager = EntityManager()
    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().start().get()

    private val activitySpace =
        ActivitySpaceImpl(
            xrExtensions!!.createNode(),
            activity,
            xrExtensions,
            entityManager,
            { xrExtensions.getSpatialState(activity) },
            executor,
        )

    private val mockGltfFeature: GltfFeature = mock(GltfFeature::class.java)
    private var testScenePose: BaseScenePose? = null

    enum class OpenXrScenePoseType {
        PERCEPTION_POSE_ACTIVITY_POSE
    }

    companion object {
        /** Creates and return list of OpenXrScenePoseType values. */
        @JvmStatic
        @Parameters
        fun data(): List<Any> {
            return listOf(OpenXrScenePoseType.PERCEPTION_POSE_ACTIVITY_POSE)
        }
    }

    /** Creates an OpenXrActivityPose instance. */
    private fun createOpenXrScenePose(
        activitySpace: ActivitySpaceImpl,
        pose: Pose,
    ): OpenXrScenePose {
        return OpenXrScenePose(activitySpace, pose)
    }

    private fun createTestScenePose(pose: Pose): BaseScenePose {
        when (testScenePoseType) {
            OpenXrScenePoseType.PERCEPTION_POSE_ACTIVITY_POSE ->
                return createOpenXrScenePose(activitySpace, pose)
        }
    }

    /** Creates a generic glTF entity. */
    private fun createGltfEntity(): GltfEntityImpl {
        val nodeHolder = NodeHolder(xrExtensions!!.createNode(), Node::class.java)
        val fakeGltfFeature = FakeGltfFeature.createWithMockFeature(mockGltfFeature, nodeHolder)

        return GltfEntityImpl(
            activity,
            fakeGltfFeature,
            activitySpace,
            xrExtensions,
            entityManager,
            executor,
        )
    }

    @Test
    fun getPoseInActivitySpace_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)

        assertNotNull(testScenePose)
        assertPose(testScenePose!!.poseInActivitySpace, Pose())
    }

    @Test
    fun getPoseInActivitySpace_whenAtSamePose_returnsIdentityPose() {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        assertPose(testScenePose!!.poseInActivitySpace, Pose())
    }

    @Test
    fun getPoseInActivitySpace_returnsDifferencePose() {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)

        assertPose(testScenePose!!.poseInActivitySpace, pose)
    }

    @Test
    fun getPoseInActivitySpace_witRotatedPerceptionPose_returnsDifferencePose() {
        val perceptionQuaternion = Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f))
        val pose = Pose(Vector3(0f, 0f, 0f), perceptionQuaternion)
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(
                Vector3(0f, 0f, 0f),
                Quaternion.Identity,
                /* scale= */ Vector3(1f, 1f, 1f),
            )
        )

        // If the activitySpace has an identity rotation, then there shouldn't be any change
        val expectedPose = Pose(Vector3(0f, 0f, 0f), perceptionQuaternion)

        assertPose(testScenePose!!.poseInActivitySpace, expectedPose)
    }

    @Test
    fun getPoseInActivitySpace_witRotatedActivitySpace_returnsDifferencePose() {
        val activitySpaceQuaternion = Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f))
        val pose = Pose(Vector3(0f, 0f, 0f), Quaternion.Identity)
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(
                Vector3(0f, 0f, 0f),
                activitySpaceQuaternion,
                /* scale= */ Vector3(1f, 1f, 1f),
            )
        )
        // If perception pose is identity, then rotation should be the inverse of the activity
        // space.
        val expectedPose =
            Pose(Vector3(0f, 0f, 0f), Quaternion.fromEulerAngles(Vector3(0f, 0f, -90f)))

        assertPose(testScenePose!!.poseInActivitySpace, expectedPose)
    }

    // TODO: Add tests with children of these entities

    @Test
    fun getActivitySpacePose_returnsDifferencePose() {
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        testScenePose = createTestScenePose(pose)

        assertNotNull(testScenePose)
        assertPose(testScenePose!!.activitySpacePose, pose)
    }

    @Test
    fun transformPoseTo_withActivitySpace_returnsTransformedPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)

        val userHeadSpaceOffset =
            Pose(Vector3(10f, 0f, 0f), Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f)))
        val transformedPose = testScenePose!!.transformPoseTo(userHeadSpaceOffset, activitySpace)

        assertPose(
            transformedPose,
            Pose(Vector3(11f, 2f, 3f), Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f))),
        )
    }

    @Test
    fun transformPoseTo_fromActivitySpaceChild_returnsUserHeadSpacePose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)
        testScenePose = createTestScenePose(pose)
        val childEntity1 = createGltfEntity()
        val childPose = Pose(Vector3(-1f, -2f, -3f), Quaternion.Identity)

        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        activitySpace.addChild(childEntity1)
        childEntity1.setPose(childPose)

        assertPose(
            activitySpace.transformPoseTo(Pose(), testScenePose!!),
            Pose(Vector3(-1f, -2f, -3f), Quaternion.Identity),
        )

        val transformedPose = childEntity1.transformPoseTo(Pose(), testScenePose!!)

        assertPose(transformedPose, Pose(Vector3(-2f, -4f, -6f), Quaternion.Identity))
    }

    @Test
    fun hitTest_returnsTransformedHitTest() = runBlocking {
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion.fromEulerAngles(Vector3(90f, 0f, 0f)))
        testScenePose = createTestScenePose(pose)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        val distance = 2.0f
        val hitPosition = Vec3(1.0f, 2.0f, 3.0f)
        val surfaceNormal = Vec3(0.0f, 1.0f, 0.0f)
        val surfaceType = com.android.extensions.xr.space.HitTestResult.SURFACE_PANEL
        val extensionsHitTestResult =
            com.android.extensions.xr.space.HitTestResult.Builder(
                    distance,
                    hitPosition,
                    true,
                    surfaceType,
                )
                .setSurfaceNormal(surfaceNormal)
                .build()
        com.android.extensions.xr.ShadowXrExtensions.extract(xrExtensions)
            .setHitTestResult(activity, extensionsHitTestResult)

        val deferredHitTestResult =
            async(start = CoroutineStart.UNDISPATCHED) {
                testScenePose!!.hitTest(
                    Vector3(1f, 1f, 1f),
                    Vector3(1f, 1f, 1f),
                    ScenePose.HitTestFilter.SELF_SCENE,
                )
            }
        executor.runAll()
        val hitTestResult = deferredHitTestResult.await()

        assertThat(hitTestResult).isNotNull()
        assertThat(hitTestResult.distance).isEqualTo(distance)
        // Since the entity is rotated 90 degrees about the x-axis, the hit position should be
        // rotated 90 degrees about the x-axis.
        assertVector3(hitTestResult.hitPosition!!, Vector3(0f, 2f, -1f))
        assertVector3(hitTestResult.surfaceNormal!!, Vector3(0f, 0f, -1f))
        assertThat(hitTestResult.surfaceType)
            .isEqualTo(HitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE)
    }
}
