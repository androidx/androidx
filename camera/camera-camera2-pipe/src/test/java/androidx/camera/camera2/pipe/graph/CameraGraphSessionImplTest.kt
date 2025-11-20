/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata.CONTROL_AE_STATE_LOCKED
import android.hardware.camera2.CaptureResult
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.testing.CameraGraphSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
internal class CameraGraphSessionImplTest {
    private val testScope = TestScope()
    private val metadata =
        FakeCameraMetadata(
            characteristics =
                mapOf(
                    CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT,
                    CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE to 1.0f,
                )
        )

    private val streamConfig = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)

    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfig))

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val simulator = CameraGraphSimulator.create(testScope, context, metadata, graphConfig)

    @Test
    fun sessionCannotBeUsedAfterClose() =
        testScope.runTest {
            val session = simulator.acquireSession()
            session.close()

            val result = assertThrows<IllegalStateException> { session.submit(Request(listOf())) }
            result.hasMessageThat().contains("submit")
        }

    @Test
    fun stopRepeatingShouldCancel3ARequests() =
        testScope.runTest {
            val session = simulator.acquireSession()
            session.startRepeating(Request(streams = listOf(StreamId(1))))
            val deferred = session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)

            assertThat(deferred.isCompleted).isFalse()

            // Don't return any results to simulate that the 3A conditions haven't been met, but the
            // app calls stopRepeating(). In which case, we should fail here with SUBMIT_CANCELLED.
            session.stopRepeating()
            assertThat(deferred.isCompleted).isTrue()
            val result = deferred.await()
            assertThat(result.status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
            session.close()
        }

    @Test
    fun initiate3ARequestsShouldThrowWhenSessionIsClosed() =
        testScope.runTest {
            val session = simulator.acquireSession()
            session.startRepeating(Request(streams = listOf(StreamId(1))))
            advanceUntilIdle()

            // Now close the session
            session.close()
            assertThrows<IllegalStateException> {
                session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
            }
        }

    @Test
    fun lock3AShouldFailWhenInvokedBeforeStartRepeating() = runTest {
        simulator.start()
        simulator.simulateCameraStarted()
        simulator.initializeSurfaces()
        advanceUntilIdle()

        val session = simulator.acquireSession()
        val afResult = session.lock3A(afLockBehavior = Lock3ABehavior.IMMEDIATE).await()
        assertThat(afResult.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)

        val aeResult = session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE).await()
        assertThat(aeResult.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
    }

    @Test
    fun lock3AShouldSucceedWhenInvokedAfterStartRepeatingAndConverged() =
        testScope.runTest {
            val stream = simulator.streams[streamConfig]!!
            val request = Request(streams = listOf(stream.id))
            simulator.acquireSession().use { it.startRepeating(request) }
            simulator.start()
            simulator.simulateCameraStarted()
            simulator.initializeSurfaces()
            advanceUntilIdle()
            simulator.simulateNextFrame()

            val session = simulator.acquireSession()
            val result = session.lock3A(aeLockBehavior = Lock3ABehavior.IMMEDIATE)
            advanceUntilIdle()

            val frame = simulator.simulateNextFrame()
            frame.simulateTotalCaptureResult(
                mapOf(CaptureResult.CONTROL_AE_STATE to CONTROL_AE_STATE_LOCKED)
            )

            assertThat(result.await().status).isEqualTo(Result3A.Status.OK)
            session.close()
        }

    @Test
    fun lock3AShouldFailWhenInvokedAfterStartAndStopRepeating() =
        testScope.runTest {
            val stream = simulator.streams[streamConfig]!!
            val request = Request(streams = listOf(stream.id))
            simulator.acquireSession().use { it.startRepeating(request) }
            simulator.start()
            simulator.simulateCameraStarted()
            simulator.initializeSurfaces()
            advanceUntilIdle()
            simulator.simulateNextFrame()

            // Stop repeating
            val session = simulator.acquireSession()
            session.stopRepeating()

            // Now lock3A should fail immediately with SUBMIT_FAILED.
            val result = session.lock3A(afLockBehavior = Lock3ABehavior.IMMEDIATE).await()
            assertThat(result.status).isEqualTo(Result3A.Status.SUBMIT_FAILED)
            session.close()
        }
}
