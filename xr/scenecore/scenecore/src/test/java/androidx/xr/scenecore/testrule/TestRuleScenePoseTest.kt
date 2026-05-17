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

package androidx.xr.scenecore.testrule

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.HitTestResult
import androidx.xr.scenecore.PerceptionSpace
import androidx.xr.scenecore.ScenePose.HitTestFilter
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testing.EntityTester
import androidx.xr.scenecore.testing.PerceptionSpaceTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class TestRuleScenePoseTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var perceptionSpace: PerceptionSpace
    private lateinit var perceptionSpaceTester: PerceptionSpaceTester

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        perceptionSpace = session.scene.perceptionSpace
        activitySpace = session.scene.activitySpace

        perceptionSpaceTester = testRule.perceptionSpaceTester
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun transformPoseTo_sameSpace_returnsUnchangedPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.fromEulerAngles(15f, 30f, 45f))

        assertThat(perceptionSpace.transformPoseTo(pose, perceptionSpace)).isEqualTo(pose)
    }

    @Test
    fun transformPoseTo_fromPerceptionToActivitySpace_returnsExpectedPose() {
        val pose = Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))

        assertThat(perceptionSpace.transformPoseTo(pose, activitySpace)).isEqualTo(pose)
    }

    @Test
    fun transformPoseTo_fromActivityToPerceptionSpace_returnsExpectedPose() {
        val pose = Pose(Vector3(7f, 8f, 9f), Quaternion.fromEulerAngles(90f, 180f, 270f))

        assertThat(activitySpace.transformPoseTo(pose, perceptionSpace)).isEqualTo(pose)
    }

    @Test
    fun transformPositionTo_sameScenePose_unchangedOutput() {
        val position = Vector3(1f, 2f, 3f)

        assertVector3(activitySpace.transformPositionTo(position, activitySpace), position)
    }

    @Test
    fun transformPositionTo_differentScenePose_returnsTranslationOfPoseTransformation() {
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        )

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        )

        // Should return the same position as transformPoseTo
        val position = Vector3(1f, 2f, 3f)
        val expectedResult =
            source.transformPoseTo(Pose(position, Quaternion.Identity), target).translation
        assertVector3(source.transformPositionTo(position, target), expectedResult)
    }

    @Test
    fun transformPositionTo_simpleSpaceTranslation_returnsTranslatedPosition() {
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(Pose(Vector3(4f, 5f, 6f)))

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(Vector3(10f, 11f, 12f)))

        // (source - destination) + localPositionOffsetInSource = localPositionInDestination
        val localPositionOffsetInSource = Vector3(1f, 2f, 3f)
        val expected = Vector3(-5f, -4f, -3f)
        assertVector3(source.transformPositionTo(localPositionOffsetInSource, target), expected)
    }

    @Test
    fun transformPositionTo_simpleSpaceRotation_returnsRotatedPosition() {
        val source = Entity.create(session)
        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(rotation = Quaternion.fromEulerAngles(180f, 0f, 0f)))

        // 180-degree rotation along the x-axis is a clockwise rotation of the y-z plane.
        val localPositionOffsetInSource = Vector3(1f, 2f, 3f)
        val expected = Vector3(1f, -2f, -3f)
        assertVector3(source.transformPositionTo(localPositionOffsetInSource, target), expected)
    }

    @Test
    fun transformVectorTo_sameScenePose_unchangedOutput() {
        val vector = Vector3(1f, 2f, 3f)

        assertVector3(activitySpace.transformVectorTo(vector, activitySpace), vector)
    }

    @Test
    fun transformVectorTo_differentScenePose_returnsUntranslatedPositionTransformation() {
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        )

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        )

        // Should return the vector from the transformed origin point to the end of the vector point
        val vector = Vector3(1f, 2f, 3f)
        val expectedResult =
            source.transformPositionTo(vector, target) -
                source.transformPositionTo(Vector3.Zero, target)
        assertVector3(source.transformVectorTo(vector, target), expectedResult)
    }

    @Test
    fun transformVectorTo_zeroVector_returnsZeroVector() {
        val vector = Vector3.Zero
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        )

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        )

        val expectedResult = Vector3.Zero
        assertVector3(source.transformVectorTo(vector, target), expectedResult)
    }

    @Test
    fun transformVectorTo_simpleSpaceTranslation_returnsSameVector() {
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(Pose(Vector3(4f, 5f, 6f)))

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(Vector3(10f, 11f, 12f)))

        // Space origin translations should be ignored when transforming vectors.
        val localVectorInSource = Vector3(1f, 2f, 3f)
        assertVector3(source.transformVectorTo(localVectorInSource, target), localVectorInSource)
    }

    @Test
    fun transformVectorTo_simpleSpaceRotation_returnsRotatedVector() {
        val source = Entity.create(session)
        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(rotation = Quaternion.fromEulerAngles(180f, 0f, 0f)))

        // 180-degree rotation along the x-axis is a clockwise rotation of the y-z plane.
        val localVectorInSource = Vector3(1f, 2f, 3f)
        val expected = Vector3(1f, -2f, -3f)
        assertVector3(source.transformVectorTo(localVectorInSource, target), expected)
    }

    @Test
    fun transformDirectionTo_sameScenePose_unchangedOutput() {
        val direction = Vector3(1f, 2f, 3f)

        assertVector3(activitySpace.transformDirectionTo(direction, activitySpace), direction)
    }

    @Test
    fun transformDirectionTo_differentScenePose_returnsUnscaledVectorTransformation() {
        val source = Entity.create(session, parent = session.scene.activitySpace)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        )

        source.setScale(Vector3(2f, 2f, 2f), Space.ACTIVITY)

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        )

        // Should return the same as a vector transformation, but with the magnitude unchanged
        val direction = Vector3(1f, 2f, 3f)
        val expectedResult =
            source.transformVectorTo(direction, target).toNormalized() * direction.length
        assertVector3(source.transformDirectionTo(direction, target), expectedResult)
    }

    @Test
    fun transformDirectionTo_zeroDirection_returnsZeroDirection() {
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(
            Pose(Vector3(4f, 5f, 6f), Quaternion.fromEulerAngles(45f, 90f, 180f))
        )

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(
            Pose(Vector3(10f, 11f, 12f), Quaternion.fromEulerAngles(270f, 180f, -90f))
        )

        val direction = Vector3.Zero
        val expectedResult = Vector3.Zero
        assertVector3(source.transformDirectionTo(direction, target), expectedResult)
    }

    @Test
    fun transformDirectionTo_simpleSpaceTranslation_returnsSameDirection() {
        val source = Entity.create(session)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(Pose(Vector3(4f, 5f, 6f)))

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(Vector3(10f, 11f, 12f)))

        // Space origin translations should be ignored when transforming directions.
        val localDirectionInSource = Vector3(1f, 2f, 3f)
        assertVector3(
            source.transformDirectionTo(localDirectionInSource, target),
            localDirectionInSource,
        )
    }

    @Test
    fun transformDirectionTo_simpleSpaceRotation_returnsRotatedDirection() {
        val source = Entity.create(session)
        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(rotation = Quaternion.fromEulerAngles(180f, 0f, 0f)))

        // 180-degree rotation along the x-axis is a clockwise rotation of the y-z plane.
        val localDirectionInSource = Vector3(1f, 2f, 3f)
        val expected = Vector3(1f, -2f, -3f)
        assertVector3(source.transformDirectionTo(localDirectionInSource, target), expected)
    }

    @Test
    fun transformDirectionTo_simpleSpaceScaleTranslate_returnsSameDirection() {
        val source = Entity.create(session, parent = session.scene.activitySpace)
        val testSource = testRule.createTester<EntityTester>(source)
        testSource.setPoseInActivitySpace(Pose(Vector3(4f, 5f, 6f)))

        source.setScale(Vector3(2f, 2f, 2f), Space.ACTIVITY)

        val target = Entity.create(session)
        val testTarget = testRule.createTester<EntityTester>(target)
        testTarget.setPoseInActivitySpace(Pose(Vector3(10f, 11f, 12f)))

        // Uniform coordinate space scaling should be ignored when transforming directions
        val localDirectionInSource = Vector3(1f, 2f, 3f)
        assertVector3(
            source.transformDirectionTo(localDirectionInSource, target),
            localDirectionInSource,
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
        val surfaceType = HitTestResult.SurfaceType.PLANE
        val expectedHitTestResult = HitTestResult(hitPosition, surfaceNormal, surfaceType, distance)

        perceptionSpaceTester.hitTestResult = expectedHitTestResult

        runTest(testDispatcher) {
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
        val surfaceType = HitTestResult.SurfaceType.PLANE
        val expectedHitTestResult = HitTestResult(hitPosition, surfaceNormal, surfaceType, distance)

        perceptionSpaceTester.hitTestResult = expectedHitTestResult

        runTest(testDispatcher) {
            assertThat(perceptionSpace.hitTest(origin, direction)).isEqualTo(expectedHitTestResult)
        }
    }
}
