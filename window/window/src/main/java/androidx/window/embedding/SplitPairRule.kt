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

import androidx.annotation.IntRange
import androidx.core.util.Preconditions.checkArgumentNonnegative
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ALWAYS
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.NEVER

/**
 * Split configuration rules for activity pairs. Define when activities that were launched on top of
 * each other should be shown side-by-side, and the visual properties of such splits. Can be set
 * either statically via [SplitController.Companion.initialize] or at runtime via
 * [SplitController.addRule]. The rules can only be  applied to activities that
 * belong to the same application and are running in the same process. The rules are  always
 * applied only to activities that will be started  after the rules were set.
 */
class SplitPairRule : SplitRule {

    /**
     * Filters used to choose when to apply this rule. The rule may be used if any one of the
     * provided filters matches.
     */
    val filters: Set<SplitPairFilter>

    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated secondary container.
     * @see SplitRule.FinishBehavior
     */
    val finishPrimaryWithSecondary: FinishBehavior

    /**
     * Determines what happens with the secondary container when all activities are finished in the
     * associated primary container.
     * @see SplitRule.FinishBehavior
     */
    val finishSecondaryWithPrimary: FinishBehavior

    /**
     * If there is an existing split with the same primary container, indicates whether the
     * existing secondary container on top and all activities in it should be destroyed when a new
     * split is created using this rule. Otherwise the new secondary will appear on top by default.
     */
    val clearTop: Boolean

    internal constructor(
        tag: String? = null,
        filters: Set<SplitPairFilter>,
        finishPrimaryWithSecondary: FinishBehavior = NEVER,
        finishSecondaryWithPrimary: FinishBehavior = ALWAYS,
        clearTop: Boolean = false,
        @IntRange(from = 0) minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        @IntRange(from = 0) minHeightDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        @IntRange(from = 0) minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,
        defaultSplitAttributes: SplitAttributes,
    ) : super(tag, minWidthDp, minHeightDp, minSmallestWidthDp, defaultSplitAttributes) {
        checkArgumentNonnegative(minWidthDp, "minWidthDp must be non-negative")
        checkArgumentNonnegative(minHeightDp, "minHeightDp must be non-negative")
        checkArgumentNonnegative(minSmallestWidthDp, "minSmallestWidthDp must be non-negative")
        this.filters = filters.toSet()
        this.clearTop = clearTop
        this.finishPrimaryWithSecondary = finishPrimaryWithSecondary
        this.finishSecondaryWithPrimary = finishSecondaryWithPrimary
    }

    /**
     * Builder for [SplitPairRule].
     *
     * @param filters See [SplitPairRule.filters].
     */
    class Builder(
        private val filters: Set<SplitPairFilter>
    ) {
        private var tag: String? = null
        @IntRange(from = 0)
        private var minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        @IntRange(from = 0)
        private var minHeightDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        @IntRange(from = 0)
        private var minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP
        private var finishPrimaryWithSecondary: FinishBehavior = NEVER
        private var finishSecondaryWithPrimary: FinishBehavior = ALWAYS
        private var clearTop: Boolean = false
        private var defaultSplitAttributes: SplitAttributes = SplitAttributes.Builder().build()

        /**
         * @see SplitPairRule.minWidthDp
         */
        fun setMinWidthDp(@IntRange(from = 0) minWidthDp: Int): Builder =
            apply { this.minWidthDp = minWidthDp }

        /**
         * @see SplitPairRule.minHeightDp
         */
        fun setMinHeightDp(@IntRange(from = 0) minHeightDp: Int): Builder =
            apply { this.minHeightDp = minHeightDp }

        /**
         * @see SplitPairRule.minSmallestWidthDp
         */
        fun setMinSmallestWidthDp(@IntRange(from = 0) minSmallestWidthDp: Int): Builder =
            apply { this.minSmallestWidthDp = minSmallestWidthDp }

        /**
         * @see SplitPairRule.finishPrimaryWithSecondary
         */
        fun setFinishPrimaryWithSecondary(
            finishPrimaryWithSecondary: FinishBehavior
        ): Builder =
            apply { this.finishPrimaryWithSecondary = finishPrimaryWithSecondary }

        /**
         * @see SplitPairRule.finishSecondaryWithPrimary
         */
        fun setFinishSecondaryWithPrimary(
            finishSecondaryWithPrimary: FinishBehavior
        ): Builder =
            apply { this.finishSecondaryWithPrimary = finishSecondaryWithPrimary }

        /**
         * @see SplitPairRule.clearTop
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setClearTop(clearTop: Boolean): Builder =
            apply { this.clearTop = clearTop }

        /** @see SplitPairRule.defaultSplitAttributes */
        fun setDefaultSplitAttributes(defaultSplitAttributes: SplitAttributes): Builder =
            apply { this.defaultSplitAttributes = defaultSplitAttributes }

        /** @see SplitPairRule.tag */
        fun setTag(tag: String?): Builder =
            apply { this.tag = tag }

        fun build() = SplitPairRule(
            tag,
            filters,
            finishPrimaryWithSecondary,
            finishSecondaryWithPrimary,
            clearTop,
            minWidthDp,
            minHeightDp,
            minSmallestWidthDp,
            defaultSplitAttributes,
        )
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     * @see filters
     */
    internal operator fun plus(filter: SplitPairFilter): SplitPairRule {
        val newSet = mutableSetOf<SplitPairFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return Builder(newSet.toSet())
            .setTag(tag)
            .setMinWidthDp(minWidthDp)
            .setMinHeightDp(minHeightDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setFinishPrimaryWithSecondary(finishPrimaryWithSecondary)
            .setFinishSecondaryWithPrimary(finishSecondaryWithPrimary)
            .setClearTop(clearTop)
            .setDefaultSplitAttributes(defaultSplitAttributes)
            .build()
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

    override fun toString(): String =
        "${SplitPairRule::class.java.simpleName}{" +
            "tag=$tag" +
            ", defaultSplitAttributes=$defaultSplitAttributes" +
            ", minWidthDp=$minWidthDp" +
            ", minHeightDp=$minHeightDp" +
            ", minSmallestWidthDp=$minSmallestWidthDp" +
            ", clearTop=$clearTop" +
            ", finishPrimaryWithSecondary=$finishPrimaryWithSecondary" +
            ", finishSecondaryWithPrimary=$finishSecondaryWithPrimary" +
            ", filters=$filters" +
            "}"
}