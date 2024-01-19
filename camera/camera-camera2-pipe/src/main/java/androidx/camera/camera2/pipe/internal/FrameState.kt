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
import androidx.camera.camera2.pipe.media.OutputDistributor
import androidx.camera.camera2.pipe.media.OutputImage
import androidx.camera.camera2.pipe.media.SharedOutputImage
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * This class represents a successfully started frame from the camera, and placeholders for the
 * images and metadata ([FrameInfo]) that we expect the camera to produce.
 */
@RequiresApi(21)
internal class FrameState(
    val requestMetadata: RequestMetadata,
    val frameId: FrameId,
    val frameNumber: FrameNumber,
    val frameTimestamp: CameraTimestamp,
    imageStreams: Set<StreamId>
) {
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
    @OptIn(ExperimentalCoroutinesApi::class)
    internal abstract class FrameOutput<T : Any> {
        private val count = atomic(1)

        /**
         * To avoid holding onto multiple status objects, this Deferred will hold *either* an
         * object of type T OR the [OutputStatus] that was passed down when this output was
         * completed.
         */
        val result = CompletableDeferred<Any>()

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
                result.cancel()
                try {
                    if (!result.isCancelled) {
                        // If we call cancel(), but the end state is not canceled, it means that
                        // the result was previously completed successfully. In this case, we need
                        // to release the underlying reference this Frame is holding on to.
                        result.getCompleted().asOutput { release(it) }
                    }
                } catch (ignored: IllegalStateException) {
                    // NoOp, this should never happen.
                }
            }
        }

        val status: OutputStatus
            get() {
                // A result of `isCancelled` indicates the frame was closed before the Output
                // arrived.
                if (count.value == 0 || result.isCancelled) {
                    return OutputStatus.UNAVAILABLE
                }

                // If the result is not canceled, and not completed, then we are waiting for the
                // output to arrive.
                if (!result.isCompleted) {
                    return OutputStatus.PENDING
                }

                // This is guaranteed to be completed.
                val result = result.getCompleted()
                if (result is OutputStatus) {
                    return result
                }
                return OutputStatus.AVAILABLE
            }

        fun getCompletedOrNull(): T? =
            if (!result.isCompleted || result.isCancelled) {
                null
            } else {
                result.getCompleted().asOutput { acquire(it) }
            }

        protected fun completeWith(output: T?, outputResult: OutputStatus): Boolean {
            val result = if (output == null) {
                result.complete(outputResult)
            } else {
                result.complete(output)
            }
            return result
        }

        private inline fun <R> Any.asOutput(block: (T) -> R): R? {
            if (this is OutputStatus) return null
            @Suppress("UNCHECKED_CAST")
            return block(this as T)
        }

        suspend fun await(): T? = result.await().asOutput { acquire(it) }

        /** Invoked to acquire the underlying resource. */
        protected abstract fun acquire(value: T): T?

        /** Invoked when the underlying resource is no longer referenced and should be released. */
        protected abstract fun release(value: T)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    inner class FrameInfoOutput : FrameOutput<FrameInfo>(),
        OutputDistributor.OutputListener<FrameInfo> {

        override fun onOutputComplete(
            cameraFrameNumber: FrameNumber,
            cameraTimestamp: CameraTimestamp,
            outputSequence: Long,
            outputNumber: Long,
            outputStatus: OutputStatus,
            output: FrameInfo?
        ) {
            check(output == null || output.frameNumber.value == outputNumber) {
                "Unexpected FrameInfo: $output " +
                    "Expected ${output?.frameNumber?.value} to match $outputNumber!"
            }

            completeWith(output, outputStatus)
            onFrameInfoComplete()
        }

        override fun acquire(value: FrameInfo): FrameInfo = value

        override fun release(value: FrameInfo) {
            // Ignored: FrameInfo is not closable.
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
            outputStatus: OutputStatus,
            output: OutputImage?
        ) {
            check(output == null || output.timestamp == outputNumber) {
                "Unexpected Image: $output, expected ${output?.timestamp} to match $outputNumber!"
            }
            val sharedImage = output?.let { SharedOutputImage.from(it) }
            if (!completeWith(sharedImage, outputStatus)) {
                sharedImage?.close()
            }
            onStreamResultComplete(streamId)
        }

        override fun release(value: SharedOutputImage) {
            value.close()
        }

        override fun acquire(value: SharedOutputImage): SharedOutputImage? = value.acquireOrNull()
    }
}
