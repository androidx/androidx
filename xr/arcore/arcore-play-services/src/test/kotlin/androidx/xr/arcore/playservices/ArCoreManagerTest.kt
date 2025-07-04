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

package androidx.xr.arcore.playservices

import android.Manifest
import android.app.Activity
import android.util.Range
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.AnchorPersistenceMode
import androidx.xr.runtime.Config.DepthEstimationMode
import androidx.xr.runtime.Config.HandTrackingMode
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.internal.ApkCheckAvailabilityErrorException
import androidx.xr.runtime.internal.ApkCheckAvailabilityInProgressException
import androidx.xr.runtime.internal.ApkNotInstalledException
import androidx.xr.runtime.internal.ConfigurationNotSupportedException
import androidx.xr.runtime.internal.GooglePlayServicesLocationLibraryNotLinkedException
import androidx.xr.runtime.internal.PermissionNotGrantedException
import androidx.xr.runtime.internal.UnsupportedDeviceException
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config as ArConfig
import com.google.ar.core.Config.PlaneFindingMode
import com.google.ar.core.Config.TextureUpdateMode
import com.google.ar.core.Frame
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ArCoreManagerTest {

    private lateinit var mockSession: Session

    private val timeSource = ArCoreTimeSource()

    private lateinit var underTest: ArCoreManager
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
            underTest = ArCoreManager(activity = it, perceptionManager, timeSource, mockArCoreApk)
        }

        mockSession = mock<Session>()
    }

    @Test
    fun configure_supportsDefaultConfiguration() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config()
        underTest.configure(config)

        assertThat(underTest.config).isEqualTo(config)
    }

    @Test
    fun configure_setsTextureUpdateMode_toValue_EXPOSE_HARDWARE_BUFFER() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        underTest.configure(Config())

        val argumentCaptor = argumentCaptor<TextureUpdateMode>()
        verify(mockArConfig).setTextureUpdateMode(argumentCaptor.capture())
        assert(argumentCaptor.firstValue == TextureUpdateMode.EXPOSE_HARDWARE_BUFFER)
    }

    @Test
    fun configure_planeTracking_setsPlaneFindingMode_toValue_Disabled() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config(planeTracking = Config.PlaneTrackingMode.DISABLED)
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

        val config = Config(planeTracking = Config.PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
        underTest.configure(config)

        val argumentCaptor = argumentCaptor<PlaneFindingMode>()
        verify(mockArConfig).setPlaneFindingMode(argumentCaptor.capture())
        assert(argumentCaptor.firstValue == PlaneFindingMode.HORIZONTAL_AND_VERTICAL)
        assertThat(underTest.config.planeTracking)
            .isEqualTo(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
    }

    @Test
    fun configure_handTracking_throwsConfigurationNotSupportedException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config(handTracking = Config.HandTrackingMode.BOTH)
        assertFailsWith<ConfigurationNotSupportedException> { underTest.configure(config) }
    }

    @Test
    fun configure_depthEstimation_throwsConfigurationNotSupportedException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config(depthEstimation = Config.DepthEstimationMode.SMOOTH_AND_RAW)
        assertFailsWith<ConfigurationNotSupportedException> { underTest.configure(config) }
    }

    @Test
    fun configure_anchorPersistence_throwsConfigurationNotSupportedException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)

        val config = Config(anchorPersistence = Config.AnchorPersistenceMode.LOCAL)
        assertFailsWith<ConfigurationNotSupportedException> { underTest.configure(config) }
    }

    @Test
    fun configure_throwsPermissionNotGrantedException_whenFineLocationPermissionNotGranted() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)
        whenever(mockSession.configure(any()))
            .doThrow(FineLocationPermissionNotGrantedException("Test Exception"))

        val config = Config()
        val exception: PermissionNotGrantedException =
            assertFailsWith<PermissionNotGrantedException> { underTest.configure(config) }

        assertThat(exception.permissions).containsExactly(Manifest.permission.ACCESS_FINE_LOCATION)
        verify(mockSession).configure(mockArConfig)
    }

    @Test
    fun configure_throwsGooglePlayServicesLocationLibraryNotLinkedException() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)
        whenever(mockSession.configure(any()))
            .doThrow(ARCore1xGooglePlayServicesLocationLibraryNotLinkedException("Test Exception"))

        val config = Config()
        assertFailsWith<GooglePlayServicesLocationLibraryNotLinkedException> {
            underTest.configure(config)
        }
        verify(mockSession).configure(mockArConfig)
    }

    @Test
    fun configure_throwsConfigurationNotSupportedException_whenUnsupportedConfiguration() {
        val mockArConfig = mock<ArConfig>()
        underTest._session = mockSession
        whenever(mockSession.config).thenReturn(mockArConfig)
        whenever(mockSession.configure(any()))
            .doThrow(UnsupportedConfigurationException("Test Exception"))

        val config = Config()
        assertFailsWith<ConfigurationNotSupportedException> { underTest.configure(config) }
        verify(mockSession).configure(mockArConfig)
    }

    @Test
    fun resume_callsSessionResume() {
        underTest._session = mockSession

        underTest.resume()

        assertThat(underTest.running).isTrue()
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

        assertThat(underTest.running).isFalse()
        verify(mockSession).pause()
    }

    @Test
    fun stop_doesNotThrowIllegalStateException() {
        underTest._session = mockSession

        underTest.stop()

        verify(mockSession).close()
    }

    @Test
    fun stopCalledTwice_doesThrowIllegalStateException() {
        underTest._session = mockSession
        doNothing().doThrow(IllegalStateException()).whenever(mockSession).close()

        underTest.stop()

        assertFailsWith<IllegalStateException> { underTest.stop() }
        verify(mockSession, times(2)).close()
    }

    @Test
    fun create_throwsArCoreNotInstalledException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it))
                .thenReturn(Availability.SUPPORTED_NOT_INSTALLED)

            assertFailsWith<ApkNotInstalledException> { underTest.create() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    @Test
    fun create_throwsArCoreUnsupportedDeviceException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it))
                .thenReturn(Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE)

            assertFailsWith<UnsupportedDeviceException> { underTest.create() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    @Test
    fun create_throwsArCoreCheckAvailabilityInProgressException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it)).thenReturn(Availability.UNKNOWN_CHECKING)

            assertFailsWith<ApkCheckAvailabilityInProgressException> { underTest.create() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }

    @Test
    fun create_throwsArCoreCheckAvailabilityErrorException() {
        activityRule.scenario.onActivity {
            whenever(mockArCoreApk.checkAvailability(it)).thenReturn(Availability.UNKNOWN_ERROR)

            assertFailsWith<ApkCheckAvailabilityErrorException> { underTest.create() }
            verify(mockArCoreApk).checkAvailability(it)
        }
    }
}
