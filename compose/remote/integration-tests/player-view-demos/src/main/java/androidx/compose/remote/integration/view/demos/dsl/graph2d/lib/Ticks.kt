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

package androidx.compose.remote.integration.view.demos.dsl.graph2d.lib

import android.annotation.SuppressLint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * A computed "nice" axis: an expanded domain whose bounds fall on round numbers, plus the evenly
 * spaced tick values and the decimals needed to label them. This is the classic 1-2-5 nice-number
 * algorithm, run host-side (the value domain for static data is known at author time, so there is
 * zero token cost and we get pixel-perfect, professional axis ticks).
 */
public class NiceAxis(
    public val min: Float,
    public val max: Float,
    public val step: Float,
    public val ticks: FloatArray,
    public val decimals: Int,
) {
    public val span: Float
        get() = max - min
}

/**
 * Compute a nice axis covering `[dataMin, dataMax]` aiming for ~[target] ticks.
 * - [includeZero] (default true) pulls the baseline to 0 — the correct default for bar charts so
 *   bar lengths are honestly proportional.
 * - If the data is flat (min == max) a sensible unit range is synthesized so the chart still draws.
 */
public fun niceAxis(
    dataMin: Float,
    dataMax: Float,
    target: Int = 5,
    includeZero: Boolean = true,
): NiceAxis {
    var lo = minOf(dataMin, dataMax)
    var hi = maxOf(dataMin, dataMax)
    if (includeZero) {
        lo = minOf(lo, 0f)
        hi = maxOf(hi, 0f)
    }
    if (lo == hi) {
        // Flat data: open up a unit window around the value.
        val pad = if (lo == 0f) 1f else abs(lo) * 0.5f
        lo -= pad
        hi += pad
    }
    val step = niceNum((hi - lo) / (target.coerceAtLeast(2) - 1), round = true)
    val niceMin = floor(lo / step) * step
    val niceMax = ceil(hi / step) * step
    val count = ((niceMax - niceMin) / step).roundToIntSafe() + 1
    val ticks = FloatArray(count) { i -> snap(niceMin + i * step, step) }
    return NiceAxis(niceMin, niceMax, step, ticks, decimalsFor(step))
}

/** Round a tick to step precision to kill float drift like 0.30000004. */
private fun snap(value: Float, step: Float): Float {
    val d = decimalsFor(step)
    if (d <= 0) return value.toDouble().let { kotlin.math.round(it).toFloat() }
    var scale = 1.0
    repeat(d) { scale *= 10 }
    return (kotlin.math.round(value.toDouble() * scale) / scale).toFloat()
}

private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

/** An axis from caller-supplied tick values (sorted ascending); domain spans first..last. */
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
public fun explicitAxis(tickValues: List<Float>): NiceAxis {
    require(tickValues.size >= 2) {
        "graph2d: explicit axis ticks need >= 2 values (was ${tickValues.size})"
    }
    val sorted = tickValues.sorted()
    var minGap = Float.MAX_VALUE
    for (i in 1 until sorted.size) {
        val g = sorted[i] - sorted[i - 1]
        if (g > 0f && g < minGap) minGap = g
    }
    if (minGap == Float.MAX_VALUE) minGap = 1f
    return NiceAxis(
        sorted.first(),
        sorted.last(),
        minGap,
        sorted.toFloatArray(),
        decimalsFor(minGap),
    )
}

/**
 * A log10 axis over positive data: the domain snaps to whole decades and ticks sit on powers of 10
 * (with 2× and 5× mantissa ticks added when the data spans ≲2 decades). Tick values are in DATA
 * space — the log mapping lives in the scale ([LinearScale] with `log = true`).
 */
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
fun logAxis(dataMin: Float, dataMax: Float): NiceAxis {
    require(dataMin > 0f && dataMax > 0f) {
        "graph2d: log axis requires positive data (was $dataMin..$dataMax)"
    }
    val lo = minOf(dataMin, dataMax)
    val hi = maxOf(dataMin, dataMax)
    val eLo = floor(log10(lo.toDouble())).toInt()
    val eHi = ceil(log10(hi.toDouble())).toInt().coerceAtLeast(eLo + 1)
    val decades = eHi - eLo
    val mantissas = if (decades <= 2) floatArrayOf(1f, 2f, 5f) else floatArrayOf(1f)
    val ticks = ArrayList<Float>()
    for (e in eLo..eHi) {
        for (m in mantissas) {
            val v = (m * 10.0.pow(e)).toFloat()
            if (e == eHi && m > 1f) continue
            ticks.add(v)
        }
    }
    val min = 10.0.pow(eLo).toFloat()
    val max = 10.0.pow(eHi).toFloat()
    return NiceAxis(min, max, ticks[1] - ticks[0], ticks.toFloatArray(), 0)
}

