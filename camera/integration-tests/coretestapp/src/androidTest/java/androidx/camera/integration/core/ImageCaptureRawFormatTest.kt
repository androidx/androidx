/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW
import androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW_JPEG
import androidx.camera.core.ImageCapture.getImageCaptureCapabilities
import androidx.camera.integration.core.ImageCaptureRawFormatTest.CaptureCallback.IN_MEMORY_CALLBACK
import androidx.camera.integration.core.ImageCaptureRawFormatTest.CaptureCallback.ON_DISC_CALLBACK
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.WakelockEmptyActivityRule
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.camera.testing.impl.fakes.FakeOnImageCapturedCallback
import androidx.camera.testing.impl.fakes.FakeOnImageSavedCallback
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureRawFormatTest(implName: String, private val cameraXConfig: CameraXConfig) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val cameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(cameraXConfig)
        )

    @get:Rule
    val externalStorageRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    @get:Rule val wakelockEmptyActivityRule = WakelockEmptyActivityRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner
    private lateinit var cameraSelector: CameraSelector

    @Before
    fun setUp(): Unit = runBlocking {
        CoreAppTestUtil.assumeCompatibleDevice()
        cameraSelector = CameraUtil.assumeFirstAvailableCameraSelector()
        createDefaultPictureFolderIfNotExist()
        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        withContext(Dispatchers.Main) {
            fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) { cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS] }
        }
    }

    @Test
    fun takePicture_withRawOutputFormatAndInMemoryCallback() = runBlocking {
        testImageCapture(OUTPUT_FORMAT_RAW, IN_MEMORY_CALLBACK)
    }

    @Test
    fun takePicture_withRawJpegOutputFormatAndInMemoryCallback() = runBlocking {
        testImageCapture(OUTPUT_FORMAT_RAW_JPEG, IN_MEMORY_CALLBACK)
    }

    @Test
    fun takePicture_withRawOutputFormatAndOnDiscCallback() = runBlocking {
        // RAW image saving on disc does not work in redmi 8
        assumeFalse(Build.DEVICE.equals("olive", ignoreCase = true)) // Redmi 8

        testImageCapture(OUTPUT_FORMAT_RAW, ON_DISC_CALLBACK)
    }

    @Test
    fun takePicture_withRawJpegOutputFormatAndOnDiscCallback() = runBlocking {
        // RAW image saving on disc does not work in redmi 8
        assumeFalse(Build.DEVICE.equals("olive", ignoreCase = true)) // Redmi 8

        testImageCapture(OUTPUT_FORMAT_RAW_JPEG, ON_DISC_CALLBACK)
    }

    private suspend fun testImageCapture(outputFormat: Int, captureCallback: CaptureCallback) {
        val cameraInfo = cameraProvider.getCameraInfo(cameraSelector)
        assumeTrue(
            getImageCaptureCapabilities(cameraInfo).supportedOutputFormats.contains(outputFormat)
        )

        val imageCapture = bindImageCapture(OUTPUT_FORMAT_RAW)

        when (captureCallback) {
            IN_MEMORY_CALLBACK -> imageCapture.verifyInMemoryImageCapture()
            ON_DISC_CALLBACK -> imageCapture.verifyOnDiskImageCapture()
        }
    }

    private suspend fun bindImageCapture(outputFormat: Int): ImageCapture {
        val imageCapture = ImageCapture.Builder().setOutputFormat(outputFormat).build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, imageCapture)
        }

        return imageCapture
    }

    private suspend fun ImageCapture.verifyInMemoryImageCapture() {
        val captureCount = if (outputFormat == OUTPUT_FORMAT_RAW_JPEG) 2 else 1
        val callback = FakeOnImageCapturedCallback(captureCount = captureCount)

        takePicture(mainExecutor, callback)

        // Wait for the signal that the images have been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = captureCount)

        verifyImageFormats(
            capturedImageFormats = callback.results.map { it.properties.format },
            imageCaptureOutputFormat = outputFormat,
        )
    }

    private suspend fun ImageCapture.verifyOnDiskImageCapture() {
        val captureCount = if (outputFormat == OUTPUT_FORMAT_RAW_JPEG) 2 else 1
        val callback = FakeOnImageSavedCallback(captureCount = captureCount)

        val rawOutputFileOptions =
            ImageCapture.OutputFileOptions.Builder(temporaryFolder.newFile("image.dng")).build()

        if (outputFormat == OUTPUT_FORMAT_RAW_JPEG) {
            val jpgOutputFileOptions =
                ImageCapture.OutputFileOptions.Builder(temporaryFolder.newFile("image.jpg")).build()
            takePicture(rawOutputFileOptions, jpgOutputFileOptions, mainExecutor, callback)
        } else {
            takePicture(rawOutputFileOptions, mainExecutor, callback)
        }

        // Wait for the signal that the images have been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = captureCount)

        verifyImageFormats(
            capturedImageFormats = callback.results.map { it.imageFormat },
            imageCaptureOutputFormat = outputFormat,
        )
    }

    private fun verifyImageFormats(capturedImageFormats: List<Int>, imageCaptureOutputFormat: Int) {
        assertThat(capturedImageFormats)
            .containsExactlyElementsIn(
                buildList {
                    add(ImageFormat.RAW_SENSOR)

                    if (imageCaptureOutputFormat == OUTPUT_FORMAT_RAW_JPEG) {
                        add(ImageFormat.JPEG)
                    }
                }
            )
    }

    private enum class CaptureCallback {
        IN_MEMORY_CALLBACK,
        ON_DISC_CALLBACK,
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }
}
