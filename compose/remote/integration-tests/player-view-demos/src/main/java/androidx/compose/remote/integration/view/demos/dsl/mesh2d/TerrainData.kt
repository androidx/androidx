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

package androidx.compose.remote.integration.view.demos.dsl.mesh2d

import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Authoring-time terrain: Perlin noise, its gradient, and a shaded-relief palette.
 *
 * None of this runs on the player. It is ordinary Kotlin that executes once, when the document is
 * written, and its whole output is a few flat arrays baked into the mesh. That is the point of the
 * literal mesh form: noise is not expressible as an RPN expression over `(u, v)` - there is no
 * hash, no permutation table and no integer arithmetic in the expression language - so the
 * parametric form cannot produce it at any price. Bake it instead.
 *
 * The cost is honest and worth stating: a parametric mesh is a few hundred bytes whatever its
 * resolution, and this one is a vertex list. See [TerrainData.approximateWireBytes].
 */
internal class PerlinNoise(seed: Int) {

    /** The classic doubled permutation table, so `perm[a + b]` never needs a wrap. */
    private val perm = IntArray(512)

    init {
        val p = IntArray(256) { it }
        // A plain LCG rather than kotlin.random, so the terrain is identical on every platform
        // that builds this document. A demo that looks different per machine is a bad demo.
        var state = seed
        for (i in 255 downTo 1) {
            state = state * 1664525 + 1013904223
            val j = ((state ushr 16) and 0x7fff) % (i + 1)
            val swap = p[i]
            p[i] = p[j]
            p[j] = swap
        }
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
        }
    }

    /**
     * Perlin's 6t^5 - 15t^4 + 10t^3 ease, which has zero first and second derivatives at 0 and 1.
     */
    private fun fade(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Dot product with one of eight unit-ish gradient directions chosen by the hash. */
    private fun grad(hash: Int, x: Float, y: Float): Float =
        when (hash and 7) {
            0 -> x
            1 -> x + y
            2 -> y
            3 -> -x + y
            4 -> -x
            5 -> -x - y
            6 -> -y
            else -> x - y
        }

    /** Gradient noise at (x, y), roughly in -1..1. */
    fun noise(x: Float, y: Float): Float {
        val fx = floor(x)
        val fy = floor(y)
        val xi = fx.toInt() and 255
        val yi = fy.toInt() and 255
        val xf = x - fx
        val yf = y - fy
        val u = fade(xf)
        val v = fade(yf)
        val aa = perm[perm[xi] + yi]
        val ab = perm[perm[xi] + yi + 1]
        val ba = perm[perm[xi + 1] + yi]
        val bb = perm[perm[xi + 1] + yi + 1]
        val lower = lerp(grad(aa, xf, yf), grad(ba, xf - 1f, yf), u)
        val upper = lerp(grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f), u)
        return lerp(lower, upper, v)
    }

    /** Fractal sum of [octaves] noise layers, each half the amplitude and twice the frequency. */
    fun fbm(x: Float, y: Float, octaves: Int): Float {
        var sum = 0f
        var amplitude = 1f
        var frequency = 1f
        var norm = 0f
        repeat(octaves) {
            sum += noise(x * frequency, y * frequency) * amplitude
            norm += amplitude
            frequency *= 2f
            amplitude *= 0.5f
        }
        return sum / norm
    }
}

/**
 * A square heightfield and everything derived from it.
 *
 * Positions are in the unit square, not pixels. The document scales the unit square up to the
 * canvas at draw time, which keeps the mesh resolution-independent and - because a half float is at
 * its most precise in 0..1 - lets the positions and uv go on the wire as f16 with no visible cost.
 */
internal class TerrainData(val grid: Int, seed: Int) {

    /** Height in 0..1, row major, `grid * grid` of them. */
    val height = FloatArray(grid * grid)

    /** d(height)/dx in height-units per unit-of-domain. */
    val slopeX = FloatArray(grid * grid)

    /** d(height)/dy in height-units per unit-of-domain. */
    val slopeY = FloatArray(grid * grid)

    /** Unit-square vertex positions, x, y interleaved. */
    val verts = FloatArray(grid * grid * 2)

    /** Slope encoded as a uv pair; see the class doc for the convention. */
    val uv = FloatArray(grid * grid * 2)

    /** Packed ARGB per vertex. */
    val colors = IntArray(grid * grid)

    /** Triangle list, two triangles per cell. */
    val indices = IntArray((grid - 1) * (grid - 1) * 6)

    /**
     * The steepest slope anywhere on this terrain, used to normalise the uv channel so that a
     * consumer reading it back knows u = 1 means "as steep as this terrain gets".
     */
    var maxSlope = 0f
        private set

