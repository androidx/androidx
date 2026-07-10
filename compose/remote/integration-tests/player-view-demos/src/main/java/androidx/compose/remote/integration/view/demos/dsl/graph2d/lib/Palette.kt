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

/**
 * Author-time ARGB color helpers shared by the charting marks. Everything here is plain Kotlin (no
 * DSL), so colors can be precomputed host-side and baked per series / per bar.
 */
public object Palette {
    /** Categorical default (mirrors [GraphTheme.DefaultPalette]). */
    public val Categorical: IntArray = GraphTheme.DefaultPalette

    /** A perceptually pleasant sequential ramp (light → saturated blue). */
    public val Blues: IntArray =
        intArrayOf(
            0xFFEFF6FB.toInt(),
            0xFFB8D6EA.toInt(),
            0xFF6BAED6.toInt(),
            0xFF2E7EBC.toInt(),
            0xFF08458A.toInt(),
        )

    /** Diverging red ↔ grey ↔ blue, good for above/below-baseline charts. */
    public val Diverging: IntArray =
        intArrayOf(
            0xFFD6604D.toInt(),
            0xFFF4A582.toInt(),
            0xFFF7F7F7.toInt(),
            0xFF92C5DE.toInt(),
            0xFF4393C3.toInt(),
        )

    /** Replace the alpha byte of [argb] with [alpha] in `0..255`. */
    public fun withAlpha(argb: Int, alpha: Int): Int =
        (argb and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    /** Multiply the alpha of [argb] by [factor] in `0..1`. */
    public fun fadeAlpha(argb: Int, factor: Float): Int {
        val a = ((argb ushr 24) and 0xFF)
        return withAlpha(argb, (a * factor.coerceIn(0f, 1f)).toInt())
    }

    /** Linearly blend two ARGB ints by [t] in `0..1` (straight-alpha, per channel). */
    public fun lerp(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        fun ch(shift: Int): Int {
            val ca = (a ushr shift) and 0xFF
            val cb = (b ushr shift) and 0xFF
            return (ca + (cb - ca) * u).toInt().coerceIn(0, 255)
        }
        return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** Sample a multi-stop [ramp] (≥1 ARGB ints) at position [t] in `0..1`. */
    public fun sample(ramp: IntArray, t: Float): Int {
        if (ramp.size == 1) return ramp[0]
        val x = t.coerceIn(0f, 1f) * (ramp.size - 1)
        val i = x.toInt().coerceAtMost(ramp.size - 2)
        return lerp(ramp[i], ramp[i + 1], x - i)
    }

    /** A slightly darker shade of [argb] (for bar borders / depth). */
    public fun darken(argb: Int, factor: Float = 0.8f): Int =
        lerp(argb, 0xFF000000.toInt(), 1f - factor)

    /** A lighter tint of [argb] (for gradients / highlights). */
    public fun lighten(argb: Int, factor: Float = 0.3f): Int =
        lerp(argb, 0xFFFFFFFF.toInt(), factor)
}
