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

package androidx.camera.common.testing

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@SuppressLint("NewApi")
class FakeImageTest {

    @Test
    fun defaultDataSpace_isUnknown() {
        val fakeImage = FakeImage(width = 640, height = 480, format = 35, timestamp = 0L)
        assertThat(fakeImage.dataSpace).isEqualTo(android.hardware.DataSpace.DATASPACE_UNKNOWN)
    }

    @Test
    fun isClosed_initiallyFalse() {
        val fakeImage = FakeImage(width = 640, height = 480, format = 35, timestamp = 0L)
        assertThat(fakeImage.isClosed).isFalse()
        assertThat(fakeImage.closeCount).isEqualTo(0)
    }

    @Test
    fun close_incrementsCloseCountAndIsClosed() {
        val fakeImage = FakeImage(width = 640, height = 480, format = 35, timestamp = 0L)

        fakeImage.close()

        assertThat(fakeImage.isClosed).isTrue()
        assertThat(fakeImage.closeCount).isEqualTo(1)

        fakeImage.close()

        assertThat(fakeImage.isClosed).isTrue()
        assertThat(fakeImage.closeCount).isEqualTo(2)
    }

    @Test
    fun imagePlanes_yuv420_createsThreeSharedPlanes() {
        val image =
            FakeImage(width = 640, height = 480, format = ImageFormat.YUV_420_888, timestamp = 0L)
        val planes = image.imagePlanes
        assertThat(planes).hasSize(3)

        // Verify dimensions and strides
        assertThat(planes[0].pixelStride).isEqualTo(1)
        assertThat(planes[0].rowStride).isEqualTo(640)
        assertThat(planes[0].buffer!!.capacity()).isEqualTo(640 * 480)

        assertThat(planes[1].pixelStride).isEqualTo(2)
        assertThat(planes[1].rowStride).isEqualTo(640)
        assertThat(planes[1].buffer!!.capacity()).isEqualTo(640 * 240)

        assertThat(planes[2].pixelStride).isEqualTo(2)
        assertThat(planes[2].rowStride).isEqualTo(640)
        assertThat(planes[2].buffer!!.capacity())
            .isEqualTo(640 * 240 - 1) // Shifted by 1 byte in NV12

        assertThat(planes[0].buffer!!.isDirect).isTrue()
    }

    @Test
    fun imagePlanes_jpeg_createsOnePlane() {
        val image = FakeImage(width = 640, height = 480, format = ImageFormat.JPEG, timestamp = 0L)
        val planes = image.imagePlanes
        assertThat(planes).hasSize(1)
        assertThat(planes[0].pixelStride).isEqualTo(1)
        assertThat(planes[0].rowStride).isEqualTo(640)
        assertThat(planes[0].buffer!!.capacity())
            .isEqualTo((640 * 480 * 3) / 4) // Estimated compressed capacity
    }

    @Test
    fun unwrapAs_self_returnsInstance() {
        val image =
            FakeImage(width = 640, height = 480, format = ImageFormat.YUV_420_888, timestamp = 0L)
        assertThat(image.unwrapAs(FakeImage::class.java)).isSameInstanceAs(image)
    }

    @Test
    fun unwrapAs_byteBuffer_returnsBackingBuffer() {
        val buf = ByteBuffer.allocateDirect(10)
        val image =
            FakeImage(
                width = 640,
                height = 480,
                format = ImageFormat.JPEG,
                timestamp = 0L,
                byteBuffer = buf,
            )
        assertThat(image.unwrapAs(ByteBuffer::class.java)).isSameInstanceAs(buf)
    }

    @Test
    fun imagePlanes_privateFormat_createsThreeZeroSizedPlanes() {
        val image =
            FakeImage(width = 640, height = 480, format = ImageFormat.PRIVATE, timestamp = 0L)
        val planes = image.imagePlanes
        assertThat(planes).hasSize(3)
        assertThat(planes[0].buffer!!.capacity()).isEqualTo(0)
        assertThat(planes[1].buffer!!.capacity()).isEqualTo(0)
        assertThat(planes[2].buffer!!.capacity()).isEqualTo(0)
    }

    @Test
    @Config(minSdk = 26)
    fun unwrapAs_hardwareBuffer_returnsHardwareBuffer() {
        val image =
            FakeImage(width = 640, height = 480, format = ImageFormat.YUV_420_888, timestamp = 0L)
        val hwBuffer = image.unwrapAs(HardwareBuffer::class.java)
        if (hwBuffer != null) {
            assertThat(hwBuffer.format).isEqualTo(HardwareBuffer.YCBCR_420_888)
        }
    }

    @Test
    fun imagePlanes_depthPointCloud_createsOnePlane() {
        val image =
            FakeImage(
                width = 640,
                height = 480,
                format = ImageFormat.DEPTH_POINT_CLOUD,
                timestamp = 0L,
            )
        val planes = image.imagePlanes
        assertThat(planes).hasSize(1)
        assertThat(planes[0].pixelStride).isEqualTo(1)
        assertThat(planes[0].rowStride).isEqualTo(640)
        assertThat(planes[0].buffer!!.capacity()).isEqualTo(640 * 480 * 2)
    }
}
