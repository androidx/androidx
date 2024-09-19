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
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ALWAYS
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.NEVER

/**
 * Split configuration rules for activity pairs. Define when activities that were launched on top
 * should be placed adjacent to the one below, and the visual properties of such splits. Can be set
 * either by [RuleController.setRules] or [RuleController.addRule]. The rules are always applied
 * only to activities that will be started from the activity fills the whole parent task container
 * or activity in the primary split after the rules were set.
 */
class SplitPairRule
internal constructor(
    /**
     * Filters used to choose when to apply this rule. The rule may be used if any one of the
     * provided filters matches.
     */
    val filters: Set<SplitPairFilter>,
    defaultSplitAttributes: SplitAttributes,
    tag: String? = null,
    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated secondary container.
     *
     * @see SplitRule.FinishBehavior.NEVER
     * @see SplitRule.FinishBehavior.ALWAYS
     * @see SplitRule.FinishBehavior.ADJACENT
     */
    val finishPrimaryWithSecondary: FinishBehavior = NEVER,
    /**
     * Determines what happens with the secondary container when all activities are finished in the
     * associated primary container.
     *
     * @see SplitRule.FinishBehavior.NEVER
     * @see SplitRule.FinishBehavior.ALWAYS
     * @see SplitRule.FinishBehavior.ADJACENT
     */
    val finishSecondaryWithPrimary: FinishBehavior = ALWAYS,
    /**
     * If there is an existing split with the same primary container, indicates whether the existing
     * secondary container on top and all activities in it should be destroyed when a new split is
     * created using this rule. Otherwise the new secondary will appear on top by default.
     */
    val clearTop: Boolean = false,
    @IntRange(from = 0) minWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
    @IntRange(from = 0) minHeightDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
    @IntRange(from = 0) minSmallestWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
    maxAspectRatioInPortrait: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT,
    maxAspectRatioInLandscape: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
) :
    SplitRule(
        tag,
        minWidthDp,
        minHeightDp,
        minSmallestWidthDp,
        maxAspectRatioInPortrait,
        maxAspectRatioInLandscape,
        defaultSplitAttributes
    ) {

    /**
     * Builder for [SplitPairRule].
     *
     * @param filters Filters used to choose when to apply this rule. The rule may be used if any
     *   one of the provided filters matches.
     */
    class Builder(private val filters: Set<SplitPairFilter>) {
        private var tag: String? = null
        @IntRange(from = 0) private var minWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0) private var minHeightDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0) private var minSmallestWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        private var maxAspectRatioInPortrait = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
        private var maxAspectRatioInLandscape = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
        private var finishPrimaryWithSecondary = NEVER
        private var finishSecondaryWithPrimary = ALWAYS
        private var clearTop = false
        private var defaultSplitAttributes = SplitAttributes.Builder().build()

        /** Creates a Builder with values initialized from the original [SplitPairRule] */
        internal constructor(original: SplitPairRule) : this(original.filters) {
            this.setTag(original.tag)
                .setMinWidthDp(original.minWidthDp)
                .setMinHeightDp(original.minHeightDp)
                .setMinSmallestWidthDp(original.minSmallestWidthDp)
                .setMaxAspectRatioInPortrait(original.maxAspectRatioInPortrait)
                .setMaxAspectRatioInLandscape(original.maxAspectRatioInLandscape)
                .setFinishPrimaryWithSecondary(original.finishPrimaryWithSecondary)
                .setFinishSecondaryWithPrimary(original.finishSecondaryWithPrimary)
                .setClearTop(original.clearTop)
                .setDefaultSplitAttributes(original.defaultSplitAttributes)
        }

        /**
         * Sets the smallest value of width of the parent window when the split should be used, in
         * DP. When the window size is smaller than requested here, activities in the secondary
         * container will be stacked on top of the activities in the primary one, completely
         * overlapping them.
         *
         * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
         * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
         *
         * @param minWidthDp the smallest value of width of the parent window when the split should
         *   be used, in DP.
         */
        fun setMinWidthDp(@IntRange(from = 0) minWidthDp: Int): Builder = apply {
            this.minWidthDp = minWidthDp
        }

        /**
         * Sets the smallest value of height of the parent task window when the split should be
         * used, in DP. When the window size is smaller than requested here, activities in the
         * secondary container will be stacked on top of the activities in the primary one,
         * completely overlapping them.
         *
         * It is useful if it's necessary to split the parent window horizontally for this
         * [SplitPairRule].
         *
         * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
         * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
         *
         * @param minHeightDp the smallest value of height of the parent task window when the split
         *   should be used, in DP.
         * @see SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
         * @see SplitAttributes.LayoutDirection.BOTTOM_TO_TOP
         */
        fun setMinHeightDp(@IntRange(from = 0) minHeightDp: Int): Builder = apply {
            this.minHeightDp = minHeightDp
        }

        /**
         * Sets the smallest value of the smallest possible width of the parent window in any
         * rotation when the split should be used, in DP. When the window size is smaller than
         * requested here, activities in the secondary container will be stacked on top of the
         * activities in the primary one, completely overlapping them.
         *
         * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
         * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
         *
         * @param minSmallestWidthDp the smallest value of the smallest possible width of the parent
         *   window in any rotation when the split should be used, in DP.
         */
        fun setMinSmallestWidthDp(@IntRange(from = 0) minSmallestWidthDp: Int): Builder = apply {
            this.minSmallestWidthDp = minSmallestWidthDp
        }

        /**
         * Sets the largest value of the aspect ratio, expressed as `height / width` in decimal
         * form, of the parent window bounds in portrait when the split should be used. When the
         * window aspect ratio is greater than requested here, activities in the secondary container
         * will be stacked on top of the activities in the primary one, completely overlapping them.
         *
         * This value is only used when the parent window is in portrait (height >= width).
         *
         * The default is [SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT] if the app doesn't set, which is
         * the recommend value to only allow split when the parent window is not too stretched in
         * portrait.
         *
         * @param aspectRatio the largest value of the aspect ratio, expressed as `height / width`
         *   in decimal form, of the parent window bounds in portrait when the split should be used.
         * @see EmbeddingAspectRatio.ratio
         * @see EmbeddingAspectRatio.ALWAYS_ALLOW
         * @see EmbeddingAspectRatio.ALWAYS_DISALLOW
         */
        fun setMaxAspectRatioInPortrait(aspectRatio: EmbeddingAspectRatio): Builder = apply {
            this.maxAspectRatioInPortrait = aspectRatio
        }

        /**
         * Sets the largest value of the aspect ratio, expressed as `width / height` in decimal
         * form, of the parent window bounds in landscape when the split should be used. When the
         * window aspect ratio is greater than requested here, activities in the secondary container
         * will be stacked on top of the activities in the primary one, completely overlapping them.
         *
         * This value is only used when the parent window is in landscape (width > height).
         *
         * The default is [SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT] if the app doesn't set, which
         * is the recommend value to always allow split when the parent window is in landscape.
         *
         * @param aspectRatio the largest value of the aspect ratio, expressed as `width / height`
         *   in decimal form, of the parent window bounds in landscape when the split should be
         *   used.
         * @see EmbeddingAspectRatio.ratio
         * @see EmbeddingAspectRatio.ALWAYS_ALLOW
         * @see EmbeddingAspectRatio.ALWAYS_DISALLOW
         */
        fun setMaxAspectRatioInLandscape(aspectRatio: EmbeddingAspectRatio): Builder = apply {
            this.maxAspectRatioInLandscape = aspectRatio
        }

        /**
         * Sets the behavior of the primary container when all activities are finished in the
         * associated secondary container.
         *
         * @param finishPrimaryWithSecondary the [SplitRule.FinishBehavior] of the primary container
         *   when all activities are finished in the associated secondary container.
         * @see SplitRule.FinishBehavior.NEVER
         * @see SplitRule.FinishBehavior.ALWAYS
         * @see SplitRule.FinishBehavior.ADJACENT
         */
        fun setFinishPrimaryWithSecondary(finishPrimaryWithSecondary: FinishBehavior): Builder =
            apply {
                this.finishPrimaryWithSecondary = finishPrimaryWithSecondary
            }

        /**
         * Sets the behavior of the secondary container when all activities are finished in the
         * associated primary container.
         *
         * @param finishSecondaryWithPrimary the [SplitRule.FinishBehavior] of the secondary
         *   container when all activities are finished in the associated primary container.
         * @see SplitRule.FinishBehavior.NEVER
         * @see SplitRule.FinishBehavior.ALWAYS
         * @see SplitRule.FinishBehavior.ADJACENT
         */
        fun setFinishSecondaryWithPrimary(finishSecondaryWithPrimary: FinishBehavior): Builder =
            apply {
                this.finishSecondaryWithPrimary = finishSecondaryWithPrimary
            }

        /**
         * Sets whether the existing secondary container on top and all activities in it should be
         * destroyed when a new split is created using this rule. Otherwise the new secondary will
         * appear on top by default.
         *
         * @param clearTop whether the existing secondary container on top and all activities in it
         *   should be destroyed when a new split is created using this rule.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setClearTop(clearTop: Boolean): Builder = apply { this.clearTop = clearTop }

        /**
         * Sets the default [SplitAttributes] to apply on the activity containers pair when the host
         * task bounds satisfy [minWidthDp], [minHeightDp], [minSmallestWidthDp],
         * [maxAspectRatioInPortrait] and [maxAspectRatioInLandscape] requirements.
         *
         * @param defaultSplitAttributes the default [SplitAttributes] to apply on the activity
         *   containers pair when the host task bounds satisfy all the rule requirements.
         */
        fun setDefaultSplitAttributes(defaultSplitAttributes: SplitAttributes): Builder = apply {
            this.defaultSplitAttributes = defaultSplitAttributes
        }

        /**
         * Sets a unique string to identify this [SplitPairRule], which defaults to `null`. The
         * suggested usage is to set the tag to be able to differentiate between different rules in
         * the [SplitAttributesCalculatorParams.splitRuleTag].
         *
         * @param tag unique string to identify this [SplitPairRule].
         */
        fun setTag(tag: String?): Builder = apply { this.tag = tag }

        /**
         * Builds a `SplitPairRule` instance.
         *
         * @return The new `SplitPairRule` instance.
         */
        fun build() =
            SplitPairRule(
                filters,
                defaultSplitAttributes,
                tag,
                finishPrimaryWithSecondary,
                finishSecondaryWithPrimary,
                clearTop,
                minWidthDp,
                minHeightDp,
                minSmallestWidthDp,
                maxAspectRatioInPortrait,
                maxAspectRatioInLandscape,
            )
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     *
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
            .setMaxAspectRatioInPortrait(maxAspectRatioInPortrait)
            .setMaxAspectRatioInLandscape(maxAspectRatioInLandscape)
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
            ", maxAspectRatioInPortrait=$maxAspectRatioInPortrait" +
            ", maxAspectRatioInLandscape=$maxAspectRatioInLandscape" +
            ", clearTop=$clearTop" +
            ", finishPrimaryWithSecondary=$finishPrimaryWithSecondary" +
            ", finishSecondaryWithPrimary=$finishSecondaryWithPrimary" +
            ", filters=$filters" +
            "}"
}
