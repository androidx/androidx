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

/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.impl

import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_STATE_FLASH_REQUIRED
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.adapter.CaptureConfigAdapter
import androidx.camera.camera2.adapter.FrameMetadataConverter.toCameraCaptureResult
import androidx.camera.camera2.adapter.future
import androidx.camera.camera2.compat.workaround.UseTorchAsFlash
import androidx.camera.camera2.compat.workaround.isFlashAvailable
import androidx.camera.camera2.compat.workaround.shouldStopRepeatingBeforeCapture
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.config.UseCaseCameraScope
import androidx.camera.camera2.impl.Camera2Logger.debug
import androidx.camera.camera2.impl.CapturePipelineImpl.PipelineTask.MAIN_CAPTURE
import androidx.camera.camera2.impl.CapturePipelineImpl.PipelineTask.POST_CAPTURE
import androidx.camera.camera2.impl.CapturePipelineImpl.PipelineTask.PRE_CAPTURE
import androidx.camera.camera2.impl.TorchControl.TorchMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.common.CameraFrameNumber
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.FLASH_MODE_SCREEN
import androidx.camera.core.ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH
import androidx.camera.core.ImageCapture.FlashMode
import androidx.camera.core.ImageCapture.FlashType
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ConvergenceUtils
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val CHECK_FLASH_REQUIRED_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(1)
private val CHECK_3A_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(1)
private val CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(5)
private val CHECK_3A_WITH_SCREEN_FLASH_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(2)

public interface CapturePipeline {

    public var template: Int

    public suspend fun submitStillCaptures(
        configs: List<CaptureConfig>,
        requestTemplate: RequestTemplate,
        sessionConfigOptions: Config,
        @CaptureMode captureMode: Int,
        @FlashType flashType: Int,
        @FlashMode flashMode: Int,
    ): List<Deferred<Void?>>

    /** Gets the [CameraCapturePipeline] instance corresponding to a [CapturePipeline] instance. */
    public suspend fun getCameraCapturePipeline(
        @CaptureMode captureMode: Int,
        @FlashMode flashMode: Int,
        @FlashType flashType: Int,
    ): CameraCapturePipeline
}

