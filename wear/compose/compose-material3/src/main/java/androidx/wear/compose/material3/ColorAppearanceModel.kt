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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

// NOTICE: The classes in this file (Hct, Cam16, ViewingConditions, HctSolver, ColorUtils and
// MathUtils) are a fork and trim of Material Color Utilities (libmonet,
// https://github.com/material-foundation/material-color-utilities), using its Kotlin port
// (kotlin/ on GitHub, com.google.ux.material.libmonetkt in google3) as the source.
//
// Each class links to the exact upstream file it was forked from. Apart from androidx ktfmt
// formatting and the changes listed in each class's KDoc, the code is identical to
// the linked upstream file. Where upstream code triggers an androidx check, it is kept as is and
// the
// check is suppressed (@Suppress) rather than rewritten.
// Do not make other manual changes, so the fork stays easy to compare and update.
//
// All calculations are done in double precision so results are bit-exact with libmonet, which is
// what Android SystemUI (monet) uses to generate dynamic color schemes.

/**
 * HCT, hue, chroma, and tone. A color system that provides a perceptually accurate color
 * measurement system that can also accurately render what colors will appear as in different
 * lighting environments.
 *
 * Using L* creates a link between the color system, contrast, and thus accessibility. Contrast
 * ratio depends on relative luminance, or Y in the XYZ color space. L*, or perceptual luminance can
 * be calculated from Y.
 *
 * Note: this class intentionally does not override equals/hashCode (identity semantics), matching
 * libmonet; [TemperatureCache] relies on that.
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/hct/Hct.kt
 *
 * Changes:
 * - setHue/setChroma/setTone removed, so Hct is immutable once created; inViewingConditions and
 *   isBlue/isYellow/isCyan removed (unused).
 */
internal class Hct private constructor(argb: Int) {
    var hue = 0.0
        private set

    var chroma = 0.0
        private set

    var tone = 0.0
        private set

    private var argb = 0

    init {
        setInternalState(argb)
    }

    fun toInt(): Int {
        return argb
    }

    override fun toString(): String {
        return "HCT(${hue.roundToInt()}, ${chroma.roundToInt()}, ${tone.roundToInt()})"
    }

    private fun setInternalState(argb: Int) {
        this.argb = argb
        val cam = Cam16.fromInt(argb)
        hue = cam.hue
        chroma = cam.chroma
        tone = ColorUtils.lstarFromArgb(argb)
    }

    companion object {
        /**
         * Create an HCT color from hue, chroma, and tone.
         *
         * @param hue 0 <= hue < 360; invalid values are corrected.
         * @param chroma 0 <= chroma < ?; Informally, colorfulness. The color returned may be lower
         *   than the requested chroma. Chroma has a different maximum for any given hue and tone.
         * @param tone 0 <= tone <= 100; invalid values are corrected.
         * @return HCT representation of a color in default viewing conditions.
         */
        @JvmStatic
        fun from(hue: Double, chroma: Double, tone: Double): Hct {
            val argb = HctSolver.solveToInt(hue, chroma, tone)
            return Hct(argb)
        }

        /**
         * Create an HCT color from a color.
         *
         * @param argb ARGB representation of a color.
         * @return HCT representation of a color in default viewing conditions
         */
        @JvmStatic
        fun fromInt(argb: Int): Hct {
            return Hct(argb)
        }
    }
}

/**
 * CAM16, a color appearance model. Colors are not just defined by their hex code, but rather, a hex
 * code and viewing conditions.
 *
 * Only the ARGB -> CAM16 direction is needed here; the CAM16 -> ARGB direction is handled by
 * [HctSolver].
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/hct/Cam16.kt
 *
 * Changes:
 * - Only fromInt is kept, and only hue and chroma are stored. j/q/m/s, the UCS coordinates
 *   (jstar/astar/bstar), distance, fromJch/fromUcs, viewed, xyzInViewingConditions, toInt and
 *   CAM16RGB_TO_XYZ removed (unused).
 * - fromIntInViewingConditions and fromXyzInViewingConditions are private.
 */
