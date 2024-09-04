/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.core

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.imagecapture.TakePictureRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Captures a new still image for in memory access.
 *
 * The caller is responsible for calling [ImageProxy.close] on the returned image.
 *
 * @param onCaptureStarted Callback for when the camera has started exposing the frame.
 * @param onCaptureProcessProgressed Callback to report the progress of the capture's processing.
 * @param onPostviewBitmapAvailable Callback to notify that the postview bitmap is available.
 * @see ImageCapture.takePicture
 * @see ImageCapture.OnImageCapturedCallback
 */
public suspend fun ImageCapture.takePicture(
    onCaptureStarted: (() -> Unit)? = null,
    onCaptureProcessProgressed: ((Int) -> Unit)? = null,
    onPostviewBitmapAvailable: ((Bitmap) -> Unit)? = null,
): ImageProxy {
    val callbackExecutor =
        (currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher)?.asExecutor()
            ?: CameraXExecutors.directExecutor()
    return suspendCancellableCoroutine { continuation ->
        lateinit var delegatingCallback: DelegatingImageCapturedCallback
        delegatingCallback =
            DelegatingImageCapturedCallback(
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureStarted() {
                        onCaptureStarted?.invoke()
                    }

                    override fun onCaptureProcessProgressed(progress: Int) {
                        onCaptureProcessProgressed?.invoke(progress)
                    }

                    override fun onPostviewBitmapAvailable(bitmap: Bitmap) {
                        onPostviewBitmapAvailable?.invoke(bitmap)
                    }

                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        delegatingCallback.dispose()
                        continuation.resume(imageProxy)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        delegatingCallback.dispose()
                        continuation.resumeWithException(exception)
                    }
                }
            )
        continuation.invokeOnCancellation { delegatingCallback.dispose() }
        takePicture(callbackExecutor, delegatingCallback)
    }
}

/**
 * Captures a new still image and saves to a file along with application specified metadata.
 *
 * @param outputFileOptions Options to store the output image file.
 * @param onCaptureStarted Callback for when the camera has started exposing the frame.
 * @param onCaptureProcessProgressed Callback to report the progress of the capture's processing.
 * @param onPostviewBitmapAvailable Callback to notify that the postview bitmap is available.
 * @see ImageCapture.takePicture
 * @see ImageCapture.OnImageSavedCallback
 */
public suspend fun ImageCapture.takePicture(
    outputFileOptions: ImageCapture.OutputFileOptions,
    onCaptureStarted: (() -> Unit)? = null,
    onCaptureProcessProgressed: ((Int) -> Unit)? = null,
    onPostviewBitmapAvailable: ((Bitmap) -> Unit)? = null,
): OutputFileResults {
    val callbackExecutor =
        (currentCoroutineContext()[ContinuationInterceptor] as? CoroutineDispatcher)?.asExecutor()
            ?: CameraXExecutors.directExecutor()
    return suspendCancellableCoroutine { continuation ->
        lateinit var delegatingCallback: DelegatingImageSavedCallback
        delegatingCallback =
            DelegatingImageSavedCallback(
                object : ImageCapture.OnImageSavedCallback {
                    override fun onCaptureStarted() {
                        onCaptureStarted?.invoke()
                    }

                    override fun onCaptureProcessProgressed(progress: Int) {
                        onCaptureProcessProgressed?.invoke(progress)
                    }

                    override fun onPostviewBitmapAvailable(bitmap: Bitmap) {
                        onPostviewBitmapAvailable?.invoke(bitmap)
                    }

                    override fun onImageSaved(outputFileResults: OutputFileResults) {
                        delegatingCallback.dispose()
                        continuation.resume(outputFileResults)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        delegatingCallback.dispose()
                        continuation.resumeWithException(exception)
                    }
                }
            )
        continuation.invokeOnCancellation { delegatingCallback.dispose() }
        takePicture(outputFileOptions, callbackExecutor, delegatingCallback)
    }
}

@VisibleForTesting
internal fun ImageCapture.getTakePictureRequest(): TakePictureRequest? {
    return takePictureManager.capturingRequest?.takePictureRequest
}

private class DelegatingImageCapturedCallback(delegate: ImageCapture.OnImageCapturedCallback) :
    ImageCapture.OnImageCapturedCallback() {
    private val _delegate = AtomicReference(delegate)
    private val delegate: ImageCapture.OnImageCapturedCallback?
        get() = _delegate.get()

    fun dispose() {
        _delegate.set(null)
    }

    override fun onCaptureStarted() {
        delegate?.onCaptureStarted()
    }

    override fun onCaptureProcessProgressed(progress: Int) {
        delegate?.onCaptureProcessProgressed(progress)
    }

    override fun onPostviewBitmapAvailable(bitmap: Bitmap) {
        delegate?.onPostviewBitmapAvailable(bitmap)
    }

    override fun onCaptureSuccess(imageProxy: ImageProxy) {
        delegate?.onCaptureSuccess(imageProxy) ?: run { imageProxy.close() }
    }

    override fun onError(exception: ImageCaptureException) {
        delegate?.onError(exception)
    }
}

private class DelegatingImageSavedCallback(delegate: ImageCapture.OnImageSavedCallback) :
    ImageCapture.OnImageSavedCallback {
    private val _delegate = AtomicReference(delegate)
    private val delegate: ImageCapture.OnImageSavedCallback?
        get() = _delegate.get()

    fun dispose() {
        _delegate.set(null)
    }

    override fun onCaptureStarted() {
        delegate?.onCaptureStarted()
    }

    override fun onCaptureProcessProgressed(progress: Int) {
        delegate?.onCaptureProcessProgressed(progress)
    }

    override fun onPostviewBitmapAvailable(bitmap: Bitmap) {
        delegate?.onPostviewBitmapAvailable(bitmap)
    }

    override fun onImageSaved(outputFileResults: OutputFileResults) {
        delegate?.onImageSaved(outputFileResults)
    }

    override fun onError(exception: ImageCaptureException) {
        delegate?.onError(exception)
    }
}
