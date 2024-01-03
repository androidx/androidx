/*
 * Copyright 2022 The Android Open Source Project
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
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val BACK_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
private const val BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK
private const val CAPTURE_TIMEOUT = 15_000.toLong() //  15 seconds

@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureWithoutStoragePermissionTest(
    implName: String,
    private val cameraXConfig: CameraXConfig
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraXConfig)
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val defaultBuilder = ImageCapture.Builder()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fakeLifecycleOwner: FakeLifecycleOwner

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(BACK_LENS_FACING))
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
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }
    }

    @Test
    fun takePictureReturnsError_FILE_IO_whenNotStoragePermissionGranted(): Unit = runBlocking {

        val checkPermissionResult =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        // This test is only for storage permission that is not granted.
        Assume.assumeTrue(checkPermissionResult == PackageManager.PERMISSION_DENIED)

        // Arrange.
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        val callback = FakeImageSavedCallback(capturesCount = 1)

        // Act.
        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Wait for the signal that saving the image has failed
        callback.awaitCapturesAndAssert(errorsCount = 1)

        // Assert.
        val error = callback.errors.first().imageCaptureError
        Truth.assertThat(error).isEqualTo(ImageCapture.ERROR_FILE_IO)
    }

    private fun createDefaultPictureFolderIfNotExist() {
        val pictureFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        if (!pictureFolder.exists()) {
            pictureFolder.mkdir()
        }
    }

    private class FakeImageSavedCallback(capturesCount: Int) :
        ImageCapture.OnImageSavedCallback {

        private val latch = CountdownDeferred(capturesCount)
        val results = mutableListOf<ImageCapture.OutputFileResults>()
        val errors = mutableListOf<ImageCaptureException>()

        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            results.add(outputFileResults)
            latch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            errors.add(exception)
            latch.countDown()
        }

        suspend fun awaitCapturesAndAssert(
            timeout: Long = CAPTURE_TIMEOUT,
            savedImagesCount: Int = 0,
            errorsCount: Int = 0
        ) {
            Truth.assertThat(withTimeoutOrNull(timeout) {
                latch.await()
            }).isNotNull()
            Truth.assertThat(results.size).isEqualTo(savedImagesCount)
            Truth.assertThat(errors.size).isEqualTo(errorsCount)
        }
    }

    private class CountdownDeferred(count: Int) {

        private val deferredItems = mutableListOf<CompletableDeferred<Unit>>().apply {
            repeat(count) { add(CompletableDeferred()) }
        }
        private var index = 0

        fun countDown() {
            deferredItems[index++].complete(Unit)
        }

        suspend fun await() {
            deferredItems.forEach { it.await() }
        }
    }
}
