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
package androidx.camera.testing

import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.Color
import android.graphics.Color.BLUE
import android.graphics.Color.GREEN
import android.graphics.Color.RED
import android.graphics.Color.YELLOW
import android.graphics.ImageFormat
import android.graphics.Rect
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.testing.impl.TestImageUtil.COLOR_BLACK
import androidx.camera.testing.impl.TestImageUtil.COLOR_DARK_GRAY
import androidx.camera.testing.impl.TestImageUtil.COLOR_GRAY
import androidx.camera.testing.impl.TestImageUtil.COLOR_WHITE
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.createGainmap
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.createJpegrBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegrFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [TestImageUtil] */
@SmallTest
@RunWith(AndroidJUnit4::class)
class TestImageUtilDeviceTest {

    companion object {
        private const val WIDTH = 640
        private const val HEIGHT = 480
    }

    @Test
    fun createTestJpeg_verifyColor() {
        // Act.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val bitmap = decodeByteArray(jpegBytes, 0, jpegBytes.size)
        // Assert.
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), RED)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, 640, 240), GREEN)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 241, 640, 480), YELLOW)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(0, 241, 320, 480), BLUE)).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun createTestJpegr_verifyColorAndGainmapContent() {
        // Act.
        val jpegBytes = createJpegrBytes(WIDTH, HEIGHT)
        val bitmap = decodeByteArray(jpegBytes, 0, jpegBytes.size)
        assertThat(bitmap.hasGainmap()).isTrue()
        val contents = bitmap.gainmap!!.gainmapContents
        // Assert.
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), RED)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, 640, 240), GREEN)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 241, 640, 480), YELLOW)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(0, 241, 320, 480), BLUE)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 0, 640, 120), COLOR_BLACK)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 121, 640, 240), COLOR_DARK_GRAY)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 241, 640, 360), COLOR_GRAY)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 361, 640, 480), COLOR_WHITE)).isEqualTo(0)
    }

    @Test
    fun createBitmap_verifyColor() {
        // Act.
        val bitmap = createBitmap(WIDTH, HEIGHT)
        // Assert.
        assertThat(getAverageDiff(bitmap, Rect(0, 0, 320, 240), RED)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 0, 640, 240), GREEN)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(321, 241, 640, 480), YELLOW)).isEqualTo(0)
        assertThat(getAverageDiff(bitmap, Rect(0, 241, 320, 480), BLUE)).isEqualTo(0)
    }

    @Test
    fun createBitmap_verifyWithIncorrectColor() {
        // The color is supposed to be RED.
        assertThat(getAverageDiff(createBitmap(WIDTH, HEIGHT), Rect(0, 0, 320, 240), Color.CYAN))
            .isEqualTo(255)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun createGainmap_verifyContents() {
        // Act.
        val contents = createGainmap(WIDTH, HEIGHT).gainmapContents
        // Assert.
        assertThat(getAverageDiff(contents, Rect(0, 0, 640, 120), COLOR_BLACK)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 121, 640, 240), COLOR_DARK_GRAY)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 241, 640, 360), COLOR_GRAY)).isEqualTo(0)
        assertThat(getAverageDiff(contents, Rect(0, 361, 640, 480), COLOR_WHITE)).isEqualTo(0)
    }

    @Test
    fun createJpegImageProxy_verifyContent() {
        // Arrange: create JPEG bytes.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        // Act: create ImageProxy output the JPEG bytes.
        val image = createJpegFakeImageProxy(jpegBytes)
        // Act: get the image out of the ImageProxy and verify its content.
        val restoredJpegBytes = jpegImageToJpegByteArray(image)
        val diff =
            getAverageDiff(
                decodeByteArray(jpegBytes, 0, jpegBytes.size),
                decodeByteArray(restoredJpegBytes, 0, restoredJpegBytes.size),
            )
        assertThat(diff).isEqualTo(0)
        assertThat(image.format).isEqualTo(ImageFormat.JPEG)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun createJpegrImageProxy_verifyContent() {
        // Arrange: create JPEG bytes of JPEG/R.
        val jpegBytes = createJpegrBytes(WIDTH, HEIGHT)
        // Act: create ImageProxy output the JPEG bytes.
        val image = createJpegrFakeImageProxy(jpegBytes)
        // Act: get the image out of the ImageProxy and verify its content.
        val restoredJpegBytes = jpegImageToJpegByteArray(image)
        val diff =
            getAverageDiff(
                decodeByteArray(jpegBytes, 0, jpegBytes.size),
                decodeByteArray(restoredJpegBytes, 0, restoredJpegBytes.size),
            )
        assertThat(diff).isEqualTo(0)
        assertThat(image.format).isEqualTo(ImageFormat.JPEG_R)
    }
}
