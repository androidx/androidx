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

package androidx.camera.camera2.pipe.integration.impl

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_STATE_FLASH_REQUIRED
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureResult
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.future
import androidx.camera.camera2.pipe.integration.compat.workaround.Lock3ABehaviorWhenCaptureImage
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlash
import androidx.camera.camera2.pipe.integration.compat.workaround.isFlashAvailable
import androidx.camera.camera2.pipe.integration.compat.workaround.shouldStopRepeatingBeforeCapture
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl.PipelineTask.MAIN_CAPTURE
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl.PipelineTask.POST_CAPTURE
import androidx.camera.camera2.pipe.integration.impl.CapturePipelineImpl.PipelineTask.PRE_CAPTURE
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
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CameraCaptureResult.EmptyCameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionProcessor.CaptureCallback
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

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
        @FlashType flashType: Int
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
    private val threads: UseCaseThreads,
    private val requestListener: ComboRequestListener,
    private val useTorchAsFlash: UseTorchAsFlash,
    private val lock3ABehaviorWhenCaptureImage: Lock3ABehaviorWhenCaptureImage,
    cameraProperties: CameraProperties,
    private val useCaseCameraState: UseCaseCameraState,
    useCaseGraphConfig: UseCaseGraphConfig,
    private val sessionProcessorManager: SessionProcessorManager?,
) : CapturePipeline {
    private val graph = useCaseGraphConfig.graph

    // If there is no flash unit, skip the flash related task instead of failing the pipeline.
    private val hasFlashUnit = cameraProperties.isFlashAvailable()

    override var template: Int = CameraDevice.TEMPLATE_PREVIEW

    private enum class PipelineTask {
        PRE_CAPTURE,
        MAIN_CAPTURE,
        POST_CAPTURE,
    }

    private data class MainCaptureParams(
        val configs: List<CaptureConfig>,
        val requestTemplate: RequestTemplate,
        val sessionConfigOptions: Config,
    )

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
    ): List<Deferred<Void?>> {
        if (pipelineTasks.contains(MAIN_CAPTURE)) {
            checkNotNull(mainCaptureParams) { "Must not be null for PipelineType.MAIN_CAPTURE" }
        }

        return if (flashMode == FLASH_MODE_SCREEN) {
            screenFlashCapture(
                mainCaptureParams,
                captureMode,
                pipelineTasks,
            )
        } else if (isTorchAsFlash(flashType)) {
            torchAsFlashCapture(
                mainCaptureParams,
                captureMode,
                flashMode,
                pipelineTasks,
            )
        } else {
            defaultCapture(
                mainCaptureParams,
                captureMode,
                flashMode,
                pipelineTasks,
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
        invokeCaptureTasks(
            pipelineTasks = listOf(PRE_CAPTURE, MAIN_CAPTURE, POST_CAPTURE),
            captureMode = captureMode,
            flashMode = flashMode,
            flashType = flashType,
            mainCaptureParams = MainCaptureParams(configs, requestTemplate, sessionConfigOptions),
        )

    override suspend fun getCameraCapturePipeline(
        captureMode: Int,
        flashMode: Int,
        flashType: Int
    ): CameraCapturePipeline {
        return object : CameraCapturePipeline {
            override fun invokePreCapture(): ListenableFuture<Void?> {
                return threads.scope.future {
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
                return threads.scope.future {
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
     * @param mainCaptureParams Parameters required for the main capture, must not be null if the
     *   receiver list contains [PipelineTask.MAIN_CAPTURE].
     * @param preCapture A function invoked during pre-capture.
     * @param postCapture A function invoked during post-capture.
     * @receiver A list of [PipelineTask].
     */
    private suspend inline fun List<PipelineTask>.invoke(
        mainCaptureParams: MainCaptureParams?,
        crossinline preCapture: suspend () -> Unit,
        crossinline postCapture: suspend () -> Unit,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#List<PipelineTask>.invoke: tasks = $this" }
        if (contains(PRE_CAPTURE)) {
            preCapture()
        }
        return if (contains(MAIN_CAPTURE)) {
                submitRequestInternal(
                    checkNotNull(mainCaptureParams),
                )
            } else {
                listOf(CompletableDeferred(null))
            }
            .also { captureSignal ->
                if (contains(POST_CAPTURE)) {
                    threads.sequentialScope.launch {
                        debug {
                            "CapturePipeline#List<PipelineTask>.invoke: Waiting for capture signal"
                        }
                        captureSignal.joinAll()
                        debug {
                            "CapturePipeline#List<PipelineTask>.invoke:" +
                                " Waiting for capture signal done"
                        }
                        postCapture()
                    }
                }
            }
    }

    private suspend fun torchAsFlashCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        @FlashMode flashMode: Int,
        pipelineTasks: List<PipelineTask>,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#torchAsFlashCapture" }
        return if (hasFlashUnit && isPhysicalFlashRequired(flashMode)) {
            torchApplyCapture(
                mainCaptureParams,
                captureMode,
                CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS,
                pipelineTasks,
            )
        } else {
            defaultNoFlashCapture(mainCaptureParams, captureMode, pipelineTasks)
        }
    }

    private suspend fun defaultCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        @FlashMode flashMode: Int,
        pipelineTasks: List<PipelineTask>,
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
                )
            } else {
                defaultNoFlashCapture(
                    mainCaptureParams,
                    captureMode,
                    pipelineTasks,
                )
            }
        } else {
            defaultNoFlashCapture(
                mainCaptureParams,
                captureMode,
                pipelineTasks,
            )
        }
    }

    private suspend fun defaultNoFlashCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        pipelineTasks: List<PipelineTask>,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#defaultNoFlashCapture" }
        val lock3ARequired = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            preCapture = {
                if (lock3ARequired) {
                    debug { "CapturePipeline#defaultNoFlashCapture: Locking 3A" }
                    lock3A(CHECK_3A_TIMEOUT_IN_NS)
                    debug { "CapturePipeline#defaultNoFlashCapture: Locking 3A done" }
                }
            },
            postCapture = {
                if (lock3ARequired) {
                    debug { "CapturePipeline#defaultNoFlashCapture: Unlocking 3A" }
                    unlock3A(CHECK_3A_TIMEOUT_IN_NS)
                    debug { "CapturePipeline#defaultNoFlashCapture: Unlocking 3A done" }
                }
            }
        )
    }

    private suspend fun torchApplyCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        timeLimitNs: Long,
        pipelineTasks: List<PipelineTask>,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#torchApplyCapture" }
        val torchOnRequired = torchControl.torchStateLiveData.value == TorchState.OFF
        val lock3ARequired = torchOnRequired || captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY

        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            preCapture = {
                if (torchOnRequired) {
                    debug { "CapturePipeline#torchApplyCapture: Setting torch" }
                    torchControl.setTorchAsync(true).join()
                    debug { "CapturePipeline#torchApplyCapture: Setting torch done" }
                }

                if (lock3ARequired) {
                    debug { "CapturePipeline#torchApplyCapture: Locking 3A" }
                    lock3A(timeLimitNs)
                    debug { "CapturePipeline#torchApplyCapture: Locking 3A done" }
                }
            },
            postCapture = {
                if (torchOnRequired) {
                    debug { "CapturePipeline#torchApplyCapture: Unsetting torch" }
                    @Suppress("DeferredResultUnused") torchControl.setTorchAsync(false)
                    debug { "CapturePipeline#torchApplyCapture: Unsetting torch done" }
                }
                if (lock3ARequired) {
                    debug { "CapturePipeline#torchApplyCapture: Unlocking 3A" }
                    unlock3A(CHECK_3A_TIMEOUT_IN_NS)
                    debug { "CapturePipeline#torchApplyCapture: Unlocking 3A done" }
                }
            }
        )
    }

    private suspend fun aePreCaptureApplyCapture(
        mainCaptureParams: MainCaptureParams?,
        timeLimitNs: Long,
        @CaptureMode captureMode: Int,
        pipelineTasks: List<PipelineTask>,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#aePreCaptureApplyCapture" }

        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            preCapture = {
                debug {
                    "CapturePipeline#aePreCaptureApplyCapture: Acquiring session for locking 3A"
                }
                graph.acquireSession().use {
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Locking 3A for capture" }
                    it.lock3AForCapture(
                            timeLimitNs = timeLimitNs,
                            triggerAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
                        )
                        .join()
                    debug {
                        "CapturePipeline#aePreCaptureApplyCapture: Locking 3A for capture done"
                    }
                }
            },
            postCapture = {
                debug {
                    "CapturePipeline#aePreCaptureApplyCapture: Acquiring session for unlocking 3A"
                }
                graph.acquireSession().use {
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Unlocking 3A" }
                    @Suppress("DeferredResultUnused")
                    it.unlock3APostCapture(cancelAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY)
                    debug { "CapturePipeline#aePreCaptureApplyCapture: Unlocking 3A done" }
                }
            }
        )
    }

    private suspend fun screenFlashCapture(
        mainCaptureParams: MainCaptureParams?,
        @CaptureMode captureMode: Int,
        pipelineTasks: List<PipelineTask>,
    ): List<Deferred<Void?>> {
        debug { "CapturePipeline#screenFlashCapture" }

        return pipelineTasks.invoke(
            mainCaptureParams = mainCaptureParams,
            preCapture = { invokeScreenFlashPreCaptureTasks(captureMode) },
            postCapture = { invokeScreenFlashPostCaptureTasks(captureMode) }
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
    public suspend fun invokeScreenFlashPreCaptureTasks(@CaptureMode captureMode: Int) {
        flashControl.startScreenFlashCaptureTasks()

        graph.acquireSession().use { session ->
            // Trigger AE precapture & wait for 3A converge
            debug { "screenFlashPreCapture: Locking 3A for capture" }
            val result3A =
                session
                    .lock3AForCapture(
                        timeLimitNs = CHECK_3A_WITH_SCREEN_FLASH_TIMEOUT_IN_NS,
                        triggerAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY,
                        waitForAwb = true,
                    )
                    .await()
            debug { "screenFlashPreCapture: Locking 3A for capture done, result3A = $result3A" }
        }
    }

    @VisibleForTesting
    public suspend fun invokeScreenFlashPostCaptureTasks(@CaptureMode captureMode: Int) {
        flashControl.stopScreenFlashCaptureTasks()

        // Unlock 3A
        debug { "screenFlashPostCapture: Acquiring session for unlocking 3A" }
        graph.acquireSession().use { session ->
            debug { "screenFlashPostCapture: Unlocking 3A" }
            @Suppress("DeferredResultUnused")
            session.unlock3APostCapture(cancelAf = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY)
            debug { "screenFlashPostCapture: Unlocking 3A done" }
        }
    }

    private suspend fun lock3A(convergedTimeLimitNs: Long): Result3A =
        graph
            .acquireSession()
            .use {
                val (aeLockBehavior, afLockBehavior, awbLockBehavior) =
                    lock3ABehaviorWhenCaptureImage.getLock3ABehaviors(
                        defaultAeBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                        defaultAfBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                        defaultAwbBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
                    )
                it.lock3A(
                    aeLockBehavior = aeLockBehavior,
                    afLockBehavior = afLockBehavior,
                    awbLockBehavior = awbLockBehavior,
                    convergedTimeLimitNs = convergedTimeLimitNs,
                    lockedTimeLimitNs = CHECK_3A_TIMEOUT_IN_NS
                )
            }
            .await()

    private suspend fun unlock3A(timeLimitNs: Long): Result3A =
        graph
            .acquireSession()
            .use {
                it.unlock3A(
                    ae = true,
                    af = true,
                    awb = true,
                    timeLimitNs = timeLimitNs,
                )
            }
            .await()

    private fun submitRequestInternal(
        params: MainCaptureParams,
    ): List<Deferred<Void?>> {
        if (sessionProcessorManager != null) {
            return submitRequestInternalWithSessionProcessor(params.configs)
        }
        debug {
            "CapturePipeline#submitRequestInternal; Submitting ${params.configs} with CameraPipe"
        }
        val deferredList = mutableListOf<CompletableDeferred<Void?>>()
        val requests =
            params.configs.mapNotNull {
                val completeSignal = CompletableDeferred<Void?>().also { deferredList.add(it) }
                try {
                    configAdapter.mapToRequest(
                        it,
                        params.requestTemplate,
                        params.sessionConfigOptions,
                        listOf(
                            object : Request.Listener {
                                override fun onAborted(request: Request) {
                                    completeSignal.completeExceptionally(
                                        ImageCaptureException(
                                            ERROR_CAMERA_CLOSED,
                                            "Capture request is cancelled because camera is closed",
                                            null
                                        )
                                    )
                                }

                                override fun onTotalCaptureResult(
                                    requestMetadata: RequestMetadata,
                                    frameNumber: FrameNumber,
                                    totalCaptureResult: FrameInfo,
                                ) {
                                    completeSignal.complete(null)
                                }

                                @SuppressLint("ClassVerificationFailure")
                                override fun onFailed(
                                    requestMetadata: RequestMetadata,
                                    frameNumber: FrameNumber,
                                    requestFailure: RequestFailure
                                ) {
                                    completeSignal.completeExceptionally(
                                        ImageCaptureException(
                                            ERROR_CAPTURE_FAILED,
                                            "Capture request failed with reason " +
                                                requestFailure.reason,
                                            null
                                        )
                                    )
                                }
                            }
                        )
                    )
                } catch (e: IllegalStateException) {
                    info(e) {
                        "CapturePipeline#submitRequestInternal: configAdapter.mapToRequest failed!"
                    }
                    completeSignal.completeExceptionally(
                        ImageCaptureException(
                            ERROR_CAPTURE_FAILED,
                            "Capture request failed with reason " + e.message,
                            e
                        )
                    )
                    null
                }
            }

        if (requests.isEmpty()) {
            // requests can be empty due to configAdapter.mapToRequest throwing exception, all the
            // deferred instances in the list should already be completed exceptionally.
            return deferredList
        }

        threads.sequentialScope.launch {
            debug {
                "CapturePipeline#submitRequestInternal: Acquiring session for submitting requests"
            }
            // graph.acquireSession may fail if camera has entered closing stage
            var cameraGraphSession: CameraGraph.Session? = null
            try {
                cameraGraphSession = graph.acquireSession()
            } catch (_: CancellationException) {
                info {
                    "CapturePipeline#submitRequestInternal: " +
                        "CameraGraph.Session could not be acquired, requests may need re-submission"
                }

                // completing the requests exceptionally so that they are retried with next camera
                deferredList.forEach {
                    it.completeExceptionally(
                        ImageCaptureException(
                            ERROR_CAMERA_CLOSED,
                            "Capture request is cancelled because camera is closed",
                            null
                        )
                    )
                }
            }

            cameraGraphSession?.use {
                val requiresStopRepeating = requests.shouldStopRepeatingBeforeCapture()
                if (requiresStopRepeating) {
                    it.stopRepeating()
                }

                debug { "CapturePipeline#submitRequestInternal: Submitting $requests" }
                it.submit(requests)

                if (requiresStopRepeating) {
                    deferredList.joinAll()
                    useCaseCameraState.tryStartRepeating()
                }
            }
        }

        return deferredList
    }

    private fun submitRequestInternalWithSessionProcessor(
        configs: List<CaptureConfig>
    ): List<Deferred<Void?>> {
        debug {
            "CapturePipeline#submitRequestInternal: Submitting $configs using SessionProcessor"
        }
        val deferredList = mutableListOf<CompletableDeferred<Void?>>()
        val callbacks =
            configs.map {
                val completeSignal = CompletableDeferred<Void?>().also { deferredList.add(it) }
                object : CaptureCallback {
                    private var cameraCaptureResult: CameraCaptureResult? = null

                    override fun onCaptureStarted(captureSequenceId: Int, timestamp: Long) {
                        for (captureCallback in it.cameraCaptureCallbacks) {
                            captureCallback.onCaptureStarted(it.id)
                        }
                    }

                    override fun onCaptureFailed(captureSequenceId: Int) {
                        completeSignal.completeExceptionally(
                            ImageCaptureException(
                                ERROR_CAPTURE_FAILED,
                                "Capture request failed",
                                null
                            )
                        )
                        for (captureCallback in it.cameraCaptureCallbacks) {
                            captureCallback.onCaptureFailed(
                                it.id,
                                CameraCaptureFailure(CameraCaptureFailure.Reason.ERROR)
                            )
                        }
                    }

                    override fun onCaptureCompleted(
                        timestamp: Long,
                        captureSequenceId: Int,
                        captureResult: CameraCaptureResult
                    ) {
                        cameraCaptureResult = captureResult
                    }

                    override fun onCaptureSequenceCompleted(captureSequenceId: Int) {
                        completeSignal.complete(null)
                        val captureResult = cameraCaptureResult ?: EmptyCameraCaptureResult()
                        for (captureCallback in it.cameraCaptureCallbacks) {
                            captureCallback.onCaptureCompleted(it.id, captureResult)
                        }
                    }

                    override fun onCaptureProcessProgressed(progress: Int) {
                        for (captureCallback in it.cameraCaptureCallbacks) {
                            captureCallback.onCaptureProcessProgressed(it.id, progress)
                        }
                    }

                    override fun onCaptureSequenceAborted(captureSequenceId: Int) {
                        completeSignal.completeExceptionally(
                            ImageCaptureException(
                                ERROR_CAMERA_CLOSED,
                                "Capture request is cancelled because camera is closed",
                                null
                            )
                        )
                    }
                }
            }
        sessionProcessorManager!!.submitCaptureConfigs(configs, callbacks)
        return deferredList
    }

    private suspend fun isPhysicalFlashRequired(@FlashMode flashMode: Int): Boolean =
        when (flashMode) {
            FLASH_MODE_ON -> true
            FLASH_MODE_AUTO -> {
                waitForResult()?.metadata?.get(CaptureResult.CONTROL_AE_STATE) ==
                    CONTROL_AE_STATE_FLASH_REQUIRED
            }
            FLASH_MODE_OFF -> false
            FLASH_MODE_SCREEN -> false
            else -> throw AssertionError(flashMode)
        }

    private suspend fun waitForResult(
        waitTimeout: Long = 0,
        checker: (totalCaptureResult: FrameInfo) -> Boolean = { _ -> true }
    ): FrameInfo? =
        ResultListener(waitTimeout, checker)
            .also { listener ->
                requestListener.addListener(listener, threads.sequentialExecutor)
                threads.sequentialScope.launch {
                    listener.result.join()
                    requestListener.removeListener(listener)
                }
            }
            .result
            .await()

    private fun isTorchAsFlash(@FlashType flashType: Int): Boolean {
        return template == CameraDevice.TEMPLATE_RECORD ||
            flashType == FLASH_TYPE_USE_TORCH_AS_FLASH ||
            useTorchAsFlash.shouldUseTorchAsFlash()
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
        frameNumber: FrameNumber,
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
