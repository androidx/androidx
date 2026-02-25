/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer.list

import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * A two-way converter between User scroll (Su) and Content scroll (Sc). When the adaptive auto
 * focus logic is applied, those two values are not equal and their proportion depends on the state
 * of the list.
 */
internal sealed interface GlimmerListAutoFocusScrollConverter {

    /**
     * Converts User scroll to Content scroll (Su -> Sc).
     *
     * @param userScroll user scroll (Su).
     * @param scrollThreshold scroll threshold (d).
     * @param viewportSize list viewport size (h).
     * @param contentLength total content length (L).
     */
    fun convertUserScrollToContentScroll(
        userScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double

    /**
     * Converts Content scroll to User scroll (Sc -> Su).
     *
     * @param contentScroll content scroll (Sc).
     * @param scrollThreshold scroll threshold (d).
     * @param viewportSize list viewport size (h).
     * @param contentLength total content length (L).
     */
    fun convertContentScrollToUserScroll(
        contentScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double
}

/**
 * A wrapper that delegates calculation to the most appropriate converter based on the list
 * parameters.
 *
 * @see AutoFocusScrollConverter.getConverter for more details on the logic behind choosing a
 *   converter.
 */
internal object AutoFocusScrollConverter : GlimmerListAutoFocusScrollConverter {

    override fun convertUserScrollToContentScroll(
        userScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        val converter = getConverter(scrollThreshold, viewportSize, contentLength)
        return converter.convertUserScrollToContentScroll(
            userScroll = userScroll,
            scrollThreshold = scrollThreshold,
            viewportSize = viewportSize,
            contentLength = contentLength,
        )
    }

    override fun convertContentScrollToUserScroll(
        contentScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        val converter = getConverter(scrollThreshold, viewportSize, contentLength)
        return converter.convertContentScrollToUserScroll(
            contentScroll = contentScroll,
            scrollThreshold = scrollThreshold,
            viewportSize = viewportSize,
            contentLength = contentLength,
        )
    }

    private fun getConverter(
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): GlimmerListAutoFocusScrollConverter {
        return when {
            // The list is not scrollable, so there's no need for focus line or conversion.
            contentLength <= viewportSize -> NoConversion
            // The list is scrollable, but it's not long enough to use complex logic. The focus line
            // always moves together with content scroll without pauses in the middle.
            contentLength <= 2 * scrollThreshold + viewportSize -> LinearScrollConverter
            // The list is very long, so it splits in three parts: focus line gradually moves to the
            // center, focus line is fixed in the center and focus line gradually moves to the end.
            else -> AdaptiveScrollConverter
        }
    }
}

/**
 * No conversion: content scroll is always equal to user scroll as in common lazy lists (Su==Sc).
 *
 * We use this behavior when a list is small enough to fully fit into viewport size (L < h). So we
 * don't need to apply the AutoFocus behavior to such a list, instead, the system moves focus by
 * swiping.
 */
private object NoConversion : GlimmerListAutoFocusScrollConverter {
    override fun convertUserScrollToContentScroll(
        userScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        return userScroll
    }

    override fun convertContentScrollToUserScroll(
        contentScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        return contentScroll
    }
}

/**
 * When a list is too short to fill the viewport and two scroll thresholds (`L < h + 2d`), the
 * dependency of content scroll (and focus line scroll) on user scroll is linear and doesn't change
 * through all the content.
 */
private object LinearScrollConverter : GlimmerListAutoFocusScrollConverter {
    override fun convertUserScrollToContentScroll(
        userScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        return (contentLength - viewportSize) * userScroll / contentLength
    }

    override fun convertContentScrollToUserScroll(
        contentScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        return contentScroll * contentLength / (contentLength - viewportSize)
    }
}

/**
 * Main converter that does the math to ensure that focus line moves smoothly through a long list.
 */
private object AdaptiveScrollConverter : GlimmerListAutoFocusScrollConverter {
    override fun convertUserScrollToContentScroll(
        userScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        val x = userScroll
        val d = scrollThreshold
        val h = viewportSize
        val L = contentLength
        val t = d + h / 2
        // The function below defines piecewise-smooth function with three segments:
        // - first: upward-opening parabola (y1 = ax^2 + bx)
        // - middle: straight line (y2 = kx + b)
        // - last: downward-opening parabola (y3 = ax^2 + bx + c).
        //
        // This function controls content movement, with its derivative determining scroll
        // sensitivity. Slopes (derivatives) match at junctions (first-order smoothness),
        // ensuring smooth transitions that feel natural and unnoticeable to the user.
        //
        // All those scary-looking formulas are mostly just bulky constants, while the curves
        // themselves stay simple—just parabolas on the sides and a line in the middle.
        val y =
            when (x) {
                0.0 -> return 0.0
                // Focus line is between the start and the center.
                // Upward-opening parabola (y1 = ax^2 + bx).
                in 0.0..t -> h * x * x / 2 / t / t + (t - h) * x / t
                // Focus line is in the center. So, only content (y) moves.
                // Straight line (y2 = kx + b).
                in t..L - t -> x - h / 2
                // Focus line is between the center and the end.
                // Downward-opening parabola (y3 = -ax^2 + bx + c).
                in L - t..L -> {
                    // Parabola is translated to the origin to improve numerical accuracy.
                    val x_t = x - L
                    -h * x_t * x_t / 2 / t / t + (t - h) * x_t / t + L - h
                }
                // Fallback to the linear if we are out of expected range.
                else ->
                    LinearScrollConverter.convertUserScrollToContentScroll(
                        userScroll = x,
                        scrollThreshold = d,
                        viewportSize = h,
                        contentLength = L,
                    )
            }
        return y
    }

    override fun convertContentScrollToUserScroll(
        contentScroll: Double,
        scrollThreshold: Double,
        viewportSize: Double,
        contentLength: Double,
    ): Double {
        val y = contentScroll
        val d = scrollThreshold
        val h = viewportSize
        val L = contentLength
        val t = d + h / 2
        // Please refer to the description of the sibling method [convertUserScrollToContentScroll].
        // The difference is that there it performs the transformation x->y, while here it is y->x.
        //
        // We have guarantees that both parabolas have only a single root within the given range.
        val x =
            when (y) {
                // Fast return.
                0.0 -> return 0.0
                // Focus line is between the start and the center.
                // Upward-opening parabola (y1 = ax^2 + bx).
                in 0.0..d -> {
                    val a = h / 2 / t / t
                    val b = (t - h) / t
                    val c = -y
                    solveQuadratic(a, b, c, 0.0, t)
                }
                // Focus line is in the center.
                // Straight line (y2 = kx + b).
                in d..L - h - d -> y + h / 2
                // Focus line is between the center and the end.
                // Downward-opening parabola (y3 = -ax^2 + bx + c).
                in L - h - d..L - h -> {
                    // Parabola is translated to the origin to improve numerical accuracy.
                    val a = -h / 2 / t / t
                    val b = (t - h) / t
                    val c = L - h - y
                    solveQuadratic(a, b, c, -t, 0.0) + L
                }
                else -> -1.0
            }
        if (x == -1.0) {
            // Fallback to the linear if we are out of expected range.
            return LinearScrollConverter.convertContentScrollToUserScroll(
                contentScroll = y,
                scrollThreshold = d,
                viewportSize = h,
                contentLength = L,
            )
        }
        return x
    }

    /** Returns a single value within the given range or -1 if there's no solution. */
    private fun solveQuadratic(
        a: Double,
        b: Double,
        c: Double,
        fromRange: Double,
        toRange: Double,
    ): Double {
        val discriminant = b * b - 4 * a * c
        val doubleTolerance = 1e-4
        val range = (fromRange - doubleTolerance)..(toRange + doubleTolerance)
        return when {
            a == 0.0 -> -1.0
            discriminant < -doubleTolerance -> -1.0 // D < 0
            discriminant.absoluteValue <= doubleTolerance -> -b / (2 * a) // D == 0
            else -> {
                // Use a numerically stable quadratic solver to avoid catastrophic cancellation.
                val sqrtD = sqrt(discriminant)
                val q = (b + b.sign * sqrtD) / -2
                val x1 = q / a
                if (x1 in range) return x1
                val x2 = c / q
                if (x2 in range) return x2
                return -1.0
            }
        }
    }
}

internal fun GlimmerListAutoFocusScrollConverter.convertUserScrollToContentScroll(
    userScroll: Double,
    properties: GlimmerListAutoFocusProperties,
): Double {
    return convertUserScrollToContentScroll(
        userScroll = userScroll,
        scrollThreshold = properties.scrollThreshold,
        viewportSize = properties.viewportSize.toDouble(),
        contentLength = properties.contentLength,
    )
}

internal fun GlimmerListAutoFocusScrollConverter.convertContentScrollToUserScroll(
    contentScroll: Double,
    properties: GlimmerListAutoFocusProperties,
): Double {
    return convertContentScrollToUserScroll(
        contentScroll = contentScroll,
        scrollThreshold = properties.scrollThreshold,
        viewportSize = properties.viewportSize.toDouble(),
        contentLength = properties.contentLength,
    )
}

/**
 * The scroll distance required to put the focus indicator in the center is calculated as a
 * proportion of the list's viewport size, using [ProportionalThresholdFactor].
 *
 * For example, if the visible list height is 500dp and [ProportionalThresholdFactor] is 0.6f, the
 * focus will reach the center after scrolling 300dp of the content — that is, sixth of the viewport
 * height.
 *
 * Note that this behavior only applies to lists with enough content to scroll. If the list is too
 * short to scroll, the focus line moves using different rules.
 */
internal const val ProportionalThresholdFactor = 0.6f