/** Implementations for the single capture. */
@UseCaseCameraScope
public class CapturePipelineImpl
@Inject
constructor(
    private val configAdapter: CaptureConfigAdapter,
    private val flashControl: FlashControl,
    private val torchControl: TorchControl,
    private val videoUsageControl: VideoUsageControl,
    private val threads: UseCaseThreads,
    private val requestListener: ComboRequestListener,
    private val useTorchAsFlash: UseTorchAsFlash,
    private val cameraProperties: CameraProperties,
    private val useCaseCameraStateProvider: Provider<UseCaseCameraState>,
    private val useCaseCameraContext: UseCaseCameraContext,
    private val lowLightBoostControl: LowLightBoostControl? = null,
) : CapturePipeline {
    private enum class PipelineTask {
        PRE_CAPTURE,
        MAIN_CAPTURE,
        POST_CAPTURE,
    }

    private data class MainCaptureParams(
        val configs: List<CaptureConfig>,
        val requestTemplate: RequestTemplate,
        var sessionConfigOptions: Config,
    )

    // If there is no flash unit, skip the flash related task instead of failing the pipeline.
    private val hasFlashUnit by lazy { cameraProperties.isFlashAvailable() }

    private val useCaseCameraState by lazy { useCaseCameraStateProvider.get() }

    override var template: Int = CameraDevice.TEMPLATE_PREVIEW

    /**
     * A [FrameMetadata] that pipeline tasks can use to determine various info, e.g. whether the
     * flash is required.
     */
    private var frameMetadata: FrameMetadata? = null

    /**
     * Returns a [FrameMetadata] that pipeline tasks can use to determine various info, e.g. whether
     * the flash is required.
     *
     * If [frameMetadata] is not already cached, this function will wait for a new [FrameInfo] from
     * the camera and cache its [FrameMetadata] for the duration of a whole capture. The cache is
     * invalidated at the start of each new capture.
     */
    private suspend fun getFrameMetadata(): FrameMetadata? {
        if (frameMetadata == null) {
            debug { "getFrameMetadata: waiting for result" }
            frameMetadata = waitForResult(CHECK_FLASH_REQUIRED_TIMEOUT_IN_NS)?.metadata
        }
        debug { "getFrameMetadata: frameMetadata = $frameMetadata" }
        return frameMetadata
    }

    /**
     * Invokes various capture pipelines (e.g. pre-capture or main capture or post-capture).
     *
     * @param pipelineTasks List of [PipelineTask] to invoke.
     * @param captureMode [CaptureMode] integer for the capture.
     * @param flashMode [FlashMode] integer for the capture.
     * @param flashType [FlashType] integer for the capture.
     * @param mainCaptureParams Parameters required for the main capture, must not be null if
     *   [pipelineTasks] contain [PipelineTask.MAIN_CAPTURE].
     */
    private suspend fun invokeCaptureTasks(
        pipelineTasks: List<PipelineTask>,
        @CaptureMode captureMode: Int,
        @FlashMode flashMode: Int,
        @FlashType flashType: Int,
        mainCaptureParams: MainCaptureParams?,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        debug {
            "CapturePipeline#invokeCaptureTasks: tasks = $pipelineTasks" +
                ", captureMode = $captureMode, flashMode = $flashMode, flashType = $flashType"
        }

        // frameMetadata is cleared for each new capture pipeline invocation. It is assumed that
        // different captures are not intertwined. Usually, camera-core ImageCapture ensures that
        // captures are queued, so we can assume individual captures are processed one-by-one
        // through this class. If this changes in future, we should ensure each different capture
        // has its own pipeline properties, specially the frameMetadata.
        frameMetadata = null

        if (pipelineTasks.contains(MAIN_CAPTURE)) {
            checkNotNull(mainCaptureParams) { "Must not be null for PipelineType.MAIN_CAPTURE" }
        }

        return if (flashMode == FLASH_MODE_SCREEN) {
            screenFlashCapture(mainCaptureParams, captureMode, pipelineTasks, correctTorchState)
        } else if (isTorchAsFlash(flashType)) {
            torchAsFlashCapture(
                mainCaptureParams,
                captureMode,
                flashMode,
                pipelineTasks,
                correctTorchState,
            )
        } else {
            defaultCapture(
                mainCaptureParams,
                captureMode,
                flashMode,
                pipelineTasks,
                correctTorchState,
            )
        }
    }

    override suspend fun submitStillCaptures(
        configs: List<CaptureConfig>,
        requestTemplate: RequestTemplate,
        sessionConfigOptions: Config,
        @CaptureMode captureMode: Int,
        @FlashType flashType: Int,
        @FlashMode flashMode: Int,
    ): List<Deferred<Void?>> =
        submitStillCaptures(
            configs = configs,
            requestTemplate = requestTemplate,
            sessionConfigOptions = sessionConfigOptions,
            captureMode = captureMode,
            flashType = flashType,
            flashMode = flashMode,
            correctTorchState = false,
        )

    internal suspend fun submitStillCaptures(
        configs: List<CaptureConfig>,
        requestTemplate: RequestTemplate,
        sessionConfigOptions: Config,
        @CaptureMode captureMode: Int,
        @FlashType flashType: Int,
        @FlashMode flashMode: Int,
        correctTorchState: Boolean,
    ): List<Deferred<Void?>> =
        invokeCaptureTasks(
            pipelineTasks = listOf(PRE_CAPTURE, MAIN_CAPTURE, POST_CAPTURE),
            captureMode = captureMode,
            flashMode = flashMode,
            flashType = flashType,
            mainCaptureParams = MainCaptureParams(configs, requestTemplate, sessionConfigOptions),
            correctTorchState = correctTorchState,
        )

    override suspend fun getCameraCapturePipeline(
        captureMode: Int,
        flashMode: Int,
        flashType: Int,
    ): CameraCapturePipeline {
        return object : CameraCapturePipeline {
            override fun invokePreCapture(): ListenableFuture<Void?> {
                return threads.sequentialScope.future {
                    invokeCaptureTasks(
                            pipelineTasks = listOf(PRE_CAPTURE),
                            captureMode = captureMode,
                            flashMode = flashMode,
                            flashType = flashType,
                            mainCaptureParams = null,
                        )
                        .joinAll()
                    null // Since the joinAll earlier returns Unit type mismatching with Void? type
                }
            }

            override fun invokePostCapture(): ListenableFuture<Void?> {
                return threads.sequentialScope.future {
                    invokeCaptureTasks(
                            pipelineTasks = listOf(POST_CAPTURE),
                            captureMode = captureMode,
                            flashMode = flashMode,
                            flashType = flashType,
                            mainCaptureParams = null,
                        )
                        .joinAll()
                    null // Since the joinAll earlier returns Unit type mismatching with Void? type
                }
            }
        }
    }

    /**
     * Invokes a capture pipeline with the sequence of pre-capture -> main capture -> post-capture
     * based on the pipeline tasks in the receiver list.
     *
     * Pre-capture and main capture share a single [CameraGraph.Session] which is closed immediately
     * after submitting the capture request (before awaiting the capture signals) so future camera
     * interactions are not blocked while waiting for capture results. Post-capture tasks acquire a
     * separate [CameraGraph.Session] after the capture signals complete.
     *
     * @param mainCaptureParams Parameters required for the main capture, must not be null if the
     *   receiver list contains [PipelineTask.MAIN_CAPTURE].
     * @param correctTorchState Whether to turn the torch OFF then ON after post-capture to work
     *   around devices where torch is closed after still capture.
     * @param preCapture A function invoked during pre-capture with the active
     *   [CameraGraph.Session].
     * @param postCapture A function invoked during post-capture with the active
     *   [CameraGraph.Session] (or null if the [CameraGraph] is closed), or null if no post-capture
     *   action is needed.
     * @receiver A list of [PipelineTask].
     */
    private suspend inline fun List<PipelineTask>.invoke(
        mainCaptureParams: MainCaptureParams?,
        correctTorchState: Boolean = false,
        crossinline preCapture: suspend (CameraGraph.Session) -> Unit,
        noinline postCapture: (suspend (CameraGraph.Session?) -> Unit)? = null,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#List<PipelineTask>.invoke: tasks = $this" }
        val session =
            try {
                useCaseCameraContext.acquireSession()
            } catch (_: CancellationException) {
                Camera2Logger.info {
                    "CapturePipeline#List<PipelineTask>.invoke: " +
                        "CameraGraph.Session could not be acquired, requests may need re-submission"
                }
                if (!contains(PRE_CAPTURE) && !contains(MAIN_CAPTURE) && contains(POST_CAPTURE)) {
                    if (postCapture != null) {
                        debug {
                            "CapturePipeline#List<PipelineTask>.invoke: " +
                                "starting POST_CAPTURE without session"
                        }
                        postCapture(null)
                        debug {
                            "CapturePipeline#List<PipelineTask>.invoke: POST_CAPTURE completed"
                        }
                    }
                    return listOf(CompletableDeferred(value = null))
                }
                val count = mainCaptureParams?.configs?.size ?: 1
                return List(count) {
                    CompletableDeferred<Void?>().apply {
                        completeExceptionally(
                            ImageCaptureException(
                                ERROR_CAMERA_CLOSED,
                                "Capture request is cancelled because camera is closed",
                                null,
                            )
                        )
                    }
                }
            }

        val (captureSignal, requiresStopRepeating) =
            session.use {
                if (contains(PRE_CAPTURE)) {
                    debug { "CapturePipeline#List<PipelineTask>.invoke: starting PRE_CAPTURE" }
                    preCapture(session)
                    debug { "CapturePipeline#List<PipelineTask>.invoke: PRE_CAPTURE completed" }
                }

                if (!contains(MAIN_CAPTURE)) {
                    if (contains(POST_CAPTURE) && postCapture != null) {
                        debug { "CapturePipeline#List<PipelineTask>.invoke: starting POST_CAPTURE" }
                        postCapture(session.takeIf { !useCaseCameraContext.isGraphClosed })
                        debug {
                            "CapturePipeline#List<PipelineTask>.invoke: POST_CAPTURE completed"
                        }
                    }
                    return listOf(CompletableDeferred(value = null))
                }

                debug { "CapturePipeline#List<PipelineTask>.invoke: starting MAIN_CAPTURE" }
                submitRequestInternal(session, checkNotNull(mainCaptureParams)).also {
                    debug { "CapturePipeline#List<PipelineTask>.invoke: MAIN_CAPTURE completed" }
                }
            }

        val hasPostCaptureWork =
            requiresStopRepeating ||
                (contains(POST_CAPTURE) && postCapture != null) ||
                correctTorchState
        if (hasPostCaptureWork) {
            threads.sequentialScope.launch(start = CoroutineStart.UNDISPATCHED) {
                debug {
                    "CapturePipeline#List<PipelineTask>.invoke:" +
                        " Waiting for POST_CAPTURE signal"
                }
                captureSignal.joinAll()
                debug {
                    "CapturePipeline#List<PipelineTask>.invoke:" +
                        " Waiting for POST_CAPTURE signal done"
                }
                val postCaptureSession =
                    try {
                        useCaseCameraContext.acquireSession()
                    } catch (_: CancellationException) {
                        null
                    }
                try {
                    if (requiresStopRepeating && postCaptureSession != null) {
                        useCaseCameraState.tryStartRepeating(postCaptureSession)
                    }
                    if (contains(POST_CAPTURE) && postCapture != null) {
                        postCapture(postCaptureSession)
                    }
                    if (correctTorchState && postCaptureSession != null) {
                        debug { "Re-enable Torch to correct the Torch state" }
                        torchControl
                            .setTorchAsync(
                                TorchMode.OFF,
                                session =
                                    postCaptureSession.takeIf {
                                        !useCaseCameraContext.isGraphClosed
                                    },
                            )
                            .join()
                        torchControl
                            .setTorchAsync(
                                TorchMode.USED_AS_FLASH,
                                session =
                                    postCaptureSession.takeIf {
                                        !useCaseCameraContext.isGraphClosed
                                    },
                            )
                            .join()
                        debug { "Re-enable Torch to correct the Torch state, done" }
                    }
                } finally {
                    postCaptureSession?.close()
                }
            }
        }
        return captureSignal
    }

    private suspend fun torchAsFlashCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        @FlashMode flashMode: Int,
        pipelineTasks: List<PipelineTask>,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#torchAsFlashCapture" }
        return if (hasFlashUnit && isPhysicalFlashRequired(flashMode)) {
            torchApplyCapture(
                mainCaptureParams,
                captureMode,
                CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS,
                pipelineTasks,
                // TODO: b/339846763 - Further refine AE precap disabling for specific
                //  legacy quirks, instead of disabling for all older UseTorchAsFlash quirks.
                !useTorchAsFlash.shouldDisableAePrecapture() && !videoUsageControl.isInVideoUsage(),
                correctTorchState,
            )
        } else {
            defaultNoFlashCapture(
                mainCaptureParams,
                captureMode,
                pipelineTasks,
                correctTorchState,
            )
        }
    }

    private suspend fun defaultCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        @FlashMode flashMode: Int,
        pipelineTasks: List<PipelineTask>,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        return if (hasFlashUnit) {
            val isFlashRequired = isPhysicalFlashRequired(flashMode)
            val timeout =
                if (isFlashRequired) CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS else CHECK_3A_TIMEOUT_IN_NS

            if (isFlashRequired || captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
                aePreCaptureApplyCapture(
                    mainCaptureParams,
                    timeout,
                    captureMode,
                    pipelineTasks,
                    isFlashRequired = isFlashRequired,
                    correctTorchState = correctTorchState,
                )
            } else {
                defaultNoFlashCapture(
                    mainCaptureParams,
                    captureMode,
                    pipelineTasks,
                    correctTorchState,
                )
            }
        } else {
            defaultNoFlashCapture(
                mainCaptureParams,
                captureMode,
                pipelineTasks,
                correctTorchState,
            )
        }
    }

    private suspend fun defaultNoFlashCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        pipelineTasks: List<PipelineTask>,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#defaultNoFlashCapture" }
        val lock3ARequired = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            correctTorchState = correctTorchState,
            preCapture = { session ->
                if (lock3ARequired) {
                    debug { "CapturePipeline#defaultNoFlashCapture: Locking 3A" }
                    lockAf(session, CHECK_3A_TIMEOUT_IN_NS, isTorchAsFlash = false)
                    debug { "CapturePipeline#defaultNoFlashCapture: Locking 3A done" }
                }
            },
            postCapture =
                if (lock3ARequired) {
                    { session ->
                        if (session != null) {
                            debug { "CapturePipeline#defaultNoFlashCapture: Unlocking 3A" }
                            unlockAf(session, CHECK_3A_TIMEOUT_IN_NS)
                            debug { "CapturePipeline#defaultNoFlashCapture: Unlocking 3A done" }
                        }
                    }
                } else {
                    null
                },
        )
    }

    private suspend fun torchApplyCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        timeLimitNs: Long,
        pipelineTasks: List<PipelineTask>,
        triggerAePreCapture: Boolean,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#torchApplyCapture" }
        val isLlbOverrideRequired = lowLightBoostControl?.isLowLightBoostOn == true
        val torchOnRequired = torchControl.torchStateLiveData.value == TorchState.OFF
        val lock3ARequired = torchOnRequired || captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY

        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            correctTorchState = correctTorchState,
            preCapture = { session ->
                if (isLlbOverrideRequired) {
                    debug { "CapturePipeline#torchApplyCapture: Disabling LLB for flash" }
                    lowLightBoostControl
                        ?.disableForFlashCaptureAsync(
                            session.takeIf { !useCaseCameraContext.isGraphClosed }
                        )
                        ?.join()
                    debug { "CapturePipeline#torchApplyCapture: Disabling LLB for flash done" }
                    if (mainCaptureParams != null) {
                        val overrideAeMode = lowLightBoostControl.getFlashOverrideAeMode()
                        mainCaptureParams.sessionConfigOptions =
                            Camera2ImplConfig.Builder()
                                .insertAllOptions(mainCaptureParams.sessionConfigOptions)
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    overrideAeMode,
                                )
                                .build()
                    }
                }

                if (torchOnRequired) {
                    debug { "CapturePipeline#torchApplyCapture: Setting torch" }
                    torchControl
                        .setTorchAsync(
                            TorchMode.USED_AS_FLASH,
                            session = session.takeIf { !useCaseCameraContext.isGraphClosed },
                        )
                        .join()
                    debug { "CapturePipeline#torchApplyCapture: Setting torch done" }
                }

                if (triggerAePreCapture) {
                    debug { "CapturePipeline#torchApplyCapture: Locking 3A for capture" }
                    val result3A =
                        session
                            .lock3AForCapture(
                                timeLimitNs = timeLimitNs,
                                triggerAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY,
                                waitForAwb = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY,
                            )
                            .await()
                    debug {
                        "CapturePipeline#torchApplyCapture: Locking 3A for capture done" +
                            ", result3A = $result3A"
                    }
                } else {
                    // TODO: b/339846763 - When triggerAePreCapture is false, AE pre-capture may
                    //  cause issues in some devices and thus should not be used here. When capture
                    //  mode is not max quality, we should only wait for 3A convergence without any
                    //  additional locking. In case of max quality, only AF should be locked, not
                    //  AE/AWB too.
                    if (lock3ARequired) {
                        if (captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
                            debug { "CapturePipeline#torchApplyCapture: Locking 3A" }
                            lockAf(session, timeLimitNs, isTorchAsFlash = true)
                            debug { "CapturePipeline#torchApplyCapture: Locking 3A done" }
                        } else {
                            debug { "CapturePipeline#torchApplyCapture: Awaiting 3A convergence" }
                            waitForResult(waitTimeoutNanos = timeLimitNs) {
                                ConvergenceUtils.is3AConverged(
                                    it.metadata.toCameraCaptureResult(),
                                    /* isTorchAsFlash = */ true,
                                )
                            }
                            debug {
                                "CapturePipeline#torchApplyCapture: 3A convergence waiting done"
                            }
                        }
                    }
                }
            },
            postCapture = { session ->
                if (torchOnRequired) {
                    debug { "CapturePipeline#torchApplyCapture: Unsetting torch" }
                    @Suppress("DeferredResultUnused")
                    torchControl.setTorchAsync(TorchMode.OFF, session = session)
                    debug { "CapturePipeline#torchApplyCapture: Unsetting torch done" }
                }
                if (session != null) {
                    if (triggerAePreCapture) {
                        debug { "CapturePipeline#torchApplyCapture: Unlocking 3A for capture" }
                        @Suppress("DeferredResultUnused")
                        session.unlock3APostCapture(
                            cancelAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
                        )
                    } else {
                        if (lock3ARequired && captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
                            debug { "CapturePipeline#torchApplyCapture: Unlocking 3A" }
                            unlockAf(session, CHECK_3A_TIMEOUT_IN_NS)
                            debug { "CapturePipeline#torchApplyCapture: Unlocking 3A done" }
                        }
                    }
                }
                if (isLlbOverrideRequired) {
                    debug { "CapturePipeline#torchApplyCapture: Restoring LLB after flash" }
                    @Suppress("DeferredResultUnused")
                    lowLightBoostControl?.restoreAfterFlashCaptureAsync(session)
                    debug { "CapturePipeline#torchApplyCapture: Restoring LLB after flash done" }
                }
            },
        )
    }

    private suspend fun aePreCaptureApplyCapture(
        mainCaptureParams: MainCaptureParams?,
        timeLimitNs: Long,
        @CaptureMode captureMode: Int,
        pipelineTasks: List<PipelineTask>,
        isFlashRequired: Boolean = true,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#aePreCaptureApplyCapture" }
        val isLlbOverrideRequired =
            isFlashRequired && (lowLightBoostControl?.isLowLightBoostOn == true)

        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            correctTorchState = correctTorchState,
            preCapture = { session ->
                if (isLlbOverrideRequired) {
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Disabling LLB for flash" }
                    lowLightBoostControl
                        ?.disableForFlashCaptureAsync(
                            session.takeIf { !useCaseCameraContext.isGraphClosed }
                        )
                        ?.join()
                    debug {
                        "CapturePipeline#aePreCaptureApplyCapture: Disabling LLB for flash done"
                    }
                    if (mainCaptureParams != null) {
                        val overrideAeMode = lowLightBoostControl.getFlashOverrideAeMode()
                        mainCaptureParams.sessionConfigOptions =
                            Camera2ImplConfig.Builder()
                                .insertAllOptions(mainCaptureParams.sessionConfigOptions)
                                .setCaptureRequestOption(
                                    CaptureRequest.CONTROL_AE_MODE,
                                    overrideAeMode,
                                )
                                .build()
                    }
                }
                debug { "CapturePipeline#aePreCaptureApplyCapture: Locking 3A for capture" }
                session
                    .lock3AForCapture(
                        timeLimitNs = timeLimitNs,
                        triggerAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY,
                        waitForAwb = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY,
                    )
                    .join()
                debug { "CapturePipeline#aePreCaptureApplyCapture: Locking 3A for capture done" }
            },
            postCapture = { session ->
                if (session != null) {
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Unlocking 3A" }
                    @Suppress("DeferredResultUnused")
                    session.unlock3APostCapture(
                        cancelAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
                    )
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Unlocking 3A done" }
                }
                if (isLlbOverrideRequired) {
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Restoring LLB after flash" }
                    @Suppress("DeferredResultUnused")
                    lowLightBoostControl?.restoreAfterFlashCaptureAsync(session)
                    debug {
                        "CapturePipeline#aePreCaptureApplyCapture: Restoring LLB after flash done"
                    }
                }
            },
        )
    }

    private suspend fun screenFlashCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        pipelineTasks: List<PipelineTask>,
        correctTorchState: Boolean = false,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#screenFlashCapture" }

        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            correctTorchState = correctTorchState,
            preCapture = { session -> invokeScreenFlashPreCaptureTasks(captureMode, session) },
            postCapture = { session -> invokeScreenFlashPostCaptureTasks(captureMode, session) },
        )
    }

    /**
     * Invokes the pre-capture tasks required for a screen flash capture.
     *
     * This method may modify the preferred AE mode in [State3AControl] to enable external flash AE
     * mode. [invokeScreenFlashPostCaptureTasks] should be used to restore the previous AE mode in
     * such case.
     *
     * @return The previous preferred AE mode in [State3AControl], null if not modified.
     */
    @VisibleForTesting
    public suspend fun invokeScreenFlashPreCaptureTasks(
        @CaptureMode captureMode: Int,
        session: CameraGraph.Session? = null,
    ) {
        flashControl.startScreenFlashCaptureTasks(
            session.takeIf { !useCaseCameraContext.isGraphClosed }
        )

        val lockBlock: suspend (CameraGraph.Session) -> Unit = { graphSession ->
            // Trigger AE precapture & wait for 3A converge
            debug { "screenFlashPreCapture: Locking 3A for capture" }
            val result3A =
                graphSession
                    .lock3AForCapture(
                        timeLimitNs = CHECK_3A_WITH_SCREEN_FLASH_TIMEOUT_IN_NS,
                        triggerAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY,
                        waitForAwb = true,
                    )
                    .await()
            debug { "screenFlashPreCapture: Locking 3A for capture done, result3A = $result3A" }
        }
        if (session != null) {
            lockBlock(session)
        } else {
            useCaseCameraContext.useGraphSession { lockBlock(it) }
        }
    }

    @VisibleForTesting
    public suspend fun invokeScreenFlashPostCaptureTasks(
        @CaptureMode captureMode: Int,
        session: CameraGraph.Session? = null,
    ) {
        flashControl.stopScreenFlashCaptureTasks(
            session.takeIf { !useCaseCameraContext.isGraphClosed }
        )

        // Unlock 3A
        val unlockBlock: suspend (CameraGraph.Session) -> Unit = { graphSession ->
            debug { "screenFlashPostCapture: Unlocking 3A" }
            @Suppress("DeferredResultUnused")
            graphSession.unlock3APostCapture(
                cancelAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
            )
            debug { "screenFlashPostCapture: Unlocking 3A done" }
        }
        if (session != null) {
            unlockBlock(session)
        } else {
            debug { "screenFlashPostCapture: Acquiring session for unlocking 3A" }
            try {
                useCaseCameraContext.useGraphSession { unlockBlock(it) }
            } catch (_: CancellationException) {
                debug {
                    "screenFlashPostCapture: CameraGraph.Session could not be acquired for unlocking 3A"
                }
            }
        }
    }

    /**
     * Locks AF by triggering a new AF scan and awaits 3A convergence.
     *
     * This function uses [CameraGraph.Session.lock3A] with `aeLockBehavior` and `awbLockBehavior`
     * set to null to avoid redundant AE/AWB locking. For 3A convergence condition, CameraX custom
     * condition is used (i.e. [ConvergenceUtils.is3AConverged]).
     */
    private suspend fun lockAf(
        session: CameraGraph.Session,
        convergedTimeLimitNs: Long,
        isTorchAsFlash: Boolean,
    ): Result3A =
        session
            .lock3A(
                aeLockBehavior = null,
                afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                awbLockBehavior = null,
                convergedCondition = getConvergeCondition(isTorchAsFlash),
                convergedTimeLimitNs = convergedTimeLimitNs,
                lockedTimeLimitNs = CHECK_3A_TIMEOUT_IN_NS,
            )
            .await()

    private fun getConvergeCondition(
        isTorchAsFlash: Boolean
    ): (frameMetadata: FrameMetadata) -> Boolean = convergeCondition@{ frameMetadata ->
        ConvergenceUtils.is3AConverged(frameMetadata.toCameraCaptureResult(), isTorchAsFlash)
    }

    /** Unlocks any active AF lock by triggering an AF cancel. */
    private suspend fun unlockAf(session: CameraGraph.Session, timeLimitNs: Long): Result3A =
        session.unlock3A(af = true, timeLimitNs = timeLimitNs).await()

    private fun submitRequestInternal(
        session: CameraGraph.Session,
        params: MainCaptureParams,
    ): Pair<List<Deferred<Void?>>, Boolean> {
        debug {
            "CapturePipeline#submitRequestInternal; Submitting ${params.configs} with CameraPipe"
        }
        val deferredList = mutableListOf<CompletableDeferred<Void?>>()
        val requests =
            params.configs.mapNotNull { captureConfig ->
                val completeSignal = CompletableDeferred<Void?>()
                deferredList.add(completeSignal)
                try {
                    configAdapter.mapToRequest(
                        captureConfig,
                        params.requestTemplate,
                        params.sessionConfigOptions,
                        listOf(
                            object : Request.Listener {
                                override fun onAborted(request: Request) {
                                    completeSignal.completeExceptionally(
                                        ImageCaptureException(
                                            ERROR_CAMERA_CLOSED,
                                            "Capture request is cancelled because camera is closed",
                                            null,
                                        )
                                    )
                                }

                                override fun onTotalCaptureResult(
                                    requestMetadata: RequestMetadata,
                                    frameNumber: CameraFrameNumber,
                                    totalCaptureResult: FrameInfo,
                                ) {
                                    completeSignal.complete(null)
                                }

                                override fun onFailed(
                                    requestMetadata: RequestMetadata,
                                    frameNumber: CameraFrameNumber,
                                    requestFailure: RequestFailure,
                                ) {
                                    completeSignal.completeExceptionally(
                                        ImageCaptureException(
                                            ERROR_CAPTURE_FAILED,
                                            "Capture request failed with reason " +
                                                requestFailure.reason,
                                            null,
                                        )
                                    )
                                }
                            }
                        ),
                    )
                } catch (e: IllegalStateException) {
                    Camera2Logger.info(e) {
                        "CapturePipeline#submitRequestInternal: configAdapter.mapToRequest failed!"
                    }
                    completeSignal.completeExceptionally(
                        ImageCaptureException(
                            ERROR_CAPTURE_FAILED,
                            "Capture request failed with reason " + e.message,
                            e,
                        )
                    )
                    null
                }
            }

        if (requests.isEmpty()) {
            // requests can be empty due to configAdapter.mapToRequest throwing exception, all the
            // deferred instances in the list should already be completed exceptionally.
            return Pair(deferredList, false)
        }

        val requiresStopRepeating = requests.shouldStopRepeatingBeforeCapture()
        if (requiresStopRepeating) {
            session.stopRepeating()
        }

        debug { "CapturePipeline#submitRequestInternal: Submitting $requests" }
        session.submit(requests)

        return Pair(deferredList, requiresStopRepeating)
    }

    private suspend fun isPhysicalFlashRequired(@FlashMode flashMode: Int): Boolean =
        when (flashMode) {
            FLASH_MODE_ON -> true
            FLASH_MODE_AUTO -> {
                getFrameMetadata()?.get(CaptureResult.CONTROL_AE_STATE) ==
                    CONTROL_AE_STATE_FLASH_REQUIRED
            }
            FLASH_MODE_OFF -> false
            FLASH_MODE_SCREEN -> false
            else -> throw AssertionError(flashMode)
        }

    private suspend fun waitForResult(
        waitTimeoutNanos: Long,
        checker: (totalCaptureResult: FrameInfo) -> Boolean = { _ -> true },
    ): FrameInfo? {
        val resultListener =
            ResultListener(waitTimeoutNanos, checker).also { listener ->
                requestListener.addListener(listener, threads.sequentialExecutor)
                threads.sequentialScope.launch {
                    listener.result.join()
                    requestListener.removeListener(listener)
                }
            }

        return withTimeoutOrNull(TimeUnit.NANOSECONDS.toMillis(waitTimeoutNanos)) {
                // ResultListener timeout is checked only when there is a captured frame, so it
                // might get stuck indefinitely without withTimeOrNull
                resultListener.result.await()
            }
            .also { frameInfo ->
                if (frameInfo == null) {
                    requestListener.removeListener(resultListener)
                }
            }
    }

    private suspend fun isTorchAsFlash(@FlashType flashType: Int): Boolean {
        return template == CameraDevice.TEMPLATE_RECORD ||
            flashType == FLASH_TYPE_USE_TORCH_AS_FLASH ||
            useTorchAsFlash.shouldUseTorchAsFlash({ getFrameMetadata() })
    }
}

