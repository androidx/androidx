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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.impl

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics.CONTROL_AE_STATE_FLASH_REQUIRED
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureResult
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.Log.info
import androidx.camera.camera2.pipe.integration.compat.workaround.UseTorchAsFlash
import androidx.camera.camera2.pipe.integration.compat.workaround.isFlashAvailable
import androidx.camera.camera2.pipe.integration.compat.workaround.shouldStopRepeatingBeforeCapture
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraScope
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH
import androidx.camera.core.ImageCapture.FlashMode
import androidx.camera.core.ImageCapture.FlashType
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

private val CHECK_3A_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(1)
private val CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS = TimeUnit.SECONDS.toNanos(5)

interface CapturePipeline {

    var template: Int

    suspend fun submitStillCaptures(
        requests: List<Request>,
        captureMode: Int,
        flashType: Int,
        flashMode: Int,
    ): List<Deferred<Void?>>
}

/**
 * Implementations for the single capture.
 */
@UseCaseCameraScope
class CapturePipelineImpl @Inject constructor(
    private val torchControl: TorchControl,
    private val threads: UseCaseThreads,
    private val requestListener: ComboRequestListener,
    private val useTorchAsFlash: UseTorchAsFlash,
    cameraProperties: CameraProperties,
    private val useCaseCameraState: UseCaseCameraState,
    useCaseGraphConfig: UseCaseGraphConfig,
) : CapturePipeline {
    private val graph = useCaseGraphConfig.graph

    // If there is no flash unit, skip the flash related task instead of failing the pipeline.
    private val hasFlashUnit = cameraProperties.isFlashAvailable()

    override var template = CameraDevice.TEMPLATE_PREVIEW

    override suspend fun submitStillCaptures(
        requests: List<Request>,
        captureMode: Int,
        flashType: Int,
        flashMode: Int,
    ): List<Deferred<Void?>> = if (isTorchAsFlash(flashType)) {
        torchAsFlashCapture(requests, captureMode, flashMode)
    } else {
        defaultCapture(requests, captureMode, flashMode)
    }

    private suspend fun torchAsFlashCapture(
        requests: List<Request>,
        captureMode: Int,
        flashMode: Int,
    ): List<Deferred<Void?>> =
        if (hasFlashUnit && isFlashRequired(flashMode)) {
            torchApplyCapture(requests, captureMode, CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS)
        } else {
            defaultNoFlashCapture(requests, captureMode)
        }

    private suspend fun defaultCapture(
        requests: List<Request>,
        captureMode: Int,
        flashMode: Int,
    ): List<Deferred<Void?>> {
        return if (hasFlashUnit) {
            val isFlashRequired = isFlashRequired(flashMode)
            val timeout =
                if (isFlashRequired) CHECK_3A_WITH_FLASH_TIMEOUT_IN_NS else CHECK_3A_TIMEOUT_IN_NS

            if (isFlashRequired || captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY) {
                aePreCaptureApplyCapture(requests, timeout)
            } else {
                defaultNoFlashCapture(requests, captureMode)
            }
        } else {
            defaultNoFlashCapture(requests, captureMode)
        }
    }

    private suspend fun defaultNoFlashCapture(
        requests: List<Request>,
        captureMode: Int
    ): List<Deferred<Void?>> {
        val lock3ARequired = captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
        if (lock3ARequired) {
            lock3A(CHECK_3A_TIMEOUT_IN_NS)
        }
        return submitRequestInternal(requests).also { captureSignal ->
            if (lock3ARequired) {
                threads.sequentialScope.launch {
                    captureSignal.joinAll()
                    unlock3A()
                }
            }
        }
    }

    private suspend fun torchApplyCapture(
        requests: List<Request>,
        captureMode: Int,
        timeLimitNs: Long,
    ): List<Deferred<Void?>> {
        val torchOnRequired = torchControl.torchStateLiveData.value == TorchState.OFF
        if (torchOnRequired) {
            torchControl.setTorchAsync(true).join()
        }

        val lock3ARequired = torchOnRequired || captureMode == CAPTURE_MODE_MAXIMIZE_QUALITY
        if (lock3ARequired) {
            lock3A(timeLimitNs)
        }

        return submitRequestInternal(requests).also { captureSignal ->
            if (torchOnRequired) {
                threads.sequentialScope.launch {
                    captureSignal.joinAll()
                    @Suppress("DeferredResultUnused")
                    torchControl.setTorchAsync(false)
                }
            }
            if (lock3ARequired) {
                threads.sequentialScope.launch {
                    captureSignal.joinAll()
                    unlock3A()
                }
            }
        }
    }

    private suspend fun aePreCaptureApplyCapture(
        requests: List<Request>,
        timeLimitNs: Long,
    ): List<Deferred<Void?>> {
        graph.acquireSession().use {
            it.lock3AForCapture(timeLimitNs = timeLimitNs).join()
        }

        return submitRequestInternal(requests).also { captureSignal ->
            threads.sequentialScope.launch {
                captureSignal.joinAll()
                graph.acquireSession().use {
                    @Suppress("DeferredResultUnused")
                    it.unlock3APostCapture()
                }
            }
        }
    }

    private suspend fun lock3A(timeLimitNs: Long): Result3A = graph.acquireSession().use {
        it.lock3A(
            aeLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
            afLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
            awbLockBehavior = Lock3ABehavior.AFTER_CURRENT_SCAN,
            timeLimitNs = timeLimitNs,
        )
    }.await()

    private suspend fun unlock3A(): Result3A = graph.acquireSession().use {
        it.unlock3A(ae = true, af = true, awb = true)
    }.await()

    private fun submitRequestInternal(requests: List<Request>): List<Deferred<Void?>> {
        val deferredList = mutableListOf<CompletableDeferred<Void?>>()
        val requestsToSubmit = requests.map { request ->
            request.copy(listeners = request.listeners.toMutableList().also { newRequestListeners ->
                deferredList.add(CompletableDeferred<Void?>().also { completeSignal ->
                    newRequestListeners.add(object : Request.Listener {
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

                        @Deprecated(
                            message = "Migrating to using RequestFailureWrapper instead of " +
                                "CaptureFailure",
                            level = DeprecationLevel.WARNING,
                            replaceWith = ReplaceWith("onFailed")
                        )
                        @SuppressLint("ClassVerificationFailure")
                        override fun onFailed(
                            requestMetadata: RequestMetadata,
                            frameNumber: FrameNumber,
                            captureFailure: CaptureFailure
                        ) {
                            completeSignal.completeExceptionally(
                                ImageCaptureException(
                                    ERROR_CAPTURE_FAILED,
                                    "Capture request failed with reason " + captureFailure.reason,
                                    null
                                )
                            )
                        }
                    })
                })
            })
        }

        threads.sequentialScope.launch {
            // graph.acquireSession may fail if camera has entered closing stage
            var cameraGraphSession: CameraGraph.Session? = null
            try {
                cameraGraphSession = graph.acquireSession()
            } catch (_: CancellationException) {
                info {
                    "CapturePipeline#submitRequestInternal:" +
                        " CameraGraph.Session could not be acquired, requests may need " +
                        "re-submission"
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
                val requiresStopRepeating = requestsToSubmit.shouldStopRepeatingBeforeCapture()
                if (requiresStopRepeating) {
                    it.stopRepeating()
                }

                it.submit(requestsToSubmit)

                if (requiresStopRepeating) {
                    deferredList.joinAll()
                    useCaseCameraState.tryStartRepeating()
                }
            }
        }

        return deferredList
    }

    private suspend fun isFlashRequired(@FlashMode flashMode: Int): Boolean =
        when (flashMode) {
            FLASH_MODE_ON -> true
            FLASH_MODE_AUTO -> {
                waitForResult()?.metadata?.get(
                    CaptureResult.CONTROL_AE_STATE
                ) == CONTROL_AE_STATE_FLASH_REQUIRED
            }

            FLASH_MODE_OFF -> false
            else -> throw AssertionError(flashMode)
        }

    private suspend fun waitForResult(
        waitTimeout: Long = 0,
        checker: (totalCaptureResult: FrameInfo) -> Boolean = { _ -> true }
    ): FrameInfo? = ResultListener(waitTimeout, checker).also { listener ->
        requestListener.addListener(listener, threads.sequentialExecutor)
        threads.sequentialScope.launch {
            listener.result.join()
            requestListener.removeListener(listener)
        }
    }.result.await()

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
 * @constructor
 * @param timeLimitNs timeout threshold in Nanos, set 0 for no timeout case.
 * @param checker the checker to define the condition to complete the [completeSignal]. Return true
 * will complete the [completeSignal], otherwise it will continue to receive the results until the
 * timeLimitNs is reached.
 */
class ResultListener(
    private val timeLimitNs: Long,
    private val checker: (totalCaptureResult: FrameInfo) -> Boolean,
) : Request.Listener {

    private val completeSignal = CompletableDeferred<FrameInfo?>()
    val result: Deferred<FrameInfo?>
        get() = completeSignal

    @Volatile
    private var timestampOfFirstUpdateNs: Long? = null

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ) {
        // Save some compute if the task is already complete or has been canceled.
        if (completeSignal.isCompleted || completeSignal.isCancelled) {
            return
        }

        val currentTimestampNs: Long? =
            totalCaptureResult.metadata[CaptureResult.SENSOR_TIMESTAMP]

        if (currentTimestampNs != null && timestampOfFirstUpdateNs == null) {
            timestampOfFirstUpdateNs = currentTimestampNs
        }

        val timestampOfFirstUpdateNs = timestampOfFirstUpdateNs
        if (timeLimitNs != 0L &&
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
