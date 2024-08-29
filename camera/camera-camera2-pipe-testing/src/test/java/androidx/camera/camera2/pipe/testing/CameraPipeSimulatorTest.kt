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

package androidx.camera.camera2.pipe.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraPipeSimulatorTest {
    private val testScope = TestScope()
    private val backCameraMetadata =
        FakeCameraMetadata(
            cameraId = FakeCameraIds.next(),
            characteristics =
                mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK)
        )
    private val frontCameraMetadata =
        FakeCameraMetadata(
            cameraId = FakeCameraIds.next(),
            characteristics =
                mapOf(CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT)
        )

    private val streamConfig = CameraStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
    private val graphConfig =
        CameraGraph.Config(camera = frontCameraMetadata.camera, streams = listOf(streamConfig))

    private val context = ApplicationProvider.getApplicationContext() as Context
    private val cameraPipe =
        CameraPipeSimulator.create(
            testScope,
            context,
            listOf(frontCameraMetadata, backCameraMetadata)
        )

    @Test
    fun cameraPipeSimulatorCanCreateCameraGraphSimulators() =
        testScope.runTest {
            val cameraGraph1 = cameraPipe.create(graphConfig)
            val cameraGraphSimulator1 = cameraPipe.cameraGraphs.find { it == cameraGraph1 }

            assertThat(cameraGraph1).isInstanceOf(CameraGraphSimulator::class.java)
            assertThat(cameraGraph1).isSameInstanceAs(cameraGraphSimulator1)

            // Assert that a new CameraGraph can be created with the same graphConfig and that they
            // produce different CameraGraph instances and simulators.
            val cameraGraphSimulator3 = cameraPipe.createCameraGraphSimulator(graphConfig)
            assertThat(cameraGraphSimulator3).isNotSameInstanceAs(cameraGraphSimulator1)
        }

    @Test
    fun cameraPipeSimulatorHasMetadataViaCameraPipe() =
        testScope.runTest {
            val cameraIds = cameraPipe.cameras().getCameraIds()

            assertThat(cameraIds).isNotNull()
            assertThat(cameraIds!!.size).isEqualTo(2)

            val firstCameraId = cameraIds.first()
            val firstMetadata = cameraPipe.cameras().getCameraMetadata(firstCameraId)

            assertThat(firstMetadata).isNotNull()
            assertThat(firstMetadata).isSameInstanceAs(frontCameraMetadata)
            assertThat(firstMetadata!!.camera).isEqualTo(firstCameraId)

            val lastCameraId = cameraIds.last()
            val lastCameraMetadata = cameraPipe.cameras().getCameraMetadata(lastCameraId)

            assertThat(lastCameraMetadata).isNotNull()
            assertThat(lastCameraMetadata).isSameInstanceAs(backCameraMetadata)
            assertThat(lastCameraMetadata!!.camera).isEqualTo(lastCameraId)
        }

    @Test
    fun cameraPipeSimulatorCanCreateDualCameraGraphs() =
        testScope.runTest {
            val cameraIds = cameraPipe.cameras().getCameraIds()

            assertThat(cameraIds).isNotNull()
            assertThat(cameraIds!!.size).isEqualTo(2)

            val firstCameraId = cameraIds.first()
            val firstMetadata = cameraPipe.cameras().getCameraMetadata(firstCameraId)

            assertThat(firstMetadata).isNotNull()
            assertThat(firstMetadata).isSameInstanceAs(frontCameraMetadata)
            assertThat(firstMetadata!!.camera).isEqualTo(firstCameraId)
        }

    @Test
    fun cameraPipeSimulatorCanVerifyCameraGraphConstructionOrder() {
        val graphConfig1 =
            CameraGraph.Config(camera = frontCameraMetadata.camera, streams = listOf(streamConfig))
        val graphConfig2 =
            CameraGraph.Config(camera = backCameraMetadata.camera, streams = listOf(streamConfig))
        val graphConfig3 =
            CameraGraph.Config(camera = frontCameraMetadata.camera, streams = listOf(streamConfig))

        val cameraGraph1 = cameraPipe.create(graphConfig1)
        val cameraGraph2 = cameraPipe.create(graphConfig2)
        val cameraGraph3 = cameraPipe.create(graphConfig3)

        assertThat(cameraPipe.cameraGraphs)
            .containsExactly(cameraGraph1, cameraGraph2, cameraGraph3)
            .inOrder()

        cameraGraph1.close()
        cameraGraph2.close()
        cameraGraph3.close()
    }

    @Test
    fun cameraPipeSimulatorCanCheckForUnclosedResources() {
        val cameraGraph = cameraPipe.create(graphConfig)
        val fakeImageReader =
            cameraPipe.fakeImageReaders.create(cameraGraph.streams[streamConfig]!!, 1)
        val fakeImage = fakeImageReader.simulateImage(123)

        // Assert that these throw exceptions
        assertThrows(IllegalStateException::class.java) { cameraPipe.checkImageReadersClosed() }
        assertThrows(IllegalStateException::class.java) { cameraPipe.checkImagesClosed() }
        assertThrows(IllegalStateException::class.java) { cameraPipe.checkCameraGraphsClosed() }

        // Close everything
        fakeImage.close()
        fakeImageReader.close()
        cameraGraph.close()

        // Assert that these no longer throw exceptions.
        cameraPipe.checkCameraGraphsClosed()
        cameraPipe.checkImageReadersClosed()
        cameraPipe.checkImagesClosed()
    }

    @Test
    fun cameraPipeSimulatorCanCreateConcurrentCameraGraphs() {
        val config1 =
            CameraGraph.Config(
                camera = frontCameraMetadata.camera,
                streams = listOf(streamConfig),
            )
        val config2 =
            CameraGraph.Config(
                camera = backCameraMetadata.camera,
                streams = listOf(streamConfig),
            )
        val concurrentCameras = listOf(config1, config2)

        val cameraGraphs =
            cameraPipe.createCameraGraphs(CameraGraph.ConcurrentConfig(concurrentCameras))

        assertThat(cameraGraphs.size).isEqualTo(2)
        assertThat(cameraGraphs[0].config.camera).isEqualTo(frontCameraMetadata.camera)
        assertThat(cameraGraphs[1].config.camera).isEqualTo(backCameraMetadata.camera)

        val config1Stream1 = cameraGraphs[0].streams[streamConfig]
        val config2Stream1 = cameraGraphs[1].streams[streamConfig]
        assertThat(config1Stream1).isNotEqualTo(config2Stream1)
    }
}
