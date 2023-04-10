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

import androidx.camera.core.ImageProxy
import androidx.camera.testing.TestImageUtil
import androidx.camera.testing.fakes.FakePlaneProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
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
        val planeProxy: ImageProxy.PlaneProxy = FakePlaneProxy(
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
        val planeProxy: ImageProxy.PlaneProxy = FakePlaneProxy(
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
        val planeProxy: ImageProxy.PlaneProxy = FakePlaneProxy(
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
        val planeProxy: ImageProxy.PlaneProxy = FakePlaneProxy(
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
}