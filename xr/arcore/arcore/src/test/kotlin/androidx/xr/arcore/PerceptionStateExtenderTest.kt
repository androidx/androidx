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

// TODO(b/494286565) - Remove deprecation suppression when androidx.xr.runtime.FieldOfView is
// removed.
@file:Suppress("DEPRECATION")

package androidx.xr.arcore

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.Trackable as RuntimeTrackable
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimeArDevice
import androidx.xr.arcore.testing.FakeRuntimeDepthMap
import androidx.xr.arcore.testing.FakeRuntimeFace
import androidx.xr.arcore.testing.FakeRuntimeHand
import androidx.xr.arcore.testing.FakeRuntimePlane
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerceptionStateExtenderTest {

    private val handJointBufferSize: Int = 728
    private val tolerance = 1e-4f
    private lateinit var fakePerceptionRuntime: FakePerceptionRuntime
    private lateinit var timeSource: TestTimeSource
    private lateinit var underTest: PerceptionStateExtender

    @Before
    fun setUp() {
        fakePerceptionRuntime =
            FakePerceptionRuntimeFactory().createRuntime(Activity()) as FakePerceptionRuntime
        timeSource = fakePerceptionRuntime.lifecycleManager.timeSource
        underTest = PerceptionStateExtender()
    }

    @After
    fun tearDown() {
        underTest.close()
    }

    @Test
    fun extend_notInitialized_throwsIllegalStateException(): Unit = runBlocking {
        val coreState = CoreState(timeSource.markNow())

        assertFailsWith<IllegalStateException> { underTest.extend(coreState) }
    }

    @Test
    fun extend_withOneState_addsAllTrackablesToTheCollection(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))

        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        val runtimeTrackable: RuntimeTrackable = FakeRuntimePlane()
        fakePerceptionRuntime.perceptionManager.addTrackable(runtimeTrackable)

        // act
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.trackableStates).hasSize(1)
        assertThat(convertTrackable((perceptionState.trackableStates.last() as Plane.State).owner))
            .isEqualTo(runtimeTrackable)
    }

    @Test
    fun extend_withTwoStates_updatesAllTrackablesInTheirCollection(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        fakePerceptionRuntime.perceptionManager.addTrackable(FakeRuntimePlane())
        underTest.extend(CoreState(timeSource.markNow()))

        timeSource += 10.milliseconds
        val timeMark = timeSource.markNow()
        fakePerceptionRuntime.perceptionManager.trackables.clear()
        val runtimeTrackable = FakeRuntimePlane()
        fakePerceptionRuntime.perceptionManager.addTrackable(runtimeTrackable)
        val coreState = CoreState(timeMark)

        // act
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.trackableStates).hasSize(1)
        assertThat(convertTrackable((perceptionState.trackableStates.last() as Plane.State).owner))
            .isEqualTo(runtimeTrackable)
    }

    @Test
    fun extend_withTwoStates_trackableStatusUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val runtimeTrackable = FakeRuntimePlane()
        fakePerceptionRuntime.perceptionManager.addTrackable(runtimeTrackable)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(
            coreState.perceptionState!!.trackableStates.last().trackingState ==
                TrackingState.TRACKING
        )

        // act
        timeSource += 10.milliseconds
        runtimeTrackable.trackingState = TrackingState.STOPPED.toRuntimeTrackingState()
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.trackableStates.last().trackingState)
            .isEqualTo(TrackingState.STOPPED)
    }

    @Test
    fun extend_withTwoStates_handStatesUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.leftHandState != null)
        check(coreState.perceptionState!!.rightHandState != null)
        check(coreState.perceptionState!!.leftHandState!!.trackingState != TrackingState.TRACKING)
        check(coreState.perceptionState!!.leftHandState!!.handJoints.isEmpty())
        check(coreState.perceptionState!!.rightHandState!!.trackingState != TrackingState.TRACKING)
        check(coreState.perceptionState!!.rightHandState!!.handJoints.isEmpty())

        // act
        timeSource += 10.milliseconds
        val handJoints: Map<HandJointType, Pose> =
            HandJointType.entries.associateWith { joint ->
                val i = joint.ordinal.toFloat()
                Pose(
                    Vector3(i + 0.5f, i + 0.6f, i + 0.7f),
                    Quaternion(i + 0.1f, i + 0.2f, i + 0.3f, i + 0.4f),
                )
            }

        val leftRuntimeHand = fakePerceptionRuntime.perceptionManager.leftHand!! as FakeRuntimeHand
        val rightRuntimeHand =
            fakePerceptionRuntime.perceptionManager.rightHand!! as FakeRuntimeHand
        leftRuntimeHand.trackingState = TrackingState.TRACKING.toRuntimeTrackingState()
        leftRuntimeHand.handJointsBuffer = generateTestBuffer(handJoints)
        rightRuntimeHand.trackingState = TrackingState.TRACKING.toRuntimeTrackingState()
        rightRuntimeHand.handJointsBuffer = generateTestBuffer(handJoints)
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.leftHandState!!.trackingState)
            .isEqualTo(TrackingState.TRACKING)
        assertThat(coreState2.perceptionState!!.rightHandState!!.trackingState)
            .isEqualTo(TrackingState.TRACKING)
        for (jointType in HandJointType.entries) {
            val leftHandJoints = coreState2.perceptionState!!.leftHandState!!.handJoints
            val rightHandJoints = coreState2.perceptionState!!.rightHandState!!.handJoints
            assertThat(leftHandJoints[jointType]!!.translation)
                .isEqualTo(handJoints[jointType]!!.translation)
            assertRotationEquals(
                leftHandJoints[jointType]!!.rotation,
                handJoints[jointType]!!.rotation,
            )
            assertThat(rightHandJoints[jointType]!!.translation)
                .isEqualTo(handJoints[jointType]!!.translation)
            assertRotationEquals(
                rightHandJoints[jointType]!!.rotation,
                handJoints[jointType]!!.rotation,
            )
        }
    }

    @Test
    fun extend_withTwoStates_arDeviceStateUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.arDeviceState.devicePose == Pose())

        // act
        timeSource += 10.milliseconds
        val expectedDevicePose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))

        val runtimeArDevice =
            fakePerceptionRuntime.perceptionManager.arDevice!! as FakeRuntimeArDevice
        runtimeArDevice.devicePose = expectedDevicePose
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.arDeviceState.devicePose)
            .isEqualTo(expectedDevicePose)
    }

    @Test
    fun extend_withTwoStates_leftRenderViewpointStateUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.leftRenderViewpointState != null)
        val renderViewpointStateValue = coreState.perceptionState!!.leftRenderViewpointState!!
        check(renderViewpointStateValue.pose.equals(Pose(Vector3(1f, 0f, 0f), Quaternion.Identity)))
        check(
            renderViewpointStateValue.localPose.equals(
                Pose(Vector3(1f, 0f, 0f), Quaternion.Identity)
            )
        )
        check(renderViewpointStateValue.fieldOfView == FieldOfView(0f, 0f, 0f, 0f))

        // act
        timeSource += 10.milliseconds
        val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val expectedFov = FieldOfView(1f, 2f, 3f, 4f)

        val runtimeViewpoint = fakePerceptionRuntime.perceptionManager.leftRenderViewpoint!!
        runtimeViewpoint.pose = expectedPose
        runtimeViewpoint.fieldOfView = expectedFov
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        val renderViewpointStateValue2 = coreState2.perceptionState!!.leftRenderViewpointState!!
        assertThat(renderViewpointStateValue2.pose).isEqualTo(expectedPose)
        assertThat(renderViewpointStateValue2.localPose).isEqualTo(expectedPose)
        assertThat(renderViewpointStateValue2.fieldOfView).isEqualTo(expectedFov)
    }

    @Test
    fun extend_withTwoStates_rightRenderViewpointStateUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.rightRenderViewpointState != null)
        val renderViewpointStateValue = coreState.perceptionState!!.rightRenderViewpointState!!
        check(renderViewpointStateValue.pose.equals(Pose(Vector3(0f, 1f, 0f), Quaternion.Identity)))
        check(
            renderViewpointStateValue.localPose.equals(
                Pose(Vector3(0f, 1f, 0f), Quaternion.Identity)
            )
        )
        check(renderViewpointStateValue.fieldOfView == FieldOfView(0f, 0f, 0f, 0f))

        // act
        timeSource += 10.milliseconds
        val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val expectedFov = FieldOfView(1f, 2f, 3f, 4f)

        val runtimeViewpoint = fakePerceptionRuntime.perceptionManager.rightRenderViewpoint!!
        runtimeViewpoint.pose = expectedPose
        runtimeViewpoint.fieldOfView = expectedFov
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        val renderViewpointStateValue2 = coreState2.perceptionState!!.rightRenderViewpointState!!
        assertThat(renderViewpointStateValue2.pose).isEqualTo(expectedPose)
        assertThat(renderViewpointStateValue2.localPose).isEqualTo(expectedPose)
        assertThat(renderViewpointStateValue2.fieldOfView).isEqualTo(expectedFov)
    }

    @Test
    fun extend_withTwoStates_monoRenderViewpointStateUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.monoRenderViewpointState != null)
        val renderViewpointStateValue = coreState.perceptionState!!.monoRenderViewpointState!!
        check(renderViewpointStateValue.pose.equals(Pose(Vector3(0f, 0f, 1f), Quaternion.Identity)))
        check(
            renderViewpointStateValue.localPose.equals(
                Pose(Vector3(0f, 0f, 1f), Quaternion.Identity)
            )
        )
        check(renderViewpointStateValue.fieldOfView == FieldOfView(0f, 0f, 0f, 0f))

        // act
        timeSource += 10.milliseconds
        val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val expectedFov = FieldOfView(1f, 2f, 3f, 4f)

        val runtimeViewpoint = fakePerceptionRuntime.perceptionManager.monoRenderViewpoint!!
        runtimeViewpoint.pose = expectedPose
        runtimeViewpoint.fieldOfView = expectedFov
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        val renderViewpointStateValue2 = coreState2.perceptionState!!.monoRenderViewpointState!!
        assertThat(renderViewpointStateValue2.pose).isEqualTo(expectedPose)
        assertThat(renderViewpointStateValue2.localPose).isEqualTo(expectedPose)
        assertThat(renderViewpointStateValue2.fieldOfView).isEqualTo(expectedFov)
    }

    @Test
    fun extend_withTwoStates_faceStatesUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.userFaceState != null)
        check(coreState.perceptionState!!.userFaceState!!.trackingState != TrackingState.TRACKING)
        check(coreState.perceptionState!!.userFaceState!!.blendShapeValues!!.isEmpty())
        check(coreState.perceptionState!!.userFaceState!!.confidenceValues!!.isEmpty())

        // act
        timeSource += 10.milliseconds
        val runtimeFace = fakePerceptionRuntime.perceptionManager.userFace!! as FakeRuntimeFace
        runtimeFace.trackingState = TrackingState.TRACKING.toRuntimeTrackingState()
        val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
        val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
        runtimeFace.blendShapeValues = expectedBlendShapeValues
        runtimeFace.confidenceValues = expectedConfidenceValues
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.userFaceState!!.trackingState)
            .isEqualTo(TrackingState.TRACKING)
        assertThat(coreState2.perceptionState!!.userFaceState!!.blendShapeValues)
            .isEqualTo(expectedBlendShapeValues)
        assertThat(coreState2.perceptionState!!.userFaceState!!.confidenceValues)
            .isEqualTo(expectedConfidenceValues)
    }

    @Test
    fun extend_perceptionStateMapSizeExceedsMax(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)

        // act
        underTest.extend(coreState)
        // make sure the perception state is added to the map at the beginning
        check(coreState.perceptionState != null)

        for (i in 1..PerceptionStateExtender.MAX_PERCEPTION_STATE_EXTENSION_SIZE) {
            timeSource += 10.milliseconds
            underTest.extend(CoreState(timeSource.markNow()))
        }

        // assert
        assertThat(coreState.perceptionState).isNull()
    }

    @Test
    fun extend_depthMapsStateUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.leftDepthMapState != null)
        check(coreState.perceptionState!!.leftDepthMapState!!.width == 0)
        check(coreState.perceptionState!!.leftDepthMapState!!.height == 0)
        check(coreState.perceptionState!!.leftDepthMapState!!.rawDepthMap == null)
        check(coreState.perceptionState!!.leftDepthMapState!!.rawConfidenceMap == null)
        check(coreState.perceptionState!!.leftDepthMapState!!.smoothDepthMap == null)
        check(coreState.perceptionState!!.leftDepthMapState!!.smoothConfidenceMap == null)

        // act
        timeSource += 10.milliseconds
        val runtimeDepthMap =
            fakePerceptionRuntime.perceptionManager.leftDepthMap as FakeRuntimeDepthMap
        val expectedWidth = 80
        val expectedHeight = 80
        val expectedRawDepthMap = FloatBuffer.wrap(FloatArray(6400) { 8.0f })
        val expectedRawConfidenceMap = ByteBuffer.wrap(ByteArray(6400) { 100 })
        val expectedSmoothDepthMap = FloatBuffer.wrap(FloatArray(6400) { 8.0f })
        val expectedSmoothConfidenceMap = ByteBuffer.wrap(ByteArray(6400) { 200.toByte() })
        runtimeDepthMap.width = expectedWidth
        runtimeDepthMap.height = expectedHeight
        runtimeDepthMap.rawDepthMap = expectedRawDepthMap
        runtimeDepthMap.rawConfidenceMap = expectedRawConfidenceMap
        runtimeDepthMap.smoothDepthMap = expectedSmoothDepthMap
        runtimeDepthMap.smoothConfidenceMap = expectedSmoothConfidenceMap
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.leftDepthMapState!!.width).isEqualTo(expectedWidth)
        assertThat(perceptionState.leftDepthMapState!!.height).isEqualTo(expectedHeight)
        assertThat(perceptionState.leftDepthMapState!!.rawDepthMap).isEqualTo(expectedRawDepthMap)
        assertThat(perceptionState.leftDepthMapState!!.rawConfidenceMap)
            .isEqualTo(expectedRawConfidenceMap)
        assertThat(perceptionState.leftDepthMapState!!.smoothDepthMap)
            .isEqualTo(expectedSmoothDepthMap)
        assertThat(perceptionState.leftDepthMapState!!.smoothConfidenceMap)
            .isEqualTo(expectedSmoothConfidenceMap)
    }

    @Test
    fun close_cleanUpData(): Unit = runBlocking {
        // arrange
        underTest.initialize(listOf(fakePerceptionRuntime))
        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        underTest.extend(coreState)
        // make sure the perception state is added to the map at the beginning
        check(coreState.perceptionState != null)

        // act
        underTest.close()

        // assert
        assertThat(coreState.perceptionState).isNull()
    }

    private fun convertTrackable(trackable: Trackable<Trackable.State>): RuntimeTrackable {
        return when (trackable) {
            is Plane -> trackable.runtimePlane
            else ->
                throw IllegalArgumentException("Unsupported trackable type: ${trackable.javaClass}")
        }
    }

    fun generateTestBuffer(handJoints: Map<HandJointType, Pose>): FloatBuffer {
        val buffer = ByteBuffer.allocate(handJointBufferSize).order(ByteOrder.nativeOrder())

        repeat(26) {
            val handJointType = HandJointType.values()[it]
            val handJointPose = handJoints[handJointType]!!
            buffer.putFloat(handJointPose.rotation.x)
            buffer.putFloat(handJointPose.rotation.y)
            buffer.putFloat(handJointPose.rotation.z)
            buffer.putFloat(handJointPose.rotation.w)
            buffer.putFloat(handJointPose.translation.x)
            buffer.putFloat(handJointPose.translation.y)
            buffer.putFloat(handJointPose.translation.z)
        }

        buffer.flip()
        return buffer.asFloatBuffer()
    }

    fun assertRotationEquals(actual: Quaternion, expected: Quaternion) {
        assertThat(actual.x).isWithin(tolerance).of(expected.x)
        assertThat(actual.y).isWithin(tolerance).of(expected.y)
        assertThat(actual.z).isWithin(tolerance).of(expected.z)
        assertThat(actual.w).isWithin(tolerance).of(expected.w)
    }
}
