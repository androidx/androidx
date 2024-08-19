/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequests
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic

/** A CaptureSequence used for testing interactions with a [FakeCaptureSequenceProcessor] */
public data class FakeCaptureSequence(
    override val repeating: Boolean,
    override val cameraId: CameraId,
    override val captureRequestList: List<Request>,
    override val captureMetadataList: List<RequestMetadata>,
    val requestMetadata: Map<Request, RequestMetadata>,
    val defaultParameters: Map<*, Any?>,
    val requiredParameters: Map<*, Any?>,
    override val listeners: List<Request.Listener>,
    override val sequenceListener: CaptureSequence.CaptureSequenceListener,
    override var sequenceNumber: Int,
) : CaptureSequence<Request> {
    public fun invokeOnSequenceCreated(): Unit = invokeOnRequests { requestMetadata, _, listener ->
        listener.onRequestSequenceCreated(requestMetadata)
    }

    public fun invokeOnSequenceSubmitted(): Unit =
        invokeOnRequests { requestMetadata, _, listener ->
            listener.onRequestSequenceSubmitted(requestMetadata)
        }

    public fun invokeOnSequenceAborted(): Unit = invokeOnRequests { requestMetadata, _, listener ->
        listener.onRequestSequenceAborted(requestMetadata)
    }

    public fun invokeOnSequenceCompleted(frameNumber: FrameNumber): Unit =
        invokeOnRequests { requestMetadata, _, listener ->
            listener.onRequestSequenceCompleted(requestMetadata, frameNumber)
        }

    public companion object {
        private val requestNumbers = atomic(0L)
        private val fakeCaptureSequenceListener = FakeCaptureSequenceListener()

        public fun create(
            cameraId: CameraId,
            repeating: Boolean,
            requests: List<Request>,
            surfaceMap: Map<StreamId, Surface>,
            defaultTemplate: RequestTemplate = RequestTemplate(1),
            defaultParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
            requiredParameters: Map<*, Any?> = emptyMap<Any, Any?>(),
            listeners: List<Request.Listener> = emptyList(),
            sequenceListener: CaptureSequence.CaptureSequenceListener = fakeCaptureSequenceListener
        ): FakeCaptureSequence? {
            if (surfaceMap.isEmpty()) {
                println(
                    "No surfaces configured for $this! Cannot build CaptureSequence for $requests"
                )
                return null
            }

            val requestInfoMap = mutableMapOf<Request, RequestMetadata>()
            val requestInfoList = mutableListOf<RequestMetadata>()
            for (request in requests) {
                val captureParameters = mutableMapOf<CaptureRequest.Key<*>, Any?>()
                val metadataParameters = mutableMapOf<Metadata.Key<*>, Any?>()
                for ((k, v) in defaultParameters) {
                    if (k != null) {
                        if (k is CaptureRequest.Key<*>) {
                            captureParameters[k] = v
                        } else if (k is Metadata.Key<*>) {
                            metadataParameters[k] = v
                        }
                    }
                }
                for ((k, v) in request.parameters) {
                    captureParameters[k] = v
                }
                for ((k, v) in request.extras) {
                    metadataParameters[k] = v
                }
                for ((k, v) in requiredParameters) {
                    if (k != null) {
                        if (k is CaptureRequest.Key<*>) {
                            captureParameters[k] = v
                        } else if (k is Metadata.Key<*>) {
                            metadataParameters[k] = v
                        }
                    }
                }

                val requestNumber = RequestNumber(requestNumbers.incrementAndGet())
                val streamMap = mutableMapOf<StreamId, Surface>()
                var hasSurface = false
                for (stream in request.streams) {
                    val surface = surfaceMap[stream]
                    if (surface == null) {
                        println("Failed to find surface for $stream on $request")
                        continue
                    }
                    hasSurface = true
                    streamMap[stream] = surface
                }

                if (!hasSurface) {
                    println("No surfaces configured for $request! Cannot build CaptureSequence.")
                    return null
                }

                val requestMetadata =
                    FakeRequestMetadata(
                        request = request,
                        requestParameters = captureParameters,
                        metadata = metadataParameters,
                        template = request.template ?: defaultTemplate,
                        streams = streamMap,
                        repeating = repeating,
                        requestNumber = requestNumber
                    )
                requestInfoList.add(requestMetadata)
                requestInfoMap[request] = requestMetadata
            }

            // Copy maps / lists for tests.
            return FakeCaptureSequence(
                repeating = repeating,
                cameraId = cameraId,
                captureRequestList = requests.toList(),
                captureMetadataList = requestInfoList,
                requestMetadata = requestInfoMap,
                defaultParameters = defaultParameters.toMap(),
                requiredParameters = requiredParameters.toMap(),
                listeners = listeners.toList(),
                sequenceListener = sequenceListener,
                sequenceNumber = -1 // Sequence number is not set until it has been submitted.
            )
        }
    }
}
