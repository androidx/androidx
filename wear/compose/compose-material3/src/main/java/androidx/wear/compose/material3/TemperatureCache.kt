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

package androidx.wear.compose.material3

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Design utilities using color temperature theory.
 *
 * Complementary color, and cache to efficiently, lazily, generate data for calculations when
 * needed.
 *
 * NOTICE: Fork and trim of Material Color Utilities (libmonet) TemperatureCache, using its Kotlin
 * port (kotlin/ on GitHub, com.google.ux.material.libmonetkt in google3) as the source. Apart from
 * androidx ktfmt formatting and the changes listed below, the code is identical to the linked
 * upstream file. Where upstream code triggers an androidx check, it is kept as is and the check is
 * suppressed (@Suppress) rather than rewritten.
 *
 * Forked from:
 * https://github.com/material-foundation/material-color-utilities/blob/5b3618b16fdc3825e21d5679bafd144662088ea1/kotlin/temperature/TemperatureCache.kt
 *
 * Changes:
 * - getAnalogousColors removed (unused); getRelativeTemperature, rawTemperature and input are
 *   private.
 * - @Suppress("ListIterator", "PrimitiveInCollection"): see the comment on the class.
 *
 * Computing [complement] creates 361 [Hct] colors, so callers should compute it once per input
 * color rather than on every recomposition.
 *
 * @param input Color to find complement of. Any colors will have the same tone, and chroma as the
 *   input color, modulo any restrictions due to the other hues having lower limits on chroma.
 */
// Kept identical to upstream instead of rewriting for these Compose lint checks:
// - PrimitiveInCollection: temperatures must stay Double to be bit-exact with libmonet;
//   androidx.collection has no ObjectDoubleMap, and ObjectFloatMap would lose precision.
// - ListIterator: one loop over 362 colors, run once per input color.
@Suppress("ListIterator", "PrimitiveInCollection")
internal class TemperatureCache(private val input: Hct) {
    private var _precomputedComplement: Hct? = null
    private var _precomputedHctsByTemp: List<Hct>? = null
    private var _precomputedHctsByHue: List<Hct>? = null
    private var _precomputedTempsByHct: Map<Hct, Double>? = null

    /**
     * A color that complements the input color aesthetically.
     *
     * In art, this is usually described as being across the color wheel. History of this shows
     * intent as a color that is just as cool-warm as the input color is warm-cool.
     */
    val complement: Hct
        get() {
            if (_precomputedComplement != null) {
                return _precomputedComplement!!
            }
            val coldestHue = coldest.hue
            val coldestTemp = tempsByHct[coldest]!!
            val warmestHue = warmest.hue
            val warmestTemp = tempsByHct[warmest]!!
            val range = warmestTemp - coldestTemp
            val startHueIsColdestToWarmest = isBetween(input.hue, coldestHue, warmestHue)
            val startHue = if (startHueIsColdestToWarmest) warmestHue else coldestHue
            val endHue = if (startHueIsColdestToWarmest) coldestHue else warmestHue
            val directionOfRotation = 1.0
            var smallestError = 1000.0
            var answer = hctsByHue[input.hue.roundToInt()]
            val complementRelativeTemp = 1.0 - getRelativeTemperature(input)
            // Find the color in the other section, closest to the inverse percentile
            // of the input color. This is the complement.
            var hueAddend = 0.0
            while (hueAddend <= 360.0) {
                val hue =
                    MathUtils.sanitizeDegreesDouble(startHue + directionOfRotation * hueAddend)
                if (!isBetween(hue, startHue, endHue)) {
                    hueAddend += 1.0
                    continue
                }
                val possibleAnswer = hctsByHue[hue.roundToInt()]
                val relativeTemp = (tempsByHct[possibleAnswer]!! - coldestTemp) / range
                val error = abs(complementRelativeTemp - relativeTemp)
                if (error < smallestError) {
                    smallestError = error
                    answer = possibleAnswer
                }
                hueAddend += 1.0
            }
            _precomputedComplement = answer
            return _precomputedComplement!!
        }

