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

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestPlane
import androidx.xr.runtime.AugmentedObjectCategory
import androidx.xr.runtime.Config
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.FACE_TRACKING
import androidx.xr.runtime.manifest.HAND_TRACKING
import androidx.xr.runtime.manifest.HEAD_TRACKING
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_FINE
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.test.assertFailsWith
import kotlin.time.ComparableTimeMark
import kotlin.time.TestTimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
class PerceptionStateExtenderTest {

    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var timeSource: TestTimeSource
    private lateinit var underTest: PerceptionStateExtender

    val perceptionStateMap: MutableMap<ComparableTimeMark, PerceptionState>
        get() = PerceptionStateExtender.perceptionStateMap

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        timeSource = TestTimeSource()
        shadowOf(activity.application)
            .grantPermissions(
                SCENE_UNDERSTANDING_COARSE,
                SCENE_UNDERSTANDING_FINE,
                HAND_TRACKING,
                HEAD_TRACKING,
                FACE_TRACKING,
            )

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(
            Config(
                augmentedObjectCategories = setOf(AugmentedObjectCategory.LAPTOP),
                planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                deviceTracking = DeviceTrackingMode.SPATIAL,
                handTracking = HandTrackingMode.BOTH,
                faceTracking = FaceTrackingMode.BLEND_SHAPES,
                depthEstimation = DepthEstimationMode.SMOOTH_AND_RAW,
            )
        )

