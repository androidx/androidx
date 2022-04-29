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

package androidx.camera.testing.fakes

import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.media.ImageWriter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.OutputSurface
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessorSurface
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

const val FAKE_CAPTURE_SEQUENCE_ID = 1

@RequiresApi(23)
class FakeSessionProcessor(
    val inputFormatPreview: Int?,
    val inputFormatCapture: Int?
) : SessionProcessor {
    private lateinit var previewProcessorSurface: DeferrableSurface
    private lateinit var captureProcessorSurface: DeferrableSurface
    private var intermediaPreviewImageReader: ImageReader? = null
    private var intermediaCaptureImageReader: ImageReader? = null
    private var intermediaPreviewImageWriter: ImageWriter? = null
    private var intermediaCaptureImageWriter: ImageWriter? = null

    private val previewOutputConfigId = 1
    private val captureOutputConfigId = 2

    private var requestProcessor: RequestProcessor? = null

    // Values of these Deferred are the timestamp to complete.
    private val initSessionCalled = CompletableDeferred<Long>()
    private val deInitSessionCalled = CompletableDeferred<Long>()
    private val onCaptureSessionStartCalled = CompletableDeferred<Long>()
    private val onCaptureSessionEndCalled = CompletableDeferred<Long>()
    private val startRepeatingCalled = CompletableDeferred<Long>()
    private val startCaptureCalled = CompletableDeferred<Long>()
    private val setParametersCalled = CompletableDeferred<Config>()
    private var latestParameters: Config = OptionsBundle.emptyBundle()
    private var blockRunAfterInitSession: () -> Unit = {}

    fun releaseSurfaces() {
        intermediaPreviewImageReader?.close()
        intermediaCaptureImageReader?.close()
    }

    fun runAfterInitSession(block: () -> Unit) {
        blockRunAfterInitSession = block
    }

    override fun initSession(
        cameraInfo: CameraInfo,
        previewSurfaceConfig: OutputSurface,
        imageCaptureSurfaceConfig: OutputSurface,
        imageAnalysisSurfaceConfig: OutputSurface?
    ): SessionConfig {
        initSessionCalled.complete(SystemClock.elapsedRealtimeNanos())
        val handler = Handler(Looper.getMainLooper())

        var sessionBuilder = SessionConfig.Builder()

        // Preview
        lateinit var previewTransformedSurface: Surface
        if (inputFormatPreview == null) { // no conversion, use origin surface.
            previewTransformedSurface = previewSurfaceConfig.surface
        } else {
            intermediaPreviewImageReader = ImageReader.newInstance(
                640, 480,
                inputFormatPreview, 2
            )
            previewTransformedSurface = intermediaPreviewImageReader!!.surface

            intermediaPreviewImageWriter = ImageWriter.newInstance(
                previewSurfaceConfig.surface, 2
            )

            intermediaPreviewImageReader!!.setOnImageAvailableListener(
                {
                    it.acquireNextImage().use {
                        val imageDequeued = intermediaPreviewImageWriter!!.dequeueInputImage()
                        intermediaPreviewImageWriter!!.queueInputImage(imageDequeued)
                    }
                },
                handler
            )
        }
        previewProcessorSurface =
            SessionProcessorSurface(previewTransformedSurface, previewOutputConfigId)
        previewProcessorSurface.terminationFuture.addListener(
            {
                intermediaPreviewImageReader?.close()
                intermediaPreviewImageWriter?.close()
            },
            CameraXExecutors.directExecutor()
        )
        sessionBuilder.addSurface(previewProcessorSurface)

        // Capture
        lateinit var captureTransformedSurface: Surface
        if (inputFormatCapture == null) { // no conversion, use origin surface.
            captureTransformedSurface = imageCaptureSurfaceConfig.surface
        } else {
            intermediaCaptureImageReader = ImageReader.newInstance(
                640, 480,
                inputFormatCapture, 2
            )
            captureTransformedSurface = intermediaCaptureImageReader!!.surface

            intermediaCaptureImageWriter = ImageWriter.newInstance(
                imageCaptureSurfaceConfig.surface, 2
            )

            intermediaCaptureImageReader!!.setOnImageAvailableListener(
                {
                    it.acquireNextImage().use {
                        val imageDequeued = intermediaCaptureImageWriter!!.dequeueInputImage()
                        intermediaCaptureImageWriter!!.queueInputImage(imageDequeued)
                    }
                },
                handler
            )
        }
        captureProcessorSurface =
            SessionProcessorSurface(captureTransformedSurface, captureOutputConfigId)

        captureProcessorSurface.terminationFuture.addListener(
            {
                intermediaCaptureImageReader?.close()
                intermediaCaptureImageWriter?.close()
            },
            CameraXExecutors.directExecutor()
        )
        sessionBuilder.addSurface(captureProcessorSurface)

        sessionBuilder.setTemplateType(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG)
        val sessionConfig = sessionBuilder.build()
        blockRunAfterInitSession()
        return sessionConfig
    }

    override fun deInitSession() {
        deInitSessionCalled.complete(SystemClock.elapsedRealtimeNanos())
        previewProcessorSurface.close()
        captureProcessorSurface.close()
    }

    override fun setParameters(config: Config) {
        setParametersCalled.complete(config)
        latestParameters = config
    }

    override fun onCaptureSessionStart(_requestProcessor: RequestProcessor) {
        onCaptureSessionStartCalled.complete(SystemClock.elapsedRealtimeNanos())
        requestProcessor = _requestProcessor
    }

    override fun onCaptureSessionEnd() {
        onCaptureSessionEndCalled.complete(SystemClock.elapsedRealtimeNanos())
    }

    fun getLatestParameters(): Config {
        return latestParameters
    }

    override fun startRepeating(callback: SessionProcessor.CaptureCallback): Int {
        startRepeatingCalled.complete(SystemClock.elapsedRealtimeNanos())
        val builder = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(previewOutputConfigId)
            setParameters(latestParameters)
            setTemplateId(CameraDevice.TEMPLATE_PREVIEW)
        }

        requestProcessor!!.setRepeating(
            builder.build(),
            object : RequestProcessor.Callback {
                override fun onCaptureStarted(
                    request: RequestProcessor.Request,
                    frameNumber: Long,
                    timestamp: Long
                ) {}

                override fun onCaptureProgressed(
                    request: RequestProcessor.Request,
                    captureResult: CameraCaptureResult
                ) {}

                override fun onCaptureCompleted(
                    request: RequestProcessor.Request,
                    captureResult: CameraCaptureResult
                ) {
                    callback.onCaptureSequenceCompleted(1)
                }

                override fun onCaptureFailed(
                    request: RequestProcessor.Request,
                    captureFailure: CameraCaptureFailure
                ) {}

                override fun onCaptureBufferLost(
                    request: RequestProcessor.Request,
                    frameNumber: Long,
                    outputConfigId: Int
                ) {}

                override fun onCaptureSequenceCompleted(
                    sequenceId: Int,
                    frameNumber: Long
                ) {}

                override fun onCaptureSequenceAborted(sequenceId: Int) {
                }
            }
        )
        return FAKE_CAPTURE_SEQUENCE_ID
    }

    override fun stopRepeating() {
    }

    override fun startCapture(callback: SessionProcessor.CaptureCallback): Int {
        startCaptureCalled.complete(SystemClock.elapsedRealtimeNanos())
        val request = RequestProcessorRequest.Builder().apply {
            addTargetOutputConfigId(captureOutputConfigId)
            setParameters(latestParameters)
            setTemplateId(CameraDevice.TEMPLATE_STILL_CAPTURE)
        }.build()

        requestProcessor!!.submit(
            request,
            object : RequestProcessor.Callback {
                override fun onCaptureCompleted(
                    request: RequestProcessor.Request,
                    captureResult: CameraCaptureResult
                ) {
                    callback.onCaptureSequenceCompleted(1)
                }

                override fun onCaptureStarted(
                    request: RequestProcessor.Request,
                    frameNumber: Long,
                    timestamp: Long
                ) {}

                override fun onCaptureProgressed(
                    request: RequestProcessor.Request,
                    captureResult: CameraCaptureResult
                ) {}

                override fun onCaptureFailed(
                    request: RequestProcessor.Request,
                    captureFailure: CameraCaptureFailure
                ) {
                    callback.onCaptureFailed(1)
                }

                override fun onCaptureBufferLost(
                    request: RequestProcessor.Request,
                    frameNumber: Long,
                    outputConfigId: Int
                ) {}

                override fun onCaptureSequenceCompleted(sequenceId: Int, frameNumber: Long) {}

                override fun onCaptureSequenceAborted(sequenceId: Int) {}
            }
        )
        return FAKE_CAPTURE_SEQUENCE_ID
    }

    override fun abortCapture(captureSequenceId: Int) {
    }

    suspend fun assertInitSessionInvoked(): Long {
        return initSessionCalled.awaitWithTimeout(3000)
    }

    suspend fun wasInitSessionInvoked(): Boolean {
        val result = withTimeoutOrNull(3000) { initSessionCalled.await() }
        return result != null
    }

    suspend fun assertDeInitSessionInvoked(): Long {
        return deInitSessionCalled.awaitWithTimeout(3000)
    }

    suspend fun assertOnCaptureSessionStartInvoked(): Long {
        return onCaptureSessionStartCalled.awaitWithTimeout(3000)
    }

    suspend fun wasOnCaptureSessionStartInvoked(): Boolean {
        val result = withTimeoutOrNull(3000) { onCaptureSessionStartCalled.await() }
        return result != null
    }

    suspend fun assertOnCaptureEndInvoked(): Long {
        return onCaptureSessionEndCalled.awaitWithTimeout(3000)
    }

    suspend fun assertStartRepeatingInvoked(): Long {
        return startRepeatingCalled.awaitWithTimeout(3000)
    }

    suspend fun assertStartCaptureInvoked(): Long {
        return startCaptureCalled.awaitWithTimeout(3000)
    }

    suspend fun assertSetParametersInvoked(): Config {
        return setParametersCalled.awaitWithTimeout(3000)
    }

    private suspend fun <T> Deferred<T>.awaitWithTimeout(timeMillis: Long): T {
        return withTimeout(timeMillis) {
            await()
        }
    }
}

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class RequestProcessorRequest(
    private val targetOutputConfigIds: List<Int>,
    private val parameters: Config,
    private val templateId: Int
) : RequestProcessor.Request {
    override fun getTargetOutputConfigIds(): List<Int> {
        return targetOutputConfigIds
    }

    override fun getParameters(): Config {
        return parameters
    }

    override fun getTemplateId(): Int {
        return templateId
    }

    class Builder {
        private var targetOutputConfigIds: MutableList<Int> = ArrayList()
        private var parameters: Config = OptionsBundle.emptyBundle()
        private var templateId = CameraDevice.TEMPLATE_PREVIEW

        fun addTargetOutputConfigId(targetOutputConfigId: Int): Builder {
            targetOutputConfigIds.add(targetOutputConfigId)
            return this
        }

        fun setParameters(parameters: Config): Builder {
            this.parameters = parameters
            return this
        }

        fun setTemplateId(templateId: Int): Builder {
            this.templateId = templateId
            return this
        }

        fun build(): RequestProcessorRequest {
            return RequestProcessorRequest(
                targetOutputConfigIds.toList(),
                OptionsBundle.from(parameters),
                templateId
            )
        }
    }
}
