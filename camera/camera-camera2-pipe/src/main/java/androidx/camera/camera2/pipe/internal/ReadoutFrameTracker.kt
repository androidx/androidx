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
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.common.CameraFrameNumber

/**
 * Tracks started frames whose readout outputs are waiting for onReadoutStarted.
 *
 * This class only records state. Callers are responsible for starting or failing the returned
 * [FrameState]s and [FrameState.ImageOutput]s outside of this class.
 */
internal class ReadoutFrameTracker {
    private val lock = Any()

    @GuardedBy("lock") private val startedFrameStates = mutableListOf<FrameState>()

    /** Outcome of [onBufferLost]: the waiting frame and which of its readout outputs were lost. */
    class BufferLostResult(
        val frameState: FrameState,
        val lostOutputs: List<FrameState.ImageOutput>,
    )

    /**
     * Records that [frameState] has started. The frame is only tracked if it expects a readout
     * timestamp.
     */
    fun onStarted(frameState: FrameState) {
        if (frameState.expectsReadoutTimestamp) {
            synchronized(lock) { startedFrameStates.add(frameState) }
        }
    }

    /**
     * Removes and returns the waiting [FrameState] for [frameNumber], or null if none.
     *
     * This currently does the same as [remove]. It is kept as a separate method because
     * onReadoutStarted is a different event from a frame completing or failing, and its handling is
     * expected to diverge (for example, readout timestamps that arrive before onStarted).
     */
    fun onReadoutStarted(frameNumber: CameraFrameNumber): FrameState? =
        synchronized(lock) { removeStartedFrameState(frameNumber) }

    /** Forgets [frameNumber]. Returns the frame if it was still waiting. */
    fun remove(frameNumber: CameraFrameNumber): FrameState? =
        synchronized(lock) { removeStartedFrameState(frameNumber) }

    /**
     * Records a buffer loss for a waiting frame. Returns null if no frame is waiting for
     * [frameNumber]. The frame is removed from the waiting list once none of its readout outputs
     * are still pending.
     */
    fun onBufferLost(
        frameNumber: CameraFrameNumber,
        streamId: StreamId,
        outputId: OutputId,
        isConcurrentStream: Boolean,
    ): BufferLostResult? {
        synchronized(lock) {
            val frameState = findStartedFrameState(frameNumber) ?: return null
            val lostOutputs = mutableListOf<FrameState.ImageOutput>()
            var hasRemainingReadoutOutputs = false
            val readoutImageOutputs = frameState.readoutImageOutputs
            for (i in readoutImageOutputs.indices) {
                val imageOutput = readoutImageOutputs[i]
                val isLostStream = imageOutput.streamId == streamId
                val isLostOutput = !isConcurrentStream || imageOutput.outputId == outputId
                if (isLostStream && isLostOutput) {
                    lostOutputs.add(imageOutput)
                } else if (imageOutput.status == OutputStatus.PENDING) {
                    // Only outputs that are still pending can keep this frame alive.
                    // Outputs that previously failed (for example, from an earlier
                    // onBufferLost on another output of the same concurrent stream)
                    // must not, otherwise the FrameState would never be removed.
                    hasRemainingReadoutOutputs = true
                }
            }
            if (!hasRemainingReadoutOutputs) {
                startedFrameStates.remove(frameState)
            }
            return BufferLostResult(frameState, lostOutputs)
        }
    }

    /** Clears all state and returns the frames that were still waiting for onReadoutStarted. */
    fun clear(): List<FrameState> =
        synchronized(lock) {
            val states = startedFrameStates.toList()
            startedFrameStates.clear()
            states
        }

    /** Returns the [FrameState] that is still waiting for onReadoutStarted, if there is one. */
    @GuardedBy("lock")
    private fun findStartedFrameState(frameNumber: CameraFrameNumber): FrameState? {
        for (i in startedFrameStates.indices) {
            val frameState = startedFrameStates[i]
            if (frameState.frameNumber == frameNumber) {
                return frameState
            }
        }
        return null
    }

    @GuardedBy("lock")
    private fun removeStartedFrameState(frameNumber: CameraFrameNumber): FrameState? =
        findStartedFrameState(frameNumber)?.also { startedFrameStates.remove(it) }
}
