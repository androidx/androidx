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

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.runtime.Config
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class GeospatialTest {

    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(ACCESS_FINE_LOCATION)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(geospatial = GeospatialMode.VPS_AND_GPS))

        arCoreTestRule.geospatial.state = GeospatialState.NOT_RUNNING
    }

    @Test
    fun getInstance_returnsGeospatial() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)

            assertThat(underTest).isNotNull()
        }

    @Test
    fun getInstance_initialStateIsNotRunning() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)

            assertThat(underTest.state.value).isEqualTo(GeospatialState.NOT_RUNNING)
        }

    @Test
    fun update_stateMatchesDeviceState_whenRunning() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            assertThat(underTest.state.value).isEqualTo(GeospatialState.RUNNING)
        }

    @Test
    fun update_stateMatchesDeviceState_whenPaused() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.PAUSED
            advanceUntilIdle()

            assertThat(underTest.state.value).isEqualTo(GeospatialState.PAUSED)
        }

    @Test
    fun update_stateMatchesDeviceState_whenInternalError() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.ERROR_INTERNAL
            advanceUntilIdle()

            assertThat(underTest.state.value).isEqualTo(GeospatialState.ERROR_INTERNAL)
        }

    @Test
    fun update_stateMatchesDeviceState_whenNotAuthorized() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.ERROR_NOT_AUTHORIZED
            advanceUntilIdle()

            assertThat(underTest.state.value).isEqualTo(GeospatialState.ERROR_NOT_AUTHORIZED)
        }

    @Test
    fun update_stateMatchesDeviceState_whenResourcesExhausted() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.ERROR_RESOURCE_EXHAUSTED
            advanceUntilIdle()

            assertThat(underTest.state.value).isEqualTo(GeospatialState.ERROR_RESOURCE_EXHAUSTED)
        }

    @Test
    fun createGeospatialPoseFromPose_success_returnsSuccessResult() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            val result = underTest.createGeospatialPoseFromPose(Pose(Vector3(), Quaternion()))
            check(result is CreateGeospatialPoseFromPoseSuccess)

            assertThat(result.pose).isEqualTo(arCoreTestRule.geospatial.expectedGeospatialPose)
            assertThat(result.horizontalAccuracy)
                .isEqualTo(arCoreTestRule.geospatial.expectedHorizontalAccuracy)
            assertThat(result.verticalAccuracy)
                .isEqualTo(arCoreTestRule.geospatial.expectedVerticalAccuracy)
            assertThat(result.orientationYawAccuracy)
                .isEqualTo(arCoreTestRule.geospatial.expectedOrientationYawAccuracy)
        }

    @Test
    fun createGeospatialPoseFromPose_notTracking_returnsNotTrackingResult() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            check(underTest.state.value == GeospatialState.NOT_RUNNING)

            val result = underTest.createGeospatialPoseFromPose(Pose(Vector3(), Quaternion()))

            assertThat(result).isInstanceOf(CreateGeospatialPoseFromPoseNotTracking::class.java)
        }

    @Test
    fun createPoseFromGeospatialPose_success_returnsSuccessResult() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            val result = underTest.createPoseFromGeospatialPose(GeospatialPose())
            check(result is CreatePoseFromGeospatialPoseSuccess)

            assertThat(result.pose).isEqualTo(arCoreTestRule.geospatial.expectedPose)
        }

    @Test
    fun createPoseFromGeospatialPose_notTracking_returnsNotTrackingResult() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            check(underTest.state.value == GeospatialState.NOT_RUNNING)

            val result = underTest.createPoseFromGeospatialPose(GeospatialPose())

            assertThat(result).isInstanceOf(CreatePoseFromGeospatialPoseNotTracking::class.java)
        }

    @Test
    fun createAnchor_success_returnsSuccessResultWithAnchor() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedAnchorPose = Pose()
            advanceUntilIdle()

            val latitude = 10.0
            val longitude = 20.0
            val altitude = 30.0
            val eastUpSouthQuaternion: Quaternion = Quaternion.Identity
            check(latitude in arCoreTestRule.geospatial.allowedAnchorLatitudeRange)

            val result =
                underTest.createAnchor(latitude, longitude, altitude, eastUpSouthQuaternion)

            assertThat(result).isInstanceOf(AnchorCreateSuccess::class.java)
            val successResult = result as AnchorCreateSuccess
            assertThat(successResult.anchor.state.value.pose).isEqualTo(Pose.Identity)
        }

    @Test
    fun createAnchor_resourceExhausted_returnsResourcesExhaustedResult() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.anchorResourceLimit = 6
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            repeat(arCoreTestRule.anchorResourceLimit) {
                underTest.createAnchor(0.0, 0.0, 0.0, Quaternion.Identity)
            }

            val result = underTest.createAnchor(0.0, 0.0, 0.0, Quaternion.Identity)

            assertThat(result).isInstanceOf(AnchorCreateResourcesExhausted::class.java)
        }

    @Test
    fun createAnchor_invalidLatitude_throwsIllegalArgumentException() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            val latitude = 99999.0
            val longitude = 20.0
            val altitude = 30.0
            val eastUpSouthQuaternion: Quaternion = Quaternion.Identity

            assertThat(latitude in arCoreTestRule.geospatial.allowedAnchorLatitudeRange).isFalse()

            assertFailsWith<IllegalArgumentException> {
                underTest.createAnchor(latitude, longitude, altitude, eastUpSouthQuaternion)
            }
        }

    @Test
    fun createPoseFromGeospatialPose_withVpsDisabled_throwsIllegalStateException() {
        val underTest = Geospatial.getInstance(session)

        session.configure(Config(geospatial = GeospatialMode.DISABLED))

        assertFailsWith<IllegalStateException> {
            underTest.createPoseFromGeospatialPose(
                GeospatialPose(0.0, 0.0, 0.0, Quaternion.Identity)
            )
        }
    }

    @Test
    fun createAnchorOnSurface_success_returnsSuccessResultWithAnchor() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.apply {
                state = GeospatialState.RUNNING
                expectedAnchorPose = Pose()
            }
            advanceUntilIdle()

            val latitude = 10.0
            val longitude = 20.0
            val altitudeAboveSurface = 30.0
            val eastUpSouthQuaternion = Quaternion.Identity
            check(latitude in arCoreTestRule.geospatial.allowedAnchorLatitudeRange)

            val result =
                underTest.createAnchorOnSurface(
                    latitude,
                    longitude,
                    altitudeAboveSurface,
                    eastUpSouthQuaternion,
                    GeospatialSurface.TERRAIN,
                )

            assertThat(result).isInstanceOf(AnchorCreateSuccess::class.java)
            val successResult = result as AnchorCreateSuccess
            assertThat(successResult.anchor.state.value.pose).isEqualTo(Pose.Identity)
        }

    @Test
    fun createAnchorOnSurface_resourceExhausted_returnsResourcesExhaustedResult() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.anchorResourceLimit = 6
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            repeat(arCoreTestRule.anchorResourceLimit) {
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion.Identity,
                    GeospatialSurface.TERRAIN,
                )
            }

            val result =
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion.Identity,
                    GeospatialSurface.TERRAIN,
                )

            assertThat(result).isInstanceOf(AnchorCreateResourcesExhausted::class.java)
        }

    @Test
    fun createAnchorOnSurface_notAuthorized_throwsAnchorNotAuthorizedException() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.ERROR_NOT_AUTHORIZED
            advanceUntilIdle()

            assertFailsWith<AnchorNotAuthorizedException> {
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion.Identity,
                    GeospatialSurface.TERRAIN,
                )
            }
        }

    @Test
    fun createAnchorOnSurface_unsupportedLocation_throwsIllegalArgumentException() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            advanceUntilIdle()

            val latitude = 10.0
            val longitude = 20.0
            val altitudeAboveSurface = -30.0
            val eastUpSouthQuaternion: Quaternion = Quaternion.Identity
            check(latitude in arCoreTestRule.geospatial.allowedAnchorLatitudeRange)

            assertFailsWith<IllegalArgumentException> {
                underTest.createAnchorOnSurface(
                    latitude,
                    longitude,
                    altitudeAboveSurface,
                    eastUpSouthQuaternion,
                    GeospatialSurface.TERRAIN,
                )
            }
        }

    @Test
    fun createAnchorOnSurface_invalidLatitude_throwsIllegalArgumentException() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedAnchorPose = Pose()
            advanceUntilIdle()

            val latitude = 99999.0
            val longitude = 20.0
            val altitude = 30.0
            val eastUpSouthQuaternion: Quaternion = Quaternion.Identity

            assertThat(latitude in arCoreTestRule.geospatial.allowedAnchorLatitudeRange).isFalse()

            assertFailsWith<IllegalArgumentException> {
                underTest.createAnchorOnSurface(
                    latitude,
                    longitude,
                    altitude,
                    eastUpSouthQuaternion,
                    GeospatialSurface.TERRAIN,
                )
            }
        }

    @Test
    fun checkVpsAvailability_vpsUnavailable_returnsVpsAvailabilityUnavailable() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedVpsResult = VpsAvailabilityUnavailable()
            advanceUntilIdle()

            val result = underTest.checkVpsAvailability(10.0, 20.0)
            assertThat(result).isInstanceOf(VpsAvailabilityUnavailable::class.java)
        }

    @Test
    fun checkVpsAvailability_vpsAvailable_returnsVpsAvailabilityAvailable() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedVpsResult = VpsAvailabilityAvailable()
            advanceUntilIdle()

            val result = underTest.checkVpsAvailability(10.0, 20.0)

            assertThat(result).isInstanceOf(VpsAvailabilityAvailable::class.java)
        }

    @Test
    fun checkVpsAvailability_errorInternal_returnsVpsAvailabilityErrorInternal() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedVpsResult = VpsAvailabilityErrorInternal()
            advanceUntilIdle()

            val result = underTest.checkVpsAvailability(10.0, 20.0)
            assertThat(result).isInstanceOf(VpsAvailabilityErrorInternal::class.java)
        }

    @Test
    fun checkVpsAvailability_networkError_returnsVpsAvailabilityNetworkError() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedVpsResult = VpsAvailabilityNetworkError()
            advanceUntilIdle()

            val result = underTest.checkVpsAvailability(10.0, 20.0)
            assertThat(result).isInstanceOf(VpsAvailabilityNetworkError::class.java)
        }

    @Test
    fun checkVpsAvailability_notAuthorized_returnsVpsAvailabilityNotAuthorized() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedVpsResult = VpsAvailabilityNotAuthorized()
            advanceUntilIdle()

            val result = underTest.checkVpsAvailability(10.0, 20.0)
            assertThat(result).isInstanceOf(VpsAvailabilityNotAuthorized::class.java)
        }

    @Test
    fun checkVpsAvailability_resourcesExhausted_returnsVpsAvailabilityResourceExhausted() =
        runTest(testDispatcher) {
            val underTest = Geospatial.getInstance(session)
            arCoreTestRule.geospatial.state = GeospatialState.RUNNING
            arCoreTestRule.geospatial.expectedVpsResult = VpsAvailabilityResourceExhausted()
            advanceUntilIdle()

            val result = underTest.checkVpsAvailability(10.0, 20.0)
            assertThat(result).isInstanceOf(VpsAvailabilityResourceExhausted::class.java)
        }
}
