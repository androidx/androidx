/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.graphics.ColorSpace.get
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.colorspace.Rgb
import androidx.compose.ui.graphics.colorspace.TransferParameters
import androidx.compose.ui.graphics.colorspace.WhitePoint

/**
 * Convert the Compose [ColorSpace] into an Android framework [android.graphics.ColorSpace]
 */
@RequiresApi(Build.VERSION_CODES.O)
fun ColorSpace.toAndroidColorSpace(): android.graphics.ColorSpace =
        with(ColorSpaceVerificationHelper) {
            androidColorSpace()
        }

/**
 * Convert the [android.graphics.ColorSpace] into a Compose [ColorSpace]
 */
@RequiresApi(Build.VERSION_CODES.O)
fun android.graphics.ColorSpace.toComposeColorSpace() =
    with(ColorSpaceVerificationHelper) {
        composeColorSpace()
    }

@RequiresApi(Build.VERSION_CODES.O)
private object ColorSpaceVerificationHelper {

    @DoNotInline
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.O)
    fun ColorSpace.androidColorSpace(): android.graphics.ColorSpace {
        return when (this) {
            ColorSpaces.Srgb -> get(android.graphics.ColorSpace.Named.SRGB)
            ColorSpaces.Aces -> get(android.graphics.ColorSpace.Named.ACES)
            ColorSpaces.Acescg -> get(android.graphics.ColorSpace.Named.ACESCG)
            ColorSpaces.AdobeRgb -> get(android.graphics.ColorSpace.Named.ADOBE_RGB)
            ColorSpaces.Bt2020 -> get(android.graphics.ColorSpace.Named.BT2020)
            ColorSpaces.Bt709 -> get(android.graphics.ColorSpace.Named.BT709)
            ColorSpaces.CieLab -> get(android.graphics.ColorSpace.Named.CIE_LAB)
            ColorSpaces.CieXyz -> get(android.graphics.ColorSpace.Named.CIE_XYZ)
            ColorSpaces.DciP3 -> get(android.graphics.ColorSpace.Named.DCI_P3)
            ColorSpaces.DisplayP3 -> get(android.graphics.ColorSpace.Named.DISPLAY_P3)
            ColorSpaces.ExtendedSrgb -> get(android.graphics.ColorSpace.Named.EXTENDED_SRGB)
            ColorSpaces.LinearExtendedSrgb ->
                get(android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB)
            ColorSpaces.LinearSrgb -> get(android.graphics.ColorSpace.Named.LINEAR_SRGB)
            ColorSpaces.Ntsc1953 -> get(android.graphics.ColorSpace.Named.NTSC_1953)
            ColorSpaces.ProPhotoRgb -> get(android.graphics.ColorSpace.Named.PRO_PHOTO_RGB)
            ColorSpaces.SmpteC -> get(android.graphics.ColorSpace.Named.SMPTE_C)
            else -> {
                if (this is Rgb) {
                    val whitePointArray = this.whitePoint.toXyz()
                    val transferParams = this.transferParameters
                    val androidTransferParams = if (transferParams != null) {
                        android.graphics.ColorSpace.Rgb.TransferParameters(
                            transferParams.a,
                            transferParams.b,
                            transferParams.c,
                            transferParams.d,
                            transferParams.e,
                            transferParams.f,
                            transferParams.gamma
                        )
                    } else {
                        null
                    }
                    if (androidTransferParams != null) {
                        android.graphics.ColorSpace.Rgb(
                            this.name,
                            this.primaries,
                            whitePointArray,
                            androidTransferParams
                        )
                    } else {
                        android.graphics.ColorSpace.Rgb(
                            this.name,
                            this.primaries,
                            whitePointArray,
                            this.oetf,
                            this.eotf,
                            this.getMinValue(0),
                            this.getMaxValue(0)
                        )
                    }
                } else {
                    get(android.graphics.ColorSpace.Named.SRGB)
                }
            }
        }
    }

    @DoNotInline
    @JvmStatic
    @RequiresApi(Build.VERSION_CODES.O)
    fun android.graphics.ColorSpace.composeColorSpace(): ColorSpace {
        return when (this.id) {
            android.graphics.ColorSpace.Named.SRGB.ordinal ->
                ColorSpaces.Srgb
            android.graphics.ColorSpace.Named.ACES.ordinal ->
                ColorSpaces.Aces
            android.graphics.ColorSpace.Named.ACESCG.ordinal ->
                ColorSpaces.Acescg
            android.graphics.ColorSpace.Named.ADOBE_RGB.ordinal ->
                ColorSpaces.AdobeRgb
            android.graphics.ColorSpace.Named.BT2020.ordinal ->
                ColorSpaces.Bt2020
            android.graphics.ColorSpace.Named.BT709.ordinal ->
                ColorSpaces.Bt709
            android.graphics.ColorSpace.Named.CIE_LAB.ordinal ->
                ColorSpaces.CieLab
            android.graphics.ColorSpace.Named.CIE_XYZ.ordinal ->
                ColorSpaces.CieXyz
            android.graphics.ColorSpace.Named.DCI_P3.ordinal ->
                ColorSpaces.DciP3
            android.graphics.ColorSpace.Named.DISPLAY_P3.ordinal ->
                ColorSpaces.DisplayP3
            android.graphics.ColorSpace.Named.EXTENDED_SRGB.ordinal ->
                ColorSpaces.ExtendedSrgb
            android.graphics.ColorSpace.Named.LINEAR_EXTENDED_SRGB.ordinal ->
                ColorSpaces.LinearExtendedSrgb
            android.graphics.ColorSpace.Named.LINEAR_SRGB.ordinal ->
                ColorSpaces.LinearSrgb
            android.graphics.ColorSpace.Named.NTSC_1953.ordinal ->
                ColorSpaces.Ntsc1953
            android.graphics.ColorSpace.Named.PRO_PHOTO_RGB.ordinal ->
                ColorSpaces.ProPhotoRgb
            android.graphics.ColorSpace.Named.SMPTE_C.ordinal ->
                ColorSpaces.SmpteC
            else -> {
                if (this is android.graphics.ColorSpace.Rgb) {
                    val transferParams = this.transferParameters
                    val whitePoint = if (this.whitePoint.size == 3) {
                        WhitePoint(this.whitePoint[0], this.whitePoint[1], this.whitePoint[2])
                    } else {
                        WhitePoint(this.whitePoint[0], this.whitePoint[1])
                    }

                    val composeTransferParams = if (transferParams != null) {
                        TransferParameters(
                            gamma = transferParams.g,
                            a = transferParams.a,
                            b = transferParams.b,
                            c = transferParams.c,
                            d = transferParams.d,
                            e = transferParams.e,
                            f = transferParams.f
                        )
                    } else {
                        null
                    }
                    Rgb(
                        name = this.name,
                        primaries = this.primaries,
                        whitePoint = whitePoint,
                        transform = this.transform,
                        oetf = { x -> this.oetf.applyAsDouble(x) },
                        eotf = { x -> this.eotf.applyAsDouble(x) },
                        min = this.getMinValue(0),
                        max = this.getMaxValue(0),
                        transferParameters = composeTransferParams,
                        id = this.id
                    )
                } else {
                    ColorSpaces.Srgb
                }
            }
        }
    }
}