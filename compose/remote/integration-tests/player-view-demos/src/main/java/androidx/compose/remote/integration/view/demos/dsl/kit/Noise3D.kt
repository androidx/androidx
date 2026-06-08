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

import kotlin.math.floor

/**
 * Cheap, **deterministic** value noise for author-time procedural content (terrain heights,
 * scatter, texture detail). Deterministic = identical every build (no RNG seed), so demos are
 * reproducible.
 *
 * This runs in Kotlin at document-build time (not on the player) — use it to generate static
 * geometry or textures. For per-frame procedural motion, drive an
 * [androidx.compose.remote.creation.dsl.RcFloat] expression instead.
 */
public object Noise3D {

    /** Integer hash → `[0, 1)`. Stable across runs. */
    public fun hash(x: Int, y: Int): Float {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0x7fffffff) / 2147483647f
    }

    private fun smooth(t: Float) = t * t * (3f - 2f * t)

    /** Bilinear, smoothstep-interpolated value noise sampled at ([x], [y]); range `[0, 1]`. */
    public fun value(x: Float, y: Float): Float {
        val xi = floor(x.toDouble()).toInt()
        val yi = floor(y.toDouble()).toInt()
        val u = smooth(x - xi)
        val v = smooth(y - yi)
        val a = hash(xi, yi)
        val b = hash(xi + 1, yi)
        val c = hash(xi, yi + 1)
        val d = hash(xi + 1, yi + 1)
        val ab = a + (b - a) * u
        val cd = c + (d - c) * u
        return ab + (cd - ab) * v
    }

    /** Fractal (multi-octave) value noise, normalized to `[0, 1]`. */
    public fun fbm(x: Float, y: Float, octaves: Int = 3): Float {
        var sum = 0f
        var amp = 0.5f
        var freq = 1f
        var norm = 0f
        var ox = 11f
        var oy = -7f
        for (o in 0 until octaves) {
            sum += value(x * freq + ox, y * freq + oy) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2.17f
            ox = -ox * 1.3f + 5f
            oy = oy * 1.7f - 3f
        }
        return (sum / norm).coerceIn(0f, 1f)
    }
}
