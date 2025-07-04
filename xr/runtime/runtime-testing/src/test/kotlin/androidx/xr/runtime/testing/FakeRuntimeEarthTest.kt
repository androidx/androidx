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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.AnchorResourcesExhaustedException
import androidx.xr.runtime.internal.Earth as RuntimeEarth
import androidx.xr.runtime.internal.GeospatialPoseNotTrackingException
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimeEarthTest {

    private lateinit var underTest: FakeRuntimeEarth

    private fun doBlocking(block: suspend CoroutineScope.() -> Unit) {
        runBlocking(block = block)
    }

    @Before
    fun setUp() {
        underTest = FakeRuntimeEarth()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createPoseFromGeospatialPose_withNextPose_returnsPoseAndClearsIt() {
        underTest.nextPose = POSE

        val result = underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)

        assertThat(result).isEqualTo(POSE)
        assertThat(underTest.nextPose).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createPoseFromGeospatialPose_withNextException_throwsExceptionAndClearsIt() {
        underTest.nextException = EXCEPTION

        val thrown =
            assertFailsWith<GeospatialPoseNotTrackingException> {
                underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)
            }

        assertThat(thrown).isEqualTo(EXCEPTION)
        assertThat(underTest.nextException).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createGeospatialPoseFromPose_withNextResult_returnsResultAndClearsIt() {
        underTest.nextGeospatialPoseResult = GEOSPATIAL_POSE_RESULT

        val result = underTest.createGeospatialPoseFromPose(POSE)

        assertThat(result).isEqualTo(GEOSPATIAL_POSE_RESULT)
        assertThat(underTest.nextGeospatialPoseResult).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createGeospatialPoseFromPose_withNextException_throwsExceptionAndClearsIt() {
        underTest.nextException = EXCEPTION

        val thrown =
            assertFailsWith<GeospatialPoseNotTrackingException> {
                underTest.createGeospatialPoseFromPose(POSE)
            }

        assertThat(thrown).isEqualTo(EXCEPTION)
        assertThat(underTest.nextException).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createGeospatialPoseFromDevicePose_withNextResult_returnsResultAndClearsIt() {
        underTest.nextGeospatialPoseResult = GEOSPATIAL_POSE_RESULT

        val result = underTest.createGeospatialPoseFromDevicePose()

        assertThat(result).isEqualTo(GEOSPATIAL_POSE_RESULT)
        assertThat(underTest.nextGeospatialPoseResult).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createGeospatialPoseFromDevicePose_withNextException_throwsExceptionAndClearsIt() {
        underTest.nextException = EXCEPTION

        val thrown =
            assertFailsWith<GeospatialPoseNotTrackingException> {
                underTest.createGeospatialPoseFromDevicePose()
            }

        assertThat(thrown).isEqualTo(EXCEPTION)
        assertThat(underTest.nextException).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createAnchor_withNextAnchor_returnsAnchorAndClearsIt() {
        underTest.nextAnchor = ANCHOR

        val result =
            underTest.createAnchor(
                GEOSPATIAL_POSE.latitude,
                GEOSPATIAL_POSE.longitude,
                GEOSPATIAL_POSE.altitude,
                GEOSPATIAL_POSE.eastUpSouthQuaternion,
            )

        assertThat(result).isEqualTo(ANCHOR)
        assertThat(underTest.nextAnchor).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createAnchor_withNextException_throwsExceptionAndClearsIt() {
        underTest.nextException = ANCHOR_EXCEPTION

        val thrown =
            assertFailsWith<AnchorResourcesExhaustedException> {
                underTest.createAnchor(
                    GEOSPATIAL_POSE.latitude,
                    GEOSPATIAL_POSE.longitude,
                    GEOSPATIAL_POSE.altitude,
                    GEOSPATIAL_POSE.eastUpSouthQuaternion,
                )
            }

        assertThat(thrown).isEqualTo(ANCHOR_EXCEPTION)
        assertThat(underTest.nextException).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createSurfaceAnchor_withNextAnchor_returnsAnchorAndClearsIt() = doBlocking {
        underTest.nextAnchor = ANCHOR

        val result =
            underTest.createAnchorOnSurface(
                GEOSPATIAL_POSE.latitude,
                GEOSPATIAL_POSE.longitude,
                GEOSPATIAL_POSE.altitude,
                GEOSPATIAL_POSE.eastUpSouthQuaternion,
                RuntimeEarth.Surface.TERRAIN,
            )

        assertThat(result).isEqualTo(ANCHOR)
        assertThat(underTest.nextAnchor).isNull()
    }

    @Ignore("Test fails with AnchorResourcesExhaustedException when run with presubmits")
    @Test
    fun createSurfaceAnchor_withNextException_throwsExceptionAndClearsIt() = doBlocking {
        underTest.nextException = ANCHOR_EXCEPTION

        val thrown =
            assertFailsWith<AnchorResourcesExhaustedException> {
                underTest.createAnchorOnSurface(
                    GEOSPATIAL_POSE.latitude,
                    GEOSPATIAL_POSE.longitude,
                    GEOSPATIAL_POSE.altitude,
                    GEOSPATIAL_POSE.eastUpSouthQuaternion,
                    RuntimeEarth.Surface.TERRAIN,
                )
            }

        assertThat(thrown).isEqualTo(ANCHOR_EXCEPTION)
        assertThat(underTest.nextException).isNull()
    }

    private companion object {
        val POSE = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val GEOSPATIAL_POSE = GeospatialPose(1.0, 2.0, 3.0, Quaternion(8f, 9f, 10f, 11f))
        val GEOSPATIAL_POSE_RESULT =
            RuntimeEarth.GeospatialPoseResult(
                GEOSPATIAL_POSE,
                horizontalAccuracy = 10.0,
                verticalAccuracy = 20.0,
                orientationYawAccuracy = 30.0,
            )
        val EXCEPTION = GeospatialPoseNotTrackingException()
        val ANCHOR = FakeRuntimeAnchor(POSE)
        val ANCHOR_EXCEPTION = AnchorResourcesExhaustedException()
    }
}
