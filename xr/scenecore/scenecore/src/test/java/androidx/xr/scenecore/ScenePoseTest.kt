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

package androidx.xr.scenecore

import android.app.Activity
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.ScenePose.HitTestFilter
import androidx.xr.scenecore.runtime.HitTestResult as RtHitTestResult
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakePerceptionSpaceScenePose
import androidx.xr.scenecore.testing.FakeScenePose
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ScenePoseTest {
    private val entityManager = EntityManager()
    private lateinit var activitySpace: ActivitySpace
    private lateinit var perceptionSpace: PerceptionSpace

    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private lateinit var fakeRuntime: SceneRuntime

    @Before
    fun setUp() {
        val fakeRuntimeFactory = FakeSceneRuntimeFactory()
        fakeRuntime = fakeRuntimeFactory.create(activity)

        activitySpace = ActivitySpace.create(fakeRuntime, entityManager)
        perceptionSpace = PerceptionSpace.create(fakeRuntime)
    }

    @Test
    fun allScenePoseTransformPoseTo_callsRuntimeScenePoseImplTransformPoseTo() {
        val pose = Pose.Identity

        assertThat(perceptionSpace.transformPoseTo(pose, perceptionSpace)).isEqualTo(pose)
    }

    @Test
    fun allScenePoseTransformPoseToEntity_callsRuntimeScenePoseImplTransformPoseTo() {
        val pose = Pose.Identity

        assertThat(perceptionSpace.transformPoseTo(pose, activitySpace)).isEqualTo(pose)
    }

    @Test
    fun allScenePoseTransformPoseFromEntity_callsRuntimeScenePoseImplTransformPoseTo() {
        val pose = Pose.Identity

        assertThat(activitySpace.transformPoseTo(pose, perceptionSpace)).isEqualTo(pose)
    }

    @Test
    fun transformPositionTo_sameScenePose_unchangedOutput() {
        val position = Vector3(1f, 2f, 3f)

        assertThat(activitySpace.transformPositionTo(position, activitySpace)).isEqualTo(position)
    }

    @Test
    fun transformPositionTo_differentScenePose_returnsTranslationOfPoseTransformation() {
        val position = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        sourceScenePose.activitySpaceScale = Vector3(7f, 8f, 9f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        targetScenePose.activitySpaceScale = Vector3(13f, 14f, 15f)

        // Should return the same position as transformPoseTo
        val expectedResult =
            sourceScenePose
                .transformPoseTo(Pose(position, Quaternion.Identity), targetScenePose)
                .translation
        assertVector3(
            sourceScenePose.transformPositionTo(position, targetScenePose),
            expectedResult,
        )
    }

    @Test
    fun transformPositionTo_simpleSpaceTranslation_returnsTranslatedPosition() {
        val localPositionOffsetInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose = Pose(Vector3(4f, 5f, 6f))

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose = Pose(Vector3(10f, 11f, 12f))
        // (source - destination) + localPositionOffsetInSource = localPositionInDestination
        val expected = Vector3(-5f, -4f, -3f)
        assertVector3(
            sourceScenePose.transformPositionTo(localPositionOffsetInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformPositionTo_simpleSpaceRotation_returnsRotatedPosition() {
        val localPositionOffsetInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(rotation = Quaternion.fromEulerAngles(180f, 0f, 0f))
        // 180 degree rotation along the x-axis is a clockwise rotation of the y-z plane.
        val expected = Vector3(1f, -2f, -3f)
        assertVector3(
            sourceScenePose.transformPositionTo(localPositionOffsetInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformPositionTo_simpleSpaceScale_returnsScaledPosition() {
        val localPositionOffsetInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpaceScale = Vector3(4f, 4f, 4f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpaceScale = Vector3(2f, 2f, 2f)
        // sourcePosition * sourceScale / targetScale = sourcePosition * (2f / 4f) = targetPosition
        val expected = Vector3(2f, 4f, 6f)
        assertVector3(
            sourceScenePose.transformPositionTo(localPositionOffsetInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformVectorTo_sameScenePose_unchangedOutput() {
        val vector = Vector3(1f, 2f, 3f)

        assertVector3(activitySpace.transformVectorTo(vector, activitySpace), vector)
    }

    @Test
    fun transformVectorTo_differentScenePose_returnsUntranslatedPositionTransformation() {
        val vector = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        sourceScenePose.activitySpaceScale = Vector3(7f, 8f, 9f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        targetScenePose.activitySpaceScale = Vector3(13f, 14f, 15f)

        // Should return the vector from the transformed origin point to the end of the vector point
        val expectedResult =
            sourceScenePose.transformPositionTo(vector, targetScenePose) -
                sourceScenePose.transformPositionTo(Vector3.Zero, targetScenePose)
        assertVector3(sourceScenePose.transformVectorTo(vector, targetScenePose), expectedResult)
    }

    @Test
    fun transformVectorTo_zeroVector_returnsUntranslatedPositionTransformation() {
        val vector = Vector3.Zero
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        sourceScenePose.activitySpaceScale = Vector3(7f, 8f, 9f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        targetScenePose.activitySpaceScale = Vector3(13f, 14f, 15f)

        val expectedResult = Vector3.Zero
        assertVector3(sourceScenePose.transformVectorTo(vector, targetScenePose), expectedResult)
    }

    @Test
    fun transformVectorTo_simpleSpaceTranslation_returnsSameVector() {
        val localVectorInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose = Pose(Vector3(4f, 5f, 6f))

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose = Pose(Vector3(10f, 11f, 12f))
        // Space origin translations should be ignored when transforming vectors.
        val expected = localVectorInSource
        assertVector3(
            sourceScenePose.transformVectorTo(localVectorInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformVectorTo_simpleSpaceRotation_returnsRotatedVector() {
        val localVectorInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(rotation = Quaternion.fromEulerAngles(180f, 0f, 0f))
        // 180 degree rotation along the x-axis is a clockwise rotation of the y-z plane.
        val expected = Vector3(1f, -2f, -3f)
        assertVector3(
            sourceScenePose.transformVectorTo(localVectorInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformVectorTo_simpleSpaceScale_returnsScaledVector() {
        val localVectorInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpaceScale = Vector3(4f, 4f, 4f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpaceScale = Vector3(4f, 2f, 1f)
        // sourcePosition * sourceScale / targetScale = (1, 2, 3) * (4/4, 4/2, 4/1) = targetPosition
        val expected = Vector3(1f, 4f, 12f)
        assertVector3(
            sourceScenePose.transformVectorTo(localVectorInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformDirectionTo_sameScenePose_unchangedOutput() {
        val direction = Vector3(1f, 2f, 3f)

        assertVector3(activitySpace.transformDirectionTo(direction, activitySpace), direction)
    }

    @Test
    fun transformDirectionTo_differentScenePose_returnsUnscaledVectorTransformation() {
        val direction = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        sourceScenePose.activitySpaceScale = Vector3(7f, 8f, 9f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        targetScenePose.activitySpaceScale = Vector3(13f, 14f, 15f)

        // Should return the same as a vector transformation, but with the magnitude unchanged
        val expectedResult =
            sourceScenePose.transformVectorTo(direction, targetScenePose).toNormalized() *
                direction.length
        assertVector3(
            sourceScenePose.transformDirectionTo(direction, targetScenePose),
            expectedResult,
        )
    }

    @Test
    fun transformDirectionTo_zeroDirection_returnsUntranslatedPositionTransformation() {
        val direction = Vector3.Zero
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose =
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        sourceScenePose.activitySpaceScale = Vector3(7f, 8f, 9f)

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        targetScenePose.activitySpaceScale = Vector3(13f, 14f, 15f)

        val expectedResult = Vector3.Zero
        assertVector3(
            sourceScenePose.transformDirectionTo(direction, targetScenePose),
            expectedResult,
        )
    }

    @Test
    fun transformDirectionTo_simpleSpaceTranslation_returnsSameDirection() {
        val localDirectionInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpacePose = Pose(Vector3(4f, 5f, 6f))

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose = Pose(Vector3(10f, 11f, 12f))
        // Space origin translations should be ignored when transforming directions.
        val expected = localDirectionInSource
        assertVector3(
            sourceScenePose.transformDirectionTo(localDirectionInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformDirectionTo_simpleSpaceRotation_returnsRotatedDirection() {
        val localDirectionInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpacePose =
            Pose(rotation = Quaternion.fromEulerAngles(180f, 0f, 0f))
        // 180 degree rotation along the x-axis is a clockwise rotation of the y-z plane.
        val expected = Vector3(1f, -2f, -3f)
        assertVector3(
            sourceScenePose.transformDirectionTo(localDirectionInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun transformDirectionTo_simpleSpaceScaleTranslate_returnsSameDirection() {
        val localDirectionInSource = Vector3(1f, 2f, 3f)
        val sourceScenePose = FakeScenePose()
        sourceScenePose.activitySpaceScale = Vector3(2f, 2f, 2f)
        sourceScenePose.activitySpacePose = Pose(Vector3(4f, 5f, 6f))

        val targetScenePose = FakeScenePose()
        targetScenePose.activitySpaceScale = Vector3(3f, 3f, 3f)
        targetScenePose.activitySpacePose = Pose(Vector3(10f, 11f, 12f))

        // Uniform coordinate space scaling should be ignored when transforming directions
        val expected = localDirectionInSource
        assertVector3(
            sourceScenePose.transformDirectionTo(localDirectionInSource, targetScenePose),
            expected,
        )
    }

    @Test
    fun poseInActivitySpace_defaultsToIdentity() {
        val pose = Pose.Identity

        assertThat(perceptionSpace.poseInActivitySpace).isEqualTo(pose)
    }

    @Test
    fun hitTest_callsRuntimeHitTest() {
        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val hitTestFilter = HitTestFilter.SELF_SCENE
        val hitPosition = Vector3(7f, 8f, 9f)
        val surfaceNormal = Vector3(10f, 11f, 12f)
        val distance = 7f
        val surfaceType = RtHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE
        val expectedHitTestResult =
            HitTestResult(hitPosition, surfaceNormal, surfaceType.toHitTestSurfaceType(), distance)

        // Set the hit test results.
        val rtHitTestResult = RtHitTestResult(hitPosition, surfaceNormal, surfaceType, distance)
        (fakeRuntime.perceptionSpaceActivityPose as FakePerceptionSpaceScenePose).hitTestResult =
            rtHitTestResult

        runBlocking {
            assertThat(perceptionSpace.hitTest(origin, direction, hitTestFilter))
                .isEqualTo(expectedHitTestResult)
        }
    }

    @Test
    fun hitTest_withDefaultHitTestFilter_callsRuntimeHitTest() {
        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val hitPosition = Vector3(7f, 8f, 9f)
        val surfaceNormal = Vector3(10f, 11f, 12f)
        val distance = 7f
        val surfaceType = RtHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE
        val expectedHitTestResult =
            HitTestResult(hitPosition, surfaceNormal, surfaceType.toHitTestSurfaceType(), distance)

        // Set the hit test results.
        val rtHitTestResult = RtHitTestResult(hitPosition, surfaceNormal, surfaceType, distance)
        (fakeRuntime.perceptionSpaceActivityPose as FakePerceptionSpaceScenePose).hitTestResult =
            rtHitTestResult

        runBlocking {
            assertThat(perceptionSpace.hitTest(origin, direction)).isEqualTo(expectedHitTestResult)
        }
    }

    @Test
    fun hitTest_convertsNullResult() {
        val origin = Vector3(1f, 2f, 3f)
        val direction = Vector3(4f, 5f, 6f)
        val hitTestFilter = HitTestFilter.SELF_SCENE
        // a null hitPosition in RtHitTestResult should result in a null (public API) HitTestResult
        val hitPosition = null
        val surfaceNormal = null
        val distance = 0f
        val surfaceType = RtHitTestResult.HitTestSurfaceType.HIT_TEST_RESULT_SURFACE_TYPE_PLANE

        // Set the hit test results.
        val rtHitTestResult = RtHitTestResult(hitPosition, surfaceNormal, surfaceType, distance)
        (fakeRuntime.perceptionSpaceActivityPose as FakePerceptionSpaceScenePose).hitTestResult =
            rtHitTestResult

        runBlocking {
            assertThat(perceptionSpace.hitTest(origin, direction, hitTestFilter)).isNull()
        }
    }
}
