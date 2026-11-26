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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.sin

/**
 * Procedurally-generated [Bitmap] textures, built in Kotlin at author time, to feed
 * `texture(...)` + a UV-mapped mesh in the demos. No bundled assets needed (except
 * [resourceBitmap], which loads a packaged image). Each generator is a small, copy-able recipe.
 *
 * Demo suppliers run with **no Android `Context`**, so these use only `Bitmap`/`Canvas`/`Paint`
 * (which need none) — and [resourceBitmap] loads via the JVM classloader, not `Resources`.
 */
public object Textures3D {

    /** One bold digit 0–9 on a transparent background — e.g. for labelling tiles. */
    public fun digit(d: Int, w: Int = 64, h: Int = 80, color: Int = 0xFF141A26.toInt()): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = h * 0.92f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        val fm = p.fontMetrics
        c.drawText(d.toString(), w / 2f, h / 2f - (fm.ascent + fm.descent) / 2f, p)
        return bmp
    }

    /** Warped vertical wood grain, dark→light brown. */
    public fun wood(
        w: Int = 128,
        h: Int = 128,
        dark: Int = 0xFF5A3A1E.toInt(),
        light: Int = 0xFF9C6334.toInt(),
    ): Bitmap {
        val px = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val warp = sin(y * 0.045f) * 5f
                var t = sin((x + warp) * 0.17f) * 0.5f + 0.5f
                t += sin((x + warp) * 0.7f) * 0.12f
                val n = Noise3D.hash(x, y)
                t = (t + n * 0.07f).coerceIn(0f, 1f)
                px[y * w + x] = lerpArgb(dark, light, t)
            }
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** A two-color checkerboard of [n]×[n] squares. */
    public fun checker(
        size: Int = 128,
        n: Int = 8,
        a: Int = 0xFFE8E8EC.toInt(),
        b: Int = 0xFF2A2E3A.toInt(),
    ): Bitmap {
        val px = IntArray(size * size)
        val cell = size / n
        for (y in 0 until size) {
            for (x in 0 until size) {
                val odd = ((x / cell) + (y / cell)) and 1
                px[y * size + x] = if (odd == 0) a else b
            }
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    /** A vertical gradient from [top] (v=0) to [bottom] (v=1). Great as a sky backdrop. */
    public fun verticalGradient(top: Int, bottom: Int, w: Int = 8, h: Int = 256): Bitmap {
        val px = IntArray(w * h)
        for (y in 0 until h) {
            val c = lerpArgb(top, bottom, y / (h - 1f))
            for (x in 0 until w) px[y * w + x] = c
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /** A soft cloudy/marbled texture from fractal [Noise3D], blended between [a] and [b]. */
    public fun cloud(
        size: Int = 128,
        a: Int = 0xFF1B3A6B.toInt(),
        b: Int = 0xFFDDE8FF.toInt(),
        scale: Float = 0.04f,
    ): Bitmap {
        val px = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val t = Noise3D.fbm(x * scale, y * scale, 4)
                px[y * size + x] = lerpArgb(a, b, t)
            }
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    /**
     * Load a packaged image (e.g. `"textures/foo.png"` under `src/main/resources/`) with no
     * Context.
     */
    public fun resourceBitmap(path: String): Bitmap? =
        Textures3D::class.java.classLoader?.getResourceAsStream(path)?.use {
            BitmapFactory.decodeStream(it)
        }

    /** Blend two ARGB ints by [t] in `[0,1]` (per-channel). */
    public fun lerpArgb(a: Int, b: Int, t: Float): Int {
        val s = t.coerceIn(0f, 1f)
        val aa = (a ushr 24) and 0xFF
        val ar = (a ushr 16) and 0xFF
        val ag = (a ushr 8) and 0xFF
        val ab = a and 0xFF
        val ba = (b ushr 24) and 0xFF
        val br = (b ushr 16) and 0xFF
        val bg = (b ushr 8) and 0xFF
        val bb = b and 0xFF
        val oa = (aa + (ba - aa) * s).toInt()
        val or = (ar + (br - ar) * s).toInt()
        val og = (ag + (bg - ag) * s).toInt()
        val ob = (ab + (bb - ab) * s).toInt()
        return (oa shl 24) or (or shl 16) or (og shl 8) or ob
    }
}