internal class Cam16
private constructor(
    /** Hue in CAM16 */
    val hue: Double,
    /** Chroma in CAM16 */
    val chroma: Double,
) {
    companion object {
        // Transforms XYZ color space coordinates to 'cone'/'RGB' responses in CAM16.
        internal val XYZ_TO_CAM16RGB =
            arrayOf(
                doubleArrayOf(0.401288, 0.650173, -0.051461),
                doubleArrayOf(-0.250268, 1.204414, 0.045854),
                doubleArrayOf(-0.002079, 0.048952, 0.953127),
            )

        /**
         * Create a CAM16 color from a color, assuming the color was viewed in default viewing
         * conditions.
         *
         * @param argb ARGB representation of a color.
         */
        fun fromInt(argb: Int): Cam16 {
            return fromIntInViewingConditions(argb, ViewingConditions.DEFAULT)
        }

        /**
         * Create a CAM16 color from a color in defined viewing conditions.
         *
         * @param argb ARGB representation of a color.
         * @param viewingConditions Information about the environment where the color was observed.
         */
        // The RGB => XYZ conversion matrix elements are derived scientific constants. While the
        // values may differ at runtime due to floating point imprecision, keeping the values the
        // same, and accurate, across implementations takes precedence.
        private fun fromIntInViewingConditions(
            argb: Int,
            viewingConditions: ViewingConditions,
        ): Cam16 {
            // Transform ARGB int to XYZ
            val red = argb and 0x00ff0000 shr 16
            val green = argb and 0x0000ff00 shr 8
            val blue = argb and 0x000000ff
            val redL = ColorUtils.linearized(red)
            val greenL = ColorUtils.linearized(green)
            val blueL = ColorUtils.linearized(blue)
            val x = 0.41233895 * redL + 0.35762064 * greenL + 0.18051042 * blueL
            val y = 0.2126 * redL + 0.7152 * greenL + 0.0722 * blueL
            val z = 0.01932141 * redL + 0.11916382 * greenL + 0.95034478 * blueL
            return fromXyzInViewingConditions(x, y, z, viewingConditions)
        }

        private fun fromXyzInViewingConditions(
            x: Double,
            y: Double,
            z: Double,
            viewingConditions: ViewingConditions,
        ): Cam16 {
            // Transform XYZ to 'cone'/'rgb' responses
            val matrix = XYZ_TO_CAM16RGB
            val rT = x * matrix[0][0] + y * matrix[0][1] + z * matrix[0][2]
            val gT = x * matrix[1][0] + y * matrix[1][1] + z * matrix[1][2]
            val bT = x * matrix[2][0] + y * matrix[2][1] + z * matrix[2][2]

            // Discount illuminant
            val rD = viewingConditions.rgbD[0] * rT
            val gD = viewingConditions.rgbD[1] * gT
            val bD = viewingConditions.rgbD[2] * bT

            // Chromatic adaptation
            val rAF = (viewingConditions.fl * abs(rD) / 100.0).pow(0.42)
            val gAF = (viewingConditions.fl * abs(gD) / 100.0).pow(0.42)
            val bAF = (viewingConditions.fl * abs(bD) / 100.0).pow(0.42)
            val rA = sign(rD) * 400.0 * rAF / (rAF + 27.13)
            val gA = sign(gD) * 400.0 * gAF / (gAF + 27.13)
            val bA = sign(bD) * 400.0 * bAF / (bAF + 27.13)

            // redness-greenness
            val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
            // yellowness-blueness
            val b = (rA + gA - 2.0 * bA) / 9.0

            // auxiliary components
            val u = (20.0 * rA + 20.0 * gA + 21.0 * bA) / 20.0
            val p2 = (40.0 * rA + 20.0 * gA + bA) / 20.0

            // hue
            val atan2 = atan2(b, a)
            val atanDegrees = Math.toDegrees(atan2)
            val hue = MathUtils.sanitizeDegreesDouble(atanDegrees)

            // achromatic response to color
            val ac = p2 * viewingConditions.nbb

            // CAM16 lightness
            val j =
                100.0 * (ac / viewingConditions.aw).pow(viewingConditions.c * viewingConditions.z)

            // CAM16 chroma
            val huePrime = if (hue < 20.14) hue + 360 else hue
            val eHue = 0.25 * (cos(Math.toRadians(huePrime) + 2.0) + 3.8)
            val p1 = 50000.0 / 13.0 * eHue * viewingConditions.nc * viewingConditions.ncb
            val t = p1 * hypot(a, b) / (u + 0.305)
            val alpha = (1.64 - 0.29.pow(viewingConditions.n)).pow(0.73) * t.pow(0.9)
            val c = alpha * sqrt(j / 100.0)
            // Only hue and chroma are relevant here for this fork. Other calculations such as
            // colorfullness and saturation are omitted. Cam16 class is constructed using only those
            // parameters.
            return Cam16(hue, c)
        }
    }
}

