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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.internal.AnchorNotAuthorizedException
import androidx.xr.runtime.internal.AnchorUnsupportedLocationException
import androidx.xr.runtime.internal.Earth
import androidx.xr.runtime.internal.GeospatialPoseNotTrackingException
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.ar.core.Anchor as ARCore1xAnchor
import com.google.ar.core.Anchor.RooftopAnchorState as ARCore1xRooftopAnchorState
import com.google.ar.core.Anchor.TerrainAnchorState as ARCore1xTerrainAnchorState
import com.google.ar.core.Earth as ARCore1xEarth
import com.google.ar.core.FutureState
import com.google.ar.core.GeospatialPose as ARCore1xGeospatialPose
import com.google.ar.core.ResolveAnchorOnRooftopFuture as ARCore1xRooftopAnchorFuture
import com.google.ar.core.ResolveAnchorOnTerrainFuture as ARCore1xTerrainAnchorFuture
import com.google.ar.core.Session
import com.google.ar.core.TrackingState as ARCore1xTrackingState
import com.google.ar.core.exceptions.NotTrackingException
import com.google.common.truth.Truth.assertThat
import java.util.function.BiConsumer
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreEarthTest {
    private val testDispatcher = StandardTestDispatcher()

    private var mockArCoreEarth: ARCore1xEarth = mock<ARCore1xEarth>()
    private val mockSession = mock<Session> { on { earth } doReturn mockArCoreEarth }
    private val mockArCoreGeospatialPose =
        mock<ARCore1xGeospatialPose> {
            on { latitude } doReturn LATITUDE
            on { longitude } doReturn LONGITUDE
            on { altitude } doReturn ALTITUDE
            on { eastUpSouthQuaternion } doReturn
                floatArrayOf(QUATERNION.x, QUATERNION.y, QUATERNION.z, QUATERNION.w)
            on { horizontalAccuracy } doReturn HORIZONTAL_ACCURACY
            on { verticalAccuracy } doReturn VERTICAL_ACCURACY
            on { orientationYawAccuracy } doReturn ORIENTATION_YAW_ACCURACY
        }
    private val underTest: ArCoreEarth = ArCoreEarth(XrResources())

    private val mockArCoreAnchor = mock<ARCore1xAnchor>()
    private val mockTerrainFuture = mock<ARCore1xTerrainAnchorFuture>()
    private val mockRooftopFuture = mock<ARCore1xRooftopAnchorFuture>()

    @Before
    fun setUp() {
        check(underTest.state == Earth.State.STOPPED) { "Earth should initially be stopped." }
    }

    private fun setupEarthTracking() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ENABLED)
        whenever(mockArCoreEarth.trackingState).thenReturn(ARCore1xTrackingState.TRACKING)
        underTest.update(mockSession)
        check(underTest.state == Earth.State.RUNNING)
    }

    // --- State Tests ---

    @Test
    fun update_whenArCoreEarthIsNull_setsStateToStopped() {
        whenever(mockSession.earth).thenReturn(null)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.STOPPED)
    }

    @Test
    fun update_whenEarthStateEnabled_setsStateToRunning() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ENABLED)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.RUNNING)
    }

    @Test
    fun update_whenEarthStateErrorApkVersionTooOld_setsStateToErrorApkVersionTooOld() {
        whenever(mockArCoreEarth.earthState)
            .thenReturn(ARCore1xEarth.EarthState.ERROR_APK_VERSION_TOO_OLD)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.ERROR_APK_VERSION_TOO_OLD)
    }

    @Test
    fun update_whenEarthStateErrorGeospatialModeDisabled_setsStateToStoppedAndNullsArCoreEarth() {
        whenever(mockArCoreEarth.earthState)
            .thenReturn(ARCore1xEarth.EarthState.ERROR_GEOSPATIAL_MODE_DISABLED)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.STOPPED)
        assertThat(underTest.arCoreEarth).isNull()
    }

    @Test
    fun update_whenEarthStateErrorInternal_setsStateToErrorInternal() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ERROR_INTERNAL)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.ERROR_INTERNAL)
    }

    @Test
    fun update_whenEarthStateErrorNotAuthorized_setsStateToErrorNotAuthorized() {
        whenever(mockArCoreEarth.earthState)
            .thenReturn(ARCore1xEarth.EarthState.ERROR_NOT_AUTHORIZED)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.ERROR_NOT_AUTHORIZED)
    }

    @Test
    fun update_whenEarthStateErrorResourceExhausted_setsStateToErrorResourceExhausted() {
        whenever(mockArCoreEarth.earthState)
            .thenReturn(ARCore1xEarth.EarthState.ERROR_RESOURCE_EXHAUSTED)

        underTest.update(mockSession)

        assertThat(underTest.state).isEqualTo(Earth.State.ERROR_RESOURCES_EXHAUSTED)
    }

    // --- createPoseFromGeospatialPose Tests ---

    @Test
    fun createPoseFromGeospatialPose_whenTracking_returnsPose() {
        setupEarthTracking()
        whenever(
                mockArCoreEarth.getPose(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE,
                    QUATERNION.x,
                    QUATERNION.y,
                    QUATERNION.z,
                    QUATERNION.w,
                )
            )
            .thenReturn(POSE.toARCorePose())

        val resultPose = underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)

        assertThat(resultPose).isEqualTo(POSE)
        verify(mockArCoreEarth)
            .getPose(
                LATITUDE,
                LONGITUDE,
                ALTITUDE,
                QUATERNION.x,
                QUATERNION.y,
                QUATERNION.z,
                QUATERNION.w,
            )
    }

    @Test
    fun createPoseFromGeospatialPose_whenNotEnabled_throwsIllegalStateException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ERROR_INTERNAL)

        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> {
            underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)
        }
        verify(mockArCoreEarth, never()).getPose(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun createPoseFromGeospatialPose_whenArCoreEarthNull_throwsIllegalStateException() {
        whenever(mockSession.earth).thenReturn(null)

        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> {
            underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)
        }
    }

    @Test
    fun createPoseFromGeospatialPose_whenNotTracking_throwsGeospatialPoseNotTrackingException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ENABLED)
        whenever(mockArCoreEarth.trackingState).thenReturn(ARCore1xTrackingState.PAUSED)

        underTest.update(mockSession)

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)
        }
        verify(mockArCoreEarth, never()).getPose(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun createPoseFromGeospatialPose_whenGetPoseThrowsNotTracking_throwsGeospatialPoseNotTrackingException() {
        setupEarthTracking()
        whenever(
                mockArCoreEarth.getPose(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE,
                    QUATERNION.x,
                    QUATERNION.y,
                    QUATERNION.z,
                    QUATERNION.w,
                )
            )
            .doThrow(NotTrackingException())

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createPoseFromGeospatialPose(GEOSPATIAL_POSE)
        }
    }

    // --- createGeospatialPoseFromPose Tests ---

    @Test
    fun createGeospatialPoseFromPose_whenTracking_returnsResult() {
        setupEarthTracking()
        // ARCore Pose doesn't override equals, so we have to use any().
        whenever(mockArCoreEarth.getGeospatialPose(any())).thenReturn(mockArCoreGeospatialPose)

        val result = underTest.createGeospatialPoseFromPose(POSE)

        assertThat(result.geospatialPose).isEqualTo(GEOSPATIAL_POSE)
        assertThat(result.horizontalAccuracy).isEqualTo(HORIZONTAL_ACCURACY)
        assertThat(result.verticalAccuracy).isEqualTo(VERTICAL_ACCURACY)
        assertThat(result.orientationYawAccuracy).isEqualTo(ORIENTATION_YAW_ACCURACY)
    }

    @Test
    fun createGeospatialPoseFromPose_whenNotEnabled_throwsIllegalStateException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ERROR_INTERNAL)

        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> { underTest.createGeospatialPoseFromPose(POSE) }
        verify(mockArCoreEarth, never()).getGeospatialPose(any())
    }

    @Test
    fun createGeospatialPoseFromPose_whenArCoreEarthNull_throwsIllegalStateException() {
        whenever(mockSession.earth).thenReturn(null)

        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> { underTest.createGeospatialPoseFromPose(POSE) }
    }

    @Test
    fun createGeospatialPoseFromPose_whenNotTracking_throwsGeospatialPoseNotTrackingException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ENABLED)
        whenever(mockArCoreEarth.trackingState).thenReturn(ARCore1xTrackingState.PAUSED)

        underTest.update(mockSession)

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createGeospatialPoseFromPose(POSE)
        }
        verify(mockArCoreEarth, never()).getGeospatialPose(any())
    }

    @Test
    fun createGeospatialPoseFromPose_whenGetGeospatialPoseThrowsNotTracking_throwsGeospatialPoseNotTrackingException() {
        setupEarthTracking()
        // ARCore Pose doesn't override equals, so we have to use any().
        whenever(mockArCoreEarth.getGeospatialPose(any())).doThrow(NotTrackingException())

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createGeospatialPoseFromPose(POSE)
        }
    }

    // --- createGeospatialPoseFromDevicePose Tests ---

    @Test
    fun createGeospatialPoseFromDevicePose_whenTracking_returnsResult() {
        setupEarthTracking()
        whenever(mockArCoreEarth.cameraGeospatialPose).thenReturn(mockArCoreGeospatialPose)

        val result = underTest.createGeospatialPoseFromDevicePose()

        assertThat(result.geospatialPose).isEqualTo(GEOSPATIAL_POSE)
        assertThat(result.horizontalAccuracy).isEqualTo(HORIZONTAL_ACCURACY)
        assertThat(result.verticalAccuracy).isEqualTo(VERTICAL_ACCURACY)
        assertThat(result.orientationYawAccuracy).isEqualTo(ORIENTATION_YAW_ACCURACY)
        verify(mockArCoreEarth).cameraGeospatialPose
    }

    @Test
    fun createGeospatialPoseFromDevicePose_whenNotEnabled_throwsIllegalStateException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ERROR_INTERNAL)

        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> { underTest.createGeospatialPoseFromDevicePose() }
        verify(mockArCoreEarth, never()).cameraGeospatialPose
    }

    @Test
    fun createGeospatialPoseFromDevicePose_whenArCoreEarthNull_throwsIllegalStateException() {
        whenever(mockSession.earth).thenReturn(null)

        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> { underTest.createGeospatialPoseFromDevicePose() }
    }

    @Test
    fun createGeospatialPoseFromDevicePose_whenNotTracking_throwsGeospatialPoseNotTrackingException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ENABLED)
        whenever(mockArCoreEarth.trackingState).thenReturn(ARCore1xTrackingState.PAUSED)

        underTest.update(mockSession)

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createGeospatialPoseFromDevicePose()
        }
        verify(mockArCoreEarth, never()).cameraGeospatialPose
    }

    @Test
    fun createGeospatialPoseFromDevicePose_whenCameraGeospatialPoseThrowsNotTracking_throwsGeospatialPoseNotTrackingException() {
        setupEarthTracking()
        whenever(mockArCoreEarth.cameraGeospatialPose).doThrow(NotTrackingException())

        assertFailsWith<GeospatialPoseNotTrackingException> {
            underTest.createGeospatialPoseFromDevicePose()
        }
    }

    // --- createAnchor Tests ---

    @Test
    fun createAnchor_whenTracking_returnsAnchor() {
        setupEarthTracking()
        val mockArCoreAnchor = mock<ARCore1xAnchor>()
        whenever(mockArCoreAnchor.pose).thenReturn(POSE.toARCorePose())
        whenever(
                mockArCoreEarth.createAnchor(
                    eq(LATITUDE),
                    eq(LONGITUDE),
                    eq(ALTITUDE),
                    eq(QUATERNION.x),
                    eq(QUATERNION.y),
                    eq(QUATERNION.z),
                    eq(QUATERNION.w),
                )
            )
            .thenReturn(mockArCoreAnchor)

        val resultAnchor = underTest.createAnchor(LATITUDE, LONGITUDE, ALTITUDE, QUATERNION)

        assertIs<ArCoreAnchor>(resultAnchor)
        assertThat(resultAnchor.pose).isEqualTo(mockArCoreAnchor.pose.toRuntimePose())
        verify(mockArCoreEarth)
            .createAnchor(
                LATITUDE,
                LONGITUDE,
                ALTITUDE,
                QUATERNION.x,
                QUATERNION.y,
                QUATERNION.z,
                QUATERNION.w,
            )
    }

    @Test
    fun createAnchor_whenNotEnabled_throwsIllegalStateException() {
        whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ERROR_INTERNAL)
        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> {
            underTest.createAnchor(LATITUDE, LONGITUDE, ALTITUDE, QUATERNION)
        }
        verify(mockArCoreEarth, never())
            .createAnchor(any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun createAnchor_whenArCoreEarthNull_throwsIllegalStateException() {
        whenever(mockSession.earth).thenReturn(null)
        underTest.update(mockSession)

        assertFailsWith<IllegalStateException> {
            underTest.createAnchor(LATITUDE, LONGITUDE, ALTITUDE, QUATERNION)
        }
    }

    // --- createAnchorOnSurface Tests ---

    @Test
    fun createAnchorOnSurface_terrainSuccess_returnsAnchor(): Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(mockArCoreAnchor.pose).thenReturn(POSE.toARCorePose())
            whenever(
                    mockArCoreEarth.resolveAnchorOnTerrainAsync(
                        eq(LATITUDE),
                        eq(LONGITUDE),
                        eq(ALTITUDE_ABOVE_SURFACE),
                        eq(QUATERNION.x),
                        eq(QUATERNION.y),
                        eq(QUATERNION.z),
                        eq(QUATERNION.w),
                        any(),
                    )
                )
                .thenAnswer { invocation ->
                    val callback =
                        invocation.getArgument<
                            BiConsumer<ARCore1xAnchor, ARCore1xTerrainAnchorState>
                        >(
                            7
                        )
                    callback.accept(mockArCoreAnchor, ARCore1xTerrainAnchorState.SUCCESS)
                    mockTerrainFuture
                }

            val resultAnchor =
                underTest.createAnchorOnSurface(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE_ABOVE_SURFACE,
                    QUATERNION,
                    Earth.Surface.TERRAIN,
                )

            assertIs<ArCoreAnchor>(resultAnchor)
            assertThat(resultAnchor.pose).isEqualTo(mockArCoreAnchor.pose.toRuntimePose())
            verify(mockArCoreEarth)
                .resolveAnchorOnTerrainAsync(any(), any(), any(), any(), any(), any(), any(), any())
        }

    @Test
    fun createAnchorOnSurface_rooftopSuccess_returnsAnchor(): Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(mockArCoreAnchor.pose).thenReturn(POSE.toARCorePose())
            whenever(
                    mockArCoreEarth.resolveAnchorOnRooftopAsync(
                        eq(LATITUDE),
                        eq(LONGITUDE),
                        eq(ALTITUDE_ABOVE_SURFACE),
                        eq(QUATERNION.x),
                        eq(QUATERNION.y),
                        eq(QUATERNION.z),
                        eq(QUATERNION.w),
                        any(),
                    )
                )
                .thenAnswer { invocation ->
                    val callback =
                        invocation.getArgument<
                            BiConsumer<ARCore1xAnchor, ARCore1xRooftopAnchorState>
                        >(
                            7
                        )
                    callback.accept(mockArCoreAnchor, ARCore1xRooftopAnchorState.SUCCESS)
                    mockRooftopFuture
                }

            val resultAnchor =
                underTest.createAnchorOnSurface(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE_ABOVE_SURFACE,
                    QUATERNION,
                    Earth.Surface.ROOFTOP,
                )

            assertIs<ArCoreAnchor>(resultAnchor)
            assertThat(resultAnchor.pose).isEqualTo(mockArCoreAnchor.pose.toRuntimePose())
            verify(mockArCoreEarth)
                .resolveAnchorOnRooftopAsync(any(), any(), any(), any(), any(), any(), any(), any())
        }

    @Test
    fun createAnchorOnSurface_terrainErrorNotAuthorized_throwsAnchorNotAuthorizedException(): Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(
                    mockArCoreEarth.resolveAnchorOnTerrainAsync(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                )
                .thenAnswer { invocation ->
                    val callback =
                        invocation.getArgument<
                            BiConsumer<ARCore1xAnchor, ARCore1xTerrainAnchorState>
                        >(
                            7
                        )
                    callback.accept(
                        mockArCoreAnchor,
                        ARCore1xTerrainAnchorState.ERROR_NOT_AUTHORIZED,
                    )
                    mockTerrainFuture
                }

            assertFailsWith<AnchorNotAuthorizedException> {
                underTest.createAnchorOnSurface(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE_ABOVE_SURFACE,
                    QUATERNION,
                    Earth.Surface.TERRAIN,
                )
            }
            verify(mockArCoreEarth)
                .resolveAnchorOnTerrainAsync(any(), any(), any(), any(), any(), any(), any(), any())
        }

    @Test
    fun createAnchorOnSurface_terrainErrorUnsupportedLocation_throwsAnchorUnsupportedLocationException():
        Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(
                    mockArCoreEarth.resolveAnchorOnTerrainAsync(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                )
                .thenAnswer { invocation ->
                    val callback =
                        invocation.getArgument<
                            BiConsumer<ARCore1xAnchor, ARCore1xTerrainAnchorState>
                        >(
                            7
                        )
                    callback.accept(
                        mockArCoreAnchor,
                        ARCore1xTerrainAnchorState.ERROR_UNSUPPORTED_LOCATION,
                    )
                    mockTerrainFuture
                }

            assertFailsWith<AnchorUnsupportedLocationException> {
                underTest.createAnchorOnSurface(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE_ABOVE_SURFACE,
                    QUATERNION,
                    Earth.Surface.TERRAIN,
                )
            }
            verify(mockArCoreEarth)
                .resolveAnchorOnTerrainAsync(any(), any(), any(), any(), any(), any(), any(), any())
        }

    @Test
    fun createAnchorOnSurface_terrainErrorInternal_throwsIllegalStateException(): Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(
                    mockArCoreEarth.resolveAnchorOnTerrainAsync(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                )
                .thenAnswer { invocation ->
                    val callback =
                        invocation.getArgument<
                            BiConsumer<ARCore1xAnchor, ARCore1xTerrainAnchorState>
                        >(
                            7
                        )
                    callback.accept(mockArCoreAnchor, ARCore1xTerrainAnchorState.ERROR_INTERNAL)
                    mockTerrainFuture
                }

            assertFailsWith<IllegalStateException> {
                underTest.createAnchorOnSurface(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE_ABOVE_SURFACE,
                    QUATERNION,
                    Earth.Surface.TERRAIN,
                )
            }
            verify(mockArCoreEarth)
                .resolveAnchorOnTerrainAsync(any(), any(), any(), any(), any(), any(), any(), any())
        }

    @Test
    fun createAnchorOnSurface_whenNotEnabled_throwsIllegalStateException(): Unit =
        runBlocking(testDispatcher.scheduler) {
            whenever(mockArCoreEarth.earthState).thenReturn(ARCore1xEarth.EarthState.ERROR_INTERNAL)
            underTest.update(mockSession)

            assertFailsWith<IllegalStateException> {
                underTest.createAnchorOnSurface(
                    LATITUDE,
                    LONGITUDE,
                    ALTITUDE_ABOVE_SURFACE,
                    QUATERNION,
                    Earth.Surface.TERRAIN,
                )
            }
            verify(mockArCoreEarth, never())
                .resolveAnchorOnTerrainAsync(any(), any(), any(), any(), any(), any(), any(), any())
        }

    @Test
    fun createAnchorOnSurface_cancellationBeforeCompletion_callsCancelAndNotDetach(): Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(mockTerrainFuture.cancel()).thenReturn(true)
            whenever(
                    mockArCoreEarth.resolveAnchorOnTerrainAsync(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                )
                .thenReturn(mockTerrainFuture)

            val job =
                launch(testDispatcher) {
                    underTest.createAnchorOnSurface(
                        LATITUDE,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        QUATERNION,
                        Earth.Surface.TERRAIN,
                    )
                }
            testDispatcher.scheduler.runCurrent() // Set the future in the coroutine
            job.cancel()
            testDispatcher.scheduler.runCurrent() // Future handles the cancellation
            job.join()

            verify(mockTerrainFuture).cancel()
            verify(mockTerrainFuture, never()).resultAnchor
            verify(mockArCoreAnchor, never()).detach()
        }

    @Test
    fun createAnchorOnSurface_cancellationAfterCompletion_callsCancelAndDetach(): Unit =
        runBlocking(testDispatcher.scheduler) {
            setupEarthTracking()
            whenever(mockTerrainFuture.cancel())
                .thenReturn(false) // Simulate failed cancellation (already complete)
            whenever(mockTerrainFuture.resultAnchor).thenReturn(mockArCoreAnchor)
            whenever(mockTerrainFuture.state).thenReturn(FutureState.DONE)
            whenever(
                    mockArCoreEarth.resolveAnchorOnTerrainAsync(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                )
                .thenReturn(mockTerrainFuture)

            val job =
                launch(testDispatcher) {
                    underTest.createAnchorOnSurface(
                        LATITUDE,
                        LONGITUDE,
                        ALTITUDE_ABOVE_SURFACE,
                        QUATERNION,
                        Earth.Surface.TERRAIN,
                    )
                }
            testDispatcher.scheduler.runCurrent() // Set the future in the coroutine
            job.cancel()
            testDispatcher.scheduler.runCurrent() // Future handles the cancellation
            job.join()

            verify(mockTerrainFuture).cancel()
            verify(mockTerrainFuture).resultAnchor
            verify(mockArCoreAnchor).detach()
        }

    private companion object {
        const val LATITUDE = 37.4220
        const val LONGITUDE = -122.0841
        const val ALTITUDE = 10.0
        val QUATERNION = Quaternion(0f, 0f, 0f, 1f)
        val GEOSPATIAL_POSE = GeospatialPose(LATITUDE, LONGITUDE, ALTITUDE, QUATERNION)
        val POSE = Pose(Vector3(1f, 2f, 3f), QUATERNION)
        val ARCORE_POSE = POSE.toARCorePose()
        const val HORIZONTAL_ACCURACY = 1.0
        const val VERTICAL_ACCURACY = 2.0
        const val ORIENTATION_YAW_ACCURACY = 3.0
        const val ALTITUDE_ABOVE_SURFACE = 5.0
    }
}
