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

package androidx.camera.core.internal.utils

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PixelFormat
import androidx.camera.core.ImageProcessingUtil
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageReaderProxys
import androidx.camera.core.ImmutableImageInfo
import androidx.camera.core.SafeCloseImageReaderProxy
import androidx.camera.core.impl.TagBundle
import androidx.camera.testing.impl.TestImageUtil
import androidx.camera.testing.impl.fakes.FakeImageProxy
import androidx.camera.testing.impl.fakes.FakeJpegPlaneProxy
import androidx.camera.testing.impl.fakes.FakePlaneProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for {@link ImageUtil}.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class ImageUtilDeviceTest {

    private val WIDTH = 160
    private val HEIGHT = 120

    @Test(expected = IllegalArgumentException::class)
    fun createBitmapWithWrongRowStride_throwsException() {
        // Arrange.
        val planeProxy: ImageProxy.PlaneProxy =
            FakePlaneProxy(
                ImageUtil.createDirectByteBuffer(
                    TestImageUtil.createBitmap(
                        WIDTH,
                        HEIGHT
                    )
                ),
                (WIDTH - 1) * ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE, // Wrong row stride.
                ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE
            )
        // Act.
        ImageUtil.createBitmapFromPlane(
            arrayOf(planeProxy),
            WIDTH,
            HEIGHT
        )
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun createBitmapWithWrongPixelStride_throwsException() {
        // Arrange.
        val planeProxy: ImageProxy.PlaneProxy =
            FakePlaneProxy(
                ImageUtil.createDirectByteBuffer(
                    TestImageUtil.createBitmap(
                        WIDTH,
                        HEIGHT
                    )
                ),
                WIDTH * ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE,
                3
            ) // Wrong pixel stride.
        // Act.
        ImageUtil.createBitmapFromPlane(
            arrayOf(planeProxy),
            WIDTH,
            HEIGHT
        )
    }

    @Test
    fun createBitmapFromPlane_bitmapCreated() {
        // Arrange.
        val original = TestImageUtil.createBitmap(WIDTH, HEIGHT)
        val byteBuffer = ImageUtil.createDirectByteBuffer(original)
        // Move the position to test the case that the ByteBuffer needs rewinding.
        byteBuffer.position(byteBuffer.capacity())
        val planeProxy: ImageProxy.PlaneProxy =
            FakePlaneProxy(
                byteBuffer,
                WIDTH * ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE,
                ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE
            )
        // Act.
        val restored = ImageUtil.createBitmapFromPlane(
            arrayOf(planeProxy),
            WIDTH,
            HEIGHT
        )
        // Assert.
        Truth.assertThat(TestImageUtil.getAverageDiff(original, restored)).isEqualTo(0)
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun createBitmapWithMultiplePlanes_throwsException() {
        // Arrange.
        val planeProxy: ImageProxy.PlaneProxy =
            FakePlaneProxy(
                ImageUtil.createDirectByteBuffer(
                    TestImageUtil.createBitmap(
                        WIDTH,
                        HEIGHT
                    )
                ),
                WIDTH * ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE,
                ImageUtil.DEFAULT_RGBA_PIXEL_STRIDE
            )
        // Act.
        ImageUtil.createBitmapFromPlane(
            arrayOf(planeProxy, planeProxy),
            WIDTH,
            HEIGHT
        )
    }

    @Test
    fun createBitmapFromImageProxy_yuv420() {
        val fakeImageProxy = TestImageUtil.createYuvFakeImageProxy(
            ImmutableImageInfo.create(
            TagBundle.emptyBundle(), 0, 0, Matrix()
        ), WIDTH, HEIGHT)

        val bitmap = ImageUtil.createBitmapFromImageProxy(fakeImageProxy)

        assertThat(bitmap.width).isEqualTo(WIDTH)
        assertThat(bitmap.height).isEqualTo(HEIGHT)
        assertThat(bitmap.byteCount).isEqualTo(76800)
    }

    @Test
    fun createBitmapFromImageProxy_rgba() {
        val fakeYuvImageProxy = TestImageUtil.createYuvFakeImageProxy(
            ImmutableImageInfo.create(
                TagBundle.emptyBundle(), 0, 0, Matrix()
            ), WIDTH, HEIGHT)

        val fakeRgbaImageProxy = ImageProcessingUtil.convertYUVToRGB(
            fakeYuvImageProxy,
            SafeCloseImageReaderProxy(
                ImageReaderProxys.createIsolatedReader(
                    WIDTH,
                    HEIGHT,
                    PixelFormat.RGBA_8888,
                    2)),
            ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4),
            0,
            false)
        assertThat(fakeRgbaImageProxy).isNotNull()

        val bitmap = ImageUtil.createBitmapFromImageProxy(fakeRgbaImageProxy!!)

        assertThat(bitmap.width).isEqualTo(WIDTH)
        assertThat(bitmap.height).isEqualTo(HEIGHT)
        assertThat(bitmap.byteCount).isEqualTo(76800)
    }

    @Test
    fun createBitmapFromImageProxy_jpeg() {
        val jpegBytes = TestImageUtil.createJpegBytes(WIDTH, HEIGHT)
        val fakeJpegImageProxy = TestImageUtil.createJpegFakeImageProxy(jpegBytes)

        val bitmap = ImageUtil.createBitmapFromImageProxy(fakeJpegImageProxy)

        assertThat(bitmap.width).isEqualTo(WIDTH)
        assertThat(bitmap.height).isEqualTo(HEIGHT)
        assertThat(bitmap.byteCount).isEqualTo(76800)

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        assertThat(TestImageUtil.getAverageDiff(jpegBytes, byteArray)).isEqualTo(0)
    }

    @Test
    fun createBitmapFromImageProxy_invalidJpegByteArray() {
        val jpegBytes = TestImageUtil.createJpegBytes(WIDTH, HEIGHT)
        val fakeJpegImageProxy = TestImageUtil.createJpegFakeImageProxy(jpegBytes)

        fakeJpegImageProxy.planes = arrayOf(
            FakeJpegPlaneProxy(
                byteArrayOf(0)
            )
        )

        assertThrows<UnsupportedOperationException> {
            ImageUtil.createBitmapFromImageProxy(fakeJpegImageProxy)
        }
    }

    @Test
    fun createBitmapFromImageProxy_invalidFormat() {
        val image = FakeImageProxy(
            ImmutableImageInfo.create(
                TagBundle.emptyBundle(), 0, 0, Matrix()
            )
        )
        image.format = ImageFormat.PRIVATE
        image.width = WIDTH
        image.height = HEIGHT

        assertThrows<IllegalArgumentException> {
            ImageUtil.createBitmapFromImageProxy(image)
        }
    }
}
