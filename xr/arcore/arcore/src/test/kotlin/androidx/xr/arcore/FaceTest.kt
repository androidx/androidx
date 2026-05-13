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

import android.Manifest.permission.CAMERA
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.Mesh
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestFace
import androidx.xr.runtime.Config
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.FACE_TRACKING
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FaceTest {
    companion object {
        const val BLEND_SHAPE_COUNT = 68
    }

    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)

        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        activityController.create().start().resume()

        shadowOf(activity.application).grantPermissions(FACE_TRACKING, CAMERA)

        session =
            (Session.create(context = activity, coroutineContext = testDispatcher)
                    as SessionCreateSuccess)
                .session
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getUserFace_returnsFaceWithUpdatedTrackingStateAndBlendShapes() {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        runTest(testDispatcher) {
            val underTest = Face.getUserFace(session)

            assertThat(underTest.state.value.blendShapeValues).isNotNull()
            assertThat(underTest.state.value.blendShapeValues!!.all { it == 0f }).isTrue()
            assertThat(underTest.state.value.confidenceValues).isNotNull()
            assertThat(underTest.state.value.confidenceValues!!.all { it == 1f }).isTrue()

            val expectedBlendShapes = MutableList(BLEND_SHAPE_COUNT) { 0.0f }
            expectedBlendShapes.forEachIndexed { i, _ ->
                expectedBlendShapes[i] = i / BLEND_SHAPE_COUNT.toFloat()
            }
            arCoreTestRule.faceTester.blendShapeValues = expectedBlendShapes.toList()
            advanceUntilIdle()

            assertThat(underTest.state.value.blendShapeValues)
                .isEqualTo(expectedBlendShapes.toFloatArray())
        }
    }

    @Test
    fun getUserFace_faceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(faceTracking = FaceTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Face.getUserFace(session) }
    }

    @Test
    fun getUserFace_faceTrackingConfiguredForMeshes_throwsIllegalStateException() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))

        assertFailsWith<IllegalStateException> { Face.getUserFace(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalFaceApi::class)
    @Test
    fun subscribe_collectReturnsFaceMesh() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: List<Face> = listOf()
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.toList() }
            }

            assertThat(underTest).isNotEmpty()
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun subscribe_faceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(faceTracking = FaceTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Face.subscribe(session) }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun subscribe_faceTrackingConfiguredForBlendShapes_throwsIllegalStateException() {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))

        assertFailsWith<IllegalStateException> { Face.subscribe(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getUserFace_stateMatchesRuntimeFace() {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        runTest(testDispatcher) {
            val underTest = Face.getUserFace(session)
            arCoreTestRule.faceTester.isValid = true
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.TRACKING)

            activityController.pause()
            advanceUntilIdle()
            session.configure(Config(faceTracking = FaceTrackingMode.DISABLED))
            advanceUntilIdle()
            activityController.resume()
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.STOPPED)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun blendShapeArray_invalidValues_doesNotUpdateState() {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        runTest(testDispatcher) {
            val underTest = Face.getUserFace(session)

            val expectedBlendShapes = MutableList(BLEND_SHAPE_COUNT) { 0.0f }
            expectedBlendShapes.forEachIndexed { i, _ ->
                expectedBlendShapes[i] = i / BLEND_SHAPE_COUNT.toFloat()
            }
            arCoreTestRule.faceTester.blendShapeValues = expectedBlendShapes
            advanceUntilIdle()

            assertThat(underTest.state.value.blendShapeValues)
                .isEqualTo(expectedBlendShapes.toFloatArray())

            var invalidBlendShapeValues = List(BLEND_SHAPE_COUNT) { 5f }
            arCoreTestRule.faceTester.blendShapeValues = invalidBlendShapeValues
            advanceUntilIdle()

            assertThat(underTest.state.value.blendShapeValues)
                .isEqualTo(expectedBlendShapes.toFloatArray())

            invalidBlendShapeValues = listOf()
            arCoreTestRule.faceTester.blendShapeValues = invalidBlendShapeValues
            advanceUntilIdle()

            assertThat(underTest.state.value.blendShapeValues)
                .isEqualTo(expectedBlendShapes.toFloatArray())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun confidenceArray_invalidValues_doesNotUpdateState() {
        session.configure(Config(faceTracking = FaceTrackingMode.BLEND_SHAPES))
        runTest(testDispatcher) {
            val underTest = Face.getUserFace(session)

            val expectedConfidences = listOf(0f, .3333f, .6666f)
            arCoreTestRule.faceTester.confidenceValues = expectedConfidences
            advanceUntilIdle()

            assertThat(underTest.state.value.confidenceValues)
                .isEqualTo(expectedConfidences.toFloatArray())

            var invalidConfidences = listOf(5f, 5f, 5f)
            arCoreTestRule.faceTester.confidenceValues = invalidConfidences
            advanceUntilIdle()

            assertThat(underTest.state.value.confidenceValues)
                .isEqualTo(expectedConfidences.toFloatArray())

            invalidConfidences = listOf()
            arCoreTestRule.faceTester.confidenceValues = invalidConfidences
            advanceUntilIdle()

            assertThat(underTest.state.value.confidenceValues)
                .isEqualTo(expectedConfidences.toFloatArray())
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun update_trackingStateMatchesRuntime() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: Face? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.first() }
            }
            check(underTest != null)

            assertThat(underTest.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.TRACKING)

            testFace.isVisible = false
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.PAUSED)
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun update_centerPoseMatchesRuntime() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: Face? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.first() }
            }

            val expectedPose = Pose(Vector3(.5f, .6f, .7f), Quaternion(.1f, .2f, .3f, .4f))
            testFace.centerPose = expectedPose
            advanceUntilIdle()

            check(underTest != null)
            assertThat(underTest.state.value.centerPose).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun update_noseTipPoseMatchesRuntime() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: Face? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.first() }
            }

            val expectedPose = Pose(Vector3(.5f, .6f, .7f), Quaternion(.1f, .2f, 3f, .4f))
            testFace.noseTipPose = expectedPose
            advanceUntilIdle()

            check(underTest != null)
            assertThat(underTest.state.value.noseTipPose).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun update_foreheadLeftPoseMatchesRuntime() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: Face? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.first() }
            }

            val expectedPose = Pose(Vector3(.5f, .6f, .7f), Quaternion(.1f, .2f, 3f, .4f))
            testFace.foreheadLeftPose = expectedPose
            advanceUntilIdle()

            check(underTest != null)
            assertThat(underTest.state.value.foreheadLeftPose).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun update_foreheadRightPoseMatchesRuntime() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: Face? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.first() }
            }

            val expectedPose = Pose(Vector3(.5f, .6f, .7f), Quaternion(.1f, .2f, 3f, .4f))
            testFace.foreheadRightPose = expectedPose
            advanceUntilIdle()

            check(underTest != null)
            assertThat(underTest.state.value.foreheadRightPose).isEqualTo(expectedPose)
        }
    }

    @OptIn(ExperimentalFaceApi::class)
    @Test
    fun update_mesh_matchesRuntime() {
        session.configure(Config(faceTracking = FaceTrackingMode.MESHES))
        runTest(testDispatcher) {
            val testFace = TestFace()
            arCoreTestRule.addTrackables(testFace)
            advanceUntilIdle()

            var underTest: Face? = null
            testScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Face.subscribe(session).collect { underTest = it.first() }
            }

            val expectedMesh =
                Mesh(
                    ShortBuffer.allocate(1).put(11),
                    FloatBuffer.allocate(1).put(12f),
                    FloatBuffer.allocate(1).put(13f),
                    FloatBuffer.allocate(1).put(14f),
                )
            testFace.mesh = expectedMesh
            advanceUntilIdle()

            check(underTest != null)
            assertThat(underTest.state.value.mesh).isEqualTo(expectedMesh)
        }
    }
}
