/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.media.Image
import android.media.ImageReader
import android.view.Surface
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.StreamId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class AndroidImageReaderTest {
    private val streamId = StreamId(32)
    private val outputId = OutputId(42)

    @Test
    fun onImageAvailable_succeedsAfterFix() {
        val mockImageReader = mock<ImageReader>()
        val mockImage = mock<Image>()
        val mockSurface = mock<Surface>()
        whenever(mockImageReader.acquireNextImage()).thenReturn(mockImage)
        whenever(mockImageReader.surface).thenReturn(mockSurface)

        val androidImageReader =
            AndroidImageReader(
                imageReader = mockImageReader,
                capacity = 10,
                usageFlags = null,
                streamId = streamId,
                outputId = outputId,
            )

        val testListener =
            object : ImageReaderWrapper.OnImageListener {
                var imageReceived: ImageWrapper? = null

                override fun onImage(streamId: StreamId, outputId: OutputId, image: ImageWrapper) {
                    imageReceived = image
                }
            }
        androidImageReader.onImageListener = testListener

        androidImageReader.onImageAvailable(mockImageReader)

        assertThat(testListener.imageReceived).isNotNull()
    }
}
