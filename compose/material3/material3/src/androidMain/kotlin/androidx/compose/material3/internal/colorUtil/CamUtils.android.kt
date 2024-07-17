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

import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * NOTICE: Fork and kotlin transpilation of
 * frameworks/base/core/java/com/android/internal/graphics/cam/CamUtils.java Manual changes have not
 * and should not be implemented except for compilation purposes between kotlin and java. Unused
 * methods were also removed.
 *
 * Collection of methods for transforming between color spaces.
 *
 * Methods are named $xFrom$Y. For example, lstarFromInt() returns L* from an ARGB integer.
 *
 * These methods, generally, convert colors between the L*a*b*, XYZ, and sRGB spaces.
 *
 * L*a*b* is a perceptually accurate color space. This is particularly important in the L*
 * dimension: it measures luminance and unlike lightness measures traditionally used in UI work via
 * RGB or HSL, this luminance transitions smoothly, permitting creation of pleasing shades of a
 * color, and more pleasing transitions between colors.
 *
 * XYZ is commonly used as an intermediate color space for converting between one color space to
 * another. For example, to convert RGB to L*a*b*, first RGB is converted to XYZ, then XYZ is
 * converted to L*a*b*.
 *
 * sRGB is a "specification originated from work in 1990s through cooperation by Hewlett-Packard and
 * Microsoft, and it was designed to be a standard definition of RGB for the internet, which it
 * indeed became...The standard is based on a sampling of computer monitors at the time...The whole
 * idea of sRGB is that if everyone assumed that RGB meant the same thing, then the results would be
 * consistent, and reasonably good. It worked." - Fairchild, Color Models and Systems: Handbook of
 * Color Psychology, 2015
 */
internal object CamUtils {
    // Transforms XYZ color space coordinates to 'cone'/'RGB' responses in CAM16.
    val XYZ_TO_CAM16RGB: Array<FloatArray> =
        arrayOf(
            floatArrayOf(0.401288f, 0.650173f, -0.051461f),
            floatArrayOf(-0.250268f, 1.204414f, 0.045854f),
            floatArrayOf(-0.002079f, 0.048952f, 0.953127f)
        )

    // Transforms 'cone'/'RGB' responses in CAM16 to XYZ color space coordinates.
    val CAM16RGB_TO_XYZ: Array<FloatArray> =
        arrayOf(
            floatArrayOf(1.86206786f, -1.01125463f, 0.14918677f),
            floatArrayOf(0.38752654f, 0.62144744f, -0.00897398f),
            floatArrayOf(-0.01584150f, -0.03412294f, 1.04996444f)
        )

    // Need this, XYZ coordinates in internal ColorUtils are private  sRGB specification has D65
    // whitepoint - Stokes, Anderson, Chandrasekar, Motta - A Standard Default Color Space for the
    // Internet: sRGB, 1996
    val WHITE_POINT_D65: FloatArray = floatArrayOf(95.047f, 100.0f, 108.883f)

    // This is a more precise sRGB to XYZ transformation matrix than traditionally used. It was
    // derived using Schlomer's technique of transforming the xyY primaries to XYZ, then applying a
    // correction to ensure mapping from sRGB 1, 1, 1 to the reference white point, D65.
    private val SRGB_TO_XYZ: Array<DoubleArray> =
        arrayOf(
            doubleArrayOf(0.41233895, 0.35762064, 0.18051042),
            doubleArrayOf(0.2126, 0.7152, 0.0722),
            doubleArrayOf(0.01932141, 0.11916382, 0.95034478),
        )

    private val XYZ_TO_SRGB: Array<DoubleArray> =
        arrayOf(
            doubleArrayOf(
                3.2413774792388685,
                -1.5376652402851851,
                -0.49885366846268053,
            ),
            doubleArrayOf(
                -0.9691452513005321,
                1.8758853451067872,
                0.04156585616912061,
            ),
            doubleArrayOf(
                0.05562093689691305,
                -0.20395524564742123,
                1.0571799111220335,
            ),
        )

    /**
     * The signum function.
     *
     * @return 1 if num > 0, -1 if num < 0, and 0 if num = 0
     */
    fun signum(num: Double): Int {
        return if (num < 0) {
            -1
        } else if (num == 0.0) {
            0
        } else {
            1
        }
    }

