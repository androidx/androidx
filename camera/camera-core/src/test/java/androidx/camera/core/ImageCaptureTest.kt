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

package androidx.camera.core

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.view.Surface
import androidx.camera.core.ImageCapture.ImageCaptureRequest
import androidx.camera.core.ImageCapture.ImageCaptureRequestProcessor
import androidx.camera.core.ImageCapture.ImageCaptureRequestProcessor.ImageCaptor
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import androidx.camera.testing.fakes.FakeImageReaderProxy
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.concurrent.futures.ResolvableFuture
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import java.util.ArrayDeque
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

private const val MAX_IMAGES = 3

/**
 * Unit tests for [ImageCapture].
 */
@MediumTest
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, shadows = [ShadowCameraX::class])
class ImageCaptureTest {

    private lateinit var callbackHandler: Handler
    private lateinit var callbackThread: HandlerThread
    private lateinit var executor: Executor
    private var fakeImageReaderProxy: FakeImageReaderProxy? = null
    private var capturedImage: ImageProxy? = null
    private val onImageCapturedCallback = object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            capturedImage = image
        }

        override fun onError(exception: ImageCaptureException) {
        }
    }

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        val cameraFactoryProvider =
            CameraFactory.Provider { _: Context?, _: CameraThreadConfig? ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(ShadowCameraX.DEFAULT_CAMERA_ID) {
                    FakeCamera(ShadowCameraX.DEFAULT_CAMERA_ID)
                }
                cameraFactory
            }
        val cameraXConfig = CameraXConfig.Builder.fromConfig(
            FakeAppConfig.create()
        ).setCameraFactoryProvider(cameraFactoryProvider).build()
        val context =
            ApplicationProvider.getApplicationContext<Context>()
        CameraX.initialize(context, cameraXConfig).get()
        callbackThread = HandlerThread("Callback")
        callbackThread.start()
        callbackHandler = Handler(callbackThread.looper)
        executor = CameraXExecutors.newHandlerExecutor(callbackHandler)
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { CameraX.unbindAll() }
        CameraX.shutdown().get()
        fakeImageReaderProxy = null
        callbackThread.quitSafely()
    }

    @Test
    fun captureImageWithViewPortCropRectLargerThanBuffer_cropRectIsFullSize() {
        // Arrange.
        val imageCapture = bindImageCapture(
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )
        // Overwrites the view port crop rect to be larger than the surface. Cannot rely on the
        // real code path to test this because ShadowMatrix#invert doesn't work.
        val largerThanBufferRect = Rect(-1, -1, 10000, 10000)
        imageCapture.viewPortCropRect = largerThanBufferRect

        // Act
        imageCapture.takePicture(executor, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable("tag", 0)
        flushHandler(callbackHandler)

        // Assert.
        Truth.assertThat(capturedImage!!.viewPortRect).isEqualTo(largerThanBufferRect)
        // When view port rect is larger, crop rect is full buffer size.
        Truth.assertThat(capturedImage!!.cropRect).isEqualTo(
            Rect(0, 0, fakeImageReaderProxy!!.width, fakeImageReaderProxy!!.height)
        )
    }

    @Test
    fun captureImageWithViewPort_isSet() {
        // Arrange
        val imageCapture = bindImageCapture(
            ViewPort.Builder(Rational(1, 1), Surface.ROTATION_0).build()
        )

        // Act
        imageCapture.takePicture(executor, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable("tag", 0)
        flushHandler(callbackHandler)

        // Assert.
        // The expected value is based on fitting the 1:1 view port into a rect with the size of
        // the ImageReader.
        val expectedPadding = (fakeImageReaderProxy!!.width - fakeImageReaderProxy!!.height) / 2
        Truth.assertThat(capturedImage!!.viewPortRect).isEqualTo(
            Rect(
                expectedPadding, 0, fakeImageReaderProxy!!.width - expectedPadding,
                fakeImageReaderProxy!!.height
            )
        )
        Truth.assertThat(capturedImage!!.cropRect).isEqualTo(
            Rect(
                expectedPadding, 0, fakeImageReaderProxy!!.width - expectedPadding,
                fakeImageReaderProxy!!.height
            )
        )
    }

    @Test
    fun capturedImageSize_isEqualToSurfaceSize() {
        // Act/arrange.
        val imageCapture = bindImageCapture()

        // Act
        imageCapture.takePicture(executor, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable("tag", 0)
        flushHandler(callbackHandler)

        // Assert.
        Truth.assertThat(capturedImage!!.width).isEqualTo(fakeImageReaderProxy?.width)
        Truth.assertThat(capturedImage!!.height).isEqualTo(fakeImageReaderProxy?.height)
    }

    @Test
    fun imageCaptureRequestProcessor_canSendRequest() {
        // Arrange.
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, createSuccessImageCaptor())
        val request = createImageCaptureRequest()

        // Act.
        requestProcessor.sendRequest(request)

        // Assert.
        verify(request).dispatchImage(any())
    }

    @Test
    fun imageCaptureRequestProcessor_canSendMultipleRequests() {
        // Arrange.
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, createSuccessImageCaptor())
        for (i in 0 until MAX_IMAGES) {
            val request = createImageCaptureRequest()

            // Act.
            requestProcessor.sendRequest(request)

            // Assert.
            verify(request).dispatchImage(any())
        }
    }

    @Test
    fun imageCaptureRequestProcessor_onlyAllowOneRequestProcessing() {
        // Arrange.
        // Create an ImageCaptor that won't complete the future.
        val captorFutureRef = AtomicReference<ResolvableFuture<ImageProxy>?>()
        val imageCaptor = createHoldImageCaptor(captorFutureRef)
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, imageCaptor)
        val request0 = createImageCaptureRequest()
        val request1 = createImageCaptureRequest()

        // Act.
        requestProcessor.sendRequest(request0)
        requestProcessor.sendRequest(request1)

        // Assert.
        // Has processing request but not complete.
        assertThat(captorFutureRef.get()).isNotNull()
        verify(request0, never()).dispatchImage(any())
        verify(request1, never()).dispatchImage(any())

        // Act.
        // Complete request0.
        captorFutureRef.getAndSet(null)!!.set(mock(ImageProxy::class.java))

        // Assert.
        // request0 is complete and request1 is in processing.
        verify(request0).dispatchImage(any())
        verify(request1, never()).dispatchImage(any())
        assertThat(captorFutureRef.get()).isNotNull()

        // Act.
        // Complete request1.
        captorFutureRef.getAndSet(null)!!.set(mock(ImageProxy::class.java))

        // Assert.
        verify(request1).dispatchImage(any())
    }

    @Test
    fun imageCaptureRequestProcessor_unableToProcessNextWhenOverMaxImages() {
        // Arrange.
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, createSuccessImageCaptor())

        // Exhaust outstanding image quota.
        val images = ArrayDeque<ImageProxy>()
        for (i in 0 until MAX_IMAGES) {
            val request = createImageCaptureRequest()
            requestProcessor.sendRequest(request)

            // Save the dispatched images.
            val captor = ArgumentCaptor.forClass(ImageProxy::class.java)
            verify(request).dispatchImage(captor.capture())
            images.offer(captor.value)
        }
        assertThat(images.size).isEqualTo(MAX_IMAGES)

        // Act.
        // Send one more request.
        val request = createImageCaptureRequest()
        requestProcessor.sendRequest(request)

        // Assert.
        verify(request, never()).dispatchImage(any())

        // Act.
        // Close one image to trigger next processing.
        images.poll()!!.close()

        // Assert.
        // It should trigger next processing.
        verify(request).dispatchImage(any())
    }

    @Test
    fun imageCaptureRequestProcessor_canCancelRequests() {
        // Arrange.
        // Create an ImageCaptor that won't complete the future.
        val captorFutureRef = AtomicReference<ResolvableFuture<ImageProxy>?>()
        val imageCaptor = createHoldImageCaptor(captorFutureRef)
        val requestProcessor = ImageCaptureRequestProcessor(MAX_IMAGES, imageCaptor)

        // Send multiple requests and save these requests.
        val requestList = ArrayList<ImageCaptureRequest>()
        for (i in 0 until 5) {
            val request = createImageCaptureRequest()
            requestList.add(request)
            requestProcessor.sendRequest(request)
        }

        // Act.
        val errorMsg = "Cancel request."
        val throwable = RuntimeException(errorMsg)
        requestProcessor.cancelRequests(throwable)

        // Assert.
        for (request in requestList) {
            verify(request).notifyCallbackError(anyInt(), eq(errorMsg), eq(throwable))
        }
        // Capture future is cancelled.
        assertThat(captorFutureRef.get()!!.isCancelled).isTrue()
    }

    @Test
    fun imageCaptureRequestProcessor_requestFail() {
        // Arrange.
        val errorMsg = "Capture failed."
        val throwable = RuntimeException(errorMsg)
        val requestProcessor =
            ImageCaptureRequestProcessor(MAX_IMAGES, createFailedImageCaptor(throwable))
        val request = createImageCaptureRequest()

        // Act.
        requestProcessor.sendRequest(request)

        // Verify.
        verify(request).notifyCallbackError(anyInt(), eq(errorMsg), eq(throwable))
    }

    private fun bindImageCapture(): ImageCapture {
        return bindImageCapture(null)
    }

    private fun bindImageCapture(viewPort: ViewPort?): ImageCapture {
        // Arrange.
        val imageCapture = ImageCapture.Builder()
            // Set non jpg format so it doesn't trigger the exif code path.
            .setBufferFormat(ImageFormat.YUV_420_888)
            .setTargetRotation(Surface.ROTATION_0)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setCaptureOptionUnpacker { _: UseCaseConfig<*>?, _: CaptureConfig.Builder? -> }
            .setImageReaderProxyProvider(getImageReaderProxyProvider())
            .build()

        // Act.
        // Bind UseCase amd take picture.
        val lifecycleOwner = FakeLifecycleOwner()
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync {
                CameraX.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    viewPort,
                    imageCapture
                )
                lifecycleOwner.startAndResume()
            }
        return imageCapture
    }

    private fun getImageReaderProxyProvider(): ImageReaderProxyProvider {
        return ImageReaderProxyProvider { width, height, imageFormat, queueDepth, usage ->
            fakeImageReaderProxy = FakeImageReaderProxy.newInstance(
                width, height, imageFormat, queueDepth, usage
            )
            fakeImageReaderProxy!!
        }
    }

    private fun flushHandler(handler: Handler?) {
        (Shadow.extract<Any>(handler!!.looper) as ShadowLooper).idle()
    }

    private fun createImageCaptureRequest(): ImageCaptureRequest {
        return mock(ImageCaptureRequest::class.java)
    }

    private fun createSuccessImageCaptor(): ImageCaptor {
        return ImageCaptor {
            Futures.immediateFuture(FakeImageProxy(FakeImageInfo()))
        }
    }

    private fun createHoldImageCaptor(
        futureHolder: AtomicReference<ResolvableFuture<ImageProxy>?>
    ): ImageCaptor {
        return ImageCaptor {
            ResolvableFuture.create<ImageProxy>().apply {
                futureHolder.set(this)
            }
        }
    }

    private fun createFailedImageCaptor(throwable: Throwable): ImageCaptor {
        return ImageCaptor {
            Futures.immediateFailedFuture(throwable)
        }
    }
}
