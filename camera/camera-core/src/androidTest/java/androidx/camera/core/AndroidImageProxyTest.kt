/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.core

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/** Unit tests for {@link AndroidImageProxy}. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidImageProxyTest {
    private val INITIAL_TIMESTAMP = 138990020L

    private val mImage = mock(Image::class.java)
    private val mYPlane = mock(Image.Plane::class.java)
    private val mUPlane = mock(Image.Plane::class.java)
    private val mVPlane = mock(Image.Plane::class.java)
    private var mImageProxy: ImageProxy? = null

    @Before
    fun setUp() {
        `when`(mImage.planes).thenReturn(arrayOf(mYPlane, mUPlane, mVPlane))
        `when`(mYPlane.rowStride).thenReturn(640)
        `when`(mYPlane.pixelStride).thenReturn(1)
        `when`(mYPlane.buffer).thenReturn(ByteBuffer.allocateDirect(640 * 480))
        `when`(mUPlane.rowStride).thenReturn(320)
        `when`(mUPlane.pixelStride).thenReturn(1)
        `when`(mUPlane.buffer).thenReturn(ByteBuffer.allocateDirect(320 * 240))
        `when`(mVPlane.rowStride).thenReturn(320)
        `when`(mVPlane.pixelStride).thenReturn(1)
        `when`(mVPlane.buffer).thenReturn(ByteBuffer.allocateDirect(320 * 240))
        `when`(mImage.timestamp).thenReturn(INITIAL_TIMESTAMP)

        mImageProxy = AndroidImageProxy(mImage)
    }

    @Test
    fun close_closesWrappedImage() {
        mImageProxy!!.close()
        verify(mImage).close()
    }

    @Test
    fun getCropRect_returnsCropRectForWrappedImage() {
        `when`(mImage.cropRect).thenReturn(Rect(0, 0, 20, 20))

        assertThat(mImageProxy!!.cropRect).isEqualTo(Rect(0, 0, 20, 20))
    }

    @Test
    fun setCropRect_setsCropRectForWrappedImage() {
        mImageProxy?.setCropRect(Rect(0, 0, 40, 40))
        verify(mImage).cropRect = (Rect(0, 0, 40, 40))
    }

    @Test
    fun getFormat_returnsFormatForWrappedImage() {
        `when`(mImage.format).thenReturn(ImageFormat.YUV_420_888)

        assertThat(mImageProxy?.format).isEqualTo(ImageFormat.YUV_420_888)
    }

    @Test
    fun getHeight_returnsHeightForWrappedImage() {
        `when`(mImage.height).thenReturn(480)

        assertThat(mImageProxy!!.height).isEqualTo(480)
    }

    @Test
    fun getWidth_returnsWidthForWrappedImage() {
        `when`(mImage.width).thenReturn(640)

        assertThat(mImageProxy!!.width).isEqualTo(640)
    }

    @Test
    fun getTimestamp_returnsTimestampForWrappedImage() {
        assertThat(mImageProxy!!.imageInfo.timestamp).isEqualTo(INITIAL_TIMESTAMP)
    }

    @Test
    fun getPlanes_returnsPlanesForWrappedImage() {
        val wrappedPlanes = mImageProxy!!.planes

        val originalPlanes = arrayOf(mYPlane, mUPlane, mVPlane)
        assertThat(wrappedPlanes.size).isEqualTo(3)

        for (i in 0..2) {
            assertThat(wrappedPlanes[i].rowStride).isEqualTo(originalPlanes[i].rowStride)
            assertThat(wrappedPlanes[i].pixelStride).isEqualTo(originalPlanes[i].pixelStride)
            assertThat(wrappedPlanes[i].buffer).isEqualTo(originalPlanes[i].buffer)
        }
    }
}
