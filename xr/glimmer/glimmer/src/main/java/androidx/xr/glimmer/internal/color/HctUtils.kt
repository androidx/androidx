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

package androidx.xr.glimmer.internal.color

import androidx.annotation.ColorInt
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign

/**
 * Color science and mathematical utilities for the HCT color system.
 *
 * Consolidates all essential mathematical, matrix, and color space translations into a single,
 * highly optimized helper with zero runtime overhead.
 */
internal object HctUtils {
    /**
     * Converts a color from RGB components to ARGB format.
     *
     * @param red the red component, in the range [0, 255]
     * @param green the green component, in the range [0, 255]
     * @param blue the blue component, in the range [0, 255]
     * @return the ARGB representation of the color
     */
    @ColorInt
    fun argbFromRgb(red: Int, green: Int, blue: Int): Int =
        (255 shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)

    /**
     * Converts a color from linear RGB components to ARGB format.
     *
     * @param linrgb the linear RGB components
     * @return the ARGB representation of the color
     */
    @ColorInt
    fun argbFromLinrgb(linrgb: DoubleArray): Int =
        argbFromRgb(delinearized(linrgb[0]), delinearized(linrgb[1]), delinearized(linrgb[2]))

    /**
     * Converts an L* value to an ARGB representation.
     *
     * @param lstar L* in L*a*b*
     * @return ARGB representation of grayscale color with lightness matching L*
     */
    @ColorInt
    fun argbFromLstar(lstar: Double): Int {
        val y = yFromLstar(lstar)
        val component = delinearized(y)
        return argbFromRgb(component, component, component)
    }

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
    fun yFromLstar(lstar: Double): Double = 100.0 * labInvf((lstar + 16.0) / 116.0)

    /**
     * Linearizes an RGB component.
     *
     * @param rgbComponent 0 <= rgb_component <= 255, represents R/G/B channel
     * @return 0.0 <= output <= 100.0, color channel converted to linear RGB space
     */
    fun linearized(rgbComponent: Int): Double {
        val normalized = rgbComponent / 255.0
        return if (normalized <= 0.040449936) {
            normalized / 12.92 * 100.0
        } else {
            ((normalized + 0.055) / 1.055).pow(2.4) * 100.0
        }
    }

    /**
     * Delinearizes an RGB component.
     *
     * @param rgbComponent 0.0 <= rgb_component <= 100.0, represents linear R/G/B channel
     * @return 0 <= output <= 255, color channel converted to regular RGB space
     */
    fun delinearized(rgbComponent: Double): Int {
        val normalized = rgbComponent / 100.0
        val delinearized =
            if (normalized <= 0.0031308) {
                normalized * 12.92
            } else {
                1.055 * normalized.pow(1.0 / 2.4) - 0.055
            }
        return round(delinearized * 255.0).coerceIn(0.0, 255.0).toInt()
    }

    /**
     * Inverse of the labF function.
     *
     * @param ft a value in the L*a*b* color space
     */
    fun labInvf(ft: Double): Double {
        val ft3 = ft * ft * ft
        return if (ft3 > Epsilon) ft3 else (116.0 * ft - 16.0) / Kappa
    }

    /**
     * Sanitizes a degree measure as a floating-point number.
     *
     * @param degrees the degree measure to sanitize.
     * @return a degree measure between 0.0 (inclusive) and 360.0 (exclusive).
     */
    fun sanitizeDegrees(degrees: Double): Double {
        var sanitized = degrees % 360.0
        if (sanitized < 0) sanitized += 360.0
        return sanitized
    }

