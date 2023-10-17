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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.compat.CameraPipeKeys
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.DeferrableSurfaces
import androidx.camera.core.impl.OutputSurface
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessor.CaptureCallback
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.streamsharing.StreamSharing
import androidx.concurrent.futures.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCamera2Interop::class)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SessionProcessorManager(
    private val sessionProcessor: SessionProcessor,
    private val cameraInfoInternal: CameraInfoInternal,
    private val scope: CoroutineScope,
) {
    private var sessionOptions = CaptureRequestOptions.Builder().build()
    private var stillCaptureOptions = CaptureRequestOptions.Builder().build()
    internal var sessionConfig: SessionConfig? = null
        set(value) {
            field = checkNotNull(value)
            sessionOptions =
                CaptureRequestOptions.Builder.from(value.implementationOptions).build()
            updateOptions()
        }

    internal fun initialize(
        useCaseManager: UseCaseManager,
        useCases: List<UseCase>,
        timeoutMillis: Long = 5_000L,
    ) = scope.launch {
        val sessionConfigAdapter = SessionConfigAdapter(useCases, null)
        val deferrableSurfaces = sessionConfigAdapter.deferrableSurfaces
        val surfaces = getSurfaces(deferrableSurfaces, timeoutMillis)
        if (!isActive) return@launch
        if (surfaces.isEmpty()) {
            Log.error { "Surface list is empty" }
            return@launch
        }
        if (surfaces.contains(null)) {
            Log.error { "Some Surfaces are invalid!" }
            sessionConfigAdapter.reportSurfaceInvalid(
                deferrableSurfaces[surfaces.indexOf(null)]
            )
            return@launch
        }
        var previewOutputSurface: OutputSurface? = null
        var captureOutputSurface: OutputSurface? = null
        var analysisOutputSurface: OutputSurface? = null
        var postviewOutputSurface: OutputSurface? = null
        for ((deferrableSurface, surface) in deferrableSurfaces.zip(surfaces)) {
            when (deferrableSurface.containerClass) {
                Preview::class.java ->
                    previewOutputSurface = createOutputSurface(deferrableSurface, surface!!)

                StreamSharing::class.java ->
                    previewOutputSurface = createOutputSurface(deferrableSurface, surface!!)

                ImageCapture::class.java ->
                    captureOutputSurface = createOutputSurface(deferrableSurface, surface!!)

                ImageAnalysis::class.java ->
                    analysisOutputSurface = createOutputSurface(deferrableSurface, surface!!)
            }
        }
        val postviewOutputConfig =
            sessionConfigAdapter.getValidSessionConfigOrNull()?.postviewOutputConfig
        if (postviewOutputConfig != null) {
            val postviewDeferrableSurface = postviewOutputConfig.surface
            postviewOutputSurface = createOutputSurface(
                postviewDeferrableSurface,
                postviewDeferrableSurface.surface.get()!!
            )
        }
        Log.debug {
            "SessionProcessorSurfaceManager: Identified surfaces: " +
                "previewOutputSurface = $previewOutputSurface, " +
                "captureOutputSurface = $captureOutputSurface, " +
                "analysisOutputSurface = $analysisOutputSurface, " +
                "postviewOutputSurface = $postviewOutputSurface"
        }

        try {
            DeferrableSurfaces.incrementAll(deferrableSurfaces)
        } catch (exception: DeferrableSurface.SurfaceClosedException) {
            sessionConfigAdapter.reportSurfaceInvalid(exception.deferrableSurface)
            return@launch
        }
        val processorSessionConfig = try {
            sessionProcessor.initSession(
                cameraInfoInternal,
                OutputSurfaceConfiguration.create(
                    previewOutputSurface!!,
                    captureOutputSurface!!,
                    analysisOutputSurface,
                    postviewOutputSurface,
                ),
            )
        } catch (throwable: Throwable) {
            Log.error(throwable) { "initSession() failed" }
            DeferrableSurfaces.decrementAll(deferrableSurfaces)
            throw throwable
        }

        // DecrementAll the output surfaces when ProcessorSurface terminates.
        processorSessionConfig.surfaces.first().terminationFuture.addListener({
            DeferrableSurfaces.decrementAll(deferrableSurfaces)
        }, CameraXExecutors.directExecutor())

        val processorSessionConfigAdapter =
            SessionConfigAdapter(useCases, processorSessionConfig)

        val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()
        val cameraGraphConfig = useCaseManager.createCameraGraphConfig(
            processorSessionConfigAdapter,
            streamConfigMap,
            mapOf(CameraPipeKeys.ignore3ARequiredParameters to true)
        )

        val useCaseManagerConfig = UseCaseManager.Companion.UseCaseManagerConfig(
            useCases,
            processorSessionConfigAdapter,
            cameraGraphConfig,
            streamConfigMap,
        )

        useCaseManager.tryResumeUseCaseManager(useCaseManagerConfig)
    }

    internal fun onCaptureSessionStart(requestProcessor: RequestProcessor) {
        sessionProcessor.onCaptureSessionStart(requestProcessor)
    }

    internal fun startRepeating(captureCallback: CaptureCallback) {
        sessionProcessor.startRepeating(captureCallback)
    }

    internal fun submitCaptureConfigs(
        captureConfigs: List<CaptureConfig>,
        captureCallbacks: List<CaptureCallback>,
    ) {
        check(captureConfigs.size == captureCallbacks.size)
        for ((config, callback) in captureConfigs.zip(captureCallbacks)) {
            if (config.templateType == CameraDevice.TEMPLATE_STILL_CAPTURE) {
                val builder = CaptureRequestOptions.Builder.from(config.implementationOptions)
                if (config.implementationOptions.containsOption(CaptureConfig.OPTION_ROTATION)) {
                    builder.setCaptureRequestOption(
                        CaptureRequest.JPEG_ORIENTATION,
                        config.implementationOptions.retrieveOption(CaptureConfig.OPTION_ROTATION)
                    )
                }
                if (config.implementationOptions.containsOption(
                        CaptureConfig.OPTION_JPEG_QUALITY
                    )
                ) {
                    builder.setCaptureRequestOption(
                        CaptureRequest.JPEG_QUALITY,
                        config.implementationOptions.retrieveOption(
                            CaptureConfig.OPTION_JPEG_QUALITY
                        )!!.toByte()
                    )
                }
                stillCaptureOptions = builder.build()
                updateOptions()
                Log.debug { "Invoking SessionProcessor.startCapture()" }
                sessionProcessor.startCapture(config.isPostviewEnabled, callback)
            } else {
                val options =
                    CaptureRequestOptions.Builder.from(config.implementationOptions).build()
                Log.debug { "Invoking SessionProcessor.startTrigger()" }
                sessionProcessor.startTrigger(options, callback)
            }
        }
    }

    internal fun onCaptureSessionEnd() {
        sessionProcessor.onCaptureSessionEnd()
    }

    private fun updateOptions() {
        val builder = Camera2ImplConfig.Builder().apply {
            insertAllOptions(sessionOptions)
            insertAllOptions(stillCaptureOptions)
        }
        sessionProcessor.setParameters(builder.build())
    }

    internal fun close() {
        sessionProcessor.deInitSession()
    }

    companion object {
        private suspend fun getSurfaces(
            deferrableSurfaces: List<DeferrableSurface>,
            timeoutMillis: Long,
        ): List<Surface?> {
            return withTimeoutOrNull(timeMillis = timeoutMillis) {
                Futures.successfulAsList(deferrableSurfaces.map {
                    Futures.nonCancellationPropagating(it.surface)
                }).await()
            }.orEmpty()
        }

        private fun createOutputSurface(
            deferrableSurface: DeferrableSurface,
            surface: Surface
        ) =
            OutputSurface.create(
                surface,
                deferrableSurface.prescribedSize,
                deferrableSurface.prescribedStreamFormat,
            )
    }
}
