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

import android.hardware.HardwareBuffer
import android.os.Build
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameCapture
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.FrameReference
import androidx.camera.camera2.pipe.ImageSourceConfig
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import androidx.camera.camera2.pipe.media.ClosingFinalizer
import androidx.camera.camera2.pipe.media.ExpectedOutputsListener
import androidx.camera.camera2.pipe.media.ImageListener
import androidx.camera.camera2.pipe.media.ImageSource
import androidx.camera.camera2.pipe.media.NoOpFinalizer
import androidx.camera.camera2.pipe.media.OutputImage

/**
 * A FrameDistributor is responsible for listening to events from each [Request] as well as images
 * that are produced by [ImageSources][ImageSource] in order to group them into [Frames][Frame] and
 * distribute these frames to downstream consumers.
 *
 * Frames can be safely held until needed, passed to other consumers, or closed at any point in time
 * while correctly handling the underlying resource counts and error handling, which is a
 * non-trivial problem to solve correctly and efficiently. For optimal behavior, an instance of this
 * class should be attached as a listener to the callbacks of *all* [Requests][Request] sent to the
 * Camera.
 *
 * Frames are distributed during [onStarted] in two primary ways:
 * 1. To [FrameCapture] for non-repeating capture requests in the [frameCaptureQueue]
 * 2. To the [frameStartedListener] as a [FrameReference], which must call [Frame.tryAcquire] if it
 *    would like to hold onto it beyond the lifetime of the method call.
 *
 * The remaining callbacks are used to distribute specific error and failure conditions to frames
 * that were previously started.
 */
