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
import androidx.camera.camera2.pipe.FrameBuffers.tryPeekFirst
import androidx.camera.camera2.pipe.FrameBuffers.tryPeekLast
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.Request
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

/**
 * End-to-end tests verifying that memory accounting correctly tracks buffer capacity, evictable
 * memory pools, and properly handles external usage when an app acquires frames via FrameBuffer(s)
 * and via explicit captures.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class MemoryEstimationTest {
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
    private val smallImageSize =
        StreamFormat.bytesPerImage(StreamFormat.YUV_420_888, width = 640, height = 480)
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
    fun internalFramesAreTrackedAsEvictable() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Simulate 1 frame entering the buffer
            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(1)

            // Usage increases by 1 image, and because it is only held by the internal
            // FrameBuffer, 100% of that allocated memory should be marked as evictable.
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)
        }

    @Test
    fun acquiringFrameHidesMemoryFromEviction() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // Memory starts in the evictable pool
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)

            // App acquires the frame (Establishing the external usage lease)
            val acquiredFrame = frameBuffer.peekFirstReference()?.tryAcquire()
            assertThat(acquiredFrame).isNotNull()

            // The memory is still actively allocated (usage remains increased)...
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            // ...but it is strictly hidden from the EvictionManager!
            assertThat(estimator.evictable.value).isEqualTo(0L)

            acquiredFrame?.close()
        }

    @Test
    fun closingExternalFrameRestoresEvictableMemory() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // App acquires and hides the memory
            val acquiredFrame = frameBuffer.peekFirstReference()?.tryAcquire()
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // App finishes processing and closes the frame
            acquiredFrame?.close()
            advanceUntilIdle()

            // The external lease is dropped. Since the internal FrameBuffer still
            // holds the frame, it becomes eligible for eviction once again.
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)
        }

    @Test
    fun closingBufferAndExternalFrameCompletelyFreesMemory() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // App acquires frame
            val acquiredFrame = frameBuffer.peekFirstReference()?.tryAcquire()

            // Simulate FrameBuffer clearing (e.g. graph shuts down or buffer capacity drops to 0)
            frameBuffer.close()
            advanceUntilIdle()

            // The buffer released its reference, but the App still holds it.
            // Usage should still be 1 image, and evictable should be 0.
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Finally, the App closes the frame
            acquiredFrame?.close()
            advanceUntilIdle()

            // Both references are gone. The hardware buffer is freed, and ALL usage is cleared.
            assertThat(estimator.usage.value).isEqualTo(0L)
            assertThat(estimator.evictable.value).isEqualTo(0L)
        }

    @Test
    fun memoryAccountingHandlesMultipleFramesCorrectly() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Feed 3 frames into the pipeline
            repeat(3) {
                val frame = frameGraph.simulateNextFrame()
                advanceUntilIdle()
                frame.simulateImage(streamId)
                advanceUntilIdle()
            }

            // 3 frames in buffer = 3 allocated images, 3 evictable images
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 3)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 3)

            // App acquires the 1st frame (removes from internal buffer and takes ownership).
            val frame1 = frameBuffer.removeFirst()
            assertThat(frame1).isNotNull()

            // Evictable drops by 1 (hidden)
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 3)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 2)

            // App acquires the 2nd frame
            val frame2 = frameBuffer.removeFirst()
            assertThat(frame2).isNotNull()

            // Evictable drops by another 1
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 1)

            // App closes the 1st frame. Because it was cleanly removed from the
            // internal buffer earlier, closing it completely destroys it.
            // Usage drops by 1, evictable stays at 1.
            frame1?.close()
            advanceUntilIdle()

            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 2)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 1)

            frame2?.close()
        }

    @Test
    fun multipleFrameBuffersShareMemoryCorrectly() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id

            // Create TWO frame buffers for the exact same stream
            val buffer1 = frameGraph.captureWith(setOf(streamId), capacity = 3)
            val buffer2 = frameGraph.captureWith(setOf(streamId), capacity = 3)
            advanceUntilIdle()

            // Simulate 1 frame
            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // Both buffers have a reference to the same underlying frame
            assertThat(buffer1.size.value).isEqualTo(1)
            assertThat(buffer2.size.value).isEqualTo(1)

            // Only 1 image is actually allocated!
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            // And it is 100% evictable because it's only held in internal buffers
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)

            // If the app acquires from buffer1, it hides the memory
            val acquired1 = buffer1.removeFirst()

            // Even though buffer2 still holds an internal reference, the app holds
            // an external lease, so the EvictionManager must not touch it!
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Once the app returns its external lease...
            acquired1?.close()
            advanceUntilIdle()

            // ...buffer2 STILL holds its internal reference.
            // Therefore, the memory is NOT freed; it goes back to being evictable.
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)

            // Finally, clear the second buffer
            buffer2.close()
            advanceUntilIdle()

            // Now all references (external and internal) are gone, and memory is freed.
            assertThat(estimator.usage.value).isEqualTo(0L)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            buffer1.close()
        }

    @Test
    fun frameCaptureHidesMemoryFromEviction() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id

            // Issue a single capture request (non-repeating) directly to the graph
            val request = Request(streams = listOf(streamId))
            val frameCapture = frameGraph.capture(request)
            advanceUntilIdle()

            // Simulate the frame and image
            val simulatedFrame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            simulatedFrame.simulateImage(streamId)
            advanceUntilIdle()

            val frame = frameCapture.awaitFrame()
            assertThat(frame).isNotNull()

            // Frame meant for explicit capture is for extern use, and thus it is not evictable.
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Close the FrameCapture object.
            frameCapture.close()

            // Close the frame.
            frame?.close()
            advanceUntilIdle()

            // Once the app closes the captured frame, the memory is completely freed
            assertThat(estimator.usage.value).isEqualTo(0L)
            assertThat(estimator.evictable.value).isEqualTo(0L)
        }

    @Test
    fun rapidAcquireAndCloseDoesNotLeak() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Simulate 1 frame
            val frame = frameGraph.simulateNextFrame()
            advanceUntilIdle()
            frame.simulateImage(streamId)
            advanceUntilIdle()

            // Stress test the state transitions
            repeat(100) {
                val acquired = frameBuffer.peekFirstReference()?.tryAcquire()
                acquired?.close()
            }
            advanceUntilIdle()

            // Estimator should be back to initial state (1 frame in buffer, 1 evictable)
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)
        }

    @Test
    fun independentStreamsHaveIndependentAccounting() =
        testScope.runTest {
            val streamA = frameGraph.streams[streamConfigLarge]!!.id
            val streamB = frameGraph.streams[streamConfigSmall]!!.id

            val bufferA = frameGraph.captureWith(setOf(streamA), capacity = 5)
            val bufferB = frameGraph.captureWith(setOf(streamB), capacity = 5)
            advanceUntilIdle()

            // 1. Simulate image for A
            val frameA = frameGraph.simulateNextFrame()
            frameA.simulateImage(streamA)
            advanceUntilIdle()

            // 2. Simulate image for B
            val frameB = frameGraph.simulateNextFrame()
            frameB.simulateImage(streamB)
            advanceUntilIdle()

            // Both are in the evictable pool
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize + smallImageSize)

            // Acquiring A should only hide A's memory
            val acquiredA = bufferA.peekFirstReference()?.tryAcquire()

            // Memory state:
            // A's memory is hidden (0 evictable)
            // B's memory is still evictable
            assertThat(estimator.evictable.value).isEqualTo(smallImageSize)

            acquiredA?.close()
            advanceUntilIdle()

            // Verify restoration
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize + smallImageSize)

            bufferA.close()
            bufferB.close()
        }

    @Test
    fun mixedBufferAndCaptureUsageAccounting() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id

            // 1. Attach a FrameBuffer
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Simulate a Frame for the buffer.
            val frame1 = frameGraph.simulateNextFrame()
            frame1.simulateImage(streamId)
            advanceUntilIdle()

            // 2. Start an explicit capture request
            val request = Request(streams = listOf(streamId))
            val frameCapture = frameGraph.capture(request)
            advanceUntilIdle()

            // Simulate a Frame for explicit capture.
            val frame2 = frameGraph.simulateNextFrame()
            frame2.simulateImage(streamId)
            advanceUntilIdle()
            val capturedFrame = frameCapture.awaitFrame()
            advanceUntilIdle()

            // Memory State Check:
            // Usage = (2 * largeImageSize)
            // Buffer frame = 1 evictable image
            // Capture frame = 1 non-evictable image (acquired by request)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 2)

            // 3. Clean up
            frameCapture.close()
            capturedFrame?.close()
            frameBuffer.close()
            advanceUntilIdle()

            // Everything freed
            assertThat(estimator.usage.value).isEqualTo(0L)
            assertThat(estimator.evictable.value).isEqualTo(0L)
        }

    @Test
    fun exceedingCapacityHandlesAllocationCorrectly() =
        testScope.runTest {
            // Restrict the global capacity to exactly ONE large image
            val tightEstimator = MemoryEstimator.create(largeImageSize)
            val tightSimulator = createSimulator(tightEstimator)
            val tightGraph = createAndStartFrameGraph(tightSimulator)

            val streamId = tightGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = tightGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // 1. Simulate the first frame (Should succeed)
            val frame1 = tightGraph.simulateNextFrame()
            frame1.simulateImage(streamId)
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(1)
            assertThat(tightEstimator.usage.value).isEqualTo(largeImageSize) // Completely full

            // 2. Simulate a second frame when memory is full
            val frame2 = tightGraph.simulateNextFrame()
            frame2.simulateImage(streamId)
            advanceUntilIdle()

            // Assert the expected behavior when memory is exhausted:
            // Both frames enter the buffer...
            assertThat(frameBuffer.size.value).isEqualTo(2)

            // ...The first frame successfully acquired the image.
            val acquiredFrame1 = frameBuffer.tryPeekFirst()
            assertThat(acquiredFrame1).isNotNull()
            assertThat(acquiredFrame1!!.getImage(streamId)).isNotNull()

            // ...The second frame was created, but its image was dropped (set to null)
            // because the estimator denied the allocation.
            val acquiredFrame2 = frameBuffer.tryPeekLast()
            assertThat(acquiredFrame2).isNotNull()
            assertThat(acquiredFrame2!!.getImage(streamId)).isNull()

            // The estimator usage should remain at exactly largeImageSize
            assertThat(tightEstimator.usage.value).isEqualTo(largeImageSize)

            // Clean up resources
            acquiredFrame1.close()
            acquiredFrame2.close()
            frameBuffer.close()
            tightGraph.close()
            tightSimulator.close()
        }

    @Test
    fun reducingCapacityEvictsOldestFrameAndRestoresMemory() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id

            // Start with a capacity of 5
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Simulate 2 frames entering the buffer
            repeat(2) {
                val frame = frameGraph.simulateNextFrame()
                frame.simulateImage(streamId)
                advanceUntilIdle()
            }

            assertThat(frameBuffer.size.value).isEqualTo(2)

            // Memory should reflect 2 allocated, 2 evictable images
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 2)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 2)

            // Reduce the capacity to 1.
            // According to FrameBuffer docs, this should evict the oldest FrameReference
            // and internally close its associated Frame.
            frameBuffer.capacity = 1
            advanceUntilIdle()

            assertThat(frameBuffer.size.value).isEqualTo(1)

            // Because the oldest frame was evicted and closed internally,
            // the memory for 1 image should be completely restored.
            assertThat(estimator.usage.value).isEqualTo(largeImageSize)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize)

            frameBuffer.close()
        }

    @Test
    fun outOfOrderFrameRemovalTracksMemoryCorrectly() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id
            val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)
            advanceUntilIdle()

            // Simulate 3 frames entering the buffer
            repeat(3) {
                val frame = frameGraph.simulateNextFrame()
                frame.simulateImage(streamId)
                advanceUntilIdle()
            }

            assertThat(frameBuffer.size.value).isEqualTo(3)
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 3)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 3)

            // App removes and acquires the FIRST and LAST frames, leaving the middle frame in the
            // buffer.
            val frame1 = frameBuffer.removeFirst()
            val frame3 = frameBuffer.removeLast()

            assertThat(frame1).isNotNull()
            assertThat(frame3).isNotNull()
            assertThat(frameBuffer.size.value).isEqualTo(1)

            // Usage is still 3, but now only 1 frame is evictable (the middle one)
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 3)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 1)

            // Close the LAST frame first
            frame3?.close()
            advanceUntilIdle()

            // Memory for the last frame is completely freed
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 2)
            assertThat(estimator.evictable.value).isEqualTo(largeImageSize * 1)

            // Close the buffer (this destroys the middle frame)
            frameBuffer.close()
            advanceUntilIdle()

            // Buffer frame destroyed, but frame1 is still held externally
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 1)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Close the FIRST frame
            frame1?.close()
            advanceUntilIdle()

            // Everything is freed
            assertThat(estimator.usage.value).isEqualTo(0L)
            assertThat(estimator.evictable.value).isEqualTo(0L)
        }

    @Test
    fun multipleOverlappingCapturesTrackMemoryCorrectly() =
        testScope.runTest {
            val streamId = frameGraph.streams[streamConfigLarge]!!.id

            // Issue 3 separate capture requests simultaneously
            val captures =
                listOf(
                    frameGraph.capture(Request(streams = listOf(streamId))),
                    frameGraph.capture(Request(streams = listOf(streamId))),
                    frameGraph.capture(Request(streams = listOf(streamId))),
                )
            advanceUntilIdle()

            // Simulate 3 frames from the camera
            repeat(3) {
                val frame = frameGraph.simulateNextFrame()
                frame.simulateImage(streamId)
                advanceUntilIdle()
            }

            // Wait for all 3 captures to resolve
            val frames = captures.mapNotNull { it.awaitFrame() }
            assertThat(frames.size).isEqualTo(3)

            // Since these are explicit captures, none of them are evictable
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 3)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Close Capture/Frame 1
            captures[0].close()
            frames[0].close()
            advanceUntilIdle()

            // 1 freed, 2 remaining
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 2)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Close Capture/Frame 3 (out of order)
            captures[2].close()
            frames[2].close()
            advanceUntilIdle()

            // 2 freed, 1 remaining
            assertThat(estimator.usage.value).isEqualTo(largeImageSize * 1)
            assertThat(estimator.evictable.value).isEqualTo(0L)

            // Close Capture/Frame 2
            captures[1].close()
            frames[1].close()
            advanceUntilIdle()

            // All freed
            assertThat(estimator.usage.value).isEqualTo(0L)
            assertThat(estimator.evictable.value).isEqualTo(0L)
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
