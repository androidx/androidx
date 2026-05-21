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

package androidx.xr.arcore.playservices

import android.app.Activity
import android.util.Range
import androidx.kruth.assertThrows
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.AnchorPersistenceMode
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.FaceTrackingMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.LibraryNotLinkedException
import androidx.xr.runtime.internal.UnsupportedDeviceException
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Camera
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config as ArConfig
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Config.TextureUpdateMode
import com.google.ar.core.Frame
import com.google.ar.core.Pose as ARCorePose
import com.google.ar.core.Session
import com.google.ar.core.exceptions.FineLocationPermissionNotGrantedException
import com.google.ar.core.exceptions.GooglePlayServicesLocationLibraryNotLinkedException as ARCore1xGooglePlayServicesLocationLibraryNotLinkedException
import com.google.ar.core.exceptions.SessionNotPausedException
import com.google.ar.core.exceptions.UnsupportedConfigurationException
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class, androidx.xr.runtime.PreviewSpatialApi::class)
@RunWith(AndroidJUnit4::class)
class ArCoreRuntimeTest {

    private lateinit var mockSession: Session
    private lateinit var mockCamera: Camera
    private lateinit var mockCameraPose: ARCorePose

    private val timeSource = ArCoreTimeSource()

    private lateinit var underTest: ArCoreRuntime
    private lateinit var mockArCoreApk: ArCoreApk

    @get:Rule val activityRule = ActivityScenarioRule(Activity::class.java)

    private companion object {
        private const val MIN_FPS: Int = 25
        private const val MAX_FPS: Int = 35
    }

    @Before
    fun setUp() {
        activityRule.scenario.onActivity {
            val perceptionManager = ArCorePerceptionManager(timeSource)
            mockArCoreApk = mock<ArCoreApk>()
            underTest = ArCoreRuntime(context = it, perceptionManager, timeSource, mockArCoreApk)
        }

        mockSession = mock<Session>()
        mockCamera = mock<Camera>()
        mockCameraPose = mock<ARCorePose>()
        whenever(mockCamera.pose).thenReturn(mockCameraPose)
    }

    @Test
    fun configure_supportsDefaultConfiguration() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().build()
        underTest.configure(config)

