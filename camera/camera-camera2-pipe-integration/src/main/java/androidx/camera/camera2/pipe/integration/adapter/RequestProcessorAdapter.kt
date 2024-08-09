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

package androidx.camera.camera2.pipe.integration.adapter

import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.core.CoroutineMutex
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.withLockLaunch
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.camera2.pipe.integration.impl.Camera2ImplConfig
import androidx.camera.camera2.pipe.integration.impl.toParameters
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessorSurface
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope

public class RequestProcessorAdapter(
    private val useCaseGraphConfig: UseCaseGraphConfig,
    private val processorSurfaces: List<SessionProcessorSurface>,
    private val scope: CoroutineScope,
) : RequestProcessor {
    private val coroutineMutex = CoroutineMutex()
    private val sequenceIds = atomic(0)
    internal var sessionConfig: SessionConfig? = null

    private class RequestProcessorCallbackAdapter(
        private val callback: RequestProcessor.Callback,
        private val sequenceId: Int,
        private val shouldInvokeSequenceCallback: Boolean,
        private val request: RequestProcessor.Request,
        private val requestProcessorAdapter: RequestProcessorAdapter,
    ) : Request.Listener {
        override fun onStarted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            timestamp: CameraTimestamp
        ) {
            callback.onCaptureStarted(request, frameNumber.value, timestamp.value)
        }

        override fun onPartialCaptureResult(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            captureResult: FrameMetadata
        ) {
            callback.onCaptureProgressed(
                request,
                PartialCaptureResultAdapter(requestMetadata, frameNumber, captureResult)
            )
        }

        override fun onComplete(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            result: FrameInfo
        ) {
            callback.onCaptureCompleted(
                request,
                CaptureResultAdapter(requestMetadata, frameNumber, result)
            )
        }

        override fun onFailed(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            requestFailure: RequestFailure
        ) {
            callback.onCaptureFailed(request, CaptureFailureAdapter(requestFailure))
        }

        override fun onBufferLost(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber,
            stream: StreamId
        ) {
            val surface = requestProcessorAdapter.getDeferrableSurface(stream)
            if (surface != null && surface is SessionProcessorSurface) {
                callback.onCaptureBufferLost(request, frameNumber.value, surface.outputConfigId)
            }
        }

        override fun onRequestSequenceCompleted(
            requestMetadata: RequestMetadata,
            frameNumber: FrameNumber
        ) {
            if (!shouldInvokeSequenceCallback) {
                return
            }
            callback.onCaptureSequenceCompleted(sequenceId, frameNumber.value)
        }

        override fun onRequestSequenceAborted(requestMetadata: RequestMetadata) {
            if (!shouldInvokeSequenceCallback) {
                return
            }
            callback.onCaptureSequenceAborted(sequenceId)
        }
    }

    override fun submit(
        request: RequestProcessor.Request,
        callback: RequestProcessor.Callback
    ): Int {
        return submit(mutableListOf(request), callback)
    }

    override fun submit(
        requests: MutableList<RequestProcessor.Request>,
        callback: RequestProcessor.Callback
    ): Int {
        Log.debug { "$this#submit" }
        val sequenceId = sequenceIds.incrementAndGet()
        val requestsToSubmit =
            requests.mapIndexed { index, request ->
                val parameters =
                    sessionConfig?.let { sessionConfig ->
                        val builder =
                            Camera2ImplConfig.Builder().apply {
                                insertAllOptions(
                                    sessionConfig.repeatingCaptureConfig.implementationOptions
                                )
                                insertAllOptions(request.parameters)
                            }
                        builder.build().toParameters()
                    }
                        ?: Camera2ImplConfig.Builder()
                            .insertAllOptions(request.parameters)
                            .build()
                            .toParameters()

                Request(
                    template = RequestTemplate(request.templateId),
                    parameters = parameters,
                    streams =
                        request.targetOutputConfigIds
                            .mapNotNull { findSurface(it) }
                            .mapNotNull { useCaseGraphConfig.surfaceToStreamMap[it] },
                    listeners =
                        listOf(
                            RequestProcessorCallbackAdapter(
                                callback,
                                sequenceId,
                                shouldInvokeSequenceCallback = index == 0,
                                request,
                                this,
                            )
                        )
                )
            }

        coroutineMutex.withLockLaunch(scope) {
            useCaseGraphConfig.graph.acquireSession().use { it.submit(requestsToSubmit) }
        }
        return sequenceId
    }

    override fun setRepeating(
        request: RequestProcessor.Request,
        callback: RequestProcessor.Callback
    ): Int {
        Log.debug { "$this#setRepeating" }
        val sequenceId = sequenceIds.incrementAndGet()
        val requestsToSubmit =
            Request(
                template = RequestTemplate(request.templateId),
                parameters =
                    Camera2ImplConfig.Builder()
                        .insertAllOptions(request.parameters)
                        .build()
                        .toParameters(),
                extras =
                    mapOf(CAMERAX_TAG_BUNDLE to sessionConfig!!.repeatingCaptureConfig.tagBundle),
                streams =
                    request.targetOutputConfigIds
                        .mapNotNull { findSurface(it) }
                        .mapNotNull { useCaseGraphConfig.surfaceToStreamMap[it] },
                listeners =
                    listOf(
                        RequestProcessorCallbackAdapter(
                            callback,
                            sequenceId,
                            shouldInvokeSequenceCallback = true,
                            request,
                            this
                        )
                    )
            )
        coroutineMutex.withLockLaunch(scope) {
            useCaseGraphConfig.graph.acquireSession().use { it.startRepeating(requestsToSubmit) }
        }
        return sequenceId
    }

    override fun abortCaptures() {
        Log.debug { "$this#abortCaptures" }
        coroutineMutex.withLockLaunch(scope) {
            useCaseGraphConfig.graph.acquireSession().use { it.abort() }
        }
    }

    override fun stopRepeating() {
        Log.debug { "$this#stopRepeating" }
        coroutineMutex.withLockLaunch(scope) {
            useCaseGraphConfig.graph.acquireSession().use { it.stopRepeating() }
        }
    }

    private fun findSurface(outputConfigId: Int): DeferrableSurface? =
        processorSurfaces.find { it.outputConfigId == outputConfigId }

    private fun getDeferrableSurface(stream: StreamId): DeferrableSurface? {
        for (entry in useCaseGraphConfig.surfaceToStreamMap.entries.iterator()) {
            if (entry.value == stream) {
                return entry.key
            }
        }
        return null
    }
}
