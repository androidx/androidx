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

package androidx.camera.video.internal.audio

import androidx.camera.core.Logger

/**
 * Maps audio frames output by an [AudioProcessingPipeline] back to the capture timestamps of the
 * input frames they were produced from.
 *
 * Processors may buffer input before producing output (such as lookahead or block-based
 * processors), so processed output does not necessarily belong to the most recently queued input.
 * This class records the capture timestamp of each input packet queued into the pipeline, and maps
 * each output frame to the input packet at the same position in the stream. Stream positions are
 * measured in time, so input and output positions stay comparable when processors change the sample
 * rate. This assumes processors neither add nor drop frames.
 *
 * This class is not thread-safe and must be confined to a single thread.
 *
 * @param inputSampleRate the sample rate of the audio queued into the pipeline
 * @param outputSampleRate the sample rate of the audio output by the pipeline
 * @param capacity the maximum number of input packets tracked ahead of the output. If output lags
 *   further behind, the oldest packets are discarded and timestamps of their frames are
 *   extrapolated. Defaults to 500.
 */
public class ProcessedAudioTimestampTracker
@JvmOverloads
constructor(
    private val inputSampleRate: Int,
    private val outputSampleRate: Int,
    private val capacity: Int = DEFAULT_CAPACITY,
) {
    // Input packets after the current packet, stored as primitives in a ring buffer to avoid
    // per-packet allocations. Allocated on first use so that no memory is used when no input is
    // ever tracked, such as when no audio processors are configured.
    private val pendingPositionsUs by lazy(LazyThreadSafetyMode.NONE) { LongArray(capacity) }
    private val pendingTimestampsUs by lazy(LazyThreadSafetyMode.NONE) { LongArray(capacity) }
    private var pendingHead = 0
    private var pendingCount = 0

    // The input packet that output frames are currently mapped against. It normally contains the
    // next output frame, but may be ahead of it after pending packets are discarded on overflow,
    // in which case timestamps are extrapolated backwards.
    private var hasCurrentPacket = false
    private var currentPositionUs = 0L
    private var currentTimestampUs = 0L

    private var inputFramesQueued = 0L
    private var outputFramesTaken = 0L
    private var overflowLogged = false

    init {
        require(inputSampleRate > 0) { "Invalid input sample rate: $inputSampleRate" }
        require(outputSampleRate > 0) { "Invalid output sample rate: $outputSampleRate" }
        require(capacity > 0) { "Invalid capacity: $capacity" }
    }

    /** Returns whether any input has been tracked since creation or the last [reset]. */
    public fun hasTrackedInput(): Boolean = hasCurrentPacket || pendingCount > 0

    /**
     * Records an input packet about to be queued into the pipeline.
     *
     * @param timestampUs the capture timestamp of the first frame of the packet
     * @param frameCount the number of frames in the packet. Packets without frames are ignored.
     */
    public fun onInputQueued(timestampUs: Long, frameCount: Int) {
        if (frameCount <= 0) {
            return
        }
        if (pendingCount == capacity) {
            if (!overflowLogged) {
                Logger.w(
                    TAG,
                    "AudioProcessor output is lagging far behind its input. Audio timestamps " +
                        "may be inaccurate.",
                )
                overflowLogged = true
            }
            pollPendingPacket()
        }
        val tail = (pendingHead + pendingCount) % capacity
        pendingPositionsUs[tail] = framesToUs(inputFramesQueued, inputSampleRate)
        pendingTimestampsUs[tail] = timestampUs
        pendingCount++
        inputFramesQueued += frameCount
    }

    /**
     * Returns the presentation time of the next [frameCount] output frames, which is the
     * presentation time of the first of them, and consumes them.
     *
     * @throws IllegalArgumentException if [frameCount] is negative
     * @throws IllegalStateException if no input has been tracked
     */
    public fun takeOutputPresentationTimeUs(frameCount: Int): Long {
        require(frameCount >= 0) { "Invalid frame count: $frameCount" }
        check(hasTrackedInput()) { "No input has been tracked." }
        val positionUs = framesToUs(outputFramesTaken, outputSampleRate)
        outputFramesTaken += frameCount
        // Advance to the latest input packet that starts at or before this position.
        while (
            pendingCount > 0 && (!hasCurrentPacket || pendingPositionsUs[pendingHead] <= positionUs)
        ) {
            pollPendingPacket()
        }
        return currentTimestampUs + (positionUs - currentPositionUs)
    }

    /** Clears all tracked input and output, such as when the pipeline is flushed. */
    public fun reset() {
        pendingHead = 0
        pendingCount = 0
        hasCurrentPacket = false
        currentPositionUs = 0L
        currentTimestampUs = 0L
        inputFramesQueued = 0L
        outputFramesTaken = 0L
        overflowLogged = false
    }

    /** Moves the oldest pending input packet to be the current packet. */
    private fun pollPendingPacket() {
        currentPositionUs = pendingPositionsUs[pendingHead]
        currentTimestampUs = pendingTimestampsUs[pendingHead]
        hasCurrentPacket = true
        pendingHead = (pendingHead + 1) % capacity
        pendingCount--
    }

    private companion object {
        private const val TAG = "ProcessedAudioTsTracker"

        /** Covers at least ~10 s of audio with typical packet sizes. */
        private const val DEFAULT_CAPACITY = 500

        private fun framesToUs(frameCount: Long, sampleRate: Int): Long =
            frameCount * 1_000_000L / sampleRate
    }
}
