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

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
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
