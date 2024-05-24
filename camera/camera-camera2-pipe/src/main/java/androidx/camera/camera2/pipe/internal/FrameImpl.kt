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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.media.OutputImage
import kotlinx.atomicfu.atomic

/**
 * FrameImpl is the canonical implementation of a [Frame].
 *
 * Each instance is closeable, and references to an underlying [FrameState] object which aggregates
 * and all of the underlying placeholder objects for each expected output.
 */
internal class FrameImpl
private constructor(
    private val frameState: FrameState,
    override val imageStreams: Set<StreamId>,
) : Frame {
    internal constructor(
        frameState: FrameState
    ) : this(frameState, frameState.imageOutputs.map { it.streamId }.toSet())

    private val closed = atomic(false)

    override fun tryAcquire(streamFilter: Set<StreamId>?): FrameImpl? {
        if (closed.value) return null
        if (!frameState.frameInfoOutput.increment()) return null

        var success = true
        val availableImageStreams = buildSet {
            for (streamResult in frameState.imageOutputs) {
                val id = streamResult.streamId
                if (
                    imageStreams.contains(id) && (streamFilter == null || streamFilter.contains(id))
                ) {
                    // Increment the reference count for the underlying image output. If successful,
                    // add it to the list of available streams.
                    if (streamResult.increment()) {
                        add(id)
                    } else {
                        success = false
                        break // Exit the loop early if we fail to increment
                    }
                }
            }
        }

        if (!success) {
            // Undo the reference counting:
            // 1. Undo the frameInfoOutput
            // 2. Undo each successfully incremented output, which is added to availableStreams.
            frameState.frameInfoOutput.decrement()
            for (streamResult in frameState.imageOutputs) {
                if (availableImageStreams.contains(streamResult.streamId)) {
                    streamResult.decrement()
                }
            }
            return null
        }

        // Return the new Frame instance
        return FrameImpl(frameState, availableImageStreams)
    }

    override fun close() {
        release()
    }

    private fun release(): Boolean {
        if (closed.compareAndSet(expect = false, update = true)) {
            // This is guaranteed to run *exactly* once no matter how many times release is called.
            frameState.frameInfoOutput.decrement()

            // Iterate through each of the image outputs and decrement the count for the
            // imageStreams that are held by this Frame.
            for (i in frameState.imageOutputs.indices) {
                val streamResult = frameState.imageOutputs[i]
                if (imageStreams.contains(streamResult.streamId)) {
                    streamResult.decrement()
                }
            }
            return true
        }
        return false
    }

    override val requestMetadata: RequestMetadata
        get() = frameState.requestMetadata

    override val frameId: FrameId
        get() = frameState.frameId

    override val frameNumber: FrameNumber
        get() = frameState.frameNumber

    override val frameTimestamp: CameraTimestamp
        get() = frameState.frameTimestamp

    override val frameInfoStatus: OutputStatus
        get() {
            if (closed.value) return OutputStatus.UNAVAILABLE
            return frameState.frameInfoOutput.status
        }

    protected fun finalize() {
        // https://kotlinlang.org/docs/java-interop.html#finalize
        // Frames that are no longer reachable should be closed to avoid memory leaks.
        if (release()) {
            Log.error {
                "Failed to close $this! This indicates a memory leak and could cause the camera" +
                    " to stall, or images to be lost."
            }
        }
    }

    override suspend fun awaitFrameInfo(): FrameInfo? {
        if (closed.value) return null
        return frameState.frameInfoOutput.await()
    }

    override fun getFrameInfo(): FrameInfo? {
        if (closed.value) return null
        return frameState.frameInfoOutput.outputOrNull()
    }

    override suspend fun awaitImage(streamId: StreamId): OutputImage? {
        if (closed.value) return null
        if (!imageStreams.contains(streamId)) return null
        val output = frameState.imageOutputs.firstOrNull { it.streamId == streamId }
        return output?.await()
    }

    override fun getImage(streamId: StreamId): OutputImage? {
        if (closed.value) return null
        if (!imageStreams.contains(streamId)) return null
        val output = frameState.imageOutputs.firstOrNull { it.streamId == streamId }
        return output?.outputOrNull()
    }

    override fun imageStatus(streamId: StreamId): OutputStatus {
        if (closed.value || !imageStreams.contains(streamId)) return OutputStatus.UNAVAILABLE
        return frameState.imageOutputs.firstOrNull { it.streamId == streamId }?.status
            ?: OutputStatus.UNAVAILABLE
    }

    override fun addListener(listener: Frame.Listener) {
        check(!closed.value) { "Cannot add Frame.Listener, $this is closed!" }
        frameState.addListener(listener)
    }

    override fun toString(): String = frameState.toString()
}
