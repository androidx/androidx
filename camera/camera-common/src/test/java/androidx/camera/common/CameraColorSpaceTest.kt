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

package androidx.camera.common

import android.graphics.ColorSpace
import android.os.Build
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [CameraColorSpace] and [CameraColorSpaces]. */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CameraColorSpaceTest {
    private val colorSpaceMap =
        mapOf(
            CameraColorSpaces.SRGB to ColorSpace.Named.SRGB,
            CameraColorSpaces.LINEAR_SRGB to ColorSpace.Named.LINEAR_SRGB,
            CameraColorSpaces.EXTENDED_SRGB to ColorSpace.Named.EXTENDED_SRGB,
            CameraColorSpaces.LINEAR_EXTENDED_SRGB to ColorSpace.Named.LINEAR_EXTENDED_SRGB,
            CameraColorSpaces.BT709 to ColorSpace.Named.BT709,
            CameraColorSpaces.BT2020 to ColorSpace.Named.BT2020,
            CameraColorSpaces.DCI_P3 to ColorSpace.Named.DCI_P3,
            CameraColorSpaces.DISPLAY_P3 to ColorSpace.Named.DISPLAY_P3,
            CameraColorSpaces.NTSC_1953 to ColorSpace.Named.NTSC_1953,
            CameraColorSpaces.SMPTE_C to ColorSpace.Named.SMPTE_C,
            CameraColorSpaces.ADOBE_RGB to ColorSpace.Named.ADOBE_RGB,
            CameraColorSpaces.PRO_PHOTO_RGB to ColorSpace.Named.PRO_PHOTO_RGB,
            CameraColorSpaces.ACES to ColorSpace.Named.ACES,
            CameraColorSpaces.ACESCG to ColorSpace.Named.ACESCG,
            CameraColorSpaces.CIE_XYZ to ColorSpace.Named.CIE_XYZ,
            CameraColorSpaces.CIE_LAB to ColorSpace.Named.CIE_LAB,
            CameraColorSpaces.BT2020_HLG to ColorSpace.Named.BT2020_HLG,
            CameraColorSpaces.BT2020_PQ to ColorSpace.Named.BT2020_PQ,
        )

    @Test
    fun toColorSpaceNamed() {
        for ((wrapper, frameworkEnum) in colorSpaceMap) {
            val converted = CameraColorSpaces.toColorSpaceNamed(wrapper)
            Truth.assertThat(converted).isEqualTo(frameworkEnum)
        }
    }

    @Test
    fun fromColorSpaceNamed() {
        for ((wrapper, frameworkEnum) in colorSpaceMap) {
            val converted = CameraColorSpaces.fromColorSpaceNamed(frameworkEnum)
            Truth.assertThat(converted).isEqualTo(wrapper)
        }
    }
}
