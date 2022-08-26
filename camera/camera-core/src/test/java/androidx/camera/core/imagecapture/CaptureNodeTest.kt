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
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.createCaptureBundle
import androidx.camera.core.imagecapture.Utils.createFakeImage
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [CaptureNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CaptureNodeTest {

    private val imagePropagated = mutableListOf<ImageProxy>()
    private val requestsPropagated = mutableListOf<ProcessingRequest>()

    private lateinit var captureNodeIn: CaptureNode.In
    private lateinit var captureNodeOut: CaptureNode.Out

    private val captureNode = CaptureNode()

    @Before
    fun setUp() {
        captureNodeIn = CaptureNode.In.of(Size(10, 10), ImageFormat.JPEG)
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.imageEdge.setListener {
            imagePropagated.add(it)
        }
        captureNodeOut.requestEdge.setListener {
            requestsPropagated.add(it)
        }
    }

    @Test
    fun send2RequestsAndImages_requestsReceived() {
        // Arrange: create 2 requests: A and B.
        // A has two stages: 1 and 2.
        val captureBundleA = createCaptureBundle(intArrayOf(1, 2))
        val callbackA = FakeTakePictureCallback()
        val requestA = FakeProcessingRequest(captureBundleA, callbackA)
        val tagBundleKeyA = captureBundleA.hashCode().toString()
        val imageA1 = createFakeImage(tagBundleKeyA, 1)
        val imageA2 = createFakeImage(tagBundleKeyA, 2)
        // B has one stage: 1
        val captureBundleB = createCaptureBundle(intArrayOf(1))
        val callbackB = FakeTakePictureCallback()
        val requestB = FakeProcessingRequest(captureBundleB, callbackB)
        val tagBundleKeyB = captureBundleB.hashCode().toString()
        val imageB1 = createFakeImage(tagBundleKeyB, 1)

        // Act: send request A.
        captureNode.onRequestAvailable(requestA)
        assertThat(callbackA.onImageCapturedCalled).isFalse()
        captureNode.onImageProxyAvailable(imageA1)
        assertThat(callbackA.onImageCapturedCalled).isFalse()
        captureNode.onImageProxyAvailable(imageA2)

        // Assert: A is received.
        assertThat(callbackA.onImageCapturedCalled).isTrue()
        assertThat(requestsPropagated).containsExactly(requestA)
        assertThat(imagePropagated).containsExactly(imageA1, imageA2)

        // Act: send request B.
        captureNode.onRequestAvailable(requestB)
        assertThat(callbackB.onImageCapturedCalled).isFalse()
        captureNode.onImageProxyAvailable(imageB1)
        assertThat(callbackB.onImageCapturedCalled).isTrue()

        // Assert: B is received.
        assertThat(callbackB.onImageCapturedCalled).isTrue()
        assertThat(requestsPropagated).containsExactly(requestA, requestB)
        assertThat(imagePropagated).containsExactly(imageA1, imageA2, imageB1)
    }

    @Test
    fun receiveImageFirst_onCaptureInvoked() {
        // Arrange: create a request with two stages: 1 and 2.
        val captureBundle = createCaptureBundle(intArrayOf(1, 2))
        val callback = FakeTakePictureCallback()
        val request = FakeProcessingRequest(captureBundle, callback)
        val tagBundleKey = captureBundle.hashCode().toString()
        val image1 = createFakeImage(tagBundleKey, 1)
        val image2 = createFakeImage(tagBundleKey, 2)

        // Act: send image1, request, then image2
        captureNode.onImageProxyAvailable(image1)
        assertThat(callback.onImageCapturedCalled).isFalse()
        assertThat(imagePropagated).isEmpty()
        assertThat(requestsPropagated).isEmpty()

        captureNode.onRequestAvailable(request)
        assertThat(callback.onImageCapturedCalled).isFalse()
        assertThat(requestsPropagated).containsExactly(request)
        assertThat(imagePropagated).containsExactly(image1)

        captureNode.onImageProxyAvailable(image2)
        assertThat(callback.onImageCapturedCalled).isTrue()
        assertThat(requestsPropagated).containsExactly(request)
        assertThat(imagePropagated).containsExactly(image1, image2)
    }

    @Test(expected = IllegalStateException::class)
    fun receiveRequestWhenThePreviousOneUnfinished_throwException() {
        // Arrange: create 2 requests: A and B.
        val requestA =
            FakeProcessingRequest(createCaptureBundle(intArrayOf(1)), FakeTakePictureCallback())
        val requestB =
            FakeProcessingRequest(createCaptureBundle(intArrayOf(1)), FakeTakePictureCallback())
        // Act: Send B without A being finished.
        captureNode.onRequestAvailable(requestA)
        captureNode.onRequestAvailable(requestB)
    }
}