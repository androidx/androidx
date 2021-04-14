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

import android.app.Activity
import android.graphics.Rect

/**
 * Subclass of [WindowBoundsHelper] used to override the results for testing.
 *
 * @see WindowBoundsHelper
 *
 * @see WindowBoundsHelper.setForTesting
 */
internal class TestWindowBoundsHelper : WindowBoundsHelper() {
    private var globalOverriddenBounds: Rect? = null
    private val overriddenBounds = mutableMapOf<Activity, Rect?>()
    private val overriddenMaximumBounds = mutableMapOf<Activity, Rect?>()

    /**
     * Overrides the bounds returned from this helper for the given context. Passing `null` [bounds]
     * has the effect of clearing the bounds override.
     *
     * Note: A global override set as a result of [.setCurrentBounds] takes precedence
     * over the value set with this method.
     */
    fun setCurrentBoundsForActivity(activity: Activity, bounds: Rect?) {
        overriddenBounds[activity] = bounds
    }

    /**
     * Overrides the max bounds returned from this helper for the given context. Passing `null`
     * [bounds] has the effect of clearing the bounds override.
     */
    fun setMaximumBoundsForActivity(activity: Activity, bounds: Rect?) {
        overriddenMaximumBounds[activity] = bounds
    }

    /**
     * Overrides the bounds returned from this helper for all supplied contexts. Passing null
     * [bounds] has the effect of clearing the global override.
     */
    fun setCurrentBounds(bounds: Rect?) {
        globalOverriddenBounds = bounds
    }

    override fun computeCurrentWindowBounds(activity: Activity): Rect {
        return globalOverriddenBounds ?: overriddenBounds[activity]
            ?: super.computeCurrentWindowBounds(activity)
    }

    override fun computeMaximumWindowBounds(activity: Activity): Rect {
        val bounds = overriddenMaximumBounds[activity]
        return bounds ?: super.computeMaximumWindowBounds(activity)
    }

    /**
     * Clears any overrides set with [.setCurrentBounds] or
     * [.setCurrentBoundsForActivity].
     */
    fun reset() {
        globalOverriddenBounds = null
        overriddenBounds.clear()
        overriddenMaximumBounds.clear()
    }
}
