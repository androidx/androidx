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

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.LayoutDirection.LOCALE
import android.util.LayoutDirection.LTR
import android.util.LayoutDirection.RTL
import android.view.WindowMetrics
import androidx.annotation.DoNotInline
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.util.Preconditions
import androidx.window.embedding.EmbeddingAspectRatio.Companion.alwaysAllow
import androidx.window.embedding.EmbeddingAspectRatio.Companion.ratio
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import kotlin.math.min

/**
 * Split configuration rules for activities that are launched to side in a split.
 * Define the visual properties of the split. Can be set either via [RuleController.setRules] or
 * via [RuleController.addRule]. The rules are always applied only to activities that will be
 * started after the rules were set.
 *
 * @see androidx.window.embedding.SplitPairRule
 * @see androidx.window.embedding.SplitPlaceholderRule
 */
open class SplitRule internal constructor(
    /**
     * The smallest value of width of the parent window when the split should be used, in DP.
     * When the window size is smaller than requested here, activities in the secondary container
     * will be stacked on top of the activities in the primary one, completely overlapping them.
     *
     * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
     * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
     */
    @IntRange(from = 0)
    val minWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,

    /**
     * The smallest value of the smallest possible width of the parent window in any rotation
     * when the split should be used, in DP. When the window size is smaller than requested
     * here, activities in the secondary container will be stacked on top of the activities in
     * the primary one, completely overlapping them.
     *
     * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
     * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
     */
    @IntRange(from = 0)
    val minSmallestWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,

    /**
     * The largest value of the aspect ratio, expressed as (height / width) in decimal form, of the
     * parent window bounds in portrait when the split should be used. When the window aspect ratio
     * is greater than requested here, activities in the secondary container will stacked on top of
     * the activities in the primary one, completely overlapping them.
     *
     * This value is only used when the parent window is in portrait (height >= width).
     *
     * The default is [SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT] if the app doesn't set, which is the
     * recommend value to only allow split when the parent window is not too stretched in portrait.
     *
     * @see EmbeddingAspectRatio.ratio
     * @see EmbeddingAspectRatio.alwaysAllow
     * @see EmbeddingAspectRatio.alwaysDisallow
     */
    val maxAspectRatioInPortrait: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT,

    /**
     * The largest value of the aspect ratio, expressed as (width / height) in decimal form, of the
     * parent window bounds in landscape when the split should be used. When the window aspect ratio
     * is greater than requested here, activities in the secondary container will stacked on top of
     * the activities in the primary one, completely overlapping them.
     *
     * This value is only used when the parent window is in landscape (width > height).
     *
     * The default is [SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT] if the app doesn't set, which is
     * the recommend value to always allow split when the parent window is in landscape.
     *
     * @see EmbeddingAspectRatio.ratio
     * @see EmbeddingAspectRatio.alwaysAllow
     * @see EmbeddingAspectRatio.alwaysDisallow
     */
    val maxAspectRatioInLandscape: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT,

    /**
     * Defines what part of the width should be given to the primary activity.
     *
     * The default is `0.5` if the app doesn't set, which is to split with equal width.
     */
    @FloatRange(from = 0.0, to = 1.0)
    val splitRatio: Float = SPLIT_RATIO_DEFAULT,

    /**
     * The layout direction for the split. The value must be one of [LTR], [RTL] or [LOCALE].
     * - [LTR]: It splits the task bounds vertically, and put the primary container on the left
     *   portion, and the secondary container on the right portion.
     * - [RTL]: It splits the task bounds vertically, and put the primary container on the right
     *   portion, and the secondary container on the left portion.
     * - [LOCALE]: It splits the task bounds vertically, and the direction is deduced from the
     *   default language script of locale. The direction can be either [LTR] or [RTL].
     */
    @LayoutDirection
    val layoutDirection: Int = LOCALE
) : EmbeddingRule() {

    init {
        Preconditions.checkArgumentNonnegative(minWidthDp, "minWidthDp must be non-negative")
        Preconditions.checkArgumentNonnegative(
            minSmallestWidthDp,
            "minSmallestWidthDp must be non-negative"
        )
        Preconditions.checkArgument(splitRatio in 0.0..1.0, "splitRatio must be in 0.0..1.0 range")
    }

    @IntDef(LTR, RTL, LOCALE)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class LayoutDirection

    /**
     * Determines what happens with the associated container when all activities are finished in
     * one of the containers in a split.
     *
     * For example, given that [SplitPairRule.finishPrimaryWithSecondary] is [FINISH_ADJACENT] and
     * secondary container finishes. The primary associated container is finished if it's
     * side-by-side with secondary container. The primary associated container is not finished
     * if it occupies entire task bounds.
     *
     * @see SplitPairRule.finishPrimaryWithSecondary
     * @see SplitPairRule.finishSecondaryWithPrimary
     * @see SplitPlaceholderRule.finishPrimaryWithPlaceholder
     */
    companion object {
        /**
         * Never finish the associated container.
         * @see SplitRule.Companion
         */
        const val FINISH_NEVER = 0
        /**
         * Always finish the associated container independent of the current presentation mode.
         * @see SplitRule.Companion
         */
        const val FINISH_ALWAYS = 1
        /**
         * Only finish the associated container when displayed side-by-side/adjacent to the one
         * being finished. Does not finish the associated one when containers are stacked on top of
         * each other.
         * @see SplitRule.Companion
         */
        const val FINISH_ADJACENT = 2

        /**
         * The default split ratio if it is not set by apps.
         * @see SplitRule.splitRatio
         */
        internal const val SPLIT_RATIO_DEFAULT = 0.5f

        /**
         * When the min dimension is set to this value, it means to always allow split.
         * @see SplitRule.minWidthDp
         * @see SplitRule.minSmallestWidthDp
         */
        const val SPLIT_MIN_DIMENSION_ALWAYS_ALLOW = 0

        /**
         * The default min dimension in DP for allowing split if it is not set by apps. The value
         * reflects [androidx.window.core.layout.WindowWidthSizeClass.MEDIUM].
         * @see SplitRule.minWidthDp
         * @see SplitRule.minSmallestWidthDp
         */
        const val SPLIT_MIN_DIMENSION_DP_DEFAULT = 600

        /**
         * The default max aspect ratio for allowing split when the parent window is in portrait.
         * @see SplitRule.maxAspectRatioInPortrait
         */
        @JvmField
        val SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT = ratio(1.4f)

        /**
         * The default max aspect ratio for allowing split when the parent window is in landscape.
         * @see SplitRule.maxAspectRatioInLandscape
         */
        @JvmField
        val SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT = alwaysAllow()
    }

    /**
     * Defines whether an associated container should be finished together with the one that's
     * already being finished based on their current presentation mode.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(FINISH_NEVER, FINISH_ALWAYS, FINISH_ADJACENT)
    internal annotation class SplitFinishBehavior

    /**
     * Verifies if the provided parent bounds satisfy the dimensions and aspect ratio requirements
     * to apply the rule.
     */
    internal fun checkParentMetrics(context: Context, parentMetrics: WindowMetrics): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            return false
        }
        val bounds = Api30Impl.getBounds(parentMetrics)
        // TODO(b/257000820): Application displayMetrics should only be used as a fallback. Replace
        // with Task density after we include it in WindowMetrics.
        val density = context.resources.displayMetrics.density
        return checkParentBounds(density, bounds)
    }

    /**
     * @see checkParentMetrics
     */
    internal fun checkParentBounds(density: Float, bounds: Rect): Boolean {
        val width = bounds.width()
        val height = bounds.height()
        if (width == 0 || height == 0) {
            return false
        }
        val minWidthPx = convertDpToPx(density, minWidthDp)
        val minSmallestWidthPx = convertDpToPx(density, minSmallestWidthDp)
        // Always allow split if the min dimensions are 0.
        val validMinWidth = minWidthDp == SPLIT_MIN_DIMENSION_ALWAYS_ALLOW || width >= minWidthPx
        val validSmallestMinWidth = minSmallestWidthDp == SPLIT_MIN_DIMENSION_ALWAYS_ALLOW ||
            min(width, height) >= minSmallestWidthPx
        val validAspectRatio = if (height >= width) {
            // Portrait
            maxAspectRatioInPortrait == alwaysAllow() ||
                height * 1f / width <= maxAspectRatioInPortrait.value
        } else {
            // Landscape
            maxAspectRatioInLandscape == alwaysAllow() ||
                width * 1f / height <= maxAspectRatioInLandscape.value
        }
        return validMinWidth && validSmallestMinWidth && validAspectRatio
    }

    /**
     * Converts the dimension from Dp to pixels.
     */
    private fun convertDpToPx(density: Float, @IntRange(from = 0) dimensionDp: Int): Int {
        return (dimensionDp * density + 0.5f).toInt()
    }

    @RequiresApi(30)
    internal object Api30Impl {
        @DoNotInline
        fun getBounds(windowMetrics: WindowMetrics): Rect {
            return windowMetrics.bounds
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitRule) return false

        if (minWidthDp != other.minWidthDp) return false
        if (minSmallestWidthDp != other.minSmallestWidthDp) return false
        if (maxAspectRatioInPortrait != other.maxAspectRatioInPortrait) return false
        if (maxAspectRatioInLandscape != other.maxAspectRatioInLandscape) return false
        if (splitRatio != other.splitRatio) return false
        if (layoutDirection != other.layoutDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minWidthDp
        result = 31 * result + minSmallestWidthDp
        result = 31 * result + maxAspectRatioInPortrait.hashCode()
        result = 31 * result + maxAspectRatioInLandscape.hashCode()
        result = 31 * result + splitRatio.hashCode()
        result = 31 * result + layoutDirection
        return result
    }

    override fun toString(): String =
        "${SplitRule::class.java.simpleName}{" +
            " splitRatio=$splitRatio" +
            ", layoutDirection=$layoutDirection" +
            ", minWidthDp=$minWidthDp" +
            ", minSmallestWidthDp=$minSmallestWidthDp" +
            ", maxAspectRatioInPortrait=$maxAspectRatioInPortrait" +
            ", maxAspectRatioInLandscape=$maxAspectRatioInLandscape" +
            "}"
}