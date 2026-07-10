/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.testing.internal

import androidx.kruth.assertThat
import androidx.xr.arcore.runtime.AnchorNotTrackingException
import androidx.xr.arcore.runtime.Geospatial
import androidx.xr.arcore.runtime.GeospatialPoseNotTrackingException
import androidx.xr.arcore.runtime.VpsAvailabilityAvailable
import androidx.xr.arcore.runtime.VpsAvailabilityErrorInternal
import androidx.xr.arcore.runtime.VpsAvailabilityNetworkError
import androidx.xr.arcore.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.arcore.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.arcore.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimeGeospatialTest {
    private lateinit var underTest: FakeRuntimeGeospatial
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        underTest = FakeRuntimeGeospatial()
    }

    @Test
    fun createPoseFromGeospatialPose_returnsExpectedPose() {
        underTest.state = Geospatial.State.RUNNING
        val expectedPose = Pose(Vector3(1f, 2f, 3f))
        underTest.expectedPose = expectedPose

        val pose = underTest.createPoseFromGeospatialPose(GeospatialPose())

        assertThat(pose).isEqualTo(expectedPose)
    }

    @Test
    fun createPoseFromGeospatialPose_notRunning_throwsGeospatialPoseNotTrackingException() {
        underTest.state = Geospatial.State.NOT_RUNNING
        val expectedPose = Pose(Vector3(1f, 2f, 3f))
        underTest.expectedPose = expectedPose

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createPoseFromGeospatialPose(GeospatialPose())
        }
    }

    @Test
    fun createGeospatialPoseFromPose_returnsExpectedGeospatialPoseResult() {
        underTest.state = Geospatial.State.RUNNING
        val expectedGeospatialPose = GeospatialPose(10.0, 20.0, 30.0, Quaternion.Identity)
        underTest.expectedGeospatialPose = expectedGeospatialPose
        underTest.expectedHorizontalAccuracy = 1.0
        underTest.expectedVerticalAccuracy = 2.0
        underTest.expectedOrientationYawAccuracy = 3.0

        val poseResult = underTest.createGeospatialPoseFromPose(Pose())

        assertThat(poseResult.geospatialPose).isEqualTo(expectedGeospatialPose)
        assertThat(poseResult.horizontalAccuracy).isEqualTo(underTest.expectedHorizontalAccuracy)
        assertThat(poseResult.verticalAccuracy).isEqualTo(underTest.expectedVerticalAccuracy)
        assertThat(poseResult.orientationYawAccuracy)
            .isEqualTo(underTest.expectedOrientationYawAccuracy)
    }

    @Test
    fun createGeospatialPoseFromPose_notRunning_throwsGeospatialPoseNotTrackingException() {
        underTest.state = Geospatial.State.NOT_RUNNING
        val expectedGeospatialPose = GeospatialPose(10.0, 20.0, 30.0, Quaternion.Identity)
        underTest.expectedGeospatialPose = expectedGeospatialPose
        underTest.expectedHorizontalAccuracy = 1.0
        underTest.expectedVerticalAccuracy = 2.0
        underTest.expectedOrientationYawAccuracy = 3.0

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createGeospatialPoseFromPose(Pose())
        }
    }

    @Test
    fun createAnchor_outOfRange_throwsIllegalArgumentException() {
        underTest.state = Geospatial.State.RUNNING

        assertFailsWith<IllegalArgumentException> {
            underTest.createAnchor(-100.0, 0.0, 0.0, Quaternion.Identity)
        }
    }

    @Test
    fun createAnchor_notRunning_throwsAnchorNotTrackingException() {
        underTest.state = Geospatial.State.NOT_RUNNING
        val expectedPose = Pose.Identity
        underTest.expectedAnchorPose = expectedPose

        assertFailsWith<AnchorNotTrackingException> {
            underTest.createAnchor(0.0, 0.0, 0.0, Quaternion.Identity)
        }
    }

    @Test
    fun createAnchor_returnsAnchor_withExpectedPose() {
        underTest.state = Geospatial.State.RUNNING
        val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
        underTest.expectedAnchorPose = expectedPose

        val anchor = underTest.createAnchor(0.0, 0.0, 0.0, Quaternion.Identity)
        assertThat(anchor.pose).isEqualTo(expectedPose)
    }

    @Test
    fun createAnchorOnSurface_outOfRange_throwsIllegalArgumentException() =
        runTest(testDispatcher) {
            underTest.state = Geospatial.State.RUNNING

            assertFailsWith<IllegalArgumentException> {
                underTest.createAnchorOnSurface(
                    -100.0,
                    0.0,
                    0.0,
                    Quaternion.Identity,
                    Geospatial.Surface.TERRAIN,
                )
            }
        }

    @Test
    fun createAnchorOnSurface_notRunning_throwsAnchorNotTrackingException() =
        runTest(testDispatcher) {
            underTest.state = Geospatial.State.NOT_RUNNING
            val expectedPose = Pose.Identity
            underTest.expectedAnchorPose = expectedPose

            assertFailsWith<AnchorNotTrackingException> {
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion.Identity,
                    Geospatial.Surface.TERRAIN,
                )
            }
        }

    @Test
    fun createAnchorOnSurface_belowSurface_throwsIllegalArgumentException() =
        runTest(testDispatcher) {
            underTest.state = Geospatial.State.RUNNING

            assertFailsWith<IllegalArgumentException> {
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    -1.0,
                    Quaternion.Identity,
                    Geospatial.Surface.TERRAIN,
                )
            }
        }

    @Test
    fun createAnchorOnSurface_returnsAnchor_withExpectedPose() =
        runTest(testDispatcher) {
            underTest.state = Geospatial.State.RUNNING
            val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(1.0f, 2.0f, 3.0f, 4.0f))
            underTest.expectedAnchorPose = expectedPose

            val anchor =
                underTest.createAnchorOnSurface(
                    0.0,
                    0.0,
                    0.0,
                    Quaternion.Identity,
                    Geospatial.Surface.TERRAIN,
                )
            assertThat(anchor.pose).isEqualTo(expectedPose)
        }

    @Test
    fun checkVpsAvailability_returnsExpectedResult() =
        runTest(testDispatcher) {
            val coordinates = Pair(10.0, 20.0)
            underTest.state = Geospatial.State.RUNNING

            underTest.expectedVpsAvailabilityResult = VpsAvailabilityNotAuthorized()
            var result = underTest.checkVpsAvailability(coordinates.first, coordinates.second)
            assertThat(result).isInstanceOf<VpsAvailabilityNotAuthorized>()

            underTest.expectedVpsAvailabilityResult = VpsAvailabilityNetworkError()
            result = underTest.checkVpsAvailability(coordinates.first, coordinates.second)
            assertThat(result).isInstanceOf<VpsAvailabilityNetworkError>()

            underTest.expectedVpsAvailabilityResult = VpsAvailabilityErrorInternal()
            result = underTest.checkVpsAvailability(coordinates.first, coordinates.second)
            assertThat(result).isInstanceOf<VpsAvailabilityErrorInternal>()

            underTest.expectedVpsAvailabilityResult = VpsAvailabilityResourceExhausted()
            result = underTest.checkVpsAvailability(coordinates.first, coordinates.second)
            assertThat(result).isInstanceOf<VpsAvailabilityResourceExhausted>()

            underTest.expectedVpsAvailabilityResult = VpsAvailabilityAvailable()
            result = underTest.checkVpsAvailability(coordinates.first, coordinates.second)
            assertThat(result).isInstanceOf<VpsAvailabilityAvailable>()

            underTest.expectedVpsAvailabilityResult = VpsAvailabilityUnavailable()
            result = underTest.checkVpsAvailability(coordinates.first, coordinates.second)
            assertThat(result).isInstanceOf<VpsAvailabilityUnavailable>()
        }
}
