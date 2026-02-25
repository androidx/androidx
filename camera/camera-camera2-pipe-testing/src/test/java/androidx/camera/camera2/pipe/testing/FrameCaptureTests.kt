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
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Frame.Companion.isFrameInfoAvailable
import androidx.camera.camera2.pipe.GraphState.GraphStateStarted
import androidx.camera.camera2.pipe.GraphState.GraphStateStarting
import androidx.camera.camera2.pipe.GraphState.GraphStateStopped
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class FrameCaptureTests {
    private val testScope = TestScope()
    private val testContext = ApplicationProvider.getApplicationContext() as Context
    private val cameraPipeSimulator = CameraPipeSimulator.create(testScope, testContext)
    private val cameraId = cameraPipeSimulator.cameras().awaitCameraIds()!!.first()
    private val cameraMetadata = cameraPipeSimulator.cameras().awaitCameraMetadata(cameraId)!!

    private val viewfinderStreamConfig =
        CameraStream.Config.create(Size(640, 480), StreamFormat.UNKNOWN)

    private val jpegStreamConfig =
        CameraStream.Config.create(
            Size(640, 480),
            StreamFormat.YUV_420_888,
            imageSourceConfig = ImageSourceConfig(capacity = 10),
        )

    private val rawStreamOutputConfigs =
        listOf(
            OutputStream.Config.create(Size(1280, 720), StreamFormat.RAW10),
            OutputStream.Config.create(Size(1920, 1080), StreamFormat.RAW10),
            OutputStream.Config.create(Size(1920, 1200), StreamFormat.RAW10),
        )
    private val rawStreamConfig =
        CameraStream.Config.create(rawStreamOutputConfigs, ImageSourceConfig(5))

    private val graphConfig =
        CameraGraph.Config(
            camera = cameraMetadata.camera,
            streams = listOf(viewfinderStreamConfig, jpegStreamConfig, rawStreamConfig),
        )

    private val cameraGraphSimulator = cameraPipeSimulator.createCameraGraphSimulator(graphConfig)
    private val cameraGraph: CameraGraph = cameraGraphSimulator

    private val viewfinderStream = cameraGraph.streams[viewfinderStreamConfig]!!
    private val jpegStream = cameraGraph.streams[jpegStreamConfig]!!
    private val rawStream = cameraGraph.streams[rawStreamConfig]!!
    private val expectedRawOutputId = rawStream.outputs.last().id

    private suspend fun startCameraGraph() {
        assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStopped)

        cameraGraph.start() // Tell the cameraGraph to start
        assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStarting)

        cameraGraphSimulator.initializeSurfaces()
        cameraGraphSimulator.simulateCameraStarted() // Simulate the camera starting successfully
        assertThat(cameraGraph.graphState.value).isEqualTo(GraphStateStarted)
    }

    @After
    fun tearDown() {
        cameraPipeSimulator.close()
    }

    @Test
    fun frameCaptureCanBeSimulated() =
        testScope.runTest {
            startCameraGraph()

            // Capture an image using the cameraGraph
            val frameCapture =
                cameraGraph.useSession { session ->
                    session.capture(Request(streams = listOf(jpegStream.id, rawStream.id)))
                }
            advanceUntilIdle()

            // Verify a capture sequence with all of the frame interactions
            val frameCaptureJob = launch {
                // TODO: Should awaitFrame be called awaitFrameStarted?
                // TODO: Should there be an awaitComplete() function?
                val frame = frameCapture.awaitFrame()
                assertThat(frame).isNotNull()

                assertThat(frame!!.frameId.value).isGreaterThan(0)
                assertThat(frame.frameTimestamp.value).isGreaterThan(0)

                val image = frame.awaitImage(jpegStream.id)
                val rawImages = frame.awaitImages(rawStream.id)
                assertThat(frame.imageStatus(jpegStream.id)).isEqualTo(OutputStatus.AVAILABLE)
                assertThat(frame.imageStatus(rawStream.id)).isEqualTo(OutputStatus.AVAILABLE)
                assertThat(frame.imageStatus(viewfinderStream.id))
                    .isEqualTo(OutputStatus.UNAVAILABLE)
                assertThat(image).isNotNull()
                assertThat(image!!.timestamp).isEqualTo(frame.frameTimestamp.value)
                assertThat(rawImages.size).isEqualTo(1)
                val rawImage = rawImages.first()
                assertThat(rawImage.timestamp).isEqualTo(frame.frameTimestamp.value)

                image.close()
                rawImage.close()

                assertThat(frame.imageStatus(jpegStream.id)).isEqualTo(OutputStatus.AVAILABLE)
                assertThat(frame.imageStatus(rawStream.id)).isEqualTo(OutputStatus.AVAILABLE)
                assertThat(frame.imageStatus(viewfinderStream.id))
                    .isEqualTo(OutputStatus.UNAVAILABLE)

                println("frame.awaitFrameInfo()")
                val frameInfo = frame.awaitFrameInfo()

                assertThat(frame.isFrameInfoAvailable).isTrue()
                assertThat(frameInfo).isNotNull()
                assertThat(frameInfo!!.frameNumber).isEqualTo(frame.frameNumber)

                println("frame.close()")
                frame.close()

                assertThat(frame.imageStatus(jpegStream.id)).isEqualTo(OutputStatus.UNAVAILABLE)
                assertThat(frame.imageStatus(rawStream.id)).isEqualTo(OutputStatus.UNAVAILABLE)
                assertThat(frame.imageStatus(viewfinderStream.id))
                    .isEqualTo(OutputStatus.UNAVAILABLE)
                assertThat(frame.isFrameInfoAvailable).isFalse()
            }

            // Simulate camera interactions:
            val frameSimulator = cameraGraphSimulator.simulateNextFrame()

            frameSimulator.simulateImage(jpegStream.id)
            frameSimulator.simulateExpectedOutputs(
                rawStream.id,
                outputIds = setOf(expectedRawOutputId),
            )
            frameSimulator.simulateImage(rawStream.id, outputId = expectedRawOutputId)
            frameSimulator.simulateComplete(emptyMap())

            // TODO: should this have a way to check to make sure all frames are closed?
            // cameraGraph?

            advanceUntilIdle()
            assertThat(frameCaptureJob.isCompleted).isTrue() // Ensure verification is complete
            cameraGraphSimulator.close()
        }
}