    /**
     * Converts an L* value to an ARGB representation.
     *
     * @param lstar L* in L*a*b*
     * @return ARGB representation of grayscale color with lightness matching L*
     */
    fun argbFromLstar(lstar: Double): Int {
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

    /** Converts a color from ARGB to XYZ. */
    private fun argbFromXyz(x: Double, y: Double, z: Double): Int {
        val matrix = XYZ_TO_SRGB
        val linearR = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z
        val linearG = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z
        val linearB = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z
        val r = delinearized(linearR)
        val g = delinearized(linearG)
        val b = delinearized(linearB)
        return argbFromRgb(r, g, b)
    }

    /** Converts a color from linear RGB components to ARGB format. */
    fun argbFromLinrgbComponents(r: Double, g: Double, b: Double): Int {
        return argbFromRgb(delinearized(r), delinearized(g), delinearized(b))
    }

    /**
     * Delinearizes an RGB component.
     *
     * @param rgbComponent 0.0 <= rgb_component <= 100.0, represents linear R/G/B channel
     * @return 0 <= output <= 255, color channel converted to regular RGB space
     */
    private fun delinearized(rgbComponent: Double): Int {
        val normalized = rgbComponent / 100.0
        val delinearized: Double =
            if (normalized <= 0.0031308) {
                normalized * 12.92
            } else {
                1.055 * normalized.pow(1.0 / 2.4) - 0.055
            }
        return clampInt(0, 255, Math.round(delinearized * 255.0).toInt())
    }

    /**
     * Clamps an integer between two integers.
     *
     * @return input when min <= input <= max, and either min or max otherwise.
     */
    private fun clampInt(min: Int, max: Int, input: Int): Int {
        if (input < min) {
            return min
        } else if (input > max) {
            return max
        }

        return input
    }

    /** Converts a color from RGB components to ARGB format. */
    private fun argbFromRgb(red: Int, green: Int, blue: Int): Int {
        return (255 shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)
    }

    fun intFromLstar(lstar: Float): Int {
        if (lstar < 1) {
            return -0x1000000
        } else if (lstar > 99) {
            return -0x1
        }

        // XYZ to LAB conversion routine, assume a and b are 0.
        val fy = (lstar + 16.0f) / 116.0f

        // fz = fx = fy because a and b are 0
        val fz = fy

        val kappa = 24389f / 27f
        val epsilon = 216f / 24389f
        val lExceedsEpsilonKappa = (lstar > 8.0f)
        val yT = if (lExceedsEpsilonKappa) fy * fy * fy else lstar / kappa
        val cubeExceedEpsilon = (fy * fy * fy) > epsilon
        val xT = if (cubeExceedEpsilon) fy * fy * fy else (116f * fy - 16f) / kappa
        val zT = if (cubeExceedEpsilon) fz * fz * fz else (116f * fy - 16f) / kappa

        return ColorUtils.XYZToColor(
            (xT * WHITE_POINT_D65[0]).toDouble(),
            (yT * WHITE_POINT_D65[1]).toDouble(),
            (zT * WHITE_POINT_D65[2]).toDouble()
        )
    }

    /** Returns L* from L*a*b*, perceptual luminance, from an ARGB integer (ColorInt). */
    fun lstarFromInt(argb: Int): Float {
        return lstarFromY(yFromInt(argb))
    }

    private fun lstarFromY(y: Float): Float {
        var yPrime = y
        yPrime /= 100.0f
        val e = 216f / 24389f
        val yIntermediate: Float
        if (yPrime <= e) {
            return ((24389f / 27f) * yPrime)
        } else {
            yIntermediate = cbrt(yPrime.toDouble()).toFloat()
        }
        return 116f * yIntermediate - 16f
    }

    private fun yFromInt(argb: Int): Float {
        val r = linearized(argb.red)
        val g = linearized(argb.green)
        val b = linearized(argb.blue)
        val matrix = SRGB_TO_XYZ
        val y = (r * matrix[1][0]) + (g * matrix[1][1]) + (b * matrix[1][2])
        return y.toFloat()
    }

    fun xyzFromInt(argb: Int): FloatArray {
        val r = linearized(argb.red)
        val g = linearized(argb.green)
        val b = linearized(argb.blue)

        val matrix = SRGB_TO_XYZ
        val x = (r * matrix[0][0]) + (g * matrix[0][1]) + (b * matrix[0][2])
        val y = (r * matrix[1][0]) + (g * matrix[1][1]) + (b * matrix[1][2])
        val z = (r * matrix[2][0]) + (g * matrix[2][1]) + (b * matrix[2][2])
        return floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat())
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
    fun yFromLstar(lstar: Double): Double {
        val ke = 8.0
        return if (lstar > ke) {
            ((lstar + 16.0) / 116.0).pow(3.0) * 100.0
        } else {
            lstar / (24389.0 / 27.0) * 100.0
        }
    }

    private fun linearized(rgbComponent: Int): Float {
        val normalized = rgbComponent.toFloat() / 255.0f

        return if (normalized <= 0.04045f) {
            normalized / 12.92f * 100.0f
        } else {
            ((normalized + 0.055f) / 1.055f).pow(2.4f) * 100.0f
        }
    }
}
