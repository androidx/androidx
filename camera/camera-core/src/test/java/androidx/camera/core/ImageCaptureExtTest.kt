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

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Looper
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImageCaptureExtTest {
    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fakeOutputFileOptions by lazy {
        ImageCapture.OutputFileOptions.Builder(temporaryFolder.newFile("fake_path")).build()
    }
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var imageCapture: ImageCapture

    @Before
    fun setup() {
        val latch = CountDownLatch(1)
        ProcessCameraProvider.configureInstance(FakeAppConfig.create())
        ProcessCameraProvider.getInstance(context)
            .addListener(
                {
                    cameraProvider = ProcessCameraProvider.getInstance(context).get()
                    latch.countDown()
                },
                CameraXExecutors.directExecutor()
            )

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()

        imageCapture = ImageCapture.Builder().build()

        cameraProvider.bindToLifecycle(
            FakeLifecycleOwner().apply { startAndResume() },
            CameraSelector.DEFAULT_BACK_CAMERA,
            imageCapture
        )
    }

    @After
    fun tearDown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun takePicture_inMemory_canGetImage(): Unit = runTest {
        // Arrange
        val imageProxy = FakeImageProxy(FakeImageInfo())
        val fakeTakePictureManager = FakeAppConfig.getTakePictureManager()!!
        fakeTakePictureManager.enqueueImageProxy(imageProxy)

        // Arrange & Act.
        val takePictureAsync = MainScope().async { imageCapture.takePicture() }

        // Assert.
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(takePictureAsync.await()).isSameInstanceAs(imageProxy)
    }

    @Test
    fun takePicture_inMemory_canCancel(): Unit = runTest {
        // Arrange & Act.
        val takePictureAsync = MainScope().async { imageCapture.takePicture() }

        // Assert: cancel() should complete the coroutine.
        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_inMemory_canPropagateCaptureStarted(): Unit = runTest {
        // Arrange.
        var callbackCalled = false

        // Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(onCaptureStarted = { callbackCalled = true })
            }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val imageCaptureCallback = imageCapture.getTakePictureRequest()?.inMemoryCallback
        imageCaptureCallback?.onCaptureStarted()

        // Assert.
        assertThat(callbackCalled).isTrue()

        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_inMemory_canPropagateCaptureProcessProgressed(): Unit = runTest {
        // Arrange.
        var callbackCalled = false
        val progress = 100
        var resultProgress = 0
        FakeAppConfig.getTakePictureManager()!!.disableAutoComplete = true

        // Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(
                    onCaptureProcessProgressed = {
                        resultProgress = it
                        callbackCalled = true
                    }
                )
            }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val imageCaptureCallback = imageCapture.getTakePictureRequest()?.inMemoryCallback
        imageCaptureCallback?.onCaptureProcessProgressed(progress)

        // Assert.
        assertThat(callbackCalled).isTrue()
        assertThat(resultProgress).isEqualTo(progress)

        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_inMemory_canPropagatePostviewBitmapAvailable(): Unit = runTest {
        // Arrange.
        var callbackCalled = false
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        lateinit var resultBitmap: Bitmap
        FakeAppConfig.getTakePictureManager()!!.disableAutoComplete = true

        // Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(
                    onPostviewBitmapAvailable = {
                        resultBitmap = it
                        callbackCalled = true
                    }
                )
            }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val imageCaptureCallback = imageCapture.getTakePictureRequest()?.inMemoryCallback
        imageCaptureCallback?.onPostviewBitmapAvailable(bitmap)

        // Assert.
        assertThat(callbackCalled).isTrue()
        assertThat(resultBitmap).isSameInstanceAs(bitmap)

        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_onDisk_canGetResult(): Unit = runTest {
        // Arrange
        val outputFileResults = ImageCapture.OutputFileResults(null)
        val fakeTakePictureManager = FakeAppConfig.getTakePictureManager()!!
        fakeTakePictureManager.enqueueOutputFileResults(outputFileResults)

        // Arrange & Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(outputFileOptions = fakeOutputFileOptions)
            }

        // Assert.
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertThat(takePictureAsync.await()).isSameInstanceAs(outputFileResults)
    }

    @Test
    fun takePicture_onDisk_canCancel(): Unit = runTest {
        // Arrange & Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(outputFileOptions = fakeOutputFileOptions)
            }

        // Assert: cancel() should complete the coroutine.
        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_canPropagateCaptureStarted(): Unit = runTest {
        // Arrange.
        var callbackCalled = false

        // Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(
                    outputFileOptions = fakeOutputFileOptions,
                    onCaptureStarted = { callbackCalled = true }
                )
            }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val imageCaptureCallback = imageCapture.getTakePictureRequest()?.onDiskCallback
        imageCaptureCallback?.onCaptureStarted()

        // Assert.
        assertThat(callbackCalled).isTrue()

        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_canPropagateCaptureProcessProgressed(): Unit = runTest {
        // Arrange.
        var callbackCalled = false
        val progress = 100
        var resultProgress = 0
        FakeAppConfig.getTakePictureManager()!!.disableAutoComplete = true

        // Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(
                    outputFileOptions = fakeOutputFileOptions,
                    onCaptureProcessProgressed = {
                        resultProgress = it
                        callbackCalled = true
                    }
                )
            }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val onImageCaptureCallback = imageCapture.getTakePictureRequest()?.onDiskCallback
        onImageCaptureCallback?.onCaptureProcessProgressed(progress)

        // Assert.
        assertThat(callbackCalled).isTrue()
        assertThat(resultProgress).isEqualTo(progress)

        takePictureAsync.cancel()
    }

    @Test
    fun takePicture_canPropagatePostviewBitmapAvailable(): Unit = runTest {
        // Arrange.
        var callbackCalled = false
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        lateinit var resultBitmap: Bitmap
        FakeAppConfig.getTakePictureManager()!!.disableAutoComplete = true

        // Act.
        val takePictureAsync =
            MainScope().async {
                imageCapture.takePicture(
                    outputFileOptions = fakeOutputFileOptions,
                    onPostviewBitmapAvailable = {
                        resultBitmap = it
                        callbackCalled = true
                    }
                )
            }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val onImageCaptureCallback = imageCapture.getTakePictureRequest()?.onDiskCallback
        onImageCaptureCallback?.onPostviewBitmapAvailable(bitmap)

        // Assert.
        assertThat(callbackCalled).isTrue()
        assertThat(resultBitmap).isSameInstanceAs(bitmap)

        takePictureAsync.cancel()
    }
}
