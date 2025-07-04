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

import androidx.xr.runtime.VpsAvailabilityAvailable
import androidx.xr.runtime.VpsAvailabilityErrorInternal
import androidx.xr.runtime.VpsAvailabilityNetworkError
import androidx.xr.runtime.VpsAvailabilityNotAuthorized
import androidx.xr.runtime.VpsAvailabilityResourceExhausted
import androidx.xr.runtime.VpsAvailabilityResult
import androidx.xr.runtime.VpsAvailabilityUnavailable
import androidx.xr.runtime.internal.AnchorNotTrackingException
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.ar.core.Anchor as ARCoreAnchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane as ARCore1xPlane
import com.google.ar.core.Pose as ARCorePose
import com.google.ar.core.Session
import com.google.ar.core.VpsAvailability as ARCore1xVpsAvailability
import com.google.ar.core.VpsAvailabilityFuture
import com.google.ar.core.exceptions.NotTrackingException
import com.google.ar.core.exceptions.ResourceExhaustedException
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.util.UUID
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(TestParameterInjector::class)
class ArCorePerceptionManagerTest {

    lateinit var mockSession: Session
    lateinit var underTest: ArCorePerceptionManager

    @Before
    fun setUp() {
        mockSession = mock<Session>()

        val timeSource = ArCoreTimeSource()
        underTest = ArCorePerceptionManager(timeSource)

        underTest.session = mockSession
    }

    @After
    fun tearDown() {
        underTest.clear()
    }

    @Test
    fun createAnchor_returnsAnchorWithTheGivenPose() {
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion(0f, 0f, 0f, 1.0f))
        val mockARCoreAnchor = mock<ARCoreAnchor>()
        val mockFrame = mock<Frame>()
        whenever(mockARCoreAnchor.pose).thenReturn(pose.toARCorePose())
        doReturn(mockARCoreAnchor).whenever(mockSession).createAnchor(any())
        whenever(mockSession.update()).thenReturn(mockFrame)

        underTest.update()
        val anchor = underTest.createAnchor(pose)