        assertThat(underTest.config).isEqualTo(config)
    }

    @Test
    @org.robolectric.annotation.Config(maxSdk = 26)
    fun configure_setsTextureUpdateMode_toValue_BIND_TO_TEXTURE_EXTERNAL_OES() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        underTest.configure(Config.Builder().build())

        val argumentCaptor = argumentCaptor<TextureUpdateMode>()
        verify(mockArConfig).setTextureUpdateMode(argumentCaptor.capture())
        assert(argumentCaptor.firstValue == TextureUpdateMode.BIND_TO_TEXTURE_EXTERNAL_OES)
    }

    @Test
    @org.robolectric.annotation.Config(minSdk = 27)
    fun configure_setsTextureUpdateMode_toValue_EXPOSE_HARDWARE_BUFFER() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        underTest.configure(Config.Builder().build())

        val argumentCaptor = argumentCaptor<TextureUpdateMode>()
        verify(mockArConfig).setTextureUpdateMode(argumentCaptor.capture())
        assert(argumentCaptor.firstValue == TextureUpdateMode.EXPOSE_HARDWARE_BUFFER)
    }

    @Test
    fun configure_faceTracking_setsAugmentedFaceMode_toValue_Disabled() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setFaceTracking(FaceTrackingMode.DISABLED).build()
        underTest.configure(config)

        val argumentCaptor = argumentCaptor<ArConfig.AugmentedFaceMode>()
        verify(mockArConfig).augmentedFaceMode = argumentCaptor.capture()
        assert(argumentCaptor.firstValue == ArConfig.AugmentedFaceMode.DISABLED)
        assertThat(underTest.config.faceTracking).isEqualTo(FaceTrackingMode.DISABLED)
    }

    @Test
    fun configure_faceTracking_setsAugmentedFaceMode_toValue_Mesh3D() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setFaceTracking(FaceTrackingMode.MESHES).build()
        underTest.configure(config)

        val argumentCaptor = argumentCaptor<ArConfig.AugmentedFaceMode>()
        verify(mockArConfig).augmentedFaceMode = argumentCaptor.capture()
        assert(argumentCaptor.firstValue == ArConfig.AugmentedFaceMode.MESH3D)
        assertThat(underTest.config.faceTracking).isEqualTo(FaceTrackingMode.MESHES)
    }

    @Test
    fun configure_faceTracking_setsAugmentedFaceMode_toValue_User_throwsUnsupportedOperationException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setFaceTracking(FaceTrackingMode.BLEND_SHAPES).build()

        assertThrows<UnsupportedOperationException> { underTest.configure(config) }
    }

    @Test
    fun configure_planeTracking_setsPlaneFindingMode_toValue_Disabled() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setPlaneTracking(PlaneTrackingMode.DISABLED).build()
        underTest.configure(config)

        val argumentCaptor = argumentCaptor<PlaneFindingMode>()
        verify(mockArConfig).setPlaneFindingMode(argumentCaptor.capture())
        assert(argumentCaptor.firstValue == PlaneFindingMode.DISABLED)
        assertThat(underTest.config.planeTracking).isEqualTo(PlaneTrackingMode.DISABLED)
    }

    @Test
    fun configure_planeTracking_setsPlaneFindingMode_toValue_HorizontalAndVertical() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config =
            Config.Builder().setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL).build()
        underTest.configure(config)

        val argumentCaptor = argumentCaptor<PlaneFindingMode>()
        verify(mockArConfig).setPlaneFindingMode(argumentCaptor.capture())
        assert(argumentCaptor.firstValue == PlaneFindingMode.HORIZONTAL_AND_VERTICAL)
        assertThat(underTest.config.planeTracking)
            .isEqualTo(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
    }

    @Test
    fun configure_imageTracking_setsAugmentedImageDatabase_toValue_Empty() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setAugmentedImageDatabase(null).build()
        underTest.configure(config)

        assertThat(mockArConfig.augmentedImageDatabase).isEqualTo(null)
        assertThat(underTest.config.augmentedImageDatabase?.entries).isNull()
    }

    @Test
    fun configure_handTracking_throwsUnsupportedOperationException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setHandTracking(HandTrackingMode.BOTH).build()
        assertFailsWith<UnsupportedOperationException> { underTest.configure(config) }
    }

    @Test
    fun configure_depthEstimation_throwsUnsupportedOperationException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setDepthEstimation(DepthEstimationMode.SMOOTH_AND_RAW).build()
        underTest.configure(config)

        assertThat(underTest.config.depthEstimation).isEqualTo(DepthEstimationMode.SMOOTH_AND_RAW)
    }

    @Test
    fun configure_anchorPersistence_throwsUnsupportedOperationException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config.Builder().setAnchorPersistence(AnchorPersistenceMode.LOCAL).build()
        assertFailsWith<UnsupportedOperationException> { underTest.configure(config) }
    }

    @Test
    fun configure_throwsSecurityException_whenFineLocationPermissionNotGranted() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)
        whenever(mockSession.configure(any()))
            .doThrow(FineLocationPermissionNotGrantedException("Test Exception"))

        val config = Config.Builder().build()
        assertFailsWith<SecurityException> { underTest.configure(config) }

        verify(mockSession).configure(mockArConfig)
    }

    @Test
    fun configure_throwsGooglePlayServicesLocationLibraryNotLinkedException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)
        whenever(mockSession.configure(any()))
            .doThrow(ARCore1xGooglePlayServicesLocationLibraryNotLinkedException("Test Exception"))

        val config = Config.Builder().build()
        assertFailsWith<LibraryNotLinkedException> { underTest.configure(config) }
        verify(mockSession).configure(mockArConfig)
    }

    @Test
    fun configure_throwsUnsupportedOperationException_whenUnsupportedConfiguration() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)
        whenever(mockSession.configure(any()))
            .doThrow(UnsupportedConfigurationException("Test Exception"))

        val config = Config.Builder().build()
        assertFailsWith<UnsupportedOperationException> { underTest.configure(config) }
        verify(mockSession).configure(mockArConfig)
    }

    @Test
    fun resume_callsSessionResume() {
        underTest._session = mockSession

        underTest.resume()

        verify(mockSession).resume()
    }

    @Test
    fun resumeCalledTwice_doesThrowSessionNotPausedException() {
        doNothing().doThrow(SessionNotPausedException()).whenever(mockSession).resume()
        underTest._session = mockSession

        underTest.resume()

        assertFailsWith<SessionNotPausedException> { underTest.resume() }
        verify(mockSession, times(2)).resume()
    }

    @Test
    fun update_updatesPerceptionManager() {
        val mockFrame = mock<Frame>()
        val mockCameraConfig = mock<CameraConfig>()
        whenever(mockFrame.camera).thenReturn(mockCamera)
        whenever(mockSession.update()).thenReturn(mockFrame)
        whenever(mockSession.cameraConfig).thenReturn(mockCameraConfig)
        whenever(mockCameraConfig.fpsRange).thenReturn(Range(MIN_FPS, MAX_FPS))
        underTest._session = mockSession
        underTest.perceptionManager.session = mockSession
        underTest.resume()

        runTest {
            underTest.update()

            verify(mockSession).update()
        }
    }

    @Test
    fun update_returnsTimeMarkFromTimeSource() {
        val mockFrame1 = mock<Frame>()
        val mockFrame2 = mock<Frame>()
        val mockCameraConfig = mock<CameraConfig>()
        val firstTimestampNs =
            1000L // first timestamp becomes the zero time mark for the time source
        val secondTimestampNs = 2000L
        whenever(mockFrame1.timestamp).thenReturn(firstTimestampNs)
        whenever(mockFrame2.timestamp).thenReturn(secondTimestampNs)
        whenever(mockFrame1.camera).thenReturn(mockCamera)
        whenever(mockFrame2.camera).thenReturn(mockCamera)
        whenever(mockSession.update()).thenReturn(mockFrame1, mockFrame2)
        whenever(mockSession.cameraConfig).thenReturn(mockCameraConfig)
        whenever(mockCameraConfig.fpsRange).thenReturn(Range(MIN_FPS, MAX_FPS))
        underTest._session = mockSession
        underTest.perceptionManager.session = mockSession
        underTest.resume()

        runTest {
            val timeMark1 = underTest.update()
            val timeMark2 = underTest.update()

            assertThat(timeMark2.minus(timeMark1))
                .isEqualTo((secondTimestampNs - firstTimestampNs).nanoseconds)
        }
    }

    @Test
    fun update_delaysForExpectedTimeBetweenFrames() {
        val mockFrame = mock<Frame>()
        whenever(mockFrame.camera).thenReturn(mockCamera)
        whenever(mockSession.update()).thenReturn(mockFrame)
        val mockCameraConfig = mock<CameraConfig>()
        whenever(mockSession.cameraConfig).thenReturn(mockCameraConfig)
        whenever(mockCameraConfig.fpsRange).thenReturn(Range(MIN_FPS, MAX_FPS))
        underTest._session = mockSession
        underTest.perceptionManager.session = mockSession
        underTest.resume()

        runTest {
            var updateHasReturned: Boolean = false
            launch {
                underTest.update()
                updateHasReturned = true
            }

            val avgFps = (MIN_FPS + MAX_FPS) / 2
            advanceTimeBy(1000L / avgFps / 2)
            assertThat(updateHasReturned).isFalse()
            advanceTimeBy(1000L / avgFps)
            assertThat(updateHasReturned).isTrue()
        }
    }

    @Test
    fun pause_doesNotThrowIllegalStateException() {
        underTest._session = mockSession

        underTest.resume()
        underTest.pause()

        verify(mockSession).pause()
    }

    @Test
    fun destroy_doesNotThrowIllegalStateException() {
        underTest._session = mockSession

        underTest.destroy()

        verify(mockSession).close()
    }

    @Test
    fun destroyCalledTwice_doesThrowIllegalStateException() {
        underTest._session = mockSession
        doNothing().doThrow(IllegalStateException()).whenever(mockSession).close()

        underTest.destroy()

        assertFailsWith<IllegalStateException> { underTest.destroy() }
        verify(mockSession, times(2)).close()
    }

    @Test
    fun initialize_throwsArCoreNotInstalledException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it))
                .thenReturn(Availability.SUPPORTED_NOT_INSTALLED)

            assertFailsWith<ApkNotInstalledException> { underTest.initialize() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    @Test
    fun initialize_throwsArCoreUnsupportedDeviceException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it))
                .thenReturn(Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE)

            assertFailsWith<UnsupportedDeviceException> { underTest.initialize() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    @Test
    fun initialize_throwsArCoreCheckAvailabilityInProgressException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it)).thenReturn(Availability.UNKNOWN_CHECKING)

            assertFailsWith<ApkCheckAvailabilityInProgressException> { underTest.initialize() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    @Test
    fun initialize_throwsArCoreCheckAvailabilityErrorException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it)).thenReturn(Availability.UNKNOWN_ERROR)

            assertFailsWith<ApkCheckAvailabilityErrorException> { underTest.initialize() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    private fun initRuntimeAndRunTest(testBody: () -> Unit) {
        activityRule.scenario.onActivity {
            val perceptionManager = ArCorePerceptionManager(timeSource)
            mockArCoreApk = mock<ArCoreApk>()
            mockSession = mock<Session>()
            underTest = ArCoreRuntime(it, perceptionManager, timeSource, mockArCoreApk)

            testBody()
        }
    }
}