// ---------------------------------------------------------------------------
// Time axis — x values are epoch days (days since 1970-01-01); ticks land on
// calendar boundaries. Pure integer arithmetic (no java.time), so it runs on
// any host. Algorithms after Howard Hinnant's date library.
// ---------------------------------------------------------------------------

/** (year, month 1..12, day 1..31) for an epoch-day count. */
internal fun civilFromDays(epochDay: Int): IntArray {
    var z = epochDay + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    return intArrayOf(if (m <= 2) y + 1 else y, m, d)
}

/** Epoch-day count for a (year, month 1..12, day 1..31) civil date. */
public fun daysFromCivil(year: Int, month: Int, day: Int): Int {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private val MONTH_NAMES =
    arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/**
 * A calendar-aware time axis over epoch-day x values, plus formatted tick labels. Tick steps come
 * from {1, 2, 7, 14 days; 1, 2, 3, 6 months; 1, 2, 5, … years} and land on day/month/year
 * boundaries; labels adapt ("Mar 5" / "Mar" / "Mar 2026" / "2026") to the chosen step.
 */
@SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
fun timeAxis(dayMin: Float, dayMax: Float, target: Int = 6): Pair<NiceAxis, List<String>> {
    val lo = floor(minOf(dayMin, dayMax)).toInt()
    val hi = ceil(maxOf(dayMin, dayMax)).toInt().coerceAtLeast(lo + 1)
    val spanDays = hi - lo
    val t = target.coerceAtLeast(2)
    val ticks = ArrayList<Int>()
    val labels = ArrayList<String>()
    val yLo = civilFromDays(lo)[0]
    val yHi = civilFromDays(hi)[0]
    val multiYear = yHi != yLo

    val dayStep = intArrayOf(1, 2, 7, 14).firstOrNull { spanDays / it <= t }
    val monthStep = intArrayOf(1, 2, 3, 6).firstOrNull { spanDays / (it * 30) <= t }
    if (dayStep != null) {
        var d = lo - ((lo % dayStep) + dayStep) % dayStep
        while (d <= hi) {
            if (d >= lo) {
                val c = civilFromDays(d)
                ticks.add(d)
                labels.add("${MONTH_NAMES[c[1] - 1]} ${c[2]}")
            }
            d += dayStep
        }
    } else if (monthStep != null) {
        val cLo = civilFromDays(lo)
        var y = cLo[0]
        var m = ((cLo[1] - 1) / monthStep) * monthStep + 1
        while (true) {
            val d = daysFromCivil(y, m, 1)
            if (d > hi) break
            if (d >= lo) {
                ticks.add(d)
                labels.add(
                    if (multiYear && m == 1) "${MONTH_NAMES[m - 1]} $y" else MONTH_NAMES[m - 1]
                )
            }
            m += monthStep
            if (m > 12) {
                m -= 12
                y++
            }
        }
    } else {
        val yearStep = niceNum(spanDays / 365.25f / (t - 1), round = true).toInt().coerceAtLeast(1)
        var y = (yLo / yearStep) * yearStep
        while (true) {
            val d = daysFromCivil(y, 1, 1)
            if (d > hi) break
            if (d >= lo) {
                ticks.add(d)
                labels.add(y.toString())
            }
            y += yearStep
        }
    }
    if (ticks.size < 2) {
        ticks.clear()
        labels.clear()
        for (d in intArrayOf(lo, hi)) {
            val c = civilFromDays(d)
            ticks.add(d)
            labels.add("${MONTH_NAMES[c[1] - 1]} ${c[2]}")
        }
    }
    val arr = FloatArray(ticks.size) { ticks[it].toFloat() }
    val step = if (arr.size > 1) arr[1] - arr[0] else 1f
    return NiceAxis(lo.toFloat(), hi.toFloat(), step, arr, 0) to labels
}

/**
 * The 1-2-5 nice number: the "roundest" value near [x]. When [round] is true it rounds to the
 * nearest of {1,2,5}×10ⁿ; otherwise it takes the ceiling — used to pick a tick step.
 */
public fun niceNum(x: Float, round: Boolean): Float {
    if (x <= 0f) return 1f
    val exp = floor(log10(x.toDouble())).toInt()
    val frac = x / 10.0.pow(exp).toFloat()
    val niceFrac =
        if (round) {
            when {
                frac < 1.5f -> 1f
                frac < 3f -> 2f
                frac < 7f -> 5f
                else -> 10f
            }
        } else {
            when {
                frac <= 1f -> 1f
                frac <= 2f -> 2f
                frac <= 5f -> 5f
                else -> 10f
            }
        }
    return niceFrac * 10.0.pow(exp).toFloat()
}
