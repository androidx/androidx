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

import kotlin.math.PI
import kotlin.math.cbrt
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The frame, or viewing conditions, where a color was seen. Used, along with a color, to create a
 * color appearance model representing the color.
 *
 * To convert a traditional color to a color appearance model, it requires knowing what conditions
 * the color was observed in. Our perception of color depends on, for example, the tone of the light
 * illuminating the color, how bright that light was, etc.
 *
 * This class is modelled separately from the color appearance model itself because there are a
 * number of calculations during the color => CAM conversion process that depend only on the viewing
 * conditions. Caching those calculations in a Frame instance saves a significant amount of time.
 *
 * Ported from frameworks/base/core/java/com/android/internal/graphics/cam/Frame.java, keeping only
 * what Material uses. The results must stay identical to that implementation's Kotlin port:
 * ColorUtilTest checks them.
 */
internal class Frame
private constructor(
    val n: Float,
    val aw: Float,
    val nbb: Float,
    val ncb: Float,
    val c: Float,
    val nc: Float,
    val rgbD: FloatArray,
    val fl: Float,
    val z: Float,
) {
    companion object {
        // Standard viewing conditions assumed in RGB specification - Stokes, Anderson,
        // Chandrasekar, Motta - A Standard Default Color Space for the Internet: sRGB, 1996.
        //
        // White point = D65
        //
        // Luminance of adapting field: 200 / Pi / 5, units are cd/m^2.
        //
        // sRGB ambient illuminance = 64 lux (per sRGB spec). However, the spec notes this is
        // artificially low and based on monitors in 1990s. Use 200, the sRGB spec says this is the
        // real average, and a survey of lux values on Wikipedia confirms this is a comfortable
        // default: somewhere between a very dark overcast day and office lighting.
        //
        // Per CAM16 introduction paper (Li et al, 2017) Ew = pi * lw, and La = lw * Yb/Yw
        // Ew = ambient environment luminance, in lux.
        // Yb/Yw is taken to be midgray, ~20% relative luminance (XYZ Y 18.4, CIELAB L* 50).
        // Therefore La = (Ew / pi) * .184
        // La = 200 / pi * .184
        // Image surround to 10 degrees = ~20% relative luminance = CIELAB L* 50
        //
        // Not from sRGB standard:
        // Surround = average, 2.0.
        // Discounting illuminant = false, doesn't occur for self-luminous displays
        val Default: Frame =
            make(
                WHITE_POINT_D65,
                (200.0f / PI * yFromLstar(50.0) / 100.0).toFloat(),
                50.0f,
                2.0f,
                false,
            )

        /** Create a custom frame. */
        private fun make(
            whitepoint: FloatArray,
            adaptingLuminance: Float,
            backgroundLstar: Float,
            surround: Float,
            discountingIlluminant: Boolean,
        ): Frame {
            // Transform white point XYZ to 'cone'/'rgb' responses
            val matrix = XYZ_TO_CAM16RGB
            val rW =
                (whitepoint[0] * matrix[0]) +
                    (whitepoint[1] * matrix[1]) +
                    (whitepoint[2] * matrix[2])
            val gW =
                (whitepoint[0] * matrix[3]) +
                    (whitepoint[1] * matrix[4]) +
                    (whitepoint[2] * matrix[5])
            val bW =
                (whitepoint[0] * matrix[6]) +
                    (whitepoint[1] * matrix[7]) +
                    (whitepoint[2] * matrix[8])

            // Scale input surround, domain (0, 2), to CAM16 surround, domain (0.8, 1.0)
            val f = 0.8f + (surround / 10.0f)
            // "Exponential non-linearity"
            val c: Float =
                if ((f >= 0.9)) lerp(0.59f, 0.69f, ((f - 0.9f) * 10.0f))
                else lerp(0.525f, 0.59f, ((f - 0.8f) * 10.0f))
            // Calculate degree of adaptation to illuminant
            var d =
                if (discountingIlluminant) 1.0f
                else
                    f *
                        (1.0f -
                            ((1.0f / 3.6f) *
                                exp(((-adaptingLuminance - 42.0f) / 92.0f).toDouble()).toFloat()))
            // Per Li et al, if D is greater than 1 or less than 0, set it to 1 or 0.
            d = if ((d > 1.0)) 1.0f else if ((d < 0.0)) 0.0f else d
            // Chromatic induction factor
            val nc = f

            // Cone responses to the whitepoint, adjusted for illuminant discounting.
            //
            // Why use 100.0 instead of the white point's relative luminance?
            //
            // Some papers and implementations, for both CAM02 and CAM16, use the Y value of the
            // reference white instead of 100. Fairchild's Color Appearance Models (3rd edition)
            // notes that this is in error: it was included in the CIE 2004a report on CIECAM02,
            // but, later parts of the conversion process account for scaling of appearance relative
            // to the white point relative luminance. This part should simply use 100 as luminance.
            val rgbD =
                floatArrayOf(
                    d * (100.0f / rW) + 1.0f - d,
                    d * (100.0f / gW) + 1.0f - d,
                    d * (100.0f / bW) + 1.0f - d,
                )
            // Luminance-level adaptation factor
            val k = 1.0f / (5.0f * adaptingLuminance + 1.0f)
            val k4 = k * k * k * k
            val k4F = 1.0f - k4
            val fl =
                (k4 * adaptingLuminance) +
                    (0.1f * k4F * k4F * cbrt(5.0 * adaptingLuminance).toFloat())

            // Intermediate factor, ratio of background relative luminance to white relative
            // luminance
            val n = yFromLstar(backgroundLstar.toDouble()).toFloat() / whitepoint[1]

            // Base exponential nonlinearity note Schlomer 2018 has a typo and uses 1.58, the
            // correct factor is 1.48
            val z = 1.48f + sqrt(n)

            // Luminance-level induction factors
            val nbb = 0.725f / n.pow(0.2f)

            // Discounted cone responses to the white point, adjusted for post-chromatic adaptation
            // perceptual nonlinearities.
            val rAFactor = (fl * rgbD[0] * rW / 100f).pow(0.42f)
            val gAFactor = (fl * rgbD[1] * gW / 100f).pow(0.42f)
            val bAFactor = (fl * rgbD[2] * bW / 100f).pow(0.42f)
            val rA = (400.0f * rAFactor) / (rAFactor + 27.13f)
            val gA = (400.0f * gAFactor) / (gAFactor + 27.13f)
            val bA = (400.0f * bAFactor) / (bAFactor + 27.13f)

            val aw = ((2.0f * rA) + gA + (0.05f * bA)) * nbb

            return Frame(n, aw, nbb, nbb, c, nc, rgbD, fl, z)
        }
    }
}

/**
 * The linear interpolation function.
 *
 * @return start if amount = 0 and stop if amount = 1
 */
private fun lerp(start: Float, stop: Float, amount: Float): Float {
    return (1.0f - amount) * start + amount * stop
}
