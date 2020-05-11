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
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeImageReaderProxy
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

/**
 * Unit tests for [ImageCapture].
 */
@MediumTest
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP, shadows = [ShadowCameraX::class])
class ImageCaptureTest {

    private var executor: Executor? = null
    private var callbackHandler: Handler? = null
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
        val callbackThread = HandlerThread("Callback")
        callbackThread.start()
        callbackHandler = Handler(callbackThread.looper)
        executor = CameraXExecutors.newHandlerExecutor(callbackHandler!!)
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { CameraX.unbindAll() }
        CameraX.shutdown().get()
        fakeImageReaderProxy = null
    }

    @Test
    fun captureImageWithViewPortCropRectLargerThanBuffer_cropRectIsFullSize() {
        // Arrange.
        val imageCapture = bindImageCapture(
            ViewPort.Builder()
                .setAspectRatio(Rational(1, 1))
                .setScaleType(ViewPort.FILL_CENTER)
                .setRotation(Surface.ROTATION_0).build()
        )
        // Overwrites the view port crop rect to be larger than the surface. Cannot rely on the
        // real code path to test this because ShadowMatrix#invert doesn't work.
        val largerThanBufferRect = Rect(-1, -1, 10000, 10000)
        imageCapture.viewPortCropRect = largerThanBufferRect

        // Act
        imageCapture.takePicture(executor!!, onImageCapturedCallback)
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
            ViewPort.Builder()
                .setAspectRatio(Rational(1, 1))
                .setScaleType(ViewPort.FILL_CENTER)
                .setRotation(Surface.ROTATION_0).build()
        )

        // Act
        imageCapture.takePicture(executor!!, onImageCapturedCallback)
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
        imageCapture.takePicture(executor!!, onImageCapturedCallback)
        // Send fake image.
        fakeImageReaderProxy?.triggerImageAvailable("tag", 0)
        flushHandler(callbackHandler)

        // Assert.
        Truth.assertThat(capturedImage!!.width).isEqualTo(fakeImageReaderProxy?.width)
        Truth.assertThat(capturedImage!!.height).isEqualTo(fakeImageReaderProxy?.height)
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
}
