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
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [RequestWithCallback]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class RequestWithCallbackTest {

    private lateinit var abortError: ImageCaptureException
    private lateinit var otherError: ImageCaptureException
    private lateinit var imageResult: ImageProxy
    private lateinit var fileResult: ImageCapture.OutputFileResults

    @Before
    fun setUp() {
        abortError = ImageCaptureException(ERROR_CAMERA_CLOSED, "", null)
        otherError = ImageCaptureException(ERROR_CAPTURE_FAILED, "", null)
        imageResult = FakeImageProxy(FakeImageInfo())
        fileResult = ImageCapture.OutputFileResults(null)
    }

    @Test
    fun abortRequestThenSendOtherErrors_receiveAbortError() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request)
        // Act.
        callback.abort(abortError)
        callback.onCaptureFailure(otherError)
        callback.onProcessFailure(otherError)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.exceptionReceived).isEqualTo(abortError)
    }

    @Test
    fun sendInMemoryResult_receiveResult() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val callback = RequestWithCallback(request)
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
        val callback = RequestWithCallback(request)
        // Act.
        callback.abort(abortError)
        callback.onFinalResult(imageResult)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.imageReceived).isNull()
    }

    @Test
    fun sendOnDiskResult_receiveResult() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val callback = RequestWithCallback(request)
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
        val callback = RequestWithCallback(request)
        // Act.
        callback.abort(abortError)
        callback.onFinalResult(imageResult)
        shadowOf(getMainLooper()).idle()
        // Assert.
        assertThat(request.imageReceived).isNull()
    }
}