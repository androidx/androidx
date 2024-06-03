/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * Split configuration rules for pinning an [ActivityStack]. Define how the pinned [ActivityStack]
 * should be displayed side-by-side with the other [ActivityStack].
 */
class SplitPinRule
internal constructor(
    /** A unique string to identify this [SplitPinRule]. */
    tag: String? = null,
    /** The default [SplitAttributes] to apply on the pin container and the paired container. */
    defaultSplitAttributes: SplitAttributes,
    /**
     * Whether this rule should be sticky. If the value is `false`, this rule be removed whenever
     * the pinned [ActivityStack] is unpinned. Set to `true` if the rule should be applied whenever
     * once again possible (e.g. the host Task bounds satisfies the size and aspect ratio
     * requirements).
     */
    val isSticky: Boolean,
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

    /** Builder for [SplitPinRule]. */
    class Builder {
        private var tag: String? = null
        @IntRange(from = 0) private var minWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0) private var minHeightDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        @IntRange(from = 0) private var minSmallestWidthDp = SPLIT_MIN_DIMENSION_DP_DEFAULT
        private var maxAspectRatioInPortrait = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
        private var maxAspectRatioInLandscape = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
        private var defaultSplitAttributes = SplitAttributes.Builder().build()
        private var isSticky: Boolean = false

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
         * [SplitPinRule].
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
         * Sets a unique string to identify this [SplitPinRule], which defaults to `null`. The
         * suggested usage is to set the tag to be able to differentiate between different rules in
         * the [SplitAttributesCalculatorParams.splitRuleTag].
         *
         * @param tag unique string to identify this [SplitPinRule].
         */
        fun setTag(tag: String?): Builder = apply { this.tag = tag }

        /**
         * Sets this rule to be sticky.
         *
         * @param isSticky whether to be a sticky rule.
         * @see isSticky
         */
        fun setSticky(isSticky: Boolean): Builder = apply { this.isSticky = isSticky }

        /**
         * Builds a [SplitPinRule] instance.
         *
         * @return The new [SplitPinRule] instance.
         */
        fun build() =
            SplitPinRule(
                tag,
                defaultSplitAttributes,
                isSticky,
                minWidthDp,
                minHeightDp,
                minSmallestWidthDp,
                maxAspectRatioInPortrait,
                maxAspectRatioInLandscape
            )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitPinRule) return false

        if (!super.equals(other)) return false
        if (isSticky != other.isSticky) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + isSticky.hashCode()
        return result
    }

    override fun toString(): String =
        "${SplitPinRule::class.java.simpleName}{" +
            "tag=$tag" +
            ", defaultSplitAttributes=$defaultSplitAttributes" +
            ", isSticky=$isSticky" +
            "}"
}
