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
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.RequestProcessorAdapter
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
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessor.CaptureCallback
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.streamsharing.StreamSharing
import androidx.concurrent.futures.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCamera2Interop::class)
class SessionProcessorManager(
    private val sessionProcessor: SessionProcessor,
    private val cameraInfoInternal: CameraInfoInternal,
    private val scope: CoroutineScope,
) {
    private val lock = Any()

    enum class State {
        /**
         * [CREATED] is the initial state, and indicates that the [SessionProcessorManager] has been
         * created but not initialized yet.
         */
        CREATED,

        /**
         * [INITIALIZED] indicates that the [SessionProcessor] has been initialized and we've
         * received the updated session configurations. See also: [SessionProcessor.deInitSession].
         */
        INITIALIZED,

        /**
         * [STARTED] indicates that we've provided our [androidx.camera.core.impl.RequestProcessor]
         * implementation to [SessionProcessor]. See also [SessionProcessor.onCaptureSessionStart].
         */
        STARTED,

        /**
         * [CLOSING] indicates that we're ending our capture session, and we'll no longer accept any
         * further capture requests. See also: [SessionProcessor.onCaptureSessionEnd].
         */
        CLOSING,

        /**
         * [CLOSED] indicates that the underlying capture session has been completely closed and
         * we've de-initialized the session. See also: [SessionProcessor.deInitSession].
         */
        CLOSED,
    }

    @GuardedBy("lock") private var state: State = State.CREATED

    @GuardedBy("lock") private var sessionOptions = CaptureRequestOptions.Builder().build()

    @GuardedBy("lock") private var stillCaptureOptions = CaptureRequestOptions.Builder().build()

    @GuardedBy("lock") private var requestProcessor: RequestProcessorAdapter? = null

    @GuardedBy("lock") private var pendingCaptureConfigs: List<CaptureConfig>? = null

    @GuardedBy("lock") private var pendingCaptureCallbacks: List<CaptureCallback>? = null

    @GuardedBy("lock")
    internal var sessionConfig: SessionConfig? = null
        set(value) =
            synchronized(lock) {
                field = checkNotNull(value)
                if (state != State.STARTED) return
                checkNotNull(requestProcessor).sessionConfig = value
                sessionOptions =
                    CaptureRequestOptions.Builder.from(value.implementationOptions).build()
                updateOptions()
            }

    internal fun isClosed() = synchronized(lock) { state == State.CLOSED || state == State.CLOSING }

    internal fun initialize(
        useCaseManager: UseCaseManager,
        useCases: List<UseCase>,
        timeoutMillis: Long = 5_000L,
        configure: (UseCaseManager.Companion.UseCaseManagerConfig?) -> Unit,
    ) =
        scope.launch {
            val sessionConfigAdapter = SessionConfigAdapter(useCases, null)
            val deferrableSurfaces = sessionConfigAdapter.deferrableSurfaces
            val surfaces = getSurfaces(deferrableSurfaces, timeoutMillis)
            if (!isActive) return@launch configure(null)
            if (surfaces.isEmpty()) {
                Log.error {
                    "Cannot initialize ${this@SessionProcessorManager}: Surface list is empty"
                }
                return@launch configure(null)
            }
            if (surfaces.contains(null)) {
                Log.error {
                    "Cannot initialize ${this@SessionProcessorManager}: Some Surfaces are invalid!"
                }
                sessionConfigAdapter.reportSurfaceInvalid(
                    deferrableSurfaces[surfaces.indexOf(null)]
                )
                return@launch configure(null)
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
            var postviewDeferrableSurface: DeferrableSurface? = null
            if (postviewOutputConfig != null) {
                postviewDeferrableSurface = postviewOutputConfig.surface
                postviewOutputSurface =
                    createOutputSurface(
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

            // IMPORTANT: The critical section (covered by synchronized) is intentionally expanded
            // to cover the sections where we increment and decrement (on failure) the use count on
            // the DeferrableSurfaces. This is needed because the SessionProcessorManager could be
            // closed while we're still initializing, and we need to make sure we either initialize
            // to a point where all the lifetimes of Surfaces are setup or we don't initialize at
            // all beyond this point.
            val processorSessionConfig =
                synchronized(lock) {
                    if (isClosed()) return@synchronized null
                    try {
                        val surfacesToIncrement = ArrayList(deferrableSurfaces)
                        postviewDeferrableSurface?.let { surfacesToIncrement.add(it) }
                        DeferrableSurfaces.incrementAll(surfacesToIncrement)
                    } catch (exception: DeferrableSurface.SurfaceClosedException) {
                        sessionConfigAdapter.reportSurfaceInvalid(exception.deferrableSurface)
                        return@synchronized null
                    }
                    try {
                        Log.debug { "Invoking $sessionProcessor SessionProcessor#initSession" }
                        sessionProcessor
                            .initSession(
                                cameraInfoInternal,
                                OutputSurfaceConfiguration.create(
                                    previewOutputSurface!!,
                                    captureOutputSurface!!,
                                    analysisOutputSurface,
                                    postviewOutputSurface,
                                ),
                            )
                            .also { state = State.INITIALIZED }
                    } catch (throwable: Throwable) {
                        Log.error(throwable) { "initSession() failed" }
                        DeferrableSurfaces.decrementAll(deferrableSurfaces)
                        postviewDeferrableSurface?.decrementUseCount()
                        throw throwable
                    }
                } ?: return@launch configure(null)

            // DecrementAll the output surfaces when ProcessorSurface terminates.
            processorSessionConfig.surfaces
                .first()
                .terminationFuture
                .addListener(
                    {
                        DeferrableSurfaces.decrementAll(deferrableSurfaces)
                        postviewDeferrableSurface?.decrementUseCount()
                    },
                    CameraXExecutors.directExecutor()
                )

            val processorSessionConfigAdapter =
                SessionConfigAdapter(useCases, processorSessionConfig)

            val streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()
            val cameraGraphConfig =
                useCaseManager.createCameraGraphConfig(
                    processorSessionConfigAdapter,
                    streamConfigMap,
                    isExtensions = true,
                )

            val useCaseManagerConfig =
                UseCaseManager.Companion.UseCaseManagerConfig(
                    useCases,
                    processorSessionConfigAdapter,
                    cameraGraphConfig,
                    streamConfigMap,
                )

            return@launch configure(useCaseManagerConfig)
        }

    internal fun onCaptureSessionStart(requestProcessor: RequestProcessorAdapter) {
        var captureConfigsToIssue: List<CaptureConfig>?
        var captureCallbacksToIssue: List<CaptureCallback>?
        synchronized(lock) {
            if (state != State.INITIALIZED) {
                Log.warn { "onCaptureSessionStart called on an uninitialized extensions session" }
                return
            }
            requestProcessor.sessionConfig = sessionConfig
            this.requestProcessor = requestProcessor

            captureConfigsToIssue = pendingCaptureConfigs
            captureCallbacksToIssue = pendingCaptureCallbacks
            pendingCaptureConfigs = null
            pendingCaptureCallbacks = null

            Log.debug { "Invoking SessionProcessor#onCaptureSessionStart" }
            sessionProcessor.onCaptureSessionStart(requestProcessor)

            state = State.STARTED
        }
        val tagBundle = sessionConfig?.repeatingCaptureConfig?.tagBundle ?: TagBundle.emptyBundle()
        startRepeating(tagBundle)
        captureConfigsToIssue?.let { captureConfigs ->
            submitCaptureConfigs(captureConfigs, checkNotNull(captureCallbacksToIssue))
        }
    }

    internal fun startRepeating(
        tagBundle: TagBundle,
        captureCallback: CaptureCallback = object : CaptureCallback {}
    ) {
        synchronized(lock) {
            if (state != State.STARTED) return
            Log.debug { "Invoking SessionProcessor#startRepeating" }
            sessionProcessor.startRepeating(tagBundle, captureCallback)
        }
    }

    internal fun stopRepeating() {
        synchronized(lock) {
            if (state != State.STARTED) return
            Log.debug { "Invoking SessionProcessor#stopRepeating" }
            sessionProcessor.stopRepeating()
        }
    }

    internal fun submitCaptureConfigs(
        captureConfigs: List<CaptureConfig>,
        captureCallbacks: List<CaptureCallback>,
    ) =
        synchronized(lock) {
            check(captureConfigs.size == captureCallbacks.size)
            if (state != State.STARTED) {
                // The lifetime of image capture requests is separate from the extensions lifetime.
                // It is therefore possible for capture requests to be issued when the capture
                // session hasn't yet started (before invoking
                // SessionProcessor.onCaptureSessionStart). This is a copy of camera-camera2's
                // behavior where it stores the last capture configs that weren't submitted.
                Log.info {
                    "SessionProcessor#submitCaptureConfigs: Session not yet started. " +
                        "The capture requests will be submitted later"
                }
                pendingCaptureConfigs = captureConfigs
                pendingCaptureCallbacks = captureCallbacks
                return
            }
            for ((config, callback) in captureConfigs.zip(captureCallbacks)) {
                if (config.templateType == CameraDevice.TEMPLATE_STILL_CAPTURE) {
                    val builder = CaptureRequestOptions.Builder.from(config.implementationOptions)
                    if (
                        config.implementationOptions.containsOption(CaptureConfig.OPTION_ROTATION)
                    ) {
                        builder.setCaptureRequestOption(
                            CaptureRequest.JPEG_ORIENTATION,
                            config.implementationOptions.retrieveOption(
                                CaptureConfig.OPTION_ROTATION
                            )
                        )
                    }
                    if (
                        config.implementationOptions.containsOption(
                            CaptureConfig.OPTION_JPEG_QUALITY
                        )
                    ) {
                        builder.setCaptureRequestOption(
                            CaptureRequest.JPEG_QUALITY,
                            config.implementationOptions
                                .retrieveOption(CaptureConfig.OPTION_JPEG_QUALITY)!!
                                .toByte()
                        )
                    }
                    synchronized(lock) {
                        stillCaptureOptions = builder.build()
                        updateOptions()
                    }
                    Log.debug { "Invoking SessionProcessor.startCapture()" }
                    sessionProcessor.startCapture(
                        config.isPostviewEnabled,
                        config.tagBundle,
                        callback
                    )
                } else {
                    val options =
                        CaptureRequestOptions.Builder.from(config.implementationOptions).build()
                    Log.debug { "Invoking SessionProcessor.startTrigger()" }
                    sessionProcessor.startTrigger(options, config.tagBundle, callback)
                }
            }
        }

    internal fun prepareClose() =
        synchronized(lock) {
            if (state == State.STARTED) {
                sessionProcessor.onCaptureSessionEnd()
            }
            // If we have an initialized SessionProcessor session (i.e., initSession was called), we
            // need to make sure close() invokes deInitSession and only does it when necessary.
            if (state == State.INITIALIZED || state == State.STARTED) {
                state = State.CLOSING
            } else {
                state = State.CLOSED
            }
        }

    internal fun close() =
        synchronized(lock) {
            // These states indicate that we had previously initialized a session (but not yet
            // de-initialized), and thus we need to de-initialize the session here.
            if (state == State.INITIALIZED || state == State.STARTED || state == State.CLOSING) {
                Log.debug { "Invoking $sessionProcessor SessionProcessor#deInitSession" }
                sessionProcessor.deInitSession()
            }
            state = State.CLOSED
        }

    @GuardedBy("lock")
    private fun updateOptions() {
        val builder =
            Camera2ImplConfig.Builder().apply {
                insertAllOptions(sessionOptions)
                insertAllOptions(stillCaptureOptions)
            }
        sessionProcessor.setParameters(builder.build())
    }

    companion object {
        private suspend fun getSurfaces(
            deferrableSurfaces: List<DeferrableSurface>,
            timeoutMillis: Long,
        ): List<Surface?> {
            return withTimeoutOrNull(timeMillis = timeoutMillis) {
                    Futures.successfulAsList(
                            deferrableSurfaces.map {
                                Futures.nonCancellationPropagating(it.surface)
                            }
                        )
                        .await()
                }
                .orEmpty()
        }

        private fun createOutputSurface(deferrableSurface: DeferrableSurface, surface: Surface) =
            OutputSurface.create(
                surface,
                deferrableSurface.prescribedSize,
                deferrableSurface.prescribedStreamFormat,
            )
    }
}
