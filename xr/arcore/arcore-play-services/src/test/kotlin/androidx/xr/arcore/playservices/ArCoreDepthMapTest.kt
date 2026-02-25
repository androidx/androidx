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

package androidx.xr.arcore.playservices

import android.media.Image
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.DepthEstimationMode
import com.google.ar.core.Frame as ARCore1xFrame
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ArCoreDepthMapTest {
    private lateinit var mockDepthMapImage: Image
    private lateinit var mockSmoothDepthMapImage: Image
    private lateinit var mockDepthMapConfidenceImage: Image
    private lateinit var mockLastFrame: ARCore1xFrame
    private lateinit var mockImagePlane: Image.Plane
    private lateinit var mockSmoothImagePlane: Image.Plane
    private lateinit var mockImageConfidencePlane: Image.Plane
    private lateinit var underTest: ArCoreDepthMap

    @Before
    fun setUp() {
        mockLastFrame = mock<ARCore1xFrame>()
        mockDepthMapImage = mock<Image>()
        mockSmoothDepthMapImage = mock<Image>()
        mockDepthMapConfidenceImage = mock<Image>()
        mockImagePlane = mock<Image.Plane>()
        mockSmoothImagePlane = mock<Image.Plane>()
        mockImageConfidencePlane = mock<Image.Plane>()
        underTest = ArCoreDepthMap()
    }

    @Test
    fun update_rawDepthMapAndRawConfidence() {
        val imageWidth = 5
        val imageHeight = 2
        val pixelSize = Short.SIZE_BYTES
        val bufferSize = 10
        val rawConfidenceValues = ByteBuffer.allocate(bufferSize)
        for (i in 1..bufferSize) {
            rawConfidenceValues.put((0..255).random().toByte())
        }
        val millimetersRawBuffer =
            ByteBuffer.allocate(pixelSize * bufferSize).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 1..bufferSize) {
            millimetersRawBuffer.putShort((1000..8000).random().toShort())
        }
        millimetersRawBuffer.position(0)
        val confidencePlaneArray = arrayOf(mockImageConfidencePlane)
        val depthMapPlaneArray = arrayOf(mockImagePlane)
        val millimetersShortBuffer = millimetersRawBuffer.asShortBuffer()

        whenever(mockLastFrame.acquireRawDepthConfidenceImage())
            .thenReturn(mockDepthMapConfidenceImage)
        whenever(mockDepthMapConfidenceImage.getPlanes()).thenReturn(confidencePlaneArray)
        whenever(mockImageConfidencePlane.getBuffer()).thenReturn(rawConfidenceValues)
        whenever(mockLastFrame.acquireRawDepthImage16Bits()).thenReturn(mockDepthMapImage)
        whenever(mockLastFrame.acquireDepthImage16Bits()).thenReturn(mockDepthMapImage)
        whenever(mockImagePlane.getBuffer()).thenReturn(millimetersRawBuffer)
        whenever(mockImagePlane.pixelStride).thenReturn(pixelSize)
        whenever(mockImagePlane.rowStride).thenReturn(pixelSize * imageWidth)
        whenever(mockDepthMapImage.getPlanes()).thenReturn(depthMapPlaneArray)
        whenever(mockDepthMapImage.width).thenReturn(imageWidth)
        whenever(mockDepthMapImage.height).thenReturn(imageHeight)

        underTest.updateDepthEstimationMode(DepthEstimationMode.RAW_ONLY)
        underTest.update(mockLastFrame)

        assertThat(underTest.rawConfidenceMap!![0]).isEqualTo(rawConfidenceValues.get(0))
        assertThat(underTest.rawConfidenceMap!![4]).isEqualTo(rawConfidenceValues.get(4))
        assertThat(underTest.rawDepthMap!![0])
            .isEqualTo((millimetersShortBuffer.get(0).toFloat()) / 1000f)
        assertThat(underTest.rawDepthMap!![9])
            .isEqualTo((millimetersShortBuffer.get(9).toFloat()) / 1000f)
        assertThat(underTest.smoothDepthMap).isEqualTo(null)
    }

    @Test
    fun update_rawAndSmoothDepthMap() {
        val imageWidth = 10
        val imageHeight = 2
        val pixelSize = Short.SIZE_BYTES
        val bufferSize = 20
        val rawConfidenceValues = ByteBuffer.allocate(bufferSize)
        for (i in 1..bufferSize) {
            rawConfidenceValues.put((0..255).random().toByte())
        }
        val millimetersRawBuffer =
            ByteBuffer.allocate(pixelSize * bufferSize).order(ByteOrder.LITTLE_ENDIAN)
        val millimetersSmoothBuffer =
            ByteBuffer.allocate(pixelSize * bufferSize).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 1..bufferSize) {
            millimetersRawBuffer.putShort((1000..8000).random().toShort())
            millimetersSmoothBuffer.putShort((1000..8000).random().toShort())
        }
        millimetersRawBuffer.position(0)
        millimetersSmoothBuffer.position(0)
        val confidencePlaneArray = arrayOf(mockImageConfidencePlane)
        val depthMapPlaneArray = arrayOf(mockImagePlane)
        val depthMapSmoothPlaneArray = arrayOf(mockSmoothImagePlane)
        val millimetersShortBuffer = millimetersRawBuffer.asShortBuffer()
        val millimetersSmoothShortBuffer = millimetersSmoothBuffer.asShortBuffer()

        whenever(mockLastFrame.acquireRawDepthConfidenceImage())
            .thenReturn(mockDepthMapConfidenceImage)
        whenever(mockDepthMapConfidenceImage.getPlanes()).thenReturn(confidencePlaneArray)
        whenever(mockImageConfidencePlane.getBuffer()).thenReturn(rawConfidenceValues)
        whenever(mockLastFrame.acquireRawDepthImage16Bits()).thenReturn(mockDepthMapImage)
        whenever(mockLastFrame.acquireDepthImage16Bits()).thenReturn(mockSmoothDepthMapImage)
        whenever(mockImagePlane.getBuffer()).thenReturn(millimetersRawBuffer)
        whenever(mockSmoothImagePlane.getBuffer()).thenReturn(millimetersSmoothBuffer)
        whenever(mockImagePlane.pixelStride).thenReturn(pixelSize)
        whenever(mockSmoothImagePlane.pixelStride).thenReturn(pixelSize)
        whenever(mockImagePlane.rowStride).thenReturn(pixelSize * imageWidth)
        whenever(mockSmoothImagePlane.rowStride).thenReturn(pixelSize * imageWidth)
        whenever(mockDepthMapImage.getPlanes()).thenReturn(depthMapPlaneArray)
        whenever(mockSmoothDepthMapImage.getPlanes()).thenReturn(depthMapSmoothPlaneArray)
        whenever(mockDepthMapImage.width).thenReturn(imageWidth)
        whenever(mockSmoothDepthMapImage.width).thenReturn(imageWidth)
        whenever(mockDepthMapImage.height).thenReturn(imageHeight)
        whenever(mockSmoothDepthMapImage.height).thenReturn(imageHeight)

        underTest.updateDepthEstimationMode(DepthEstimationMode.SMOOTH_AND_RAW)
        underTest.update(mockLastFrame)

        assertThat(underTest.rawConfidenceMap!![0]).isEqualTo(rawConfidenceValues.get(0))
        assertThat(underTest.rawConfidenceMap!![10]).isEqualTo(rawConfidenceValues.get(10))
        assertThat(underTest.smoothConfidenceMap!![0]).isEqualTo(rawConfidenceValues.get(0))
        assertThat(underTest.smoothConfidenceMap!![10]).isEqualTo(rawConfidenceValues.get(10))
        assertThat(underTest.rawDepthMap!![0])
            .isEqualTo((millimetersShortBuffer.get(0).toFloat()) / 1000f)
        assertThat(underTest.rawDepthMap!![15])
            .isEqualTo((millimetersShortBuffer.get(15).toFloat()) / 1000f)
        assertThat(underTest.smoothDepthMap!![0])
            .isEqualTo((millimetersSmoothShortBuffer.get(0).toFloat()) / 1000f)
        assertThat(underTest.smoothDepthMap!![19])
            .isEqualTo((millimetersSmoothShortBuffer.get(19).toFloat()) / 1000f)
    }
}
