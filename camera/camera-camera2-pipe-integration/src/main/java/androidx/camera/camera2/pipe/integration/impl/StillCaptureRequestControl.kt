/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.integration.adapter.asListenableFuture
import androidx.camera.camera2.pipe.integration.adapter.propagateCompletion
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@CameraScope
public class StillCaptureRequestControl
@Inject
constructor(
    private val flashControl: FlashControl,
    private val threads: UseCaseThreads,
) : UseCaseCameraControl {
    private val mutex = Mutex()

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            _requestControl?.let { submitPendingRequests() }
        }

    public data class CaptureRequest(
        val captureConfigs: List<CaptureConfig>,
        @ImageCapture.CaptureMode val captureMode: Int,
        @ImageCapture.FlashType val flashType: Int,
        val result: CompletableDeferred<List<Void?>>,
    )

    /**
     * These requests failed to be completely processed with some UseCaseCamera that was open when
     * corresponding request was issued. (e.g. UseCaseCamera was closed for recreation) Thus, these
     * requests should be retried when a new UseCaseCamera is created.
     */
    @GuardedBy("mutex") private val pendingRequests = LinkedList<CaptureRequest>()

    override fun reset() {
        threads.sequentialScope.launch {
            mutex.withLock {
                while (pendingRequests.isNotEmpty()) {
                    pendingRequests
                        .poll()
                        ?.result
                        ?.completeExceptionally(
                            ImageCaptureException(
                                ImageCapture.ERROR_CAMERA_CLOSED,
                                "Capture request is cancelled due to a reset",
                                null
                            )
                        )
                }
            }
        }
    }

    public fun issueCaptureRequests(
        captureConfigs: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): ListenableFuture<List<Void?>> {
        val signal = CompletableDeferred<List<Void?>>()

        threads.sequentialScope.launch {
            val request = CaptureRequest(captureConfigs, captureMode, flashType, signal)
            requestControl?.let { requestControl ->
                submitRequest(request, requestControl)
                    .propagateResultOrEnqueueRequest(request, requestControl)
            }
                ?: run {
                    // UseCaseCamera may become null by the time the coroutine is started
                    mutex.withLock { pendingRequests.add(request) }
                    debug {
                        "StillCaptureRequestControl: useCaseCamera is null, $request" +
                            " will be retried with a future UseCaseCamera"
                    }
                }
        }

        return Futures.nonCancellationPropagating(signal.asListenableFuture())
    }

    private fun submitPendingRequests() {
        threads.sequentialScope.launch {
            mutex.withLock {
                while (pendingRequests.isNotEmpty()) {
                    pendingRequests.poll()?.let { request ->
                        requestControl?.let { requestControl ->
                            submitRequest(request, requestControl)
                                .propagateResultOrEnqueueRequest(
                                    submittedRequest = request,
                                    currentRequestControl = requestControl
                                )
                        }
                    }
                }
            }
        }
    }

    private suspend fun submitRequest(
        request: CaptureRequest,
        requestControl: UseCaseCameraRequestControl
    ): Deferred<List<Void?>> {
        debug { "StillCaptureRequestControl: submitting $request at $requestControl" }
        // Prior to submitStillCaptures, wait until the pending flash mode session change is
        // completed. On some devices, AE preCapture triggered in submitStillCaptures may not
        // work properly if the repeating request to change the flash mode is not completed.
        val flashMode = flashControl.awaitFlashModeUpdate()
        debug { "StillCaptureRequestControl: Issuing single capture" }
        val deferredList =
            requestControl.issueSingleCaptureAsync(
                request.captureConfigs,
                request.captureMode,
                request.flashType,
                flashMode,
            )

        return threads.sequentialScope.async {
            // requestControl.issueSingleCaptureAsync shouldn't be invoked from here directly,
            // because sequentialScope.async is may not be executed immediately
            debug { "StillCaptureRequestControl: Waiting for deferred list from $request" }
            deferredList.awaitAll().also {
                debug { "StillCaptureRequestControl: Waiting for deferred list from $request done" }
            }
        }
    }

    private fun Deferred<List<Void?>>.propagateResultOrEnqueueRequest(
        submittedRequest: CaptureRequest,
        currentRequestControl: UseCaseCameraRequestControl
    ) {
        invokeOnCompletion { cause: Throwable? ->
            if (
                cause is ImageCaptureException &&
                    cause.imageCaptureError == ImageCapture.ERROR_CAMERA_CLOSED
            ) {
                threads.sequentialScope.launch {
                    var isPending = true

                    requestControl?.let { latestRequestControl ->
                        if (currentRequestControl != latestRequestControl) {
                            // camera has already been changed, can retry immediately
                            submitRequest(submittedRequest, latestRequestControl)
                                .propagateResultOrEnqueueRequest(
                                    submittedRequest = submittedRequest,
                                    currentRequestControl = latestRequestControl
                                )
                            isPending = false
                        }
                    }

                    // no new camera to retry at, adding to pending list for trying later
                    if (isPending) {
                        mutex.withLock { pendingRequests.add(submittedRequest) }
                        debug {
                            "StillCaptureRequestControl: failed to submit $submittedRequest" +
                                ", will be retried with a future UseCaseCamera"
                        }
                    }
                }
            } else {
                propagateCompletion(submittedRequest.result, cause)
            }
        }
    }

    @Module
    public abstract class Bindings {
        @Binds
        @IntoSet
        public abstract fun provideControls(
            control: StillCaptureRequestControl
        ): UseCaseCameraControl
    }
}
