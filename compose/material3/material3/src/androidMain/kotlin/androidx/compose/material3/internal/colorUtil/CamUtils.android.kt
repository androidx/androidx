/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.internal.colorUtil

import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.pow

// Methods for transforming between color spaces, ported from
// frameworks/base/core/java/com/android/internal/graphics/cam/CamUtils.java, keeping only what
// Material uses. The results must stay identical to that implementation's Kotlin port:
// ColorUtilTest checks them.
//
// Methods are named $xFrom$Y. For example, yFromLstar() returns Y in XYZ from L* in L*a*b*.
//
// These methods, generally, convert colors between the L*a*b*, XYZ, and sRGB spaces.
//
// L*a*b* is a perceptually accurate color space. This is particularly important in the L*
// dimension: it measures luminance and unlike lightness measures traditionally used in UI work via
// RGB or HSL, this luminance transitions smoothly, permitting creation of pleasing shades of a
// color, and more pleasing transitions between colors.
//
// XYZ is commonly used as an intermediate color space for converting between one color space to
// another. For example, to convert RGB to L*a*b*, first RGB is converted to XYZ, then XYZ is
// converted to L*a*b*.
//
// sRGB is a "specification originated from work in 1990s through cooperation by Hewlett-Packard
// and Microsoft, and it was designed to be a standard definition of RGB for the internet, which it
// indeed became...The standard is based on a sampling of computer monitors at the time...The whole
// idea of sRGB is that if everyone assumed that RGB meant the same thing, then the results would be
// consistent, and reasonably good. It worked." - Fairchild, Color Models and Systems: Handbook of
// Color Psychology, 2015
//
// The 3x3 matrices here and in HCTSolver are flattened in row-major order. ART doesn't compile
// methods that index into an Array<DoubleArray> when double[][] isn't loaded yet at compile time,
// which would leave the color math interpreted. Primitive arrays are always loaded.

/** Transforms XYZ color space coordinates to 'cone'/'RGB' responses in CAM16. */
internal val XYZ_TO_CAM16RGB: FloatArray =
    floatArrayOf(
        0.401288f,
        0.650173f,
        -0.051461f,
        -0.250268f,
        1.204414f,
        0.045854f,
        -0.002079f,
        0.048952f,
        0.953127f,
    )

/**
 * The sRGB white point, D65, in XYZ - Stokes, Anderson, Chandrasekar, Motta - A Standard Default
 * Color Space for the Internet: sRGB, 1996
 */
internal val WHITE_POINT_D65: FloatArray = floatArrayOf(95.047f, 100.0f, 108.883f)

/**
 * This is a more precise sRGB to XYZ transformation matrix than traditionally used. It was derived
 * using Schlomer's technique of transforming the xyY primaries to XYZ, then applying a correction
 * to ensure mapping from sRGB 1, 1, 1 to the reference white point, D65.
 */
internal val SRGB_TO_XYZ: DoubleArray =
    doubleArrayOf(
        0.41233895,
        0.35762064,
        0.18051042,
        0.2126,
        0.7152,
        0.0722,
        0.01932141,
        0.11916382,
        0.95034478,
    )

private val XYZ_TO_SRGB: DoubleArray =
    doubleArrayOf(
        3.2413774792388685,
        -1.5376652402851851,
        -0.49885366846268053,
        -0.9691452513005321,
        1.8758853451067872,
        0.04156585616912061,
        0.05562093689691305,
        -0.20395524564742123,
        1.0571799111220335,
    )

/**
 * Converts an L* value to an ARGB representation.
 *
 * @param lstar L* in L*a*b*
 * @return ARGB representation of grayscale color with lightness matching L*
 */
internal fun argbFromLstar(lstar: Double): Int {
    val fy = (lstar + 16.0) / 116.0
    val kappa = 24389.0 / 27.0
    val epsilon = 216.0 / 24389.0
    val lExceedsEpsilonKappa = lstar > 8.0
    val y = if (lExceedsEpsilonKappa) fy * fy * fy else lstar / kappa
    val cubeExceedEpsilon = fy * fy * fy > epsilon
    val x = if (cubeExceedEpsilon) fy * fy * fy else lstar / kappa
    val z = if (cubeExceedEpsilon) fy * fy * fy else lstar / kappa
    val whitePoint = WHITE_POINT_D65
    return argbFromXyz(x * whitePoint[0], y * whitePoint[1], z * whitePoint[2])
}

/** Converts a color from XYZ to ARGB. */
private fun argbFromXyz(x: Double, y: Double, z: Double): Int {
    val matrix = XYZ_TO_SRGB
    val linearR = matrix[0] * x + matrix[1] * y + matrix[2] * z
    val linearG = matrix[3] * x + matrix[4] * y + matrix[5] * z
    val linearB = matrix[6] * x + matrix[7] * y + matrix[8] * z
    return argbFromLinrgbComponents(linearR, linearG, linearB)
}

/** Converts a color from linear RGB components to ARGB format. */
internal fun argbFromLinrgbComponents(r: Double, g: Double, b: Double): Int =
    argbFromRgb(delinearized(r), delinearized(g), delinearized(b))

/**
 * Delinearizes an RGB component.
 *
 * @param rgbComponent 0.0 <= rgb_component <= 100.0, represents linear R/G/B channel
 * @return 0 <= output <= 255, color channel converted to regular RGB space
 */
private fun delinearized(rgbComponent: Double): Int {
    val normalized = rgbComponent / 100.0
    val delinearized =
        if (normalized <= 0.0031308) {
            normalized * 12.92
        } else {
            1.055 * normalized.pow(1.0 / 2.4) - 0.055
        }
    return (delinearized * 255.0).fastRoundToInt().fastCoerceIn(0, 255)
}

/** Converts a color from RGB components to ARGB format. */
private fun argbFromRgb(red: Int, green: Int, blue: Int): Int =
    (255 shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)

/**
 * Converts an L* value to a Y value.
 *
 * L* in L*a*b* and Y in XYZ measure the same quantity, luminance.
 *
 * L* measures perceptual luminance, a linear scale. Y in XYZ measures relative luminance, a
 * logarithmic scale.
 *
 * @param lstar L* in L*a*b*
 * @return Y in XYZ
 */
internal fun yFromLstar(lstar: Double): Double {
    val ke = 8.0
    return if (lstar > ke) {
        ((lstar + 16.0) / 116.0).pow(3.0) * 100.0
    } else {
        lstar / (24389.0 / 27.0) * 100.0
    }
}

/**
 * Linearizes an RGB component.
 *
 * @param rgbComponent 0 <= rgb_component <= 255, represents R/G/B channel
 * @return 0.0 <= output <= 100.0, color channel converted to linear RGB space
 */
internal fun linearized(rgbComponent: Int): Float {
    val normalized = rgbComponent.toFloat() / 255.0f

    return if (normalized <= 0.04045f) {
        normalized / 12.92f * 100.0f
    } else {
        ((normalized + 0.055f) / 1.055f).pow(2.4f) * 100.0f
    }
}
