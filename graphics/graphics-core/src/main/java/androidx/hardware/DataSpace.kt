/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.hardware

import android.hardware.HardwareBuffer
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.graphics.surface.SurfaceControlCompat

/**
 * DataSpace identifies three components of colors - standard (primaries), transfer and range. A
 * DataSpace describes how buffer data, such as from an Image or a HardwareBuffer should be
 * interpreted by both applications and typical hardware. As buffer information is not guaranteed to
 * be representative of color information, while DataSpace is typically used to describe three
 * aspects of interpreting colors, some DataSpaces may describe other typical interpretations of
 * buffer data such as depth information. Note that while ColorSpace and DataSpace are similar
 * concepts, they are not equivalent. Not all ColorSpaces, such as ColorSpace.Named.ACES, are able
 * to be understood by typical hardware blocks so they cannot be DataSpaces. Standard aspect Defines
 * the chromaticity coordinates of the source primaries in terms of the CIE 1931 definition of x and
 * y specified in ISO 11664-1. Transfer aspect Transfer characteristics are the opto-electronic
 * transfer characteristic at the source as a function of linear optical intensity (luminance). For
 * digital signals, E corresponds to the recorded value. Normally, the transfer function is applied
 * in RGB space to each of the R, G and B components independently. This may result in color shift
 * that can be minized by applying the transfer function in Lab space only for the L component.
 * Implementation may apply the transfer function in RGB space for all pixel formats if desired.
 * Range aspect Defines the range of values corresponding to the unit range of 0-1.
 */
