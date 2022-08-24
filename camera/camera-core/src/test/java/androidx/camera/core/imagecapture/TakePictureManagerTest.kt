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

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [TakePictureManager].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TakePictureManagerTest {

    private val imagePipeline = FakeImagePipeline()
    private val imageCaptureControl = FakeImageCaptureControl()
    private val takePictureManager = TakePictureManager(imageCaptureControl, imagePipeline)

    @Test(expected = IllegalStateException::class)
    fun callOnFailureTwice_throwsException() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)
        val exception = ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "", null)
        val processingRequest = imagePipeline.getProcessingRequest(request)
        processingRequest.onImageCaptured()
        // Act.
        processingRequest.onProcessFailure(exception)
        processingRequest.onProcessFailure(exception)
    }

    @Test(expected = IllegalStateException::class)
    fun callOnFinalResultTwice_throwsException() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)
        val processingRequest = imagePipeline.getProcessingRequest(request)
        processingRequest.onImageCaptured()
        // Act.
        processingRequest.onFinalResult(FakeImageProxy(FakeImageInfo()))
        processingRequest.onFinalResult(OutputFileResults(null))
    }

    @Test
    fun pause_requestsPausedUntilResumed() {
        // Arrange: send 2 request.
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)

        // Act: pause the manage and finish the 1st request.
        takePictureManager.pause()
        imagePipeline.getProcessingRequest(request1).onImageCaptured()

        // Assert: only one request is sent.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()

        // Act: resume to process the 2nd request.
        takePictureManager.resume()

        // Assert: 2nd request is sent too.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
    }

    @Test
    fun submitRequestFails_appGetsErrorCallback() {
        // Arrange: configure ImageCaptureControl to always fail.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val cause = Exception()
        imageCaptureControl.response = Futures.immediateFailedFuture(cause)

        // Act.
        takePictureManager.offerRequest(request)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.exceptionReceived!!.cause).isEqualTo(cause)
        assertThat(takePictureManager.hasInFlightRequest()).isFalse()
    }

    @Test
    fun submitTwoRequests_ImageCaptureControlCalledInOrder() {
        // Arrange: setup ImagePipeline request and response
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val response1 = listOf(CaptureConfig.defaultEmptyCaptureConfig())
        val response2 = listOf(
            CaptureConfig.defaultEmptyCaptureConfig(),
            CaptureConfig.defaultEmptyCaptureConfig()
        )
        imagePipeline.captureConfigMap[request1] = response1
        imagePipeline.captureConfigMap[request2] = response2

        // Act: offer 2 requests
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)

        // Assert:
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
        assertThat(imageCaptureControl.latestCaptureConfigs).isEqualTo(response1)
        assertThat(takePictureManager.mNewRequests.single()).isEqualTo(request2)

        // Act: invoke image captured so TakePictureManager can issue another request.
        imagePipeline.getProcessingRequest(request1).onImageCaptured()

        // Assert: imageCaptureControl was invoked in the exact given order.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
        assertThat(imageCaptureControl.latestCaptureConfigs).isEqualTo(response2)
        assertThat(takePictureManager.mNewRequests).isEmpty()
    }

    /**
     * When post-processing results come back in a different order as they are being sent.
     */
    @Test
    fun pipelineReturnsMultipleResponsesOutOfOrder_appReceivesCorrectly() {
        // Arrange: setup 3 requests and their responses in the order of 1->2->3.
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val request3 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)
        takePictureManager.offerRequest(request3)
        val response1 = ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "", null)
        val response2 = OutputFileResults(null)
        val response3 = FakeImageProxy(FakeImageInfo())
        imagePipeline.getProcessingRequest(request1).onImageCaptured()
        imagePipeline.getProcessingRequest(request2).onImageCaptured()
        imagePipeline.getProcessingRequest(request3).onImageCaptured()

        // Act: send the responses in the order of 3->1->2
        imagePipeline.getProcessingRequest(request3).onFinalResult(response3)
        imagePipeline.getProcessingRequest(request1).onProcessFailure(response1)
        imagePipeline.getProcessingRequest(request2).onFinalResult(response2)
        shadowOf(getMainLooper()).idle()

        // Assert: responses received correctly
        assertThat(request1.exceptionReceived).isEqualTo(response1)
        assertThat(request2.fileReceived).isEqualTo(response2)
        assertThat(request3.imageReceived).isEqualTo(response3)
    }

    @Test
    fun pipelineReturnsError_appReceivesError() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)

        // Act: send exception via ImagePipeline
        val exception = ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "", null)
        imagePipeline.getProcessingRequest(request).onImageCaptured()
        imagePipeline.getProcessingRequest(request).onProcessFailure(exception)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.exceptionReceived).isEqualTo(exception)
    }

    @Test
    fun pipelineReturnsOnDiskResult_receivedByApp() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        takePictureManager.offerRequest(request)

        // Act: send OutputFileResults via ImagePipeline
        val outputFileResults = OutputFileResults(null)
        imagePipeline.getProcessingRequest(request).onImageCaptured()
        imagePipeline.getProcessingRequest(request).onFinalResult(outputFileResults)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.fileReceived).isEqualTo(outputFileResults)
    }

    @Test
    fun pipelineReturnsInMemoryResult_receivedByApp() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)

        // Act: send ImageProxy via ImagePipeline
        val image = FakeImageProxy(FakeImageInfo())
        imagePipeline.getProcessingRequest(request).onImageCaptured()
        imagePipeline.getProcessingRequest(request).onFinalResult(image)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.imageReceived).isEqualTo(image)
    }
}