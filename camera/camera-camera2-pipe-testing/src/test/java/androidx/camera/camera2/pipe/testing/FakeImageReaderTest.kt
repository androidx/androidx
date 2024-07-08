/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.os.Build
import android.util.Size
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamFormat
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class FakeImageReaderTest {
    private val imageReader =
        FakeImageReader.create(StreamFormat.PRIVATE, StreamId(32), OutputId(42), Size(640, 480), 10)

    @After
    fun cleanup() {
        imageReader.close()
    }

    @Test
    fun imageReaderCanBeClosed() {
        assertThat(imageReader.isClosed).isFalse()

        imageReader.close()

        assertThat(imageReader.isClosed).isTrue()
    }

    @Test
    fun imageReaderCanSimulateImages() {
        val fakeImage = imageReader.simulateImage(100)

        assertThat(fakeImage.width).isEqualTo(640)
        assertThat(fakeImage.height).isEqualTo(480)
        assertThat(fakeImage.format).isEqualTo(StreamFormat.PRIVATE.value)
        assertThat(fakeImage.timestamp).isEqualTo(100)
        assertThat(fakeImage.isClosed).isFalse()
    }

    @Test
    fun closingAnImageReaderDoesNotCloseImages() {
        val fakeImage = imageReader.simulateImage(100)
        imageReader.close()

        assertThat(fakeImage.isClosed).isFalse()
    }

    @Test
    fun imageReaderSimulatesANewImageEachTime() {
        val fakeImage1 = imageReader.simulateImage(100)
        val fakeImage2 = imageReader.simulateImage(100)

        assertThat(fakeImage1).isNotSameInstanceAs(fakeImage2)
        fakeImage2.close()

        assertThat(fakeImage1.isClosed).isFalse()
        assertThat(fakeImage2.isClosed).isTrue()
    }

    @Test
    fun simulatingImagesArePassedToListener() {
        val fakeListener = FakeOnImageListener()

        imageReader.simulateImage(100)
        imageReader.setOnImageListener(fakeListener)
        val image2 = imageReader.simulateImage(200)
        val image3 = imageReader.simulateImage(300)

        assertThat(fakeListener.onImageEvents.size).isEqualTo(2)

        assertThat(fakeListener.onImageEvents[0].image).isNotNull()
        assertThat(fakeListener.onImageEvents[0].image).isSameInstanceAs(image2)
        assertThat(fakeListener.onImageEvents[0].streamId).isEqualTo(StreamId(32))
        assertThat(fakeListener.onImageEvents[0].outputId).isEqualTo(OutputId(42))

        assertThat(fakeListener.onImageEvents[1].image).isNotNull()
        assertThat(fakeListener.onImageEvents[1].image).isSameInstanceAs(image3)
        assertThat(fakeListener.onImageEvents[1].streamId).isEqualTo(StreamId(32))
        assertThat(fakeListener.onImageEvents[1].outputId).isEqualTo(OutputId(42))
    }
}
