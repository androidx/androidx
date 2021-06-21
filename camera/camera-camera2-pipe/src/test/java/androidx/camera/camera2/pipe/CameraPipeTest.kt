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

package androidx.camera.camera2.pipe

import android.content.Context
import android.os.Build
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestProcessor
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.testing.RobolectricCameras
import androidx.camera.camera2.pipe.testing.awaitEvent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
internal class CameraPipeTest {

    @Test
    fun createCameraPipe() {
        val context = ApplicationProvider.getApplicationContext() as Context
        assertThat(context).isNotNull()

        val cameraPipe = CameraPipe(CameraPipe.Config(context))
        assertThat(cameraPipe).isNotNull()
    }

    @Test
    fun createCameraGraph() {
        val fakeCameraId = RobolectricCameras.create()
        val context = ApplicationProvider.getApplicationContext() as Context
        val cameraPipe = CameraPipe(CameraPipe.Config(context))
        val cameraGraph = cameraPipe.create(
            CameraGraph.Config(
                camera = fakeCameraId,
                streams = listOf(),
                defaultTemplate = RequestTemplate(0)
            )
        )
        assertThat(cameraGraph).isNotNull()
    }

    @Test
    fun iterateCameraIds() {
        val fakeCameraId = RobolectricCameras.create()
        val context = ApplicationProvider.getApplicationContext() as Context
        val cameraPipe = CameraPipe(CameraPipe.Config(context))
        val cameras = cameraPipe.cameras()
        val cameraList = runBlocking { cameras.ids() }

        assertThat(cameraList).isNotNull()
        assertThat(cameraList.size).isEqualTo(1)
        assertThat(cameraList).contains(fakeCameraId)
    }

    @Test
    fun createExternalCameraGraph() {
        val fakeRequestProcessor = FakeRequestProcessor()
        val fakeCameraMetadata = FakeCameraMetadata()

        val config = CameraGraph.Config(
            camera = fakeCameraMetadata.camera,
            streams = listOf(),
            defaultTemplate = RequestTemplate(0)
        )

        val cameraGraph = CameraPipe.External().create(
            config,
            fakeCameraMetadata,
            fakeRequestProcessor
        )
        assertThat(cameraGraph).isNotNull()

        val request = Request(streams = emptyList())
        cameraGraph.start()

        // Check that repeating request can be issued
        runBlocking {
            cameraGraph.acquireSession().use {
                it.startRepeating(request)
            }

            val repeatingEvent = fakeRequestProcessor.nextEvent()
            assertThat(repeatingEvent.startRepeating).isTrue()
            assertThat(repeatingEvent.requestSequence!!.requests.first()).isSameInstanceAs(request)

            cameraGraph.stop()

            val closeEvent = fakeRequestProcessor.awaitEvent { it.close }
            assertThat(closeEvent.close).isTrue()
        }

        fakeRequestProcessor.reset()

        // Check that repeating request is saved and reused.
        runBlocking {
            cameraGraph.start()

            val repeatingEvent = fakeRequestProcessor.nextEvent()
            if (!repeatingEvent.startRepeating) {
                throw RuntimeException("$repeatingEvent")
            }

            assertThat(repeatingEvent.startRepeating).isTrue()
            assertThat(repeatingEvent.requestSequence!!.requests.first()).isSameInstanceAs(request)

            cameraGraph.stop()
        }
    }
}