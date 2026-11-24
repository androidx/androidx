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

package androidx.compose.remote.document

import androidx.annotation.RestrictTo
import kotlin.math.pow

/**
 * Pure-Kotlin ARGB/HSV color arithmetic matching `Utils` in `remote-core` bit-for-bit, with zero
 * dependency on `remote-core` or Android graphics APIs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteColorMath {
    /** Extracts the 8-bit alpha channel (`0..255`) from an ARGB int. */
    @JvmStatic public fun alpha(argb: Int): Int = (argb ushr 24) and 0xFF

    /** Extracts the 8-bit red channel (`0..255`) from an ARGB int. */
    @JvmStatic public fun red(argb: Int): Int = (argb ushr 16) and 0xFF

    /** Extracts the 8-bit green channel (`0..255`) from an ARGB int. */
    @JvmStatic public fun green(argb: Int): Int = (argb ushr 8) and 0xFF

    /** Extracts the 8-bit blue channel (`0..255`) from an ARGB int. */
    @JvmStatic public fun blue(argb: Int): Int = argb and 0xFF

    /** Packs 8-bit [alpha], [red], [green], and [blue] channels into an ARGB int. */
    @JvmStatic
    public fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        ((alpha and 0xFF) shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)

    /** Clamps an integer channel value to `0..255` matching `Utils.clamp`. */
    @JvmStatic public fun clampChannel(value: Int): Int = value.coerceIn(0, 255)

    /**
     * Converts normalized `[0, 1]` [alpha], [red], [green], and [blue] channels to an ARGB int
     * matching `Utils.toARGB`. Channels are not clamped, so out-of-range inputs overflow into
     * adjacent byte lanes identically to `Utils.toARGB`.
     */
    @JvmStatic
    public fun toArgb(alpha: Float, red: Float, green: Float, blue: Float): Int {
        val a = (alpha * 255.0f + 0.5f).toInt()
        val r = (red * 255.0f + 0.5f).toInt()
        val g = (green * 255.0f + 0.5f).toInt()
        val b = (blue * 255.0f + 0.5f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Converts `[0, 1)` [hue], `[0, 1]` [saturation], and `[0, 1]` [value] to an opaque ARGB int
     * matching `Utils.hsvToRgb`. Returns `0` when `(hue * 6f).toInt()` falls outside `0..5` (for
     * example `hue >= 1f` or `hue <= -1f / 6f`).
     */
    @JvmStatic
    public fun hsvToRgb(hue: Float, saturation: Float, value: Float): Int {
        val h = (hue * 6f).toInt()
        val f = hue * 6f - h
        val p = (0.5f + 255f * value * (1f - saturation)).toInt()
        val q = (0.5f + 255f * value * (1f - f * saturation)).toInt()
        val t = (0.5f + 255f * value * (1f - (1f - f) * saturation)).toInt()
        val v = (0.5f + 255f * value).toInt()
        return when (h) {
            0 -> 0xFF000000.toInt() or ((v shl 16) + (t shl 8) + p)
            1 -> 0xFF000000.toInt() or ((q shl 16) + (v shl 8) + p)
            2 -> 0xFF000000.toInt() or ((p shl 16) + (v shl 8) + t)
            3 -> 0xFF000000.toInt() or ((p shl 16) + (q shl 8) + v)
            4 -> 0xFF000000.toInt() or ((t shl 16) + (p shl 8) + v)
            5 -> 0xFF000000.toInt() or ((v shl 16) + (p shl 8) + q)
            else -> 0
        }
    }

    /**
     * Gamma-2.2 interpolates between [c1] and [c2] at fraction [t] matching
     * `Utils.interpolateColor`.
     */
    @JvmStatic
    public fun interpolateColor(c1: Int, c2: Int, t: Float): Int {
        if (t.isNaN() || t == 0.0f) {
            return c1
        } else if (t == 1.0f) {
            return c2
        }
        val a1 = 0xFF and (c1 shr 24)
        val r1 = 0xFF and (c1 shr 16)
        val g1 = 0xFF and (c1 shr 8)
        val b1 = 0xFF and c1
        val c1fr = (r1 / 255.0f).toDouble().pow(2.2).toFloat()
        val c1fg = (g1 / 255.0f).toDouble().pow(2.2).toFloat()
        val c1fb = (b1 / 255.0f).toDouble().pow(2.2).toFloat()
        val c1fa = a1 / 255f

        val a2 = 0xFF and (c2 shr 24)
        val r2 = 0xFF and (c2 shr 16)
        val g2 = 0xFF and (c2 shr 8)
        val b2 = 0xFF and c2
        val c2fr = (r2 / 255.0f).toDouble().pow(2.2).toFloat()
        val c2fg = (g2 / 255.0f).toDouble().pow(2.2).toFloat()
        val c2fb = (b2 / 255.0f).toDouble().pow(2.2).toFloat()
        val c2fa = a2 / 255f

        val fr = c1fr + t * (c2fr - c1fr)
        val fg = c1fg + t * (c2fg - c1fg)
        val fb = c1fb + t * (c2fb - c1fb)
        val fa = c1fa + t * (c2fa - c1fa)

        val outR = clampChannel((fr.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f).toInt())
        val outG = clampChannel((fg.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f).toInt())
        val outB = clampChannel((fb.toDouble().pow(1.0 / 2.2).toFloat() * 255.0f).toInt())
        val outA = clampChannel((fa * 255.0f).toInt())
        return (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
    }

    /** Returns the normalized hue (`0..1`) of [argb] matching `Utils.getHue`. */
    @JvmStatic
    public fun hue(argb: Int): Float {
        val rf = red(argb) / 255f
        val gf = green(argb) / 255f
        val bf = blue(argb) / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        val delta = max - min
        var h =
            if (max == min) {
                0f
            } else if (max == rf) {
                ((gf - bf) / delta) % 6f
            } else if (max == gf) {
                ((bf - rf) / delta) + 2f
            } else {
                ((rf - gf) / delta) + 4f
            }
        h = (h * 60f) % 360f
        if (h < 0f) {
            h += 360f
        }
        return h / 360f
    }

    /** Returns the normalized saturation (`0..1`) of [argb] matching `Utils.getSaturation`. */
    @JvmStatic
    public fun saturation(argb: Int): Float {
        val rf = red(argb) / 255f
        val gf = green(argb) / 255f
        val bf = blue(argb) / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        return if (max == min) 0f else (max - min) / max
    }

    /** Returns the normalized brightness (`0..1`) of [argb] matching `Utils.getBrightness`. */
    @JvmStatic
    public fun brightness(argb: Int): Float {
        val rf = red(argb) / 255f
        val gf = green(argb) / 255f
        val bf = blue(argb) / 255f
        return maxOf(rf, gf, bf)
    }
}
