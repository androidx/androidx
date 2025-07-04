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

package androidx.xr.arcore

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.CoreState
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.Trackable as RuntimeTrackable
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeRuntime
import androidx.xr.runtime.testing.FakeRuntimeArDevice
import androidx.xr.runtime.testing.FakeRuntimeDepthMap
import androidx.xr.runtime.testing.FakeRuntimeFace
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeRuntimeHand
import androidx.xr.runtime.testing.FakeRuntimePlane
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
    private lateinit var fakeRuntime: FakeRuntime
    private lateinit var timeSource: TestTimeSource
    private lateinit var underTest: PerceptionStateExtender

    @Before
    fun setUp() {
        fakeRuntime = FakeRuntimeFactory().createRuntime(Activity())
        timeSource = fakeRuntime.lifecycleManager.timeSource
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
        underTest.initialize(fakeRuntime)

        val timeMark = timeSource.markNow()
        val coreState = CoreState(timeMark)
        val runtimeTrackable: RuntimeTrackable = FakeRuntimePlane()
        fakeRuntime.perceptionManager.addTrackable(runtimeTrackable)

        // act
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.trackables).hasSize(1)
        assertThat(convertTrackable(perceptionState.trackables.last())).isEqualTo(runtimeTrackable)
    }

    @Test
    fun extend_withTwoStates_updatesAllTrackablesInTheirCollection(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        fakeRuntime.perceptionManager.addTrackable(FakeRuntimePlane())
        underTest.extend(CoreState(timeSource.markNow()))

        timeSource += 10.milliseconds
        val timeMark = timeSource.markNow()
        fakeRuntime.perceptionManager.trackables.clear()
        val runtimeTrackable = FakeRuntimePlane()
        fakeRuntime.perceptionManager.addTrackable(runtimeTrackable)
        val coreState = CoreState(timeMark)

        // act
        underTest.extend(coreState)

        // assert
        val perceptionState = coreState.perceptionState!!
        assertThat(perceptionState.trackables).hasSize(1)
        assertThat(convertTrackable(perceptionState.trackables.last())).isEqualTo(runtimeTrackable)
    }

    @Test
    fun extend_withTwoStates_trackableStatusUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val runtimeTrackable = FakeRuntimePlane()
        fakeRuntime.perceptionManager.addTrackable(runtimeTrackable)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(
            coreState.perceptionState!!.trackables.last().state.value.trackingState ==
                TrackingState.TRACKING
        )

        // act
        timeSource += 10.milliseconds
        runtimeTrackable.trackingState = TrackingState.STOPPED
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.trackables.last().state.value.trackingState)
            .isEqualTo(TrackingState.STOPPED)
    }

    @Test
    fun extend_withTwoStates_handStatesUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.leftHand != null)
        check(coreState.perceptionState!!.rightHand != null)
        check(
            coreState.perceptionState!!.leftHand!!.state.value.trackingState !=
                TrackingState.TRACKING
        )
        check(coreState.perceptionState!!.leftHand!!.state.value.handJoints.isEmpty())
        check(
            coreState.perceptionState!!.rightHand!!.state.value.trackingState !=
                TrackingState.TRACKING
        )
        check(coreState.perceptionState!!.rightHand!!.state.value.handJoints.isEmpty())

        // act
        timeSource += 10.milliseconds
        val handJoints: Map<HandJointType, Pose> =
            HandJointType.values().associate { joint ->
                val i = joint.ordinal.toFloat()
                joint to
                    Pose(
                        Vector3(i + 0.5f, i + 0.6f, i + 0.7f),
                        Quaternion(i + 0.1f, i + 0.2f, i + 0.3f, i + 0.4f),
                    )
            }

        val leftRuntimeHand = fakeRuntime.perceptionManager.leftHand!! as FakeRuntimeHand
        val rightRuntimeHand = fakeRuntime.perceptionManager.rightHand!! as FakeRuntimeHand
        leftRuntimeHand.trackingState = TrackingState.TRACKING
        leftRuntimeHand.handJointsBuffer = generateTestBuffer(handJoints)
        rightRuntimeHand.trackingState = TrackingState.TRACKING
        rightRuntimeHand.handJointsBuffer = generateTestBuffer(handJoints)
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.leftHand!!.state.value.trackingState)
            .isEqualTo(TrackingState.TRACKING)
        assertThat(coreState2.perceptionState!!.rightHand!!.state.value.trackingState)
            .isEqualTo(TrackingState.TRACKING)
        for (jointType in HandJointType.values()) {
            val leftHandJoints = coreState2.perceptionState!!.leftHand!!.state.value.handJoints
            val rightHandJoints = coreState2.perceptionState!!.rightHand!!.state.value.handJoints
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
        underTest.initialize(fakeRuntime)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.arDevice.state.value.devicePose == Pose())

        // act
        timeSource += 10.milliseconds
        val expectedDevicePose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))

        val runtimeArDevice = fakeRuntime.perceptionManager.arDevice!! as FakeRuntimeArDevice
        runtimeArDevice.devicePose = expectedDevicePose
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.arDevice.state.value.devicePose)
            .isEqualTo(expectedDevicePose)
    }

    @Test
    fun extend_withTwoStates_viewCameraStateUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.viewCameras.isNotEmpty())
        check(coreState.perceptionState!!.viewCameras[0].state.value.pose == Pose())
        check(coreState.perceptionState!!.viewCameras[0].state.value.localPose == Pose())
        check(
            coreState.perceptionState!!.viewCameras[0].state.value.fieldOfView ==
                FieldOfView(0f, 0f, 0f, 0f)
        )

        // act
        timeSource += 10.milliseconds
        val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val expectedFov = FieldOfView(1f, 2f, 3f, 4f)

        val runtimeViewCamera = fakeRuntime.perceptionManager.viewCameras[0]
        runtimeViewCamera.pose = expectedPose
        runtimeViewCamera.fieldOfView = expectedFov
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.viewCameras[0].state.value.pose)
            .isEqualTo(expectedPose)
        assertThat(coreState2.perceptionState!!.viewCameras[0].state.value.localPose)
            .isEqualTo(expectedPose)
        assertThat(coreState2.perceptionState!!.viewCameras[0].state.value.fieldOfView)
            .isEqualTo(expectedFov)
    }

    @Test
    fun extend_withTwoStates_faceStatesUpdated(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.userFace != null)
        check(
            coreState.perceptionState!!.userFace!!.state.value.trackingState !=
                TrackingState.TRACKING
        )
        check(coreState.perceptionState!!.userFace!!.state.value.blendShapeValues.isEmpty())
        check(coreState.perceptionState!!.userFace!!.state.value.confidenceValues.isEmpty())

        // act
        timeSource += 10.milliseconds
        val runtimeFace = fakeRuntime.perceptionManager.userFace!! as FakeRuntimeFace
        runtimeFace.trackingState = TrackingState.TRACKING
        val expectedBlendShapeValues = floatArrayOf(0.1f, 0.2f, 0.3f)
        val expectedConfidenceValues = floatArrayOf(0.4f, 0.5f, 0.6f)
        runtimeFace.blendShapeValues = expectedBlendShapeValues
        runtimeFace.confidenceValues = expectedConfidenceValues
        val coreState2 = CoreState(timeSource.markNow())
        underTest.extend(coreState2)

        // assert
        assertThat(coreState2.perceptionState!!.userFace!!.state.value.trackingState)
            .isEqualTo(TrackingState.TRACKING)
        assertThat(coreState2.perceptionState!!.userFace!!.state.value.blendShapeValues)
            .isEqualTo(expectedBlendShapeValues)
        assertThat(coreState2.perceptionState!!.userFace!!.state.value.confidenceValues)
            .isEqualTo(expectedConfidenceValues)
    }

    @Test
    fun extend_perceptionStateMapSizeExceedsMax(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
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
        underTest.initialize(fakeRuntime)
        val coreState = CoreState(timeSource.markNow())
        underTest.extend(coreState)
        check(coreState.perceptionState!!.depthMaps.isNotEmpty())
        check(coreState.perceptionState!!.depthMaps[0].state.value.width == 0)
        check(coreState.perceptionState!!.depthMaps[0].state.value.height == 0)
        check(coreState.perceptionState!!.depthMaps[0].state.value.rawDepthMap == null)
        check(coreState.perceptionState!!.depthMaps[0].state.value.rawConfidenceMap == null)
        check(coreState.perceptionState!!.depthMaps[0].state.value.smoothDepthMap == null)
        check(coreState.perceptionState!!.depthMaps[0].state.value.smoothConfidenceMap == null)

        // act
        timeSource += 10.milliseconds
        val runtimeDepthMap = fakeRuntime.perceptionManager.depthMaps[0] as FakeRuntimeDepthMap
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
        assertThat(perceptionState.depthMaps[0].state.value.width).isEqualTo(expectedWidth)
        assertThat(perceptionState.depthMaps[0].state.value.height).isEqualTo(expectedHeight)
        assertThat(perceptionState.depthMaps[0].state.value.rawDepthMap)
            .isEqualTo(expectedRawDepthMap)
        assertThat(perceptionState.depthMaps[0].state.value.rawConfidenceMap)
            .isEqualTo(expectedRawConfidenceMap)
        assertThat(perceptionState.depthMaps[0].state.value.smoothDepthMap)
            .isEqualTo(expectedSmoothDepthMap)
        assertThat(perceptionState.depthMaps[0].state.value.smoothConfidenceMap)
            .isEqualTo(expectedSmoothConfidenceMap)
    }

    @Test
    fun close_cleanUpData(): Unit = runBlocking {
        // arrange
        underTest.initialize(fakeRuntime)
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