internal class FrameDistributor(
    private val streamGraphImpl: StreamGraphImpl,
    private val frameCaptureQueue: FrameCaptureQueue,
    isCameraTimebaseRealtime: Boolean,
    realtimeToMonotonicOffsetNs: Long,
) : AutoCloseable, Request.Listener {
    /**
     * Listener to observe new [FrameReferences][FrameReference] as they are started by the camera.
     */
    fun interface FrameStartedListener {
        /**
         * Invoked when a new [Frame] has started. Implementations must synchronously invoke
         * [Frame.tryAcquire] if they want to maintain a valid reference to the [Frame]
         */
        fun onFrameStarted(frameReference: FrameReference)
    }

    private val frameInfoDistributor =
        OutputDistributor<FrameInfo>(
            outputFinalizer = NoOpFinalizer,
            outputMatcher = OutputMatcher.EXACT,
        )

    // This is an example CameraGraph configuration a camera configured with both capture and
    // non capture output streams, as well as physical outputs:
    //
    // Camera-0 (Logical)
    //   Stream-1  Output-1   Viewfinder  (No Images)
    //   Stream-2  Output-2   YUV_420
    //   Stream-3  Output-3   RAW10       [Camera-4]
    //             Output-4   RAW10       [Camera-5]
    //             Output-5   RAW10       [Camera-6]
    //
    // In this scenario the FrameDistributor will handle distributing images to Stream-2 and
    // to Stream-3 if they are configured with an ImageSource. Each of these streams is
    // associated with its own OutputDistributor for error handling and grouping.
    private val imageDistributors: Map<StreamId, Map<OutputId, OutputDistributor<OutputImage>>>
    private val imageStreams: Set<CameraStream>
    private val concurrentImageStreams: Set<StreamId>?

    var frameStartedListener: FrameStartedListener = FrameStartedListener {}

    init {
        val streams = mutableSetOf<CameraStream>()
        val concurrentStreams = mutableSetOf<StreamId>()
        imageDistributors =
            streamGraphImpl.imageSourceMap.mapValues { (cameraStreamId, imageSource) ->
                val cameraStream = checkNotNull(streamGraphImpl[cameraStreamId])
                val cameraStreamConfig = streamGraphImpl.getCameraStreamConfig(cameraStreamId)!!
                val imageSourceConfig = cameraStreamConfig.imageSourceConfig!!

                streams.add(cameraStream)
                if (cameraStreamConfig.imageSourceConfig.enableConcurrentOutputs) {
                    concurrentStreams.add(cameraStreamId)
                }
                val outputMatcher =
                    selectTimestampMatcher(
                        cameraStreamId,
                        cameraStreamConfig,
                        imageSourceConfig,
                        isCameraTimebaseRealtime,
                        realtimeToMonotonicOffsetNs,
                    )

                val imageDistributorMap = buildMap {
                    for (outputStream in cameraStream.outputs) {
                        val imageDistributor =
                            OutputDistributor<OutputImage>(
                                outputFinalizer = ClosingFinalizer,
                                outputMatcher = outputMatcher,
                            )
                        put(outputStream.id, imageDistributor)
                    }
                }

                imageSource.imageListener =
                    ImageListener { streamId, outputId, outputTimestamp, image ->
                        val imageDistributor =
                            checkNotNull(imageDistributorMap[outputId]) {
                                "Received unexpected images on $imageSource from ($streamId, $outputId)"
                            }
                        if (image != null) {
                            imageDistributor.onOutputResult(
                                outputTimestamp,
                                OutputResult.from(OutputImage.from(streamId, outputId, image)),
                            )
                        } else {
                            imageDistributor.onOutputResult(
                                outputTimestamp,
                                OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED),
                            )
                        }
                    }

                imageSource.expectedOutputsListener =
                    ExpectedOutputsListener { timestamp, outputIds ->
                        val outputs = cameraStream.outputs
                        for (i in outputs.indices) {
                            val outputId = outputs[i].id
                            if (!outputIds.contains(outputId)) {
                                // Not expecting output for this output stream.
                                val imageDistributor = checkNotNull(imageDistributorMap[outputId])
                                imageDistributor.onOutputResult(
                                    timestamp,
                                    OutputResult.failure(OutputStatus.UNAVAILABLE),
                                )
                            }
                        }
                    }

                imageDistributorMap
            }

        imageStreams = streams
        concurrentImageStreams = if (concurrentStreams.isEmpty()) null else concurrentStreams
    }

    /**
     * Create and distribute a [Frame] to the pending [FrameCapture] (If one has been registered for
     * this request), and to the [FrameStartedListener]
     */
    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp,
    ) {
        // When the camera begins exposing a frame, create a placeholder for all of the outputs that
        // will be produced, and tell each of the image distributors to expect results for this
        // frameNumber and timestamp.
        val frameState =
            FrameState(
                requestMetadata,
                frameNumber,
                timestamp,
                imageStreams,
                concurrentImageStreams,
            )

        // Tell the frameInfo distributor to expect FrameInfo at the provided FrameNumber
        frameInfoDistributor.onOutputStarted(
            cameraFrameNumber = frameNumber,
            cameraTimestamp = timestamp,
            cameraOutputNumber = frameNumber.value, // Number to match output against
            outputListener = frameState.frameInfoOutput,
        )

        // Tell each imageDistributor to expect an Image at the provided CameraTimestamp.
        for (i in frameState.imageOutputs.indices) {
            val imageOutput = frameState.imageOutputs[i]
            val imageDistributorMap = checkNotNull(imageDistributors[imageOutput.streamId])
            val imageDistributor = checkNotNull(imageDistributorMap[imageOutput.outputId])

            // Images are matched to the frame based on the cameraTimestamp.
            imageDistributor.onOutputStarted(
                cameraFrameNumber = frameNumber,
                cameraTimestamp = timestamp,
                cameraOutputNumber = timestamp.value, // Number to match output against
                outputListener = imageOutput,
            )

            if (!requestMetadata.streams.keys.contains(imageOutput.streamId)) {
                // Edge case: It's possible that a CaptureRequest submitted to the camera
                // is different than the Request used to create it in a few scenarios (such
                // as the surface being unavailable or invalid). If this happens, tell the
                // imageDistributor that the output has failed for this specific frame.
                imageDistributor.onOutputFailure(frameState.frameNumber)
            }
        }

        // Create a Frame, and offer it
        val frame = FrameImpl(frameState)
        frameStartedListener.onFrameStarted(frame)

        // If there is an explicit capture request associated with this request, pass it to the
        // FrameCapture.
        if (!requestMetadata.repeating) {
            val frameCapture = frameCaptureQueue.remove(requestMetadata.request)
            if (frameCapture != null) {
                frameCapture.completeWith(frame)
                return
            }
        }

        // Close the frame. This releases the reference we are holding.
        frame.close()
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo,
    ) {
        // Tell the frameInfo distributor that the metadata for this exposure has been computed and
        // can be distributed.
        frameInfoDistributor.onOutputResult(frameNumber.value, OutputResult.from(result))
    }

    override fun onBufferLost(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        streamId: StreamId,
        outputId: OutputId,
    ) {
        val imageDistributorMap = imageDistributors[streamId] ?: return

        // Tell the specific image distributor for this stream that the output has failed and will
        // not arrive for this frame. When onBufferLost occurs, other images and metadata may still
        // complete successfully.
        if (concurrentImageStreams?.contains(streamId) == true) {
            // If this is a concurrent multi-output stream, we will be notified on each buffer lost.
            checkNotNull(imageDistributorMap[outputId]).onOutputFailure(frameNumber)
        } else {
            check(imageDistributorMap.contains(outputId))
            // If this is not a concurrent multi-output stream, we will only be notified once per
            // stream, and thus we would need to inform all image distributors.
            for (imageDistributor in imageDistributorMap.values) {
                imageDistributor.onOutputFailure(frameNumber)
            }
        }
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure,
    ) {
        // Metadata will not arrive for this frame:
        frameInfoDistributor.onOutputResult(
            frameNumber.value,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_FAILED),
        )

        // There are two scenarios:
        // 1. Images were captured: In this case, camera2 will send individual failures for each
        //    failed image via the onBufferLost callback.
        // 2. Images were *not* captured: (This case), if images were not captured, all outputs have
        //    failed, and camera2 will not invoke onBufferLost. We are responsible for marking all
        //    outputs as failed.
        if (!requestFailure.wasImageCaptured) {
            // Note: The actual streams used by camera2 are specified in requestMetadata.streams and
            //   may be different than requestMetadata.request.streams if one of the surfaces was
            //   not ready or available. Make sure we iterate over `requestMetadata.streams`
            for (stream in requestMetadata.streams.keys) {
                val imageDistributorMap = imageDistributors[stream] ?: continue
                for (imageDistributor in imageDistributorMap.values) {
                    imageDistributor.onOutputFailure(frameNumber)
                }
            }
        }
    }

    override fun onAborted(request: Request) {
        // When a request is aborted, it may (or may not) be in some stage of capture, and it might
        // not have started yet. The only thing we know for sure is:
        //
        // 1. Only single requests can be aborted (repeating requests are never guaranteed to
        //    produce outputs, so they never get aborted calls)
        // 2. If a request is already started, the camera should handle the failures for aborted
        //    captures as a separate set of events.
        frameCaptureQueue.remove(request)?.completeWithFailure(OutputStatus.ERROR_OUTPUT_ABORTED)
    }

    override fun close() {
        // Closing the captureQueue aborts all pending and future capture requests.
        frameCaptureQueue.close()

        // Stop distributing FrameInfo
        frameInfoDistributor.close()

        // Stop distributing Images
        for (imageDistributorMap in imageDistributors.values) {
            for (imageDistributor in imageDistributorMap.values) {
                imageDistributor.close()
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        @JvmStatic
        private fun selectTimestampMatcher(
            cameraStreamId: StreamId,
            cameraStreamConfig: CameraStream.Config,
            imageSourceConfig: ImageSourceConfig,
            isCameraTimebaseRealtime: Boolean,
            realtimeToMonotonicOffsetNs: Long,
        ): OutputMatcher {
            // This logic mirrors the behavior of Camera3OutputStream.cpp which uses similar logic
            // to determine the timebase for outputs.
            //
            // See frameworks/av/services/camera/libcameraservice/device3/Camera3OutputStream.cpp
            // for more details.

            // TODO: Add support for OutputConfiguration.setReadoutTimestampEnabled which changes
            //   the timestamps of images being produced by the ImageReader.

            // TODO: Consider altering the detection delta for inexact ratios during high-speed
            //   recording.
            if (isCameraTimebaseRealtime) {
                if (
                    cameraStreamConfig.isDefaultTimebase() &&
                        !imageSourceConfig.isVideoEncodeUsage() &&
                        !imageSourceConfig.isHwComposerUsage()
                ) {
                    return OutputMatcher.EXACT
                } else if (
                    cameraStreamConfig.isRealtimeTimebase() || cameraStreamConfig.isSensorTimebase()
                ) {
                    return OutputMatcher.EXACT
                }
                Log.debug {
                    "Configuring $cameraStreamId with inexact realtime-to-monotonic" +
                        " timestamp matching rules."
                }
                return OutputMatcher.forTimestampsWithOffset(realtimeToMonotonicOffsetNs)
            }

            // If the Camera Timebase is monotonic...

            if (cameraStreamConfig.isRealtimeTimebase()) {
                Log.debug {
                    "Configuring $cameraStreamId with inexact monotonic-to-realtime" +
                        " timestamp matching rules."
                }
                // Camera is in monotonic but outputs are using the realtime timebase.
                return OutputMatcher.forTimestampsWithOffset(-realtimeToMonotonicOffsetNs)
            }

            // Default case for most non-realtime devices.
            return OutputMatcher.EXACT
        }

        private inline fun CameraStream.Config.isRealtimeTimebase() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                this.outputs.any {
                    it.timestampBase == OutputStream.TimestampBase.TIMESTAMP_BASE_REALTIME
                }

        private inline fun CameraStream.Config.isSensorTimebase() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                this.outputs.any {
                    it.timestampBase == OutputStream.TimestampBase.TIMESTAMP_BASE_SENSOR
                }

        private inline fun CameraStream.Config.isDefaultTimebase() =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                this.outputs.all {
                    it.timestampBase == null ||
                        it.timestampBase == OutputStream.TimestampBase.TIMESTAMP_BASE_DEFAULT
                }

        private inline fun ImageSourceConfig.isVideoEncodeUsage(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                this.usageFlags != null &&
                this.usageFlags and HardwareBuffer.USAGE_VIDEO_ENCODE != 0L

        private inline fun ImageSourceConfig.isHwComposerUsage(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                this.usageFlags != null &&
                this.usageFlags and HardwareBuffer.USAGE_COMPOSER_OVERLAY != 0L
    }
}