class DataSpace private constructor() {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true,
        value =
            [
                DATASPACE_DEPTH,
                DATASPACE_DYNAMIC_DEPTH,
                DATASPACE_HEIF,
                DATASPACE_JPEG_R,
                DATASPACE_UNKNOWN,
                DATASPACE_SCRGB_LINEAR,
                DATASPACE_SRGB,
                DATASPACE_SCRGB,
                DATASPACE_DISPLAY_P3,
                DATASPACE_BT2020_HLG,
                DATASPACE_BT2020_PQ,
                DATASPACE_ADOBE_RGB,
                DATASPACE_JFIF,
                DATASPACE_BT601_625,
                DATASPACE_BT601_525,
                DATASPACE_BT2020,
                DATASPACE_BT709,
                DATASPACE_DCI_P3,
                DATASPACE_SRGB_LINEAR
            ]
    )
    annotation class NamedDataSpace

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [RANGE_UNSPECIFIED, RANGE_FULL, RANGE_LIMITED, RANGE_EXTENDED])
    annotation class DataSpaceRange

    companion object {

        /**
         * Default-assumption data space, when not explicitly specified.
         *
         * It is safest to assume the buffer is an image with sRGB primaries and encoding ranges,
         * but the consumer and/or the producer of the data may simply be using defaults. No
         * automatic gamma transform should be expected, except for a possible display gamma
         * transform when drawn to a screen.
         */
        const val DATASPACE_UNKNOWN = 0

        /**
         * scRGB linear encoding:
         *
         * The red, green, and blue components are stored in extended sRGB space, but are linear,
         * not gamma-encoded. The RGB primaries and the white point are the same as BT.709.
         *
         * The values are floating point. A pixel value of 1.0, 1.0, 1.0 corresponds to sRGB white
         * (D65) at 80 nits. Values beyond the range [0.0 - 1.0] would correspond to other colors
         * spaces and/or HDR content.
         */
        // STANDARD_BT709 | TRANSFER_LINEAR | RANGE_EXTENDED
        const val DATASPACE_SCRGB_LINEAR = 406913024

        /**
         * sRGB gamma encoding:
         *
         * The red, green and blue components are stored in sRGB space, and converted to linear
         * space when read, using the SRGB transfer function for each of the R, G and B components.
         * When written, the inverse transformation is performed.
         *
         * The alpha component, if present, is always stored in linear space and is left unmodified
         * when read or written.
         *
         * Use full range and BT.709 standard.
         */
        const val DATASPACE_SRGB = 142671872 // STANDARD_BT709 | TRANSFER_SRGB | RANGE_FULL

        /**
         * scRGB:
         *
         * The red, green, and blue components are stored in extended sRGB space, and gamma-encoded
         * using the SRGB transfer function. The RGB primaries and the white point are the same as
         * BT.709.
         *
         * The values are floating point. A pixel value of 1.0, 1.0, 1.0 corresponds to sRGB white
         * (D65) at 80 nits. Values beyond the range [0.0 - 1.0] would correspond to other colors
         * spaces and/or HDR content.
         */
        const val DATASPACE_SCRGB = 411107328 // STANDARD_BT709 | TRANSFER_SRGB | RANGE_EXTENDED

        /**
         * Display P3
         *
         * Use same primaries and white-point as DCI-P3 but sRGB transfer function.
         */
        const val DATASPACE_DISPLAY_P3 = 143261696 // STANDARD_DCI_P3 | TRANSFER_SRGB | RANGE_FULL

        /**
         * ITU-R Recommendation 2020 (BT.2020)
         *
         * Ultra High-definition television
         *
         * Use full range, SMPTE 2084 (PQ) transfer and BT2020 standard
         */
        const val DATASPACE_BT2020_PQ = 163971072 // STANDARD_BT2020 | TRANSFER_ST2084 | RANGE_FULL

        /**
         * Adobe RGB
         *
         * Use full range, gamma 2.2 transfer and Adobe RGB primaries Note: Application is
         * responsible for gamma encoding the data as a 2.2 gamma encoding is not supported in HW.
         */
        // STANDARD_ADOBE_RGB | TRANSFER_GAMMA2_2 | RANGE_FULL
        const val DATASPACE_ADOBE_RGB = 151715840

        /**
         * ITU-R Recommendation 2020 (BT.2020)
         *
         * Ultra High-definition television
         *
         * Use full range, BT.709 transfer and BT2020 standard
         */
        // STANDARD_BT2020 | TRANSFER_SMPTE_170M | RANGE_FULL
        const val DATASPACE_BT2020 = 147193856

        /**
         * ITU-R Recommendation 709 (BT.709)
         *
         * High-definition television
         *
         * Use limited range, BT.709 transfer and BT.709 standard.
         */
        // STANDARD_BT709 | TRANSFER_SMPTE_170M | RANGE_LIMITED
        const val DATASPACE_BT709 = 281083904

        /**
         * SMPTE EG 432-1 and SMPTE RP 431-2.
         *
         * Digital Cinema DCI-P3
         *
         * Use full range, gamma 2.6 transfer and D65 DCI-P3 standard Note: Application is
         * responsible for gamma encoding the data as a 2.6 gamma encoding is not supported in HW.
         */
        // STANDARD_DCI_P3 | TRANSFER_GAMMA2_6 | RANGE_FULL
        const val DATASPACE_DCI_P3 = 155844608

        /**
         * sRGB linear encoding:
         *
         * The red, green, and blue components are stored in sRGB space, but are linear, not
         * gamma-encoded. The RGB primaries and the white point are the same as BT.709.
         *
         * The values are encoded using the full range ([0,255] for 8-bit) for all components.
         */
        // STANDARD_BT709 | TRANSFER_LINEAR | RANGE_FULL
        const val DATASPACE_SRGB_LINEAR = 138477568

        /**
         * Depth.
         *
         * This value is valid with formats HAL_PIXEL_FORMAT_Y16 and HAL_PIXEL_FORMAT_BLOB.
         */
        const val DATASPACE_DEPTH = 4096

        /**
         * ISO 16684-1:2011(E) Dynamic Depth.
         *
         * Embedded depth metadata following the dynamic depth specification.
         */
        const val DATASPACE_DYNAMIC_DEPTH = 4098

        /**
         * High Efficiency Image File Format (HEIF).
         *
         * This value is valid with [HardwareBuffer.BLOB][android.hardware.HardwareBuffer.BLOB]
         * format. The combination is an HEIC image encoded by HEIC or HEVC encoder according to
         * ISO/IEC 23008-12.
         */
        const val DATASPACE_HEIF = 4100

        /**
         * Hybrid Log Gamma encoding.
         *
         * Composed of the following -
         * <pre>
         * Primaries: STANDARD_BT2020
         * Transfer: TRANSFER_HLG
         * Range: RANGE_FULL</pre>
         */
        const val DATASPACE_BT2020_HLG = 168165376

        /**
         * JPEG File Interchange Format (JFIF).
         *
         * Composed of the following -
         * <pre>
         * Primaries: STANDARD_BT601_625
         * Transfer: TRANSFER_SMPTE_170M
         * Range: RANGE_FULL</pre>
         *
         * Same model as BT.601-625, but all values (Y, Cb, Cr) range from `0` to `255`
         */
        const val DATASPACE_JFIF = 146931712

        /**
         * ISO/IEC TBD
         *
         * JPEG image with embedded recovery map following the Jpeg/R specification.
         *
         * This value must always remain aligned with the public ImageFormat Jpeg/R definition and
         * is valid with formats: HAL_PIXEL_FORMAT_BLOB: JPEG image encoded by Jpeg/R encoder
         * according to ISO/IEC TBD. The image contains a standard SDR JPEG and a recovery map.
         * Jpeg/R decoders can use the map to recover the input image.
         */
        const val DATASPACE_JPEG_R = 4101

        /**
         * ITU-R Recommendation 601 (BT.601) - 525-line
         *
         * Standard-definition television, 525 Lines (NTSC).
         *
         * Composed of the following -
         * <pre>
         * Primaries: STANDARD_BT601_625
         * Transfer: TRANSFER_SMPTE_170M
         * Range: RANGE_LIMITED</pre>
         */
        const val DATASPACE_BT601_625 = 281149440

        /**
         * ITU-R Recommendation 709 (BT.709)
         *
         * High-definition television.
         *
         * Composed of the following -
         * <pre>
         * Primaries: STANDARD_BT601_525
         * Transfer: TRANSFER_SMPTE_170M
         * Range: RANGE_LIMITED</pre>
         */
        const val DATASPACE_BT601_525 = 281280512

        /** Range characteristics are unknown or are determined by the application. */
        const val RANGE_UNSPECIFIED = 0 shl 27

        /**
         * Full range uses all values for Y, Cb and Cr from `0` to `2^b-1`, where b is the bit depth
         * of the color format.
         */
        const val RANGE_FULL = 1 shl 27

        /**
         * Limited range uses values `16/256*2^b` to `235/256*2^b` for Y, and `1/16*2^b` to
         * `15/16*2^b` for Cb, Cr, R, G and B, where b is the bit depth of the color format.
         *
         * E.g. For 8-bit-depth formats: Luma (Y) samples should range from 16 to 235, inclusive
         * Chroma (Cb, Cr) samples should range from 16 to 240, inclusive
         *
         * For 10-bit-depth formats: Luma (Y) samples should range from 64 to 940, inclusive Chroma
         * (Cb, Cr) samples should range from 64 to 960, inclusive.
         */
        const val RANGE_LIMITED = 2 shl 27

        /**
         * Extended range can be used in combination with FP16 to communicate scRGB or with
         * [SurfaceControlCompat.Transaction.setExtendedRangeBrightness] to indicate an HDR range.
         *
         * When used with floating point pixel formats and #STANDARD_BT709 then [0.0 - 1.0] is the
         * standard sRGB space and values outside the range [0.0 - 1.0] can encode color outside the
         * sRGB gamut. [-0.5, 7.5] is the standard scRGB range. Used to blend/merge multiple
         * dataspaces on a single display.
         *
         * As of [android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE] this may be combined with
         * [SurfaceControlCompat.Transaction.setExtendedRangeBrightness] and other formats such as
         * [HardwareBuffer.RGBA_8888] or [HardwareBuffer.RGBA_1010102] to communicate a variable HDR
         * brightness range
         */
        const val RANGE_EXTENDED = 3 shl 27
    }
}
