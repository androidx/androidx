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
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Host-side statistical transforms for the distribution charts (binning, quantiles, KDE, ECDF). All
 * pure Kotlin over the raw sample — distribution data is known at author time, so the heavy lifting
 * is done here for free and the marks just map the resulting geometry reactively.
 */
public object Stats {

    /** Binned histogram: [edges] has `count+1` entries, [counts] has `count`. */
    public class Histogram(public val edges: FloatArray, public val counts: IntArray) {
        public val maxCount: Int
            get() = counts.maxOrNull() ?: 0
    }

    /** Five-number summary plus whisker bounds (1.5·IQR) and outliers. */
    public class BoxStats(
        public val min: Float,
        public val q1: Float,
        public val median: Float,
        public val q3: Float,
        public val max: Float,
        public val whiskerLo: Float,
        public val whiskerHi: Float,
        public val outliers: FloatArray,
    )

    /** A sampled curve (x, y). */
    public class Curve(public val xs: FloatArray, public val ys: FloatArray)

    /** Sorted ascending copy. */
    private fun sorted(data: FloatArray): FloatArray = data.copyOf().also { it.sort() }

    /** Linear-interpolated quantile [p] in `0..1` over an already-sorted array. */
    public fun quantileSorted(s: FloatArray, p: Float): Float {
        if (s.isEmpty()) return 0f
        if (s.size == 1) return s[0]
        val h = (s.size - 1) * p.coerceIn(0f, 1f)
        val lo = h.toInt()
        val frac = h - lo
        return if (lo + 1 < s.size) s[lo] + frac * (s[lo + 1] - s[lo]) else s[lo]
    }

    public fun mean(data: FloatArray): Float = if (data.isEmpty()) 0f else data.sum() / data.size

    public fun stdDev(data: FloatArray): Float {
        if (data.size < 2) return 0f
        val m = mean(data)
        var s = 0f
        for (v in data) s += (v - m) * (v - m)
        return sqrt(s / (data.size - 1))
    }

    /** Histogram with [bins] buckets (default: Sturges' rule). */
    public fun histogram(data: FloatArray, bins: Int = 0): Histogram {
        if (data.isEmpty()) return Histogram(floatArrayOf(0f, 1f), intArrayOf(0))
        var lo = data.min()
        var hi = data.max()
        if (lo == hi) {
            lo -= 0.5f
            hi += 0.5f
        }
        val k = if (bins > 0) bins else max(1, ceil(ln(data.size.toDouble()) / ln(2.0) + 1).toInt())
        val edges = FloatArray(k + 1) { lo + (hi - lo) * it / k }
        val counts = IntArray(k)
        val width = (hi - lo) / k
        for (v in data) {
            var idx = ((v - lo) / width).toInt()
            if (idx < 0) idx = 0
            if (idx >= k) idx = k - 1
            counts[idx]++
        }
        return Histogram(edges, counts)
    }

    /** Five-number summary + Tukey whiskers/outliers. */
    @SuppressLint("PrimitiveInCollection", "deprecation", "UnknownNullness")
    public fun box(data: FloatArray): BoxStats {
        val s = sorted(data)
        if (s.isEmpty()) return BoxStats(0f, 0f, 0f, 0f, 0f, 0f, 0f, FloatArray(0))
        val q1 = quantileSorted(s, 0.25f)
        val med = quantileSorted(s, 0.5f)
        val q3 = quantileSorted(s, 0.75f)
        val iqr = q3 - q1
        val fenceLo = q1 - 1.5f * iqr
        val fenceHi = q3 + 1.5f * iqr
        var wLo = s[0]
        var wHi = s[s.size - 1]
        val outliers = ArrayList<Float>()
        for (v in s) if (v < fenceLo || v > fenceHi) outliers.add(v)
        for (v in s) if (v >= fenceLo) {
            wLo = v
            break
        }
        for (i in s.indices.reversed()) if (s[i] <= fenceHi) {
            wHi = s[i]
            break
        }
        return BoxStats(s[0], q1, med, q3, s[s.size - 1], wLo, wHi, outliers.toFloatArray())
    }

