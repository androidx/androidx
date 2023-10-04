/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.os.Build
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import kotlinx.atomicfu.atomic

/**
 * The goal of this class is simple: Associate a start event with its associated output.
 *
 * In addition this class must:
 * 1. Track and cancel events due to skipped [onOutputStarted] events.
 * 2. Track and finalize resources due to skipped [onOutputAvailable] events.
 * 3. Track and cancel events that match [onOutputFailure] events.
 * 4. Track and handle out-of-order [onOutputStarted] events.
 * 5. Finalize all resources and cancel all events during [close]
 *
 * This class makes several assumptions:
 * 1. [onOutputStarted] events *usually* arrive in order, relative to each other.
 * 2. [onOutputAvailable] events *usually* arrive in order, relative to each other.
 * 3. [onOutputStarted] events *usually* happen before a corresponding [onOutputAvailable] event
 * 4. [onOutputStarted] events may have a large number of events (1-50) before [onOutputAvailable]
 *      events start coming in.
 * 5. [onOutputStarted] and [onOutputAvailable] are 1:1 under normal circumstances.
 *
 * @param maximumCachedOutputs indicates how many available outputs this distributor will accept
 *   without matching [onOutputStarted] event before closing them with the [outputFinalizer].
 * @param outputFinalizer is responsible for closing outputs, if required.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class OutputDistributor<T>(
    private val maximumCachedOutputs: Int = 3,
    private val outputFinalizer: Finalizer<T>
) : AutoCloseable {

    internal interface OutputCompleteListener<T> {
        /**
         * Invoked when an output is in a completed state, and will *always* be invoked exactly
         * once per [OutputDistributor.onOutputStarted] event.
         *
         * On failures (The output being unavailable, the [OutputDistributor] being closed before
         * an output has arrived, or an explicit output failure event), this method will still be
         * invoked with a null [output].
         */
        fun onOutputComplete(
            cameraFrameNumber: FrameNumber,
            cameraTimestamp: CameraTimestamp,
            outputSequence: Long,
            outputNumber: Long,
            output: T?
        )
    }

    private val lock = Any()

    @GuardedBy("lock") private var closed = false
    @GuardedBy("lock") private var outputSequenceNumbers = 1L
    @GuardedBy("lock") private var newestOutputNumber = Long.MIN_VALUE
    @GuardedBy("lock") private var newestFrameNumber = FrameNumber(Long.MIN_VALUE)
    @GuardedBy("lock") private var lastFailedFrameNumber = Long.MIN_VALUE
    @GuardedBy("lock") private var lastFailedOutputNumber = Long.MIN_VALUE

    private val startedOutputs = mutableListOf<StartedOutput<T>>()
    private val availableOutputs = mutableMapOf<Long, T?>()

    /**
     * Indicates a camera2 output has started at a particular frameNumber and timestamp as well as
     * supplying the callback to listen for the output to become available. The
     * [outputCompleteListener] can be invoked synchronously if the output is already available.
     *
     * @param cameraFrameNumber The Camera2 FrameNumber for this output
     * @param cameraTimestamp The Camera2 CameraTimestamp for this output
     * @param outputNumber untyped number that corresponds to the number provided by
     *   [onOutputAvailable]. For Images, this will likely be the timestamp of the image (Which may
     *   be the same as the CameraTimestamp, but may also be different if the timebase of the
     *   the images is different), or the value of the frameNumber if this OutputDistributor is
     *   handling metadata.
     * @param outputCompleteListener will be invoked whenever the output is fully resolved,
     *   either because the output has been successfully matched, or because the output has failed,
     *   or because this OutputDistributor is now closed.
     */
    fun onOutputStarted(
        cameraFrameNumber: FrameNumber,
        cameraTimestamp: CameraTimestamp,
        outputNumber: Long,
        outputCompleteListener: OutputCompleteListener<T>
    ) {
        var outputsToCancel: List<StartedOutput<T>>? = null
        var outputToComplete: T? = null
        var invokeOutputCompleteListener = false
        var outputToFinalize: T? = null

        val outputSequence: Long
        synchronized(lock) {
            outputSequence = outputSequenceNumbers++
            if (closed ||
                lastFailedFrameNumber == cameraFrameNumber.value ||
                lastFailedOutputNumber == outputNumber
            ) {
                outputToFinalize = availableOutputs.remove(outputNumber)
                invokeOutputCompleteListener = true
                return@synchronized
            }

            // Determine if the frameNumber is out of order relative to other onOutputStarted calls
            val isFrameNumberOutOfOrder = cameraFrameNumber.value < newestFrameNumber.value
            if (!isFrameNumberOutOfOrder) {
                newestFrameNumber = cameraFrameNumber
            }

            // Determine if the outputNumber is out of order relative to other onOutputStarted calls
            val isOutputNumberOutOfOrder = outputNumber < newestOutputNumber
            if (!isOutputNumberOutOfOrder) {
                newestOutputNumber = outputNumber
            }
            val isOutOfOrder = isFrameNumberOutOfOrder || isOutputNumberOutOfOrder

            // onOutputStarted should only be invoked once. Check to see that there are no other
            // duplicate events.
            check(
                !startedOutputs.any {
                    it.cameraFrameNumber == cameraFrameNumber ||
                        it.cameraTimestamp == cameraTimestamp ||
                        it.outputNumber == outputNumber
                }
            ) {
                "onOutputStarted was invoked multiple times with a previously started output!" +
                    "onOutputStarted with $cameraFrameNumber, $cameraTimestamp, $outputNumber. " +
                    "Previously started outputs: $startedOutputs"
            }

            // Check for matching outputs
            if (availableOutputs.containsKey(outputNumber)) {
                // If we found a matching output, get and remove it from the list of
                // availableOutputs.
                outputToComplete = availableOutputs.remove(outputNumber)
                invokeOutputCompleteListener = true
                outputsToCancel = removeOutputsOlderThan(
                    isOutOfOrder,
                    outputSequence,
                    outputNumber
                )
                return@synchronized
            }

            // If there are no available outputs that match the outputNumber, add it to the list
            // of startedOutputs.
            startedOutputs.add(
                StartedOutput(
                    isOutOfOrder,
                    cameraFrameNumber,
                    cameraTimestamp,
                    outputSequence,
                    outputNumber,
                    outputCompleteListener
                )
            )
        }

        // Invoke finalizers and listeners outside of the synchronized block to avoid holding locks.
        outputsToCancel?.forEach { it.completeWith(null) }
        outputToFinalize?.let { outputFinalizer.finalize(it) }
        if (invokeOutputCompleteListener) {
            outputCompleteListener.onOutputComplete(
                cameraFrameNumber = cameraFrameNumber,
                cameraTimestamp = cameraTimestamp,
                outputSequence = outputSequence,
                outputNumber = outputNumber,
                outputToComplete
            )
        }
    }

    /**
     * Indicates a camera2 output has arrived for a specific [outputNumber]. outputNumber will
     * often refer to a FrameNumber for TotalCaptureResult distribution, and will often refer to a
     * nanosecond timestamp for ImageReader Image distribution.
     */
    fun onOutputAvailable(outputNumber: Long, output: T?) {
        var outputToFinalize: T? = null
        var outputsToCancel: List<StartedOutput<T>>? = null

        synchronized(lock) {
            if (closed || lastFailedOutputNumber == outputNumber) {
                outputToFinalize = output
                return@synchronized
            }

            val matchingOutput = startedOutputs.firstOrNull { it.outputNumber == outputNumber }

            // Complete the matching output, if possible, and remove it from the list of started
            // outputs.
            if (matchingOutput != null) {
                outputsToCancel = removeOutputsOlderThan(matchingOutput)

                matchingOutput.completeWith(output)
                startedOutputs.remove(matchingOutput)
                return@synchronized
            }

            // If there is no started output, put this output into the queue of pending outputs.
            availableOutputs[outputNumber] = output

            // If there are too many pending outputs, remove the oldest one.
            if (availableOutputs.size > maximumCachedOutputs) {
                val oldestOutput = availableOutputs.keys.first()
                outputToFinalize = availableOutputs.remove(oldestOutput)
                return@synchronized
            }
        }

        // Invoke finalizers and listeners outside of the synchronized block to avoid holding locks.
        outputToFinalize?.let { outputFinalizer.finalize(it) }
        outputsToCancel?.forEach { it.completeWith(null) }
    }

    /**
     * Indicates an output will not arrive for a specific [FrameNumber].
     */
    fun onOutputFailure(frameNumber: FrameNumber) {
        var outputToCancel: StartedOutput<T>? = null

        synchronized(lock) {
            if (closed) {
                return
            }
            lastFailedFrameNumber = frameNumber.value
            startedOutputs
                .singleOrNull { it.cameraFrameNumber == frameNumber }
                ?.let {
                    lastFailedOutputNumber = it.outputNumber
                    startedOutputs.remove(it)
                    outputToCancel = it
                }
        }

        // Invoke listeners outside of the synchronized block to avoid holding locks.
        outputToCancel?.completeWith(null)
    }

    @GuardedBy("lock")
    private fun removeOutputsOlderThan(output: StartedOutput<T>): List<StartedOutput<T>> =
        removeOutputsOlderThan(output.isOutOfOrder, output.outputSequence, output.outputNumber)

    private fun removeOutputsOlderThan(
        isOutOfOrder: Boolean,
        outputSequence: Long,
        outputNumber: Long
    ): List<StartedOutput<T>> {
        // This filter is bi-modal: If [output] is outOfOrder, it will only remove *other* out of
        // order events that are older than the most recent event. Similarly, if it's normal and in
        // order, then this will ignore other outOfOrder events.
        val outputsToCancel =
            startedOutputs.filter {
                it.isOutOfOrder == isOutOfOrder &&
                    it.outputSequence < outputSequence &&
                    it.outputNumber < outputNumber
            }
        startedOutputs.removeAll(outputsToCancel)
        return outputsToCancel
    }

    override fun close() {
        var outputsToFinalize: List<T?>
        var outputsToCancel: List<StartedOutput<T>>

        synchronized(lock) {
            if (closed) {
                return
            }
            closed = true

            outputsToFinalize = availableOutputs.values.toMutableList()
            availableOutputs.clear()
            outputsToCancel = startedOutputs.toMutableList()
            startedOutputs.clear()
        }

        for (pendingOutput in outputsToFinalize) {
            outputFinalizer.finalize(pendingOutput)
        }
        for (startedOutput in outputsToCancel) {
            startedOutput.completeWith(null)
        }
    }

    /**
     * Utility class that holds the parameters of an [onOutputStarted] event until the output
     * arrives.
     */
    private data class StartedOutput<T>(
        val isOutOfOrder: Boolean,
        val cameraFrameNumber: FrameNumber,
        val cameraTimestamp: CameraTimestamp,
        val outputSequence: Long,
        val outputNumber: Long,
        private val outputCompleteListener: OutputCompleteListener<T>
    ) {
        private val complete = atomic(false)

        fun completeWith(output: T?) {
            check(complete.compareAndSet(expect = false, update = true)) {
                "Output $outputSequence at $cameraFrameNumber for $outputNumber was completed " +
                    "multiple times!"
            }
            outputCompleteListener.onOutputComplete(
                cameraFrameNumber,
                cameraTimestamp,
                outputSequence,
                outputNumber,
                output
            )
        }
    }
}
