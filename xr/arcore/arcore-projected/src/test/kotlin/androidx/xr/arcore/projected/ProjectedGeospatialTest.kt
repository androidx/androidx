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

package androidx.xr.arcore.projected

import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityNetworkError
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(TestParameterInjector::class)
class ProjectedGeospatialTest {
    @Mock private lateinit var service: IProjectedPerceptionService
    private lateinit var xrResources: XrResources
    private lateinit var projectedGeospatial: ProjectedGeospatial

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        xrResources = XrResources()
        xrResources.service = service
        projectedGeospatial = xrResources.geospatial
        xrResources.trackingState = TrackingState.TRACKING
        xrResources.geospatialTrackingState = TrackingState.TRACKING
    }

    @Test
    fun createPoseFromGeospatialPose_returnsCorrectPose() {
        val geospatialPose = GeospatialPose(1.0, 2.0, 3.0, Quaternion(0.1f, 0.2f, 0.3f, 0.4f))
        val expectedPose =
            ProjectedPose().apply {
                vector =
                    ProjectedVector3().apply {
                        x = 1f
                        y = 2f
                        z = 3f
                    }
                q =
                    ProjectedQuarternion().apply {
                        x = 4f
                        y = 5f
                        z = 6f
                        w = 7f
                    }
            }
        `when`(service.createPoseFromGeospatialPose(any(ProjectedEarthPose::class.java)))
            .thenReturn(expectedPose)

        val resultPose = projectedGeospatial.createPoseFromGeospatialPose(geospatialPose)

        val expectedResultPose =
            Pose(
                Vector3(expectedPose.vector.x, expectedPose.vector.y, expectedPose.vector.z),
                Quaternion(expectedPose.q.x, expectedPose.q.y, expectedPose.q.z, expectedPose.q.w),
            )
        assertThat(resultPose).isEqualTo(expectedResultPose)
    }

    @Test
    fun createGeospatialPoseFromPose_returnsCorrectGeospatialPoseResult() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(0.1f, 0.2f, 0.3f, 0.4f))
        val expectedEarthPose =
            ProjectedEarthPose().apply {
                latitude = 10.0
                longitude = 20.0
                altitude = 30.0
                eus =
                    ProjectedQuarternion().apply {
                        x = 0.5f
                        y = 0.6f
                        z = 0.7f
                        w = 0.8f
                    }
                locationAccuracyMeters = 1.0
                altitudeAccuracyMeters = 2.0
                orientationYawAccuracyDegrees = 3.0
            }

        `when`(service.createGeospatialPoseFromPose(any(ProjectedPose::class.java)))
            .thenReturn(expectedEarthPose)

        val result = projectedGeospatial.createGeospatialPoseFromPose(pose)

        val expectedNormalizedQuaternion =
            Quaternion(
                expectedEarthPose.eus.x,
                expectedEarthPose.eus.y,
                expectedEarthPose.eus.z,
                expectedEarthPose.eus.w,
            )
        assertThat(result.geospatialPose.latitude).isEqualTo(expectedEarthPose.latitude)
        assertThat(result.geospatialPose.longitude).isEqualTo(expectedEarthPose.longitude)
        assertThat(result.geospatialPose.altitude).isEqualTo(expectedEarthPose.altitude)
        assertThat(result.geospatialPose.eastUpSouthQuaternion.x)
            .isEqualTo(expectedNormalizedQuaternion.x)
        assertThat(result.geospatialPose.eastUpSouthQuaternion.y)
            .isEqualTo(expectedNormalizedQuaternion.y)
        assertThat(result.geospatialPose.eastUpSouthQuaternion.z)
            .isEqualTo(expectedNormalizedQuaternion.z)
        assertThat(result.geospatialPose.eastUpSouthQuaternion.w)
            .isEqualTo(expectedNormalizedQuaternion.w)
        assertThat(result.horizontalAccuracy).isEqualTo(expectedEarthPose.locationAccuracyMeters)
        assertThat(result.verticalAccuracy).isEqualTo(expectedEarthPose.altitudeAccuracyMeters)
        assertThat(result.orientationYawAccuracy)
            .isEqualTo(expectedEarthPose.orientationYawAccuracyDegrees)
    }

    @Test
    fun createPoseFromGeospatialPose_notTracking_throwsException() {
        xrResources.trackingState = TrackingState.STOPPED
        xrResources.geospatialTrackingState = TrackingState.STOPPED
        assertFailsWith<GeospatialPoseNotTrackingException> {
            projectedGeospatial.createPoseFromGeospatialPose(GeospatialPose())
        }
    }

    @Test
    fun createPoseFromGeospatialPose_partiallyTracking_throwsException() {
        // This should throw
        xrResources.trackingState = TrackingState.STOPPED
        xrResources.geospatialTrackingState = TrackingState.TRACKING
        assertFailsWith<GeospatialPoseNotTrackingException> {
            projectedGeospatial.createPoseFromGeospatialPose(GeospatialPose())
        }

        // This should also throw
        xrResources.trackingState = TrackingState.TRACKING
        xrResources.geospatialTrackingState = TrackingState.STOPPED
        assertFailsWith<GeospatialPoseNotTrackingException> {
            projectedGeospatial.createPoseFromGeospatialPose(GeospatialPose())
        }
    }

    // vpsState is the enum VpsAvailability, see the code in
    // third_party/arcore/java/com/google/ar/core/VpsAvailability.java
    // and the onVpsAvailabilityChanged callback call in
    // java/com/google/android/projection/core/modules/perception/PerceptionManagerService.java.
    enum class VpsAvailabilityTestCase(
        val vpsState: Int,
        val expectedResult: KClass<out VpsAvailabilityResult>,
    ) {
        // VpsAvailability.AVAILABLE
        AVAILABLE(1, VpsAvailabilityAvailable::class),
        // VpsAvailability.UNAVAILABLE
        UNAVAILABLE(2, VpsAvailabilityUnavailable::class),
        // VpsAvailability.ERROR_NETWORK_CONNECTION
        NETWORK_ERROR(-2, VpsAvailabilityNetworkError::class),
        // VpsAvailability.ERROR_NOT_AUTHORIZED
        NOT_AUTHORIZED(-3, VpsAvailabilityNotAuthorized::class),
        // VpsAvailability.ERROR_RESOURCE_EXHAUSTED
        RESOURCE_EXHAUSTED(-4, VpsAvailabilityResourceExhausted::class),
        // VpsAvailability.ERROR_INTERNAL
        INTERNAL_ERROR(-1, VpsAvailabilityErrorInternal::class),
        // VpsAvailability.UNKNOWN
        UNKNOWN(0, VpsAvailabilityErrorInternal::class),
    }

    @Test
    fun checkVpsAvailability_returnsCorrectResult(
        @TestParameter testCase: VpsAvailabilityTestCase
    ) = runTest {
        doAnswer { invocation ->
                val callback = invocation.getArgument<IVpsAvailabilityCallback>(2)
                callback.onVpsAvailabilityChanged(testCase.vpsState)
                null
            }
            .`when`(service)
            .checkVpsAvailability(eq(1.0), eq(2.0), any(IVpsAvailabilityCallback::class.java))

        val result = projectedGeospatial.checkVpsAvailability(1.0, 2.0)

        assertThat(result).isInstanceOf(testCase.expectedResult.java)
        verify(service)
            .checkVpsAvailability(eq(1.0), eq(2.0), any(IVpsAvailabilityCallback::class.java))
    }
}
