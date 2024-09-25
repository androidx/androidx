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

package androidx.camera.integration.core.fakecamera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.fakes.FakeAppConfig
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraControl
import androidx.camera.testing.imagecapture.CaptureResult.Companion.successfulResult
import androidx.camera.testing.impl.IgnoreProblematicDeviceRule
import androidx.camera.testing.impl.fakes.FakeOnImageCapturedCallback
import androidx.camera.testing.impl.fakes.FakeOnImageSavedCallback
import androidx.camera.testing.rules.FakeCameraTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests using a fake camera instead of real camera by replacing the camera-camera2 layer with
 * camera-testing layer.
 *
 * They are aimed to ensure that integration between camera-core and camera-testing work seamlessly.
 */
@RunWith(Parameterized::class)
class ImageCaptureTest(
    @CameraSelector.LensFacing private val lensFacing: Int,
) {
    @get:Rule val fakeCameraRule = FakeCameraTestRule(ApplicationProvider.getApplicationContext())

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

    // Required for MediaStore tests on some emulators
    @get:Rule
    val storagePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var camera: FakeCamera
    private lateinit var cameraControl: FakeCameraControl
    private lateinit var imageCapture: ImageCapture

    @Before fun setup() = runBlocking { imageCapture = bindImageCapture() }

    // Duplicate to ImageCaptureTest on core-test-app JVM tests, any change here may need to be
    // reflected there too
    @Test
    fun canSubmitTakePictureRequest(): Unit = runBlocking {
        val countDownLatch = CountDownLatch(1)
        cameraControl.setOnNewCaptureRequestListener { countDownLatch.countDown() }

        imageCapture.takePicture(CameraXExecutors.directExecutor(), FakeOnImageCapturedCallback())

        assertThat(countDownLatch.await(3, TimeUnit.SECONDS)).isTrue()
    }

    // Duplicate to ImageCaptureTest on core-test-app JVM tests, any change here may need to be
    // reflected there too
    @Test
    fun canCreateBitmapFromTakenImage_whenImageCapturedCallbackIsUsed(): Unit = runBlocking {
        assumeFalse(
            "This emulator fails to create a bitmap from an android.media.Image instance" +
                ", the emulator is known to have various issues and generally ignored in our tests",
            IgnoreProblematicDeviceRule.isPixel2Api26Emulator
        )

        val callback = FakeOnImageCapturedCallback(closeImageOnSuccess = false)

        imageCapture.takePicture(CameraXExecutors.directExecutor(), callback)
        cameraControl.submitCaptureResult(successfulResult())

        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
        assertThat(callback.results.first().image.toBitmap()).isNotNull()
    }

    // Duplicate to ImageCaptureTest on core-test-app JVM tests, any change here may need to be
    // reflected there too
    @Test
    fun canFindImage_whenFileStorageAndImageSavedCallbackIsUsed(): Unit = runBlocking {
        val saveLocation = temporaryFolder.newFile()
        val previousLength = saveLocation.length()
        val callback = FakeOnImageSavedCallback()

        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            CameraXExecutors.directExecutor(),
            callback
        )
        cameraControl.submitCaptureResult(successfulResult())

        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
        assertThat(saveLocation.length()).isGreaterThan(previousLength)
    }

    // Duplicate to ImageCaptureTest on androidTest/fakecamera/ImageCaptureTest, any change here may
    // need to be reflected there too
    @Test
    fun canFindImage_whenMediaStoreAndImageSavedCallbackIsUsed(): Unit = runBlocking {
        val initialCount = getMediaStoreCameraXImageCount()
        val callback = FakeOnImageSavedCallback()

        imageCapture.takePicture(
            createMediaStoreOutputOptions(),
            CameraXExecutors.directExecutor(),
            callback
        )
        cameraControl.submitCaptureResult(successfulResult())

        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
        assertThat(getMediaStoreCameraXImageCount()).isEqualTo(initialCount + 1)
    }

    private suspend fun bindImageCapture(): ImageCapture {
        val imageCapture = ImageCapture.Builder().build()

        withContext(Dispatchers.Main) {
            fakeCameraRule.bindUseCases(lensFacing, listOf(imageCapture))
        }

        camera = fakeCameraRule.getFakeCamera(lensFacing)
        cameraControl = camera.cameraControl as FakeCameraControl

        return imageCapture
    }

    private fun getFakeConfigCameraProvider(context: Context): ProcessCameraProvider {
        var cameraProvider: ProcessCameraProvider? = null
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

        Truth.assertWithMessage("ProcessCameraProvider.getInstance timed out!")
            .that(latch.await(5, TimeUnit.SECONDS))
            .isTrue()

        return cameraProvider!!
    }

    private fun createMediaStoreOutputOptions(): ImageCapture.OutputFileOptions {
        // Create time stamped name and MediaStore entry.
        val name =
            FILENAME_PREFIX +
                SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                    .format(System.currentTimeMillis())
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                }
            }

        // Create output options object which contains file + metadata
        return ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
    }

    private fun getMediaStoreCameraXImageCount(): Int {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("$FILENAME_PREFIX%")

        val query =
            ApplicationProvider.getApplicationContext<Context>()
                .contentResolver
                .query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )

        return query?.use { cursor -> cursor.count } ?: 0
    }

    companion object {
        private const val FILENAME_PREFIX = "cameraXPhoto"

        @JvmStatic
        @Parameterized.Parameters(name = "LensFacing = {0}")
        fun data() =
            listOf(
                arrayOf(CameraSelector.LENS_FACING_BACK),
                arrayOf(CameraSelector.LENS_FACING_FRONT),
            )
    }
}
