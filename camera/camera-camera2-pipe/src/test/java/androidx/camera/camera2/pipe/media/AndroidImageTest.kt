/*
 * Copyright 2025 The Android Open Source Project
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

import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.media.Image
import android.os.Build
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests for [AndroidImage] */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class AndroidImageTest {
    private val mockImage: Image =
        mock() {
            whenever(it.height).thenReturn(IMAGE_HEIGHT)
            whenever(it.width).thenReturn(IMAGE_WIDTH)
            whenever(it.format).thenReturn(IMAGE_FORMAT)
            whenever(it.timestamp).thenReturn(IMAGE_TIMESTAMP)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                whenever(it.hardwareBuffer).thenReturn(mock<HardwareBuffer>())
            }

            var currentCropRect: Rect = IMAGE_CROP_RECT
            doAnswer { invocation -> currentCropRect = invocation.getArgument<Rect>(0) }
                .whenever(it)
                .cropRect = any()

            whenever(it.cropRect).thenAnswer { currentCropRect }
        }

    internal lateinit var androidImage: AndroidImage

    @Before
    fun setup() {
        androidImage = AndroidImage(mockImage)
    }

    @Test
    fun getHeight_returnsImageHeight() {
        assertThat(mockImage.height).isEqualTo(androidImage.height)
    }

    @Test
    fun getWidth_returnsImageWidth() {
        assertThat(mockImage.width).isEqualTo(androidImage.width)
    }

    @Test
    fun getFormat_returnsImageFormat() {
        assertThat(mockImage.format).isEqualTo(androidImage.format)
    }

    @Test
    fun getTimestamp_returnsTimestamp() {
        assertThat(mockImage.timestamp).isEqualTo(androidImage.timestamp)
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun unwrapAsHardwareBuffer_returnsImageHardwareBuffer() {
        val hardwareBuffer = androidImage.unwrapAs(HardwareBuffer::class)

        assertThat(hardwareBuffer).isNotNull()
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    fun getHardwareBuffer_returnsImageHardwareBuffer() {
        val hardwareBuffer = androidImage.hardwareBuffer

        assertThat(hardwareBuffer).isNotNull()
    }

    @Test
    fun getCropRect_returnsImageCropRect() {
        val cropRect = androidImage.cropRect

        assertThat(cropRect.left).isEqualTo(mockImage.cropRect.left)
        assertThat(cropRect.top).isEqualTo(mockImage.cropRect.top)
        assertThat(cropRect.right).isEqualTo(mockImage.cropRect.right)
        assertThat(cropRect.bottom).isEqualTo(mockImage.cropRect.bottom)
    }

    @Test
    fun setCropRect_setsNewRectValue() {
        androidImage.cropRect = NEW_CROP_RECT

        assertThat(androidImage.cropRect).isEqualTo(NEW_CROP_RECT)
    }

    companion object {
        private val IMAGE_HEIGHT: Int = 100
        private val IMAGE_WIDTH: Int = 200
        private val IMAGE_FORMAT: Int = 3
        private val IMAGE_TIMESTAMP: Long = 1234
        private val IMAGE_CROP_RECT: Rect = Rect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT)
        private val NEW_CROP_RECT: Rect =
            Rect(IMAGE_WIDTH / 2, IMAGE_HEIGHT / 2, IMAGE_WIDTH, IMAGE_HEIGHT)
    }
}
