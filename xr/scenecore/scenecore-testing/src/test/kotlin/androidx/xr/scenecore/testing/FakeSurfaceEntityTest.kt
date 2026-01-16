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

package androidx.xr.scenecore.testing

import android.graphics.ImageFormat
import android.media.ImageReader
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.TextureResource
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeSurfaceEntityTest {
    val testCanvasShape = SurfaceEntity.Shape.Quad(FloatSize2d(1f, 1f))

    lateinit var underTest: FakeSurfaceEntity

    @Before
    fun setUp() {
        underTest = FakeSurfaceEntity()
    }

    @After
    fun release() {
        underTest.surface.release()
        underTest.dispose()
    }

    @Test
    fun getDefaultValue_returnsDefaultValue() {
        check(underTest.stereoMode == SurfaceEntity.StereoMode.SIDE_BY_SIDE)
    }

    @Test
    fun getDimensions_setCanvasShape_returnsCanvasShapeDimensions() {
        underTest.shape = testCanvasShape

        assertThat(underTest.dimensions).isEqualTo(testCanvasShape.dimensions)
    }

    @Test
    fun getSurface_setSurface_returnsCorrectValue() {
        val surface = ImageReader.newInstance(1, 1, ImageFormat.YUV_420_888, 1).surface
        underTest.setSurface(surface)

        assertThat(underTest.surface).isEqualTo(surface)
    }

    @Test
    fun setPrimaryAlphaMaskTexture_getsCorrectPrimaryAlphaMask() {
        val fakeTextureResource = object : TextureResource {}
        underTest.setPrimaryAlphaMaskTexture(fakeTextureResource)

        assertThat(underTest.primaryAlphaMask).isEqualTo(fakeTextureResource)
    }

    @Test
    fun setAuxiliaryAlphaMaskTexture_getsCorrectAuxiliaryAlphaMask() {
        val fakeTextureResource = object : TextureResource {}
        underTest.setAuxiliaryAlphaMaskTexture(fakeTextureResource)

        assertThat(underTest.auxiliaryAlphaMask).isEqualTo(fakeTextureResource)
    }

    @Test
    fun getContentColorMetadataSet_setContentColorMetadataSet_returnCorrectly() {
        // Default value
        check(!underTest.contentColorMetadataSet)

        underTest.mContentColorMetadataSet = true

        assertThat(underTest.contentColorMetadataSet).isTrue()
    }

    @Test
    fun setContentColorMetadata_returnsContentColorMetadata() {
        check(underTest.colorSpace == SurfaceEntity.ColorSpace.BT709)
        check(underTest.colorTransfer == SurfaceEntity.ColorTransfer.LINEAR)
        check(underTest.colorRange == SurfaceEntity.ColorRange.FULL)
        check(underTest.maxContentLightLevel == 0)

        underTest.setContentColorMetadata(
            SurfaceEntity.ColorSpace.BT2020,
            SurfaceEntity.ColorTransfer.SRGB,
            SurfaceEntity.ColorRange.LIMITED,
            100,
        )

        assertThat(underTest.colorSpace).isEqualTo(SurfaceEntity.ColorSpace.BT2020)
        assertThat(underTest.colorTransfer).isEqualTo(SurfaceEntity.ColorTransfer.SRGB)
        assertThat(underTest.colorRange).isEqualTo(SurfaceEntity.ColorRange.LIMITED)
        assertThat(underTest.maxContentLightLevel).isEqualTo(100)
    }

    @Test
    fun resetContentColorMetadata_returnDefaultContentColorMetadata() {
        underTest.setContentColorMetadata(
            SurfaceEntity.ColorSpace.BT2020,
            SurfaceEntity.ColorTransfer.SRGB,
            SurfaceEntity.ColorRange.LIMITED,
            100,
        )

        assertThat(underTest.colorSpace).isEqualTo(SurfaceEntity.ColorSpace.BT2020)
        assertThat(underTest.colorTransfer).isEqualTo(SurfaceEntity.ColorTransfer.SRGB)
        assertThat(underTest.colorRange).isEqualTo(SurfaceEntity.ColorRange.LIMITED)
        assertThat(underTest.maxContentLightLevel).isEqualTo(100)

        underTest.resetContentColorMetadata()

        assertThat(underTest.colorSpace).isEqualTo(SurfaceEntity.ColorSpace.BT709)
        assertThat(underTest.colorTransfer).isEqualTo(SurfaceEntity.ColorTransfer.LINEAR)
        assertThat(underTest.colorRange).isEqualTo(SurfaceEntity.ColorRange.FULL)
        assertThat(underTest.maxContentLightLevel).isEqualTo(0)
    }
}