/**
 * In traditional color spaces, a color can be identified solely by the observer's measurement of
 * the color. Color appearance models such as CAM16 also use information about the environment where
 * the color was observed, known as the viewing conditions.
 *
 * This class caches intermediate values of the CAM16 conversion process that depend only on viewing
 * conditions, enabling speed ups.
 *
 * Parameters are intermediate values of the CAM16 conversion process. Their names are shorthand for
 * technical color science terminology, this class would not benefit from documenting them
 * individually. A brief overview is available in the CAM16 specification, and a complete overview
 * requires a color science textbook, such as Fairchild's Color Appearance Models.
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/hct/ViewingConditions.kt
 *
 * Changes:
 * - Only DEFAULT is exposed: make and defaultWithBackgroundLstar are private.
 * - make has @Suppress("NAME_SHADOWING") (upstream shadows the backgroundLstar parameter).
 */
@ConsistentCopyVisibility
internal data class ViewingConditions
private constructor(
    val n: Double,
    val aw: Double,
    val nbb: Double,
    internal val ncb: Double,
    internal val c: Double,
    internal val nc: Double,
    val rgbD: DoubleArray,
    internal val fl: Double,
    val flRoot: Double,
    internal val z: Double,
) {
    companion object {
        /** sRGB-like viewing conditions. */
        val DEFAULT = defaultWithBackgroundLstar(50.0)

        /**
         * Create ViewingConditions from a simple, physically relevant, set of parameters.
         *
         * @param whitePoint White point, measured in the XYZ color space. default = D65, or sunny
         *   day afternoon
         * @param adaptingLuminance The luminance of the adapting field. Informally, how bright it
         *   is in the room where the color is viewed. Can be calculated from lux by multiplying lux
         *   by 0.0586. default = 11.72, or 200 lux.
         * @param backgroundLstar The lightness of the area surrounding the color. measured by L* in
         *   L*a*b*. default = 50.0
         * @param surround A general description of the lighting surrounding the color. 0 is pitch
         *   dark, like watching a movie in a theater. 1.0 is a dimly light room, like watching TV
         *   at home at night. 2.0 means there is no difference between the lighting on the color
         *   and around it. default = 2.0
         * @param discountingIlluminant Whether the eye accounts for the tint of the ambient
         *   lighting, such as knowing an apple is still red in green light. default = false, the
         *   eye does not perform this process on self-luminous objects like displays.
         */
        @Suppress("NAME_SHADOWING") // Kept identical to upstream.
        private fun make(
            whitePoint: DoubleArray,
            adaptingLuminance: Double,
            backgroundLstar: Double,
            surround: Double,
            discountingIlluminant: Boolean,
        ): ViewingConditions {
            // A background of pure black is non-physical and leads to infinities that represent
            // the idea that any color viewed in pure black can't be seen.
            val backgroundLstar = max(0.1, backgroundLstar)
            // Transform white point XYZ to 'cone'/'rgb' responses
            val matrix = Cam16.XYZ_TO_CAM16RGB
            val xyz = whitePoint
            val rW = xyz[0] * matrix[0][0] + xyz[1] * matrix[0][1] + xyz[2] * matrix[0][2]
            val gW = xyz[0] * matrix[1][0] + xyz[1] * matrix[1][1] + xyz[2] * matrix[1][2]
            val bW = xyz[0] * matrix[2][0] + xyz[1] * matrix[2][1] + xyz[2] * matrix[2][2]
            val f = 0.8 + surround / 10.0
            val c =
                if (f >= 0.9) {
                    MathUtils.lerp(0.59, 0.69, (f - 0.9) * 10.0)
                } else {
                    MathUtils.lerp(0.525, 0.59, (f - 0.8) * 10.0)
                }
            var d =
                if (discountingIlluminant) {
                    1.0
                } else {
                    f * (1.0 - 1.0 / 3.6 * exp((-adaptingLuminance - 42.0) / 92.0))
                }
            d = d.coerceIn(0.0, 1.0)
            val nc = f
            val rgbD =
                doubleArrayOf(
                    d * (100.0 / rW) + 1.0 - d,
                    d * (100.0 / gW) + 1.0 - d,
                    d * (100.0 / bW) + 1.0 - d,
                )
            val k = 1.0 / (5.0 * adaptingLuminance + 1.0)
            val k4 = k * k * k * k
            val k4F = 1.0 - k4
            val fl = k4 * adaptingLuminance + 0.1 * k4F * k4F * cbrt(5.0 * adaptingLuminance)
            val n = ColorUtils.yFromLstar(backgroundLstar) / whitePoint[1]
            val z = 1.48 + sqrt(n)
            val nbb = 0.725 / n.pow(0.2)
            val ncb = nbb
            val rgbAFactors =
                doubleArrayOf(
                    (fl * rgbD[0] * rW / 100.0).pow(0.42),
                    (fl * rgbD[1] * gW / 100.0).pow(0.42),
                    (fl * rgbD[2] * bW / 100.0).pow(0.42),
                )
            val rgbA =
                doubleArrayOf(
                    400.0 * rgbAFactors[0] / (rgbAFactors[0] + 27.13),
                    400.0 * rgbAFactors[1] / (rgbAFactors[1] + 27.13),
                    400.0 * rgbAFactors[2] / (rgbAFactors[2] + 27.13),
                )
            val aw = (2.0 * rgbA[0] + rgbA[1] + 0.05 * rgbA[2]) * nbb
            return ViewingConditions(n, aw, nbb, ncb, c, nc, rgbD, fl, fl.pow(0.25), z)
        }

        /**
         * Create sRGB-like viewing conditions with a custom background lstar.
         *
         * Default viewing conditions have a lstar of 50, midgray.
         */
        private fun defaultWithBackgroundLstar(lstar: Double): ViewingConditions {
            return make(
                ColorUtils.whitePointD65(),
                200.0 / PI * ColorUtils.yFromLstar(50.0) / 100f,
                lstar,
                2.0,
                false,
            )
        }
    }
}

