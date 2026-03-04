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
package androidx.xr.arcore.projected

import android.app.Activity
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class ProjectedManagerTest {
    @Mock private lateinit var mockActivity: Activity
    @Mock private lateinit var mockPerceptionService: IProjectedPerceptionService.Stub
    @Captor private lateinit var projectedConfigCaptor: ArgumentCaptor<ProjectedConfig>
    private lateinit var perceptionManager: ProjectedPerceptionManager
    private lateinit var underTest: ProjectedManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        `when`(mockPerceptionService.asBinder()).thenReturn(mockPerceptionService)
        `when`(mockPerceptionService.queryLocalInterface(anyString()))
            .thenReturn(mockPerceptionService)
        perceptionManager = ProjectedPerceptionManager(ProjectedTimeSource())
        underTest =
            ProjectedManager(
                mockActivity,
                perceptionManager,
                ProjectedTimeSource(),
                Dispatchers.IO,
                testPerceptionService = mockPerceptionService,
            )
    }

    @Test
    fun update_whenRunning_updatesPerceptionManagerState() = runTest {
        val projectedPose =
            ProjectedPose().apply {
                vector =
                    ProjectedVector3().apply {
                        x = 1.0f
                        y = 2.0f
                        z = 3.0f
                    }
                q =
                    ProjectedQuarternion().apply {
                        x = 1.0f
                        y = 2.0f
                        z = 3.0f
                        w = 4.0f
                    }
            }
        val expectedPose = Pose(Vector3(1.0f, 2.0f, 3.0f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        val expectedUpdateResult = ProjectedUpdateResult()
        expectedUpdateResult.deviceTrackingState = ProjectedTrackingState.TRACKING
        expectedUpdateResult.earthTrackingState = ProjectedTrackingState.STOPPED
        expectedUpdateResult.devicePose = projectedPose
        `when`(mockPerceptionService.update()).thenReturn(expectedUpdateResult)
        underTest.create()
        val config = Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN)

        underTest.configure(config)
        underTest.running.set(true)

        underTest.update()

        assertThat(perceptionManager.xrResources.deviceTrackingState)
            .isEqualTo(TrackingState.TRACKING)
        assertThat(perceptionManager.xrResources.geospatialTrackingState)
            .isEqualTo(TrackingState.STOPPED)
        assertThat(perceptionManager.arDevice.devicePose).isEqualTo(expectedPose)
        assertThat(perceptionManager.xrResources.geospatial.state)
            .isEqualTo(Geospatial.State.NOT_RUNNING)
    }

    @Test
    fun configure_withGeospatialEnabled_startsService() {
        underTest.create()
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                geospatial = GeospatialMode.VPS_AND_GPS,
            )

        underTest.configure(config)

        verify(mockPerceptionService).startWithConfiguration(any())
    }

    @Test
    fun configure_withGeospatialEnabledWithoutLocationPermissions_throwsSecurityException() {
        underTest.create()
        `when`(mockPerceptionService.startWithConfiguration(any()))
            .thenReturn(
                -21 /*ProjectedStatus.PROJECTED_ERROR_FINE_LOCATION_PERMISSION_NOT_GRANTED*/
            )
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                geospatial = GeospatialMode.VPS_AND_GPS,
            )

        assertThrows(SecurityException::class.java) { underTest.configure(config) }
    }

    @Test
    fun configure_whenAllFeaturesAreDisabled_stopsService() {
        underTest.create()
        underTest.running.set(true)

        val config =
            Config(
                deviceTracking = DeviceTrackingMode.DISABLED,
                geospatial = GeospatialMode.DISABLED,
            )
        underTest.configure(config)

        verify(mockPerceptionService).stop()
    }

    @Test
    fun configure_withIncompatibleSettings_throwsException() {
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.DISABLED,
                geospatial = GeospatialMode.VPS_AND_GPS,
            )
        assertThrows(UnsupportedOperationException::class.java) { underTest.configure(config) }
    }

    @Test
    fun configure_spatialLastKnownWithGeo_sends6DofAndGeoEnabled() {
        underTest.create()
        underTest.running.set(true)
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                geospatial = GeospatialMode.VPS_AND_GPS,
            )

        underTest.configure(config)

        verify(mockPerceptionService).startWithConfiguration(projectedConfigCaptor.capture())
        assertThat(projectedConfigCaptor.value.geospatialMode)
            .isEqualTo(ProjectedGeospatialMode.ENABLED)
        assertThat(projectedConfigCaptor.value.trackingMode)
            .isEqualTo(ProjectedTrackingMode.PROJECTED_TRACKING_6DOF)
    }

    @Test
    fun configure_spatialLastKnownWithoutGeo_sends6DofAndGeoDisabled() {
        underTest.create()
        underTest.running.set(true)
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN,
                geospatial = GeospatialMode.DISABLED,
            )

        underTest.configure(config)

        verify(mockPerceptionService, times(1))
            .startWithConfiguration(projectedConfigCaptor.capture())
        assertThat(projectedConfigCaptor.value.geospatialMode)
            .isEqualTo(ProjectedGeospatialMode.DISABLED)
        assertThat(projectedConfigCaptor.value.trackingMode)
            .isEqualTo(ProjectedTrackingMode.PROJECTED_TRACKING_6DOF)
    }

    @Test
    fun configure_inertialLastKnownWithoutGeo_sends3DofAndGeoDisabled() {
        underTest.create()
        underTest.running.set(true)
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.INERTIAL_LAST_KNOWN,
                geospatial = GeospatialMode.DISABLED,
            )

        underTest.configure(config)

        verify(mockPerceptionService, times(1))
            .startWithConfiguration(projectedConfigCaptor.capture())
        assertThat(projectedConfigCaptor.value.geospatialMode)
            .isEqualTo(ProjectedGeospatialMode.DISABLED)
        assertThat(projectedConfigCaptor.value.trackingMode)
            .isEqualTo(ProjectedTrackingMode.PROJECTED_TRACKING_3DOF)
    }

    @Test
    fun configure_inertialLastKnownWithGeo_sendsExpectedConfig() {
        underTest.create()
        underTest.running.set(true)
        val config =
            Config(
                deviceTracking = DeviceTrackingMode.INERTIAL_LAST_KNOWN,
                geospatial = GeospatialMode.VPS_AND_GPS,
            )

        underTest.configure(config)

        verify(mockPerceptionService, times(1))
            .startWithConfiguration(projectedConfigCaptor.capture())
        assertThat(projectedConfigCaptor.value.geospatialMode)
            .isEqualTo(ProjectedGeospatialMode.ENABLED)
        // Geo is not compatible with 3DoF, so it forces 6DoF
        assertThat(projectedConfigCaptor.value.trackingMode)
            .isEqualTo(ProjectedTrackingMode.PROJECTED_TRACKING_6DOF)
    }

    @Test
    fun create_never_startsService() {
        underTest.create()

        verify(mockPerceptionService, never()).startWithConfiguration(any())
    }

    @Test
    fun stop_whenServiceIsRunning_stopsServiceAndUnbinds() {
        underTest.create()
        underTest.running.set(true)

        underTest.stop()

        verify(mockPerceptionService).stop()
        verify(mockActivity).unbindService(any())
    }

    @Test
    fun stop_whenServiceIsNotRunning_doesNothing() {
        underTest.running.set(false)

        underTest.stop()

        verify(mockPerceptionService, never()).stop()
    }

    @Test
    fun stop_calledMultipleTimes_onlyUnbindsServiceOnce() {
        underTest.create()
        underTest.running.set(true)

        underTest.stop()
        underTest.stop()

        verify(mockActivity, times(1)).unbindService(any())
    }
}
