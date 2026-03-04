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

package androidx.xr.arcore

import android.content.ContentResolver
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimeAnchor
import androidx.xr.arcore.testing.FakeRuntimeFace
import androidx.xr.runtime.Config
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class FaceTest {
    private lateinit var runtime: FakePerceptionRuntime
    private lateinit var xrResourcesManager: XrResourcesManager
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @get:Rule
    val grantPermissionRule = GrantPermissionRule.grant("android.permission.FACE_TRACKING")

    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)

        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        xrResourcesManager = XrResourcesManager()
        mockContentResolver = activity.contentResolver

        val shadowApplication = shadowOf(activity.application)
        shadowApplication.grantPermissions("android.permission.FACE_TRACKING")
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }

        FakePerceptionRuntimeFactory.hasCreatePermission = true

        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        runtime = session.runtimes.first() as FakePerceptionRuntime
        xrResourcesManager.lifecycleManager = session.perceptionRuntime.lifecycleManager

        FakeRuntimeAnchor.anchorsCreatedCount = 0
    }

    @After
    fun tearDown() {
        xrResourcesManager.clear()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getUserFace_returnsFaceWithUpdatedTrackingStateAndBlendShapes() {
        runTest(testDispatcher) {
            session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val userFace = Face.getUserFace(session)
            val runtimeFace = perceptionManager.userFace!! as FakeRuntimeFace
            val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
            val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
            check(userFace != null)
            check(userFace.state.value.trackingState != TrackingState.TRACKING)
            check(!userFace.state.value.blendShapeValues.contentEquals(expectedBlendShapeValues))
            check(!userFace.state.value.confidenceValues.contentEquals(expectedConfidenceValues))
            runtimeFace.trackingState = TrackingState.TRACKING
            runtimeFace.blendShapeValues = expectedBlendShapeValues
            runtimeFace.confidenceValues = expectedConfidenceValues

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(userFace.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
            assertThat(userFace.state.value.blendShapeValues).isEqualTo(expectedBlendShapeValues)
            assertThat(userFace.state.value.confidenceValues).isEqualTo(expectedConfidenceValues)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun collect_collectReturnsFaceMeshes() =
        runTest(testDispatcher) {
            session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
            val perceptionManager = runtime.perceptionManager
            val runtimeFace = FakeRuntimeFace()
            perceptionManager.addTrackable(runtimeFace)
            activityController.resume()
            advanceUntilIdle()
            activityController.pause()
            var underTest = emptyList<Face>()

            val job =
                backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    underTest = Face.subscribe(session).first().toList()
                }
            advanceUntilIdle()

            assertThat(underTest.size).isEqualTo(1)
            assertThat(underTest.first().runtimeFace).isEqualTo(runtimeFace)
            job.cancel()
        }

    @Test
    fun getUserFace_faceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(faceTracking = FaceTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Face.getUserFace(session) }
    }

    @Test
    fun getUserFace_stateMatchesRuntimeFace() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        val runtimeFace = FakeRuntimeFace()
        val underTest = Face(runtimeFace, xrResourcesManager)
        val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
        val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
        check(underTest.state.value.trackingState != TrackingState.TRACKING)
        check(!underTest.state.value.blendShapeValues.contentEquals(expectedBlendShapeValues))
        check(!underTest.state.value.confidenceValues.contentEquals(expectedConfidenceValues))
        runtimeFace.trackingState = TrackingState.TRACKING
        runtimeFace.blendShapeValues = expectedBlendShapeValues
        runtimeFace.confidenceValues = expectedConfidenceValues

        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(underTest.state.value.blendShapeValues).isEqualTo(expectedBlendShapeValues)
        assertThat(underTest.state.value.confidenceValues).isEqualTo(expectedConfidenceValues)
        assertThat(underTest.state.value.blendShapes.keys.size)
            .isEqualTo(expectedBlendShapeValues.size)
        assertThat(underTest.state.value.blendShapes.values.size)
            .isEqualTo(expectedBlendShapeValues.size)
    }

    @Test
    fun update_trackingStateMatchesRuntime() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        val runtimeFace = FakeRuntimeFace()
        xrResourcesManager.syncTrackables(listOf(runtimeFace))
        val underTest = xrResourcesManager.trackablesMap[runtimeFace] as Face
        check(underTest.state.value.trackingState == runtimeFace.trackingState)

        runtimeFace.trackingState = TrackingState.TRACKING
        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
    }

    @Test
    fun update_centerPoseMatchesRuntime() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        val runtimeFace = FakeRuntimeFace()
        xrResourcesManager.syncTrackables(listOf(runtimeFace))
        val underTest = xrResourcesManager.trackablesMap[runtimeFace] as Face
        runtimeFace.centerPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))

        underTest.update()

        assertThat(underTest.state.value.centerPose).isEqualTo(runtimeFace.centerPose)
    }

    @Test
    fun update_noseTipPoseMatchesRuntime() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        val runtimeFace = FakeRuntimeFace()
        xrResourcesManager.syncTrackables(listOf(runtimeFace))
        val underTest = xrResourcesManager.trackablesMap[runtimeFace] as Face

        val newPose = Pose(Vector3(9f, 9f, 9f), Quaternion(9f, 9f, 9f, 1f))
        runtimeFace.noseTipPose = newPose
        underTest.update()

        assertThat(underTest.state.value.noseTipPose).isEqualTo(newPose)
    }

    @Test
    fun update_foreheadLeftPoseMatchesRuntime() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        val runtimeFace = FakeRuntimeFace()
        xrResourcesManager.syncTrackables(listOf(runtimeFace))
        val underTest = xrResourcesManager.trackablesMap[runtimeFace] as Face

        val newPose = Pose(Vector3(9f, 9f, 9f), Quaternion(9f, 9f, 9f, 1f))
        runtimeFace.foreheadLeftPose = newPose
        underTest.update()

        assertThat(underTest.state.value.foreheadLeftPose).isEqualTo(newPose)
    }

    @Test
    fun update_foreheadRightPoseMatchesRuntime() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        val runtimeFace = FakeRuntimeFace()
        xrResourcesManager.syncTrackables(listOf(runtimeFace))
        val underTest = xrResourcesManager.trackablesMap[runtimeFace] as Face

        val newPose = Pose(Vector3(9f, 9f, 9f), Quaternion(9f, 9f, 9f, 1f))
        runtimeFace.foreheadRightPose = newPose
        underTest.update()

        assertThat(underTest.state.value.foreheadRightPose).isEqualTo(newPose)
    }

    @Test
    fun update_mesh_matchesRuntime() = runBlocking {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        val runtimeFace = FakeRuntimeFace()
        xrResourcesManager.syncTrackables(listOf(runtimeFace))
        val underTest = xrResourcesManager.trackablesMap[runtimeFace] as Face
        runtimeFace.mesh =
            Mesh(
                ShortBuffer.allocate(1).put(11),
                FloatBuffer.allocate(1).put(12f),
                FloatBuffer.allocate(1).put(13f),
                FloatBuffer.allocate(1).put(14f),
            )

        underTest.update()

        assertThat(underTest.state.value.mesh?.triangleIndices)
            .isEqualTo(runtimeFace.mesh.triangleIndices)
    }
}
