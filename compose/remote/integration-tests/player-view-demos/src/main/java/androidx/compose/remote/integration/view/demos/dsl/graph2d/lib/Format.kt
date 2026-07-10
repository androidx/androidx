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

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Host-side number → label formatting for axis ticks and value labels. Tick values for static data
 * are known at author time, so we format them as plain [String]s and bake them with `remoteText`;
 * this avoids the reactive `createTextFromFloat` path for the common case and gives full control
 * over SI suffixes, decimals and signs.
 *
 * A [NumberFormat] is a tiny strategy object; pass one to an axis via `axis { format = ... }`.
 */
public fun interface NumberFormat {
    public fun format(value: Float): String

    public companion object {
        /** Fixed number of decimals. */
        public fun decimals(n: Int): NumberFormat = NumberFormat { v -> fixed(v, n) }

        /** Auto: picks decimals from [step] so adjacent ticks read distinctly. */
        public fun auto(step: Float): NumberFormat {
            val d = decimalsFor(step)
            return NumberFormat { v -> fixed(v, d) }
        }

        /** SI-suffixed (1.2k, 3.4M, 5.6B) — great for large value axes. */
        public val SI: NumberFormat = NumberFormat { v -> si(v) }

        /** Percentage: multiplies by 100 and appends `%`. */
        public fun percent(decimals: Int = 0): NumberFormat = NumberFormat { v ->
            fixed(v * 100f, decimals) + "%"
        }

        /** Currency-style prefix (e.g. `$`) with SI scaling. */
        public fun currency(symbol: String = "$"): NumberFormat = NumberFormat { v ->
            symbol + si(v)
        }
    }
}

/** Decimals needed to distinguish ticks spaced [step] apart. */
public fun decimalsFor(step: Float): Int {
    val s = abs(step)
    if (s == 0f || s >= 1f) return 0
    var d = 0
    var t = s
    while (t < 1f && d < 6) {
        t *= 10f
        d++
    }
    return d
}

/** Fixed-decimal formatting without relying on java.util locale formatting. */
public fun fixed(value: Float, decimals: Int): String {
    if (decimals <= 0) return value.roundToLong().toString()
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val scaled = (value.toDouble() * scale).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val abs = abs(scaled)
    val whole = abs / scale
    val frac = (abs % scale).toString().padStart(decimals, '0')
    return "$sign$whole.$frac"
}

/** SI-suffixed compact form: 1234 → "1.2k", 2_500_000 → "2.5M". */
public fun si(value: Float): String {
    val a = abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        a >= 1_000_000_000f -> sign + trimDot(fixed(a / 1_000_000_000f, 1)) + "B"
        a >= 1_000_000f -> sign + trimDot(fixed(a / 1_000_000f, 1)) + "M"
        a >= 1_000f -> sign + trimDot(fixed(a / 1_000f, 1)) + "k"
        a >= 1f || a == 0f -> sign + value.roundToLong().toString().removePrefix("-")
        else -> sign + trimDot(fixed(a, 2))
    }.let { if (it == "-0") "0" else it }
}

private fun trimDot(s: String): String = if (s.endsWith(".0")) s.dropLast(2) else s
