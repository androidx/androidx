/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.MutableOptionsBundle
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.internal.compat.workaround.ExifRotationAvailability
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.ALLOWED_RESOLUTIONS_SLOW
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.SurfaceTextureProvider
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.camera.testing.fakes.FakeSessionProcessor
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private val DEFAULT_RESOLUTION = Size(640, 480)
private val BACK_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
private val FRONT_SELECTOR = CameraSelector.DEFAULT_FRONT_CAMERA
private const val BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK
private const val CAPTURE_TIMEOUT = 10_000.toLong() //  10 seconds

@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraXConfig)
    )

    @get:Rule
    val externalStorageRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @get:Rule
    val temporaryFolder =
        TemporaryFolder(ApplicationProvider.getApplicationContext<Context>().cacheDir)

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
        assumeTrue(CameraUtil.hasCameraWithLensFacing(BACK_LENS_FACING))
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

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectSize() = runBlocking {
        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .setTargetRotation(Surface.ROTATION_0)
            .build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()
        var sizeEnvelope = imageProperties.size

        // Some devices may not be able to fit the requested resolution. In this case, the returned
        // size should be able to enclose 640 x 480.
        if (sizeEnvelope != DEFAULT_RESOLUTION) {
            val rotationDegrees = imageProperties.rotationDegrees

            // If the image data is rotated by 90 or 270, we need to ensure our desired width fits
            // within the height of this image and our desired height fits in the width.
            if (rotationDegrees == 270 || rotationDegrees == 90) {
                sizeEnvelope = Size(sizeEnvelope!!.height, sizeEnvelope.width)
            }

            // Ensure the width and height can be cropped from the source image
            assertThat(sizeEnvelope!!.width).isAtLeast(DEFAULT_RESOLUTION.width)
            assertThat(sizeEnvelope.height).isAtLeast(DEFAULT_RESOLUTION.height)
        }
    }

    @Test
    fun canCaptureMultipleImages() {
        canTakeImages(defaultBuilder, numImages = 5)
    }

    @Test
    fun canCaptureMultipleImagesWithMaxQuality() {
        canTakeImages(
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY),
            numImages = 5
        )
    }

    @Test
    fun canCaptureMultipleImagesWithZsl() = runBlocking {
        val useCase = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG).build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        if (camera.cameraInfo.isZslSupported) {
            val numImages = 5
            val callback = FakeImageCaptureCallback(capturesCount = numImages)
            for (i in 0 until numImages) {
                useCase.takePicture(mainExecutor, callback)
            }

            callback.awaitCapturesAndAssert(
                timeout = numImages * CAPTURE_TIMEOUT,
                capturedImagesCount = numImages
            )
        }
    }

    @Test
    fun canCaptureImageWithFlashModeOn() {
        canTakeImages(defaultBuilder.setFlashMode(ImageCapture.FLASH_MODE_ON))
    }

    @Test
    fun canCaptureImageWithFlashModeOn_frontCamera() {
        // This test also wants to ensure that the image can be captured without the flash unit.
        // Front camera usually doesn't have a flash unit.
        canTakeImages(
            defaultBuilder.setFlashMode(ImageCapture.FLASH_MODE_ON),
            cameraSelector = FRONT_SELECTOR
        )
    }

    @Test
    fun canCaptureImageWithFlashModeOnAndUseTorch() {
        canTakeImages(
            defaultBuilder.setFlashType(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
                .setFlashMode(ImageCapture.FLASH_MODE_ON),
        )
    }

    @Test
    fun canCaptureImageWithFlashModeOnAndUseTorch_frontCamera() {
        // This test also wants to ensure that the image can be captured without the flash unit.
        // Front camera usually doesn't have a flash unit.
        canTakeImages(
            defaultBuilder.setFlashType(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH)
                .setFlashMode(ImageCapture.FLASH_MODE_ON),
            cameraSelector = FRONT_SELECTOR
        )
    }

    private fun canTakeImages(
        builder: ImageCapture.Builder,
        cameraSelector: CameraSelector = BACK_SELECTOR,
        numImages: Int = 1,
    ): Unit = runBlocking {
        val useCase = builder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, cameraSelector, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = numImages)
        repeat(numImages) {
            useCase.takePicture(mainExecutor, callback)
        }

        callback.awaitCapturesAndAssert(
            timeout = numImages * CAPTURE_TIMEOUT,
            capturedImagesCount = numImages
        )
    }

    @Test
    fun saveCanSucceed_withNonExistingFile() {
        val saveLocation = temporaryFolder.newFile("test${System.currentTimeMillis()}.jpg")

        // make sure file does not exist
        if (saveLocation.exists()) {
            saveLocation.delete()
        }
        assertThat(!saveLocation.exists())

        canSaveToFile(saveLocation)
    }

    @Test
    fun saveCanSucceed_withExistingFile() {
        val saveLocation = temporaryFolder.newFile("test.jpg")
        assertThat(saveLocation.exists())

        canSaveToFile(saveLocation)
    }

    @Test
    fun saveCanSucceed_toExternalStoragePublicFolderFile() {
        val pictureFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        assumeTrue(pictureFolder.exists())
        val saveLocation = File(pictureFolder, "test.jpg")
        canSaveToFile(saveLocation)
        saveLocation.delete()
    }

    private fun canSaveToFile(saveLocation: File) = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageSavedCallback(capturesCount = 1)
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)
    }

    @Test
    fun saveToUri(): Unit = runBlocking {
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

        // Assert: Wait for the signal that the image has been saved
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Verify save location Uri is available.
        val saveLocationUri = callback.results.first().savedUri
        assertThat(saveLocationUri).isNotNull()

        // Clean up.
        context.contentResolver.delete(saveLocationUri!!, null, null)
    }

    @Test
    fun saveToOutputStream() = runBlocking {
        // Arrange.
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        FileOutputStream(saveLocation).use { outputStream ->
            // Act.
            useCase.takePicture(
                ImageCapture.OutputFileOptions.Builder(outputStream).build(),
                mainExecutor,
                callback
            )

            // Assert: Wait for the signal that the image has been saved.
            callback.awaitCapturesAndAssert(savedImagesCount = 1)
        }
    }

    @Test
    fun canSaveFile_withRotation() = runBlocking {
        // TODO(b/147448711) Add back in once cuttlefish has correct user cropping functionality.
        assumeFalse(
            "Cuttlefish does not correctly handle crops. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        val useCase = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image
        val exif = Exif.createFromFile(saveLocation)

        val saveLocationRotated90 = temporaryFolder.newFile("testRotated90.jpg")

        val callbackRotated90 = FakeImageSavedCallback(capturesCount = 1)

        useCase.targetRotation = Surface.ROTATION_90
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocationRotated90).build(),
            mainExecutor,
            callbackRotated90
        )

        // Wait for the signal that the image has been saved.
        callbackRotated90.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image
        val exifRotated90 = Exif.createFromFile(saveLocationRotated90)

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the rotated capture is scaled to fit within
        // the sensor region
        val aspectRatioThreshold = 0.01

        // If rotation is equal then buffers were rotated by HAL so the aspect ratio should be
        // rotated by 90 degrees. Otherwise the aspect ratio should be the same.
        if (exif.rotation == exifRotated90.rotation) {
            val aspectRatio = exif.height.toDouble() / exif.width
            val aspectRatioRotated90 = exifRotated90.width.toDouble() / exifRotated90.height
            assertThat(abs(aspectRatio - aspectRatioRotated90)).isLessThan(aspectRatioThreshold)
        } else {
            val aspectRatio = exif.width.toDouble() / exif.height
            val aspectRatioRotated90 = exifRotated90.width.toDouble() / exifRotated90.height
            assertThat(abs(aspectRatio - aspectRatioRotated90)).isLessThan(aspectRatioThreshold)
        }
    }

    @Test
    fun canSaveFile_flippedHorizontal() = runBlocking {
        // Use a non-rotated configuration since some combinations of rotation + flipping vertically
        // can be equivalent to flipping horizontally
        val configBuilder = ImageCapture.Builder.fromConfig(createNonRotatedConfiguration())

        val metadata = ImageCapture.Metadata()
        metadata.isReversedHorizontal = true

        canSaveFileWithMetadata(
            configBuilder = configBuilder,
            metadata = metadata,
            verifyExif = { exif ->
                assertThat(exif.isFlippedHorizontally).isTrue()
            }
        )
    }

    @Test
    fun canSaveFile_flippedVertical() = runBlocking {
        // Use a non-rotated configuration since some combinations of rotation + flipping
        // horizontally can be equivalent to flipping vertically
        val configBuilder = ImageCapture.Builder.fromConfig(createNonRotatedConfiguration())

        val metadata = ImageCapture.Metadata()
        metadata.isReversedVertical = true

        canSaveFileWithMetadata(
            configBuilder = configBuilder,
            metadata = metadata,
            verifyExif = { exif ->
                assertThat(exif.isFlippedVertically).isTrue()
            }
        )
    }

    // See b/263289024, writing location data might cause the output JPEG image corruption on some
    // specific Android 12 devices. This issue happens if:
    // 1. The image is not cropped from the original captured image (unnecessary Exif copy is done)
    // 2. The inserted location provider is FUSED_PROVIDER
    @Test
    fun canSaveFile_withFusedProviderLocation() {
        val latitudeValue = 50.0
        val longitudeValue = -100.0

        val metadata = ImageCapture.Metadata().apply {
            location = Location(LocationManager.FUSED_PROVIDER).apply {
                latitude = latitudeValue
                longitude = longitudeValue
            }
        }

        canSaveFileWithMetadata(
            defaultBuilder,
            metadata,
            verifyExif = { exif ->
                assertThat(exif.location).isNotNull()
                assertThat(exif.location!!.provider).isEqualTo(metadata.location!!.provider)
                assertThat(exif.location!!.latitude).isEqualTo(latitudeValue)
                assertThat(exif.location!!.longitude).isEqualTo(longitudeValue)
            }
        )
    }

    private fun canSaveFileWithMetadata(
        configBuilder: ImageCapture.Builder,
        metadata: ImageCapture.Metadata,
        verifyExif: (Exif) -> Unit
    ) = runBlocking {
        val useCase = configBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions
            .Builder(saveLocation)
            .setMetadata(metadata)
            .build()

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // Retrieve the exif from the image
        val exif = Exif.createFromFile(saveLocation)
        verifyExif(exif)
    }

    @Test
    fun canSaveMultipleFiles() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val numImages = 5
        val callback = FakeImageSavedCallback(capturesCount = numImages)

        for (i in 0 until numImages) {
            val saveLocation = temporaryFolder.newFile("test$i.jpg")
            useCase.takePicture(
                ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
                mainExecutor,
                callback
            )
        }

        // Wait for the signal that all the images have been saved.
        callback.awaitCapturesAndAssert(
            timeout = numImages * CAPTURE_TIMEOUT,
            savedImagesCount = numImages
        )
    }

    @Test
    fun saveWillFail_whenInvalidFilePathIsUsed() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        // Note the invalid path
        val saveLocation = File("/not/a/real/path.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(saveLocation).build()

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(outputFileOptions, mainExecutor, callback)

        // Wait for the signal that saving the image has failed
        callback.awaitCapturesAndAssert(errorsCount = 1)

        val error = callback.errors.first().imageCaptureError
        assertThat(error).isEqualTo(ImageCapture.ERROR_FILE_IO)
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @Test
    @OptIn(
        markerClass = [ExperimentalCamera2Interop::class]
    )
    fun camera2InteropCaptureSessionCallbacks() = runBlocking {
        val stillCaptureCount = AtomicInteger(0)
        val captureCallback = object : CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                if (request.get(CaptureRequest.CONTROL_CAPTURE_INTENT) ==
                    CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE
                ) {
                    stillCaptureCount.incrementAndGet()
                }
            }
        }
        val builder = ImageCapture.Builder()
        CameraPipeUtil.setCameraCaptureSessionCallback(implName, builder, captureCallback)

        val useCase = builder.build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)

        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // Because interop listener will get both image capture and preview callbacks, ensure
        // that there is one CAPTURE_INTENT_STILL_CAPTURE from all onCaptureCompleted() callbacks.
        assertThat(stillCaptureCount.get()).isEqualTo(1)
    }

    @Test
    fun takePicture_withBufferFormatRaw10() = runBlocking {
        // RAW10 does not work in redmi 8
        assumeFalse(Build.DEVICE.equals("olive", ignoreCase = true)) // Redmi 8
        val cameraCharacteristics = CameraUtil.getCameraCharacteristics(BACK_LENS_FACING)
        val map = cameraCharacteristics!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val resolutions = map!!.getOutputSizes(ImageFormat.RAW10)

        // Ignore this tests on devices that do not support RAW10 image format.
        assumeNotNull(resolutions)
        assumeTrue(resolutions!!.isNotEmpty())
        assumeTrue(isRawSupported(cameraCharacteristics))

        val useCase = ImageCapture.Builder()
            .setBufferFormat(ImageFormat.RAW10)
            .build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)

        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()
        assertThat(imageProperties.format).isEqualTo(ImageFormat.RAW10)
    }

    private fun isRawSupported(cameraCharacteristics: CameraCharacteristics): Boolean {
        val capabilities =
            cameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                ?: IntArray(0)
        return capabilities.any { capability ->
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW == capability
        }
    }

    @SdkSuppress(minSdkVersion = 28)
    @Test(expected = IllegalArgumentException::class)
    fun constructor_withBufferFormatAndSessionProcessorIsSet_throwsException(): Unit = runBlocking {
        val sessionProcessor = FakeSessionProcessor(
            inputFormatPreview = null, // null means using the same output surface
            inputFormatCapture = ImageFormat.YUV_420_888
        )

        val imageCapture = ImageCapture.Builder()
            .setBufferFormat(ImageFormat.RAW_SENSOR)
            .build()
        val preview = Preview.Builder().build()
        withContext(Dispatchers.Main) {
            val cameraSelector =
                getCameraSelectorWithSessionProcessor(BACK_SELECTOR, sessionProcessor)
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner, cameraSelector, imageCapture, preview)
        }
    }

    @Test
    fun onStateOffline_abortAllCaptureRequests() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        // After the use case can be reused, the capture requests can only be cancelled after the
        // onStateAttached() callback has been received. In the normal code flow, the
        // onStateDetached() should also come after onStateAttached(). There is no API to
        // directly know  onStateAttached() callback has been received. Therefore, taking a
        // picture and waiting for the capture success callback to know the use case's
        // onStateAttached() callback has been received.
        val callback = FakeImageCaptureCallback(capturesCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val callback2 = FakeImageCaptureCallback(capturesCount = 3)
        imageCapture.takePicture(mainExecutor, callback2)
        imageCapture.takePicture(mainExecutor, callback2)
        imageCapture.takePicture(mainExecutor, callback2)

        withContext(Dispatchers.Main) {
            imageCapture.onStateDetached()
        }

        callback2.awaitCaptures()
        assertThat(callback2.results.size + callback2.errors.size).isEqualTo(3)
        assertThat(callback2.errors.size).isAtLeast(1)

        for (error in callback2.errors) {
            assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
        }
    }

    @Test
    fun unbind_abortAllCaptureRequests() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 3)
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)

        // Needs to run on main thread because takePicture gets posted on main thread if it isn't
        // running on the main thread. Which means the internal ImageRequests likely get issued
        // after ImageCapture is removed so errors out with a different error from
        // ERROR_CAMERA_CLOSED
        withContext(Dispatchers.Main) {
            cameraProvider.unbind(imageCapture)
        }

        // Wait for the signal that the image capture has failed.
        callback.awaitCapturesAndAssert(errorsCount = 3)

        assertThat(callback.results.size + callback.errors.size).isEqualTo(3)
        for (error in callback.errors) {
            assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
        }
    }

    @Test
    fun takePictureReturnsErrorNO_CAMERA_whenNotBound() = runBlocking {
        val imageCapture = ImageCapture.Builder().build()
        val callback = FakeImageCaptureCallback(capturesCount = 1)

        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image capture has failed.
        callback.awaitCapturesAndAssert(errorsCount = 1)

        val error = callback.errors.first()
        assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_INVALID_CAMERA)
    }

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() = runBlocking {
        val useCase = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun defaultAspectRatioWillBeSet_whenRatioDefaultIsSet() = runBlocking {
        val useCase = ImageCapture.Builder().setTargetAspectRatio(AspectRatio.RATIO_DEFAULT).build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Suppress("DEPRECATION") // legacy resolution API
    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() = runBlocking {
        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .build()

        assertThat(
            useCase.currentConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
            )
        ).isFalse()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        assertThat(
            useCase.currentConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
            )
        ).isFalse()
    }

    @Test
    fun targetRotationCanBeUpdatedAfterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        imageCapture.targetRotation = Surface.ROTATION_90
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_90)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun targetResolutionIsUpdatedAfterTargetRotationIsUpdated() = runBlocking {
        val imageCapture = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageCapture.targetRotation = Surface.ROTATION_90

        val newConfig = imageCapture.currentConfig as ImageOutputConfig
        val expectedTargetResolution = Size(DEFAULT_RESOLUTION.height, DEFAULT_RESOLUTION.width)

        // Expected targetResolution will be reversed from original target resolution.
        assertThat(newConfig.targetResolution).isEqualTo(expectedTargetResolution)
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectCroppingSizeWithoutSettingRotation() {
        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .build()

        capturedImageHasCorrectCroppingSize(
            useCase,
            rotateCropRect = { capturedImageRotationDegrees ->
                capturedImageRotationDegrees % 180 != 0
            }
        )
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectCroppingSizeSetRotationBuilder() {
        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // that the initial set target cropping aspect ratio matches the sensor orientation.
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()

        capturedImageHasCorrectCroppingSize(
            useCase,
            rotateCropRect = { capturedImageRotationDegrees ->
                capturedImageRotationDegrees % 180 != 0
            }
        )
    }

    @Suppress("DEPRECATION") // test for legacy resolution API
    @Test
    fun capturedImageHasCorrectCroppingSize_setUseCaseRotation90FromRotationInBuilder() {
        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // that the initial set target cropping aspect ratio matches the sensor orientation.
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()

        // Updates target rotation to opposite one.
        useCase.targetRotation = if (isRotateNeeded) Surface.ROTATION_0 else Surface.ROTATION_90

        capturedImageHasCorrectCroppingSize(
            useCase,
            rotateCropRect = { capturedImageRotationDegrees ->
                capturedImageRotationDegrees % 180 == 0
            }
        )
    }

    private fun capturedImageHasCorrectCroppingSize(
        useCase: ImageCapture,
        rotateCropRect: (Int) -> Boolean
    ) = runBlocking {
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // After target rotation is updated, the result cropping aspect ratio should still the
        // same as original one.
        val expectedCroppingRatio = Rational(DEFAULT_RESOLUTION.width, DEFAULT_RESOLUTION.height)

        val imageProperties = callback.results.first()
        val cropRect = imageProperties.cropRect

        // Rotate the captured ImageProxy's crop rect into the coordinate space of the final
        // displayed image
        val resultCroppingRatio: Rational = if (rotateCropRect(imageProperties.rotationDegrees)) {
            Rational(cropRect!!.height(), cropRect.width())
        } else {
            Rational(cropRect!!.width(), cropRect.height())
        }

        assertThat(resultCroppingRatio).isEqualTo(expectedCroppingRatio)
        if (imageProperties.format == ImageFormat.JPEG && isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }
    }

    @Test
    fun capturedImageHasCorrectCroppingSize_setCropAspectRatioAfterBindToLifecycle() = runBlocking {
        val useCase = ImageCapture.Builder().build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)

        // Checks camera device sensor degrees to set target cropping aspect ratio match the
        // sensor orientation.
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0

        // Set the default aspect ratio of ImageCapture to the target cropping aspect ratio.
        val targetCroppingAspectRatio = if (isRotateNeeded) Rational(3, 4) else Rational(4, 3)

        useCase.setCropAspectRatio(targetCroppingAspectRatio)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // After target rotation is updated, the result cropping aspect ratio should still the
        // same as original one.
        val imageProperties = callback.results.first()
        val cropRect = imageProperties.cropRect

        // Rotate the captured ImageProxy's crop rect into the coordinate space of the final
        // displayed image
        val resultCroppingRatio: Rational = if (imageProperties.rotationDegrees % 180 != 0) {
            Rational(cropRect!!.height(), cropRect.width())
        } else {
            Rational(cropRect!!.width(), cropRect.height())
        }

        if (imageProperties.format == ImageFormat.JPEG && isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(
                imageProperties.exif!!.rotation
            )
        }

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the target aspect ratio of ImageCapture will
        // be corrected in API 21 Legacy devices and the captured image will be scaled to fit
        // within the cropping aspect ratio.
        val aspectRatioThreshold = 0.01
        assertThat(
            abs(resultCroppingRatio.toDouble() - targetCroppingAspectRatio.toDouble())
        ).isLessThan(aspectRatioThreshold)
    }

    @Test
    fun capturedImageHasCorrectCroppingSize_viewPortOverwriteCropAspectRatio() = runBlocking {
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0

        val useCase = ImageCapture.Builder()
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()

        // Sets a crop aspect ratio to the use case. This will be overwritten by the view port
        // setting.
        val useCaseCroppingAspectRatio = Rational(4, 3)
        useCase.setCropAspectRatio(useCaseCroppingAspectRatio)

        // Sets view port with different aspect ratio and then attach the use case
        val viewPortAspectRatio = Rational(2, 1)
        val viewPort = ViewPort.Builder(
            viewPortAspectRatio,
            if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0
        ).build()

        val useCaseGroup = UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(useCase).build()

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCaseGroup)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)

        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // After target rotation is updated, the result cropping aspect ratio should still the
        // same as original one.
        val imageProperties = callback.results.first()
        val cropRect = imageProperties.cropRect

        // Rotate the captured ImageProxy's crop rect into the coordinate space of the final
        // displayed image
        val resultCroppingRatio: Rational = if (imageProperties.rotationDegrees % 180 != 0) {
            Rational(cropRect!!.height(), cropRect.width())
        } else {
            Rational(cropRect!!.width(), cropRect.height())
        }

        if (imageProperties.format == ImageFormat.JPEG && isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(
                imageProperties.exif!!.rotation
            )
        }

        // Compare aspect ratio with a threshold due to floating point rounding. Can't do direct
        // comparison of height and width, because the target aspect ratio of ImageCapture will
        // be corrected in API 21 Legacy devices and the captured image will be scaled to fit
        // within the cropping aspect ratio.
        val aspectRatioThreshold = 0.01
        assertThat(
            abs(resultCroppingRatio.toDouble() - viewPortAspectRatio.toDouble())
        ).isLessThan(aspectRatioThreshold)
    }

    @Test
    fun useCaseConfigCanBeReset_afterUnbind() = runBlocking {
        val useCase = defaultBuilder.build()
        val initialConfig = useCase.currentConfig
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        withContext(Dispatchers.Main) {
            cameraProvider.unbind(useCase)
        }

        val configAfterUnbinding = useCase.currentConfig
        assertThat(initialConfig == configAfterUnbinding).isTrue()
    }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.targetRotation = Surface.ROTATION_180
        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Check the target rotation is kept when the use case is unbound.
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun cropAspectRatioIsRetained_whenUseCaseIsReused() = runBlocking {
        val useCase = defaultBuilder.build()
        val cropAspectRatio = Rational(1, 1)
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        useCase.setCropAspectRatio(cropAspectRatio)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Rebind the use case.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()
        val cropRect = imageProperties.cropRect
        val cropRectAspectRatio = Rational(cropRect!!.height(), cropRect.width())

        // The crop aspect ratio could be kept after the use case is reused. So that the aspect
        // of the result cropRect is 1:1.
        assertThat(cropRectAspectRatio).isEqualTo(cropAspectRatio)
    }

    @Test
    fun useCaseCanBeReusedInSameCamera() = runBlocking {
        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation1 = temporaryFolder.newFile("test1.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation1).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Rebind the use case to the same camera.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation2 = temporaryFolder.newFile("test2.jpg")

        val callback2 = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation2).build(),
            mainExecutor,
            callback2
        )

        // Wait for the signal that the image has been saved.
        callback2.awaitCapturesAndAssert(savedImagesCount = 1)
    }

    @Test
    fun useCaseCanBeReusedInDifferentCamera() = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT))

        val useCase = defaultBuilder.build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation1 = temporaryFolder.newFile("test1.jpg")

        val callback = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation1).build(),
            mainExecutor,
            callback
        )

        // Wait for the signal that the image has been saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            cameraProvider.unbind(useCase)
        }

        // Rebind the use case to different camera.
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                useCase
            )
        }

        val saveLocation2 = temporaryFolder.newFile("test2.jpg")

        val callback2 = FakeImageSavedCallback(capturesCount = 1)

        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation2).build(),
            mainExecutor,
            callback2
        )

        // Wait for the signal that the image has been saved.
        callback2.awaitCapturesAndAssert(savedImagesCount = 1)
    }

    @Test
    fun returnValidTargetRotation_afterUseCaseIsCreated() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.targetRotation).isNotEqualTo(ImageOutputConfig.INVALID_ROTATION)
    }

    @Test
    fun returnCorrectTargetRotation_afterUseCaseIsAttached() = runBlocking {
        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_180)
            .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun returnDefaultFlashMode_beforeUseCaseIsAttached() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_OFF)
    }

    @Test
    fun returnCorrectFlashMode_afterUseCaseIsAttached() = runBlocking {
        val imageCapture = ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_ON)
            .build()
        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, imageCapture)
        }
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_ON)
    }

    @Test
    fun returnJpegImage_whenSoftwareJpegIsEnabled() = runBlocking {
        val builder = ImageCapture.Builder()

        // Enables software Jpeg
        builder.mutableConfig.insertOption(
            ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER,
            true
        )

        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()

        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees).isEqualTo(
            camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation)
        )

        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun canSaveJpegFileWithRotation_whenSoftwareJpegIsEnabled() = runBlocking {
        val builder = ImageCapture.Builder()

        // Enables software Jpeg
        builder.mutableConfig.insertOption(
            ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER,
            true
        )
        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val saveLocation = temporaryFolder.newFile("test.jpg")
        val callback = FakeImageSavedCallback(capturesCount = 1)
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(saveLocation).build(),
            mainExecutor, callback)

        // Wait for the signal that the image has been captured and saved.
        callback.awaitCapturesAndAssert(savedImagesCount = 1)

        // For YUV to JPEG case, the rotation will only be in Exif.
        val exif = Exif.createFromFile(saveLocation)
        assertThat(exif.rotation).isEqualTo(
            camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation))
    }

    @Test
    fun returnYuvImage_withYuvBufferFormat() = runBlocking {
        val builder = ImageCapture.Builder().setBufferFormat(ImageFormat.YUV_420_888)
        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()
        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees).isEqualTo(
            camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation)
        )
        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    fun returnYuvImage_whenSoftwareJpegIsEnabledWithYuvBufferFormat() = runBlocking {
        val builder = ImageCapture.Builder().setBufferFormat(ImageFormat.YUV_420_888)

        // Enables software Jpeg
        builder.mutableConfig.insertOption(
            ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER,
            true
        )

        val useCase = builder.build()
        var camera: Camera
        withContext(Dispatchers.Main) {
            camera = cameraProvider.bindToLifecycle(fakeLifecycleOwner, BACK_SELECTOR, useCase)
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()

        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees).isEqualTo(
            camera.cameraInfo.getSensorRotationDegrees(useCase.targetRotation)
        )
        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun returnJpegImage_whenSessionProcessorIsSet() = runBlocking {
        assumeTrue(
            "TODO(b/275493663): Enable when camera-pipe has extensions support",
            implName != CameraPipeConfig::class.simpleName
        )

        val builder = ImageCapture.Builder()
        val sessionProcessor = FakeSessionProcessor(
            inputFormatPreview = null, // null means using the same output surface
            inputFormatCapture = ImageFormat.YUV_420_888
        )

        val imageCapture = builder.build()
        val preview = Preview.Builder().build()

        var camera: Camera
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            val cameraSelector =
                getCameraSelectorWithSessionProcessor(BACK_SELECTOR, sessionProcessor)
            camera = cameraProvider.bindToLifecycle(
                fakeLifecycleOwner, cameraSelector, imageCapture, preview
            )
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()

        // Check the output image rotation degrees value is correct.
        assertThat(imageProperties.rotationDegrees).isEqualTo(
            camera.cameraInfo.getSensorRotationDegrees(imageCapture.targetRotation)
        )
        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.JPEG)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun returnJpegImage_whenSessionProcessorIsSet_outputFormatJpeg() = runBlocking {
        assumeFalse(
            "Cuttlefish does not correctly handle Jpeg exif. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        val sessionProcessor = FakeSessionProcessor(
            inputFormatPreview = null, // null means using the same output surface
            inputFormatCapture = null
        )

        val imageCapture = ImageCapture.Builder().build()
        val preview = Preview.Builder().build()

        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            val cameraSelector =
                getCameraSelectorWithSessionProcessor(BACK_SELECTOR, sessionProcessor)
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner, cameraSelector, imageCapture, preview
            )
        }

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()

        // Check the output image rotation degrees value is correct.
        if (isRotationOptionSupportedDevice()) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }

        // Check the output format is correct.
        assertThat(imageProperties.format).isEqualTo(ImageFormat.JPEG)
    }

    private fun getCameraSelectorWithSessionProcessor(
        cameraSelector: CameraSelector,
        sessionProcessor: SessionProcessor
    ): CameraSelector {
        val identifier = Identifier.create("idStr")
        ExtendedCameraConfigProviderStore.addConfig(identifier) { _, _ ->
            object : CameraConfig {
                override fun getConfig(): Config {
                    return MutableOptionsBundle.create()
                }

                override fun getCompatibilityId(): Identifier {
                    return Identifier.create(0)
                }

                override fun getSessionProcessor(
                    valueIfMissing: SessionProcessor?
                ): SessionProcessor? {
                    return sessionProcessor
                }

                override fun getSessionProcessor(): SessionProcessor {
                    return sessionProcessor
                }
            }
        }

        val builder = CameraSelector.Builder.fromSelector(cameraSelector)
        builder.addCameraFilter(object : CameraFilter {
            override fun filter(cameraInfos: MutableList<CameraInfo>): MutableList<CameraInfo> {
                val newCameraInfos = mutableListOf<CameraInfo>()
                newCameraInfos.addAll(cameraInfos)
                return newCameraInfos
            }

            override fun getIdentifier(): Identifier {
                return identifier
            }
        })

        return builder.build()
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @Test
    fun unbindPreview_imageCapturingShouldSuccess() = runBlocking {
        // Arrange.
        val imageCapture = ImageCapture.Builder().build()
        val previewStreamReceived = CompletableDeferred<Boolean>()
        val preview = Preview.Builder().also {
            CameraPipeUtil.setCameraCaptureSessionCallback(
                implName,
                it,
                object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        previewStreamReceived.complete(true)
                    }
                })
        }.build()
        withContext(Dispatchers.Main) {
            preview.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner, BACK_SELECTOR, imageCapture, preview
            )
        }
        assertWithMessage("Preview doesn't start").that(
            previewStreamReceived.awaitWithTimeoutOrNull()
        ).isTrue()

        // Act.
        val callback = FakeImageCaptureCallback(capturesCount = 1)
        withContext(Dispatchers.Main) {
            // Test the reproduce step in b/235119898
            cameraProvider.unbind(preview)
            imageCapture.takePicture(mainExecutor, callback)
        }

        // Assert.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)
    }

    @Test
    fun capturedImage_withHighResolutionEnabled_imageCaptureOnly() = runBlocking {
        capturedImage_withHighResolutionEnabled()
    }

    @Test
    fun capturedImage_withHighResolutionEnabled_imageCapturePreviewImageAnalysis() = runBlocking {
        val preview = Preview.Builder().build().also {
            withContext(Dispatchers.Main) {
                it.setSurfaceProvider(SurfaceTextureProvider.createSurfaceTextureProvider())
            }
        }
        val imageAnalysis = ImageAnalysis.Builder().build().also { imageAnalysis ->
            imageAnalysis.setAnalyzer(Dispatchers.Default.asExecutor()) { imageProxy ->
                imageProxy.close()
            }
        }
        capturedImage_withHighResolutionEnabled(preview, imageAnalysis)
    }

    private fun capturedImage_withHighResolutionEnabled(
        preview: Preview? = null,
        imageAnalysis: ImageAnalysis? = null
    ) = runBlocking {
        // TODO(b/247492645) Remove camera-pipe-integration restriction after porting
        //  ResolutionSelector logic
        assumeTrue(implName != CameraPipeConfig::class.simpleName)

        val maxHighResolutionOutputSize = CameraUtil.getMaxHighResolutionOutputSizeWithLensFacing(
            BACK_SELECTOR.lensFacing!!,
            ImageFormat.JPEG
        )
        // Only runs the test when the device has high resolution output sizes
        assumeTrue(maxHighResolutionOutputSize != null)

        val resolutionSelector = ResolutionSelector.Builder()
            .setAllowedResolutionMode(ALLOWED_RESOLUTIONS_SLOW)
            .setResolutionFilter { _, _ ->
                listOf(maxHighResolutionOutputSize)
            }
            .build()
        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_SELECTOR.lensFacing!!)
        // Sets the target rotation to the camera sensor orientation to avoid the captured image
        // buffer data rotated by the HAL and impact the final image resolution check
        val targetRotation = if (sensorOrientation!! % 180 == 0) {
            Surface.ROTATION_0
        } else {
            Surface.ROTATION_90
        }
        val imageCapture = ImageCapture.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(targetRotation)
            .build()

        val useCases = arrayListOf<UseCase>(imageCapture)
        preview?.let { useCases.add(it) }
        imageAnalysis?.let { useCases.add(it) }

        withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                BACK_SELECTOR,
                *useCases.toTypedArray()
            )
        }

        assertThat(imageCapture.resolutionInfo!!.resolution).isEqualTo(maxHighResolutionOutputSize)

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()
        assertThat(imageProperties.size).isEqualTo(maxHighResolutionOutputSize)
    }

    private fun createNonRotatedConfiguration(): ImageCaptureConfig {
        // Create a configuration with target rotation that matches the sensor rotation.
        // This assumes a back-facing camera (facing away from screen)
        val sensorRotation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val surfaceRotation = CameraOrientationUtil.degreesToSurfaceRotation(
            sensorRotation!!
        )
        return ImageCapture.Builder()
            .setTargetRotation(surfaceRotation)
            .useCaseConfig
    }

    @Suppress("DEPRECATION")
    private fun createDefaultPictureFolderIfNotExist() {
        val pictureFolder = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        if (!pictureFolder.exists()) {
            pictureFolder.mkdir()
        }
    }

    /**
     * See ImageCaptureRotationOptionQuirk. Some real devices or emulator do not support the
     * capture rotation option correctly. The capture rotation option setting can't be correctly
     * applied to the exif metadata of the captured images. Therefore, the exif rotation related
     * verification in the tests needs to be ignored on these devices or emulator.
     */
    private fun isRotationOptionSupportedDevice() =
        ExifRotationAvailability().isRotationOptionSupported

    private class ImageProperties(
        val size: Size? = null,
        val format: Int = -1,
        val rotationDegrees: Int = -1,
        val cropRect: Rect? = null,
        val exif: Exif? = null,
    )

    private class FakeImageCaptureCallback(capturesCount: Int) :
        ImageCapture.OnImageCapturedCallback() {

        private val latch = CountdownDeferred(capturesCount)
        val results = mutableListOf<ImageProperties>()
        val errors = mutableListOf<ImageCaptureException>()

        override fun onCaptureSuccess(image: ImageProxy) {
            results.add(
                ImageProperties(
                    size = Size(image.width, image.height),
                    format = image.format,
                    rotationDegrees = image.imageInfo.rotationDegrees,
                    cropRect = image.cropRect,
                    exif = getExif(image),
                )
            )
            image.close()
            latch.countDown()
        }

        override fun onError(exception: ImageCaptureException) {
            errors.add(exception)
            latch.countDown()
        }

        private fun getExif(image: ImageProxy): Exif? {
            if (image.format == ImageFormat.JPEG) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val data = ByteArray(buffer.capacity())
                buffer[data]
                return Exif.createFromInputStream(ByteArrayInputStream(data))
            }
            return null
        }

        suspend fun awaitCaptures(timeout: Long = CAPTURE_TIMEOUT) {
            assertThat(withTimeoutOrNull(timeout) {
                latch.await()
            }).isNotNull()
        }

        suspend fun awaitCapturesAndAssert(
            timeout: Long = CAPTURE_TIMEOUT,
            capturedImagesCount: Int = 0,
            errorsCount: Int = 0
        ) {
            assertThat(withTimeoutOrNull(timeout) {
                latch.await()
            }).isNotNull()
            assertThat(results.size).isEqualTo(capturedImagesCount)
            assertThat(errors.size).isEqualTo(errorsCount)
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
            assertThat(withTimeoutOrNull(timeout) {
                latch.await()
            }).isNotNull()
            assertThat(results.size).isEqualTo(savedImagesCount)
            assertThat(errors.size).isEqualTo(errorsCount)
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

    private suspend fun <T> Deferred<T>.awaitWithTimeoutOrNull(
        timeMillis: Long = TimeUnit.SECONDS.toMillis(5)
    ): T? {
        return withTimeoutOrNull(timeMillis) {
            await()
        }
    }
}
