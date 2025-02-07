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

package androidx.wear.compose.material3

import androidx.annotation.ColorInt
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.ColorUtils
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cam is forked from androidx.compose.material3.internal.colorUtils
 * - required to construct errorDim color via SetLuminance in Wear Compose DynamicColorScheme
 *
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
            val xyz = CamUtils.xyzFromInt(argb)

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
                return HctSolver.solveToInt(
                    huePrime.toDouble(),
                    chroma.toDouble(),
                    lstar.toDouble()
                )
            }

            if (chroma < 1.0 || Math.round(lstar) <= 0.0 || Math.round(lstar) >= 100.0) {
                return CamUtils.intFromLstar(lstar)
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
                return CamUtils.intFromLstar(lstar)
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
                val clippedLstar = CamUtils.lstarFromInt(clipped)
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

/**
 * Frame is forked from androidx.compose.material3.internal.colorUtils
 * - required to construct errorDim color via SetLuminance in Wear Compose DynamicColorScheme
 *
 * NOTICE: Fork and kotlin transpilation of
 * frameworks/base/core/java/com/android/internal/graphics/cam/Frame.java Manual changes have not
 * and should not be implemented except for compilation purposes between kotlin and java. Unused
 * methods were also removed.
 *
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
 */
internal class Frame
private constructor(
    @get:VisibleForTesting val n: Float,
    @get:VisibleForTesting val aw: Float,
    @get:VisibleForTesting val nbb: Float,
    val ncb: Float,
    val c: Float,
    val nc: Float,
    @get:VisibleForTesting val rgbD: FloatArray,
    val fl: Float,
    @get:VisibleForTesting val flRoot: Float,
    val z: Float
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
                CamUtils.WHITE_POINT_D65,
                (200.0f / PI * CamUtils.yFromLstar(50.0) / 100.0).toFloat(),
                50.0f,
                2.0f,
                false
            )

        /** Create a custom frame. */
        fun make(
            whitepoint: FloatArray,
            adaptingLuminance: Float,
            backgroundLstar: Float,
            surround: Float,
            discountingIlluminant: Boolean
        ): Frame {
            // Transform white point XYZ to 'cone'/'rgb' responses
            val matrix = CamUtils.XYZ_TO_CAM16RGB
            val rW =
                (whitepoint[0] * matrix[0][0]) +
                    (whitepoint[1] * matrix[0][1]) +
                    (whitepoint[2] * matrix[0][2])
            val gW =
                (whitepoint[0] * matrix[1][0]) +
                    (whitepoint[1] * matrix[1][1]) +
                    (whitepoint[2] * matrix[1][2])
            val bW =
                (whitepoint[0] * matrix[2][0]) +
                    (whitepoint[1] * matrix[2][1]) +
                    (whitepoint[2] * matrix[2][2])

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
            val n = CamUtils.yFromLstar(backgroundLstar.toDouble()).toFloat() / whitepoint[1]

            // Base exponential nonlinearity note Schlomer 2018 has a typo and uses 1.58, the
            // correct factor is 1.48
            val z = 1.48f + sqrt(n)

            // Luminance-level induction factors
            val nbb = 0.725f / n.pow(0.2f)

            // Discounted cone responses to the white point, adjusted for post-chromatic adaptation
            // perceptual nonlinearities.
            val rgbAFactors =
                floatArrayOf(
                    (fl * rgbD[0] * rW / 100f).pow(0.42f),
                    (fl * rgbD[1] * gW / 100f).pow(0.42f),
                    (fl * rgbD[2] * bW / 100f).pow(0.42f)
                )

            val rgbA =
                floatArrayOf(
                    (400.0f * rgbAFactors[0]) / (rgbAFactors[0] + 27.13f),
                    (400.0f * rgbAFactors[1]) / (rgbAFactors[1] + 27.13f),
                    (400.0f * rgbAFactors[2]) / (rgbAFactors[2] + 27.13f),
                )

            val aw = ((2.0f * rgbA[0]) + rgbA[1] + (0.05f * rgbA[2])) * nbb

            return Frame(n, aw, nbb, nbb, c, nc, rgbD, fl, fl.pow(0.25f), z)
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

/**
 * CamUtils is forked from androidx.compose.material3.internal.colorUtil
 * - required to construct errorDim color via SetLuminance in Wear Compose DynamicColorScheme
 *
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

/**
 * HctSolver is forked from androidx.compose.material3.internal.colorUtil
 * - required to construct errorDim color via SetLuminance in Wear Compose DynamicColorScheme
 *
 * NOTICE: Fork and kotlin transpilation of
 * frameworks/base/core/java/com/android/internal/graphics/cam/HctSolver.java Manual changes have
 * not and should not be implemented except for compilation purposes between kotlin and java. Unused
 * methods were also removed.
 *
 * An efficient algorithm for determining the closest sRGB color to a set of HCT coordinates, based
 * on geometrical insights for finding intersections in linear RGB, CAM16, and L*a*b*.
 *
 * Algorithm identified and implemented by Tianguang Zhang. Copied from
 * //java/com/google/ux/material/libmonet/hct on May 22 2022. ColorUtils/MathUtils functions that
 * were required were added to CamUtils.
 */
internal object HctSolver {
    // Matrix used when converting from linear RGB to CAM16.
    private val SCALED_DISCOUNT_FROM_LINRGB: Array<DoubleArray> =
        arrayOf(
            doubleArrayOf(
                0.001200833568784504,
                0.002389694492170889,
                0.0002795742885861124,
            ),
            doubleArrayOf(
                0.0005891086651375999,
                0.0029785502573438758,
                0.0003270666104008398,
            ),
            doubleArrayOf(
                0.00010146692491640572,
                0.0005364214359186694,
                0.0032979401770712076,
            ),
        )

    // Matrix used when converting from CAM16 to linear RGB.
    private val LINRGB_FROM_SCALED_DISCOUNT: Array<DoubleArray> =
        arrayOf(
            doubleArrayOf(
                1373.2198709594231,
                -1100.4251190754821,
                -7.278681089101213,
            ),
            doubleArrayOf(
                -271.815969077903,
                559.6580465940733,
                -32.46047482791194,
            ),
            doubleArrayOf(
                1.9622899599665666,
                -57.173814538844006,
                308.7233197812385,
            ),
        )

    // Weights for transforming a set of linear RGB coordinates to Y in XYZ.
    private val Y_FROM_LINRGB: DoubleArray = doubleArrayOf(0.2126, 0.7152, 0.0722)

    // Lookup table for plane in XYZ's Y axis (relative luminance) that corresponds to a given
    // L* in L*a*b*. HCT's T is L*, and XYZ's Y is directly correlated to linear RGB, this table
    // allows us to thus find the intersection between HCT and RGB, giving a solution to the
    // RGB coordinates that correspond to a given set of HCT coordinates.
    private val CRITICAL_PLANES: DoubleArray =
        doubleArrayOf(
            0.015176349177441876,
            0.045529047532325624,
            0.07588174588720938,
            0.10623444424209313,
            0.13658714259697685,
            0.16693984095186062,
            0.19729253930674434,
            0.2276452376616281,
            0.2579979360165119,
            0.28835063437139563,
            0.3188300904430532,
            0.350925934958123,
            0.3848314933096426,
            0.42057480301049466,
            0.458183274052838,
            0.4976837250274023,
            0.5391024159806381,
            0.5824650784040898,
            0.6277969426914107,
            0.6751227633498623,
            0.7244668422128921,
            0.775853049866786,
            0.829304845476233,
            0.8848452951698498,
            0.942497089126609,
            1.0022825574869039,
            1.0642236851973577,
            1.1283421258858297,
            1.1946592148522128,
            1.2631959812511864,
            1.3339731595349034,
            1.407011200216447,
            1.4823302800086415,
            1.5599503113873272,
            1.6398909516233677,
            1.7221716113234105,
            1.8068114625156377,
            1.8938294463134073,
            1.9832442801866852,
            2.075074464868551,
            2.1693382909216234,
            2.2660538449872063,
            2.36523901573795,
            2.4669114995532007,
            2.5710888059345764,
            2.6777882626779785,
            2.7870270208169257,
            2.898822059350997,
            3.0131901897720907,
            3.1301480604002863,
            3.2497121605402226,
            3.3718988244681087,
            3.4967242352587946,
            3.624204428461639,
            3.754355295633311,
            3.887192587735158,
            4.022731918402185,
            4.160988767090289,
            4.301978482107941,
            4.445716283538092,
            4.592217266055746,
            4.741496401646282,
            4.893568542229298,
            5.048448422192488,
            5.20615066083972,
            5.3666897647573375,
            5.5300801301023865,
            5.696336044816294,
            5.865471690767354,
            6.037501145825082,
            6.212438385869475,
            6.390297286737924,
            6.571091626112461,
            6.7548350853498045,
            6.941541251256611,
            7.131223617812143,
            7.323895587840543,
            7.5195704746346665,
            7.7182615035334345,
            7.919981813454504,
            8.124744458384042,
            8.332562408825165,
            8.543448553206703,
            8.757415699253682,
            8.974476575321063,
            9.194643831691977,
            9.417930041841839,
            9.644347703669503,
            9.873909240696694,
            10.106627003236781,
            10.342513269534024,
            10.58158024687427,
            10.8238400726681,
            11.069304815507364,
            11.317986476196008,
            11.569896988756009,
            11.825048221409341,
            12.083451977536606,
            12.345119996613247,
            12.610063955123938,
            12.878295467455942,
            13.149826086772048,
            13.42466730586372,
            13.702830557985108,
            13.984327217668513,
            14.269168601521828,
            14.55736596900856,
            14.848930523210871,
            15.143873411576273,
            15.44220572664832,
            15.743938506781891,
            16.04908273684337,
            16.35764934889634,
            16.66964922287304,
            16.985093187232053,
            17.30399201960269,
            17.62635644741625,
            17.95219714852476,
            18.281524751807332,
            18.614349837764564,
            18.95068293910138,
            19.290534541298456,
            19.633915083172692,
            19.98083495742689,
            20.331304511189067,
            20.685334046541502,
            21.042933821039977,
            21.404114048223256,
            21.76888489811322,
            22.137256497705877,
            22.50923893145328,
            22.884842241736916,
            23.264076429332462,
            23.6469514538663,
            24.033477234264016,
            24.42366364919083,
            24.817520537484558,
            25.21505769858089,
            25.61628489293138,
            26.021211842414342,
            26.429848230738664,
            26.842203703840827,
            27.258287870275353,
            27.678110301598522,
            28.10168053274597,
            28.529008062403893,
            28.96010235337422,
            29.39497283293396,
            29.83362889318845,
            30.276079891419332,
            30.722335150426627,
            31.172403958865512,
            31.62629557157785,
            32.08401920991837,
            32.54558406207592,
            33.010999283389665,
            33.4802739966603,
            33.953417292456834,
            34.430438229418264,
            34.911345834551085,
            35.39614910352207,
            35.88485700094671,
            36.37747846067349,
            36.87402238606382,
            37.37449765026789,
            37.87891309649659,
            38.38727753828926,
            38.89959975977785,
            39.41588851594697,
            39.93615253289054,
            40.460400508064545,
            40.98864111053629,
            41.520882981230194,
            42.05713473317016,
            42.597404951718396,
            43.141702194811224,
            43.6900349931913,
            44.24241185063697,
            44.798841244188324,
            45.35933162437017,
            45.92389141541209,
            46.49252901546552,
            47.065252796817916,
            47.64207110610409,
            48.22299226451468,
            48.808024568002054,
            49.3971762874833,
            49.9904556690408,
            50.587870934119984,
            51.189430279724725,
            51.79514187861014,
            52.40501387947288,
            53.0190544071392,
            53.637271562750364,
            54.259673423945976,
            54.88626804504493,
            55.517063457223934,
            56.15206766869424,
            56.79128866487574,
            57.43473440856916,
            58.08241284012621,
            58.734331877617365,
            59.39049941699807,
            60.05092333227251,
            60.715611475655585,
            61.38457167773311,
            62.057811747619894,
            62.7353394731159,
            63.417162620860914,
            64.10328893648692,
            64.79372614476921,
            65.48848194977529,
            66.18756403501224,
            66.89098006357258,
            67.59873767827808,
            68.31084450182222,
            69.02730813691093,
            69.74813616640164,
            70.47333615344107,
            71.20291564160104,
            71.93688215501312,
            72.67524319850172,
            73.41800625771542,
            74.16517879925733,
            74.9167682708136,
            75.67278210128072,
            76.43322770089146,
            77.1981124613393,
            77.96744375590167,
            78.74122893956174,
            79.51947534912904,
            80.30219030335869,
            81.08938110306934,
            81.88105503125999,
            82.67721935322541,
            83.4778813166706,
            84.28304815182372,
            85.09272707154808,
            85.90692527145302,
            86.72564993000343,
            87.54890820862819,
            88.3767072518277,
            89.2090541872801,
            90.04595612594655,
            90.88742016217518,
            91.73345337380438,
            92.58406282226491,
            93.43925555268066,
            94.29903859396902,
            95.16341895893969,
            96.03240364439274,
            96.9059996312159,
            97.78421388448044,
            98.6670533535366,
            99.55452497210776,
        )

    /**
     * Sanitizes a small enough angle in radians.
     *
     * @param angle An angle in radians; must not deviate too much from 0.
     * @return A coterminal angle between 0 and 2pi.
     */
    private fun sanitizeRadians(angle: Double): Double {
        return (angle + Math.PI * 8) % (Math.PI * 2)
    }

    /**
     * Delinearizes an RGB component, returning a floating-point number.
     *
     * @param rgbComponent 0.0 <= rgb_component <= 100.0, represents linear R/G/B channel
     * @return 0.0 <= output <= 255.0, color channel converted to regular RGB space
     */
    private fun trueDelinearized(rgbComponent: Double): Double {
        val normalized = rgbComponent / 100.0
        val delinearized =
            if (normalized <= 0.0031308) {
                normalized * 12.92
            } else {
                1.055 * normalized.pow(1.0 / 2.4) - 0.055
            }
        return delinearized * 255
    }

    private fun chromaticAdaptation(component: Double): Double {
        val af: Double = abs(component).pow(0.42)
        return CamUtils.signum(component) * 400.0 * af / (af + 27.13)
    }

    /**
     * Returns the hue of a linear RGB color in CAM16.
     *
     * @param linrgb The linear RGB coordinates of a color.
     * @return The hue of the color in CAM16, in radians.
     */
    private fun hueOf(linrgb: DoubleArray): Double {
        // Calculate scaled discount components using in-lined matrix multiplication to avoid an
        // array allocation.
        val matrix = SCALED_DISCOUNT_FROM_LINRGB
        val rD = linrgb[0] * matrix[0][0] + linrgb[1] * matrix[0][1] + linrgb[2] * matrix[0][2]
        val gD = linrgb[0] * matrix[1][0] + linrgb[1] * matrix[1][1] + linrgb[2] * matrix[1][2]
        val bD = linrgb[0] * matrix[2][0] + linrgb[1] * matrix[2][1] + linrgb[2] * matrix[2][2]

        val rA = chromaticAdaptation(rD)
        val gA = chromaticAdaptation(gD)
        val bA = chromaticAdaptation(bD)
        // redness-greenness
        val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
        // yellowness-blueness
        val b = (rA + gA - 2.0 * bA) / 9.0
        return atan2(b, a)
    }

    /**
     * Cyclic order is the idea that 330° → 5° → 200° is in order, but, 180° → 270° → 210° is not.
     * Visually, A B and C are angles, and they are in cyclic order if travelling from A to C in a
     * way that increases angle (ex. counter-clockwise if +x axis = 0 degrees and +y = 90) means you
     * must cross B.
     *
     * @param a first angle in possibly cyclic triplet
     * @param b second angle in possibly cyclic triplet
     * @param c third angle in possibly cyclic triplet
     * @return true if B is between A and C
     */
    private fun areInCyclicOrder(a: Double, b: Double, c: Double): Boolean {
        val deltaAB = sanitizeRadians(b - a)
        val deltaAC = sanitizeRadians(c - a)
        return deltaAB < deltaAC
    }

    /**
     * Find an intercept using linear interpolation.
     *
     * @param source The starting number.
     * @param mid The number in the middle.
     * @param target The ending number.
     * @return A number t such that lerp(source, target, t) = mid.
     */
    private fun intercept(source: Double, mid: Double, target: Double): Double {
        if (target == source) {
            return target
        }
        return (mid - source) / (target - source)
    }

    /**
     * Linearly interpolate between two points in three dimensions.
     *
     * @param source three dimensions representing the starting point
     * @param t the percentage to travel between source and target, from 0 to 1
     * @param target three dimensions representing the end point
     * @return three dimensions representing the point t percent from source to target.
     */
    private fun lerpPoint(source: DoubleArray, t: Double, target: DoubleArray): DoubleArray {
        return doubleArrayOf(
            source[0] + (target[0] - source[0]) * t,
            source[1] + (target[1] - source[1]) * t,
            source[2] + (target[2] - source[2]) * t,
        )
    }

    /**
     * Intersects a segment with a plane.
     *
     * @param source The coordinates of point A.
     * @param coordinate The R-, G-, or B-coordinate of the plane.
     * @param target The coordinates of point B.
     * @param axis The axis the plane is perpendicular with. (0: R, 1: G, 2: B)
     * @return The intersection point of the segment AB with the plane R=coordinate, G=coordinate,
     *   or B=coordinate
     */
    private fun setCoordinate(
        source: DoubleArray,
        coordinate: Double,
        target: DoubleArray,
        axis: Int
    ): DoubleArray {
        val t = intercept(source[axis], coordinate, target[axis])
        return lerpPoint(source, t, target)
    }

    /** Ensure X is between 0 and 100. */
    private fun isBounded(x: Double): Boolean {
        return x in 0.0..100.0
    }

    /**
     * Returns the nth possible vertex of the polygonal intersection.
     *
     * @param y The Y value of the plane.
     * @param n The zero-based index of the point. 0 <= n <= 11.
     * @return The nth possible vertex of the polygonal intersection of the y plane and the RGB cube
     *   in linear RGB coordinates, if it exists. If the possible vertex lies outside of the cube,
     *   [-1.0, -1.0, -1.0] is returned.
     */
    private fun nthVertex(y: Double, n: Int): DoubleArray {
        val kR = Y_FROM_LINRGB[0]
        val kG = Y_FROM_LINRGB[1]
        val kB = Y_FROM_LINRGB[2]
        val coordA = if (n % 4 <= 1) 0.0 else 100.0
        val coordB = if (n % 2 == 0) 0.0 else 100.0
        if (n < 4) {
            val r = (y - coordA * kG - coordB * kB) / kR
            return if (isBounded(r)) {
                doubleArrayOf(r, coordA, coordB)
            } else {
                doubleArrayOf(-1.0, -1.0, -1.0)
            }
        } else if (n < 8) {
            val g = (y - coordB * kR - coordA * kB) / kG
            return if (isBounded(g)) {
                doubleArrayOf(coordB, g, coordA)
            } else {
                doubleArrayOf(-1.0, -1.0, -1.0)
            }
        } else {
            val b = (y - coordA * kR - coordB * kG) / kB
            return if (isBounded(b)) {
                doubleArrayOf(coordA, coordB, b)
            } else {
                doubleArrayOf(-1.0, -1.0, -1.0)
            }
        }
    }

    /**
     * Finds the segment containing the desired color.
     *
     * @param y The Y value of the color.
     * @param targetHue The hue of the color.
     * @return A list of two sets of linear RGB coordinates, each corresponding to an endpoint of
     *   the segment containing the desired color.
     */
    private fun bisectToSegment(y: Double, targetHue: Double): Array<DoubleArray> {
        var left = doubleArrayOf(-1.0, -1.0, -1.0)
        var right = left
        var leftHue = 0.0
        var rightHue = 0.0
        var initialized = false
        var uncut = true
        for (n in 0..11) {
            val mid = nthVertex(y, n)
            if (mid[0] < 0) {
                continue
            }
            val midHue = hueOf(mid)
            if (!initialized) {
                left = mid
                right = mid
                leftHue = midHue
                rightHue = midHue
                initialized = true
                continue
            }
            if (uncut || areInCyclicOrder(leftHue, midHue, rightHue)) {
                uncut = false
                if (areInCyclicOrder(leftHue, targetHue, midHue)) {
                    right = mid
                    rightHue = midHue
                } else {
                    left = mid
                    leftHue = midHue
                }
            }
        }
        return arrayOf(left, right)
    }

    private fun criticalPlaneBelow(x: Double): Int {
        return floor(x - 0.5).toInt()
    }

    private fun criticalPlaneAbove(x: Double): Int {
        return ceil(x - 0.5).toInt()
    }

    /**
     * Finds a color with the given Y and hue on the boundary of the cube.
     *
     * @param y The Y value of the color.
     * @param targetHue The hue of the color.
     * @return The desired color, in linear RGB coordinates.
     */
    private fun bisectToLimit(y: Double, targetHue: Double): Int {
        val segment = bisectToSegment(y, targetHue)
        var left = segment[0]
        var leftHue = hueOf(left)
        var right = segment[1]
        for (axis in 0..2) {
            if (left[axis] != right[axis]) {
                var lPlane: Int
                var rPlane: Int
                if (left[axis] < right[axis]) {
                    lPlane = criticalPlaneBelow(trueDelinearized(left[axis]))
                    rPlane = criticalPlaneAbove(trueDelinearized(right[axis]))
                } else {
                    lPlane = criticalPlaneAbove(trueDelinearized(left[axis]))
                    rPlane = criticalPlaneBelow(trueDelinearized(right[axis]))
                }
                for (i in 0..7) {
                    if (abs((rPlane - lPlane).toDouble()) <= 1) {
                        break
                    } else {
                        val mPlane = floor((lPlane + rPlane) / 2.0).toInt()
                        val midPlaneCoordinate = CRITICAL_PLANES[mPlane]
                        val mid = setCoordinate(left, midPlaneCoordinate, right, axis)
                        val midHue = hueOf(mid)
                        if (areInCyclicOrder(leftHue, targetHue, midHue)) {
                            right = mid
                            rPlane = mPlane
                        } else {
                            left = mid
                            leftHue = midHue
                            lPlane = mPlane
                        }
                    }
                }
            }
        }
        return CamUtils.argbFromLinrgbComponents(
            (left[0] + right[0]) / 2,
            (left[1] + right[1]) / 2,
            (left[2] + right[2]) / 2
        )
    }

    /** Equation used in CAM16 conversion that removes the effect of chromatic adaptation. */
    private fun inverseChromaticAdaptation(adapted: Double): Double {
        val adaptedAbs = abs(adapted)
        val base = max(0.0, 27.13 * adaptedAbs / (400.0 - adaptedAbs))
        return CamUtils.signum(adapted) * base.pow(1.0 / 0.42)
    }

    /**
     * Finds a color with the given hue, chroma, and Y.
     *
     * @param hueRadians The desired hue in radians.
     * @param chroma The desired chroma.
     * @param y The desired Y.
     * @return The desired color as a hexadecimal integer, if found; 0 otherwise.
     */
    private fun findResultByJ(hueRadians: Double, chroma: Double, y: Double): Int {
        // Initial estimate of j.
        var j = sqrt(y) * 11.0
        // ===========================================================
        // Operations inlined from Cam16 to avoid repeated calculation
        // ===========================================================
        val viewingConditions: Frame = Frame.Default
        val tInnerCoeff: Double = 1 / (1.64 - 0.29.pow(viewingConditions.n.toDouble())).pow(0.73)
        val eHue = 0.25 * (cos(hueRadians + 2.0) + 3.8)
        val p1: Double = (eHue * (50000.0 / 13.0) * viewingConditions.nc * viewingConditions.ncb)
        val hSin = sin(hueRadians)
        val hCos = cos(hueRadians)
        for (iterationRound in 0..4) {
            // ===========================================================
            // Operations inlined from Cam16 to avoid repeated calculation
            // ===========================================================
            val jNormalized = j / 100.0
            val alpha = if (chroma == 0.0 || j == 0.0) 0.0 else chroma / sqrt(jNormalized)
            val t: Double = (alpha * tInnerCoeff).pow(1.0 / 0.9)
            val acExponent: Double = 1.0 / viewingConditions.c / viewingConditions.z
            val ac: Double = viewingConditions.aw * jNormalized.pow(acExponent)
            val p2: Double = ac / viewingConditions.nbb
            val gamma = 23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11 * t * hCos + 108.0 * t * hSin)
            val a = gamma * hCos
            val b = gamma * hSin
            val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
            val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
            val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0
            val rCScaled = inverseChromaticAdaptation(rA)
            val gCScaled = inverseChromaticAdaptation(gA)
            val bCScaled = inverseChromaticAdaptation(bA)
            val matrix = LINRGB_FROM_SCALED_DISCOUNT
            val linrgbR =
                rCScaled * matrix[0][0] + gCScaled * matrix[0][1] + bCScaled * matrix[0][2]
            val linrgbG =
                rCScaled * matrix[1][0] + gCScaled * matrix[1][1] + bCScaled * matrix[1][2]
            val linrgbB =
                rCScaled * matrix[2][0] + gCScaled * matrix[2][1] + bCScaled * matrix[2][2]
            // ===========================================================
            // Operations inlined from Cam16 to avoid repeated calculation
            // ===========================================================
            if (linrgbR < 0 || linrgbG < 0 || linrgbB < 0) {
                return 0
            }
            val kR = Y_FROM_LINRGB[0]
            val kG = Y_FROM_LINRGB[1]
            val kB = Y_FROM_LINRGB[2]
            val fnj = kR * linrgbR + kG * linrgbG + kB * linrgbB
            if (fnj <= 0) {
                return 0
            }
            if (iterationRound == 4 || abs(fnj - y) < 0.002) {
                if (linrgbR > 100.01 || linrgbG > 100.01 || linrgbB > 100.01) {
                    return 0
                }
                return CamUtils.argbFromLinrgbComponents(linrgbR, linrgbG, linrgbB)
            }
            // Iterates with Newton method,
            // Using 2 * fn(j) / j as the approximation of fn'(j)
            j -= (fnj - y) * j / (2 * fnj)
        }
        return 0
    }

    /**
     * Finds an sRGB color with the given hue, chroma, and L*, if possible.
     *
     * @param hueDegrees The desired hue, in degrees.
     * @param chroma The desired chroma.
     * @param lstar The desired L*.
     * @return A hexadecimal representing the sRGB color. The color has sufficiently close hue,
     *   chroma, and L* to the desired values, if possible; otherwise, the hue and L* will be
     *   sufficiently close, and chroma will be maximized.
     */
    fun solveToInt(hueDegrees: Double, chroma: Double, lstar: Double): Int {
        var hueDegreesPrime = hueDegrees
        if (chroma < 0.0001 || lstar < 0.0001 || lstar > 99.9999) {
            return CamUtils.argbFromLstar(lstar)
        }
        hueDegreesPrime = sanitizeDegreesDouble(hueDegreesPrime)
        val hueRadians = Math.toRadians(hueDegreesPrime)
        val y: Double = CamUtils.yFromLstar(lstar)
        val exactAnswer = findResultByJ(hueRadians, chroma, y)
        if (exactAnswer != 0) {
            return exactAnswer
        }
        return bisectToLimit(y, hueRadians)
    }

    /**
     * Sanitizes a degree measure as a floating-point number.
     *
     * @return a degree measure between 0.0 (inclusive) and 360.0 (exclusive).
     */
    private fun sanitizeDegreesDouble(degrees: Double): Double {
        var degreesPrime = degrees
        degreesPrime %= 360.0
        if (degreesPrime < 0) {
            degreesPrime += 360.0
        }
        return degreesPrime
    }
}

// These extension functions are manually copied from androidx.core.graphics.Color
// - required to construct errorDim color via SetLuminance in Wear Compose DynamicColorScheme
private inline val @receiver:ColorInt Int.red: Int
    get() = (this shr 16) and 0xff
private inline val @receiver:ColorInt Int.green: Int
    get() = (this shr 8) and 0xff
private inline val @receiver:ColorInt Int.blue: Int
    get() = this and 0xff
