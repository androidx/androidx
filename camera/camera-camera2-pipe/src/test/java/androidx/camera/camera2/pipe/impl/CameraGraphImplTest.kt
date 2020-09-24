/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
import android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.testing.CameraPipeRobolectricTestRunner
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeCameras
import androidx.camera.camera2.pipe.testing.FakeGraphProcessor
import androidx.camera.camera2.pipe.testing.FakeGraphState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(CameraPipeRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraGraphImplTest {
    private val fakeCameraId = FakeCameras.create()
    private val fakeMetadata = FakeCameraMetadata(
        mapOf(INFO_SUPPORTED_HARDWARE_LEVEL to INFO_SUPPORTED_HARDWARE_LEVEL_FULL),
        cameraId = fakeCameraId
    )
    private val fakeGraphProcessor = FakeGraphProcessor()
    private val fakeGraphState = FakeGraphState()
    private lateinit var impl: CameraGraphImpl

    @Before
    fun setUp() {
        val config = CameraGraph.Config(
            camera = fakeCameraId,
            streams = listOf(),
            template = RequestTemplate(0)
        )
        impl = CameraGraphImpl(
            config,
            fakeMetadata,
            fakeGraphProcessor,
            StreamMap(
                fakeMetadata,
                config
            ),
            fakeGraphState
        )
    }

    @Test
    fun createCameraGraphImpl() {
        assertThat(impl).isNotNull()
    }

    @Test
    fun testAcquireSession() = runBlocking {
        val session = impl.acquireSession()
        assertThat(session).isNotNull()
    }

    @Test
    fun testAcquireSessionOrNull() {
        val session = impl.acquireSessionOrNull()
        assertThat(session).isNotNull()
    }

    @Test
    fun testAcquireSessionOrNullAfterAcquireSession() = runBlocking {
        val session = impl.acquireSession()
        assertThat(session).isNotNull()

        // Since a session is already active, an attempt to acquire another session will fail.
        val session1 = impl.acquireSessionOrNull()
        assertThat(session1).isNull()

        // Closing an active session should allow a new session instance to be created.
        session.close()

        val session2 = impl.acquireSessionOrNull()
        assertThat(session2).isNotNull()
    }

    @Test
    fun sessionSubmitsRequestsToGraphProcessor() {
        val session = checkNotNull(impl.acquireSessionOrNull())
        val request = Request(listOf())
        session.submit(request)

        assertThat(fakeGraphProcessor.requestQueue).contains(listOf(request))
    }

    @Test
    fun sessionSetsRepeatingRequestOnGraphProcessor() {
        val session = checkNotNull(impl.acquireSessionOrNull())
        val request = Request(listOf())
        session.setRepeating(request)

        assertThat(fakeGraphProcessor.repeatingRequest).isSameInstanceAs(request)
    }

    @Test
    fun sessionAbortsRequestOnGraphProcessor() {
        val session = checkNotNull(impl.acquireSessionOrNull())
        val request = Request(listOf())
        session.submit(request)
        session.abort()

        assertThat(fakeGraphProcessor.requestQueue).isEmpty()
    }

    @Test
    fun closingSessionDoesNotCloseGraphProcessor() {
        val session = impl.acquireSessionOrNull()
        checkNotNull(session).close()

        assertThat(fakeGraphProcessor.closed).isFalse()
    }

    @Test
    fun closingCameraGraphClosesGraphProcessor() {
        impl.close()
        assertThat(fakeGraphProcessor.closed).isTrue()
    }

    @Test
    fun stoppingCameraGraphStopsGraphProcessor() {
        assertThat(fakeGraphState.active).isFalse()
        impl.start()
        assertThat(fakeGraphState.active).isTrue()
        impl.stop()
        assertThat(fakeGraphState.active).isFalse()
        impl.start()
        assertThat(fakeGraphState.active).isTrue()
        impl.close()
        assertThat(fakeGraphProcessor.closed).isTrue()
        assertThat(fakeGraphState.active).isFalse()
    }
}