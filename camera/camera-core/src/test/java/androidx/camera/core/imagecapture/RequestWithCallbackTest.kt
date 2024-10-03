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
import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCapture.OnImageCapturedCallback
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.camera.testing.impl.fakes.FakeImageProxy
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [RequestWithCallback] */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RequestWithCallbackTest {

    private lateinit var abortError: ImageCaptureException
    private lateinit var otherError: ImageCaptureException
    private lateinit var imageResult: ImageProxy
    private lateinit var fileResult: ImageCapture.OutputFileResults
    private lateinit var retryControl: FakeRetryControl
    private lateinit var captureRequestFuture: ListenableFuture<Void>

    @Before
    fun setUp() {
        abortError = ImageCaptureException(ERROR_CAMERA_CLOSED, "", null)
        otherError = ImageCaptureException(ERROR_CAPTURE_FAILED, "", null)
        imageResult = FakeImageProxy(FakeImageInfo())
        fileResult = ImageCapture.OutputFileResults(null, ImageFormat.JPEG)
        retryControl = FakeRetryControl()
        captureRequestFuture = CallbackToFutureAdapter.getFuture { "captureRequestFuture" }
    }

    @After
    fun tearDown() {
        captureRequestFuture.cancel(true)
    }

    @Test
    fun failCaptureWithRemainingRetries_requestIsRetried() {
        // Arrange: create a request the a retry counter of 1.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        request.incrementRetryCounter()
        assertThat(request.remainingRetries).isEqualTo(1)
        val callback = RequestWithCallback(request, retryControl)

        // Act.
        callback.onCaptureFailure(otherError)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(retryControl.retriedRequest).isEqualTo(request)
        assertThat(request.remainingRetries).isEqualTo(0)
    }

    @Test
    fun failCaptureWithoutRemainingRetries_requestNotRetried() {
        // Arrange: create a request the a retry counter of 0.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        assertThat(request.remainingRetries).isEqualTo(0)
        val callback = RequestWithCallback(request, retryControl)

        // Act.
        callback.onCaptureFailure(otherError)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(retryControl.retriedRequest).isNull()
        assertThat(request.exceptionReceived).isEqualTo(otherError)
    }

    @Test
    fun abortAndRetryAfterSuccess_abortIsNoOp() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)

        // Act: deliver result then abort
        callback.onImageCaptured()
        callback.onFinalResult(imageResult)
        callback.abortSilentlyAndRetry()
        shadowOf(getMainLooper()).idle()

        // Assert: retry is not queued.
        assertThat(retryControl.retriedRequest).isNull()
    }

    @Test
    fun abortAndFailAfterFail_abortIsNoOp() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)

        // Fail request then abort
        callback.onCaptureFailure(otherError)
        callback.abortAndSendErrorToApp(abortError)
        shadowOf(getMainLooper()).idle()

        // Assert:
        assertThat(request.exceptionReceived).isEqualTo(otherError)
    }

    @Test
    fun abortRequestThenSendOtherErrors_receiveAbortError() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)
        callback.setCaptureRequestFuture(captureRequestFuture)
        // Act.
        callback.abortAndSendErrorToApp(abortError)
        callback.onCaptureFailure(otherError)
        callback.onProcessFailure(otherError)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.exceptionReceived).isEqualTo(abortError)
        assertThat(captureRequestFuture.isCancelled).isTrue()
    }

    @Test
    fun sendOnCaptureStarted_receiveInMemoryCallback() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)
        // Act.
        callback.onCaptureStarted()
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.captureStarted).isTrue()
    }

    @Test
    fun sendOnCaptureStarted_receiveOnDiskCallback() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val callback = RequestWithCallback(request, retryControl)
        // Act.
        callback.onCaptureStarted()
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.captureStarted).isTrue()
    }

    @Test
    fun sendOnCaptureProcessProgressed_receiveInMemoryCallback() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)
        // Act.
        callback.onCaptureProcessProgressed(20)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.captureProcessProgress).isEqualTo(20)
    }

    @Test
    fun sendOnCaptureProcessProgressed_receiveOnDiskCallback() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val callback = RequestWithCallback(request, retryControl)
        // Act.
        callback.onCaptureProcessProgressed(20)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.captureProcessProgress).isEqualTo(20)
    }

    @Test
    fun sendOnCaptureStartedTwice_receiveInMemoryCallbackOnce() {
        // Arrange.
        var startedCount = 0
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        request.setInMemoryCallback(
            object : OnImageCapturedCallback() {
                override fun onCaptureStarted() {
                    startedCount++
                }
            }
        )
        val callback = RequestWithCallback(request, retryControl)
        // Act: call onCaptureStarted twice intentionally.
        callback.onCaptureStarted()
        callback.onCaptureStarted()
        shadowOf(getMainLooper()).idle()
        // Assert: receive only once in the in memory callback.
        assertThat(startedCount).isEqualTo(1)
    }

    @Test
    fun sendOnCaptureStartedTwice_receiveOnDiskCallbackOnce() {
        // Arrange.
        var startedCount = 0
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        request.setOnDiskCallback(
            object : OnImageSavedCallback {
                override fun onCaptureStarted() {
                    startedCount++
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {}

                override fun onError(exception: ImageCaptureException) {}
            }
        )
        val callback = RequestWithCallback(request, retryControl)
        // Act: call onCaptureStarted twice intentionally.
        callback.onCaptureStarted()
        callback.onCaptureStarted()
        shadowOf(getMainLooper()).idle()
        // Assert: receive only once in the on disk callback.
        assertThat(startedCount).isEqualTo(1)
    }

    @Test
    fun sendInMemoryResult_receiveResult() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)
        // Act.
        callback.onImageCaptured()
        callback.onFinalResult(imageResult)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.imageReceived).isEqualTo(imageResult)
    }

    @Test
    fun abortRequestAndSendInMemoryResult_doNotReceiveResult() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request, retryControl)
        callback.setCaptureRequestFuture(captureRequestFuture)
        // Act.
        callback.abortAndSendErrorToApp(abortError)
        callback.onFinalResult(imageResult)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.imageReceived).isNull()
        assertThat(captureRequestFuture.isCancelled).isTrue()
    }

    @Test
    fun sendOnDiskResult_receiveResult() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val callback = RequestWithCallback(request, retryControl)
        // Act.
        callback.onImageCaptured()
        callback.onFinalResult(fileResult)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.fileReceived).isEqualTo(fileResult)
    }

    @Test
    fun abortRequestAndSendOnDiskResult_doNotReceiveResult() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val callback = RequestWithCallback(request, retryControl)
        callback.setCaptureRequestFuture(captureRequestFuture)
        // Act.
        callback.abortAndSendErrorToApp(abortError)
        callback.onFinalResult(imageResult)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.imageReceived).isNull()
        assertThat(captureRequestFuture.isCancelled).isTrue()
    }
}
