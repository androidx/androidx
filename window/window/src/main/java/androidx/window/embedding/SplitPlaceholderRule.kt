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

package androidx.window.embedding

import android.content.Intent
import android.util.LayoutDirection
import androidx.window.core.ExperimentalWindowApi

/**
 * Configuration rules for split placeholders.
 */
@ExperimentalWindowApi
class SplitPlaceholderRule(
    /**
     * Filters used to choose when to apply this rule.
     */
    filters: Set<ActivityFilter>,

    /**
     * Intent to launch the placeholder activity.
     */
    val placeholderIntent: Intent,

    minWidth: Int = 0,
    minSmallestWidth: Int = 0,
    splitRatio: Float = 0.5f,
    @LayoutDir
    layoutDirection: Int = LayoutDirection.LOCALE
) : SplitRule(
    minWidth,
    minSmallestWidth,
    splitRatio,
    layoutDirection
) {
    /**
     * Read-only filters used to choose when to apply this rule.
     */
    val filters: Set<ActivityFilter> = filters.toSet()

    /**
     * Creates a new immutable instance by adding a filter to the set.
     */
    internal operator fun plus(filter: ActivityFilter): SplitPlaceholderRule {
        val newSet = mutableSetOf<ActivityFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return SplitPlaceholderRule(
            newSet.toSet(),
            placeholderIntent,
            minWidth,
            minSmallestWidth,
            splitRatio,
            layoutDirection
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPlaceholderRule) return false
        if (!super.equals(other)) return false

        if (!super.equals(other)) return false
        if (filters != other.filters) return false
        if (placeholderIntent != other.placeholderIntent) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + filters.hashCode()
        result = 31 * result + placeholderIntent.hashCode()
        return result
    }
}