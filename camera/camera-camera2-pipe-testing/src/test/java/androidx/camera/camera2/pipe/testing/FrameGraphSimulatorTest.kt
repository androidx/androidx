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

package androidx.camera.camera2.pipe.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FrameGraphSimulatorTest {
    private val testScope = TestScope()
    private val backCameraMetadata =
        FakeCameraMetadata.fromTemplate(
            template = HighEndDeviceTemplate,
            cameraId = FakeCameraIds.next(),
            lensFacing = CameraCharacteristics.LENS_FACING_BACK,
        )
    private val frontCameraMetadata =
        FakeCameraMetadata.fromTemplate(
            template = HighEndDeviceTemplate,
            cameraId = FakeCameraIds.next(),
            lensFacing = CameraCharacteristics.LENS_FACING_FRONT,
        )

    private val streamConfig = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = frontCameraMetadata.camera, streams = listOf(streamConfig))

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val cameraPipe =
        CameraPipeSimulator.create(
            testScope,
            context,
            listOf(frontCameraMetadata, backCameraMetadata),
        )

    private val frameGraphSimulator: FrameGraphSimulator =
        cameraPipe.createFrameGraph(FrameGraph.Config(graphConfig))

    @Test
    fun setSurface_validSurface_updatesSetSurfaceResults() {
        FakeSurfaces().use { fakeSurfaces ->
            val surface: Surface = fakeSurfaces.createFakeSurface()
            assertThat(frameGraphSimulator.setSurfaceResults.size).isEqualTo(0)

            frameGraphSimulator.setSurface(STREAM_ID, surface)

            assertThat(frameGraphSimulator.setSurfaceResults.size).isEqualTo(1)
            assertThat(frameGraphSimulator.setSurfaceResults[STREAM_ID]).isSameInstanceAs(surface)
        }
    }

    @Test
    fun setSurface_nullSurface_updatesSetSurfaceResults() {
        val surface: Surface? = null
        assertThat(frameGraphSimulator.setSurfaceResults.size).isEqualTo(0)

        frameGraphSimulator.setSurface(STREAM_ID, surface)

        assertThat(frameGraphSimulator.setSurfaceResults.size).isEqualTo(1)
        assertThat(frameGraphSimulator.setSurfaceResults[STREAM_ID]).isNull()
    }

    @Test
    fun closeFrameGraphSimulator_closesRealFrameGraphAndCameraGraphSimulator() {
        frameGraphSimulator.close()

        assertThat(frameGraphSimulator.isClosed).isTrue()
        assertThat(cameraPipe.cameraGraphs.first().isClosed).isTrue()
    }

    companion object {
        val STREAM_ID = StreamId(1)
    }
}
