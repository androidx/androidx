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

import androidx.compose.remote.creation.dsl.RcFloat
import androidx.compose.remote.creation.dsl.log
import kotlin.math.log10

/**
 * Maps a continuous data value to a pixel coordinate, **reactively**. The pixel endpoints are live
 * [RcFloat]s (derived from `componentWidth()/componentHeight()`), so the chart fills whatever space
 * its Canvas is given. The linear factor [scale] and [offset] are each flushed to a single hidden
 * variable, so per-value mapping is a tiny `v*scale + offset` expression well under the 32-token
 * cap.
 *
 * `pixel0` is the pixel of `domainMin`, `pixel1` the pixel of `domainMax`; for a vertical value
 * axis pass `pixel0 = plotBottom`, `pixel1 = plotTop` so larger values map upward.
 *
 * With [logScale] the mapping is linear in log10 space (domain must be positive): host values take
 * `log10` on the host, reactive values use the engine's `log` op.
 */
@Suppress("RestrictedApiAndroidX")
public class LinearScale(
    public val domainMin: Float,
    public val domainMax: Float,
    pixel0: RcFloat,
    pixel1: RcFloat,
    public val logScale: Boolean = false,
) {
    private fun t(v: Float): Float = if (logScale) log10(v.coerceAtLeast(1e-9f)) else v

    private val span: Float = (t(domainMax) - t(domainMin)).let { if (it == 0f) 1f else it }

    /** Pixels per (possibly log-transformed) data unit (reactive, flushed once). */
    public val scale: RcFloat = ((pixel1 - pixel0) * (1f / span)).flush()

    /**
     * Pixel at data 0 of the domain shift: `pixel = t(value)*scale + offset` (reactive, flushed).
     */
    public val offset: RcFloat = (pixel0 - scale * t(domainMin)).flush()

    /** Map a host-known value (e.g. a tick) to its reactive pixel. */
    public fun map(value: Float): RcFloat = scale * t(value) + offset

    /** Map a reactive value (e.g. a series datum) to its reactive pixel. */
    public fun mapR(value: RcFloat): RcFloat =
        if (logScale) log(value) * scale + offset else value * scale + offset

    public companion object {
        public fun of(
            axis: NiceAxis,
            pixel0: RcFloat,
            pixel1: RcFloat,
            logScale: Boolean = false,
        ): LinearScale = LinearScale(axis.min, axis.max, pixel0, pixel1, logScale)
    }
}

/**
 * Categorical scale over [count] bands within `[start, start+widthR]`, where [start] is a host
 * pixel (a fixed margin edge) and [widthR] is the reactive plot extent. d3 convention: [padInner]
 * (gap between bands) and [padOuter] (end gap) are fractions of step. Positions are reactive
 * [RcFloat]s; [step]/[bandWidth] are flushed once.
 */
@Suppress("RestrictedApiAndroidX")
public class BandScale(
    public val count: Int,
    public val start: Float,
    widthR: RcFloat,
    public val padInner: Float = 0.2f,
    public val padOuter: Float = 0.1f,
) {
    private val n: Int = count.coerceAtLeast(1)
    private val divisor: Float = (n - padInner + 2f * padOuter).coerceAtLeast(1e-3f)

    /** Distance between consecutive band starts. */
    public val step: RcFloat = (widthR * (1f / divisor)).flush()

    /** Width of a single band. */
    public val bandWidth: RcFloat = (step * (1f - padInner)).flush()

    /** Left (or top) edge of band [i]. */
    public fun bandStart(i: Int): RcFloat = step * (padOuter + i) + start

    /** Center of band [i]. */
    public fun center(i: Int): RcFloat = bandStart(i) + bandWidth * 0.5f
}

/**
 * Places [count] ordered points evenly across `[start, start+widthR]` with points on the endpoints
 * — the layout for line/area charts (a line spans the full plot extent). Reactive positions.
 */
@Suppress("RestrictedApiAndroidX")
public class PointScale(
    public val count: Int,
    public val start: Float,
    private val widthR: RcFloat,
) {
    private val n: Int = count.coerceAtLeast(1)

    /** Position of point [i]. */
    public fun position(i: Int): RcFloat =
        widthR * (if (n == 1) 0.5f else i.toFloat() / (n - 1)) + start

    /** Spacing between adjacent points (reactive). */
    public val step: RcFloat
        get() = widthR * (if (n == 1) 0f else 1f / (n - 1))
}