    /**
     * Temperature relative to all colors with the same chroma and tone.
     *
     * @param hct HCT to find the relative temperature of.
     * @return Value on a scale from 0 to 1.
     */
    private fun getRelativeTemperature(hct: Hct): Double {
        val range = tempsByHct[warmest]!! - tempsByHct[coldest]!!
        val differenceFromColdest = tempsByHct[hct]!! - tempsByHct[coldest]!!
        // Handle when there's no difference in temperature between warmest and
        // coldest: for example, at T100, only one color is available, white.
        return if (range == 0.0) {
            0.5
        } else {
            differenceFromColdest / range
        }
    }

    /** Coldest color with same chroma and tone as input. */
    private val coldest: Hct
        get() = hctsByTemp[0]

    /**
     * HCTs for all colors with the same chroma/tone as the input.
     *
     * Sorted by hue, ex. index 0 is hue 0.
     */
    private val hctsByHue: List<Hct>
        get() {
            if (_precomputedHctsByHue != null) {
                return _precomputedHctsByHue!!
            }
            val hcts: MutableList<Hct> = ArrayList()
            var hue = 0.0
            while (hue <= 360.0) {
                val colorAtHue = Hct.from(hue, input.chroma, input.tone)
                hcts.add(colorAtHue)
                hue += 1.0
            }
            val unmodifiableList = Collections.unmodifiableList(hcts)
            _precomputedHctsByHue = unmodifiableList
            return unmodifiableList
        }

    /**
     * HCTs for all colors with the same chroma/tone as the input.
     *
     * Sorted from coldest first to warmest last.
     */
    private val hctsByTemp: List<Hct>
        get() {
            if (_precomputedHctsByTemp != null) {
                return _precomputedHctsByTemp!!
            }
            val hcts: MutableList<Hct> = ArrayList(hctsByHue)
            hcts.add(input)
            hcts.sortWith(compareBy { tempsByHct[it]!! })
            _precomputedHctsByTemp = hcts
            return hcts
        }

    /** Keys of HCTs in getHctsByTemp, values of raw temperature. */
    private val tempsByHct: Map<Hct, Double>
        get() {
            if (_precomputedTempsByHct != null) {
                return _precomputedTempsByHct!!
            }
            val allHcts: MutableList<Hct> = ArrayList(hctsByHue)
            allHcts.add(input)
            val temperaturesByHct: MutableMap<Hct, Double> = HashMap()
            for (hct in allHcts) {
                temperaturesByHct[hct] = rawTemperature(hct)
            }
            _precomputedTempsByHct = temperaturesByHct
            return temperaturesByHct
        }

    /** Warmest color with same chroma and tone as input. */
    private val warmest: Hct
        get() = hctsByTemp[hctsByTemp.size - 1]

    companion object {
        /**
         * Value representing cool-warm factor of a color. Values below 0 are considered cool,
         * above, warm.
         *
         * Color science has researched emotion and harmony, which art uses to select colors.
         * Warm-cool is the foundation of analogous and complementary colors. See:
         * - Li-Chen Ou's Chapter 19 in Handbook of Color Psychology (2015).
         * - Josef Albers' Interaction of Color chapters 19 and 21.
         *
         * Implementation of Ou, Woodcock and Wright's algorithm, which uses Lab/LCH color space.
         * Return value has these properties:
         * - Values below 0 are cool, above 0 are warm.
         * - Lower bound: -9.66. Chroma is infinite. Assuming max of Lab chroma 130.
         * - Upper bound: 8.61. Chroma is infinite. Assuming max of Lab chroma 130.
         */
        private fun rawTemperature(color: Hct): Double {
            val lab = ColorUtils.labFromArgb(color.toInt())
            val hue = MathUtils.sanitizeDegreesDouble(Math.toDegrees(atan2(lab[2], lab[1])))
            val chroma = hypot(lab[1], lab[2])
            return -0.5 +
                0.02 *
                    chroma.pow(1.07) *
                    cos(Math.toRadians(MathUtils.sanitizeDegreesDouble(hue - 50.0)))
        }

        /** Determines if an angle is between two other angles, rotating clockwise. */
        private fun isBetween(angle: Double, a: Double, b: Double): Boolean {
            return if (a < b) {
                a <= angle && angle <= b
            } else {
                a <= angle || angle <= b
            }
        }
    }
}
