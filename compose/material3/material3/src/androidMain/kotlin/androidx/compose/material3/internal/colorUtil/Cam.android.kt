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

import androidx.annotation.Nullable
import androidx.compose.material3.internal.colorUtil.CamUtils.intFromLstar
import androidx.compose.material3.internal.colorUtil.CamUtils.lstarFromInt
import androidx.compose.material3.internal.colorUtil.CamUtils.xyzFromInt
import androidx.compose.material3.internal.colorUtil.HctSolver.solveToInt
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * NOTICE: Fork and kotlin transpilation of
 * frameworks/base/core/java/com/android/internal/graphics/cam/Cam.java Manual changes have not and
 * should not be implemented except for compilation purposes between kotlin and java. Unused methods
 * were also removed.
 *
 * A color appearance model, based on CAM16, extended to use L* as the lightness dimension, and
 * coupled to a gamut mapping algorithm. Creates a color system, enables a digital design system.
 */
internal class Cam(
    /** Hue in CAM16 */
    // CAM16 color dimensions, see getters for documentation.
    val hue: Float,
    /** Chroma in CAM16 */
    val chroma: Float,
    /** Lightness in CAM16 */
    val j: Float,
    /**
     * Colorfulness in CAM16.
     *
     * Prefer chroma, colorfulness is an absolute quantity. For example, a yellow toy car is much
     * more colorful outside than inside, but it has the same chroma in both environments.
     */
    val m: Float,
    /**
     * Saturation in CAM16.
     *
     * Colorfulness in proportion to brightness. Prefer chroma, saturation measures colorfulness
     * relative to the color's own brightness, where chroma is colorfulness relative to white.
     */
    val s: Float,
    /** Lightness coordinate in CAM16-UCS */
    // Coordinates in UCS space. Used to determine color distance, like delta E equations in L*a*b*.
    var jstar: Float,
    /** a* coordinate in CAM16-UCS */
    val astar: Float,
    /** b* coordinate in CAM16-UCS */
    val bstar: Float
) {

    /**
     * Distance in CAM16-UCS space between two colors.
     *
     * Much like L*a*b* was designed to measure distance between colors, the CAM16 standard defined
     * a color space called CAM16-UCS to measure distance between CAM16 colors.
     */
    fun distance(other: Cam): Float {
        val dJ = jstar - other.jstar
        val dA = astar - other.astar
        val dB = bstar - other.bstar
        val dEPrime = sqrt((dJ * dJ + dA * dA + dB * dB).toDouble())
        val dE: Double = 1.41 * dEPrime.pow(0.63)
        return dE.toFloat()
    }

    /** Returns perceived color as an ARGB integer, as viewed in standard sRGB frame. */
    fun viewedInSrgb(): Int {
        return viewed(Frame.Default)
    }

    /** Returns color perceived in a frame as an ARGB integer. */
    fun viewed(frame: Frame): Int {
        val alpha = if ((chroma == 0.0f || j == 0.0f)) 0.0f else chroma / sqrt(j / 100.0f)

        val t = (alpha / (1.64f - 0.29f.pow(frame.n).pow(0.73f)).pow(1.0f / 0.9f))
        val hRad = hue * PI.toFloat() / 180.0f

        val eHue = 0.25f * (cos(hRad + 2.0f) + 3.8f)
        val ac = frame.aw * (j / 100.0f).pow(1.0f / frame.c / frame.z)
        val p1 = eHue * (50000.0f / 13.0f) * frame.nc * frame.ncb
        val p2 = (ac / frame.nbb)

        val hSin = sin(hRad)
        val hCos = cos(hRad)

        val gamma = 23.0f * (p2 + 0.305f) * t / (23.0f * p1 + 11.0f * t * hCos + 108.0f * t * hSin)
        val a = gamma * hCos
        val b = gamma * hSin
        val rA = (460.0f * p2 + 451.0f * a + 288.0f * b) / 1403.0f
        val gA = (460.0f * p2 - 891.0f * a - 261.0f * b) / 1403.0f
        val bA = (460.0f * p2 - 220.0f * a - 6300.0f * b) / 1403.0f

        val rCBase = max(0.0f, (27.13f * abs(rA)) / (400.0f - abs(rA)))
        val rC = sign(rA) * (100.0f / frame.fl) * rCBase.pow(1.0f / 0.42f)
        val gCBase = max(0.0f, (27.13f * abs(gA)) / (400.0f - abs(gA)))
        val gC = sign(gA) * (100.0f / frame.fl) * gCBase.pow(1.0f / 0.42f)
        val bCBase = max(0.0f, (27.13f * abs(bA)) / (400.0f - abs(bA)))
        val bC = (sign(bA) * (100.0f / frame.fl) * bCBase.pow(1.0f / 0.42f))
        val rF = rC / frame.rgbD[0]
        val gF = gC / frame.rgbD[1]
        val bF = bC / frame.rgbD[2]

        val matrix = CamUtils.CAM16RGB_TO_XYZ
        val x = (rF * matrix[0][0]) + (gF * matrix[0][1]) + (bF * matrix[0][2])
        val y = (rF * matrix[1][0]) + (gF * matrix[1][1]) + (bF * matrix[1][2])
        val z = (rF * matrix[2][0]) + (gF * matrix[2][1]) + (bF * matrix[2][2])

        val argb = ColorUtils.XYZToColor(x.toDouble(), y.toDouble(), z.toDouble())
        return argb
    }

    companion object {
        // The maximum difference between the requested L* and the L* returned.
        private const val DL_MAX = 0.2f

        // The maximum color distance, in CAM16-UCS, between a requested color and the color
        // returned.
        private const val DE_MAX = 1.0f

        // When the delta between the floor & ceiling of a binary search for chroma is less than
        // this, the binary search terminates.
        private const val CHROMA_SEARCH_ENDPOINT = 0.4f

        // When the delta between the floor & ceiling of a binary search for J, lightness in CAM16,
        // is less than this, the binary search terminates.
        private const val LIGHTNESS_SEARCH_ENDPOINT = 0.01f

        /**
         * Given a hue & chroma in CAM16, L* in L*a*b*, return an ARGB integer. The chroma of the
         * color returned may, and frequently will, be lower than requested. Assumes the color is
         * viewed in the frame defined by the sRGB standard.
         */
        fun getInt(hue: Float, chroma: Float, lstar: Float): Int {
            return getInt(hue, chroma, lstar, Frame.Default)
        }

        /**
         * Create a color appearance model from a ARGB integer representing a color. It is assumed
         * the color was viewed in the frame defined in the sRGB standard.
         */
        fun fromInt(argb: Int): Cam {
            return fromIntInFrame(argb, Frame.Default)
        }

        /**
         * Create a color appearance model from a ARGB integer representing a color, specifying the
         * frame in which the color was viewed. Prefer Cam.fromInt.
         */
        private fun fromIntInFrame(argb: Int, frame: Frame): Cam {
            // Transform ARGB int to XYZ
            val xyz = xyzFromInt(argb)

            // Transform XYZ to 'cone'/'rgb' responses
            val matrix = CamUtils.XYZ_TO_CAM16RGB
            val rT = (xyz[0] * matrix[0][0]) + (xyz[1] * matrix[0][1]) + (xyz[2] * matrix[0][2])
            val gT = (xyz[0] * matrix[1][0]) + (xyz[1] * matrix[1][1]) + (xyz[2] * matrix[1][2])
            val bT = (xyz[0] * matrix[2][0]) + (xyz[1] * matrix[2][1]) + (xyz[2] * matrix[2][2])

            // Discount illuminant
            val rD = frame.rgbD[0] * rT
            val gD = frame.rgbD[1] * gT
            val bD = frame.rgbD[2] * bT

            // Chromatic adaptation
            val rAF = (frame.fl * abs(rD) / 100f).pow(0.42f)
            val gAF = (frame.fl * abs(gD) / 100f).pow(0.42f)
            val bAF = (frame.fl * abs(bD) / 100f).pow(0.42f)
            val rA = (sign(rD) * 400.0f * rAF / (rAF + 27.13f))
            val gA = (sign(gD) * 400.0f * gAF / (gAF + 27.13f))
            val bA = (sign(bD) * 400.0f * bAF / (bAF + 27.13f))

            // redness-greenness
            val a = (11f * rA + -12f * gA + bA) / 11.0f
            // yellowness-blueness
            val b = (rA + gA - 2f * bA) / 9.0f

            // auxiliary components
            val u = (20.0f * rA + 20.0f * gA + 21.0f * bA) / 20.0f
            val p2 = (40.0f * rA + 20.0f * gA + bA) / 20.0f

            // hue
            val atan2 = atan2(b, a)
            val atanDegrees = atan2 * 180.0f / PI.toFloat()
            val hue =
                if (atanDegrees < 0) atanDegrees + 360.0f
                else if (atanDegrees >= 360) atanDegrees - 360.0f else atanDegrees
            val hueRadians = hue * PI.toFloat() / 180.0f

            // achromatic response to color
            val ac = p2 * frame.nbb

            // CAM16 lightness and brightness
            val j = 100.0f * (ac / frame.aw).pow((frame.c * frame.z))

            // CAM16 chroma, colorfulness, and saturation.
            val huePrime = if ((hue < 20.14)) hue + 360 else hue
            val eHue = 0.25f * (cos(huePrime * PI.toFloat() / 180f + 2f) + 3.8f)
            val p1 = 50000.0f / 13.0f * eHue * frame.nc * frame.ncb
            val t = p1 * sqrt((a * a + b * b)) / (u + 0.305f)
            val alpha = t.pow(0.9f) * (1.64f - 0.29f.pow(frame.n)).pow(0.73f)
            // CAM16 chroma, colorfulness, saturation
            val c = alpha * sqrt(j / 100f)
            val m = c * frame.flRoot
            val s = 50.0f * sqrt(((alpha * frame.c) / (frame.aw + 4.0f)))

            // CAM16-UCS components
            val jstar = (1.0f + 100.0f * 0.007f) * j / (1.0f + 0.007f * j)
            val mstar = 1.0f / 0.0228f * ln((1.0f + 0.0228f * m))
            val astar = mstar * cos(hueRadians)
            val bstar = mstar * sin(hueRadians)

            return Cam(hue, c, j, m, s, jstar, astar, bstar)
        }

        /**
         * Create a CAM from lightness, chroma, and hue coordinates. It is assumed those coordinates
         * were measured in the sRGB standard frame.
         */
        private fun fromJch(j: Float, c: Float, h: Float): Cam {
            return fromJchInFrame(j, c, h, Frame.Default)
        }

        /**
         * Create a CAM from lightness, chroma, and hue coordinates, and also specify the frame in
         * which the color is being viewed.
         */
        private fun fromJchInFrame(j: Float, c: Float, h: Float, frame: Frame): Cam {
            val m = c * frame.flRoot
            val alpha = c / sqrt(j / 100.0).toFloat()
            val s = 50.0f * sqrt(((alpha * frame.c) / (frame.aw + 4.0f)))

            val hueRadians = h * Math.PI.toFloat() / 180.0f
            val jstar = (1.0f + 100.0f * 0.007f) * j / (1.0f + 0.007f * j)
            val mstar = 1.0f / 0.0228f * ln(1.0 + 0.0228 * m).toFloat()
            val astar = mstar * cos(hueRadians.toDouble()).toFloat()
            val bstar = mstar * sin(hueRadians.toDouble()).toFloat()
            return Cam(h, c, j, m, s, jstar, astar, bstar)
        }

        /**
         * Given a hue & chroma in CAM16, L* in L*a*b*, and the frame in which the color will be
         * viewed, return an ARGB integer.
         *
         * The chroma of the color returned may, and frequently will, be lower than requested. This
         * is a fundamental property of color that cannot be worked around by engineering. For
         * example, a red hue, with high chroma, and high L* does not exist: red hues have a maximum
         * chroma below 10 in light shades, creating pink.
         */
        private fun getInt(hue: Float, chroma: Float, lstar: Float, frame: Frame): Int {
            // This is a crucial routine for building a color system, CAM16 itself is not
            // sufficient.
            //
            // * Why these dimensions?
            // Hue and chroma from CAM16 are used because they're the most accurate measures of
            // those quantities. L* from L*a*b* is used because it correlates with luminance,
            // luminance is used to measure contrast for a11y purposes, thus providing a key
            // constraint on what colors can be used.
            //
            // * Why is this routine required to build a color system?
            // In all perceptually accurate color spaces (i.e. L*a*b* and later), `chroma` may be
            // impossible for a given `hue` and `lstar`.
            // For example, a high chroma light red does not exist - chroma is limited to below 10
            // at light red shades, we call that pink. High chroma light green does exist, but not
            // dark.
            // Also, when converting from another color space to RGB, the color may not be able to
            // be represented in RGB. In those cases, the conversion process ends with RGB values
            // outside 0-255.
            // The vast majority of color libraries surveyed simply round to 0 to 255. That is not
            // an option for this library, as it distorts the expected luminance, and thus the
            // expected contrast needed for a11y
            //
            // * What does this routine do?
            // Dealing with colors in one color space not fitting inside RGB is, loosely referred to
            // as gamut mapping or tone mapping. These algorithms are traditionally idiosyncratic,
            // there is no universal answer. However, because the intent of this library is to build
            // a system for digital design, and digital design uses luminance to measure
            // contrast/a11y, we have one very important constraint that leads to an objective
            // algorithm: the L* of the returned color _must_ match the requested L*.
            //
            // Intuitively, if the color must be distorted to fit into the RGB gamut, and the L*
            // requested *must* be fulfilled, than the hue or chroma of the returned color will need
            // to be different from the requested hue/chroma.
            //
            // After exploring both options, it was more intuitive that if the requested chroma
            // could not be reached, it used the highest possible chroma. The alternative was
            // finding the closest hue where the requested chroma could be reached, but that is not
            // nearly as intuitive, as the requested hue is so fundamental to the color description.

            // If the color doesn't have meaningful chroma, return a gray with the requested Lstar.
            //
            // Yellows are very chromatic at L = 100, and blues are very chromatic at L = 0. All the
            // other hues are white at L = 100, and black at L = 0. To preserve consistency for
            // users of this system, it is better to simply return white at L* > 99, and black and
            // L* < 0.

            var huePrime = hue
            if (frame == Frame.Default) {
                // If the viewing conditions are the same as the default sRGB-like viewing
                // conditions, skip to using HctSolver: it uses geometrical insights to find the
                // closest in-gamut match to hue/chroma/lstar.
                return solveToInt(huePrime.toDouble(), chroma.toDouble(), lstar.toDouble())
            }

            if (chroma < 1.0 || Math.round(lstar) <= 0.0 || Math.round(lstar) >= 100.0) {
                return intFromLstar(lstar)
            }

            huePrime = if (huePrime < 0) 0f else min(360.0f, huePrime)

            // The highest chroma possible. Updated as binary search proceeds.
            var high = chroma

            // The guess for the current binary search iteration. Starts off at the highest chroma,
            // thus, if a color is possible at the requested chroma, the search can stop after one
            // try.
            var mid = chroma
            var low = 0.0f
            var isFirstLoop = true

            var answer: Cam? = null

            while (abs((low - high).toDouble()) >= CHROMA_SEARCH_ENDPOINT) {
                // Given the current chroma guess, mid, and the desired hue, find J, lightness in
                // CAM16 color space, that creates a color with L* = `lstar` in the L*a*b* color
                // space.
                val possibleAnswer = findCamByJ(huePrime, mid, lstar)

                if (isFirstLoop) {
                    if (possibleAnswer != null) {
                        return possibleAnswer.viewed(frame)
                    } else {
                        // If this binary search iteration was the first iteration, and this point
                        // has been reached, it means the requested chroma was not available at the
                        // requested hue and L*.
                        // Proceed to a traditional binary search that starts at the midpoint
                        // between the requested chroma and 0.
                        isFirstLoop = false
                        mid = low + (high - low) / 2.0f
                        continue
                    }
                }

                if (possibleAnswer == null) {
                    // There isn't a CAM16 J that creates a color with L* `lstar`. Try a lower
                    // chroma.
                    high = mid
                } else {
                    answer = possibleAnswer
                    // It is possible to create a color. Try higher chroma.
                    low = mid
                }

                mid = low + (high - low) / 2.0f
            }

            // There was no answer: meaning, for the desired hue, there was no chroma low enough to
            // generate a color with the desired L*.
            // All values of L* are possible when there is 0 chroma. Return a color with 0 chroma,
            // i.e. a shade of gray, with the desired L*.
            if (answer == null) {
                return intFromLstar(lstar)
            }

            return answer.viewed(frame)
        }

        // Find J, lightness in CAM16 color space, that creates a color with L* = `lstar` in the
        // L*a*b* color space.
        //
        // Returns null if no J could be found that generated a color with L* `lstar`.
        @Nullable
        private fun findCamByJ(hue: Float, chroma: Float, lstar: Float): Cam? {
            var low = 0.0f
            var high = 100.0f
            var mid: Float
            var bestdL = 1000.0f
            var bestdE = 1000.0f

            var bestCam: Cam? = null
            while (abs((low - high).toDouble()) > LIGHTNESS_SEARCH_ENDPOINT) {
                mid = low + (high - low) / 2
                // Create the intended CAM color
                val camBeforeClip = fromJch(mid, chroma, hue)
                // Convert the CAM color to RGB. If the color didn't fit in RGB, during the
                // conversion, the initial RGB values will be outside 0 to 255. The final RGB values
                // are clipped to 0 to 255, distorting the intended color.
                val clipped = camBeforeClip.viewedInSrgb()
                val clippedLstar = lstarFromInt(clipped)
                val dL = abs((lstar - clippedLstar).toDouble()).toFloat()

                // If the clipped color's L* is within error margin...
                if (dL < DL_MAX) {
                    // ...check if the CAM equivalent of the clipped color is far away from intended
                    // CAM color. For the intended color, use lightness and chroma from the clipped
                    // color, and the intended hue. Callers are wondering what the lightness is,
                    // they know chroma may be distorted, so the only concern here is if the hue
                    // slipped too far.
                    val camClipped = fromInt(clipped)
                    val dE = camClipped.distance(fromJch(camClipped.j, camClipped.chroma, hue))
                    if (dE <= DE_MAX) {
                        bestdL = dL
                        bestdE = dE
                        bestCam = camClipped
                    }
                }

                // If there's no error at all, there's no need to search more.
                //
                // Note: this happens much more frequently than expected, but this is a very
                // delicate property which relies on extremely precise sRGB <=> XYZ calculations, as
                // well as fine tuning of the constants that determine error margins and when the
                // binary search can terminate.
                if (bestdL == 0f && bestdE == 0f) {
                    break
                }

                if (clippedLstar < lstar) {
                    low = mid
                } else {
                    high = mid
                }
            }

            return bestCam
        }
    }
}
