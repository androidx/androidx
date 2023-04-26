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
import androidx.camera.core.imagecapture.Utils.createCaptureBundle
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [SingleBundlingNode].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SingleBundlingNodeTest {

    private val packetPropagated = mutableListOf<ProcessingNode.InputPacket>()
    private lateinit var captureNodeOut: CaptureNode.Out
    private lateinit var matchingNodeOut: ProcessingNode.In

    private val node = SingleBundlingNode()

    @Before
    fun setUp() {
        captureNodeOut = CaptureNode.Out.of(ImageFormat.JPEG, ImageFormat.JPEG, false)
        matchingNodeOut = node.transform(captureNodeOut)
        matchingNodeOut.edge.setListener {
            packetPropagated.add(it)
        }
    }

    @Test
    fun setRequestAndImageTwice_twoPacketsPropagated() {
        // Arrange: create 2 requests: A and B.
        val captureBundleA = createCaptureBundle(intArrayOf(1))
        val requestA = FakeProcessingRequest(
            captureBundleA,
            FakeTakePictureCallback(),
            Futures.immediateFuture(null)
        )
        val tagBundleKeyA = captureBundleA.hashCode().toString()
        val imageA1 = Utils.createFakeImage(tagBundleKeyA, 1)
        val captureBundleB = createCaptureBundle(intArrayOf(1))
        val requestB = FakeProcessingRequest(
            captureBundleB,
            FakeTakePictureCallback(),
            Futures.immediateFuture(null)
        )
        val tagBundleKeyB = captureBundleB.hashCode().toString()
        val imageB1 = Utils.createFakeImage(tagBundleKeyB, 1)

        // Act: send packet A and its image.
        captureNodeOut.requestEdge.accept(requestA)
        captureNodeOut.imageEdge.accept(imageA1)

        // Assert: packet A is propagated.
        val packetA = packetPropagated.single()
        assertThat(packetA.imageProxy).isEqualTo(imageA1)
        assertThat(packetA.processingRequest).isEqualTo(requestA)

        // Act: send packet B and its image.
        captureNodeOut.requestEdge.accept(requestB)
        captureNodeOut.imageEdge.accept(imageB1)

        // Assert: packet B is propagated.
        val packetB = packetPropagated[1]
        assertThat(packetB.imageProxy).isEqualTo(imageB1)
        assertThat(packetB.processingRequest).isEqualTo(requestB)
    }
}