        perceptionStateMap.clear()
        underTest = PerceptionStateExtender()
        underTest.initialize(session.runtimes)
    }

    @Test
    fun extend_notInitialized_throwsIllegalStateException(): Unit =
        runTest(testDispatcher) {
            val coreState = CoreState(timeSource.markNow())

            val underTestNotInitialized = PerceptionStateExtender()

            assertFailsWith<IllegalStateException> { underTestNotInitialized.extend(coreState) }
        }

    @Test
    fun extend_once_addsAllTrackableObjectsToTheCollection() =
        runTest(testDispatcher) {
            arCoreTestRule.addTrackables(
                TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL),
                TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL),
            )
            advanceUntilIdle()

            val timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            val perceptionState = perceptionStateMap[timeMark]!!
            assertThat(perceptionState.trackableStates).hasSize(2)
            perceptionState.trackableStates.forEach {
                assertThat(it).isInstanceOf(Plane.State::class.java)
            }
        }

    @Test
    fun extend_twice_updatesTrackableObjects() =
        runTest(testDispatcher) {
            arCoreTestRule.addTrackables(TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL))
            advanceUntilIdle()

            // first extend
            var timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            var planeState = perceptionStateMap[timeMark]!!.trackableStates.single() as Plane.State
            assertThat(planeState.centerPose).isEqualTo(Pose.Identity)

            // move the trackable
            val expectedPose = Pose(Vector3.Right, Quaternion.Identity)
            arCoreTestRule.planes[0].centerPose = expectedPose
            advanceUntilIdle()

            // second extend
            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            planeState = perceptionStateMap[timeMark]!!.trackableStates.single() as Plane.State
            assertThat(planeState.centerPose).isEqualTo(expectedPose)
        }

    @Test
    fun extend_twice_trackableStatusUpdated() =
        runTest(testDispatcher) {
            arCoreTestRule.addTrackables(TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL))
            advanceUntilIdle()

            var timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            var perceptionState = perceptionStateMap[timeMark]!!
            var planeState = perceptionState.trackableStates.single() as Plane.State
            assertThat(planeState.trackingState).isEqualTo(TrackingState.TRACKING)

            arCoreTestRule.planes[0].isVisible = false
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            perceptionState = perceptionStateMap[timeMark]!!
            planeState = perceptionState.trackableStates.single() as Plane.State
            assertThat(planeState.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun extend_twice_leftHandStatesUpdated() =
        runTest(testDispatcher) {
            arCoreTestRule.leftHand.isVisible = false
            advanceUntilIdle()

            var timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            var leftHandState = perceptionStateMap[timeMark]!!.leftHandState!!
            assertThat(leftHandState.trackingState).isEqualTo(TrackingState.PAUSED)
            assertThat(leftHandState.handJoints[HandJointType.THUMB_TIP]).isNull()

            arCoreTestRule.leftHand.isVisible = true
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            leftHandState = perceptionStateMap[timeMark]!!.leftHandState!!
            assertThat(leftHandState.trackingState).isEqualTo(TrackingState.TRACKING)
            assertThat(leftHandState.handJoints[HandJointType.THUMB_TIP]).isNotNull()
        }

    @Test
    fun extend_twice_rightHandStatesUpdated() =
        runTest(testDispatcher) {
            arCoreTestRule.rightHand.isVisible = false
            advanceUntilIdle()

            var timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            var rightHandState = perceptionStateMap[timeMark]!!.rightHandState!!
            assertThat(rightHandState.trackingState).isEqualTo(TrackingState.PAUSED)
            assertThat(rightHandState.handJoints[HandJointType.THUMB_TIP]).isNull()

            arCoreTestRule.rightHand.isVisible = true
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            rightHandState = perceptionStateMap[timeMark]!!.rightHandState!!
            assertThat(rightHandState.trackingState).isEqualTo(TrackingState.TRACKING)
            assertThat(rightHandState.handJoints[HandJointType.THUMB_TIP]).isNotNull()
        }

    @Test
    fun extend_twice_arDeviceStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var perceptionState = perceptionStateMap[timeMark]!!
            assertThat(perceptionState.arDeviceState.devicePose).isEqualTo(Pose.Identity)

            val expectedDevicePose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            arCoreTestRule.device.pose = expectedDevicePose
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            perceptionState = perceptionStateMap[timeMark]!!
            assertThat(perceptionState.arDeviceState.devicePose).isEqualTo(expectedDevicePose)
        }

    @Test
    fun extend_twice_leftRenderViewpointStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var leftRenderViewpointState = perceptionStateMap[timeMark]!!.leftRenderViewpointState!!
            assertThat(leftRenderViewpointState.pose).isEqualTo(Pose.Identity)
            assertThat(leftRenderViewpointState.fieldOfView.angleLeft).isEqualTo(0f)
            assertThat(leftRenderViewpointState.fieldOfView.angleRight).isEqualTo(0f)
            assertThat(leftRenderViewpointState.fieldOfView.angleUp).isEqualTo(0f)
            assertThat(leftRenderViewpointState.fieldOfView.angleDown).isEqualTo(0f)

            val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            val expectedFov = FieldOfView(1f, 2f, 3f, 4f)
            arCoreTestRule.leftRenderViewpoint.apply {
                pose = expectedPose
                fieldOfView = expectedFov
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            leftRenderViewpointState = perceptionStateMap[timeMark]!!.leftRenderViewpointState!!
            assertThat(leftRenderViewpointState.pose).isEqualTo(expectedPose)
            assertThat(leftRenderViewpointState.fieldOfView.angleLeft)
                .isEqualTo(expectedFov.angleLeft)
            assertThat(leftRenderViewpointState.fieldOfView.angleRight)
                .isEqualTo(expectedFov.angleRight)
            assertThat(leftRenderViewpointState.fieldOfView.angleUp).isEqualTo(expectedFov.angleUp)
            assertThat(leftRenderViewpointState.fieldOfView.angleDown)
                .isEqualTo(expectedFov.angleDown)
        }

    @Test
    fun extend_twice_rightRenderViewpointStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var rightRenderViewpointState =
                perceptionStateMap[timeMark]!!.rightRenderViewpointState!!
            assertThat(rightRenderViewpointState.pose).isEqualTo(Pose.Identity)
            assertThat(rightRenderViewpointState.fieldOfView.angleLeft).isEqualTo(0f)
            assertThat(rightRenderViewpointState.fieldOfView.angleRight).isEqualTo(0f)
            assertThat(rightRenderViewpointState.fieldOfView.angleUp).isEqualTo(0f)
            assertThat(rightRenderViewpointState.fieldOfView.angleDown).isEqualTo(0f)

            val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            val expectedFov = FieldOfView(1f, 2f, 3f, 4f)
            arCoreTestRule.rightRenderViewpoint.apply {
                pose = expectedPose
                fieldOfView = expectedFov
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            rightRenderViewpointState = perceptionStateMap[timeMark]!!.rightRenderViewpointState!!
            assertThat(rightRenderViewpointState.pose).isEqualTo(expectedPose)
            assertThat(rightRenderViewpointState.fieldOfView.angleLeft)
                .isEqualTo(expectedFov.angleLeft)
            assertThat(rightRenderViewpointState.fieldOfView.angleRight)
                .isEqualTo(expectedFov.angleRight)
            assertThat(rightRenderViewpointState.fieldOfView.angleUp).isEqualTo(expectedFov.angleUp)
            assertThat(rightRenderViewpointState.fieldOfView.angleDown)
                .isEqualTo(expectedFov.angleDown)
        }

    @Test
    fun extend_twice_monoRenderViewpointStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var monoRenderViewpointState = perceptionStateMap[timeMark]!!.monoRenderViewpointState!!
            assertThat(monoRenderViewpointState.pose).isEqualTo(Pose.Identity)
            assertThat(monoRenderViewpointState.fieldOfView.angleLeft).isEqualTo(0f)
            assertThat(monoRenderViewpointState.fieldOfView.angleRight).isEqualTo(0f)
            assertThat(monoRenderViewpointState.fieldOfView.angleUp).isEqualTo(0f)
            assertThat(monoRenderViewpointState.fieldOfView.angleDown).isEqualTo(0f)

            val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            val expectedFov = FieldOfView(1f, 2f, 3f, 4f)
            arCoreTestRule.monoRenderViewpoint.apply {
                pose = expectedPose
                fieldOfView = expectedFov
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            monoRenderViewpointState = perceptionStateMap[timeMark]!!.monoRenderViewpointState!!
            assertThat(monoRenderViewpointState.pose).isEqualTo(expectedPose)
            assertThat(monoRenderViewpointState.fieldOfView.angleLeft)
                .isEqualTo(expectedFov.angleLeft)
            assertThat(monoRenderViewpointState.fieldOfView.angleRight)
                .isEqualTo(expectedFov.angleRight)
            assertThat(monoRenderViewpointState.fieldOfView.angleUp).isEqualTo(expectedFov.angleUp)
            assertThat(monoRenderViewpointState.fieldOfView.angleDown)
                .isEqualTo(expectedFov.angleDown)
        }

    @Test
    fun extend_twice_faceStatesUpdated() =
        runTest(testDispatcher) {
            arCoreTestRule.face.isValid = false
            advanceUntilIdle()

            var timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            var faceState = perceptionStateMap[timeMark]!!.userFaceState!!
            assertThat(faceState.trackingState).isEqualTo(TrackingState.PAUSED)
            assertThat(faceState.blendShapeValues)
                .isEqualTo(FloatArray(Face.blendShapeMapKeys.size))
            assertThat(faceState.confidenceValues)
                .isEqualTo(FloatArray(Face.confidenceRegions.size))

            val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
            val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
            arCoreTestRule.face.apply {
                isValid = true
                blendShapeValues = expectedBlendShapeValues.toList()
                confidenceValues = expectedConfidenceValues.toList()
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            faceState = perceptionStateMap[timeMark]!!.userFaceState!!
            assertThat(faceState.trackingState).isEqualTo(TrackingState.TRACKING)
            assertThat(faceState.blendShapeValues).isEqualTo(expectedBlendShapeValues)
            assertThat(faceState.confidenceValues).isEqualTo(expectedConfidenceValues)
        }

    @Test
    fun extend_perceptionStateMapSizeExceedsMax_stateIsNull() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            repeat(PerceptionStateExtender.MAX_PERCEPTION_STATE_EXTENSION_SIZE) {
                advanceUntilIdle()
                val nextTimeMark = timeSource.markNow()
                underTest.extend(CoreState(nextTimeMark))
            }

            assertThat(perceptionStateMap[timeMark]).isNull()
        }

    @Test
    fun extend_twice_leftDepthsStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var leftDepthState = perceptionStateMap[timeMark]!!.leftDepthState!!
            assertThat(leftDepthState).isNotNull()
            assertThat(leftDepthState.width).isEqualTo(0)
            assertThat(leftDepthState.height).isEqualTo(0)
            assertThat(leftDepthState.rawDepthMap).isNull()
            assertThat(leftDepthState.rawConfidenceMap).isNull()
            assertThat(leftDepthState.smoothDepthMap).isNull()
            assertThat(leftDepthState.smoothConfidenceMap).isNull()

            val expectedWidth = 2
            val expectedHeight = 2
            val expectedRawDepth = FloatBuffer.wrap(FloatArray(4) { 8.0f })
            val expectedRawConfidenceMap = ByteBuffer.wrap(ByteArray(4) { 99 })
            val expectedSmoothDepth = FloatBuffer.wrap(FloatArray(4) { 8.888f })
            val expectedSmoothConfidenceMap = ByteBuffer.wrap(ByteArray(4) { 100 })
            arCoreTestRule.leftDepth.apply {
                width = expectedWidth
                height = expectedHeight
                rawDepthMap = expectedRawDepth
                rawConfidenceMap = expectedRawConfidenceMap
                smoothDepthMap = expectedSmoothDepth
                smoothConfidenceMap = expectedSmoothConfidenceMap
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            leftDepthState = perceptionStateMap[timeMark]!!.leftDepthState!!
            assertThat(leftDepthState).isNotNull()
            assertThat(leftDepthState.width).isEqualTo(expectedWidth)
            assertThat(leftDepthState.height).isEqualTo(expectedHeight)
            assertThat(leftDepthState.rawDepthMap).isEqualTo(expectedRawDepth)
            assertThat(leftDepthState.rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
            assertThat(leftDepthState.smoothDepthMap).isEqualTo(expectedSmoothDepth)
            assertThat(leftDepthState.smoothConfidenceMap).isEqualTo(expectedSmoothConfidenceMap)
        }

    @Test
    fun extend_twice_rightDepthsStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var rightDepthState = perceptionStateMap[timeMark]!!.rightDepthState!!
            assertThat(rightDepthState).isNotNull()
            assertThat(rightDepthState.width).isEqualTo(0)
            assertThat(rightDepthState.height).isEqualTo(0)
            assertThat(rightDepthState.rawDepthMap).isNull()
            assertThat(rightDepthState.rawConfidenceMap).isNull()
            assertThat(rightDepthState.smoothDepthMap).isNull()
            assertThat(rightDepthState.smoothConfidenceMap).isNull()

            val expectedWidth = 2
            val expectedHeight = 2
            val expectedRawDepth = FloatBuffer.wrap(FloatArray(4) { 8.0f })
            val expectedRawConfidenceMap = ByteBuffer.wrap(ByteArray(4) { 99 })
            val expectedSmoothDepth = FloatBuffer.wrap(FloatArray(4) { 8.888f })
            val expectedSmoothConfidenceMap = ByteBuffer.wrap(ByteArray(4) { 100 })
            arCoreTestRule.rightDepth.apply {
                width = expectedWidth
                height = expectedHeight
                rawDepthMap = expectedRawDepth
                rawConfidenceMap = expectedRawConfidenceMap
                smoothDepthMap = expectedSmoothDepth
                smoothConfidenceMap = expectedSmoothConfidenceMap
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            rightDepthState = perceptionStateMap[timeMark]!!.rightDepthState!!
            assertThat(rightDepthState).isNotNull()
            assertThat(rightDepthState.width).isEqualTo(expectedWidth)
            assertThat(rightDepthState.height).isEqualTo(expectedHeight)
            assertThat(rightDepthState.rawDepthMap).isEqualTo(expectedRawDepth)
            assertThat(rightDepthState.rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
            assertThat(rightDepthState.smoothDepthMap).isEqualTo(expectedSmoothDepth)
            assertThat(rightDepthState.smoothConfidenceMap).isEqualTo(expectedSmoothConfidenceMap)
        }

    @Test
    fun extend_twice_monoDepthsStateUpdated() =
        runTest(testDispatcher) {
            var timeMark = timeSource.markNow()

            underTest.extend(CoreState(timeMark))

            var monoDepthState = perceptionStateMap[timeMark]!!.monoDepthState!!
            assertThat(monoDepthState).isNotNull()
            assertThat(monoDepthState.width).isEqualTo(0)
            assertThat(monoDepthState.height).isEqualTo(0)
            assertThat(monoDepthState.rawDepthMap).isNull()
            assertThat(monoDepthState.rawConfidenceMap).isNull()
            assertThat(monoDepthState.smoothDepthMap).isNull()
            assertThat(monoDepthState.smoothConfidenceMap).isNull()

            val expectedWidth = 2
            val expectedHeight = 2
            val expectedRawDepth = FloatBuffer.wrap(FloatArray(4) { 8.0f })
            val expectedRawConfidenceMap = ByteBuffer.wrap(ByteArray(4) { 99 })
            val expectedSmoothDepth = FloatBuffer.wrap(FloatArray(4) { 8.888f })
            val expectedSmoothConfidenceMap = ByteBuffer.wrap(ByteArray(4) { 100 })
            arCoreTestRule.monoDepth.apply {
                width = expectedWidth
                height = expectedHeight
                rawDepthMap = expectedRawDepth
                rawConfidenceMap = expectedRawConfidenceMap
                smoothDepthMap = expectedSmoothDepth
                smoothConfidenceMap = expectedSmoothConfidenceMap
            }
            advanceUntilIdle()

            timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))

            monoDepthState = perceptionStateMap[timeMark]!!.monoDepthState!!
            assertThat(monoDepthState).isNotNull()
            assertThat(monoDepthState.width).isEqualTo(expectedWidth)
            assertThat(monoDepthState.height).isEqualTo(expectedHeight)
            assertThat(monoDepthState.rawDepthMap).isEqualTo(expectedRawDepth)
            assertThat(monoDepthState.rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
            assertThat(monoDepthState.smoothDepthMap).isEqualTo(expectedSmoothDepth)
            assertThat(monoDepthState.smoothConfidenceMap).isEqualTo(expectedSmoothConfidenceMap)
        }

    @Test
    fun close_cleanUpData() {
        runTest(testDispatcher) {
            val timeMark = timeSource.markNow()
            underTest.extend(CoreState(timeMark))
            assertThat(perceptionStateMap[timeMark]).isNotNull()

            underTest.close()

            assertThat(perceptionStateMap[timeMark]).isNull()
        }
    }
}