/**
 * A listener receives the result from the repeating request, and sends it to the [checker] to
 * determine if the [completeSignal] can be completed.
 *
 * @param timeLimitNs timeout threshold in Nanos, set 0 for no timeout case.
 * @param checker the checker to define the condition to complete the [completeSignal]. Return true
 *   will complete the [completeSignal], otherwise it will continue to receive the results until the
 *   timeLimitNs is reached.
 * @constructor
 */
public class ResultListener(
    private val timeLimitNs: Long,
    private val checker: (totalCaptureResult: FrameInfo) -> Boolean,
) : Request.Listener {

    private val completeSignal = CompletableDeferred<FrameInfo?>()
    public val result: Deferred<FrameInfo?>
        get() = completeSignal

    @Volatile private var timestampOfFirstUpdateNs: Long? = null

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: CameraFrameNumber,
        totalCaptureResult: FrameInfo,
    ) {
        // Save some compute if the task is already complete or has been canceled.
        if (completeSignal.isCompleted || completeSignal.isCancelled) {
            return
        }

        val currentTimestampNs: Long? = totalCaptureResult.metadata[CaptureResult.SENSOR_TIMESTAMP]

        if (currentTimestampNs != null && timestampOfFirstUpdateNs == null) {
            timestampOfFirstUpdateNs = currentTimestampNs
        }

        val timestampOfFirstUpdateNs = timestampOfFirstUpdateNs
        if (
            timeLimitNs != 0L &&
                timestampOfFirstUpdateNs != null &&
                currentTimestampNs != null &&
                currentTimestampNs - timestampOfFirstUpdateNs > timeLimitNs
        ) {
            completeSignal.complete(null)
            debug {
                "Wait for capture result timeout, current: $currentTimestampNs " +
                    "first: $timestampOfFirstUpdateNs"
            }
            return
        }
        if (!checker(totalCaptureResult)) {
            return
        }

        completeSignal.complete(totalCaptureResult)
    }
}
