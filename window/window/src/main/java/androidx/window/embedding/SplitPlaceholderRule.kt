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
import androidx.annotation.IntRange
import androidx.core.util.Preconditions.checkArgument
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ALWAYS
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.NEVER

/**
 * Configuration rules for split placeholders.
 *
 * A placeholder activity is usually a mostly empty activity that temporarily occupies the secondary
 * container of a split. The placeholder is intended to be replaced when another activity with
 * content is launched in a dedicated [SplitPairRule]. The placeholder activity is then occluded by
 * the newly launched activity. The placeholder can provide some optional features but must not host
 * important UI elements exclusively, since the placeholder is not shown on some devices and screen
 * configurations, such as devices with small screens.
 *
 * Configuration rules can be added using [RuleController.addRule] or [RuleController.setRules].
 *
 * See
 * [Activity embedding](https://developer.android.com/guide/topics/large-screens/activity-embedding#placeholders)
 * for more information.
 */
class SplitPlaceholderRule : SplitRule {

    /**
     * Filters used to choose when to apply this rule. The rule may be used if any one of the
     * provided filters matches.
     */
    val filters: Set<ActivityFilter>

    /** Intent to launch the placeholder activity. */
    val placeholderIntent: Intent

    /**
     * Determines whether the placeholder will show on top in a smaller window size after it first
     * appeared in a split with sufficient minimum width.
     */
    val isSticky: Boolean

    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated placeholder container.
     *
     * **Note** that it is not valid to set [SplitRule.FinishBehavior.NEVER]
     *
     * @see SplitRule.FinishBehavior.ALWAYS
     * @see SplitRule.FinishBehavior.ADJACENT
     */
    val finishPrimaryWithPlaceholder: FinishBehavior

    internal constructor(
        tag: String? = null,
        filters: Set<ActivityFilter>,
        placeholderIntent: Intent,
        isSticky: Boolean,
        finishPrimaryWithPlaceholder: FinishBehavior = ALWAYS,
        @IntRange(from = 0) minWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        @IntRange(from = 0) minHeightDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        @IntRange(from = 0) minSmallestWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        maxAspectRatioInPortrait: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT,
        maxAspectRatioInLandscape: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT,
        defaultSplitAttributes: SplitAttributes,
    ) : super(
        tag,
        minWidthDp,
        minHeightDp,
        minSmallestWidthDp,
        maxAspectRatioInPortrait,
        maxAspectRatioInLandscape,
        defaultSplitAttributes
    ) {
        checkArgument(
            finishPrimaryWithPlaceholder != NEVER,
            "NEVER is not a valid configuration for SplitPlaceholderRule. " +
                "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the current API."
        )
        this.filters = filters.toSet()
        this.placeholderIntent = placeholderIntent
        this.isSticky = isSticky
        this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
    }

