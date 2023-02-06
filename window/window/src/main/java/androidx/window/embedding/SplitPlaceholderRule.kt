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
import android.util.LayoutDirection.LOCALE
import android.util.LayoutDirection.LTR
import android.util.LayoutDirection.RTL
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.core.util.Preconditions.checkArgument
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT

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

    /**
     * Intent to launch the placeholder activity.
     */
    val placeholderIntent: Intent

    /**
     * Determines whether the placeholder will show on top in a smaller window size after it first
     * appeared in a split with sufficient minimum width.
     */
    val isSticky: Boolean

    /**
     * Defines whether a container should be finished together when the associated placeholder
     * activity is being finished based on current presentation mode.
     */
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(FINISH_ALWAYS, FINISH_ADJACENT)
    internal annotation class SplitPlaceholderFinishBehavior

    /**
     * Determines what happens with the primary container when all activities are finished in the
     * associated placeholder container.
     *
     * @see SplitRule.FINISH_ALWAYS
     * @see SplitRule.FINISH_ADJACENT
     */
    @SplitPlaceholderFinishBehavior
    val finishPrimaryWithPlaceholder: Int

    internal constructor(
        filters: Set<ActivityFilter>,
        placeholderIntent: Intent,
        isSticky: Boolean,
        @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int = FINISH_ALWAYS,
        @IntRange(from = 0) minWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        @IntRange(from = 0) minSmallestWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,
        maxAspectRatioInPortrait: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT,
        maxAspectRatioInLandscape: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT,
        @FloatRange(from = 0.0, to = 1.0) splitRatio: Float = SPLIT_RATIO_DEFAULT,
        @LayoutDirection layoutDirection: Int = LOCALE
    ) : super(minWidthDp, minSmallestWidthDp, maxAspectRatioInPortrait, maxAspectRatioInLandscape,
        splitRatio, layoutDirection) {
        checkArgument(finishPrimaryWithPlaceholder != FINISH_NEVER,
            "FINISH_NEVER is not a valid configuration for SplitPlaceholderRule. " +
                "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the current API.")
        this.filters = filters.toSet()
        this.placeholderIntent = placeholderIntent
        this.isSticky = isSticky
        this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
    }

    /**
     * Builder for [SplitPlaceholderRule].
     *
     * @param filters Filters used to choose when to apply this rule. The rule may be used if any
     * one of the provided filters matches.
     * @param placeholderIntent Intent to launch the placeholder activity.
     */
    class Builder(
        private val filters: Set<ActivityFilter>,
        private val placeholderIntent: Intent,
    ) {
        @IntRange(from = 0)
        private var minWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0)
        private var minSmallestWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        private var maxAspectRatioInPortrait = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
        private var maxAspectRatioInLandscape = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
        @SplitPlaceholderFinishBehavior
        private var finishPrimaryWithPlaceholder = FINISH_ALWAYS
        private var isSticky = false
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
         * associated placeholder container.
         *
         * @see SplitRule.FINISH_ALWAYS
         * @see SplitRule.FINISH_ADJACENT
         */
        fun setFinishPrimaryWithPlaceholder(
            @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int
        ): Builder =
            apply {
               this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
            }

        /**
         * Sets whether the placeholder will show on top in a smaller window size after it first
         * appeared in a split with sufficient minimum width.
         */
        fun setSticky(isSticky: Boolean): Builder =
            apply { this.isSticky = isSticky }

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

        fun build() = SplitPlaceholderRule(filters, placeholderIntent, isSticky,
            finishPrimaryWithPlaceholder, minWidthDp, minSmallestWidthDp, maxAspectRatioInPortrait,
            maxAspectRatioInLandscape, splitRatio, layoutDirection)
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     * @see filters
     */
    internal operator fun plus(filter: ActivityFilter): SplitPlaceholderRule {
        val newSet = mutableSetOf<ActivityFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return Builder(newSet.toSet(), placeholderIntent)
            .setMinWidthDp(minWidthDp)
            .setMinSmallestWidthDp(minSmallestWidthDp)
            .setMaxAspectRatioInPortrait(maxAspectRatioInPortrait)
            .setMaxAspectRatioInLandscape(maxAspectRatioInLandscape)
            .setSticky(isSticky)
            .setFinishPrimaryWithPlaceholder(finishPrimaryWithPlaceholder)
            .setSplitRatio(splitRatio)
            .setLayoutDirection(layoutDirection)
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
            " splitRatio=$splitRatio" +
            ", layoutDirection=$layoutDirection" +
            ", minWidthDp=$minWidthDp" +
            ", minSmallestWidthDp=$minSmallestWidthDp" +
            ", maxAspectRatioInPortrait=$maxAspectRatioInPortrait" +
            ", maxAspectRatioInLandscape=$maxAspectRatioInLandscape" +
            ", placeholderIntent=$placeholderIntent" +
            ", isSticky=$isSticky" +
            ", finishPrimaryWithPlaceholder=$finishPrimaryWithPlaceholder" +
            ", filters=$filters" +
            "}"
}