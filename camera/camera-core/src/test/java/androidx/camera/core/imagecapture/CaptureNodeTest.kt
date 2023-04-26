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

import android.graphics.ImageFormat.JPEG
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Size
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxyProvider
import androidx.camera.core.imagecapture.Utils.createCaptureBundle
import androidx.camera.core.imagecapture.Utils.createFakeImage
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeImageReaderProxy
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
        captureNodeIn = CaptureNode.In.of(Size(10, 10), JPEG, JPEG, false, null)
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.imageEdge.setListener {
            imagePropagated.add(it)
        }
        captureNodeOut.requestEdge.setListener {
            requestsPropagated.add(it)
        }
    }

    @After
    fun tearDown() {
        captureNode.release()
    }

    @Test
    fun hasImageReaderProxyProvider_useTheProvidedImageReader() {
        // Arrange: create a fake ImageReaderProxyProvider.
        val imageReader = FakeImageReaderProxy(CaptureNode.MAX_IMAGES)
        val imageReaderProvider = ImageReaderProxyProvider { _, _, _, _, _ ->
            imageReader
        }
        val input = CaptureNode.In.of(Size(10, 10), JPEG, JPEG, false, imageReaderProvider)
        // Act: transform.
        val node = CaptureNode()
        node.transform(input)
        // Assert: ImageReaderProxyProvider is used.
        assertThat(input.surface.surface.get()).isEqualTo(imageReader.surface)
        node.release()
    }

    @Test
    fun release_imageReaderNotClosedUntilTermination() {
        // Arrange: increment the DeferrableSurface's use count to prevent it from being terminated.
        captureNode.inputEdge.surface.incrementUseCount()
        // Act: release.
        captureNode.release()
        shadowOf(getMainLooper()).idle()
        // Assert: ImageReader is not closed.
        assertThat(captureNode.mSafeCloseImageReaderProxy!!.isClosed).isFalse()

        // Act: decrease the use count. Now the DeferrableSurface will terminate.
        captureNode.inputEdge.surface.decrementUseCount()
        shadowOf(getMainLooper()).idle()
        // Assert: ImageReader is closed.
        assertThat(captureNode.mSafeCloseImageReaderProxy!!.isClosed).isTrue()
    }

    @Test
    fun transform_verifyInputSurface() {
        assertThat(captureNodeIn.surface.surface.get())
            .isEqualTo(captureNode.mSafeCloseImageReaderProxy!!.surface)
    }

    @Test
    fun send2RequestsAndImages_requestsReceived() {
        // Arrange: create 2 requests: A and B.
        // A has two stages: 1 and 2.
        val captureBundleA = createCaptureBundle(intArrayOf(1, 2))
        val callbackA = FakeTakePictureCallback()
        val requestA =
            FakeProcessingRequest(captureBundleA, callbackA, Futures.immediateFuture(null))
        val tagBundleKeyA = captureBundleA.hashCode().toString()
        val imageA1 = createFakeImage(tagBundleKeyA, 1)
        val imageA2 = createFakeImage(tagBundleKeyA, 2)
        // B has one stage: 1
        val captureBundleB = createCaptureBundle(intArrayOf(1))
        val callbackB = FakeTakePictureCallback()
        val requestB =
            FakeProcessingRequest(captureBundleB, callbackB, Futures.immediateFuture(null))
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

    @Test(expected = IllegalStateException::class)
    fun receiveRequestWhenThePreviousOneUnfinished_throwException() {
        // Arrange: create 2 requests: A and B.
        val requestA = FakeProcessingRequest(
            createCaptureBundle(intArrayOf(1)),
            FakeTakePictureCallback(),
            Futures.immediateFuture(null)
        )
        val requestB = FakeProcessingRequest(
            createCaptureBundle(intArrayOf(1)),
            FakeTakePictureCallback(),
            Futures.immediateFuture(null)
        )
        // Act: Send B without A being finished.
        captureNode.onRequestAvailable(requestA)
        captureNode.onRequestAvailable(requestB)
    }
}