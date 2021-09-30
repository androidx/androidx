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

import android.util.LayoutDirection
import androidx.window.core.ExperimentalWindowApi

/**
 * Split configuration rules for activity pairs. Define when activities that were launched on top of
 * each other should be shown side-by-side, and the visual properties of such splits. Can be set
 * either statically via [SplitController.Companion.initialize] or at runtime via
 * [SplitController.registerRule]. The rules can only be  applied to activities that
 * belong to the same application and are running in the same process. The rules are  always
 * applied only to activities that will be started  after the rules were set.
 */
@ExperimentalWindowApi
class SplitPairRule(
    /**
     * Filters used to choose when to apply this rule.
     */
    filters: Set<SplitPairFilter>,

    /**
     * When all activities are finished in the secondary container, the activity in the primary
     * container that created the split should also be finished.
     */
    val finishPrimaryWithSecondary: Boolean = false,

    /**
     * When all activities are finished in the primary container, the activities in the secondary
     * container in the split should also be finished.
     */
    val finishSecondaryWithPrimary: Boolean = true,

    /**
     * If there is an existing split with the same primary container, indicates whether the
     * existing secondary container on top and all activities in it should be destroyed when a new
     * split is created using this rule. Otherwise the new secondary will appear on top by default.
     */
    val clearTop: Boolean = false,

    minWidth: Int = 0,
    minSmallestWidth: Int = 0,
    splitRatio: Float = 0.5f,
    @LayoutDir
    layoutDir: Int = LayoutDirection.LOCALE,
) : SplitRule(
    minWidth,
    minSmallestWidth,
    splitRatio,
    layoutDir
) {
    /**
     * Read-only filters used to choose when to apply this rule.
     */
    val filters: Set<SplitPairFilter> = filters.toSet()

    /**
     * Creates a new immutable instance by adding a filter to the set.
     */
    internal operator fun plus(filter: SplitPairFilter): SplitPairRule {
        val newSet = mutableSetOf<SplitPairFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return SplitPairRule(
            newSet.toSet(),
            finishPrimaryWithSecondary,
            finishSecondaryWithPrimary,
            clearTop,
            minWidth,
            minSmallestWidth,
            splitRatio,
            layoutDirection
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPairRule) return false

        if (!super.equals(other)) return false
        if (filters != other.filters) return false
        if (finishPrimaryWithSecondary != other.finishPrimaryWithSecondary) return false
        if (finishSecondaryWithPrimary != other.finishSecondaryWithPrimary) return false
        if (clearTop != other.clearTop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + filters.hashCode()
        result = 31 * result + finishPrimaryWithSecondary.hashCode()
        result = 31 * result + finishSecondaryWithPrimary.hashCode()
        result = 31 * result + clearTop.hashCode()
        return result
    }
}