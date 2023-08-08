/*
 * Copyright 2023 The Android Open Source Project
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
import android.util.Pair
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.createProcessingRequest
import androidx.camera.core.impl.ImageReaderProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.core.internal.CameraCaptureResultImageInfo
import androidx.camera.testing.impl.fakes.FakeImageReaderProxy
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [NoMetadataImageReader].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class NoMetadataImageReaderTest {
    @Test
    fun acquireLatestImage_receiveImageWithTimestamp() {
        verifyReceiveImageWithTimestamp { imageReader -> imageReader.acquireLatestImage()!! }
    }

    @Test
    fun acquireNextImage_receiveImageWithTimestamp() {
        verifyReceiveImageWithTimestamp { imageReader -> imageReader.acquireNextImage()!! }
    }

    private fun verifyReceiveImageWithTimestamp(
        getImageProxy: Function1<ImageReaderProxy, ImageProxy>
    ) {
        // Arrange: create a NoMetadataImageReader and set a listener to receive the image.
        val fakeImageReader = FakeImageReaderProxy(3)
        val noMetadataImageReader = NoMetadataImageReader(fakeImageReader)
        var image: ImageProxy? = null
        noMetadataImageReader.setOnImageAvailableListener(
            { imageReader ->
                image = getImageProxy(imageReader)
            },
            directExecutor()
        )
        noMetadataImageReader.acceptProcessingRequest(createProcessingRequest())
        // Act: trigger the image available callback.
        fakeImageReader.triggerImageAvailable(TagBundle.create(Pair("key", "value")), 1L)
        // Assert: the image should be received with the timestamp.
        assertThat(image!!.imageInfo).isInstanceOf(CameraCaptureResultImageInfo::class.java)
        assertThat(image!!.imageInfo.timestamp).isEqualTo(1L)
    }
}
