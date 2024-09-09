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
import android.graphics.Rect
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
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.processing.Packet
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraControl.CaptureSuccessListener
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import kotlinx.atomicfu.atomic

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
    private val outputFileResultsQueue = ArrayDeque<OutputFileResults>()

    private val pendingRequestCount = atomic(0)

    private val captureCompleteListener = CaptureSuccessListener { _ ->
        completeCapturingRequest()

        // TODO - handle CameraControl receiving more than one capture per takePictureRequest
        if (pendingRequestCount.decrementAndGet() == 0) {
            removeCaptureCompleteListener()
        }
    }

    private fun addCaptureCompleteListener() {
        Logger.d(TAG, "addCaptureCompleteListener: fakeCameras = $fakeCameras")

        fakeCameras.forEach { camera ->
            if (camera.cameraControlInternal is FakeCameraControl) {
                (camera.cameraControlInternal as FakeCameraControl).addCaptureSuccessListener(
                    captureCompleteListener
                )
            } else {
                Logger.w(
                    TAG,
                    "Ignoring ${camera.cameraControlInternal} as it's not FakeCameraControl!"
                )
            }
        }
    }

    private fun removeCaptureCompleteListener() {
        Logger.d(TAG, "removeCaptureCompleteListener: fakeCameras = $fakeCameras")

        fakeCameras.forEach { camera ->
            if (camera.cameraControlInternal is FakeCameraControl) {
                (camera.cameraControlInternal as FakeCameraControl).removeCaptureSuccessListener(
                    captureCompleteListener
                )
            } else {
                Logger.w(
                    TAG,
                    "Ignoring ${camera.cameraControlInternal} as it's not FakeCameraControl!"
                )
            }
        }
    }

    override fun setImagePipeline(imagePipeline: ImagePipeline) {
        managerDelegate.imagePipeline = imagePipeline
    }

    override fun offerRequest(takePictureRequest: TakePictureRequest) {
        if (pendingRequestCount.getAndIncrement() == 0) {
            addCaptureCompleteListener()
        }

        managerDelegate.offerRequest(takePictureRequest)
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
    private fun completeCapturingRequest() {
        Log.d(
            TAG,
            "completeCapturingRequest: capturingRequest = ${managerDelegate.capturingRequest}"
        )
        managerDelegate.capturingRequest?.apply {
            runOnMainThread { // onCaptureStarted, onImageCaptured etc. are @MainThread annotated
                Logger.d(TAG, "completeCapturingRequest: runOnMainThread")
                onCaptureStarted()
                onImageCaptured()

                // TODO: b/365519650 - Take FakeCameraCaptureResult as parameter to contain extra
                //  user-provided data like bitmap/image proxy and use that to complete capture.
                val bitmap = takePictureRequest.createBitmap()

                val outputFileOptions =
                    takePictureRequest.outputFileOptions // enables smartcast for null check

                if (takePictureRequest.onDiskCallback != null && outputFileOptions != null) {
                    if (outputFileResultsQueue.isEmpty()) {
                        if (outputFileOptions.size > 1) {
                            Logger.w(
                                TAG,
                                "Simultaneous capture not supported, outputFileOptions = $outputFileOptions"
                            )
                        }
                        onFinalResult(
                            createOutputFileResults(
                                takePictureRequest,
                                outputFileOptions[0],
                                bitmap
                            )
                        )
                    } else {
                        onFinalResult(outputFileResultsQueue.removeFirst())
                    }
                } else {
                    if (imageProxyQueue.isEmpty()) {
                        onFinalResult(createImageProxy(takePictureRequest, bitmap))
                    } else {
                        onFinalResult(imageProxyQueue.removeFirst())
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
    public fun enqueueOutputFileResults(outputFileResults: OutputFileResults) {
        outputFileResultsQueue.add(outputFileResults)
    }

    private fun createOutputFileResults(
        takePictureRequest: TakePictureRequest,
        outputFileOptions: OutputFileOptions,
        bitmap: Bitmap,
    ): OutputFileResults {
        // TODO - Take a bitmap as input and use that directly
        val bytesPacket = takePictureRequest.convertBitmapToBytes(bitmap)
        return jpegBytes2Disk.apply(JpegBytes2Disk.In.of(bytesPacket, outputFileOptions))
    }

    private fun createImageProxy(
        takePictureRequest: TakePictureRequest,
        bitmap: Bitmap,
    ): ImageProxy {
        if (canLoadImageProcessingUtilJniLib()) {
            try {
                val bytesPacket = takePictureRequest.convertBitmapToBytes(bitmap)
                return jpegBytes2Image.apply(bytesPacket).data
            } catch (e: Exception) {
                // We have observed this kind of issue in Pixel 2 API 26 emulator, however this is
                // added as a general workaround as this may happen in any emulator/device and
                // similar to how not all resolutions are supported due to device capabilities even
                // in production code.
                Logger.e(
                    TAG,
                    "createImageProxy: failed for cropRect = ${takePictureRequest.cropRect}" +
                        " which may happen due to a high resolution not being supported" +
                        ", trying again with 640x480",
                    e
                )

                val bytesPacket =
                    takePictureRequest.convertBitmapToBytes(
                        createBitmap(640, 480),
                        Rect(0, 0, 640, 480)
                    )
                Logger.d(TAG, "createImageProxy: bytesPacket size = ${bytesPacket.size}")

                return jpegBytes2Image.apply(bytesPacket).data
            }
        } else {
            return bitmap.toFakeImageProxy()
        }
    }

    private fun Bitmap.toFakeImageProxy(): ImageProxy {
        return FakeImageProxy(FakeImageInfo(), this)
    }

    private fun TakePictureRequest.createBitmap() =
        TestImageUtil.createBitmap(cropRect.width(), cropRect.height())

    private fun TakePictureRequest.convertBitmapToBytes(
        bitmap: Bitmap,
        cropRect: Rect = this.cropRect
    ): Packet<ByteArray> {
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

    private fun runOnMainThread(block: () -> Any) {
        CameraXExecutors.mainThreadExecutor().submit(block)
    }

    private companion object {
        private const val TAG = "TakePictureManagerWrap"
    }
}
