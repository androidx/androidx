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
package androidx.window

import android.graphics.Rect

/**
 * Metrics about a [android.view.Window], consisting of its bounds.
 *
 *
 * This is usually obtained from [WindowManager#getCurrentWindowMetrics] or
 * [WindowManager#getMaximumWindowMetrics].
 *
 * @see WindowManager#getCurrentWindowMetrics
 * @see WindowManager#getMaximumWindowMetrics
 */
public class WindowMetrics internal constructor(private val _bounds: Bounds) {

    public constructor(bounds: Rect) : this(Bounds(bounds))

    /**
     * Returns a new [Rect] describing the bounds of the area the window occupies.
     *
     *
     * **Note that the size of the reported bounds can have different size than
     * [Display#getSize].** This method reports the window size including all system
     * decorations, while [Display#getSize] reports the area excluding navigation bars
     * and display cutout areas.
     *
     * @return window bounds in pixels.
     */
    public val bounds: Rect
        get() = _bounds.toRect()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as WindowMetrics
        return _bounds == that._bounds
    }

    override fun hashCode(): Int {
        return _bounds.hashCode()
    }
}