    /** Gaussian KDE sampled on [gridN] points; [bandwidth] 0 ⇒ Silverman's rule of thumb. */
    public fun kde(
        data: FloatArray,
        gridN: Int = 64,
        bandwidth: Float = 0f,
        padBandwidths: Float = 3f,
    ): Curve {
        if (data.isEmpty()) return Curve(floatArrayOf(0f, 1f), floatArrayOf(0f, 0f))
        val n = data.size
        val h =
            if (bandwidth > 0f) bandwidth
            else
                (1.06f * max(stdDev(data), 1e-3f) * Math.pow(n.toDouble(), -0.2).toFloat())
                    .coerceAtLeast(1e-3f)
        val lo = data.min() - padBandwidths * h
        val hi = data.max() + padBandwidths * h
        val xs = FloatArray(gridN) { lo + (hi - lo) * it / (gridN - 1) }
        val ys = FloatArray(gridN)
        val norm = 1f / (n * h * sqrt(2.0 * Math.PI).toFloat())
        for (g in 0 until gridN) {
            var sum = 0f
            val x = xs[g]
            for (v in data) {
                val u = (x - v) / h
                sum += exp(-0.5f * u * u)
            }
            ys[g] = sum * norm
        }
        return Curve(xs, ys)
    }

    /** Empirical CDF: sorted unique-ish xs with cumulative proportion in `0..1`. */
    public fun ecdf(data: FloatArray): Curve {
        val s = sorted(data)
        val n = s.size
        if (n == 0) return Curve(floatArrayOf(0f, 1f), floatArrayOf(0f, 0f))
        val xs = FloatArray(n)
        val ys = FloatArray(n)
        for (i in 0 until n) {
            xs[i] = s[i]
            ys[i] = (i + 1f) / n
        }
        return Curve(xs, ys)
    }

    /** Inverse standard-normal CDF (probit), Acklam's rational approximation. [p] in `(0,1)`. */
    public fun invNormCdf(p: Float): Float {
        val pp = p.coerceIn(1e-6f, 1f - 1e-6f).toDouble()
        val a =
            doubleArrayOf(
                -3.969683028665376e+01,
                2.209460984245205e+02,
                -2.759285104469687e+02,
                1.383577518672690e+02,
                -3.066479806614716e+01,
                2.506628277459239e+00,
            )
        val b =
            doubleArrayOf(
                -5.447609879822406e+01,
                1.615858368580409e+02,
                -1.556989798598866e+02,
                6.680131188771972e+01,
                -1.328068155288572e+01,
            )
        val c =
            doubleArrayOf(
                -7.784894002430293e-03,
                -3.223964580411365e-01,
                -2.400758277161838e+00,
                -2.549732539343734e+00,
                4.374664141464968e+00,
                2.938163982698783e+00,
            )
        val d =
            doubleArrayOf(
                7.784695709041462e-03,
                3.224671290700398e-01,
                2.445134137142996e+00,
                3.754408661907416e+00,
            )
        val plow = 0.02425
        val phigh = 1 - plow
        val x: Double =
            if (pp < plow) {
                val q = sqrt(-2.0 * ln(pp))
                (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            } else if (pp <= phigh) {
                val q = pp - 0.5
                val r = q * q
                (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1)
            } else {
                val q = sqrt(-2.0 * ln(1 - pp))
                -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1)
            }
        return x.toFloat()
    }

    /** Deterministic pseudo-jitter in `[-amp, amp]` for point index [i] (stable across frames). */
    public fun jitter(i: Int, amp: Float): Float {
        var h = i * 374761393 + 668265263
        h = (h xor (h ushr 13)) * 1274126177
        val f = ((h ushr 8) and 0xFFFF) / 65535f // 0..1
        return (f * 2f - 1f) * amp
    }
}
