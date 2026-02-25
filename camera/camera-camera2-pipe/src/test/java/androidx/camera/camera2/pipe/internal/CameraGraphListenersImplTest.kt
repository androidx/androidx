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

package androidx.camera.camera2.pipe.internal

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.StrictMode
import androidx.camera.camera2.pipe.compat.Camera2Quirks
import androidx.camera.camera2.pipe.graph.GraphProcessorImpl
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import androidx.camera.camera2.pipe.graph.Listener3A
import androidx.camera.camera2.pipe.testing.FakeCamera2MetadataProvider
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.isRepeating
import androidx.camera.camera2.pipe.testing.FakeCaptureSequenceProcessor.Companion.listeners
import androidx.camera.camera2.pipe.testing.FakeGraphConfigs
import androidx.camera.camera2.pipe.testing.FakeRequestListener
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class CameraGraphListenersImplTest {
    private val testScope = TestScope()
    private val graphListener = FakeRequestListener()
    private val graphProcessor =
        GraphProcessorImpl(
            FakeThreads.fromTestScope(testScope),
            CameraGraphId.nextId(),
            FakeGraphConfigs.graphConfig,
            Listener3A(),
            listOf(graphListener),
            Camera2Quirks(
                metadataProvider =
                    FakeCamera2MetadataProvider(
                        mapOf(CameraId("0") to FakeCameraMetadata(cameraId = CameraId("0")))
                    ),
                strictMode = StrictMode(false),
            ),
        )

    private val surfaceMap =
        mapOf(StreamId(0) to Surface(SurfaceTexture(0)), StreamId(1) to Surface(SurfaceTexture(1)))
    private val csp = FakeCaptureSequenceProcessor().also { it.surfaceMap = surfaceMap }
    private val grp = GraphRequestProcessor.from(csp)
    private val requestListener: Request.Listener = mock()
    private val request = Request(listOf(StreamId(0)), listeners = listOf(requestListener))
    private val request2 = Request(listOf(StreamId(1)), listeners = listOf(requestListener))

    @Test
    fun add_requestContainsBothGraphAndAddedListeners() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp)
            graphProcessor.repeatingRequest = request
            val newListener: Request.Listener = mock()

            val listeners =
                CameraGraphRequestListenersImpl(GraphSessionLock(), graphProcessor, testScope)
            listeners.add(newListener)
            advanceUntilIdle()

            assertEquals(2, csp.events.size)
            assertTrue(csp.events[1].isRepeating)
            assertEquals(2, csp.events[1].listeners.size)
            assertThat(csp.events[1].listeners).contains(graphListener)
            assertThat(csp.events[1].listeners).contains(newListener)
        }

    @Test
    fun add_requestAndAddedListenerReceiveCallbacks() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp)
            graphProcessor.repeatingRequest = request
            val newListener: Request.Listener = mock()
            advanceUntilIdle()

            val listeners =
                CameraGraphRequestListenersImpl(GraphSessionLock(), graphProcessor, testScope)
            listeners.add(newListener)
            advanceUntilIdle()

            // The request should be invalidated when there's a listener change, trigger a new
            // callback.
            verify(requestListener, times(2)).onRequestSequenceCreated(any())
            verify(newListener, times(1)).onRequestSequenceCreated(any())
        }

    @Test
    fun multipleAdd_receiveCallbacks() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp)
            graphProcessor.repeatingRequest = request
            val newListener1: Request.Listener = mock()
            val newListener2: Request.Listener = mock()
            val newListener3: Request.Listener = mock()
            advanceUntilIdle()

            val listeners =
                CameraGraphRequestListenersImpl(GraphSessionLock(), graphProcessor, testScope)
            listeners.add(newListener1)
            advanceUntilIdle()
            listeners.addAll(listOf(newListener2, newListener3))
            advanceUntilIdle()

            verify(requestListener, times(3)).onRequestSequenceCreated(any())
            verify(newListener1, times(2)).onRequestSequenceCreated(any())
            verify(newListener2, times(1)).onRequestSequenceCreated(any())
            verify(newListener3, times(1)).onRequestSequenceCreated(any())
        }

    @Test
    fun remove_noLongerReceiveCallback() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp)
            graphProcessor.repeatingRequest = request
            val newListener1: Request.Listener = mock()
            val newListener2: Request.Listener = mock()
            advanceUntilIdle()

            val listeners =
                CameraGraphRequestListenersImpl(GraphSessionLock(), graphProcessor, testScope)
            listeners.addAll(listOf(newListener1, newListener2))
            advanceUntilIdle()
            listeners.remove(newListener2)
            advanceUntilIdle()

            // The request should be invalidated when there's a listener change, trigger a new
            // callback.
            verify(requestListener, times(3)).onRequestSequenceCreated(any())
            verify(newListener1, times(2)).onRequestSequenceCreated(any())
            // Removed listener no longer receive callbacks.
            verify(newListener2, times(1)).onRequestSequenceCreated(any())
        }

    @Test
    fun multipleRemove_noLongerReceiveCallbacks() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp)
            graphProcessor.repeatingRequest = request
            val newListener1: Request.Listener = mock()
            val newListener2: Request.Listener = mock()
            val newListener3: Request.Listener = mock()
            advanceUntilIdle()

            val listeners =
                CameraGraphRequestListenersImpl(GraphSessionLock(), graphProcessor, testScope)
            listeners.addAll(listOf(newListener1, newListener2, newListener3))
            advanceUntilIdle()
            listeners.remove(newListener1)
            advanceUntilIdle()
            listeners.removeAll(listOf(newListener1, newListener2))
            advanceUntilIdle()

            verify(requestListener, times(4)).onRequestSequenceCreated(any())
            verify(newListener1, times(1)).onRequestSequenceCreated(any())
            verify(newListener2, times(2)).onRequestSequenceCreated(any())
            verify(newListener3, times(3)).onRequestSequenceCreated(any())
        }

    @Test
    fun updateRequest_listenersContinueToReceiveCallbacks() =
        testScope.runTest {
            graphProcessor.onGraphStarted(grp)
            graphProcessor.repeatingRequest = request
            val newListener: Request.Listener = mock()
            advanceUntilIdle()

            val listeners =
                CameraGraphRequestListenersImpl(GraphSessionLock(), graphProcessor, testScope)
            listeners.add(newListener)
            advanceUntilIdle()

            // The request should be invalidated when there's a listener change, trigger a new
            // callback.
            verify(requestListener, times(2)).onRequestSequenceCreated(any())
            verify(newListener, times(1)).onRequestSequenceCreated(any())
        }
}
