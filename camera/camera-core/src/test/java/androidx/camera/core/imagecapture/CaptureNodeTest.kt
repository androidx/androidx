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
import android.graphics.ImageFormat.RAW_SENSOR
import android.graphics.ImageFormat.YUV_420_888
import android.os.Build
import android.os.Looper.getMainLooper
import android.util.Pair
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxyProvider
import androidx.camera.core.imagecapture.Utils.createCaptureBundle
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.impl.fakes.FakeImageProxy
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [CaptureNode]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CaptureNodeTest {

    private val imagePropagated = mutableListOf<ImageProxy>()

    private lateinit var captureNodeIn: CaptureNode.In
    private lateinit var captureNodeOut: ProcessingNode.In

    private val captureNode = CaptureNode()

    @Before
    fun setUp() {
        captureNodeIn = CaptureNode.In.of(Size(10, 10), JPEG, listOf(JPEG), false, null)
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.edge.setListener { imagePropagated.add(it.imageProxy) }
    }

    @After
    fun tearDown() {
        captureNode.release()
    }

    @Test
    fun isNotSimultaneousCapture_createOneImageReaders() {
        // Arrange: enable isSimultaneousCaptureEnabled in CaptureNode.In
        val input = CaptureNode.In.of(Size(10, 10), RAW_SENSOR, listOf(RAW_SENSOR), false, null)

        // Act: transform.
        val node = CaptureNode()
        val output = node.transform(input)

        // Assert
        assertThat(output.outputFormats.size).isEqualTo(1)
        assertThat(output.outputFormats[0]).isEqualTo(RAW_SENSOR)
        assertThat(input.surface).isNotNull()
        assertThat(input.cameraCaptureCallback).isNotNull()
        assertThat(input.secondarySurface).isNull()
        assertThat(input.secondaryCameraCaptureCallback).isNull()
    }

    @Test
    fun isSimultaneousCapture_createTwoImageReaders() {
        // Arrange: enable isSimultaneousCaptureEnabled in CaptureNode.In
        val input =
            CaptureNode.In.of(Size(10, 10), RAW_SENSOR, listOf(RAW_SENSOR, JPEG), false, null)

        // Act: transform.
        val node = CaptureNode()
        val output = node.transform(input)

        // Assert
        assertThat(output.outputFormats.size).isEqualTo(2)
        assertThat(input.surface).isNotNull()
        assertThat(input.cameraCaptureCallback).isNotNull()
        assertThat(input.secondarySurface).isNotNull()
        assertThat(input.secondaryCameraCaptureCallback).isNotNull()
    }

    @Test
    fun hasImageReaderProxyProvider_useTheProvidedImageReader() {
        // Arrange: create a fake ImageReaderProxyProvider.
        val imageReader = FakeImageReaderProxy(CaptureNode.MAX_IMAGES)
        val imageReaderProvider = ImageReaderProxyProvider { _, _, _, _, _ -> imageReader }
        val input = CaptureNode.In.of(Size(10, 10), JPEG, listOf(JPEG), false, imageReaderProvider)
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
    fun requestAborted_imageArrivesBeforeNextRequestAvailable() {
        // Arrange: Configure the CaptureNode with isVirtualCamera = true and FakeImageReaderProxy
        // create 2 requests: A and B and prepare TagBundles.
        val captureNode = CaptureNode()
        val imageReaderProxy = FakeImageReaderProxy(2)
        captureNodeIn =
            CaptureNode.In.of(
                Size(10, 10),
                JPEG,
                listOf(JPEG),
                /* isVirtualCamera */ true,
                { _, _, _, _, _ -> imageReaderProxy }
            )
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.edge.setListener { imagePropagated.add(it.imageProxy) }

        // Create request A
        val captureBundleA = createCaptureBundle(intArrayOf(1))
        val callbackA = FakeTakePictureCallback()
        var captureFutureCompleterA: CallbackToFutureAdapter.Completer<Void>? = null
        val captureFuture1 =
            CallbackToFutureAdapter.getFuture {
                captureFutureCompleterA = it
                "test"
            }
        val requestA = FakeProcessingRequest(captureBundleA, callbackA, captureFuture1)
        val tagBundleKeyA = captureBundleA.hashCode().toString()
        val tagBundleA = TagBundle.create(Pair(tagBundleKeyA, /* stage id */ 1))

        // Create request B
        val captureBundleB = createCaptureBundle(intArrayOf(2))
        val callbackB = FakeTakePictureCallback()
        val requestB =
            FakeProcessingRequest(captureBundleB, callbackB, Futures.immediateFuture(null))
        val tagBundleKeyB = captureBundleB.hashCode().toString()
        val tagBundleB = TagBundle.create(Pair(tagBundleKeyB, /* stage id */ 2))

        // Act: send request A and abort it
        captureNodeIn.requestEdge.accept(requestA)
        captureFutureCompleterA!!.setException(
            ImageCaptureException(ImageCapture.ERROR_CAMERA_CLOSED, "aborted", null)
        )

        // Image from requestA arrives before sending request B
        val imageA = imageReaderProxy.triggerImageAvailableSync(tagBundleA)
        // send request B
        captureNodeIn.requestEdge.accept(requestB)
        // Image from requestB arrives
        val imageB = imageReaderProxy.triggerImageAvailableSync(tagBundleB)

        // Assert: onImageCaptured is not invoked on requestA and its image should be closed.
        assertThat(callbackA.onImageCapturedCalled).isFalse()
        assertThat(imageA.isClosed).isTrue()

        // Assert: onImageCaptured is invoked on requestB and its image is propagated.
        assertThat(callbackB.onImageCapturedCalled).isTrue()
        assertThat(imageB.isClosed).isFalse()
        assertThat(imagePropagated.size).isEqualTo(1)
        assertThat(imagePropagated.get(0).imageInfo.tagBundle.getTag(tagBundleKeyB)).isEqualTo(2)
    }

    @Test
    fun requestNoProgressSent_ensureProgress100IsNotInvoked() {
        // Arrange: Configure the CaptureNode and submit fake request.
        val captureNode = CaptureNode()
        val imageReaderProxy = FakeImageReaderProxy(2)
        captureNodeIn =
            CaptureNode.In.of(
                Size(10, 10),
                JPEG,
                listOf(JPEG),
                /* isVirtualCamera */ true,
                { _, _, _, _, _ -> imageReaderProxy }
            )
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.edge.setListener { imagePropagated.add(it.imageProxy) }

        // Create request
        val captureBundle = createCaptureBundle(intArrayOf(1))
        val takePictureCallback = FakeTakePictureCallback()
        val request = FakeProcessingRequest(captureBundle, takePictureCallback)
        val tagBundle =
            TagBundle.create(Pair(captureBundle.hashCode().toString(), /* stage id */ 1))

        captureNodeIn.requestEdge.accept(request)
        imageReaderProxy.triggerImageAvailableSync(tagBundle)
        shadowOf(getMainLooper()).idle()

        // Assert: captureProcessProgress is not invoked.
        assertThat(takePictureCallback.captureProcessProgressList).isEmpty()
    }

    @Test
    fun requestSentNon100Progress_progress100isInvoked() {
        // Arrange: Configure the CaptureNode and submit fake request.
        val captureNode = CaptureNode()
        val imageReaderProxy = FakeImageReaderProxy(2)
        captureNodeIn =
            CaptureNode.In.of(
                Size(10, 10),
                JPEG,
                listOf(JPEG),
                /* isVirtualCamera */ true,
                { _, _, _, _, _ -> imageReaderProxy }
            )
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.edge.setListener { imagePropagated.add(it.imageProxy) }

        // Create request
        val captureBundle = createCaptureBundle(intArrayOf(1))
        val takePictureCallback = FakeTakePictureCallback()
        val request = FakeProcessingRequest(captureBundle, takePictureCallback)
        val tagBundle =
            TagBundle.create(Pair(captureBundle.hashCode().toString(), /* stage id */ 1))

        captureNodeIn.requestEdge.accept(request)
        // Act: notify onCaptureProcessProgressed that is not 100 after request starts.
        captureNodeIn.cameraCaptureCallback.onCaptureProcessProgressed(1, 50)
        captureNodeIn.cameraCaptureCallback.onCaptureProcessProgressed(1, 90)

        imageReaderProxy.triggerImageAvailableSync(tagBundle)
        shadowOf(getMainLooper()).idle()

        // Assert: captureProcessProgress 100 is also invoked.
        assertThat(takePictureCallback.captureProcessProgressList).containsExactly(50, 90, 100)
    }

    @Test
    fun requestSent100Progress_ensureNoDuplicate() {
        // Arrange: Configure the CaptureNode and submit fake request.
        val captureNode = CaptureNode()
        val imageReaderProxy = FakeImageReaderProxy(2)
        captureNodeIn =
            CaptureNode.In.of(
                Size(10, 10),
                JPEG,
                listOf(JPEG),
                /* isVirtualCamera */ true,
                { _, _, _, _, _ -> imageReaderProxy }
            )
        captureNodeOut = captureNode.transform(captureNodeIn)
        captureNodeOut.edge.setListener { imagePropagated.add(it.imageProxy) }

        // Create request
        val captureBundle = createCaptureBundle(intArrayOf(1))
        val takePictureCallback = FakeTakePictureCallback()
        val request = FakeProcessingRequest(captureBundle, takePictureCallback)
        val tagBundle =
            TagBundle.create(Pair(captureBundle.hashCode().toString(), /* stage id */ 1))

        captureNodeIn.requestEdge.accept(request)
        // Act: notify onCaptureProcessProgressed that is not 100 after request starts.
        captureNodeIn.cameraCaptureCallback.onCaptureProcessProgressed(1, 100)

        imageReaderProxy.triggerImageAvailableSync(tagBundle)
        shadowOf(getMainLooper()).idle()

        // Assert: captureProcessProgress 100 is only sent once.
        assertThat(takePictureCallback.captureProcessProgressList).containsExactly(100)
    }

    private fun FakeImageReaderProxy.triggerImageAvailableSync(
        tagBundle: TagBundle,
    ): FakeImageProxy {
        val image = triggerImageAvailable(tagBundle, 100L)
        shadowOf(getMainLooper()).idle()
        return image
    }

    @Test
    fun transformWithPostviewSizeAndYuv() {
        // Arrange: set the postviewSize to the CaptureNode.In
        val postviewSize = Size(640, 480)

        val input =
            CaptureNode.In.of(
                Size(10, 10),
                JPEG,
                listOf(JPEG),
                false,
                null,
                postviewSize,
                YUV_420_888
            )

        // Act: transform.
        val node = CaptureNode()
        node.transform(input)

        // Assert: postview surface is created
        assertThat(input.postviewSurface).isNotNull()
        assertThat(input.postviewSurface!!.prescribedSize).isEqualTo(postviewSize)
        assertThat(input.postviewSurface!!.prescribedStreamFormat).isEqualTo(YUV_420_888)
        node.release()
    }

    @Test
    fun transformWithPostviewSizeAndJpeg() {
        // Arrange: set the postviewSize to the CaptureNode.In
        val postviewSize = Size(640, 480)

        val input =
            CaptureNode.In.of(Size(10, 10), JPEG, listOf(JPEG), false, null, postviewSize, JPEG)

        // Act: transform.
        val node = CaptureNode()
        node.transform(input)

        // Assert: postview surface is created
        assertThat(input.postviewSurface).isNotNull()
        assertThat(input.postviewSurface!!.prescribedSize).isEqualTo(postviewSize)
        assertThat(input.postviewSurface!!.prescribedStreamFormat).isEqualTo(JPEG)
        node.release()
    }
}
