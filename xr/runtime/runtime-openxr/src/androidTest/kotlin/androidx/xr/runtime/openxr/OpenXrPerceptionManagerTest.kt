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

package androidx.xr.runtime.openxr

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Config
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.internal.AnchorInvalidUuidException
import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.UUID
import kotlin.test.assertFailsWith
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// TODO - b/382119583: Remove the @SdkSuppress annotation once "androidx.xr.runtime.openxr.test"
// supports a
// lower SDK version.
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrPerceptionManagerTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.runtime.openxr.test")
        }

        const val XR_TIME = 50L * 1_000_000 // 50 milliseconds in nanoseconds.
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    lateinit var openXrManager: OpenXrManager
    lateinit var underTest: OpenXrPerceptionManager

    @Before
    fun setUp() {
        underTest = OpenXrPerceptionManager(OpenXrTimeSource())
    }

    @After
    fun tearDown() {
        underTest.clear()
    }

    @Test
    fun createAnchor_returnsAnchorWithTheGivenPose() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        val pose = Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f))
        val anchor = underTest.createAnchor(pose)

        assertThat(anchor.pose).isEqualTo(pose)
    }

    @Test
    fun createAnchor_anchorLimitReached_throwsException() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)

        // Number of calls comes from 'kAnchorResourcesLimit' defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        repeat(5) { underTest.createAnchor(Pose()) }

        assertThrows(AnchorResourcesExhaustedException::class.java) {
            underTest.createAnchor(Pose())
        }
    }

    @Test
    fun detachAnchor_removesAnchorWhenItDetaches() = initOpenXrManagerAndRunTest {
        underTest.update(XR_TIME)

        val anchor = underTest.createAnchor(Pose())
        check(underTest.xrResources.updatables.contains(anchor as Updatable))

        anchor.detach()

        assertThat(underTest.xrResources.updatables).doesNotContain(anchor as Updatable)
    }

    @Test
    fun updatePlanes_addsIdentityPlane() = initOpenXrManagerAndRunTest {
        // TODO: b/345314278 -- Add more meaningful tests once trackables are implemented properly
        // and a
        // fake perception library can be used mock trackables.
        underTest.updatePlanes(XR_TIME)

        assertThat(underTest.trackables).hasSize(1)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat((underTest.trackables.first() as OpenXrPlane).centerPose)
            .isEqualTo(Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1.0f)))
    }

    @Test
    fun updatePlanes_planeTrackingDisabled_doesNotAddPlane() = initOpenXrManagerAndRunTest {
        // TODO: b/345314278 -- Add more meaningful tests once trackables are implemented properly
        // and
        // a fake perception library can be used mock trackables.
        openXrManager.configure(Config(planeTracking = Config.PlaneTrackingMode.DISABLED))

        underTest.updatePlanes(XR_TIME)

        assertThat(underTest.trackables).hasSize(0)
    }

    @Test
    fun update_updatesTrackables() = initOpenXrManagerAndRunTest {
        // TODO: b/345314278 -- Add more meaningful tests once trackables are implemented properly
        // and a
        // fake perception library can be used mock trackables.
        underTest.updatePlanes(XR_TIME)
        underTest.update(XR_TIME)

        assertThat(underTest.trackables).hasSize(1)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat((underTest.trackables.first() as OpenXrPlane).centerPose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Ignore(
        "b/425697141 - Requires HEAD_TRACKING permission which is not available on Android test runners."
    )
    @Test
    fun update_updatesHands() = initOpenXrManagerAndRunTest {
        check(underTest.xrResources.updatables.size == 3)
        check(underTest.leftHand.trackingState != TrackingState.TRACKING)
        check(underTest.rightHand.trackingState != TrackingState.TRACKING)

        underTest.update(XR_TIME)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        val leftHandJoints = underTest.leftHand.handJoints
        assertThat(underTest.leftHand.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(leftHandJoints).hasSize(HandJointType.values().size)
        for (jointType in HandJointType.values()) {
            val jointTypeIndex = jointType.ordinal.toFloat()
            assertThat(leftHandJoints[jointType]!!.rotation)
                .isEqualTo(
                    Quaternion(
                        jointTypeIndex + 0.1f,
                        jointTypeIndex + 0.2f,
                        jointTypeIndex + 0.3f,
                        jointTypeIndex + 0.4f,
                    )
                )
            assertThat(leftHandJoints[jointType]!!.translation)
                .isEqualTo(
                    Vector3(jointTypeIndex + 0.5f, jointTypeIndex + 0.6f, jointTypeIndex + 0.7f)
                )
        }

        val rightHandJoints = underTest.rightHand.handJoints
        assertThat(underTest.rightHand.trackingState).isEqualTo(TrackingState.TRACKING)
        assertThat(rightHandJoints).hasSize(HandJointType.values().size)
        for (jointType in HandJointType.values()) {
            val jointTypeIndex = jointType.ordinal.toFloat()
            assertThat(rightHandJoints[jointType]!!.rotation)
                .isEqualTo(
                    Quaternion(
                        jointTypeIndex + 0.1f,
                        jointTypeIndex + 0.2f,
                        jointTypeIndex + 0.3f,
                        jointTypeIndex + 0.4f,
                    )
                )
            assertThat(rightHandJoints[jointType]!!.translation)
                .isEqualTo(
                    Vector3(jointTypeIndex + 0.5f, jointTypeIndex + 0.6f, jointTypeIndex + 0.7f)
                )
        }
    }

    @Test
    fun update_updatesArDevice() = initOpenXrManagerAndRunTest {
        check(underTest.xrResources.updatables.size == 1)
        check(underTest.arDevice.devicePose == Pose())

        underTest.update(XR_TIME)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        val arDevice = underTest.arDevice
        assertThat(arDevice.devicePose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Test
    fun update_updatesViewCameras() = initOpenXrManagerAndRunTest {
        check(underTest.xrResources.updatables.size == 1)
        check(underTest.viewCameras.size == 2)
        check(underTest.viewCameras[0].pose == Pose())
        check(underTest.viewCameras[1].pose == Pose())
        check(underTest.viewCameras[0].fieldOfView == FieldOfView(0f, 0f, 0f, 0f))
        check(underTest.viewCameras[1].fieldOfView == FieldOfView(0f, 0f, 0f, 0f))

        underTest.update(XR_TIME)

        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        val viewCameras = underTest.viewCameras
        assertThat(viewCameras.size).isEqualTo(2)
        assertThat(viewCameras[0].pose)
            .isEqualTo(Pose(Vector3(2f, 0f, 0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
        assertThat(viewCameras[0].fieldOfView).isEqualTo(FieldOfView(1f, 2f, 3f, 4f))
        assertThat(viewCameras[1].pose)
            .isEqualTo(Pose(Vector3(0f, 2f, 0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
        assertThat(viewCameras[1].fieldOfView).isEqualTo(FieldOfView(2f, 1f, 3f, 4f))
    }

    @Test
    fun update_withRawOnlyConfig_updatesRawDepthMaps() = initOpenXrManagerAndRunTest {
        check(underTest.depthMaps.size == 2)
        check(underTest.depthMaps[0].width == 0)
        check(underTest.depthMaps[0].height == 0)
        check(underTest.depthMaps[0].rawDepthMap == null)
        check(underTest.depthMaps[0].rawConfidenceMap == null)
        check(underTest.depthMaps[0].smoothDepthMap == null)
        check(underTest.depthMaps[0].smoothConfidenceMap == null)
        check(underTest.depthMaps[1].width == 0)
        check(underTest.depthMaps[1].height == 0)
        check(underTest.depthMaps[1].rawDepthMap == null)
        check(underTest.depthMaps[1].rawConfidenceMap == null)
        check(underTest.depthMaps[1].smoothDepthMap == null)
        check(underTest.depthMaps[1].smoothConfidenceMap == null)

        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.RAW_ONLY))
        underTest.update(XR_TIME)

        assertThat(underTest.depthMaps[0].width).isEqualTo(80)
        assertThat(underTest.depthMaps[0].height).isEqualTo(80)
        // The expected values of the raw depth and confidence buffers come from kTestRawDepthData
        // and kTestRawDepthConfidenceData in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        val expectedRawDepthMap: FloatBuffer = FloatBuffer.wrap(FloatArray(6400) { 8.0f })
        val expectedRawConfidenceMap: ByteBuffer = ByteBuffer.wrap(ByteArray(6400) { 100 })
        assertThat(underTest.depthMaps[0].rawDepthMap).isEqualTo(expectedRawDepthMap)
        assertThat(underTest.depthMaps[0].rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
        assertThat(underTest.depthMaps[0].smoothDepthMap).isEqualTo(null)
        assertThat(underTest.depthMaps[0].smoothConfidenceMap).isEqualTo(null)
        assertThat(underTest.depthMaps[1].width).isEqualTo(80)
        assertThat(underTest.depthMaps[1].height).isEqualTo(80)
        assertThat(underTest.depthMaps[1].rawDepthMap).isEqualTo(expectedRawDepthMap)
        assertThat(underTest.depthMaps[1].rawConfidenceMap).isEqualTo(expectedRawConfidenceMap)
        assertThat(underTest.depthMaps[1].smoothDepthMap).isEqualTo(null)
        assertThat(underTest.depthMaps[1].smoothConfidenceMap).isEqualTo(null)
    }

    @Test
    fun update_withSmoothOnlyConfig_updatesSmoothDepthMaps() = initOpenXrManagerAndRunTest {
        check(underTest.depthMaps.size == 2)
        check(underTest.depthMaps[0].width == 0)
        check(underTest.depthMaps[0].height == 0)
        check(underTest.depthMaps[0].rawDepthMap == null)
        check(underTest.depthMaps[0].rawConfidenceMap == null)
        check(underTest.depthMaps[0].smoothDepthMap == null)
        check(underTest.depthMaps[0].smoothConfidenceMap == null)
        check(underTest.depthMaps[1].width == 0)
        check(underTest.depthMaps[1].height == 0)
        check(underTest.depthMaps[1].rawDepthMap == null)
        check(underTest.depthMaps[1].rawConfidenceMap == null)
        check(underTest.depthMaps[1].smoothDepthMap == null)
        check(underTest.depthMaps[1].smoothConfidenceMap == null)

        openXrManager.configure(Config(depthEstimation = Config.DepthEstimationMode.SMOOTH_ONLY))
        underTest.update(XR_TIME)

        assertThat(underTest.depthMaps[0].width).isEqualTo(80)
        assertThat(underTest.depthMaps[0].height).isEqualTo(80)
        // The expected values of the smooth depth and confidence buffers come from
        // kTestSmoothDepthData and kTestSmoothDepthConfidenceData in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        val expectedSmoothDepthMap: FloatBuffer = FloatBuffer.wrap(FloatArray(6400) { 10.0f })
        val expectedSmoothConfidenceMap: ByteBuffer =
            ByteBuffer.wrap(ByteArray(6400) { 200.toByte() })
        assertThat(underTest.depthMaps[0].rawDepthMap).isEqualTo(null)
        assertThat(underTest.depthMaps[0].rawConfidenceMap).isEqualTo(null)
        assertThat(underTest.depthMaps[0].smoothDepthMap).isEqualTo(expectedSmoothDepthMap)
        assertThat(underTest.depthMaps[0].smoothConfidenceMap)
            .isEqualTo(expectedSmoothConfidenceMap)
        assertThat(underTest.depthMaps[1].width).isEqualTo(80)
        assertThat(underTest.depthMaps[1].height).isEqualTo(80)
        assertThat(underTest.depthMaps[1].rawDepthMap).isEqualTo(null)
        assertThat(underTest.depthMaps[1].rawConfidenceMap).isEqualTo(null)
        assertThat(underTest.depthMaps[1].smoothDepthMap).isEqualTo(expectedSmoothDepthMap)
        assertThat(underTest.depthMaps[1].smoothConfidenceMap)
            .isEqualTo(expectedSmoothConfidenceMap)
    }

    @Test
    fun hitTest_returnsHitResults() = initOpenXrManagerAndRunTest {
        underTest.updatePlanes(XR_TIME)
        underTest.update(XR_TIME)
        check(underTest.trackables.isNotEmpty())
        val trackable = underTest.trackables.first() as OpenXrPlane

        // TODO: b/345314278 -- Add more meaningful tests once trackables are implemented properly
        // and a
        // fake perception library can be used to mock trackables.
        val hitResults = underTest.hitTest(Ray(Vector3(4f, 3f, 2f), Vector3(2f, 1f, 0f)))

        assertThat(hitResults).hasSize(1)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(hitResults.first().hitPose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
        assertThat(hitResults.first().trackable).isEqualTo(trackable)
        assertThat(hitResults.first().distance).isEqualTo(5f) // sqrt((4-0)^2 + (3-0)^2 + (2-2)^2)
    }

    @Test
    fun hitTest_planeTrackingDisabled_throwsIllegalStateException() = initOpenXrManagerAndRunTest {
        openXrManager.configure(Config(planeTracking = Config.PlaneTrackingMode.DISABLED))
        underTest.updatePlanes(XR_TIME)
        underTest.update(XR_TIME)

        assertFailsWith<IllegalStateException> {
            underTest.hitTest(Ray(Vector3(4f, 3f, 2f), Vector3(2f, 1f, 0f)))
        }
    }

    @Test
    fun getPersistedAnchorUuids_returnsStubUuid() = initOpenXrManagerAndRunTest {
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kUuid` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(underTest.getPersistedAnchorUuids())
            .containsExactly(UUID.fromString("01020304-0506-0708-090a-0b0c0d0e0f10"))
    }

    @Test
    fun loadAnchor_invalidUuid_throwsException() = initOpenXrManagerAndRunTest {
        assertThrows(AnchorInvalidUuidException::class.java) { underTest.loadAnchor(UUID(0L, 0L)) }
    }

    @Test
    fun loadAnchor_returnsAnchorWithGivenUuidAndPose() = initOpenXrManagerAndRunTest {
        // The stub doesn't care about the UUID, so we can use any UUID.
        val uuid = UUID.randomUUID()
        val anchor = underTest.loadAnchor(uuid)

        assertThat(anchor.uuid).isEqualTo(uuid)
        // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time being
        // they
        // come from `kPose` defined in //third_party/jetpack_xr_natives/openxr/openxr_stub.cc
        assertThat(anchor.pose)
            .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
    }

    @Test
    fun loadAnchor_anchorLimitReached_throwsException() = initOpenXrManagerAndRunTest {
        // Number of calls comes from 'kAnchorResourcesLimit' defined in
        // //third_party/jetpack_xr_natives/openxr/openxr_stub.cc.
        // The UUID is randomized because the manager will not create duplicate anchors for the same
        // UUID.
        repeat(5) { underTest.loadAnchor(UUID.randomUUID()) }

        assertThrows(AnchorResourcesExhaustedException::class.java) {
            underTest.loadAnchor(UUID.randomUUID())
        }
    }

    @Test
    fun loadAnchorFromNativePointer_returnsAnchorWithGivenNativePointer() =
        initOpenXrManagerAndRunTest {
            val anchor = underTest.loadAnchorFromNativePointer(123L) as OpenXrAnchor
            assertThat(anchor.nativePointer).isEqualTo(123L)

            // TODO - b/346615429: Define values here using the stub's Kotlin API. For the time
            // being they
            // come from `kPose` defined in
            // //third_party/arcore/androidx/native/openxr/openxr_stub.cc
            assertThat(anchor.pose)
                .isEqualTo(Pose(Vector3(0f, 0f, 2.0f), Quaternion(0f, 1.0f, 0f, 1.0f)))
        }

    @Test
    fun unpersistAnchor_doesNotThrowIllegalStateException() = initOpenXrManagerAndRunTest {
        underTest.unpersistAnchor(UUID.randomUUID())
    }

    @Test
    fun clear_clearXrResources() = initOpenXrManagerAndRunTest {
        underTest.updatePlanes(XR_TIME)
        underTest.update(XR_TIME)
        underTest.createAnchor(Pose())
        check(underTest.trackables.isNotEmpty())
        check(underTest.xrResources.trackablesMap.isNotEmpty())
        check(underTest.xrResources.updatables.isNotEmpty())

        underTest.clear()

        assertThat(underTest.trackables).isEmpty()
        assertThat(underTest.xrResources.trackablesMap).isEmpty()
        assertThat(underTest.xrResources.updatables).isEmpty()
    }

    private fun initOpenXrManagerAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val timeSource = OpenXrTimeSource()
            openXrManager = OpenXrManager(it, underTest, timeSource)
            openXrManager.create()
            openXrManager.resume()
            openXrManager.configure(
                Config(
                    deviceTracking = Config.DeviceTrackingMode.LAST_KNOWN,
                    planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    //                    handTracking = Config.HandTrackingMode.BOTH,
                )
            )

            testBody()

            // Pause and stop the OpenXR manager here in lieu of an @After method to ensure that the
            // calls to the OpenXR manager are coming from the same thread.
            openXrManager.pause()
            openXrManager.stop()
        }
    }
}
