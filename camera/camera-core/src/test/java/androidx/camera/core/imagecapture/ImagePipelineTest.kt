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

package androidx.camera.core.imagecapture

import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCapture.CaptureMode
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxyProvider
import androidx.camera.core.SafeCloseImageReaderProxy
import androidx.camera.core.imagecapture.CaptureNode.MAX_IMAGES
import androidx.camera.core.imagecapture.ImagePipeline.JPEG_QUALITY_MAX_QUALITY
import androidx.camera.core.imagecapture.ImagePipeline.JPEG_QUALITY_MIN_LATENCY
import androidx.camera.core.imagecapture.TakePictureManager.CaptureError
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.FULL_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.JPEG_QUALITY
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.SIZE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.imagecapture.Utils.createCameraCaptureResultImageInfo
import androidx.camera.core.imagecapture.Utils.injectRotationOptionQuirk
import androidx.camera.core.impl.CaptureConfig.OPTION_JPEG_QUALITY
import androidx.camera.core.impl.CaptureConfig.OPTION_ROTATION
import androidx.camera.core.impl.ImageCaptureConfig
import androidx.camera.core.impl.ImageCaptureConfig.OPTION_BUFFER_FORMAT
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.ImageInputConfig.OPTION_INPUT_DYNAMIC_RANGE
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.IoConfig.OPTION_IO_EXECUTOR
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createJpegrBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegrFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createRawFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createYuvFakeImageProxy
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy
import androidx.camera.testing.impl.fakes.GrayscaleImageEffect
import androidx.core.util.Pair
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [ImagePipeline]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ImagePipelineTest {

    companion object {
        private const val TEMPLATE_TYPE = CameraDevice.TEMPLATE_STILL_CAPTURE
        private val IN_MEMORY_REQUEST =
            FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        private val CALLBACK = FakeTakePictureCallback()
        private val FAILURE = ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "", null)
    }

    private lateinit var imagePipeline: ImagePipeline
    private lateinit var imageCaptureConfig: ImageCaptureConfig
    private lateinit var cameraCharacteristics: CameraCharacteristics

    @Before
    fun setUp() {
        imageCaptureConfig = createImageCaptureConfig()
        cameraCharacteristics = mock(CameraCharacteristics::class.java)
        imagePipeline = ImagePipeline(imageCaptureConfig, SIZE, cameraCharacteristics)
    }

    @After
    fun tearDown() {
        imagePipeline.close()
    }

    @Test
    fun createPipeline_captureNodeHasImageReaderProxyProvider() {
        // Arrange.
        val imageReaderProxyProvider = ImageReaderProxyProvider { _, _, _, _, _ ->
            FakeImageReaderProxy(MAX_IMAGES)
        }
        val builder =
            ImageCapture.Builder()
                .setImageReaderProxyProvider(imageReaderProxyProvider)
                .setCaptureOptionUnpacker { _, builder -> builder.templateType = TEMPLATE_TYPE }
        builder.mutableConfig.insertOption(ImageInputConfig.OPTION_INPUT_FORMAT, ImageFormat.JPEG)
        // Act.
        val pipeline = ImagePipeline(builder.useCaseConfig, SIZE, cameraCharacteristics)
        // Assert.
        assertThat(pipeline.captureNode.inputEdge.imageReaderProxyProvider)
            .isEqualTo(imageReaderProxyProvider)
    }

    @Test
    fun createPipelineWithoutImageReaderProxyProvider_isNull() {
        assertThat(imagePipeline.captureNode.inputEdge.imageReaderProxyProvider).isNull()
    }

    @Test
    fun createPipelineWithVirtualCamera_receivesImageProxy() {
        // Arrange: close the pipeline and create a new one not expecting metadata.
        imagePipeline.close()
        imagePipeline =
            ImagePipeline(
                imageCaptureConfig,
                SIZE,
                cameraCharacteristics,
                /*cameraEffect=*/ null,
                /*isVirtualCamera=*/ true
            )

        // Act & assert: send and receive ImageProxy.
        sendInMemoryRequest_receivesImageProxy()
    }

    @Test
    fun createPipelineWithoutEffect_processingNodeHasNoEffect() {
        assertThat(imagePipeline.processingNode.mImageProcessor).isNull()
    }

    @Test
    fun createPipelineWithEffect_processingNodeContainsEffect() {
        assertThat(
                ImagePipeline(
                        imageCaptureConfig,
                        SIZE,
                        cameraCharacteristics,
                        GrayscaleImageEffect(),
                        false
                    )
                    .processingNode
                    .mImageProcessor
            )
            .isNotNull()
    }

    @Test
    fun createRequests_verifyCameraRequest() {
        // Arrange.
        val captureInput = imagePipeline.captureNode.inputEdge

        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))

        // Assert: CameraRequest is constructed correctly.
        verifyCaptureRequest(captureInput, result)
    }

    @Config(minSdk = 34)
    @Test
    fun createRequests_verifyCameraRequest_whenFormatIsJpegr() {
        // Arrange.
        imageCaptureConfig = createImageCaptureConfig(inputFormat = ImageFormat.JPEG_R)
        imagePipeline = ImagePipeline(imageCaptureConfig, SIZE, cameraCharacteristics)
        val captureInput = imagePipeline.captureNode.inputEdge

        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))

        // Assert: CameraRequest is constructed correctly.
        verifyCaptureRequest(captureInput, result)
    }

    @Test
    fun createRequests_verifyCameraRequest_whenFormatIsRAw() {
        // Arrange.
        imageCaptureConfig = createImageCaptureConfig(inputFormat = ImageFormat.RAW_SENSOR)
        imagePipeline = ImagePipeline(imageCaptureConfig, SIZE, cameraCharacteristics)
        val captureInput = imagePipeline.captureNode.inputEdge

        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))

        // Assert: CameraRequest is constructed correctly.
        verifyCaptureRequest(captureInput, result)
    }

    @Test
    fun createCameraRequestWithRotationQuirk_rotationNotInCaptureConfig() {
        // Arrange.
        injectRotationOptionQuirk()

        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))
        // Assert: CameraRequest is constructed correctly.
        val cameraRequest = result.first!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.implementationOptions.retrieveOption(OPTION_ROTATION, null))
            .isNull()
    }

    @Test
    fun createRequests_verifyProcessingRequest() {
        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))
        // Assert: ProcessingRequest is constructed correctly.
        val processingRequest = result.second!!
        assertThat(processingRequest.jpegQuality).isEqualTo(IN_MEMORY_REQUEST.jpegQuality)
        assertThat(processingRequest.rotationDegrees).isEqualTo(IN_MEMORY_REQUEST.rotationDegrees)
        assertThat(processingRequest.sensorToBufferTransform)
            .isEqualTo(IN_MEMORY_REQUEST.sensorToBufferTransform)
        assertThat(processingRequest.cropRect).isEqualTo(IN_MEMORY_REQUEST.cropRect)

        // Act: fail the camera request.
        processingRequest.onProcessFailure(FAILURE)
        // Assert: The failure is propagated.
        assertThat(CALLBACK.processFailure).isEqualTo(FAILURE)
    }

    @Test
    fun createRequestWithCroppingAndMaxQuality_cameraRequestJpegQualityIsMaxQuality() {
        assertThat(getCameraRequestJpegQuality(CROP_RECT, CAPTURE_MODE_MAXIMIZE_QUALITY))
            .isEqualTo(JPEG_QUALITY_MAX_QUALITY)
    }

    @Test
    fun createRequestWithCroppingAndMinLatency_cameraRequestJpegQualityIsMinLatency() {
        assertThat(getCameraRequestJpegQuality(CROP_RECT, CAPTURE_MODE_MINIMIZE_LATENCY))
            .isEqualTo(JPEG_QUALITY_MIN_LATENCY)
    }

    @Test
    fun createRequestWithoutCroppingAndMaxQuality_cameraRequestJpegQualityIsOriginal() {
        assertThat(getCameraRequestJpegQuality(FULL_RECT, CAPTURE_MODE_MAXIMIZE_QUALITY))
            .isEqualTo(JPEG_QUALITY)
    }

    @Test
    fun createSessionConfigBuilderWithYuvPostviewEnabled() {
        // Arrange.
        val postviewSize = Size(640, 480)
        imagePipeline =
            ImagePipeline(
                imageCaptureConfig,
                SIZE,
                cameraCharacteristics,
                null,
                false,
                postviewSize,
                ImageFormat.YUV_420_888
            )

        // Act: create SessionConfig
        val sessionConfig = imagePipeline.createSessionConfigBuilder(SIZE).build()

        // Assert: SessionConfig contains the postview output config.
        assertThat(sessionConfig.postviewOutputConfig).isNotNull()
        assertThat(sessionConfig.postviewOutputConfig!!.surface.prescribedSize)
            .isEqualTo(postviewSize)
        assertThat(sessionConfig.postviewOutputConfig!!.surface.prescribedStreamFormat)
            .isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    fun createSessionConfigBuilderWithJpegPostviewEnabled() {
        // Arrange.
        val postviewSize = Size(640, 480)
        imagePipeline =
            ImagePipeline(
                imageCaptureConfig,
                SIZE,
                cameraCharacteristics,
                null,
                false,
                postviewSize,
                ImageFormat.JPEG
            )

        // Act: create SessionConfig
        val sessionConfig = imagePipeline.createSessionConfigBuilder(SIZE).build()

        // Assert: SessionConfig contains the postview output config.
        assertThat(sessionConfig.postviewOutputConfig).isNotNull()
        assertThat(sessionConfig.postviewOutputConfig!!.surface.prescribedSize)
            .isEqualTo(postviewSize)
        assertThat(sessionConfig.postviewOutputConfig!!.surface.prescribedStreamFormat)
            .isEqualTo(ImageFormat.JPEG)
    }

    @Test
    fun createCameraRequestWithPostviewEnabled() {
        // Arrange.
        val postviewSize = Size(640, 480)
        imagePipeline =
            ImagePipeline(
                imageCaptureConfig,
                SIZE,
                cameraCharacteristics,
                null,
                false,
                postviewSize,
                ImageFormat.YUV_420_888
            )

        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))

        // Assert: isPostviewEnabled is true on CaptureConfig.
        val cameraRequest = result.first!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.isPostviewEnabled).isTrue()
    }

    private fun getCameraRequestJpegQuality(cropRect: Rect, @CaptureMode captureMode: Int): Int {
        // Arrange: TakePictureRequest with cropping
        val request =
            TakePictureRequest.of(
                mainThreadExecutor(),
                null,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {}

                    override fun onError(exception: ImageCaptureException) {}
                },
                OUTPUT_FILE_OPTIONS,
                cropRect,
                SENSOR_TO_BUFFER,
                ROTATION_DEGREES,
                JPEG_QUALITY,
                captureMode,
                listOf()
            )

        // Act: create camera request.
        val result = imagePipeline.createRequests(request, CALLBACK, Futures.immediateFuture(null))

        // Get JPEG quality and return.
        val cameraRequest = result.first!!
        val captureConfig = cameraRequest.captureConfigs.single()
        return captureConfig.implementationOptions.retrieveOption(OPTION_JPEG_QUALITY)!!
    }

    @Test
    fun createRequests_captureTagMatches() {
        // Act: create requests
        val result =
            imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))

        // Assert: ProcessingRequest's tag matches camera request.
        val cameraRequest = result.first!!
        val processingRequest = result.second!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.tagBundle.getTag(processingRequest.tagBundleKey))
            .isEqualTo(processingRequest.stageIds.single())
    }

    @Test
    fun createPipelineWithYuvOutput_getsYuvImage() {
        val builder =
            ImageCapture.Builder().setCaptureOptionUnpacker { _, builder ->
                builder.templateType = TEMPLATE_TYPE
            }
        builder.mutableConfig.insertOption(OPTION_BUFFER_FORMAT, ImageFormat.YUV_420_888)
        builder.mutableConfig.insertOption(OPTION_IO_EXECUTOR, mainThreadExecutor())
        builder.mutableConfig.insertOption(ImageInputConfig.OPTION_INPUT_FORMAT, ImageFormat.JPEG)
        val pipeline = ImagePipeline(builder.useCaseConfig, SIZE, cameraCharacteristics)

        // Arrange & act.
        sendInMemoryRequest(pipeline, ImageFormat.YUV_420_888)

        assertThat(CALLBACK.inMemoryResult!!.format).isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    fun sendInMemoryRequest_receivesImageProxy() {
        // Arrange & act.
        val image = sendInMemoryRequest(imagePipeline)

        // Assert: the image is received by TakePictureCallback.
        assertThat(image.format).isEqualTo(ImageFormat.JPEG)
        assertThat(CALLBACK.inMemoryResult!!.format).isEqualTo(ImageFormat.JPEG)
        assertThat(CALLBACK.inMemoryResult!!.planes).isEqualTo(image.planes)
    }

    @Config(minSdk = 34)
    @Test
    fun sendInMemoryRequest_receivesImageProxy_whenFormatIsJpegr() {
        // Arrange & act.
        imageCaptureConfig = createImageCaptureConfig(inputFormat = ImageFormat.JPEG_R)
        imagePipeline = ImagePipeline(imageCaptureConfig, SIZE, cameraCharacteristics)
        val image = sendInMemoryRequest(imagePipeline, ImageFormat.JPEG_R)

        // Assert: the image is received by TakePictureCallback.
        assertThat(image.format).isEqualTo(ImageFormat.JPEG_R)
        assertThat(CALLBACK.inMemoryResult!!.format).isEqualTo(ImageFormat.JPEG_R)
        assertThat(CALLBACK.inMemoryResult!!.planes).isEqualTo(image.planes)
    }

    @Test
    fun sendInMemoryRequest_receivesImageProxy_whenFormatIsRaw() {
        // Arrange & act.
        imageCaptureConfig = createImageCaptureConfig(inputFormat = ImageFormat.RAW_SENSOR)
        imagePipeline = ImagePipeline(imageCaptureConfig, SIZE, cameraCharacteristics)
        val image = sendInMemoryRequest(imagePipeline, ImageFormat.RAW_SENSOR)

        // Assert: the image is received by TakePictureCallback.
        assertThat(image.format).isEqualTo(ImageFormat.RAW_SENSOR)
        assertThat(CALLBACK.inMemoryResult!!.format).isEqualTo(ImageFormat.RAW_SENSOR)
        assertThat(CALLBACK.inMemoryResult!!.planes).isEqualTo(image.planes)
    }

    /** Creates a ImageProxy and sends it to the pipeline. */
    private fun sendInMemoryRequest(
        pipeline: ImagePipeline,
        format: Int = ImageFormat.JPEG
    ): ImageProxy {
        // Arrange.
        val processingRequest =
            imagePipeline
                .createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))
                .second!!
        val imageInfo =
            createCameraCaptureResultImageInfo(
                processingRequest.tagBundleKey,
                processingRequest.stageIds.single()
            )
        val image =
            when (format) {
                ImageFormat.YUV_420_888 -> {
                    createYuvFakeImageProxy(imageInfo, WIDTH, HEIGHT)
                }
                ImageFormat.JPEG_R -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        throw IllegalArgumentException("JPEG_R is supported after API 34.")
                    }
                    val jpegBytes = createJpegrBytes(WIDTH, HEIGHT)
                    createJpegrFakeImageProxy(imageInfo, jpegBytes)
                }
                ImageFormat.RAW_SENSOR -> {
                    createRawFakeImageProxy(imageInfo, WIDTH, HEIGHT)
                }
                else -> {
                    val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
                    createJpegFakeImageProxy(imageInfo, jpegBytes)
                }
            }

        // Act: send processing request and the image.
        pipeline.submitProcessingRequest(processingRequest)
        pipeline.captureNode.onImageProxyAvailable(image)
        shadowOf(getMainLooper()).idle()

        return image
    }

    @Test
    fun acquireImageProxy_capacityIsUpdated() {
        // Arrange.
        val images = ArrayDeque<ImageProxy>()
        val imageReaderProxy = FakeImageReaderProxy(MAX_IMAGES)
        imagePipeline.captureNode.mSafeCloseImageReaderProxy =
            SafeCloseImageReaderProxy(imageReaderProxy)

        // Act.
        // Exhaust outstanding image quota.
        for (i in 0 until MAX_IMAGES) {
            val imageInfo = FakeImageInfo()
            imageReaderProxy.triggerImageAvailable(imageInfo.tagBundle, 0)
            imagePipeline.captureNode.mSafeCloseImageReaderProxy!!.acquireNextImage()?.let {
                images.add(it)
            }
        }

        // Assert: the capacity of queue is 0.
        assertThat(imagePipeline.capacity).isEqualTo(0)
    }

    @Test
    fun notifyCallbackError_captureFailureIsCalled() {
        // Arrange.
        val processingRequest =
            imagePipeline
                .createRequests(IN_MEMORY_REQUEST, CALLBACK, Futures.immediateFuture(null))
                .second!!

        // Act: send processing request and the image.
        imagePipeline.submitProcessingRequest(processingRequest)
        imagePipeline.notifyCaptureError(CaptureError.of(processingRequest.requestId, FAILURE))

        shadowOf(getMainLooper()).idle()
        // Assert: The failure is propagated.
        assertThat(CALLBACK.captureFailure).isEqualTo(FAILURE)
    }

    private fun createImageCaptureConfig(
        templateType: Int = TEMPLATE_TYPE,
        inputFormat: Int = ImageFormat.JPEG,
    ): ImageCaptureConfig {
        val builder =
            ImageCapture.Builder().setCaptureOptionUnpacker { _, builder ->
                builder.templateType = templateType
            }
        builder.mutableConfig.insertOption(OPTION_IO_EXECUTOR, mainThreadExecutor())
        builder.mutableConfig.insertOption(ImageInputConfig.OPTION_INPUT_FORMAT, inputFormat)
        if (Build.VERSION.SDK_INT >= 34 && inputFormat == ImageFormat.JPEG_R) {
            builder.mutableConfig.insertOption(OPTION_INPUT_DYNAMIC_RANGE, HLG_10_BIT)
        }
        builder.setSessionOptionUnpacker { _, _, _ -> }
        return builder.useCaseConfig
    }

    private fun verifyCaptureRequest(
        captureInput: CaptureNode.In,
        result: Pair<CameraRequest, ProcessingRequest>
    ) {
        val cameraRequest = result.first!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.cameraCaptureCallbacks)
            .containsExactly(captureInput.cameraCaptureCallback)
        assertThat(captureConfig.surfaces).containsExactly(captureInput.surface)
        assertThat(captureConfig.templateType).isEqualTo(TEMPLATE_TYPE)
        val jpegQuality = captureConfig.implementationOptions.retrieveOption(OPTION_JPEG_QUALITY)
        assertThat(jpegQuality).isEqualTo(JPEG_QUALITY)
        assertThat(captureConfig.implementationOptions.retrieveOption(OPTION_ROTATION))
            .isEqualTo(ROTATION_DEGREES)

        // Act: fail the processing request.
        val processingRequest = result.second!!
        processingRequest.onCaptureFailure(FAILURE)
        // Assert: The failure is propagated.
        assertThat(CALLBACK.captureFailure).isEqualTo(FAILURE)
    }
}
