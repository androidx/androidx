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

import android.util.LayoutDirection.LOCALE
import android.util.LayoutDirection.LTR
import android.util.LayoutDirection.RTL
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT

/**
 * Split configuration rules for activity pairs. Define when activities that were launched on top of
 * each other should be shown side-by-side, and the visual properties of such splits. Can be set
 * either via [RuleController.setRules] or via [RuleController.addRule]. The rules are always
 * applied only to activities that will be started after the rules were set.
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
     *
     * @see SplitRule.FINISH_NEVER
     * @see SplitRule.FINISH_ALWAYS
     * @see SplitRule.FINISH_ADJACENT
     */
    @SplitFinishBehavior
    val finishPrimaryWithSecondary: Int

    /**
     * Determines what happens with the secondary container when all activities are finished in the
     * associated primary container.
     *
     * @see SplitRule.FINISH_NEVER
     * @see SplitRule.FINISH_ALWAYS
     * @see SplitRule.FINISH_ADJACENT
     */
    @SplitFinishBehavior
    val finishSecondaryWithPrimary: Int

    /**
     * If there is an existing split with the same primary container, indicates whether the
     * existing secondary container on top and all activities in it should be destroyed when a new
     * split is created using this rule. Otherwise the new secondary will appear on top by default.
     */
    val clearTop: Boolean

    internal constructor(
        filters: Set<SplitPairFilter>,
        @SplitFinishBehavior finishPrimaryWithSecondary: Int = FINISH_NEVER,
        @SplitFinishBehavior finishSecondaryWithPrimary: Int = FINISH_ALWAYS,
        clearTop: Boolean = false,
        @IntRange(from = 0) minWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        @IntRange(from = 0) minSmallestWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        maxAspectRatioInPortrait: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT,
        maxAspectRatioInLandscape: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT,
        @FloatRange(from = 0.0, to = 1.0) splitRatio: Float = SPLIT_RATIO_DEFAULT,
        @LayoutDirection layoutDirection: Int = LOCALE
    ) : super(minWidthDp, minSmallestWidthDp, maxAspectRatioInPortrait, maxAspectRatioInLandscape,
        splitRatio, layoutDirection) {
        this.filters = filters.toSet()
        this.clearTop = clearTop
        this.finishPrimaryWithSecondary = finishPrimaryWithSecondary
        this.finishSecondaryWithPrimary = finishSecondaryWithPrimary
    }

    /**
     * Builder for [SplitPairRule].
     *
     * @param filters Filters used to choose when to apply this rule. The rule may be used if any
     * one of the provided filters matches.
     */
    class Builder(
        private val filters: Set<SplitPairFilter>,
    ) {
        @IntRange(from = 0)
        private var minWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0)
        private var minSmallestWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        private var maxAspectRatioInPortrait = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
        private var maxAspectRatioInLandscape = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
        @SplitFinishBehavior
        private var finishPrimaryWithSecondary = FINISH_NEVER
        @SplitFinishBehavior
        private var finishSecondaryWithPrimary = FINISH_ALWAYS
        private var clearTop = false
        @FloatRange(from = 0.0, to = 1.0)
        private var splitRatio = SPLIT_RATIO_DEFAULT
        @LayoutDirection
        private var layoutDirection = LOCALE

        /**
         * Sets the smallest value of width of the parent window when the split should be used, in
         * DP.
         * When the window size is smaller than requested here, activities in the secondary
         * container will be stacked on top of the activities in the primary one, completely
         * overlapping them.
         *
         * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
         * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
         */
        fun setMinWidthDp(@IntRange(from = 0) minWidthDp: Int): Builder =
            apply { this.minWidthDp = minWidthDp }

        /**
         * Sets the smallest value of the smallest possible width of the parent window in any
         * rotation  when the split should be used, in DP. When the window size is smaller than
         * requested here, activities in the secondary container will be stacked on top of the
         * activities in the primary one, completely overlapping them.
         *
         * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
         * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
         */
        fun setMinSmallestWidthDp(@IntRange(from = 0) minSmallestWidthDp: Int): Builder =
            apply { this.minSmallestWidthDp = minSmallestWidthDp }

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
         * @see EmbeddingAspectRatio.ratio
         * @see EmbeddingAspectRatio.ALWAYS_ALLOW
         * @see EmbeddingAspectRatio.ALWAYS_DISALLOW
         */
        fun setMaxAspectRatioInPortrait(aspectRatio: EmbeddingAspectRatio): Builder =
            apply { this.maxAspectRatioInPortrait = aspectRatio }

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
         * @see EmbeddingAspectRatio.ratio
         * @see EmbeddingAspectRatio.ALWAYS_ALLOW
         * @see EmbeddingAspectRatio.ALWAYS_DISALLOW
         */
        fun setMaxAspectRatioInLandscape(aspectRatio: EmbeddingAspectRatio): Builder =
            apply { this.maxAspectRatioInLandscape = aspectRatio }

        /**
         * Sets the behavior of the primary container when all activities are finished in the
         * associated secondary container.
         *
         * @see SplitRule.FINISH_NEVER
         * @see SplitRule.FINISH_ALWAYS
         * @see SplitRule.FINISH_ADJACENT
         */
        fun setFinishPrimaryWithSecondary(
            @SplitFinishBehavior finishPrimaryWithSecondary: Int
        ): Builder =
            apply { this.finishPrimaryWithSecondary = finishPrimaryWithSecondary }

        /**
         * Sets the behavior of the secondary container when all activities are finished in the
         * associated primary container.
         *
         * @see SplitRule.FINISH_NEVER
         * @see SplitRule.FINISH_ALWAYS
         * @see SplitRule.FINISH_ADJACENT
         */
        fun setFinishSecondaryWithPrimary(
            @SplitFinishBehavior finishSecondaryWithPrimary: Int
        ): Builder =
            apply { this.finishSecondaryWithPrimary = finishSecondaryWithPrimary }

        /**
         * Sets whether the existing secondary container on top and all activities in it should be
         * destroyed when a new split is created using this rule. Otherwise the new secondary will
         * appear on top by default.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setClearTop(clearTop: Boolean): Builder =
            apply { this.clearTop = clearTop }

        /**
         * Sets what part of the width should be given to the primary activity.
         *
         * The default is `0.5` if the app doesn't set, which is to split with equal width.
         */
        fun setSplitRatio(@FloatRange(from = 0.0, to = 1.0) splitRatio: Float): Builder =
            apply { this.splitRatio = splitRatio }

        /**
         * Sets the layout direction for the split. The value must be one of [LTR], [RTL] or
         * [LOCALE].
         * - [LTR]: It splits the task bounds vertically, and put the primary container on the left
         *   portion, and the secondary container on the right portion.
         * - [RTL]: It splits the task bounds vertically, and put the primary container on the right
         *   portion, and the secondary container on the left portion.
         * - [LOCALE]: It splits the task bounds vertically, and the direction is deduced from the
         *   default language script of locale. The direction can be either [LTR] or [RTL].
         */
        fun setLayoutDirection(@LayoutDirection layoutDirection: Int): Builder =
            apply { this.layoutDirection = layoutDirection }

        fun build() = SplitPairRule(filters, finishPrimaryWithSecondary, finishSecondaryWithPrimary,
            clearTop, minWidthDp, minSmallestWidthDp, maxAspectRatioInPortrait,
            maxAspectRatioInLandscape, splitRatio, layoutDirection)
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
            .setMinWidthDp(minWidthDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setMaxAspectRatioInPortrait(maxAspectRatioInPortrait)
            .setMaxAspectRatioInLandscape(maxAspectRatioInLandscape)
            .setFinishPrimaryWithSecondary(finishPrimaryWithSecondary)
            .setFinishSecondaryWithPrimary(finishSecondaryWithPrimary)
            .setClearTop(clearTop)
            .setSplitRatio(splitRatio)
            .setLayoutDirection(layoutDirection)
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
            " splitRatio=$splitRatio" +
            ", layoutDirection=$layoutDirection" +
            ", minWidthDp=$minWidthDp" +
            ", minSmallestWidthDp=$minSmallestWidthDp" +
            ", maxAspectRatioInPortrait=$maxAspectRatioInPortrait" +
            ", maxAspectRatioInLandscape=$maxAspectRatioInLandscape" +
            ", clearTop=$clearTop" +
            ", finishPrimaryWithSecondary=$finishPrimaryWithSecondary" +
            ", finishSecondaryWithPrimary=$finishSecondaryWithPrimary" +
            ", filters=$filters" +
            "}"
}