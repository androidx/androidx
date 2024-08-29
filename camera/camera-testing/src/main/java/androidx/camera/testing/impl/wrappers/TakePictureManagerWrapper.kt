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

package androidx.camera.testing.impl.wrappers

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageProcessingUtil
import androidx.camera.core.ImageProxy
import androidx.camera.core.Logger
import androidx.camera.core.imagecapture.Bitmap2JpegBytes
import androidx.camera.core.imagecapture.ImageCaptureControl
import androidx.camera.core.imagecapture.ImagePipeline
import androidx.camera.core.imagecapture.JpegBytes2Disk
import androidx.camera.core.imagecapture.JpegBytes2Image
import androidx.camera.core.imagecapture.RequestWithCallback
import androidx.camera.core.imagecapture.TakePictureManager
import androidx.camera.core.imagecapture.TakePictureManagerImpl
import androidx.camera.core.imagecapture.TakePictureRequest
import androidx.camera.core.processing.Packet
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil
import androidx.camera.testing.impl.fakes.FakeCameraCaptureResult
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy

/**
 * A [TakePictureManager] implementation wrapped around the real implementation
 * [TakePictureManagerImpl].
 *
 * It is used for fake cameras and provides fake image capture results when required from a camera.
 */
public class TakePictureManagerWrapper(
    imageCaptureControl: ImageCaptureControl,
    private val fakeCameras: List<FakeCamera>
) : TakePictureManager {
    // Try to keep the fake as close to real as possible
    private val managerDelegate = TakePictureManagerImpl(imageCaptureControl)

    private val bitmap2JpegBytes = Bitmap2JpegBytes()
    private val jpegBytes2Disk = JpegBytes2Disk()
    private val jpegBytes2Image = JpegBytes2Image()

    private val imageProxyQueue = ArrayDeque<ImageProxy>()
    private val outputFileResultsQueue = ArrayDeque<ImageCapture.OutputFileResults>()

    /** Whether to disable auto capture completion. */
    public var disableAutoComplete: Boolean = false

    override fun setImagePipeline(imagePipeline: ImagePipeline) {
        managerDelegate.imagePipeline = imagePipeline
    }

    override fun offerRequest(takePictureRequest: TakePictureRequest) {
        val listeners = mutableListOf<FakeCameraControl.OnNewCaptureRequestListener>()

        fakeCameras.forEach { camera ->
            if (camera.cameraControlInternal is FakeCameraControl) {
                (camera.cameraControlInternal as FakeCameraControl).apply {
                    val listener =
                        FakeCameraControl.OnNewCaptureRequestListener {
                            if (!disableAutoComplete) {
                                completeCapturingRequest(this)
                            }
                        }
                    listeners.add(listener)
                    addOnNewCaptureRequestListener(listener)
                }
            } else {
                Logger.w(
                    TAG,
                    "Ignoring ${camera.cameraControlInternal} as it's not FakeCameraControl!"
                )
            }
        }

        managerDelegate.offerRequest(takePictureRequest)

        fakeCameras.forEach { camera ->
            if (camera.cameraControlInternal is FakeCameraControl) {
                (camera.cameraControlInternal as FakeCameraControl)
                    .removeOnNewCaptureRequestListeners(listeners)
            } else {
                Logger.w(
                    TAG,
                    "Ignoring ${camera.cameraControlInternal} as it's not FakeCameraControl!"
                )
            }
        }
    }

    override fun pause() {
        managerDelegate.pause()
    }

    override fun resume() {
        managerDelegate.resume()
    }

    override fun abortRequests() {
        managerDelegate.abortRequests()
    }

    @VisibleForTesting
    override fun hasCapturingRequest(): Boolean = managerDelegate.hasCapturingRequest()

    @VisibleForTesting
    override fun getCapturingRequest(): RequestWithCallback? = managerDelegate.capturingRequest

    @VisibleForTesting
    override fun getIncompleteRequests(): List<RequestWithCallback> =
        managerDelegate.incompleteRequests

    @VisibleForTesting
    override fun getImagePipeline(): ImagePipeline = managerDelegate.imagePipeline

    @VisibleForTesting
    public fun completeCapturingRequest(fakeCameraControl: FakeCameraControl) {
        Log.d(
            TAG,
            "completeCapturingRequest: capturingRequest = ${managerDelegate.capturingRequest}"
        )
        managerDelegate.capturingRequest?.apply {
            onCaptureStarted()

            // This ensures the future from CameraControlInternal#submitStillCaptureRequests() is
            // completed and not garbage collected later
            // TODO - notify all the new requests, not only the last one
            fakeCameraControl.notifyLastRequestOnCaptureCompleted(FakeCameraCaptureResult())

            onImageCaptured()

            takePictureRequest.also { req ->
                val outputFileOptions = req.outputFileOptions // enables smartcast for null check
                if (req.onDiskCallback != null && outputFileOptions != null) {
                    if (outputFileResultsQueue.isEmpty()) {
                        onFinalResult(createOutputFileResults(req, outputFileOptions))
                    } else {
                        onFinalResult(outputFileResultsQueue.first())
                        outputFileResultsQueue.removeFirst()
                    }
                } else {
                    if (imageProxyQueue.isEmpty()) {
                        onFinalResult(createImageProxy(req))
                    } else {
                        onFinalResult(imageProxyQueue.first())
                        imageProxyQueue.removeFirst()
                    }
                }
            }
        }
    }

    /**
     * Enqueues an [ImageProxy] to be used as result for the next image capture with
     * [ImageCapture.OnImageCapturedCallback].
     *
     * Note that the provided [ImageProxy] is consumed by next image capture and is not available
     * for following captures. If no result is available during a capture, CameraX will create a
     * fake image by itself and provide result based on that.
     */
    public fun enqueueImageProxy(imageProxy: ImageProxy) {
        imageProxyQueue.add(imageProxy)
    }

    /**
     * Enqueues an [OutputFileResults] to be used as result for the next image capture with
     * [ImageCapture.OnImageSavedCallback].
     *
     * Note that the provided [OutputFileResults] is consumed by next image capture and is not
     * available for following captures. If no result is available during a capture, CameraX will
     * create a fake image by itself and provide result based on that.
     */
    public fun enqueueOutputFileResults(outputFileResults: ImageCapture.OutputFileResults) {
        outputFileResultsQueue.add(outputFileResults)
    }

    private fun createOutputFileResults(
        takePictureRequest: TakePictureRequest,
        outputFileOptions: OutputFileOptions
    ): ImageCapture.OutputFileResults {
        // TODO - Take a bitmap as input and use that directly
        val bytesPacket =
            takePictureRequest.convertBitmapToBytes(
                TestImageUtil.createBitmap(
                    takePictureRequest.cropRect.width(),
                    takePictureRequest.cropRect.height()
                )
            )
        return jpegBytes2Disk.apply(JpegBytes2Disk.In.of(bytesPacket, outputFileOptions))
    }

    private fun createImageProxy(
        takePictureRequest: TakePictureRequest,
    ): ImageProxy {
        // TODO - Take a bitmap as input and use that directly
        val bitmap =
            TestImageUtil.createBitmap(
                takePictureRequest.cropRect.width(),
                takePictureRequest.cropRect.height()
            )
        if (canLoadImageProcessingUtilJniLib()) {
            val bytesPacket =
                takePictureRequest.convertBitmapToBytes(
                    TestImageUtil.createBitmap(
                        takePictureRequest.cropRect.width(),
                        takePictureRequest.cropRect.height()
                    )
                )
            return jpegBytes2Image.apply(bytesPacket).data
        } else {
            return bitmap.toFakeImageProxy()
        }
    }

    private fun Bitmap.toFakeImageProxy(): ImageProxy {
        return FakeImageProxy(FakeImageInfo(), this)
    }

    private fun TakePictureRequest.convertBitmapToBytes(bitmap: Bitmap): Packet<ByteArray> {
        val inputPacket =
            Packet.of(
                bitmap,
                ExifUtil.createExif(
                    TestImageUtil.createJpegBytes(cropRect.width(), cropRect.height())
                ),
                cropRect,
                rotationDegrees,
                Matrix(),
                FakeCameraCaptureResult()
            )

        return bitmap2JpegBytes.apply(Bitmap2JpegBytes.In.of(inputPacket, jpegQuality))
    }

    private fun canLoadImageProcessingUtilJniLib(): Boolean {
        try {
            System.loadLibrary(ImageProcessingUtil.JNI_LIB_NAME)
            return true
        } catch (e: UnsatisfiedLinkError) {
            Logger.d(TAG, "canLoadImageProcessingUtilJniLib", e)
            return false
        }
    }

    private companion object {
        private const val TAG = "TakePictureManagerWrap"
    }
}
