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
import androidx.annotation.RequiresApi
import androidx.camera.common.compat.Api34Compat

/** Constants and utility functions for [CameraColorSpace]. */
public object CameraColorSpaces {
    public const val UNKNOWN: String = "UNKNOWN"
    public const val SRGB: String = "SRGB"
    public const val LINEAR_SRGB: String = "LINEAR_SRGB"
    public const val EXTENDED_SRGB: String = "EXTENDED_SRGB"
    public const val LINEAR_EXTENDED_SRGB: String = "LINEAR_EXTENDED_SRGB"
    public const val BT709: String = "BT709"
    public const val BT2020: String = "BT2020"
    public const val DCI_P3: String = "DCI_P3"
    public const val DISPLAY_P3: String = "DISPLAY_P3"
    public const val NTSC_1953: String = "NTSC_1953"
    public const val SMPTE_C: String = "SMPTE_C"
    public const val ADOBE_RGB: String = "ADOBE_RGB"
    public const val PRO_PHOTO_RGB: String = "PRO_PHOTO_RGB"
    public const val ACES: String = "ACES"
    public const val ACESCG: String = "ACESCG"
    public const val CIE_XYZ: String = "CIE_XYZ"
    public const val CIE_LAB: String = "CIE_LAB"
    public const val BT2020_HLG: String = "BT2020_HLG"
    public const val BT2020_PQ: String = "BT2020_PQ"

    /**
     * Converts a [android.graphics.ColorSpace.Named] to a [CameraColorSpace] string.
     *
     * @param colorSpaceNamed The [android.graphics.ColorSpace.Named] to convert.
     * @return The corresponding [CameraColorSpace] string or [UNKNOWN] if not supported.
     */
    @JvmStatic
    @JvmName("fromColorSpaceNamed")
    @RequiresApi(26)
    @CameraColorSpace
    public fun fromColorSpaceNamed(colorSpaceNamed: ColorSpace.Named): String {
        when (colorSpaceNamed) {
            ColorSpace.Named.SRGB -> return SRGB
            ColorSpace.Named.LINEAR_SRGB -> return LINEAR_SRGB
            ColorSpace.Named.EXTENDED_SRGB -> return EXTENDED_SRGB
            ColorSpace.Named.LINEAR_EXTENDED_SRGB -> return LINEAR_EXTENDED_SRGB
            ColorSpace.Named.BT709 -> return BT709
            ColorSpace.Named.BT2020 -> return BT2020
            ColorSpace.Named.DCI_P3 -> return DCI_P3
            ColorSpace.Named.DISPLAY_P3 -> return DISPLAY_P3
            ColorSpace.Named.NTSC_1953 -> return NTSC_1953
            ColorSpace.Named.SMPTE_C -> return SMPTE_C
            ColorSpace.Named.ADOBE_RGB -> return ADOBE_RGB
            ColorSpace.Named.PRO_PHOTO_RGB -> return PRO_PHOTO_RGB
            ColorSpace.Named.ACES -> return ACES
            ColorSpace.Named.ACESCG -> return ACESCG
            ColorSpace.Named.CIE_XYZ -> return CIE_XYZ
            ColorSpace.Named.CIE_LAB -> return CIE_LAB
            else -> {}
        }

        if (Build.VERSION.SDK_INT >= 34) {
            if (colorSpaceNamed == Api34Compat.getBt2020Hlg()) return BT2020_HLG
            if (colorSpaceNamed == Api34Compat.getBt2020Pq()) return BT2020_PQ
        }

        return UNKNOWN
    }

    /**
     * Converts a [CameraColorSpace] string to a [ColorSpace.Named].
     *
     * @param colorSpace The [CameraColorSpace] string to convert.
     * @return The corresponding [ColorSpace.Named] or `null` if not supported.
     */
    @JvmStatic
    @JvmName("toColorSpaceNamed")
    @RequiresApi(26)
    public fun toColorSpaceNamed(@CameraColorSpace colorSpace: String): ColorSpace.Named? {
        when (colorSpace) {
            SRGB -> return ColorSpace.Named.SRGB
            LINEAR_SRGB -> return ColorSpace.Named.LINEAR_SRGB
            EXTENDED_SRGB -> return ColorSpace.Named.EXTENDED_SRGB
            LINEAR_EXTENDED_SRGB -> return ColorSpace.Named.LINEAR_EXTENDED_SRGB
            BT709 -> return ColorSpace.Named.BT709
            BT2020 -> return ColorSpace.Named.BT2020
            DCI_P3 -> return ColorSpace.Named.DCI_P3
            DISPLAY_P3 -> return ColorSpace.Named.DISPLAY_P3
            NTSC_1953 -> return ColorSpace.Named.NTSC_1953
            SMPTE_C -> return ColorSpace.Named.SMPTE_C
            ADOBE_RGB -> return ColorSpace.Named.ADOBE_RGB
            PRO_PHOTO_RGB -> return ColorSpace.Named.PRO_PHOTO_RGB
            ACES -> return ColorSpace.Named.ACES
            ACESCG -> return ColorSpace.Named.ACESCG
            CIE_XYZ -> return ColorSpace.Named.CIE_XYZ
            CIE_LAB -> return ColorSpace.Named.CIE_LAB
            else -> {}
        }

        if (Build.VERSION.SDK_INT >= 34) {
            return when (colorSpace) {
                BT2020_HLG -> Api34Compat.getBt2020Hlg()
                BT2020_PQ -> Api34Compat.getBt2020Pq()
                else -> null
            }
        }

        return null
    }
}
