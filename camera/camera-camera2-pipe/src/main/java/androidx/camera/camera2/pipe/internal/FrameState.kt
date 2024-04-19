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

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.internal.FrameState.State.COMPLETE
import androidx.camera.camera2.pipe.internal.FrameState.State.FRAME_INFO_COMPLETE
import androidx.camera.camera2.pipe.internal.FrameState.State.STARTED
import androidx.camera.camera2.pipe.internal.FrameState.State.STREAM_RESULTS_COMPLETE
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.completeWithFailure
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.completeWithOutput
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.outputOrNull
import androidx.camera.camera2.pipe.internal.OutputResult.Companion.outputStatus
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.media.SharedOutputImage
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * This class represents a successfully started frame from the camera, and placeholders for the
 * images and metadata ([FrameInfo]) that we expect the camera to produce.
 */
@RequiresApi(21)
internal class FrameState(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val frameTimestamp: CameraTimestamp,
    imageStreams: Set<StreamId>
) {
    val frameId = nextFrameId()
    val frameInfoOutput: FrameInfoOutput = FrameInfoOutput()
    val imageOutputs: List<ImageOutput> = buildList {
        for (streamId in requestMetadata.streams.keys) {
            // Only create StreamResult's for streams that this OutputFrameDistributor supports.
            if (imageStreams.contains(streamId)) {
                val imageOutput = ImageOutput(streamId)
                add(imageOutput)
            }
        }
    }

    /**
     * State always begins in the [STARTED] state, and ends in the [COMPLETE] state. There are three
     * paths to get there, and is used to control when certain methods on the listener are invoked.
     *
     * [STARTED] -> [COMPLETE]
     * [STARTED] -> [FRAME_INFO_COMPLETE] -> [COMPLETE]
     * [STARTED] -> [STREAM_RESULTS_COMPLETE] -> [COMPLETE]
     */
    private enum class State {
        STARTED,
        FRAME_INFO_COMPLETE,
        STREAM_RESULTS_COMPLETE,
        COMPLETE
    }

    private val state = atomic(STARTED)
    private val streamResultCount = atomic(0)
    private val outputFrameListeners = CopyOnWriteArrayList<Frame.Listener>()

    fun addListener(listener: Frame.Listener) {
        listener.onFrameStarted(frameNumber, frameTimestamp)

        // Note: This operation is safe since the outputFrameListeners is a CopyOnWriteArrayList.
        outputFrameListeners.add(listener)
    }

    fun onFrameInfoComplete() {
        // Invoke the onOutputResultsAvailable onOutputMetadataAvailable.
        for (i in outputFrameListeners.indices) {
            outputFrameListeners[i].onFrameInfoAvailable()
        }

        val state = state.updateAndGet { current ->
            when (current) {
                STARTED -> FRAME_INFO_COMPLETE
                STREAM_RESULTS_COMPLETE -> COMPLETE
                else -> throw IllegalStateException(
                    "Unexpected frame state for $this! State is $current "
                )
            }
        }

        if (state == COMPLETE) {
            invokeOnFrameComplete()
        }
    }

    fun onStreamResultComplete(streamId: StreamId) {
        val allResultsCompleted = streamResultCount.incrementAndGet() != imageOutputs.size

        // Invoke the onOutputResultsAvailable listener.
        for (i in outputFrameListeners.indices) {
            outputFrameListeners[i].onImageAvailable(streamId)
        }

        if (allResultsCompleted) return

        // Invoke the onOutputResultsAvailable listener.
        for (i in outputFrameListeners.indices) {
            outputFrameListeners[i].onImagesAvailable()
        }

        val state = state.updateAndGet { current ->
            when (current) {
                STARTED -> STREAM_RESULTS_COMPLETE
                FRAME_INFO_COMPLETE -> COMPLETE
                else -> throw IllegalStateException(
                    "Unexpected frame state for $this! State is $current "
                )
            }
        }

        if (state == COMPLETE) {
            invokeOnFrameComplete()
        }
    }

    private fun invokeOnFrameComplete() {
        // Invoke the onOutputResultsAvailable listener.
        for (i in outputFrameListeners.indices) {
            outputFrameListeners[i].onFrameComplete()
        }
    }

    override fun toString(): String = "Frame-$frameId(${frameNumber.value}@${frameTimestamp.value})"

    /**
     * [FrameOutput] handles the logic and reference counting that is required to safely handle a
     * shared `CompletableDeferred` instance that may contain an expensive closable resource.
     */
    internal abstract class FrameOutput<T : Any> {
        private val count = atomic(1)

        /**
         * To avoid holding onto multiple status objects, this Deferred will hold *either* an
         * object of type T OR the [OutputStatus] that was passed down when this output was
         * completed.
         */
        protected val internalResult = CompletableDeferred<OutputResult<T>>()
        val result: Deferred<OutputResult<T>> get() = internalResult

        fun increment(): Boolean {
            val current =
                count.updateAndGet { current ->
                    if (current <= 0) {
                        0
                    } else {
                        current + 1
                    }
                }
            return current != 0
        }

        fun decrement() {
            if (count.decrementAndGet() == 0) {
                // UNAVAILABLE is used to indicate outputs that have been closed or released during
                // normal operation.
                internalResult.completeWithFailure(OutputStatus.UNAVAILABLE)
                release()
            }
        }

        val status: OutputStatus
            get() {
                // A result of `isCancelled` indicates the frame was closed before the Output
                // arrived.
                if (count.value == 0) {
                    return OutputStatus.UNAVAILABLE
                }
                return internalResult.outputStatus()
            }

        abstract fun outputOrNull(): T?
        abstract suspend fun await(): T?

        protected abstract fun release()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    inner class FrameInfoOutput : FrameOutput<FrameInfo>(),
        OutputDistributor.OutputListener<FrameInfo> {

        override fun onOutputComplete(
            cameraFrameNumber: FrameNumber,
            cameraTimestamp: CameraTimestamp,
            outputSequence: Long,
            outputNumber: Long,
            outputResult: OutputResult<FrameInfo>
        ) {
            internalResult.complete(outputResult)
            onFrameInfoComplete()
        }

        override suspend fun await(): FrameInfo? = result.await().output
        override fun outputOrNull() = result.outputOrNull()

        override fun release() {
            // NoOp
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    inner class ImageOutput(val streamId: StreamId) :
        FrameOutput<SharedOutputImage>(),
        OutputDistributor.OutputListener<OutputImage> {
        override fun onOutputComplete(
            cameraFrameNumber: FrameNumber,
            cameraTimestamp: CameraTimestamp,
            outputSequence: Long,
            outputNumber: Long,
            outputResult: OutputResult<OutputImage>
        ) {
            val output = outputResult.output
            if (output != null) {
                val sharedImage = SharedOutputImage.from(output)
                if (!internalResult.completeWithOutput(sharedImage)) {
                    sharedImage.close()
                }
            } else {
                internalResult.completeWithFailure(outputResult.status)
            }

            onStreamResultComplete(streamId)
        }

        override fun outputOrNull(): SharedOutputImage? = result.outputOrNull()?.acquireOrNull()

        override suspend fun await(): SharedOutputImage? = result.await().output?.acquireOrNull()

        override fun release() {
            internalResult.outputOrNull()?.close()
        }
    }

    companion object {
        private val frameIds = atomic(0L)
        private fun nextFrameId(): FrameId = FrameId(frameIds.incrementAndGet())
    }
}
