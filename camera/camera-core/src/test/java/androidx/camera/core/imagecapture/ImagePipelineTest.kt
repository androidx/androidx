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
import android.hardware.camera2.CameraDevice
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SIZE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.imagecapture.Utils.injectRotationOptionQuirk
import androidx.camera.core.impl.CaptureConfig.OPTION_ROTATION
import androidx.camera.core.impl.ImageInputConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.internal.IoConfig.OPTION_IO_EXECUTOR
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.fakes.FakeImageInfo
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [ImagePipeline].
 */
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

    @Before
    fun setUp() {
        // Create ImageCaptureConfig.
        val builder = ImageCapture.Builder().setCaptureOptionUnpacker { _, builder ->
            builder.templateType = TEMPLATE_TYPE
        }
        builder.mutableConfig.insertOption(OPTION_IO_EXECUTOR, mainThreadExecutor())
        builder.mutableConfig.insertOption(ImageInputConfig.OPTION_INPUT_FORMAT, ImageFormat.JPEG)
        imagePipeline = ImagePipeline(builder.useCaseConfig, SIZE)
    }

    @After
    fun tearDown() {
        imagePipeline.close()
    }

    @Test
    fun createRequests_verifyCameraRequest() {
        // Arrange.
        val captureInput = imagePipeline.captureNode.inputEdge

        // Act: create requests
        val result = imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK)
        // Assert: CameraRequest is constructed correctly.
        val cameraRequest = result.first!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.cameraCaptureCallbacks)
            .containsExactly(captureInput.cameraCaptureCallback)
        assertThat(captureConfig.surfaces).containsExactly(captureInput.surface)
        assertThat(captureConfig.templateType).isEqualTo(TEMPLATE_TYPE)
        assertThat(captureConfig.implementationOptions.retrieveOption(OPTION_ROTATION))
            .isEqualTo(ROTATION_DEGREES)

        // Act: fail the camera request.
        cameraRequest.onCaptureFailure(FAILURE)
        // Assert: The failure is propagated.
        assertThat(CALLBACK.captureFailure).isEqualTo(FAILURE)
    }

    @Test
    fun createCameraRequestWithRotationQuirk_rotationNotInCaptureConfig() {
        // Arrange.
        injectRotationOptionQuirk()

        // Act: create requests
        val result = imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK)
        // Assert: CameraRequest is constructed correctly.
        val cameraRequest = result.first!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.implementationOptions.retrieveOption(OPTION_ROTATION, null))
            .isNull()
    }

    @Test
    fun createRequests_verifyProcessingRequest() {
        // Act: create requests
        val result = imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK)
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
    fun createRequests_captureTagMatches() {
        // Act: create requests
        val result = imagePipeline.createRequests(IN_MEMORY_REQUEST, CALLBACK)

        // Assert: ProcessingRequest's tag matches camera request.
        val cameraRequest = result.first!!
        val processingRequest = result.second!!
        val captureConfig = cameraRequest.captureConfigs.single()
        assertThat(captureConfig.tagBundle.getTag(processingRequest.tagBundleKey))
            .isEqualTo(processingRequest.stageIds.single())
    }

    @Test
    fun sendInMemoryRequest_receivesImageProxy() {
        // Arrange.
        val processingRequest = imagePipeline.createRequests(
            IN_MEMORY_REQUEST, CALLBACK
        ).second!!
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val imageInfo = FakeImageInfo().apply {
            this.setTag(processingRequest.tagBundleKey, processingRequest.stageIds.single())
        }
        val image = createJpegFakeImageProxy(imageInfo, jpegBytes)

        // Act: send processing request and the image.
        imagePipeline.postProcess(processingRequest)
        imagePipeline.captureNode.onImageProxyAvailable(image)
        shadowOf(getMainLooper()).idle()

        // Assert: the image is received by TakePictureCallback.
        assertThat(CALLBACK.inMemoryResult!!.planes).isEqualTo(image.planes)
    }
}