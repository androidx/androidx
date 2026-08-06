/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import android.util.Size
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.testing.CameraPipeSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.FrameGraphSimulator
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class StreamGraphCapacityEstimationTest {
    private val testScope = TestScope()
    private val context = ApplicationProvider.getApplicationContext() as Context
    private val metadata = FakeCameraMetadata()

    // 1280x720 YUV_420_888 = 1,382,400 bytes per image
    private val streamConfigLarge =
        CameraStream.Config.create(
            Size(1280, 720),
            StreamFormat.YUV_420_888,
            imageSourceConfig = ImageSourceConfig(capacity = 5),
        )
    val streamConfigSmall =
        CameraStream.Config.create(
            Size(640, 480),
            StreamFormat.YUV_420_888,
            imageSourceConfig = ImageSourceConfig(capacity = 5),
        )

    private val graphConfig =
        CameraGraph.Config(
            camera = metadata.camera,
            streams = listOf(streamConfigLarge, streamConfigSmall),
        )

    private val largeImageSize = StreamFormat.bytesPerImage(StreamFormat.YUV_420_888, 1280, 720)
    private val initialCapacity = largeImageSize * 10

    private lateinit var estimator: MemoryEstimator
    private lateinit var simulator: CameraPipeSimulator
    private lateinit var frameGraph: FrameGraphSimulator

    @Before
    fun setup() {
        estimator = MemoryEstimator.create(initialCapacity)
        simulator = createSimulator(estimator)
        frameGraph = createAndStartFrameGraph(simulator)
    }

    @After
    fun teardown() {
        frameGraph.close()
        simulator.close()
    }

    @Test
    fun streamGraphCapacityDecreasesWhenFramesAcquiredExternally() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Initial availability should be 5, limited by ImageSourceConfig(capacity = 5)
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(5)

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // Frame is buffered internally, which means it is evictable.
            // Because it can be safely evicted if needed, availability remains 5.
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(5)

            // App acquires the frame (external usage)
            val acquiredFrame = frameBuffer.peekFirstReference()?.tryAcquire()
            advanceUntilIdle()

            // The frame is now held externally and is no longer evictable.
            // The availability must drop to 4.
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(4)

            acquiredFrame?.close()
            advanceUntilIdle()

            // Frame closed -> slot and memory restored -> availability 5
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(5)

            frameBuffer.close()
        }

    @Test
    fun streamGraphCapacityReflectsMemoryConstraints() =
        testScope.runTest {
            // Create a custom estimator with memory exactly for 3 large images
            val tightEstimator = MemoryEstimator.create(largeImageSize * 3)
            val tightSimulator = createSimulator(tightEstimator)
            val tightGraph = createAndStartFrameGraph(tightSimulator)
            val streamId = tightGraph.streams[streamConfigLarge]!!.id

            // Even though the ImageSource config requests a capacity of 5, the memory limits it to
            // 3
            assertThat(tightGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(3)

            val frameBuffer = tightGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            val frame = tightGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // The frame is held internally in the buffer, meaning it is still evictable.
            // Availability remains 3.
            assertThat(tightGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(3)

            // Acquire externally
            val acquiredFrame = frameBuffer.peekFirstReference()?.tryAcquire()
            advanceUntilIdle()

            // Memory is no longer evictable, availability drops to 2
            assertThat(tightGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(2)

            acquiredFrame?.close()
            frameBuffer.close()
            tightGraph.close()
            tightSimulator.close()
        }

    @Test
    fun streamGraphCapacityWithMultipleStreamsReturnsMinimum() =
        testScope.runTest {
            val streamLarge = frameGraph.streams[streamConfigLarge]!!.id
            val streamSmall = frameGraph.streams[streamConfigSmall]!!.id

            // Initial availability is 5 for both individually
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamLarge))).isEqualTo(5)
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamSmall))).isEqualTo(5)

            // When queried together, the capacity should reflect the constraining factor
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamLarge, streamSmall)))
                .isEqualTo(5)

            val bufferLarge = frameGraph.captureWith(setOf(streamLarge), capacity = 5)
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamLarge)
            advanceUntilIdle()

            // Hide 1 large frame externally
            val acquired = bufferLarge.peekFirstReference()?.tryAcquire()
            advanceUntilIdle()

            // Large stream's isolated capacity is now 4, Small stream is 5.
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamLarge))).isEqualTo(4)
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamSmall))).isEqualTo(5)

            // The combined estimated availability must be bound by the most restricted stream (4)
            assertThat(frameGraph.streams.estimateAvailableFrames(setOf(streamLarge, streamSmall)))
                .isEqualTo(4)

            acquired?.close()
            bufferLarge.close()
        }

    @Test
    fun streamGraphCapacityWithUnboundedMemoryReliesOnImageSourceCapacity() =
        testScope.runTest {
            // 1. Create an unbounded memory estimator (Long.MAX_VALUE capacity)
            val unboundedEstimator = MemoryEstimator.create()
            val unboundedSimulator = createSimulator(unboundedEstimator)
            val unboundedGraph = createAndStartFrameGraph(unboundedSimulator)
            val streamId = unboundedGraph.streams[streamConfigLarge]!!.id

            // 2. Initial availability should be 5, limited ONLY by ImageSourceConfig(capacity = 5)
            // Even though memory is essentially infinite, the physical slots are capped.
            assertThat(unboundedGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(5)

            val frameBuffer = unboundedGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            val frame = unboundedGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // Frame is buffered internally (evictable).
            assertThat(unboundedGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(5)

            // 3. Acquire externally to consume a physical ImageReader/ImageSource slot
            val acquiredFrame = frameBuffer.peekFirstReference()?.tryAcquire()
            advanceUntilIdle()

            // 4. The frame is now held externally, consuming 1 physical slot.
            // Availability must drop to 4, driven entirely by physical slots, not memory.
            assertThat(unboundedGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(4)

            acquiredFrame?.close()
            frameBuffer.close()
            unboundedGraph.close()
            unboundedSimulator.close()
        }

    @Test
    fun streamGraphCapacityAccountsForExpectedOutputInMultiOutputStream() =
        testScope.runTest {
            // 1. Setup a multi-output stream config
            val outputLarge = OutputStream.Config.create(Size(1280, 720), StreamFormat.YUV_420_888)
            val outputSmall = OutputStream.Config.create(Size(640, 480), StreamFormat.YUV_420_888)
            val streamConfigMulti =
                CameraStream.Config.create(
                    outputs = listOf(outputLarge, outputSmall),
                    imageSourceConfig =
                        ImageSourceConfig(capacity = 5).apply { enableConcurrentOutputs = true },
                )

            val customGraphConfig =
                CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfigMulti))

            // 2. Set memory budget to exactly 2 large images
            // largeImageSize (1280x720) = 1,382,400 bytes
            // smallImageSize (640x480) = 460,800 bytes
            val tightEstimator = MemoryEstimator.create(largeImageSize * 2)
            val tightSimulator =
                CameraPipeSimulator.create(
                    testScope = testScope,
                    testContext = context,
                    fakeCameras = listOf(metadata),
                    memoryEstimator = tightEstimator,
                )
            val tightGraph = tightSimulator.createFrameGraph(FrameGraph.Config(customGraphConfig))
            tightGraph.start()
            tightGraph.initializeSurfaces()
            tightGraph.simulateCameraStarted()
            advanceUntilIdle()

            val streamId = tightGraph.streams[streamConfigMulti]!!.id
            val outputIdSmall = tightGraph.streams[streamConfigMulti]!!.outputs[1].id

            // 3. Initial availability should fall back to the first output (outputLarge)
            // Since the budget is exactly 2 large images, availability should be 2.
            assertThat(tightGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(2)

            // 4. Simulate an expected output event targeting the small output
            // Use the simulator API to properly trigger the internal ImageReaderImageSource
            // listener
            tightGraph.simulateExpectedOutputs(
                streamId,
                123456789L, // simulated timestamp
                setOf(outputIdSmall),
            )
            advanceUntilIdle()

            // 5. Availability should now be based on outputSmall's memory footprint
            // Budget allows: (1382400 * 2) / 460800 = 6 small images
            // However, ImageSourceConfig capacity is strictly capped at 5.
            assertThat(tightGraph.streams.estimateAvailableFrames(setOf(streamId))).isEqualTo(5)

            tightGraph.close()
            tightSimulator.close()
        }

    private fun createSimulator(memoryEstimator: MemoryEstimator): CameraPipeSimulator {
        return CameraPipeSimulator.create(
            testScope = testScope,
            testContext = context,
            fakeCameras = listOf(metadata),
            memoryEstimator = memoryEstimator,
        )
    }

    private fun createAndStartFrameGraph(simulator: CameraPipeSimulator): FrameGraphSimulator {
        val frameGraph = simulator.createFrameGraph(FrameGraph.Config(graphConfig))
        frameGraph.start()
        frameGraph.initializeSurfaces()
        frameGraph.simulateCameraStarted()
        testScope.advanceUntilIdle()
        return frameGraph
    }
}
