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

package androidx.camera.core

import android.os.Build
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Represents a color space for configuring camera sessions and querying output metadata.
 *
 * Use predefined constants ([UNSPECIFIED], [SRGB], [DISPLAY_P3], [BT2020_HLG], [BT2020_PQ]) for
 * common camera color spaces, or convert from an Android platform
 * [android.graphics.ColorSpace.Named] value on API 26+ via [fromNamed].
 *
 * @see SessionConfig
 * @see CameraInfo
 */
// TODO: b/561876511 - Make this public
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ColorSpace
private constructor(
    /**
     * The ordinal of the corresponding [android.graphics.ColorSpace.Named] entry, or
     * [NAMED_ORDINAL_UNSPECIFIED] when unspecified.
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public val namedOrdinal: Int
) {
    init {
        require(namedOrdinal >= NAMED_ORDINAL_UNSPECIFIED) {
            "Invalid ColorSpace.Named ordinal: $namedOrdinal"
        }
    }

    /**
     * Converts this [ColorSpace] to the corresponding Android [android.graphics.ColorSpace.Named]
     * constant, or `null` if this color space is [UNSPECIFIED] or has no matching platform constant
     * on the current Android version.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    public fun toNamed(): android.graphics.ColorSpace.Named? {
        if (namedOrdinal == NAMED_ORDINAL_UNSPECIFIED) {
            return null
        }
        return android.graphics.ColorSpace.Named.entries.getOrNull(namedOrdinal)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorSpace) return false
        return namedOrdinal == other.namedOrdinal
    }

    override fun hashCode(): Int = namedOrdinal

    override fun toString(): String =
        when (namedOrdinal) {
            NAMED_ORDINAL_UNSPECIFIED -> "ColorSpace.UNSPECIFIED"
            ORDINAL_SRGB -> "ColorSpace.SRGB"
            ORDINAL_DISPLAY_P3 -> "ColorSpace.DISPLAY_P3"
            ORDINAL_BT2020_HLG -> "ColorSpace.BT2020_HLG"
            ORDINAL_BT2020_PQ -> "ColorSpace.BT2020_PQ"
            else -> "ColorSpace(namedOrdinal=$namedOrdinal)"
        }

    public companion object {
        private const val ORDINAL_SRGB: Int = 0
        private const val ORDINAL_DISPLAY_P3: Int = 7
        private const val ORDINAL_BT2020_HLG: Int = 16
        private const val ORDINAL_BT2020_PQ: Int = 17

        /** Sentinel ordinal representing [UNSPECIFIED]. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val NAMED_ORDINAL_UNSPECIFIED: Int = -1

        /**
         * An unspecified color space, allowing the camera device to select a color space based on
         * the stream configuration, including the configured [DynamicRange] and outputs.
         */
        @JvmField public val UNSPECIFIED: ColorSpace = ColorSpace(NAMED_ORDINAL_UNSPECIFIED)

        /**
         * Standard RGB (sRGB) color space (IEC 61966-2-1), corresponding to
         * [android.graphics.ColorSpace.Named.SRGB].
         */
        @JvmField public val SRGB: ColorSpace = ColorSpace(ORDINAL_SRGB)

        /**
         * Display P3 wide-gamut color space (SMPTE EG 432-1), corresponding to
         * [android.graphics.ColorSpace.Named.DISPLAY_P3].
         */
        @JvmField public val DISPLAY_P3: ColorSpace = ColorSpace(ORDINAL_DISPLAY_P3)

        /**
         * ITU-R Rec. 2100 Hybrid Log-Gamma (HLG) wide-gamut HDR color space, corresponding to
         * [android.graphics.ColorSpace.Named.BT2020_HLG].
         */
        @JvmField public val BT2020_HLG: ColorSpace = ColorSpace(ORDINAL_BT2020_HLG)

        /**
         * ITU-R Rec. 2100 Perceptual Quantizer (PQ) wide-gamut HDR color space, corresponding to
         * [android.graphics.ColorSpace.Named.BT2020_PQ].
         */
        @JvmField public val BT2020_PQ: ColorSpace = ColorSpace(ORDINAL_BT2020_PQ)

        /**
         * Returns a [ColorSpace] corresponding to the given Android
         * [android.graphics.ColorSpace.Named] constant.
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        public fun fromNamed(named: android.graphics.ColorSpace.Named): ColorSpace =
            fromNamedOrdinal(named.ordinal)

        /**
         * Returns a [ColorSpace] from a [android.graphics.ColorSpace.Named] ordinal, or
         * [UNSPECIFIED] when [ordinal] is [NAMED_ORDINAL_UNSPECIFIED].
         *
         * @throws IllegalArgumentException if [ordinal] is less than [NAMED_ORDINAL_UNSPECIFIED].
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromNamedOrdinal(
            @IntRange(from = NAMED_ORDINAL_UNSPECIFIED.toLong()) ordinal: Int
        ): ColorSpace =
            when (ordinal) {
                NAMED_ORDINAL_UNSPECIFIED -> UNSPECIFIED
                ORDINAL_SRGB -> SRGB
                ORDINAL_DISPLAY_P3 -> DISPLAY_P3
                ORDINAL_BT2020_HLG -> BT2020_HLG
                ORDINAL_BT2020_PQ -> BT2020_PQ
                else -> ColorSpace(ordinal)
            }
    }
}
