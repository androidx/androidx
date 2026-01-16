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
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.SurfaceEntity
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider
import com.android.extensions.xr.node.Node
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FakeSurfaceFeatureTest {
    private lateinit var underTest: FakeSurfaceFeature

    private fun createSurfaceFeature(): FakeSurfaceFeature {
        val xrExtensions = XrExtensionsProvider.getXrExtensions()!!
        val nodeHolder = NodeHolder(xrExtensions.createNode(), Node::class.java)
        val fakeSurfaceFeature = FakeSurfaceFeature(nodeHolder)

        // Constructor values
        assertThat(fakeSurfaceFeature.stereoMode).isEqualTo(SurfaceEntity.StereoMode.MONO)
        assertThat(fakeSurfaceFeature.shape.dimensions)
            .isEqualTo(SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)).dimensions)

        return fakeSurfaceFeature
    }

    @Before
    fun setUp() {
        underTest = createSurfaceFeature()
    }

    @After
    fun tearDown() {
        underTest.dispose()
    }

    @Test
    fun getSurface_setSurface_getsCorrectly() {
        ImageReader.newInstance(2, 2, ImageFormat.YUV_420_888, 1).use { imageReader ->
            val surface = imageReader.surface

            assertThat(underTest.surface).isNotEqualTo(surface)

            underTest.setSurface(surface)

            assertThat(underTest.surface).isEqualTo(surface)
        }
    }

    @Test
    fun setSurfacePixelDimensions_setsCorrectly() {
        val width = 1920
        val height = 1080
        underTest.setSurfacePixelDimensions(width, height)
        val expectedDimensions = IntSize2d(width, height)

        assertThat(underTest.surfacePixelDimensions).isEqualTo(expectedDimensions)
    }

    @Test
    fun setColliderEnabled_setsCorrectly() {
        check(!underTest.colliderEnabled)

        underTest.setColliderEnabled(true)
        assertThat(underTest.colliderEnabled).isTrue()
    }

    @Test
    fun setPrimaryAlphaMaskTexture_setsCorrectly() {
        val alphaMask = FakeResource(0)
        underTest.setPrimaryAlphaMaskTexture(alphaMask)

        assertThat(underTest.primaryAlphaMask).isEqualTo(alphaMask)
    }

    @Test
    fun setAuxiliaryAlphaMaskTexture_setsCorrectly() {
        val alphaMask = FakeResource(0)
        underTest.setAuxiliaryAlphaMaskTexture(alphaMask)

        assertThat(underTest.auxiliaryAlphaMask).isEqualTo(alphaMask)
    }

    @Test
    fun getContentColorMetadataSet_getsCorrectly() {
        check(!underTest.contentColorMetadataSet)

        underTest.setContentColorMetadata(
            SurfaceEntity.ColorSpace.BT2020,
            SurfaceEntity.ColorTransfer.SRGB,
            SurfaceEntity.ColorRange.LIMITED,
            100,
        )

        assertThat(underTest.contentColorMetadataSet).isTrue()

        underTest.resetContentColorMetadata()

        assertThat(underTest.contentColorMetadataSet).isFalse()
    }

    @Test
    fun setContentColorMetadata_resetContentColorMetadata_setsAndResetsCorrectly() {
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

        underTest.resetContentColorMetadata()

        assertThat(underTest.colorSpace).isEqualTo(SurfaceEntity.ColorSpace.BT709)
        assertThat(underTest.colorTransfer).isEqualTo(SurfaceEntity.ColorTransfer.LINEAR)
        assertThat(underTest.colorRange).isEqualTo(SurfaceEntity.ColorRange.FULL)
        assertThat(underTest.maxContentLightLevel).isEqualTo(0)
    }
}