    /**
     * Builder for [SplitPlaceholderRule].
     *
     * @param filters Filters used to choose when to apply this rule. The rule may be used if any
     *   one of the provided filters matches.
     * @param placeholderIntent Intent to launch the placeholder activity.
     */
    class Builder(private val filters: Set<ActivityFilter>, private val placeholderIntent: Intent) {
        private var tag: String? = null
        @IntRange(from = 0) private var minWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0) private var minHeightDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0) private var minSmallestWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        private var maxAspectRatioInPortrait = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
        private var maxAspectRatioInLandscape = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
        private var finishPrimaryWithPlaceholder = ALWAYS
        private var isSticky = false
        private var defaultSplitAttributes = SplitAttributes.Builder().build()

        /** Creates a Builder with values initialized from the original [SplitPlaceholderRule] */
        internal constructor(
            original: SplitPlaceholderRule
        ) : this(original.filters, original.placeholderIntent) {
            this.setTag(original.tag)
                .setMinWidthDp(original.minWidthDp)
                .setMinHeightDp(original.minHeightDp)
                .setMinSmallestWidthDp(original.minSmallestWidthDp)
                .setMaxAspectRatioInPortrait(original.maxAspectRatioInPortrait)
                .setMaxAspectRatioInLandscape(original.maxAspectRatioInLandscape)
                .setFinishPrimaryWithPlaceholder(original.finishPrimaryWithPlaceholder)
                .setSticky(original.isSticky)
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
         * [SplitPlaceholderRule].
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
         * associated placeholder container.
         *
         * **Note** that it is not valid to set [SplitRule.FinishBehavior.NEVER]
         *
         * @param finishPrimaryWithPlaceholder the [SplitRule.FinishBehavior] of the primary
         *   container when all activities are finished in the associated placeholder container.
         * @see SplitRule.FinishBehavior.ALWAYS
         * @see SplitRule.FinishBehavior.ADJACENT
         */
        fun setFinishPrimaryWithPlaceholder(finishPrimaryWithPlaceholder: FinishBehavior): Builder =
            apply {
                this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
            }

        /**
         * Sets whether the placeholder will show on top in a smaller window size after it first
         * appeared in a split with sufficient minimum width.
         *
         * @param isSticky whether the placeholder will show on top in a smaller window size after
         *   it first appeared in a split with sufficient minimum width.
         */
        fun setSticky(isSticky: Boolean): Builder = apply { this.isSticky = isSticky }

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
         * Sets a unique string to identify this [SplitPlaceholderRule], which defaults to `null`.
         * The suggested usage is to set the tag to be able to differentiate between different rules
         * in the [SplitAttributesCalculatorParams.splitRuleTag].
         *
         * @param tag unique string to identify this [SplitPlaceholderRule].
         */
        fun setTag(tag: String?): Builder = apply { this.tag = tag }

        /**
         * Builds a `SplitPlaceholderRule` instance.
         *
         * @return The new `SplitPlaceholderRule` instance.
         */
        fun build() =
            SplitPlaceholderRule(
                tag,
                filters,
                placeholderIntent,
                isSticky,
                finishPrimaryWithPlaceholder,
                minWidthDp,
                minHeightDp,
                minSmallestWidthDp,
                maxAspectRatioInPortrait,
                maxAspectRatioInLandscape,
                defaultSplitAttributes,
            )
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     *
     * @see filters
     */
    internal operator fun plus(filter: ActivityFilter): SplitPlaceholderRule {
        val newSet = mutableSetOf<ActivityFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return Builder(newSet.toSet(), placeholderIntent)
            .setTag(tag)
            .setMinWidthDp(minWidthDp)
            .setMinHeightDp(minHeightDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setMaxAspectRatioInPortrait(maxAspectRatioInPortrait)
            .setMaxAspectRatioInLandscape(maxAspectRatioInLandscape)
            .setSticky(isSticky)
            .setFinishPrimaryWithPlaceholder(finishPrimaryWithPlaceholder)
            .setDefaultSplitAttributes(defaultSplitAttributes)
            .build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPlaceholderRule) return false
        if (!super.equals(other)) return false

        if (placeholderIntent != other.placeholderIntent) return false
        if (isSticky != other.isSticky) return false
        if (finishPrimaryWithPlaceholder != other.finishPrimaryWithPlaceholder) return false
        if (filters != other.filters) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + placeholderIntent.hashCode()
        result = 31 * result + isSticky.hashCode()
        result = 31 * result + finishPrimaryWithPlaceholder.hashCode()
        result = 31 * result + filters.hashCode()
        return result
    }

    override fun toString(): String =
        "SplitPlaceholderRule{" +
            "tag=$tag" +
            ", defaultSplitAttributes=$defaultSplitAttributes" +
            ", minWidthDp=$minWidthDp" +
            ", minHeightDp=$minHeightDp" +
            ", minSmallestWidthDp=$minSmallestWidthDp" +
            ", maxAspectRatioInPortrait=$maxAspectRatioInPortrait" +
            ", maxAspectRatioInLandscape=$maxAspectRatioInLandscape" +
            ", placeholderIntent=$placeholderIntent" +
            ", isSticky=$isSticky" +
            ", finishPrimaryWithPlaceholder=$finishPrimaryWithPlaceholder" +
            ", filters=$filters" +
            "}"
}
