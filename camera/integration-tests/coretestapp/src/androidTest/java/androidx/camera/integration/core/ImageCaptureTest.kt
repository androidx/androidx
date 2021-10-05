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
import android.media.ImageWriter
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Rational
import android.util.Size
import android.view.Surface
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.ViewPort
import androidx.camera.core.impl.CaptureBundle
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.CaptureProcessor
import androidx.camera.core.impl.CaptureStage
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageOutputConfig
import androidx.camera.core.impl.ImageProxyBundle
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeCaptureStage
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

private val DEFAULT_RESOLUTION = Size(640, 480)
private val GUARANTEED_RESOLUTION = Size(640, 480)
private val BACK_SELECTOR = CameraSelector.DEFAULT_BACK_CAMERA
private const val BACK_LENS_FACING = CameraSelector.LENS_FACING_BACK
private const val CAPTURE_TIMEOUT = 10_000.toLong() //  10 seconds

@LargeTest
@RunWith(Parameterized::class)
class ImageCaptureTest(private val implName: String, private val cameraXConfig: CameraXConfig) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val externalStorageRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

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
    private lateinit var camera: CameraUseCaseAdapter

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(BACK_LENS_FACING))
        createDefaultPictureFolderIfNotExist()
        CameraX.initialize(context, cameraXConfig).get(10, TimeUnit.SECONDS)
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::camera.isInitialized) {
            // TODO: The removeUseCases() call might be removed after clarifying the
            // abortCaptures() issue in b/162314023
            withContext(Dispatchers.Main) {
                camera.removeUseCases(camera.useCases)
            }
        }
        CameraX.shutdown().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun capturedImageHasCorrectSize() = runBlocking {
        skipTestOnCameraPipeConfig()

        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .setTargetRotation(Surface.ROTATION_0)
            .build()

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        val imageProperties = callback.results.first()
        var sizeEnvelope = imageProperties.size

        // Some devices may not be able to fit the requested resolution within the image
        // boundaries. In this case, they should always fall back to a guaranteed resolution of
        // 640 x 480.
        if (sizeEnvelope != GUARANTEED_RESOLUTION) {
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
    fun canSupportGuaranteedSizeFront() {
        skipTestOnCameraPipeConfig()

        canSupportGuaranteedSize(
            CameraSelector.LENS_FACING_FRONT,
            CameraSelector.DEFAULT_FRONT_CAMERA
        )
    }

    @Test
    fun canSupportGuaranteedSizeBack() {
        skipTestOnCameraPipeConfig()

        canSupportGuaranteedSize(
            CameraSelector.LENS_FACING_BACK,
            CameraSelector.DEFAULT_BACK_CAMERA
        )
    }

    /** check both front and back device cameras can support the guaranteed 640x480 size. */
    private fun canSupportGuaranteedSize(lensFacing: Int, selector: CameraSelector) = runBlocking {
        assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))
        assumeTrue(!CameraUtil.requiresCorrectedAspectRatio(lensFacing))

        // Checks camera device sensor degrees to set correct target rotation value to make sure
        // the exactly matching result size 640x480 can be selected if the device supports it.
        val sensorOrientation = CameraUtil.getSensorOrientation(lensFacing)
        val isRotateNeeded = sensorOrientation!! % 180 != 0
        val useCase = ImageCapture.Builder()
            .setTargetResolution(GUARANTEED_RESOLUTION)
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()

        camera = CameraUtil.createCameraAndAttachUseCase(context, selector, useCase)

        val callback = FakeImageCaptureCallback(capturesCount = 1)
        useCase.takePicture(mainExecutor, callback)

        // Wait for the signal that the image has been captured.
        callback.awaitCapturesAndAssert(capturedImagesCount = 1)

        // Check the captured image exactly matches 640x480 size. This test can also check
        // whether the guaranteed resolution 640x480 is really supported for JPEG format on the
        // devices when running the test.
        val imageProperties = callback.results.first()
        Assert.assertEquals(GUARANTEED_RESOLUTION, imageProperties.size)
    }

    @Test
    fun canCaptureMultipleImages() {
        skipTestOnCameraPipeConfig()

        canTakeMultipleImages(defaultBuilder)
    }

    @Test
    fun canCaptureMultipleImagesWithMaxQuality() {
        skipTestOnCameraPipeConfig()

        canTakeMultipleImages(
            ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        )
    }

    private fun canTakeMultipleImages(builder: ImageCapture.Builder): Unit = runBlocking {
        val useCase = builder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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

    @Test
    fun saveCanSucceed_withNonExistingFile() {
        skipTestOnCameraPipeConfig()

        val saveLocation = File(context.cacheDir, "test" + System.currentTimeMillis() + ".jpg")
        saveLocation.deleteOnExit()

        // make sure file does not exist
        if (saveLocation.exists()) {
            saveLocation.delete()
        }
        assertThat(!saveLocation.exists())

        canSaveToFile(saveLocation)
    }

    @Test
    fun saveCanSucceed_withExistingFile() {
        skipTestOnCameraPipeConfig()

        val saveLocation = File.createTempFile("test", ".jpg")
        saveLocation.deleteOnExit()
        assertThat(saveLocation.exists())

        canSaveToFile(saveLocation)
    }

    private fun canSaveToFile(saveLocation: File) = runBlocking {
        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        skipTestOnCameraPipeConfig()

        // Arrange.
        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        skipTestOnCameraPipeConfig()

        // Arrange.
        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val saveLocation = File.createTempFile("test", ".jpg")
        saveLocation.deleteOnExit()

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
        skipTestOnCameraPipeConfig()

        // TODO(b/147448711) Add back in once cuttlefish has correct user cropping functionality.
        assumeFalse(
            "Cuttlefish does not correctly handle crops. Unable to test.",
            Build.MODEL.contains("Cuttlefish")
        )

        val useCase = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val saveLocation = File.createTempFile("test", ".jpg")
        saveLocation.deleteOnExit()

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

        val saveLocationRotated90 = File.createTempFile("testRotated90", ".jpg")
        saveLocationRotated90.deleteOnExit()

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
        skipTestOnCameraPipeConfig()

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
        skipTestOnCameraPipeConfig()

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

    @Test
    fun canSaveFile_withAttachedLocation() {
        skipTestOnCameraPipeConfig()

        val location = Location("ImageCaptureTest")
        val metadata = ImageCapture.Metadata()
        metadata.location = location
        canSaveFileWithMetadata(
            configBuilder = defaultBuilder,
            metadata = metadata,
            verifyExif = { exif ->
                assertThat(exif.location!!.provider).isEqualTo(location.provider)
            }
        )
    }

    private fun canSaveFileWithMetadata(
        configBuilder: ImageCapture.Builder,
        metadata: ImageCapture.Metadata,
        verifyExif: (Exif) -> Unit
    ) = runBlocking {
        val useCase = configBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val saveLocation = File.createTempFile("test", ".jpg")
        saveLocation.deleteOnExit()
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
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val numImages = 5
        val callback = FakeImageSavedCallback(capturesCount = numImages)

        for (i in 0 until numImages) {
            val saveLocation = File.createTempFile("test$i", ".jpg")
            saveLocation.deleteOnExit()
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
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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

    @Test
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    fun camera2InteropCaptureSessionCallbacks() = runBlocking {
        skipTestOnCameraPipeConfig()

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
        Camera2Interop.Extender(builder).setSessionCaptureCallback(captureCallback)
        val useCase = builder.build()

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        skipTestOnCameraPipeConfig()

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

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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

    @Test(expected = IllegalArgumentException::class)
    fun constructor_withBufferFormatAndCaptureProcessor_throwsException() {
        val captureProcessor = object : CaptureProcessor {
            override fun onOutputSurface(surface: Surface?, imageFormat: Int) {}
            override fun process(bundle: ImageProxyBundle?) {}
            override fun onResolutionUpdate(size: Size?) {}
        }
        ImageCapture.Builder()
            .setBufferFormat(ImageFormat.RAW_SENSOR)
            .setCaptureProcessor(captureProcessor)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_maxCaptureStageInvalid_throwsException() {
        ImageCapture.Builder().setMaxCaptureStages(0).build()
    }

    @Test
    fun captureStagesAbove1_withoutCaptureProcessor() = runBlocking {
        skipTestOnCameraPipeConfig()

        val captureBundle = CaptureBundle {
            listOf(
                FakeCaptureStage(0, CaptureConfig.Builder().build()),
                FakeCaptureStage(1, CaptureConfig.Builder().build())
            )
        }

        val imageCapture = ImageCapture.Builder()
            .setCaptureBundle(captureBundle)
            .build()

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)

        val callback = FakeImageCaptureCallback(capturesCount = 1)

        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image capture has failed.
        callback.awaitCapturesAndAssert(errorsCount = 1)

        val error = callback.errors.first()
        assertThat(error).isInstanceOf(ImageCaptureException::class.java)
        assertThat(error.cause).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun captureStageExceedMaxCaptureStage_whenIssueTakePicture() = runBlocking {
        skipTestOnCameraPipeConfig()

        // Initial the captureStages not greater than the maximum count to bypass the CaptureStage
        // count checking during bindToLifeCycle.
        val captureStages = mutableListOf<CaptureStage>()
        captureStages.add(FakeCaptureStage(0, CaptureConfig.Builder().build()))

        val captureBundle = CaptureBundle { captureStages.toList() }
        val captureProcessor = object : CaptureProcessor {
            override fun onOutputSurface(surface: Surface?, imageFormat: Int) {}
            override fun process(bundle: ImageProxyBundle?) {}
            override fun onResolutionUpdate(size: Size?) {}
        }
        val imageCapture = ImageCapture.Builder()
            .setMaxCaptureStages(1)
            .setCaptureBundle(captureBundle)
            .setCaptureProcessor(captureProcessor)
            .build()

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)

        // Add an additional capture stage to test the case
        // captureStage.size() >　mMaxCaptureStages during takePicture.
        captureStages.add(FakeCaptureStage(1, CaptureConfig.Builder().build()))

        val callback = FakeImageCaptureCallback(capturesCount = 2)

        // Take 2 photos.
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)

        // It should get onError() callback twice.
        callback.awaitCapturesAndAssert(errorsCount = 2)

        callback.errors.forEach { error ->
            assertThat(error.cause).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun onStateOffline_abortAllCaptureRequests() = runBlocking {
        skipTestOnCameraPipeConfig()

        val imageCapture = ImageCapture.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)

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

        for (error in callback2.errors) {
            assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_CAMERA_CLOSED)
        }
    }

    @Test
    fun unbind_abortAllCaptureRequests() = runBlocking {
        skipTestOnCameraPipeConfig()

        val imageCapture = ImageCapture.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)

        val callback = FakeImageCaptureCallback(capturesCount = 3)
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)
        imageCapture.takePicture(mainExecutor, callback)

        // Needs to run on main thread because takePicture gets posted on main thread if it isn't
        // running on the main thread. Which means the internal ImageRequests likely get issued
        // after ImageCapture is removed so errors out with a different error from
        // ERROR_CAMERA_CLOSED
        withContext(Dispatchers.Main) {
            camera.removeUseCases(setOf(imageCapture))
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
        skipTestOnCameraPipeConfig()

        val imageCapture = ImageCapture.Builder().build()
        val callback = FakeImageCaptureCallback(capturesCount = 1)

        imageCapture.takePicture(mainExecutor, callback)

        // Wait for the signal that the image capture has failed.
        callback.awaitCapturesAndAssert(errorsCount = 1)

        val error = callback.errors.first()
        assertThat(error.imageCaptureError).isEqualTo(ImageCapture.ERROR_INVALID_CAMERA)
    }

    @Test
    fun defaultAspectRatioWillBeSet_whenTargetResolutionIsNotSet() {
        skipTestOnCameraPipeConfig()

        val useCase = ImageCapture.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val config = useCase.currentConfig as ImageOutputConfig
        assertThat(config.targetAspectRatio).isEqualTo(AspectRatio.RATIO_4_3)
    }

    @Test
    fun defaultAspectRatioWontBeSet_whenTargetResolutionIsSet() {
        skipTestOnCameraPipeConfig()

        val useCase = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .build()

        assertThat(
            useCase.currentConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO
            )
        ).isFalse()

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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

    @Test
    fun targetResolutionIsUpdatedAfterTargetRotationIsUpdated() {
        skipTestOnCameraPipeConfig()

        val imageCapture = ImageCapture.Builder()
            .setTargetResolution(DEFAULT_RESOLUTION)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)

        // Updates target rotation from ROTATION_0 to ROTATION_90.
        imageCapture.targetRotation = Surface.ROTATION_90

        val newConfig = imageCapture.currentConfig as ImageOutputConfig
        val expectedTargetResolution = Size(DEFAULT_RESOLUTION.height, DEFAULT_RESOLUTION.width)

        // Expected targetResolution will be reversed from original target resolution.
        assertThat(newConfig.targetResolution).isEqualTo(expectedTargetResolution)
    }

    @Test
    fun capturedImageHasCorrectCroppingSizeWithoutSettingRotation() {
        skipTestOnCameraPipeConfig()

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

    @Test
    fun capturedImageHasCorrectCroppingSizeSetRotationBuilder() {
        skipTestOnCameraPipeConfig()

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

    @Test
    fun capturedImageHasCorrectCroppingSize_setUseCaseRotation90FromRotationInBuilder() {
        skipTestOnCameraPipeConfig()

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
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        if (imageProperties.format == ImageFormat.JPEG) {
            assertThat(imageProperties.rotationDegrees).isEqualTo(imageProperties.exif!!.rotation)
        }
    }

    @Test
    fun capturedImageHasCorrectCroppingSize_setCropAspectRatioAfterBindToLifecycle() = runBlocking {
        skipTestOnCameraPipeConfig()

        val useCase = ImageCapture.Builder().build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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

        if (imageProperties.format == ImageFormat.JPEG) {
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
        skipTestOnCameraPipeConfig()

        val sensorOrientation = CameraUtil.getSensorOrientation(BACK_LENS_FACING)
        val isRotateNeeded = sensorOrientation!! % 180 != 0

        val useCase = ImageCapture.Builder()
            .setTargetRotation(if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0)
            .build()

        // Sets a crop aspect ratio to the use case. This will be overwritten by the view port
        // setting.
        val useCaseCroppingAspectRatio = Rational(4, 3)
        useCase.setCropAspectRatio(useCaseCroppingAspectRatio)

        camera = CameraUtil.createCameraUseCaseAdapter(context, BACK_SELECTOR)

        val viewPortAspectRatio = Rational(2, 1)
        val viewPort = ViewPort.Builder(
            viewPortAspectRatio,
            if (isRotateNeeded) Surface.ROTATION_90 else Surface.ROTATION_0
        ).build()

        // Sets view port with different aspect ratio and then attach the use case
        camera.setViewPort(viewPort)

        withContext(Dispatchers.Main) {
            camera.addUseCases(listOf(useCase))
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

        if (imageProperties.format == ImageFormat.JPEG) {
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
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        val initialConfig = useCase.currentConfig
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        withContext(Dispatchers.Main) {
            camera.removeUseCases(setOf(useCase))
        }

        val configAfterUnbinding = useCase.currentConfig
        assertThat(initialConfig == configAfterUnbinding).isTrue()
    }

    @Test
    fun targetRotationIsRetained_whenUseCaseIsReused() = runBlocking {
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        // Generally, the device can't be rotated to Surface.ROTATION_180. Therefore,
        // use it to do the test.
        useCase.targetRotation = Surface.ROTATION_180
        withContext(Dispatchers.Main) {
            // Unbind the use case.
            camera.removeUseCases(setOf(useCase))
        }

        // Check the target rotation is kept when the use case is unbound.
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)

        // Check the target rotation is kept when the use case is rebound to the
        // lifecycle.
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)
        assertThat(useCase.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun cropAspectRatioIsRetained_whenUseCaseIsReused() = runBlocking {
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        val cropAspectRatio = Rational(1, 1)
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        useCase.setCropAspectRatio(cropAspectRatio)

        withContext(Dispatchers.Main) {
            // Unbind the use case.
            camera.removeUseCases(setOf(useCase))
        }

        // Rebind the use case.
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val saveLocation1 = File.createTempFile("test1", ".jpg")
        saveLocation1.deleteOnExit()

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
            camera.removeUseCases(setOf(useCase))
        }

        // Rebind the use case to the same camera.
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val saveLocation2 = File.createTempFile("test2", ".jpg")
        saveLocation2.deleteOnExit()

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
        skipTestOnCameraPipeConfig()

        val useCase = defaultBuilder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

        val saveLocation1 = File.createTempFile("test1", ".jpg")
        saveLocation1.deleteOnExit()

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
            camera.removeUseCases(setOf(useCase))
        }

        // Rebind the use case to different camera.
        camera = CameraUtil.createCameraAndAttachUseCase(
            context,
            CameraSelector.DEFAULT_FRONT_CAMERA,
            useCase
        )

        val saveLocation2 = File.createTempFile("test2", ".jpg")
        saveLocation2.deleteOnExit()

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
    fun returnCorrectTargetRotation_afterUseCaseIsAttached() {
        skipTestOnCameraPipeConfig()

        val imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_180)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)
        assertThat(imageCapture.targetRotation).isEqualTo(Surface.ROTATION_180)
    }

    @Test
    fun returnDefaultFlashMode_beforeUseCaseIsAttached() {
        val imageCapture = ImageCapture.Builder().build()
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_OFF)
    }

    @Test
    fun returnCorrectFlashMode_afterUseCaseIsAttached() {
        skipTestOnCameraPipeConfig()

        val imageCapture = ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_ON)
            .build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, imageCapture)
        assertThat(imageCapture.flashMode).isEqualTo(ImageCapture.FLASH_MODE_ON)
    }

    // Output JPEG format image when setting a CaptureProcessor is only enabled for devices whose
    // API level is at least 29.
    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun returnJpegImage_whenSoftwareJpegIsEnabled() = runBlocking {
        skipTestOnCameraPipeConfig()

        val builder = ImageCapture.Builder()

        // Enables software Jpeg
        builder.mutableConfig.insertOption(
            ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER,
            true
        )

        val useCase = builder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
    @SdkSuppress(minSdkVersion = 26)
    fun returnYuvImage_whenSoftwareJpegIsEnabledWithYuvBufferFormat() = runBlocking {
        skipTestOnCameraPipeConfig()

        val builder = ImageCapture.Builder().setBufferFormat(ImageFormat.YUV_420_888)

        // Enables software Jpeg
        builder.mutableConfig.insertOption(
            ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER,
            true
        )

        val useCase = builder.build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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

    // Output JPEG format image when setting a CaptureProcessor is only enabled for devices that
    // API level is at least 29.
    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun returnJpegImage_whenCaptureProcessorIsSet() = runBlocking {
        skipTestOnCameraPipeConfig()

        val builder = ImageCapture.Builder()
        val simpleCaptureProcessor = SimpleCaptureProcessor()

        // Set a CaptureProcessor to directly pass the image to output surface.
        val useCase = builder.setCaptureProcessor(simpleCaptureProcessor).build()
        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        simpleCaptureProcessor.close()
    }

    // Output JPEG format image when setting a CaptureProcessor is only enabled for devices that
    // API level is at least 29.
    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun returnJpegImage_whenSoftwareJpegIsEnabledWithCaptureProcessor() = runBlocking {
        skipTestOnCameraPipeConfig()

        val builder = ImageCapture.Builder()
        val simpleCaptureProcessor = SimpleCaptureProcessor()

        // Set a CaptureProcessor to directly pass the image to output surface.
        val useCase = builder.setCaptureProcessor(simpleCaptureProcessor).build()

        // Enables software Jpeg
        builder.mutableConfig.insertOption(
            ImageCaptureConfig.OPTION_USE_SOFTWARE_JPEG_ENCODER,
            true
        )

        camera = CameraUtil.createCameraAndAttachUseCase(context, BACK_SELECTOR, useCase)

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
        simpleCaptureProcessor.close()
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

    // TODO(b/185260678): Remove when image capture support is added to Camera-pipe-integration
    private fun skipTestOnCameraPipeConfig() {
        assumeFalse(
            "Image capture isn't supported on Camera-pipe-integration (b/185260678)",
            implName == CameraPipeConfig::class.simpleName
        )
    }

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
            withTimeout(timeout) {
                latch.await()
            }
        }

        suspend fun awaitCapturesAndAssert(
            timeout: Long = CAPTURE_TIMEOUT,
            capturedImagesCount: Int = 0,
            errorsCount: Int = 0
        ) {
            withTimeout(timeout) {
                latch.await()
            }
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
            withTimeout(timeout) {
                latch.await()
            }
            assertThat(results.size).isEqualTo(savedImagesCount)
            assertThat(errors.size).isEqualTo(errorsCount)
        }
    }

    private class SimpleCaptureProcessor : CaptureProcessor {

        private var imageWriter: ImageWriter? = null

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onOutputSurface(surface: Surface, imageFormat: Int) {
            imageWriter = ImageWriter.newInstance(surface, 2)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun process(bundle: ImageProxyBundle) {
            val imageProxyListenableFuture = bundle.getImageProxy(bundle.captureIds[0])
            try {
                val imageProxy = imageProxyListenableFuture.get()
                // Directly passing the input YUV image to the output surface.
                imageWriter!!.queueInputImage(imageProxy.image)
            } catch (exception: ExecutionException) {
                throw IllegalArgumentException(
                    "Can't extract ImageProxy from the ImageProxyBundle.",
                    exception
                )
            } catch (exception: InterruptedException) {
                throw IllegalArgumentException(
                    "Can't extract ImageProxy from the ImageProxyBundle.",
                    exception
                )
            }
        }

        override fun onResolutionUpdate(size: Size) {}

        fun close() {
            imageWriter?.close()
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