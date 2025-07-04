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

package androidx.camera.camera2.pipe

import android.graphics.ColorSpace
import android.os.Build
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [CameraColorSpace] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ColorSpaceNamedTest {
    val colorSpaceMap =
        mapOf(
            CameraColorSpace.SRGB to ColorSpace.Named.SRGB,
            CameraColorSpace.LINEAR_SRGB to ColorSpace.Named.LINEAR_SRGB,
            CameraColorSpace.EXTENDED_SRGB to ColorSpace.Named.EXTENDED_SRGB,
            CameraColorSpace.LINEAR_EXTENDED_SRGB to ColorSpace.Named.LINEAR_EXTENDED_SRGB,
            CameraColorSpace.BT709 to ColorSpace.Named.BT709,
            CameraColorSpace.BT2020 to ColorSpace.Named.BT2020,
            CameraColorSpace.DCI_P3 to ColorSpace.Named.DCI_P3,
            CameraColorSpace.DISPLAY_P3 to ColorSpace.Named.DISPLAY_P3,
            CameraColorSpace.NTSC_1953 to ColorSpace.Named.NTSC_1953,
            CameraColorSpace.SMPTE_C to ColorSpace.Named.SMPTE_C,
            CameraColorSpace.ADOBE_RGB to ColorSpace.Named.ADOBE_RGB,
            CameraColorSpace.PRO_PHOTO_RGB to ColorSpace.Named.PRO_PHOTO_RGB,
            CameraColorSpace.ACES to ColorSpace.Named.ACES,
            CameraColorSpace.ACESCG to ColorSpace.Named.ACESCG,
            CameraColorSpace.CIE_XYZ to ColorSpace.Named.CIE_XYZ,
            CameraColorSpace.CIE_LAB to ColorSpace.Named.CIE_LAB,
            CameraColorSpace.BT2020_HLG to ColorSpace.Named.BT2020_HLG,
            CameraColorSpace.BT2020_PQ to ColorSpace.Named.BT2020_PQ,
        )

    @Test
    fun toColorSpaceNamed() {
        colorSpaceMap.forEach { (wrapper, frameworkEnum) ->
            val converted = wrapper.toColorSpaceNamed()
            Truth.assertThat(converted).isEqualTo(frameworkEnum)
        }
    }

    @Test
    fun fromColorSpaceNamed() {
        colorSpaceMap.forEach { (wrapper, frameworkEnum) ->
            val converted = CameraColorSpace.fromColorSpaceNamed(frameworkEnum)
            Truth.assertThat(converted).isEqualTo(wrapper)
        }
    }
}
