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
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.FrameGraph
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * End-to-end tests verifying the CameraPipeResourceTrimmer and GraphResourceTrimmer correctly evict
 * the oldest frames from active FrameBuffers when memory bounds and stream capacities are reached
 * to make room for new requests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class ResourceTrimmerTest {
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

    private val graphConfig =
        CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfigLarge))

    private val largeImageSize = StreamFormat.bytesPerImage(StreamFormat.YUV_420_888, 1280, 720)

    private lateinit var estimator: MemoryEstimator
    private lateinit var simulator: CameraPipeSimulator
    private lateinit var frameGraph: FrameGraphSimulator

    @After
    fun teardown() {
        if (this::frameGraph.isInitialized) frameGraph.close()
        if (this::simulator.isInitialized) simulator.close()
    }

    @Test
    fun trimmingIsSkippedIfSufficientMemoryExists() = testScope.runTest {
        // Give plenty of capacity
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 10))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        repeat(2) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        // Flush queues right before the assertion
        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(2)

        val capture = frameGraph.capture(Request(streams = listOf(streamId)))

        advanceUntilIdle()
        // Trimmer runs, but sees memory is sufficient. It should NOT evict anything.
        assertThat(frameBuffer.size.value).isEqualTo(2)

        capture.close()
    }

    @Test
    fun trimmingEvictsOldestFrameWhenMemoryIsFull() = testScope.runTest {
        // Restrict capacity to exactly 1 frame + margin
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 1))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        // 1st frame - Free memory exactly equals the margin
        val f1 = frameGraph.simulateNextFrame()
        f1.simulateImage(streamId)

        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(1)

        // 2nd frame - Eats into the margin. The background trimmer has not woken up yet.
        val f2 = frameGraph.simulateNextFrame()
        f2.simulateImage(streamId)

        // 3rd frame - Wakes up the trimmer, which evicts the oldest frame to make room.
        val f3 = frameGraph.simulateNextFrame()
        advanceUntilIdle()
        f3.simulateImage(streamId)

        advanceUntilIdle()
        // Buffer stays at 2 (f2 and f3) because the oldest frame (f1) was evicted.
        assertThat(frameBuffer.size.value).isEqualTo(2)
        assertThat(estimator.memoryUsage.value).isEqualTo(largeImageSize * 2)
    }

    @Test
    fun trimmingRoundRobinsAcrossMultipleBuffers() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 1))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val buffer1 = frameGraph.captureWith(setOf(streamId), capacity = 5)
        val buffer2 = frameGraph.captureWith(setOf(streamId), capacity = 5)

        // 1st frame
        val f1 = frameGraph.simulateNextFrame()
        f1.simulateImage(streamId)
        advanceUntilIdle()

        assertThat(buffer1.size.value).isEqualTo(1)
        assertThat(buffer2.size.value).isEqualTo(1)

        // 2nd frame eats into the margin
        val f2 = frameGraph.simulateNextFrame()
        f2.simulateImage(streamId)
        advanceUntilIdle()

        // 3rd frame wakes up the trimmer
        val f3 = frameGraph.simulateNextFrame()
        advanceUntilIdle()
        f3.simulateImage(streamId)
        advanceUntilIdle()

        // Trimmer round-robins and removes f1 from BOTH buffers fairly to release the
        // underlying memory.
        // Both buffers will now hold f2 and f3.
        assertThat(buffer1.size.value).isEqualTo(2)
        assertThat(buffer2.size.value).isEqualTo(2)
        assertThat(estimator.memoryUsage.value).isEqualTo(largeImageSize * 2)

        buffer1.close()
        buffer2.close()
    }

    @Test
    fun trimmingFailsToFreeMemoryIfFramesAreAcquiredExternally() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 2))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        // Fill memory
        repeat(2) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()
        // The app explicitly ACQUIRES both frames. They are no longer evictable.
        val acquiredFrame1 = frameBuffer.removeFirst()
        val acquiredFrame2 = frameBuffer.removeFirst()
        assertThat(acquiredFrame1).isNotNull()
        assertThat(acquiredFrame2).isNotNull()

        // Queue a new capture
        val capture = frameGraph.capture(Request(streams = listOf(streamId)))

        // Simulate the capture frame
        val f3 = frameGraph.simulateNextFrame()
        f3.simulateImage(streamId)

        advanceUntilIdle()
        // The trimmer ran, but since the frames were acquired externally, it cannot free the space.
        // Usage is now 3.
        assertThat(estimator.memoryUsage.value).isEqualTo(largeImageSize * 3)

        acquiredFrame1?.close()
        acquiredFrame2?.close()
        capture.close()
    }

    @Test
    fun trimmingDeprioritizesRecentlyActiveGraphAndTrimsFromOlderGraphsFirst() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT.toLong()
        // Set capacity to exactly 2 frames + margin of 2 graphs
        estimator = MemoryEstimator.create(largeImageSize * (margin * 2 + 2))

        val metadata2 = FakeCameraMetadata(cameraId = CameraId("2"))
        simulator =
            CameraPipeSimulator.create(
                testScope,
                context,
                listOf(metadata, metadata2),
                estimator,
            )

        val graphConfig2 =
            CameraGraph.Config(camera = metadata2.camera, streams = listOf(streamConfigLarge))

        val fg1 = simulator.createFrameGraph(FrameGraph.Config(graphConfig))
        val fg2 = simulator.createFrameGraph(FrameGraph.Config(graphConfig2))

        // Start FG1 first!
        fg1.start()
        fg1.initializeSurfaces()
        fg1.simulateCameraStarted()

        // FG2 starts second. It becomes the "recentlyActiveTrimmer" and gets the protection shield.
        fg2.start()
        fg2.initializeSurfaces()
        fg2.simulateCameraStarted()

        val stream1 = fg1.streams[streamConfigLarge]!!.id
        val stream2 = fg2.streams[streamConfigLarge]!!.id

        val buffer1 = fg1.captureWith(setOf(stream1), capacity = 5)
        val buffer2 = fg2.captureWith(setOf(stream2), capacity = 5)

        // 1 frame in FG1
        val f1 = fg1.simulateNextFrame()
        f1.simulateImage(stream1)

        // 1 frame in FG2
        val f2 = fg2.simulateNextFrame()
        f2.simulateImage(stream2)

        advanceUntilIdle()
        assertThat(buffer1.size.value).isEqualTo(1)
        assertThat(buffer2.size.value).isEqualTo(1)

        // 2nd frame in FG2 breaches the global margin
        val f3 = fg2.simulateNextFrame()
        f3.simulateImage(stream2)

        // 4th frame wakes up the trimmer
        fg2.simulateNextFrame()

        advanceUntilIdle()
        // Because of Protected Round-Robin, FG2 is completely shielded during the first pass.
        // The trimmer must penalize the older graph (FG1) to make room.
        assertThat(buffer1.size.value).isEqualTo(0) // FG1 lost its frame
        assertThat(buffer2.size.value).isEqualTo(3) // FG2 kept its frames and added the new ones

        buffer1.close()
        buffer2.close()
        fg1.close()
        fg2.close()
    }

    @Test
    fun trimmingFallsBackToNextGraphIfFirstCannotFreeEnoughMemory() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        estimator = MemoryEstimator.create(largeImageSize * (margin * 2 + 2))

        val metadata2 = FakeCameraMetadata(cameraId = CameraId("2"))
        simulator =
            CameraPipeSimulator.create(
                testScope,
                context,
                listOf(metadata, metadata2),
                estimator,
            )

        val graphConfig2 =
            CameraGraph.Config(camera = metadata2.camera, streams = listOf(streamConfigLarge))

        val fg1 = simulator.createFrameGraph(FrameGraph.Config(graphConfig))
        val fg2 = simulator.createFrameGraph(FrameGraph.Config(graphConfig2))

        fg1.start()
        fg1.initializeSurfaces()
        fg1.simulateCameraStarted()

        fg2.start()
        fg2.initializeSurfaces()
        fg2.simulateCameraStarted()

        val stream1 = fg1.streams[streamConfigLarge]!!.id
        val stream2 = fg2.streams[streamConfigLarge]!!.id

        val buffer1 = fg1.captureWith(setOf(stream1), capacity = 5)
        val buffer2 = fg2.captureWith(setOf(stream2), capacity = 5)

        // 2 frames in FG2, 0 frames in FG1
        repeat(2) {
            val f = fg2.simulateNextFrame()
            f.simulateImage(stream2)
        }

        advanceUntilIdle()
        assertThat(buffer1.size.value).isEqualTo(0)
        assertThat(buffer2.size.value).isEqualTo(2)

        // 3rd frame in FG2 breaches the margin
        val f3 = fg2.simulateNextFrame()
        f3.simulateImage(stream2)

        // 4th frame wakes up the trimmer
        fg2.simulateNextFrame()

        advanceUntilIdle()
        // FG1 is asked to trim first (because it's older), but it has 0 frames in its buffer.
        // It returns false. The trimmer then asks FG2 which successfully evicts 1 frame.
        assertThat(buffer1.size.value).isEqualTo(0)
        assertThat(buffer2.size.value).isEqualTo(3)

        buffer1.close()
        buffer2.close()
        fg1.close()
        fg2.close()
    }

    @Test
    fun trimmerRemainsRegisteredAfterGraphClosesIfBuffersAreActive() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        estimator = MemoryEstimator.create(largeImageSize * (margin + 2))

        val metadata2 = FakeCameraMetadata(cameraId = CameraId("2"))
        simulator =
            CameraPipeSimulator.create(
                testScope,
                context,
                listOf(metadata, metadata2),
                estimator,
            )

        val graphConfig2 =
            CameraGraph.Config(camera = metadata2.camera, streams = listOf(streamConfigLarge))

        val fg1 = simulator.createFrameGraph(FrameGraph.Config(graphConfig))
        val fg2 = simulator.createFrameGraph(FrameGraph.Config(graphConfig2))

        fg1.start()
        fg1.initializeSurfaces()
        fg1.simulateCameraStarted()

        val stream1 = fg1.streams[streamConfigLarge]!!.id
        val buffer1 = fg1.captureWith(setOf(stream1), capacity = 5)

        // 2 frames in Graph 1
        repeat(2) {
            val f = fg1.simulateNextFrame()
            f.simulateImage(stream1)
        }

        // Close Graph 1. The graph is dead, but the app still holds `buffer1`.
        // FG1 drops its proactive margin contribution to 0.
        fg1.close()

        // Start Graph 2. It introduces its own proactive margin.
        fg2.start()
        fg2.initializeSurfaces()
        fg2.simulateCameraStarted()

        val stream2 = fg2.streams[streamConfigLarge]!!.id

        // ATTACH a buffer to FG2 so it creates a repeating request!
        val buffer2 = fg2.captureWith(setOf(stream2), capacity = 5)

        // Simulating a frame in FG2 breaches the memory margin
        val f3 = fg2.simulateNextFrame()
        f3.simulateImage(stream2)

        // Wake up the trimmer
        fg2.simulateNextFrame()

        advanceUntilIdle()
        // Because `buffer1` is still active, Graph 1's trimmer delayed its unregistration.
        // It successfully evicts a frame from `buffer1` to make room.
        assertThat(buffer1.size.value).isEqualTo(1)

        buffer1.close()
        buffer2.close()
        fg2.close()
    }

    @Test
    fun trimmingEvictsOnlyEnoughFramesToSatisfyMemoryRequirement() = testScope.runTest {
        val streamConfigSmall =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
            )
        val smallImageSize = StreamFormat.bytesPerImage(StreamFormat.YUV_420_888, 640, 480)

        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        // Set capacity exactly enough for the repeating margin (3 large images) + 2 large
        // images
        estimator = MemoryEstimator.create(largeImageSize * (margin + 2))

        val mixedGraphConfig =
            CameraGraph.Config(
                camera = metadata.camera,
                streams = listOf(streamConfigLarge, streamConfigSmall),
            )

        simulator = CameraPipeSimulator.create(testScope, context, listOf(metadata), estimator)
        val fg = simulator.createFrameGraph(FrameGraph.Config(mixedGraphConfig))
        fg.start()
        fg.initializeSurfaces()
        fg.simulateCameraStarted()

        val largeStreamId = fg.streams[streamConfigLarge]!!.id
        val smallStreamId = fg.streams[streamConfigSmall]!!.id

        // The repeating request only contains the large stream, so the margin is 3 *
        // largeImageSize
        val largeBuffer = fg.captureWith(setOf(largeStreamId), capacity = 5)

        // Fill buffer with 2 large images.
        // Usage is now 2 large images. Free memory exactly equals the margin.
        repeat(2) {
            val f = fg.simulateNextFrame()
            f.simulateImage(largeStreamId)
        }

        advanceUntilIdle()
        assertThat(largeBuffer.size.value).isEqualTo(2)

        // Simulate a 3rd frame but only provide a SMALL image.
        // 1. The new frame enters the largeBuffer (size becomes 3).
        // 2. Memory allocates the small image, pushing total usage to (2L + 1S).
        // 3. Free memory is now strictly LESS than the margin.
        val f3 = fg.simulateNextFrame()
        f3.simulateImage(smallStreamId)

        // 4th frame wakes up the trimmer, which evicts EXACTLY 1 large frame to restore the
        // margin.
        fg.simulateNextFrame()

        advanceUntilIdle()
        // 4 frames entered the buffer, 1 oldest large frame was evicted -> 3 remain.
        assertThat(largeBuffer.size.value).isEqualTo(3)

        // Usage should reflect 1 large frame + 1 small frame remaining
        assertThat(estimator.memoryUsage.value).isEqualTo(largeImageSize + smallImageSize)

        largeBuffer.close()
        fg.close()
    }

    @Test
    fun trimmingIsSkippedForRequestsWithZeroBytesNeeded() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 2))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        repeat(2) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(2)

        // Queue a capture request with an unrecognized StreamId (0 bytes needed)
        val dummyStreamId = StreamId(999)
        val capture = frameGraph.capture(Request(streams = listOf(dummyStreamId)))

        advanceUntilIdle()
        // No memory was needed, so the trimmer immediately returns true.
        assertThat(frameBuffer.size.value).isEqualTo(2)

        capture.close()
    }

    @Test
    fun trimmingDropsNewImageWhenImageReaderSlotsAreFull() = testScope.runTest {
        // Memory is practically unbounded
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 10))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        // maxImages is exactly 5.
        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 10)

        // Fill physical slots completely (5 frames)
        repeat(5) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(5)

        // Simulate arrival of a 6th image.
        // Since maxImages is 5, the new image will be dropped. The frame itself
        // is still recorded in the buffer.
        val f6 = frameGraph.simulateNextFrame()
        f6.simulateImage(streamId)

        advanceUntilIdle()
        // Buffer size grows to 6 because the frame metadata is still valid.
        assertThat(frameBuffer.size.value).isEqualTo(6)

        // Verify the 6th frame has dropped image status
        val frames = frameBuffer.removeAll()
        assertThat(frames.last().imageStatus(streamId)).isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)
        frames.forEach { it.close() }
    }

    @Test
    fun trimmingDropsNewImageIfSlotsAreFullAndFramesAreAcquiredExternally() = testScope.runTest {
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 10))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 10)

        // Fill physical slots completely (5 frames)
        repeat(5) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()
        // The app explicitly ACQUIRES all 5 frames. They are now locked.
        val acquiredFrames = frameBuffer.removeAll()
        assertThat(acquiredFrames).hasSize(5)

        // Simulate arrival of a 6th image.
        // Slots are full, and no frames are evictable. New image MUST be dropped.
        val f6 = frameGraph.simulateNextFrame()
        f6.simulateImage(streamId)

        advanceUntilIdle()
        // Buffer holds the 6th frame, but its image was dropped.
        assertThat(frameBuffer.size.value).isEqualTo(1)
        val droppedFrame = frameBuffer.removeFirst()
        assertThat(droppedFrame).isNotNull()
        assertThat(droppedFrame!!.imageStatus(streamId))
            .isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)

        // Clean up
        droppedFrame.close()
        acquiredFrames.forEach { it.close() }
    }

    @Test
    fun trimmingMarginUpdatesDynamicallyWhenRepeatingRequestChanges() = testScope.runTest {
        val streamConfigSmall =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 5),
            )

        val mixedGraphConfig =
            CameraGraph.Config(
                camera = metadata.camera,
                streams = listOf(streamConfigLarge, streamConfigSmall),
            )

        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT

        // Set capacity exactly enough for the LARGE margin + 2 large images
        val capacityBytes = (largeImageSize * margin) + (largeImageSize * 2)
        estimator = MemoryEstimator.create(capacityBytes)

        simulator = CameraPipeSimulator.create(testScope, context, listOf(metadata), estimator)
        val fg = simulator.createFrameGraph(FrameGraph.Config(mixedGraphConfig))
        fg.start()
        fg.initializeSurfaces()
        fg.simulateCameraStarted()

        val largeStreamId = fg.streams[streamConfigLarge]!!.id
        val smallStreamId = fg.streams[streamConfigSmall]!!.id

        // Step 1: Start with ONLY the large stream in the buffer/repeating request.
        val largeBuffer = fg.captureWith(setOf(largeStreamId), capacity = 5)

        // Fill buffer with 2 large images. The trimmer is happy because remaining
        // memory equals (margin * largeImageSize).
        repeat(2) {
            val f = fg.simulateNextFrame()
            f.simulateImage(largeStreamId)
        }

        advanceUntilIdle()
        assertThat(largeBuffer.size.value).isEqualTo(2)

        // Step 2: ATTACH the small stream to the buffer.
        // This updates the repeating request to now include BOTH large and small streams.
        // The trimmer should recalculate its required margin to: margin * (large + small)
        val dualBuffer = fg.captureWith(setOf(largeStreamId, smallStreamId), capacity = 5)

        advanceUntilIdle()
        // Because the required margin just increased, the current free memory is no longer
        // enough!
        // The trimmer should proactively wake up and evict from `largeBuffer` to satisfy
        // the new, larger margin requirement—even before any new frames arrive.
        assertThat(largeBuffer.size.value).isEqualTo(1)

        largeBuffer.close()
        dualBuffer.close()
        fg.close()
    }

    @Test
    fun trimmingProactivelyMaintainsRepeatingFrameMargin() = testScope.runTest {
        // Memory capacity: Exactly enough for the 3-frame margin + 2 extra frames.
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 2))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        // Creating the FrameBuffer creates the repeating request, establishing the margin.
        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        // Feed 2 frames in. Memory usage = 2.
        // Remaining capacity = margin. The trimmer is satisfied and does nothing.
        repeat(2) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(2)
        assertThat(estimator.memoryUsage.value).isEqualTo(largeImageSize * 2)

        // Feed a 3rd frame in.
        // Memory usage briefly hits 3. Remaining capacity drops BELOW the required margin.
        val f3 = frameGraph.simulateNextFrame()
        f3.simulateImage(streamId)

        // Trigger the trimmer
        frameGraph.simulateNextFrame()

        advanceUntilIdle()
        // Buffer size is 3 because 1 older frame was evicted to accommodate f4.
        assertThat(frameBuffer.size.value).isEqualTo(3)

        frameBuffer.close()
    }

    @Test
    fun trimmingCombinesBurstAndRepeatingMargin() = testScope.runTest {
        // Capacity: Exactly enough for the 3-frame margin + 2 extra frames (5 total)
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 2))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        // Step 1: Fill buffer with 2 frames.
        // Total capacity (5) - Usage (2) = 3 frames of Free Memory.
        // The trimmer is happy because Free Memory (3) exactly equals the required margin (3).
        repeat(2) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(2)

        // Step 2: Queue a 2-frame burst.
        // Target = 2 (burst) + 3 (margin) = 5 frames needed!
        // Free memory is only 3. It MUST evict 2 older frames.
        val burstRequest1 = Request(streams = listOf(streamId))
        val burstRequest2 = Request(streams = listOf(streamId))

        // Queueing captures automatically triggers `onRequestsQueued` inside CameraPipe
        val capture1 = frameGraph.capture(burstRequest1)
        val capture2 = frameGraph.capture(burstRequest2)

        advanceUntilIdle()
        // Step 3: Assert the buffer was aggressively cleared.
        // Both old frames should be gone, proving the system made room for the upcoming
        // burst frames WHILE protecting the 3-frame margin.
        assertThat(frameBuffer.size.value).isEqualTo(0)

        capture1.close()
        capture2.close()
    }

    @Test
    fun trimmerUnregistersWhenGraphAndBuffersAreClosed() = testScope.runTest {
        // Set capacity for EXACTLY 1 graph's margin (3) + 1 extra frame (4 total)
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        estimator = MemoryEstimator.create(largeImageSize * (margin + 1))

        val metadata2 = FakeCameraMetadata(cameraId = CameraId("2"))
        simulator =
            CameraPipeSimulator.create(
                testScope,
                context,
                listOf(metadata, metadata2),
                estimator,
            )

        val graphConfig2 =
            CameraGraph.Config(camera = metadata2.camera, streams = listOf(streamConfigLarge))

        val fg1 = simulator.createFrameGraph(FrameGraph.Config(graphConfig))
        val fg2 = simulator.createFrameGraph(FrameGraph.Config(graphConfig2))

        // Start both graphs.
        // Total margin required by system is now 6 frames!
        fg1.start()
        fg1.initializeSurfaces()
        fg1.simulateCameraStarted()

        fg2.start()
        fg2.initializeSurfaces()
        fg2.simulateCameraStarted()

        val stream1 = fg1.streams[streamConfigLarge]!!.id
        val stream2 = fg2.streams[streamConfigLarge]!!.id

        val buffer1 = fg1.captureWith(setOf(stream1), capacity = 5)
        val buffer2 = fg2.captureWith(setOf(stream2), capacity = 5)

        // At this point, the system needs 6 frames of margin, but only has 4 frames total.
        // If we try to capture anything, the trimmer will aggressively evict it.

        // Now, close Graph 1 AND its buffer.
        // Because both the graph and the buffer are closed, it should completely
        // unregister from the global trimmer.
        // The global margin requirement should drop back down to 3 frames!
        fg1.close()
        buffer1.close()

        // Simulate 1 frame arriving in Graph 2.
        // Usage = 1. Free memory = 3.
        // If Graph 1 properly unregistered, required margin is 3, so Trimmer does nothing.
        // If Graph 1 leaked, required margin is 6, and Trimmer will instantly delete this
        // frame.
        val f1 = fg2.simulateNextFrame()
        f1.simulateImage(stream2)

        advanceUntilIdle()
        // The frame survived! This proves Graph 1 successfully unregistered
        // and released its system-wide margin constraints.
        assertThat(buffer2.size.value).isEqualTo(1)

        buffer2.close()
        fg2.close()
    }

    @Test
    fun streamCapacityTrimmingIsolatesIndependentStreams() = testScope.runTest {
        // Provide massive memory capacity so the global memory trimmer doesn't interfere.
        setupSimulators(largeImageSize * 100)

        val streamConfigViewfinder =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 2), // Low capacity
            )

        val mixedGraphConfig =
            CameraGraph.Config(
                camera = metadata.camera,
                streams = listOf(streamConfigLarge, streamConfigViewfinder),
            )

        simulator = CameraPipeSimulator.create(testScope, context, listOf(metadata), estimator)
        val fg = simulator.createFrameGraph(FrameGraph.Config(mixedGraphConfig))
        fg.start()
        fg.initializeSurfaces()
        fg.simulateCameraStarted()

        val largeStreamId = fg.streams[streamConfigLarge]!!.id
        val viewfinderStreamId = fg.streams[streamConfigViewfinder]!!.id

        // Attach separate buffers for each stream
        val largeBuffer = fg.captureWith(setOf(largeStreamId), capacity = 5)
        val viewfinderBuffer = fg.captureWith(setOf(viewfinderStreamId), capacity = 5)

        // Fill the viewfinder completely (2 frames), and put 2 frames in the large buffer
        repeat(2) {
            val f = fg.simulateNextFrame()
            f.simulateImages()
        }

        advanceUntilIdle()
        assertThat(largeBuffer.size.value).isEqualTo(2)
        assertThat(viewfinderBuffer.size.value).isEqualTo(2)

        // Queue a capture ONLY for the viewfinder stream.
        // This will push the viewfinder over its slot limit (2 + 1 > 2).
        val capture = fg.capture(Request(streams = listOf(viewfinderStreamId)))

        // Simulate the frame.
        val f3 = fg.simulateNextFrame()
        f3.simulateImages()

        advanceUntilIdle()
        // The trimmer should have evicted 1 frame from the viewfinderBuffer to make room.
        assertThat(viewfinderBuffer.size.value).isEqualTo(2)

        // The largeBuffer should NOT have been trimmed because its stream has plenty of
        // capacity
        // and the global memory budget wasnt breached. It should successfully hold 3 frames.
        assertThat(largeBuffer.size.value).isEqualTo(3)

        capture.close()
        largeBuffer.close()
        viewfinderBuffer.close()
        fg.close()
    }

    @Test
    fun streamCapacityTrimmingWorksWhenMaxImagesIsOne() = testScope.runTest {
        // Provide massive memory capacity so the global memory trimmer doesn't interfere.
        setupSimulators(largeImageSize * 100)

        // Set up a stream with the absolute minimum capacity
        val streamConfigTiny =
            CameraStream.Config.create(
                Size(640, 480),
                StreamFormat.YUV_420_888,
                imageSourceConfig = ImageSourceConfig(capacity = 1), // Max images = 1
            )

        val mixedGraphConfig =
            CameraGraph.Config(camera = metadata.camera, streams = listOf(streamConfigTiny))

        simulator = CameraPipeSimulator.create(testScope, context, listOf(metadata), estimator)
        val fg = simulator.createFrameGraph(FrameGraph.Config(mixedGraphConfig))
        fg.start()
        fg.initializeSurfaces()
        fg.simulateCameraStarted()

        val tinyStreamId = fg.streams[streamConfigTiny]!!.id
        val frameBuffer = fg.captureWith(setOf(tinyStreamId), capacity = 5)

        // Fill the physical slots completely (1 frame)
        val f1 = fg.simulateNextFrame()
        f1.simulateImages()

        advanceUntilIdle()
        assertThat(frameBuffer.size.value).isEqualTo(1)

        // Queue exactly 1 capture.
        // The trimmer will calculate: slotsToFree = (1 current + 1 required) - 1 max = 1.
        val capture = fg.capture(Request(streams = listOf(tinyStreamId)))

        // Wakes up the trimmer, which must evict the ONLY frame in the buffer to make room.
        val f2 = fg.simulateNextFrame()
        f2.simulateImages()

        advanceUntilIdle()
        // The buffer size drops to 0 temporarily during trim, then f2 is added, so it's 1.
        assertThat(frameBuffer.size.value).isEqualTo(1)

        // Verify the only frame in the buffer is indeed the new one (f2)
        val finalFrames = frameBuffer.removeAll()
        assertThat(finalFrames.single().frameNumber).isEqualTo(f2.frameNumber)

        capture.close()
        finalFrames.forEach { it.close() }
        fg.close()
    }

    @Test
    fun closingAllBuffersClearsRepeatingMarginDynamically() = testScope.runTest {
        // Capacity: Exactly enough for the 3-frame margin + 2 extra frames (5 total)
        val margin = CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
        setupSimulators(largeImageSize * (margin + 2))
        val streamId = frameGraph.streams[streamConfigLarge]!!.id

        val frameBuffer = frameGraph.captureWith(setOf(streamId), capacity = 5)

        // 1. Fill the buffer with 2 frames
        repeat(2) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)
        }

        advanceUntilIdle()

        // 2. App acquires the frames explicitly so they cannot be evicted
        val acquired1 = frameBuffer.removeFirst()
        val acquired2 = frameBuffer.removeFirst()
        assertThat(acquired1).isNotNull()
        assertThat(acquired2).isNotNull()

        // 3. Close the buffer (but leave the graph OPEN)
        // Because of the new `onRepeatingRequestUpdated(null)` line, the 3-frame margin
        // requirement should instantly drop to 0!
        frameBuffer.close()

        // 4. Queue a 3-frame burst request
        // If the 3-frame margin was NOT cleared, this burst would require 8 frames of capacity
        // (2 acquired + 3 burst + 3 phantom margin) and would fail/trim.
        // Because the margin WAS cleared, it only requires 5 frames (2 acquired + 3 burst),
        // which exactly fits our global capacity!
        val burstCaptures = List(3) { frameGraph.capture(Request(streams = listOf(streamId))) }

        val burstFrames = mutableListOf<androidx.camera.camera2.pipe.Frame>()
        for (capture in burstCaptures) {
            val f = frameGraph.simulateNextFrame()
            f.simulateImage(streamId)

            // Note: Required before the suspending assertion to ensure queues are flushed.
            advanceUntilIdle()

            val captured = capture.awaitFrame()
            assertThat(captured).isNotNull()
            assertThat(captured!!.getImage(streamId)).isNotNull() // Successfully allocated!
            burstFrames.add(captured)
        }

        // Clean up
        burstFrames.forEach { it.close() }
        burstCaptures.forEach { it.close() }
        acquired1?.close()
        acquired2?.close()
    }

    private fun setupSimulators(capacity: Long) {
        estimator = MemoryEstimator.create(capacity)
        simulator = CameraPipeSimulator.create(testScope, context, listOf(metadata), estimator)
        frameGraph = simulator.createFrameGraph(FrameGraph.Config(graphConfig))
        frameGraph.start()
        frameGraph.initializeSurfaces()
        frameGraph.simulateCameraStarted()
        testScope.advanceUntilIdle()
    }
}