    /**
     * Multiplies a 1x3 row vector with a 3x3 matrix.
     *
     * @param row the row vector
     * @param matrix the matrix
     * @param dest destination array to store the result, avoiding allocation on every call
     * @return the resulting row vector
     */
    fun matrixMultiply(
        row: DoubleArray,
        matrix: Array<DoubleArray>,
        dest: DoubleArray = DoubleArray(3),
    ): DoubleArray {
        val a = row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2]
        val b = row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2]
        val c = row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2]
        dest[0] = a
        dest[1] = b
        dest[2] = c
        return dest
    }

    /**
     * Multiplies 3 vector components with a 3x3 matrix.
     *
     * @param r the first component of the row vector
     * @param g the second component of the row vector
     * @param b the third component of the row vector
     * @param matrix the matrix
     * @param dest destination array to store the result, avoiding allocation on every call
     * @return the resulting row vector
     */
    fun matrixMultiply(
        r: Double,
        g: Double,
        b: Double,
        matrix: Array<DoubleArray>,
        dest: DoubleArray = DoubleArray(3),
    ): DoubleArray {
        val a = r * matrix[0][0] + g * matrix[0][1] + b * matrix[0][2]
        val bVal = r * matrix[1][0] + g * matrix[1][1] + b * matrix[1][2]
        val c = r * matrix[2][0] + g * matrix[2][1] + b * matrix[2][2]
        dest[0] = a
        dest[1] = bVal
        dest[2] = c
        return dest
    }

    /**
     * Translates an ARGB integer directly into CAM16 Hue using highly optimized calculations that
     * completely skip unused parameters (q, m, s, jstar, etc.).
     *
     * @param argb ARGB representation of a color.
     * @return the CAM16 Hue.
     */
    fun argbToHue(@ColorInt argb: Int): Double {
        // Transform ARGB int to XYZ
        val red = argb and 0x00ff0000 shr 16
        val green = argb and 0x0000ff00 shr 8
        val blue = argb and 0x000000ff
        val redL = linearized(red)
        val greenL = linearized(green)
        val blueL = linearized(blue)
        val x = 0.41233895 * redL + 0.35762064 * greenL + 0.18051042 * blueL
        val y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL
        val z = 0.01932141 * redL + 0.11916382 * greenL + 0.95034478 * blueL

        val vc = ViewingConditions

        // Transform XYZ to 'cone'/'rgb' responses
        val rT = x * 0.401288 + y * 0.650173 + z * -0.051461
        val gT = x * -0.250268 + y * 1.204414 + z * 0.045854
        val bT = x * -0.002079 + y * 0.048952 + z * 0.953127

        // Discount illuminant
        val rD = vc.rgbD[0] * rT
        val gD = vc.rgbD[1] * gT
        val bD = vc.rgbD[2] * bT

        // Chromatic adaptation
        val rAF = (vc.fl * abs(rD) / 100.0).pow(0.42)
        val gAF = (vc.fl * abs(gD) / 100.0).pow(0.42)
        val bAF = (vc.fl * abs(bD) / 100.0).pow(0.42)
        val rA = sign(rD) * 400.0 * rAF / (rAF + 27.13)
        val gA = sign(gD) * 400.0 * gAF / (gAF + 27.13)
        val bA = sign(bD) * 400.0 * bAF / (bAF + 27.13)

        // redness-greenness
        val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
        // yellowness-blueness
        val b = (rA + gA - 2.0 * bA) / 9.0

        val atan2 = atan2(b, a)
        val atanDegrees = Math.toDegrees(atan2)
        return sanitizeDegrees(atanDegrees)
    }

    /**
     * Translates an ARGB integer directly into L* (tone) in HCT color space.
     *
     * @param argb ARGB representation of a color.
     * @return the L* (tone) value.
     */
    fun argbToTone(@ColorInt argb: Int): Double {
        val red = argb and 0x00ff0000 shr 16
        val green = argb and 0x0000ff00 shr 8
        val blue = argb and 0x000000ff
        val redL = linearized(red)
        val greenL = linearized(green)
        val blueL = linearized(blue)
        val y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL
        return lstarFromY(y)
    }

    /**
     * Converts Y in XYZ relative luminance to L* perceptual lightness.
     *
     * @param y Y in XYZ
     * @return L* in L*a*b*
     */
    fun lstarFromY(y: Double): Double {
        val yNormalized = y / 100.0
        return if (yNormalized <= Epsilon) {
            Kappa * yNormalized
        } else {
            Math.cbrt(yNormalized) * 116.0 - 16.0
        }
    }
}

/** Threshold separating linear and cube-root luminance response curves. */
private const val Epsilon = 216.0 / 24389.0

/** Slope constant for perceptual lightness near zero luminance. */
private const val Kappa = 24389.0 / 27.0