    init {
        val noise = PerlinNoise(seed)

        // --- heights, normalised to 0..1 so the palette does not depend on the noise's range
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (iy in 0 until grid) {
            for (ix in 0 until grid) {
                // Frequency has to be chosen against the grid, not by eye. The finest octave here
                // is NOISE_TURNS * 2^(OCTAVES-1) = 6.4 cycles across 40 cells, about six cells per
                // cycle, which the grid resolves comfortably. Push either number up and the top
                // octave falls below Nyquist - and because the hillshade differentiates the
                // field, and differentiation multiplies each octave by its frequency, the
                // unresolved octave is precisely the one that dominates the shading. The result
                // looks like static rather than terrain.
                val nx = ix / (grid - 1f) * NOISE_TURNS
                val ny = iy / (grid - 1f) * NOISE_TURNS
                val h = noise.fbm(nx, ny, OCTAVES)
                height[iy * grid + ix] = h
                if (h < lo) lo = h
                if (h > hi) hi = h
            }
        }
        val span = if (hi - lo < 1e-6f) 1f else hi - lo
        for (i in height.indices) {
            height[i] = (height[i] - lo) / span
        }

        // --- gradient by central difference, one-sided at the edges
        val cell = 1f / (grid - 1f)
        for (iy in 0 until grid) {
            for (ix in 0 until grid) {
                val i = iy * grid + ix
                val xPrev = height[iy * grid + maxOf(ix - 1, 0)]
                val xNext = height[iy * grid + minOf(ix + 1, grid - 1)]
                val yPrev = height[maxOf(iy - 1, 0) * grid + ix]
                val yNext = height[minOf(iy + 1, grid - 1) * grid + ix]
                // The divisor is the actual distance spanned, which is one cell at the edges and
                // two in the interior - getting this wrong doubles the apparent slope in a
                // one-pixel border and shows up as a bright rim.
                val dx = if (ix == 0 || ix == grid - 1) cell else 2f * cell
                val dy = if (iy == 0 || iy == grid - 1) cell else 2f * cell
                slopeX[i] = (xNext - xPrev) / dx
                slopeY[i] = (yNext - yPrev) / dy
            }
        }

        // --- one 3x3 box pass over the gradient
        //
        // Central differencing is a high-pass filter: whatever cell-scale wobble survives in the
        // height field comes out of it amplified, and it lands in two visible places at once -
        // the hillshade and the uv channel. Smoothing the derivative rather than the height keeps
        // the terrain's shape intact and only takes the speckle off the shading. It also makes
        // the field the balls roll on continuous enough that they do not jitter between cells.
        smooth(slopeX)
        smooth(slopeY)

        for (i in slopeX.indices) {
            val mag = sqrt(slopeX[i] * slopeX[i] + slopeY[i] * slopeY[i])
            if (mag > maxSlope) maxSlope = mag
        }
        if (maxSlope < 1e-6f) maxSlope = 1f

        // --- positions, uv, colours
        for (iy in 0 until grid) {
            for (ix in 0 until grid) {
                val i = iy * grid + ix
                verts[i * 2] = ix / (grid - 1f)
                verts[i * 2 + 1] = iy / (grid - 1f)

                val gx = slopeX[i]
                val gy = slopeY[i]
                val mag = sqrt(gx * gx + gy * gy)
                // u: steepness, 0 = flat, 1 = the steepest this terrain gets.
                uv[i * 2] = (mag / maxSlope).coerceIn(0f, 1f)
                // v: aspect, the compass direction of steepest descent, mapped 0..1.
                uv[i * 2 + 1] = (atan2(gy, gx) / TWO_PI + 0.5f).coerceIn(0f, 1f)

                colors[i] = shade(height[i], gx, gy)
            }
        }

        // --- grid topology, two triangles per cell, consistent winding
        var k = 0
        for (iy in 0 until grid - 1) {
            for (ix in 0 until grid - 1) {
                val topLeft = iy * grid + ix
                val topRight = topLeft + 1
                val bottomLeft = topLeft + grid
                val bottomRight = bottomLeft + 1
                indices[k++] = topLeft
                indices[k++] = bottomLeft
                indices[k++] = topRight
                indices[k++] = topRight
                indices[k++] = bottomLeft
                indices[k++] = bottomRight
            }
        }
    }

