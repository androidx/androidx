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

package androidx.camera.camera2.impl

import androidx.annotation.GuardedBy
import androidx.camera.camera2.adapter.CameraSessionLifecycleAdapter
import androidx.camera.camera2.adapter.asListenableFuture
import androidx.camera.camera2.adapter.propagateCompletion
import androidx.camera.camera2.config.CameraScope
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CameraSessionLifecycleCallback
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import com.google.common.util.concurrent.ListenableFuture
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import java.util.LinkedList
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
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
    cameraSessionLifecycleAdapter: CameraSessionLifecycleAdapter,
) : UseCaseCameraControl {
    private val mutex = Mutex()
    private var sessionStartCount = 0L
    @Volatile private var isSessionStarted = false

    init {
        cameraSessionLifecycleAdapter.addSessionLifecycleCallback(
            threads.sequentialExecutor,
            object : CameraSessionLifecycleCallback() {
                override fun onSessionStarted() {
                    isSessionStarted = true
                    sessionStartCount++
                    trySubmitPendingRequests()
                }

                override fun onSessionStopped() {
                    isSessionStarted = false
                }
            },
        )
    }

    private var _requestControl: UseCaseCameraRequestControl? = null
    override var requestControl: UseCaseCameraRequestControl?
        get() = _requestControl
        set(value) {
            _requestControl = value
            isSessionStarted = false
            trySubmitPendingRequests()
        }

    public data class CaptureRequest(
        val captureConfigs: List<CaptureConfig>,
        @ImageCapture.CaptureMode val captureMode: Int,
        @ImageCapture.FlashType val flashType: Int,
        val result: CompletableDeferred<List<Void?>>,
    )

    /**
     * These requests failed to be completely processed with some UseCaseCamera that was open when
     * corresponding request was issued (e.g. UseCaseCamera was closed for recreation, or the
     * underlying camera session disconnected and restarted in-place). Thus, these requests should
     * be retried when a new UseCaseCamera is created or when the camera session starts again.
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
                                null,
                            )
                        )
                }
            }
        }
    }

    /**
     * Issues [captureConfigs] to the active [UseCaseCameraRequestControl], or queues them in
     * [pendingRequests] until a session is ready.
     *
     * Cancelling the returned [ListenableFuture] (e.g. via `RequestWithCallback.abort()` on
     * `ON_STOP`) marks the request's result cancelled so it is never queued or retried, without
     * cancelling any already-running [CapturePipeline] coroutine (preserving `NonCancellable`
     * post-capture 3A/flash/torch restoration).
     */
    public fun issueCaptureRequests(
        captureConfigs: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
    ): ListenableFuture<List<Void?>> {
        val signal = CompletableDeferred<List<Void?>>()

        threads.sequentialScope.launch {
            val request = CaptureRequest(captureConfigs, captureMode, flashType, signal)
            val requestControl = requestControl
            if (requestControl != null && requestControl.awaitSurfaceSetup()) {
                submitRequest(request, requestControl)
            } else if (enqueuePendingRequest(request)) {
                // UseCaseCamera may become null by the time the coroutine is started
                Camera2Logger.debug {
                    "StillCaptureRequestControl: useCaseCamera is null, $request" +
                        " will be retried with a future UseCaseCamera"
                }
            }
        }

        return signal.asListenableFuture().also { future ->
            future.addListener(
                {
                    if (future.isCancelled) {
                        signal.cancel()
                    }
                },
                CameraXExecutors.directExecutor(),
            )
        }
    }

    /** Queues [request] for a future session unless its caller already cancelled it. */
    private suspend fun enqueuePendingRequest(request: CaptureRequest): Boolean = mutex.withLock {
        if (request.result.isActive) {
            pendingRequests.add(request)
            true
        } else {
            false
        }
    }

    private fun trySubmitPendingRequests() {
        threads.sequentialScope.launch {
            val requestControl = requestControl ?: return@launch
            if (requestControl.awaitSurfaceSetup()) {
                mutex.withLock {
                    while (pendingRequests.isNotEmpty()) {
                        pendingRequests.poll()?.let { request ->
                            submitRequest(request, requestControl)
                        }
                    }
                }
            }
        }
    }

    private suspend fun submitRequest(
        request: CaptureRequest,
        requestControl: UseCaseCameraRequestControl,
    ) {
        if (!request.result.isActive) {
            return
        }
        Camera2Logger.debug { "StillCaptureRequestControl: submitting $request at $requestControl" }
        // Prior to submitStillCaptures, wait until the pending flash mode session change is
        // completed. On some devices, AE preCapture triggered in submitStillCaptures may not
        // work properly if the repeating request to change the flash mode is not completed.
        val flashMode = flashControl.awaitFlashModeUpdate()
        if (!request.result.isActive) {
            return
        }
        val submittedSessionStartCount = sessionStartCount
        Camera2Logger.debug { "StillCaptureRequestControl: Issuing single capture" }
        val deferredList =
            requestControl.issueSingleCaptureAsync(
                request.captureConfigs,
                request.captureMode,
                request.flashType,
                flashMode,
            )

        threads.sequentialScope
            .async(start = CoroutineStart.UNDISPATCHED) {
                // requestControl.issueSingleCaptureAsync shouldn't be invoked from here directly,
                // because sequentialScope.async is may not be executed immediately
                Camera2Logger.debug {
                    "StillCaptureRequestControl: Waiting for deferred list from $request"
                }
                deferredList.awaitAll().also {
                    Camera2Logger.debug {
                        "StillCaptureRequestControl: Waiting for deferred list from $request done"
                    }
                }
            }
            .propagateResultOrEnqueueRequest(
                submittedRequest = request,
                currentRequestControl = requestControl,
                submittedSessionStartCount = submittedSessionStartCount,
            )
    }

    private fun Deferred<List<Void?>>.propagateResultOrEnqueueRequest(
        submittedRequest: CaptureRequest,
        currentRequestControl: UseCaseCameraRequestControl,
        submittedSessionStartCount: Long,
    ) {
        invokeOnCompletion { cause: Throwable? ->
            if (
                cause is ImageCaptureException &&
                    cause.imageCaptureError == ImageCapture.ERROR_CAMERA_CLOSED
            ) {
                threads.sequentialScope.launch {
                    var isPending = true

                    requestControl?.let { latestRequestControl ->
                        if (
                            currentRequestControl != latestRequestControl ||
                                (isSessionStarted &&
                                    submittedSessionStartCount != sessionStartCount)
                        ) {
                            // camera has already been changed or session restarted, can retry
                            // immediately
                            submitRequest(submittedRequest, latestRequestControl)
                            isPending = false
                        }
                    }

                    // no new camera to retry at, adding to pending list for trying later
                    if (isPending && enqueuePendingRequest(submittedRequest)) {
                        Camera2Logger.debug {
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
