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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SurfaceEntityTest {

    @Test
    fun surfaceProtection_toString() {
        assertThat(SurfaceEntity.SurfaceProtection.NONE.toString()).isEqualTo("NONE")
        assertThat(SurfaceEntity.SurfaceProtection.PROTECTED.toString()).isEqualTo("PROTECTED")
    }

    @Test
    fun superSampling_toString() {
        assertThat(SurfaceEntity.SuperSampling.NONE.toString()).isEqualTo("NONE")
        assertThat(SurfaceEntity.SuperSampling.PENTAGON.toString()).isEqualTo("PENTAGON")
    }

    @Test
    fun drawMode_toString() {
        assertThat(SurfaceEntity.DrawMode.TRIANGLES.toString()).isEqualTo("TRIANGLES")
        assertThat(SurfaceEntity.DrawMode.TRIANGLE_STRIP.toString()).isEqualTo("TRIANGLE_STRIP")
        assertThat(SurfaceEntity.DrawMode.TRIANGLE_FAN.toString()).isEqualTo("TRIANGLE_FAN")
    }

    @Test
    fun stereoMode_toString() {
        assertThat(SurfaceEntity.StereoMode.MONO.toString()).isEqualTo("MONO")
        assertThat(SurfaceEntity.StereoMode.TOP_BOTTOM.toString()).isEqualTo("TOP_BOTTOM")
        assertThat(SurfaceEntity.StereoMode.SIDE_BY_SIDE.toString()).isEqualTo("SIDE_BY_SIDE")
        assertThat(SurfaceEntity.StereoMode.MULTIVIEW_LEFT_PRIMARY.toString())
            .isEqualTo("MULTIVIEW_LEFT_PRIMARY")
        assertThat(SurfaceEntity.StereoMode.MULTIVIEW_RIGHT_PRIMARY.toString())
            .isEqualTo("MULTIVIEW_RIGHT_PRIMARY")
    }

    @Test
    fun mediaBlendingMode_toString() {
        assertThat(SurfaceEntity.MediaBlendingMode.TRANSPARENT.toString()).isEqualTo("TRANSPARENT")
        assertThat(SurfaceEntity.MediaBlendingMode.OPAQUE.toString()).isEqualTo("OPAQUE")
    }

    @Test
    fun colorSpace_toString() {
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.BT709.toString())
            .isEqualTo("BT709")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.BT601_PAL.toString())
            .isEqualTo("BT601_PAL")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.BT2020.toString())
            .isEqualTo("BT2020")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.BT601_525.toString())
            .isEqualTo("BT601_525")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.DISPLAY_P3.toString())
            .isEqualTo("DISPLAY_P3")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.DCI_P3.toString())
            .isEqualTo("DCI_P3")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorSpace.ADOBE_RGB.toString())
            .isEqualTo("ADOBE_RGB")
    }

    @Test
    fun colorTransfer_toString() {
        assertThat(SurfaceEntity.ContentColorMetadata.ColorTransfer.LINEAR.toString())
            .isEqualTo("LINEAR")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorTransfer.SRGB.toString())
            .isEqualTo("SRGB")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorTransfer.SDR.toString()).isEqualTo("SDR")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorTransfer.GAMMA_2_2.toString())
            .isEqualTo("GAMMA_2_2")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorTransfer.ST2084.toString())
            .isEqualTo("ST2084")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorTransfer.HLG.toString()).isEqualTo("HLG")
    }

    @Test
    fun colorRange_toString() {
        assertThat(SurfaceEntity.ContentColorMetadata.ColorRange.FULL.toString()).isEqualTo("FULL")
        assertThat(SurfaceEntity.ContentColorMetadata.ColorRange.LIMITED.toString())
            .isEqualTo("LIMITED")
    }
}
