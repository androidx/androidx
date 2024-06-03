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

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import androidx.annotation.UiContext

/**
 * Implementation of [WindowMetricsCalculator] for testing.
 *
 * @see WindowMetricsCalculator
 */
internal class TestWindowMetricsCalculator : WindowMetricsCalculator {
    private var overrideBounds: Rect? = null
    private var overrideMaxBounds: Rect? = null
    private val currentBounds = mutableMapOf<Context, Rect>()
    private val maxBounds = mutableMapOf<Context, Rect>()

    /**
     * Sets the bounds returned from this helper for the given context.
     *
     * Note: An override set via [setOverrideBounds] takes precedence over the values set with this
     * method.
     */
    fun setBounds(@UiContext context: Context, currentBounds: Rect, maxBounds: Rect) {
        this.currentBounds[context] = currentBounds
        this.maxBounds[context] = maxBounds
    }

    /** Clears the bounds that were set via [setBounds] for the given context. */
    fun clearBounds(@UiContext context: Context) {
        currentBounds.remove(context)
        maxBounds.remove(context)
    }

    /** Overrides the bounds returned from this helper for all supplied contexts. */
    fun setOverrideBounds(currentBounds: Rect, maxBounds: Rect) {
        overrideBounds = currentBounds
        overrideMaxBounds = maxBounds
    }

    /** Clears the overrides that were set in [setOverrideBounds]. */
    fun clearOverrideBounds() {
        overrideBounds = null
        overrideMaxBounds = null
    }

    override fun computeCurrentWindowMetrics(activity: Activity): WindowMetrics {
        return computeCurrentWindowMetrics(activity as Context)
    }

    override fun computeCurrentWindowMetrics(@UiContext context: Context): WindowMetrics {
        val bounds = overrideBounds ?: currentBounds[context] ?: Rect()
        return WindowMetrics(bounds, density = 1f)
    }

    override fun computeMaximumWindowMetrics(activity: Activity): WindowMetrics {
        return computeMaximumWindowMetrics(activity as Context)
    }

    override fun computeMaximumWindowMetrics(@UiContext context: Context): WindowMetrics {
        val bounds = overrideMaxBounds ?: maxBounds[context] ?: Rect()
        return WindowMetrics(bounds, density = 1f)
    }

    /** Clears any overrides set with [.setCurrentBounds] or [.setCurrentBoundsForActivity]. */
    fun reset() {
        overrideBounds = null
        overrideMaxBounds = null
        currentBounds.clear()
        maxBounds.clear()
    }
}
