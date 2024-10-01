/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.window.layout

import android.graphics.Rect
import android.util.DisplayMetrics
import androidx.annotation.RestrictTo
import androidx.window.core.Bounds

/**
 * Metrics about a [android.view.Window], consisting of its bounds.
 *
 * This is obtained from [WindowMetricsCalculator.computeCurrentWindowMetrics] or
 * [WindowMetricsCalculator.computeMaximumWindowMetrics].
 *
 * @see WindowMetricsCalculator
 */
class WindowMetrics
internal constructor(
    private val _bounds: Bounds,
    /**
     * Returns the logical density of the display this window is in.
     *
     * @see [DisplayMetrics.density]
     */
    val density: Float
) {

    /** An internal constructor for [WindowMetrics] */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(bounds: Rect, density: Float) : this(Bounds(bounds), density)

    /**
     * Returns a new [Rect] describing the bounds of the area the window occupies.
     *
     * **Note that the size of the reported bounds can have different size than [Display#getSize].**
     * This method reports the window size including all system decorations, while [Display#getSize]
     * reports the area excluding navigation bars and display cutout areas.
     *
     * @return window bounds in pixels.
     */
    val bounds: Rect
        get() = _bounds.toRect()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WindowMetrics

        if (_bounds != other._bounds) return false
        if (density != other.density) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _bounds.hashCode()
        result = 31 * result + density.hashCode()
        return result
    }

    override fun toString(): String {
        return "WindowMetrics(_bounds=$_bounds, density=$density)"
    }
}
