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
import android.media.Image
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.impl.CameraControlInternal.ControlUpdateCallback
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraCaptureResult
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.fakes.FakeCameraDeviceSurfaceManager
import androidx.camera.testing.fakes.FakeCameraFactory
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
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
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    shadows = [ShadowCameraX::class, ShadowImageReader::class]
)
class ImageCaptureTest {

    private var executor: Executor? = null
    private var callbackHandler: Handler? = null
    private var fakeCameraControl: FakeCameraControl? = null

    @Before
    @Throws(ExecutionException::class, InterruptedException::class)
    fun setUp() {
        fakeCameraControl = FakeCameraControl(
            object : ControlUpdateCallback {
                override fun onCameraControlUpdateSessionConfig(
                    sessionConfig: SessionConfig
                ) {
                }

                override fun onCameraControlCaptureRequests(
                    captureConfigs: List<CaptureConfig>
                ) {
                }
            })
        val cameraFactoryProvider =
            CameraFactory.Provider { _: Context?, _: CameraThreadConfig? ->
                val cameraFactory = FakeCameraFactory()
                cameraFactory.insertDefaultBackCamera(ShadowCameraX.DEFAULT_CAMERA_ID) {
                    FakeCamera(
                        ShadowCameraX.DEFAULT_CAMERA_ID, fakeCameraControl,
                        FakeCameraInfoInternal()
                    )
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
        ShadowImageReader.clear()
    }

    @After
    @Throws(ExecutionException::class, InterruptedException::class)
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { CameraX.unbindAll() }
        CameraX.shutdown().get()
    }

    @Test
    fun capturedImageSize_isEqualToSurfaceSize() {
        // Arrange.
        val timestamp = 0L
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setCaptureOptionUnpacker { _: UseCaseConfig<*>?, _: CaptureConfig.Builder? -> }
            .build()
        var capturedImage: ImageProxy? = null
        val onImageCapturedCallback = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                capturedImage = image
            }

            override fun onError(exception: ImageCaptureException) {
            }
        }

        // Act.
        // Bind UseCase amd take picture.
        val lifecycleOwner = FakeLifecycleOwner()
        InstrumentationRegistry.getInstrumentation()
            .runOnMainSync {
                CameraX.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageCapture
                )
                lifecycleOwner.startAndResume()
            }
        imageCapture.takePicture(executor!!, onImageCapturedCallback)
        // Send mock image.
        ShadowImageReader.triggerCallbackWithMockImage(createMockImage(timestamp))
        flushHandler(callbackHandler)
        // Send fake image info.
        fakeCameraControl?.notifyAllRequestsOnCaptureCompleted(
            FakeCameraCaptureResult.Builder().setTimestamp(timestamp).build()
        )
        flushHandler(callbackHandler)

        // Assert.
        Truth.assertThat(capturedImage?.width)
            .isEqualTo(FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.width)
        Truth.assertThat(capturedImage?.height)
            .isEqualTo(FakeCameraDeviceSurfaceManager.MAX_OUTPUT_SIZE.height)
    }

    private fun flushHandler(handler: Handler?) {
        (Shadow.extract<Any>(handler!!.looper) as ShadowLooper).idle()
    }

    private fun createMockImage(timestamp: Long): Image? {
        val mockImage = mock(Image::class.java)
        Mockito.`when`(mockImage.timestamp).thenReturn(timestamp)
        return mockImage
    }
}
