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

import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * The hue and chroma of a color in CAM16, a color appearance model, as viewed in the frame defined
 * by the sRGB standard. Together with L* in L*a*b*, they are the HCT (hue, chroma, tone)
 * coordinates of a color, which Material uses to generate tones of a color.
 *
 * Only hue and chroma are kept, which is all Material needs, so creating a [Cam] doesn't allocate.
 *
 * Ported from frameworks/base/core/java/com/android/internal/graphics/cam/Cam.java, keeping only
 * what Material uses. The results must stay identical to that implementation's Kotlin port:
 * ColorUtilTest checks them.
 */
@JvmInline
internal value class Cam private constructor(private val packedValue: Long) {

    /** Hue in CAM16, in degrees. */
    val hue: Float
        get() = unpackFloat1(packedValue)

    /** Chroma in CAM16. */
    val chroma: Float
        get() = unpackFloat2(packedValue)

    companion object {
        /**
         * Given a hue & chroma in CAM16, L* in L*a*b*, return an ARGB integer. The chroma of the
         * color returned may, and frequently will, be lower than requested. Assumes the color is
         * viewed in the frame defined by the sRGB standard.
         */
        fun getInt(hue: Float, chroma: Float, lstar: Float): Int =
            solveToInt(hue.toDouble(), chroma.toDouble(), lstar.toDouble())

        /**
         * Create a color appearance model from a ARGB integer representing a color. It is assumed
         * the color was viewed in the frame defined in the sRGB standard.
         */
        fun fromInt(argb: Int): Cam {
            val frame = Frame.Default

            // Transform ARGB int to XYZ
            val red = linearized(argb.red)
            val green = linearized(argb.green)
            val blue = linearized(argb.blue)
            val srgbToXyz = SRGB_TO_XYZ
            val x =
                ((red * srgbToXyz[0]) + (green * srgbToXyz[1]) + (blue * srgbToXyz[2])).toFloat()
            val y =
                ((red * srgbToXyz[3]) + (green * srgbToXyz[4]) + (blue * srgbToXyz[5])).toFloat()
            val z =
                ((red * srgbToXyz[6]) + (green * srgbToXyz[7]) + (blue * srgbToXyz[8])).toFloat()

            // Transform XYZ to 'cone'/'rgb' responses
            val matrix = XYZ_TO_CAM16RGB
            val rT = (x * matrix[0]) + (y * matrix[1]) + (z * matrix[2])
            val gT = (x * matrix[3]) + (y * matrix[4]) + (z * matrix[5])
            val bT = (x * matrix[6]) + (y * matrix[7]) + (z * matrix[8])

            // Discount illuminant
            val rD = frame.rgbD[0] * rT
            val gD = frame.rgbD[1] * gT
            val bD = frame.rgbD[2] * bT

            // Chromatic adaptation
            val rAF = (frame.fl * abs(rD) / 100f).pow(0.42f)
            val gAF = (frame.fl * abs(gD) / 100f).pow(0.42f)
            val bAF = (frame.fl * abs(bD) / 100f).pow(0.42f)
            val rA = sign(rD) * 400.0f * rAF / (rAF + 27.13f)
            val gA = sign(gD) * 400.0f * gAF / (gAF + 27.13f)
            val bA = sign(bD) * 400.0f * bAF / (bAF + 27.13f)

            // redness-greenness
            val a = (11f * rA + -12f * gA + bA) / 11.0f
            // yellowness-blueness
            val b = (rA + gA - 2f * bA) / 9.0f

            // auxiliary components
            val u = (20.0f * rA + 20.0f * gA + 21.0f * bA) / 20.0f
            val p2 = (40.0f * rA + 20.0f * gA + bA) / 20.0f

            // hue
            val atanDegrees = atan2(b, a) * 180.0f / PI.toFloat()
            val hue =
                if (atanDegrees < 0) atanDegrees + 360.0f
                else if (atanDegrees >= 360) atanDegrees - 360.0f else atanDegrees

            // achromatic response to color
            val ac = p2 * frame.nbb

            // CAM16 lightness
            val j = 100.0f * (ac / frame.aw).pow((frame.c * frame.z))

            // CAM16 chroma
            val huePrime = if (hue < 20.14) hue + 360 else hue
            val eHue = 0.25f * (cos(huePrime * PI.toFloat() / 180f + 2f) + 3.8f)
            val p1 = 50000.0f / 13.0f * eHue * frame.nc * frame.ncb
            val t = p1 * sqrt((a * a + b * b)) / (u + 0.305f)
            val alpha = t.pow(0.9f) * (1.64f - 0.29f.pow(frame.n)).pow(0.73f)
            val chroma = alpha * sqrt(j / 100f)

            return Cam(packFloats(hue, chroma))
        }
    }
}
