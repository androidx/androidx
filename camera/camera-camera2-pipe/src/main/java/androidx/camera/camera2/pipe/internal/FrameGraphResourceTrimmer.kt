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

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.MemoryEstimator
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.framegraph.FrameBufferImpl
import androidx.camera.camera2.pipe.media.ImageReaderImageSource
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic

/**
 * A [CameraGraphScope] resource trimmer that can trim evictable resources from a CameraGraph in
 * order to fulfil the request for new resources.
 *
 * It usually does this by keeping track of the CameraGraph's
 * [androidx.camera.camera2.pipe.FrameBuffer]s, and trimming the Frame(s) from them based on some
 * policy.
 */
internal interface FrameGraphResourceTrimmer : Request.Listener {
    /**
     * Registers a [FrameBufferImpl] with this trimmer. Once attached, the trimmer may proactively
     * drop older frames from this buffer during [trim] or [trimForStreams] operations to free up
     * capacity for new incoming requests.
     */
    fun onFrameBufferAttached(frameBuffer: FrameBufferImpl)

    /**
     * Unregisters a [FrameBufferImpl] from this trimmer. This should be called when the buffer is
     * closed. If this was the last active buffer and the graph is closed, this will also trigger
     * the trimmer to unregister itself from the global [CameraPipeResourceTrimmer].
     */
    fun onFrameBufferDetached(frameBuffer: FrameBufferImpl)

    /**
     * The estimated memory footprint (in bytes) of a single frame from the current repeating
     * request.
     */
    val repeatingFrameSizeBytes: Long

    /**
     * The total estimated memory (in bytes) required to fulfill all currently pending explicit
     * capture requests, plus a safety margin for the active repeating request.
     */
    val currentMemoryRequirement: Long

    /**
     * Updates the trimmer with the latest repeating request to accurately calculate the baseline
     * memory footprint of the active repeating request.
     */
    fun onRepeatingRequestUpdated(request: Request?)

    /**
     * Evaluates the physical capacity of the underlying image sources and proactively drops older
     * frames if necessary to avoid stalling the camera pipeline.
     *
     * For each active stream, this calculates the number of frames required by pending explicit
     * capture requests. If the currently open images plus the required images exceed the
     * [ImageReaderImageSource.maxImages] limit, the pipeline will stall. To prevent this
     * starvation, this method proactively trims the oldest frames belonging to that specific
     * [StreamId] across all active [FrameBufferImpl]s until enough capacity is restored, or we
     * can't trim anymore.
     */
    fun trimForStreams()

    /**
     * Trims a single frame from one of the active buffers to free up global memory.
     *
     * This method cycles through all active buffers using a round-robin approach. It attempts to
     * trim the oldest frame from each buffer one by one until one successfully frees up a frame.
     * This guarantees fairness, ensuring that a single buffer doesn't get completely drained while
     * others retain all their frames.
     *
     * @return `true` if a frame was successfully evicted from any of the buffers, `false` if all
     *   buffers are empty or cannot be trimmed.
     */
    fun trim(): Boolean

    /**
     * Signals the global [CameraPipeResourceTrimmer] to wake up, re-evaluate global memory bounds,
     * and initiate trimming across all camera graphs if the memory budget is exceeded.
     */
    fun invalidate()

    /**
     * Lifecycle callback invoked when the [androidx.camera.camera2.pipe.CameraGraph] is initially
     * created. This registers this trimmer with the global [CameraPipeResourceTrimmer] so its
     * memory boundaries are tracked.
     */
    fun onGraphCreated()

    /**
     * Lifecycle callback invoked when the [androidx.camera.camera2.pipe.CameraGraph] starts
     * actively processing frames. This updates the global [CameraPipeResourceTrimmer] to mark this
     * graph as recently active, which in turn gives it the lowest eviction priority.
     */
    fun onGraphStarted()

    /**
     * Lifecycle callback invoked when the [androidx.camera.camera2.pipe.CameraGraph] is closed.
     * Marks the graph as inactive, zeroes out the repeating frame memory footprint, and unregisters
     * the trimmer from the CameraPipeResourceTrimmer if there are no active frame buffers.
     */
    fun onGraphClosed()
}

internal object NoOpFrameGraphResourceTrimmer : FrameGraphResourceTrimmer {
    override fun onFrameBufferAttached(frameBuffer: FrameBufferImpl) {}

    override fun onFrameBufferDetached(frameBuffer: FrameBufferImpl) {}

    override val repeatingFrameSizeBytes: Long
        get() = 0L

    override val currentMemoryRequirement: Long
        get() = 0L

    override fun onRepeatingRequestUpdated(request: Request?) {}

    override fun trimForStreams() {}

    override fun trim(): Boolean = true

    override fun invalidate() {}

    override fun onGraphCreated() {}

    override fun onGraphStarted() {}

    override fun onGraphClosed() {}
}