        assertThat(anchor.pose).isEqualTo(pose)
        verify(mockARCoreAnchor).pose
        verify(mockSession).createAnchor(any())
        verify(mockSession).update()
    }

    @Test
    fun createAnchor_anchorLimitReached_throwsException() {
        val mockFrame = mock<Frame>()
        val mockARCoreAnchor = mock<ARCoreAnchor>()
        doReturn(mockARCoreAnchor)
            .doReturn(mockARCoreAnchor)
            .doReturn(mockARCoreAnchor)
            .doReturn(mockARCoreAnchor)
            .doReturn(mockARCoreAnchor)
            .doThrow(ResourceExhaustedException())
            .whenever(mockSession)
            .createAnchor(any())
        whenever(mockSession.update()).thenReturn(mockFrame)

        underTest.update()
        repeat(5) { underTest.createAnchor(Pose()) }

        assertThrows(ResourceExhaustedException::class.java) { underTest.createAnchor(Pose()) }
        verify(mockSession, times(6)).createAnchor(any())
    }

    @Test
    fun createAnchor_notTracking_throwsException() {
        whenever(mockSession.createAnchor(any())).doThrow(NotTrackingException())

        assertFailsWith<AnchorNotTrackingException> { underTest.createAnchor(Pose()) }
    }

    @Test
    fun clear_clearXrResources() {
        underTest.clear()

        assertThat(underTest.trackables).isEmpty()
    }

    @Test
    fun getPersistedAnchorUuids_throwsNotImplementedError() {
        assertFailsWith<NotImplementedError> { underTest.getPersistedAnchorUuids() }
    }

    @Test
    fun hitTest_returnsHitResults() {
        val mockFrame = mock<Frame>()
        val mockHitResult = mock<HitResult>()
        val mockPlane = mock<ARCore1xPlane>()
        val timestamp = 1000L
        whenever(mockHitResult.distance).thenReturn(5.0f)
        whenever(mockHitResult.hitPose)
            .thenReturn(
                ARCorePose(floatArrayOf(3.0f, 2.0f, 1.0f), floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f))
            )
        whenever(mockHitResult.trackable).thenReturn(mockPlane)
        whenever(mockFrame.getUpdatedTrackables(ARCore1xPlane::class.java))
            .thenReturn(listOf(mockPlane))
        whenever(mockFrame.hitTest(any(), eq(0), any(), eq(0))).thenReturn(listOf(mockHitResult))
        whenever(mockFrame.timestamp).thenReturn(timestamp)
        whenever(mockSession.update()).thenReturn(mockFrame)

        underTest.update()
        val hitResults = underTest.hitTest(Ray(Vector3(4f, 3f, 2f), Vector3(2f, 1f, 0f)))

        assertThat(hitResults).hasSize(1)
        assertThat(hitResults.first().hitPose)
            .isEqualTo(Pose(Vector3(3f, 2f, 1f), Quaternion(0f, 0f, 0f, 1.0f)))
        assertThat(hitResults.first().distance).isEqualTo(5f)
        verify(mockFrame).hitTest(any(), eq(0), any(), eq(0))
    }

    @Test
    fun loadAnchor_throwsNotImplementedError() {
        val uuid = UUID.randomUUID()

        assertFailsWith<NotImplementedError> { underTest.loadAnchor(uuid) }
    }

    @Test
    fun loadAnchorFromNativePointer_throwsNotImplementedError() {
        assertFailsWith<NotImplementedError> { underTest.loadAnchorFromNativePointer(0L) }
    }

    @Test
    fun unpersistAnchor_throwsNotImplementedError() {
        val uuid = UUID.randomUUID()

        assertFailsWith<NotImplementedError> { underTest.unpersistAnchor(uuid) }
    }

    @Test
    fun update_callsSessionUpdate() {
        val mockFrame = mock<Frame>()
        whenever(mockSession.update()).thenReturn(mockFrame)

        underTest.update()

        verify(mockSession).update()
    }

    @Test
    fun update_addsPlanesToTrackables() {
        val mockFrame = mock<Frame>()
        val mockPlane = mock<ARCore1xPlane>()
        val timestamp = 1000L
        whenever(mockFrame.getUpdatedTrackables(ARCore1xPlane::class.java))
            .thenReturn(listOf(mockPlane))
        whenever(mockFrame.timestamp).thenReturn(timestamp)
        whenever(mockSession.update()).thenReturn(mockFrame)

        underTest.update()

        assertThat(underTest.trackables).hasSize(1)
        assertIs<ArCorePlane>(underTest.trackables.first())
        verify(mockFrame).getUpdatedTrackables(ARCore1xPlane::class.java)
    }

    enum class VpsAvailabilityTestCase(
        val arCoreVpsAvailability: ARCore1xVpsAvailability,
        val expectedXrVpsAvailability: KClass<out VpsAvailabilityResult>,
    ) {
        AVAILABLE(ARCore1xVpsAvailability.AVAILABLE, VpsAvailabilityAvailable::class),
        ERROR_INTERNAL(ARCore1xVpsAvailability.ERROR_INTERNAL, VpsAvailabilityErrorInternal::class),
        ERROR_NETWORK_CONNECTION(
            ARCore1xVpsAvailability.ERROR_NETWORK_CONNECTION,
            VpsAvailabilityNetworkError::class,
        ),
        ERROR_NOT_AUTHORIZED(
            ARCore1xVpsAvailability.ERROR_NOT_AUTHORIZED,
            VpsAvailabilityNotAuthorized::class,
        ),
        ERROR_RESOURCE_EXHAUSTED(
            ARCore1xVpsAvailability.ERROR_RESOURCE_EXHAUSTED,
            VpsAvailabilityResourceExhausted::class,
        ),
        UNAVAILABLE(ARCore1xVpsAvailability.UNAVAILABLE, VpsAvailabilityUnavailable::class),
        UNKNOWN(ARCore1xVpsAvailability.UNKNOWN, VpsAvailabilityErrorInternal::class),
    }

    @Test
    fun checkVpsAvailability_returnsCorrectType(@TestParameter testCase: VpsAvailabilityTestCase) =
        runTest {
            val mockFuture = mock<VpsAvailabilityFuture>()
            whenever(
                    mockSession.checkVpsAvailabilityAsync(
                        any(),
                        any(),
                        any<Consumer<ARCore1xVpsAvailability?>>(),
                    )
                )
                .thenAnswer { invocation ->
                    val callback = invocation.getArgument<Consumer<ARCore1xVpsAvailability?>>(2)
                    callback.accept(testCase.arCoreVpsAvailability)
                    mockFuture
                }

            val result = underTest.checkVpsAvailability(0.0, 0.0)

            assertThat(result::class).isEqualTo(testCase.expectedXrVpsAvailability)
        }
}
