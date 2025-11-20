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
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * A compatibility wrapper for color space names.
 *
 * @see [android.graphics.ColorSpace.Named].
 */
@JvmInline
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public value class CameraColorSpace private constructor(public val colorSpaceName: String) {
    public companion object {
        public val UNKNOWN: CameraColorSpace = CameraColorSpace("UNKNOWN")
        public val SRGB: CameraColorSpace = CameraColorSpace("SRGB")
        public val LINEAR_SRGB: CameraColorSpace = CameraColorSpace("LINEAR_SRGB")
        public val EXTENDED_SRGB: CameraColorSpace = CameraColorSpace("EXTENDED_SRGB")
        public val LINEAR_EXTENDED_SRGB: CameraColorSpace = CameraColorSpace("LINEAR_EXTENDED_SRGB")
        public val BT709: CameraColorSpace = CameraColorSpace("BT709")
        public val BT2020: CameraColorSpace = CameraColorSpace("BT2020")
        public val DCI_P3: CameraColorSpace = CameraColorSpace("DCI_P3")
        public val DISPLAY_P3: CameraColorSpace = CameraColorSpace("DISPLAY_P3")
        public val NTSC_1953: CameraColorSpace = CameraColorSpace("NTSC_1953")
        public val SMPTE_C: CameraColorSpace = CameraColorSpace("SMPTE_C")
        public val ADOBE_RGB: CameraColorSpace = CameraColorSpace("ADOBE_RGB")
        public val PRO_PHOTO_RGB: CameraColorSpace = CameraColorSpace("PRO_PHOTO_RGB")
        public val ACES: CameraColorSpace = CameraColorSpace("ACES")
        public val ACESCG: CameraColorSpace = CameraColorSpace("ACESCG")
        public val CIE_XYZ: CameraColorSpace = CameraColorSpace("CIE_XYZ")
        public val CIE_LAB: CameraColorSpace = CameraColorSpace("CIE_LAB")
        public val BT2020_HLG: CameraColorSpace = CameraColorSpace("BT2020_HLG")
        public val BT2020_PQ: CameraColorSpace = CameraColorSpace("BT2020_PQ")

        /**
         * Converts a [android.graphics.ColorSpace.Named] to a [CameraColorSpace].
         *
         * @param colorSpaceNamed The [android.graphics.ColorSpace.Named] to convert.
         * @return The corresponding [CameraColorSpace] or null if the color space is not supported.
         */
        @JvmStatic
        @RequiresApi(26)
        public fun fromColorSpaceNamed(colorSpaceNamed: ColorSpace.Named): CameraColorSpace? {
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

            // Color spaces available in API 34
            if (Build.VERSION.SDK_INT < 34) {
                return null
            }

            return when (colorSpaceNamed) {
                ColorSpace.Named.BT2020_HLG -> BT2020_HLG
                ColorSpace.Named.BT2020_PQ -> BT2020_PQ
                else -> null
            }
        }
    }

    /**
     * Converts a [CameraColorSpace] to a [ColorSpace.Named].
     *
     * @return The corresponding [ColorSpace.Named] or null if the color space is not supported.
     */
    @RequiresApi(26)
    public fun toColorSpaceNamed(): ColorSpace.Named? {
        when (this) {
            UNKNOWN -> return null
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

        if (Build.VERSION.SDK_INT < 34) {
            return null
        }

        return when (this) {
            BT2020_HLG -> ColorSpace.Named.BT2020_HLG
            BT2020_PQ -> ColorSpace.Named.BT2020_PQ
            else -> null
        }
    }
}