@CameraGraphScope
internal class FrameGraphResourceTrimmerImpl
@Inject
constructor(
    private val streamGraph: StreamGraph,
    private val frameCaptureQueue: FrameCaptureQueue,
    private val memoryEstimator: MemoryEstimator,
    private val cameraPipeResourceTrimmer: CameraPipeResourceTrimmer,
) : FrameGraphResourceTrimmer {

    private val lock = Any()

    @GuardedBy("lock") private var isGraphClosed = false

    private val imageReaderSources: Map<StreamId, ImageReaderImageSource> = buildMap {
        for (stream in streamGraph.streams) {
            streamGraph
                .getImageSource(stream.id)
                ?.unwrapAs(ImageReaderImageSource::class.java)
                ?.let { put(stream.id, it) }
        }
    }

    private val activeBuffers = CopyOnWriteArrayList<FrameBufferImpl>()

    private val _repeatingFrameSizeBytes = atomic(0L)

    /**
     * Persistent counter used to ensure fair, round-robin eviction across all active
     * [FrameBufferImpl]s when trimming to free up global memory capacity. This prevents the first
     * buffer in the list from being disproportionately starved during memory pressure.
     */
    private val globalTrimCounter = atomic(0)
    /**
     * Persistent counter used to ensure fair, round-robin eviction when trimming to free up
     * ImageReader slots for a specific [StreamId]. If multiple buffers are attached to the same
     * stream, this ensures they are trimmed evenly.
     */
    private val streamTrimCounter = atomic(0)

    override val repeatingFrameSizeBytes: Long
        get() = _repeatingFrameSizeBytes.value

    override val currentMemoryRequirement: Long
        get() {
            val burstMemoryNeeded = calculateByteSize(frameCaptureQueue.pendingRequests)
            val repeatingMargin =
                repeatingFrameSizeBytes * CameraPipeResourceTrimmer.REPEATING_FRAME_MARGIN_COUNT
            return burstMemoryNeeded + repeatingMargin
        }

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp,
    ) {
        cameraPipeResourceTrimmer.invalidate()
    }

    override fun onFrameBufferAttached(frameBuffer: FrameBufferImpl) {
        activeBuffers.add(frameBuffer)
    }

    override fun onFrameBufferDetached(frameBuffer: FrameBufferImpl) {
        activeBuffers.remove(frameBuffer)
        checkAndUnregister()
    }

    override fun onRepeatingRequestUpdated(request: Request?) {
        val newSize =
            if (request != null) {
                calculateByteSize(listOf(request))
            } else {
                0L
            }

        val previousSize = _repeatingFrameSizeBytes.getAndSet(newSize)
        if (previousSize != newSize) {
            invalidate()
        }
    }

    override fun trimForStreams() {
        val pendingRequests = frameCaptureQueue.pendingRequests
        for ((streamId, imageSource) in imageReaderSources) {
            val requiredImages = pendingRequests.count { it.streams.contains(streamId) }
            val maxImages = imageSource.maxImages
            // Keep trimming across the buffers in a round-robin fashion until the demand is met,
            // or we can no longer trim.
            while (true) {
                val currentOpen = imageSource.openImages.value
                val slotsToFree = (currentOpen + requiredImages) - maxImages
                if (slotsToFree <= 0 || !trimFor(streamId)) {
                    break
                }
            }
        }
    }

    override fun trim(): Boolean {
        return trimNextRoundRobin(globalTrimCounter) { buffer -> buffer.trimFirst() }
    }

    override fun invalidate() {
        cameraPipeResourceTrimmer.invalidate()
    }

    private fun trimFor(streamId: StreamId): Boolean {
        return trimNextRoundRobin(streamTrimCounter) { buffer ->
            buffer.streams.contains(streamId) && buffer.trimFirst()
        }
    }

    private inline fun trimNextRoundRobin(
        counter: AtomicInt,
        predicate: (FrameBufferImpl) -> Boolean,
    ): Boolean {
        val size = activeBuffers.size
        if (size == 0) return false

        for (i in 0 until size) {
            val index = counter.getAndIncrement()
            val buffer = activeBuffers.getRoundRobinIndex(index, size) ?: continue

            if (predicate(buffer)) {
                return true
            }
        }
        return false
    }

    private fun calculateByteSize(requests: List<Request>): Long {
        var byteSize = 0L
        for (request in requests) {
            for (streamId in request.streams) {
                val targetOutput = selectExpectedOutput(streamId) ?: continue

                val streamByteSize =
                    StreamFormat.bytesPerImage(
                        targetOutput.format,
                        targetOutput.size.width,
                        targetOutput.size.height,
                    )
                byteSize += streamByteSize
            }
        }
        return byteSize
    }

    private fun selectExpectedOutput(streamId: StreamId): OutputStream? {
        val stream = streamGraph[streamId] ?: return null
        if (stream.outputs.isEmpty()) return null

        // Pick the primary output for multi-resolution streams using the precomputed map.
        val primaryOutputId = imageReaderSources[streamId]?.primaryOutputIdFlow?.value

        if (primaryOutputId != null) {
            val activeOutput = stream.outputs.firstOrNull { it.id == primaryOutputId }
            if (activeOutput != null) {
                return activeOutput
            }
        }

        // Default to the first output if no primary output is set or found.
        return stream.outputs.first()
    }

    override fun onGraphCreated() {
        synchronized(lock) {
            if (!isGraphClosed) {
                cameraPipeResourceTrimmer.register(this)
            }
        }
    }

    override fun onGraphStarted() {
        synchronized(lock) {
            if (!isGraphClosed) {
                cameraPipeResourceTrimmer.markRecentlyActive(this)
            }
        }
    }

    override fun onGraphClosed() {
        synchronized(lock) {
            isGraphClosed = true
            _repeatingFrameSizeBytes.value = 0
            checkAndUnregister()
        }
        cameraPipeResourceTrimmer.invalidate()
    }

    private fun checkAndUnregister() {
        synchronized(lock) {
            if (isGraphClosed && activeBuffers.isEmpty()) {
                cameraPipeResourceTrimmer.unregister(this)
            }
        }
    }
}