/**
 * An efficient algorithm for determining the closest sRGB color to a set of HCT coordinates, based
 * on geometrical insights for finding intersections in linear RGB, CAM16, and L*a*b*.
 *
 * Algorithm identified and implemented by Tianguang Zhang.
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/hct/HctSolver.kt
 *
 * Changes:
 * - solveToCam removed (unused).
 * - All helpers except solveToInt are private.
 */
internal object HctSolver {
    private val SCALED_DISCOUNT_FROM_LINRGB =
        arrayOf(
            doubleArrayOf(0.001200833568784504, 0.002389694492170889, 0.0002795742885861124),
            doubleArrayOf(0.0005891086651375999, 0.0029785502573438758, 0.0003270666104008398),
            doubleArrayOf(0.00010146692491640572, 0.0005364214359186694, 0.0032979401770712076),
        )
    private val LINRGB_FROM_SCALED_DISCOUNT =
        arrayOf(
            doubleArrayOf(1373.2198709594231, -1100.4251190754821, -7.278681089101213),
            doubleArrayOf(-271.815969077903, 559.6580465940733, -32.46047482791194),
            doubleArrayOf(1.9622899599665666, -57.173814538844006, 308.7233197812385),
        )
    private val Y_FROM_LINRGB = doubleArrayOf(0.2126, 0.7152, 0.0722)
    private val CRITICAL_PLANES =
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
        return (angle + PI * 8) % (PI * 2)
    }

    /**
     * Delinearizes an RGB component, returning a floating-point number.
     *
     * @param rgbComponent 0.0 <= rgb_component <= 100.0, represents linear R/G/B channel
     * @return 0.0 <= output <= 255.0, color channel converted to regular RGB space
     */
    private fun trueDelinearized(rgbComponent: Double): Double {
        val normalized = rgbComponent / 100.0
        val delinearized: Double =
            if (normalized <= 0.0031308) {
                normalized * 12.92
            } else {
                1.055 * normalized.pow(1.0 / 2.4) - 0.055
            }
        return delinearized * 255.0
    }

    private fun chromaticAdaptation(component: Double): Double {
        val af = abs(component).pow(0.42)
        return sign(component) * 400.0 * af / (af + 27.13)
    }

    /**
     * Returns the hue of a linear RGB color in CAM16.
     *
     * @param linrgb The linear RGB coordinates of a color.
     * @return The hue of the color in CAM16, in radians.
     */
    private fun hueOf(linrgb: DoubleArray): Double {
        val scaledDiscount = MathUtils.matrixMultiply(linrgb, SCALED_DISCOUNT_FROM_LINRGB)
        val rA = chromaticAdaptation(scaledDiscount[0])
        val gA = chromaticAdaptation(scaledDiscount[1])
        val bA = chromaticAdaptation(scaledDiscount[2])
        // redness-greenness
        val a = (11.0 * rA + -12.0 * gA + bA) / 11.0
        // yellowness-blueness
        val b = (rA + gA - 2.0 * bA) / 9.0
        return atan2(b, a)
    }

    private fun areInCyclicOrder(a: Double, b: Double, c: Double): Boolean {
        val deltaAB = sanitizeRadians(b - a)
        val deltaAC = sanitizeRadians(c - a)
        return deltaAB < deltaAC
    }

    /**
     * Solves the lerp equation.
     *
     * @param source The starting number.
     * @param mid The number in the middle.
     * @param target The ending number.
     * @return A number t such that lerp(source, target, t) = mid.
     */
    private fun intercept(source: Double, mid: Double, target: Double): Double {
        return (mid - source) / (target - source)
    }

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
        axis: Int,
    ): DoubleArray {
        val t = intercept(source[axis], coordinate, target[axis])
        return lerpPoint(source, t, target)
    }

    private fun isBounded(x: Double): Boolean {
        return 0.0 <= x && x <= 100.0
    }

    /**
     * Returns the nth possible vertex of the polygonal intersection.
     *
     * @param y The Y value of the plane.
     * @param n The zero-based index of the point. 0 <= n <= 11.
     * @return The nth possible vertex of the polygonal intersection of the y plane and the RGB
     *   cube, in linear RGB coordinates, if it exists. If this possible vertex lies outside of the
     *   cube, null is returned.
     */
    private fun nthVertex(y: Double, n: Int): DoubleArray? {
        val kR = Y_FROM_LINRGB[0]
        val kG = Y_FROM_LINRGB[1]
        val kB = Y_FROM_LINRGB[2]
        val coordA = if (n % 4 <= 1) 0.0 else 100.0
        val coordB = if (n % 2 == 0) 0.0 else 100.0
        return when {
            n < 4 -> {
                val g = coordA
                val b = coordB
                val r = (y - g * kG - b * kB) / kR
                if (isBounded(r)) {
                    doubleArrayOf(r, g, b)
                } else {
                    null
                }
            }
            n < 8 -> {
                val b = coordA
                val r = coordB
                val g = (y - r * kR - b * kB) / kG
                if (isBounded(g)) {
                    doubleArrayOf(r, g, b)
                } else {
                    null
                }
            }
            else -> {
                val r = coordA
                val g = coordB
                val b = (y - r * kR - g * kG) / kB
                if (isBounded(b)) {
                    doubleArrayOf(r, g, b)
                } else {
                    null
                }
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
        var left: DoubleArray? = null
        var right: DoubleArray? = null
        var leftHue = 0.0
        var rightHue = 0.0
        var initialized = false
        var uncut = true
        for (n in 0..11) {
            val mid = nthVertex(y, n)
            if (mid != null) {
                val midHue = hueOf(mid)
                if (!initialized) {
                    left = mid
                    right = mid
                    leftHue = midHue
                    rightHue = midHue
                    initialized = true
                } else if (uncut || areInCyclicOrder(leftHue, midHue, rightHue)) {
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
        }
        return arrayOf(left!!, right!!)
    }

    private fun midpoint(a: DoubleArray, b: DoubleArray): DoubleArray {
        return doubleArrayOf((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2)
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
    private fun bisectToLimit(y: Double, targetHue: Double): DoubleArray {
        val segment = bisectToSegment(y, targetHue)
        var left = segment[0]
        var leftHue = hueOf(left)
        var right = segment[1]
        for (axis in 0..2) {
            if (left[axis] != right[axis]) {
                var lPlane =
                    if (left[axis] < right[axis]) {
                        criticalPlaneBelow(trueDelinearized(left[axis]))
                    } else {
                        criticalPlaneAbove(trueDelinearized(left[axis]))
                    }
                var rPlane =
                    if (left[axis] < right[axis]) {
                        criticalPlaneAbove(trueDelinearized(right[axis]))
                    } else {
                        criticalPlaneBelow(trueDelinearized(right[axis]))
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
        return midpoint(left, right)
    }

    private fun inverseChromaticAdaptation(adapted: Double): Double {
        val adaptedAbs = abs(adapted)
        val base = Math.max(0.0, 27.13 * adaptedAbs / (400.0 - adaptedAbs))
        return sign(adapted) * base.pow(1.0 / 0.42)
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
        val viewingConditions = ViewingConditions.DEFAULT
        val tInnerCoeff = 1 / (1.64 - 0.29.pow(viewingConditions.n)).pow(0.73)
        val eHue = 0.25 * (cos(hueRadians + 2.0) + 3.8)
        val p1 = eHue * (50000.0 / 13.0) * viewingConditions.nc * viewingConditions.ncb
        val hSin = sin(hueRadians)
        val hCos = cos(hueRadians)
        for (iterationRound in 0..4) {
            // ===========================================================
            // Operations inlined from Cam16 to avoid repeated calculation
            // ===========================================================
            val jNormalized = j / 100.0
            val alpha = if (chroma == 0.0 || j == 0.0) 0.0 else chroma / sqrt(jNormalized)
            val t = (alpha * tInnerCoeff).pow(1.0 / 0.9)
            val ac =
                viewingConditions.aw *
                    jNormalized.pow(1.0 / viewingConditions.c / viewingConditions.z)
            val p2 = ac / viewingConditions.nbb
            val gamma = 23.0 * (p2 + 0.305) * t / (23.0 * p1 + 11 * t * hCos + 108.0 * t * hSin)
            val a = gamma * hCos
            val b = gamma * hSin
            val rA = (460.0 * p2 + 451.0 * a + 288.0 * b) / 1403.0
            val gA = (460.0 * p2 - 891.0 * a - 261.0 * b) / 1403.0
            val bA = (460.0 * p2 - 220.0 * a - 6300.0 * b) / 1403.0
            val rCScaled = inverseChromaticAdaptation(rA)
            val gCScaled = inverseChromaticAdaptation(gA)
            val bCScaled = inverseChromaticAdaptation(bA)
            val linrgb =
                MathUtils.matrixMultiply(
                    doubleArrayOf(rCScaled, gCScaled, bCScaled),
                    LINRGB_FROM_SCALED_DISCOUNT,
                )
            // ===========================================================
            // Operations inlined from Cam16 to avoid repeated calculation
            // ===========================================================
            if (linrgb[0] < 0 || linrgb[1] < 0 || linrgb[2] < 0) {
                return 0
            }
            val kR = Y_FROM_LINRGB[0]
            val kG = Y_FROM_LINRGB[1]
            val kB = Y_FROM_LINRGB[2]
            val fnj = kR * linrgb[0] + kG * linrgb[1] + kB * linrgb[2]
            if (fnj <= 0) {
                return 0
            }
            if (iterationRound == 4 || abs(fnj - y) < 0.002) {
                return if (linrgb[0] > 100.01 || linrgb[1] > 100.01 || linrgb[2] > 100.01) {
                    0
                } else {
                    ColorUtils.argbFromLinrgb(linrgb)
                }
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
        if (chroma < 0.0001 || lstar < 0.0001 || lstar > 99.9999) {
            return ColorUtils.argbFromLstar(lstar)
        }
        val hueRadians = MathUtils.sanitizeDegreesDouble(hueDegrees) / 180 * PI
        val y = ColorUtils.yFromLstar(lstar)
        val exactAnswer = findResultByJ(hueRadians, chroma, y)
        if (exactAnswer != 0) {
            return exactAnswer
        }
        val linrgb = bisectToLimit(y, hueRadians)
        return ColorUtils.argbFromLinrgb(linrgb)
    }
}

/**
 * Color science utilities.
 *
 * Utility methods for color science constants and color space conversions that aren't HCT or CAM16.
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/utils/ColorUtils.kt
 *
 * Changes:
 * - Only the conversions needed by this file and TemperatureCache are kept.
 * - Helpers only used inside this object (including labF and labInvf) are private.
 */
internal object ColorUtils {
    private val SRGB_TO_XYZ =
        arrayOf(
            doubleArrayOf(0.41233895, 0.35762064, 0.18051042),
            doubleArrayOf(0.2126, 0.7152, 0.0722),
            doubleArrayOf(0.01932141, 0.11916382, 0.95034478),
        )

    private val WHITE_POINT_D65 = doubleArrayOf(95.047, 100.0, 108.883)

    /** Converts a color from RGB components to ARGB format. */
    private fun argbFromRgb(red: Int, green: Int, blue: Int): Int {
        return 255 shl 24 or (red and 255 shl 16) or (green and 255 shl 8) or (blue and 255)
    }

    /** Converts a color from linear RGB components to ARGB format. */
    fun argbFromLinrgb(linrgb: DoubleArray): Int {
        val r = delinearized(linrgb[0])
        val g = delinearized(linrgb[1])
        val b = delinearized(linrgb[2])
        return argbFromRgb(r, g, b)
    }

    /** Returns the red component of a color in ARGB format. */
    private fun redFromArgb(argb: Int): Int {
        return argb shr 16 and 255
    }

    /** Returns the green component of a color in ARGB format. */
    private fun greenFromArgb(argb: Int): Int {
        return argb shr 8 and 255
    }

    /** Returns the blue component of a color in ARGB format. */
    private fun blueFromArgb(argb: Int): Int {
        return argb and 255
    }

    /** Converts a color from ARGB to XYZ. */
    private fun xyzFromArgb(argb: Int): DoubleArray {
        val r = linearized(redFromArgb(argb))
        val g = linearized(greenFromArgb(argb))
        val b = linearized(blueFromArgb(argb))
        return MathUtils.matrixMultiply(doubleArrayOf(r, g, b), SRGB_TO_XYZ)
    }

    /**
     * Converts a color from ARGB representation to L*a*b* representation.
     *
     * @param argb the ARGB representation of a color
     * @return a Lab object representing the color
     */
    fun labFromArgb(argb: Int): DoubleArray {
        val linearR = linearized(redFromArgb(argb))
        val linearG = linearized(greenFromArgb(argb))
        val linearB = linearized(blueFromArgb(argb))
        val matrix = SRGB_TO_XYZ
        val x = matrix[0][0] * linearR + matrix[0][1] * linearG + matrix[0][2] * linearB
        val y = matrix[1][0] * linearR + matrix[1][1] * linearG + matrix[1][2] * linearB
        val z = matrix[2][0] * linearR + matrix[2][1] * linearG + matrix[2][2] * linearB
        val whitePoint = WHITE_POINT_D65
        val xNormalized = x / whitePoint[0]
        val yNormalized = y / whitePoint[1]
        val zNormalized = z / whitePoint[2]
        val fx = labF(xNormalized)
        val fy = labF(yNormalized)
        val fz = labF(zNormalized)
        val l = 116.0 * fy - 16
        val a = 500.0 * (fx - fy)
        val b = 200.0 * (fy - fz)
        return doubleArrayOf(l, a, b)
    }

    /**
     * Converts an L* value to an ARGB representation.
     *
     * @param lstar L* in L*a*b*
     * @return ARGB representation of grayscale color with lightness matching L*
     */
    fun argbFromLstar(lstar: Double): Int {
        val y = yFromLstar(lstar)
        val component = delinearized(y)
        return argbFromRgb(component, component, component)
    }

    /**
     * Computes the L* value of a color in ARGB representation.
     *
     * @param argb ARGB representation of a color
     * @return L*, from L*a*b*, coordinate of the color
     */
    fun lstarFromArgb(argb: Int): Double {
        val y = xyzFromArgb(argb)[1]
        return 116.0 * labF(y / 100.0) - 16.0
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
        return 100.0 * labInvf((lstar + 16.0) / 116.0)
    }

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
    private fun delinearized(rgbComponent: Double): Int {
        val normalized = rgbComponent / 100.0
        val delinearized: Double =
            if (normalized <= 0.0031308) {
                normalized * 12.92
            } else {
                1.055 * normalized.pow(1.0 / 2.4) - 0.055
            }
        return (delinearized * 255.0).roundToInt().coerceIn(0, 255)
    }

    /**
     * Returns the standard white point; white on a sunny day.
     *
     * @return The white point
     */
    fun whitePointD65(): DoubleArray {
        return WHITE_POINT_D65
    }

    private fun labF(t: Double): Double {
        val e = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        return if (t > e) {
            t.pow(1.0 / 3.0)
        } else {
            (kappa * t + 16) / 116
        }
    }

    private fun labInvf(ft: Double): Double {
        val e = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        val ft3 = ft * ft * ft
        return if (ft3 > e) {
            ft3
        } else {
            (116 * ft - 16) / kappa
        }
    }
}

/**
 * Utility methods for mathematical operations.
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/utils/MathUtils.kt
 *
 * Changes:
 * - Only lerp, sanitizeDegreesDouble and matrixMultiply are kept.
 * - sanitizeDegreesDouble has @Suppress("NAME_SHADOWING") (upstream shadows the degrees parameter).
 */
internal object MathUtils {
    /**
     * The linear interpolation function.
     *
     * @return start if amount = 0 and stop if amount = 1
     */
    fun lerp(start: Double, stop: Double, amount: Double): Double {
        return (1.0 - amount) * start + amount * stop
    }

    /**
     * Sanitizes a degree measure as a floating-point number.
     *
     * @return a degree measure between 0.0 (inclusive) and 360.0 (exclusive).
     */
    @Suppress("NAME_SHADOWING") // Kept identical to upstream.
    fun sanitizeDegreesDouble(degrees: Double): Double {
        var degrees = degrees % 360.0
        if (degrees < 0) {
            degrees += 360.0
        }
        return degrees
    }

    /** Multiplies a 1x3 row vector with a 3x3 matrix. */
    fun matrixMultiply(row: DoubleArray, matrix: Array<DoubleArray>): DoubleArray {
        val a = row[0] * matrix[0][0] + row[1] * matrix[0][1] + row[2] * matrix[0][2]
        val b = row[0] * matrix[1][0] + row[1] * matrix[1][1] + row[2] * matrix[1][2]
        val c = row[0] * matrix[2][0] + row[1] * matrix[2][1] + row[2] * matrix[2][2]
        return doubleArrayOf(a, b, c)
    }
}