    /** A 3x3 box blur over [field], in place, clamping at the edges. */
    private fun smooth(field: FloatArray) {
        val copy = field.clone()
        for (iy in 0 until grid) {
            val y0 = (iy - 1).coerceAtLeast(0)
            val y1 = iy
            val y2 = (iy + 1).coerceAtMost(grid - 1)
            for (ix in 0 until grid) {
                val x0 = (ix - 1).coerceAtLeast(0)
                val x1 = ix
                val x2 = (ix + 1).coerceAtMost(grid - 1)
                field[iy * grid + ix] =
                    (copy[y0 * grid + x0] +
                        copy[y0 * grid + x1] +
                        copy[y0 * grid + x2] +
                        copy[y1 * grid + x0] +
                        copy[y1 * grid + x1] +
                        copy[y1 * grid + x2] +
                        copy[y2 * grid + x0] +
                        copy[y2 * grid + x1] +
                        copy[y2 * grid + x2]) / 9f
            }
        }
    }

    /**
     * The brightness term is a Lambertian hillshade: the surface normal is `normalize(-dz/dx,
     * -dz/dy, 1)` and the light comes from the upper left, which is the convention every relief map
     * uses because the eye reads top-lit surfaces as convex.
     */
    private fun shade(h: Float, gx: Float, gy: Float): Int {
        val hue = ramp(h, HEIGHT_STOPS, HUE_STOPS)
        val sat = ramp(h, HEIGHT_STOPS, SAT_STOPS)

        val nz = 1f / sqrt(gx * gx + gy * gy + 1f)
        val nx = -gx * nz
        val ny = -gy * nz
        var lambert = nx * LIGHT_X + ny * LIGHT_Y + nz * LIGHT_Z
        if (lambert < 0f) lambert = 0f
        // Ambient keeps the unlit faces readable rather than crushing them to black.
        val value = (0.32f + 0.68f * lambert).coerceIn(0f, 1f)

        return hsvToArgb(hue, sat, value)
    }

    /** Piecewise-linear lookup of [outputs] against ascending [stops]. */
    private fun ramp(t: Float, stops: FloatArray, outputs: FloatArray): Float {
        if (t <= stops[0]) return outputs[0]
        for (i in 1 until stops.size) {
            if (t <= stops[i]) {
                val span = stops[i] - stops[i - 1]
                val f = if (span < 1e-6f) 0f else (t - stops[i - 1]) / span
                return outputs[i - 1] + (outputs[i] - outputs[i - 1]) * f
            }
        }
        return outputs[outputs.size - 1]
    }

    /** An estimate of what this mesh costs on the wire, for the doc comment to be honest about. */
    fun approximateWireBytes(halfFloat: Boolean): Int {
        val positionBytes = verts.size * if (halfFloat) 2 else 4
        val uvBytes = uv.size * if (halfFloat) 2 else 4
        return positionBytes + uvBytes + colors.size * 4 + indices.size * 2
    }

    internal companion object {
        private const val TWO_PI = 6.2831855f
        private const val NOISE_TURNS = 3.2f
        private const val OCTAVES = 4

        private fun smooth(t: Float): Float = t * t * t * (t * (t * 6f - 15f) + 10f)

        /** Light from the upper left and somewhat above, normalised. */
        private const val LIGHT_X = -0.5121475f
        private const val LIGHT_Y = -0.6827300f
        private const val LIGHT_Z = 0.5206073f

        /** Height breakpoints for the palette. */
        private val HEIGHT_STOPS = floatArrayOf(0f, 0.30f, 0.38f, 0.55f, 0.74f, 1f)

        /** Hue in turns: deep water, shallows, shore, grass, rock, snow. */
        private val HUE_STOPS = floatArrayOf(0.62f, 0.55f, 0.45f, 0.28f, 0.10f, 0.08f)

        /** Saturation: vivid water and grass, washed-out rock, near-white peaks. */
        private val SAT_STOPS = floatArrayOf(0.80f, 0.70f, 0.45f, 0.62f, 0.42f, 0.06f)

        /** HSV with hue in turns, to packed opaque ARGB. */
        fun hsvToArgb(hue: Float, sat: Float, value: Float): Int {
            val h = (hue - floor(hue)) * 6f
            val sector = h.toInt()
            val f = h - sector
            val p = value * (1f - sat)
            val q = value * (1f - sat * f)
            val t = value * (1f - sat * (1f - f))
            val r: Float
            val g: Float
            val b: Float
            when (sector) {
                0 -> {
                    r = value
                    g = t
                    b = p
                }
                1 -> {
                    r = q
                    g = value
                    b = p
                }
                2 -> {
                    r = p
                    g = value
                    b = t
                }
                3 -> {
                    r = p
                    g = q
                    b = value
                }
                4 -> {
                    r = t
                    g = p
                    b = value
                }
                else -> {
                    r = value
                    g = p
                    b = q
                }
            }
            val ri = (r.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val gi = (g.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            val bi = (b.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
            return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
        }
    }
}
