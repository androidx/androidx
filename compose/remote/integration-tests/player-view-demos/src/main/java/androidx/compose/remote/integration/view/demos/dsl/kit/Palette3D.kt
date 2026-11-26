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

package androidx.compose.remote.integration.view.demos.dsl.kit

import androidx.compose.remote.creation.dsl.RcCanvasScope
import androidx.compose.remote.creation.dsl.RcColor
import androidx.compose.remote.creation.dsl.RcFloat

/**
 * Color helpers for the demos: a **reactive** HSV color whose hue is an [RcFloat] (so it animates
 * on the player via the variable-aware paint path), author-time color blending, and a few
 * ready-made palettes.
 */

/**
 * A reactive HSV [RcColor]: [hue] is an [RcFloat] in `[0,1)` (one trip round the color wheel),
 * re-resolved every frame on the player. Use with `color(RcColor)` in `scene3D`. Cycle the hue with
 * e.g. `color(hueColor((continuousSeconds() * 0.1f) % 1f))`.
 */
@Suppress("RestrictedApiAndroidX")
public fun RcCanvasScope.hueColor(
    hue: RcFloat,
    sat: Float = 0.7f,
    value: Float = 0.95f,
    alpha: Int = 255,
): RcColor = remoteColorExpression(alpha = alpha, hue = hue, saturation = sat, value = value)

/** Blend two ARGB ints by [t] in `[0,1]` (author time). */
public fun lerpColor(a: Int, b: Int, t: Float): Int = Textures3D.lerpArgb(a, b, t)

/** Pick a color from a gradient defined by [stops] (≥2 ARGB ints) at position [t] in `[0,1]`. */
public fun gradient(stops: IntArray, t: Float): Int {
    if (stops.size == 1) return stops[0]
    val x = (t.coerceIn(0f, 1f)) * (stops.size - 1)
    val i = x.toInt().coerceAtMost(stops.size - 2)
    return lerpColor(stops[i], stops[i + 1], x - i)
}

/** Curated palettes (ARGB) for the demos. */
public object Palettes {
    public val SUNSET: IntArray =
        intArrayOf(
            0xFF2A1A4A.toInt(),
            0xFF7A2A6A.toInt(),
            0xFFE0533F.toInt(),
            0xFFF5A05A.toInt(),
            0xFFFFE08A.toInt(),
        )
    public val OCEAN: IntArray =
        intArrayOf(
            0xFF02131F.toInt(),
            0xFF0A3A5A.toInt(),
            0xFF1E7FA0.toInt(),
            0xFF5FC9C0.toInt(),
            0xFFCFF5E8.toInt(),
        )
    public val NEON: IntArray =
        intArrayOf(
            0xFF18E0C8.toInt(),
            0xFF3A7BFF.toInt(),
            0xFFB14BFF.toInt(),
            0xFFFF4FA3.toInt(),
            0xFFFFE14F.toInt(),
        )
    public val EMBER: IntArray =
        intArrayOf(
            0xFF1A0A06.toInt(),
            0xFF6E1E0A.toInt(),
            0xFFD2451A.toInt(),
            0xFFF59A2E.toInt(),
            0xFFFFE3A0.toInt(),
        )
}
