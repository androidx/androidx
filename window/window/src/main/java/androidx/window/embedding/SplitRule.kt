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
import android.view.WindowMetrics
import androidx.annotation.DoNotInline
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.util.Preconditions
import androidx.window.embedding.EmbeddingAspectRatio.Companion.ALWAYS_ALLOW
import androidx.window.embedding.EmbeddingAspectRatio.Companion.ratio
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_ALWAYS_ALLOW
import androidx.window.embedding.SplitRule.Companion.SPLIT_MIN_DIMENSION_DP_DEFAULT
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ADJACENT
import kotlin.math.min

/**
 * Split configuration rules for activities that are launched to side in a split.
 * Define the visual properties of the split. Can be set either via [RuleController.setRules] or
 * via [RuleController.addRule]. The rules are always applied only to activities that will be
 * started after the rules were set.
 *
 * Note that regardless of whether the minimal requirements ([minWidthDp], [minHeightDp] and
 * [minSmallestWidthDp]) are met or not, the callback set in
 * [SplitController.setSplitAttributesCalculator] will still be called for the rule if the
 * calculator is registered via [SplitController.setSplitAttributesCalculator].
 * Whether this [SplitRule]'s minimum requirements are satisfied is dispatched in
 * [SplitAttributesCalculatorParams.areDefaultConstraintsSatisfied] instead.
 * The width and height could be verified in the [SplitAttributes] calculator callback
 * as the sample linked below shows.
 *
 * It is useful if this [SplitRule] is supported to split the parent container in different
 * directions with different device states.
 *
 * @sample androidx.window.samples.embedding.splitWithOrientations
 * @see androidx.window.embedding.SplitPairRule
 * @see androidx.window.embedding.SplitPlaceholderRule
 */
open class SplitRule internal constructor(
    tag: String? = null,
    /**
     * The smallest value of width of the parent task window when the split should be used, in DP.
     * When the window size is smaller than requested here, activities in the secondary container
     * will be stacked on top of the activities in the primary one, completely overlapping them.
     *
     * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
     * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
     */
    @IntRange(from = 0)
    val minWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,

    /**
     * The smallest value of height of the parent task window when the split should be used, in DP.
     * When the window size is smaller than requested here, activities in the secondary container
     * will be stacked on top of the activities in the primary one, completely overlapping them.
     * It is useful if it's necessary to split the parent window horizontally for this [SplitRule].
     *
     * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
     * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
     *
     * @see SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
     * @see SplitAttributes.LayoutDirection.BOTTOM_TO_TOP
     */
    @IntRange(from = 0)
    val minHeightDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,

    /**
     * The smallest value of the smallest possible width of the parent task window in any rotation
     * when the split should be used, in DP. When the window size is smaller than requested here,
     * activities in the secondary container will be stacked on top of the activities in the primary
     * one, completely overlapping them.
     *
     * The default is [SPLIT_MIN_DIMENSION_DP_DEFAULT] if the app doesn't set.
     * [SPLIT_MIN_DIMENSION_ALWAYS_ALLOW] means to always allow split.
     */
    @IntRange(from = 0)
    val minSmallestWidthDp: Int = SPLIT_MIN_DIMENSION_DP_DEFAULT,

    /**
     * The largest value of the aspect ratio, expressed as `height / width` in decimal form, of the
     * parent window bounds in portrait when the split should be used. When the window aspect ratio
     * is greater than requested here, activities in the secondary container will be stacked on top
     * of the activities in the primary one, completely overlapping them.
     *
     * This value is only used when the parent window is in portrait (height >= width).
     *
     * The default is [SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT], which is the recommend value to
     * only allow split when the parent window is not too stretched in portrait.
     *
     * @see EmbeddingAspectRatio.ratio
     * @see EmbeddingAspectRatio.ALWAYS_ALLOW
     * @see EmbeddingAspectRatio.ALWAYS_DISALLOW
     */
    val maxAspectRatioInPortrait: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_PORTRAIT_DEFAULT,

    /**
     * The largest value of the aspect ratio, expressed as `width / height` in decimal form, of the
     * parent window bounds in landscape when the split should be used. When the window aspect ratio
     * is greater than requested here, activities in the secondary container will be stacked on top
     * of the activities in the primary one, completely overlapping them.
     *
     * This value is only used when the parent window is in landscape (width > height).
     *
     * The default is [SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT], which is the recommend value to
     * always allow split when the parent window is in landscape.
     *
     * @see EmbeddingAspectRatio.ratio
     * @see EmbeddingAspectRatio.ALWAYS_ALLOW
     * @see EmbeddingAspectRatio.ALWAYS_DISALLOW
     */
    val maxAspectRatioInLandscape: EmbeddingAspectRatio = SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT,

    /**
     * The default [SplitAttributes] to apply on the activity containers pair when the host task
     * bounds satisfy [minWidthDp], [minHeightDp], [minSmallestWidthDp],
     * [maxAspectRatioInPortrait] and [maxAspectRatioInLandscape] requirements.
     */
    val defaultSplitAttributes: SplitAttributes,
) : EmbeddingRule(tag) {

    init {
        Preconditions.checkArgumentNonnegative(minWidthDp, "minWidthDp must be non-negative")
        Preconditions.checkArgumentNonnegative(minHeightDp, "minHeightDp must be non-negative")
        Preconditions.checkArgumentNonnegative(
            minSmallestWidthDp,
            "minSmallestWidthDp must be non-negative"
        )
    }

    companion object {
        /**
         * When the min dimension is set to this value, it means to always allow split.
         * @see SplitRule.minWidthDp
         * @see SplitRule.minSmallestWidthDp
         */
        const val SPLIT_MIN_DIMENSION_ALWAYS_ALLOW = 0

        /**
         * The default min dimension in DP for allowing split if it is not set by apps. The value
         * reflects [androidx.window.core.layout.WindowWidthSizeClass.MEDIUM].
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
        val SPLIT_MAX_ASPECT_RATIO_LANDSCAPE_DEFAULT = ALWAYS_ALLOW
    }

    /**
     * Determines what happens with the associated container when all activities are finished in
     * one of the containers in a split.
     *
     * For example, given that [SplitPairRule.finishPrimaryWithSecondary] is [ADJACENT] and
     * secondary container finishes. The primary associated container is finished if it's
     * adjacent to the secondary container. The primary associated container is not finished
     * if it occupies entire task bounds.
     *
     * @see SplitPairRule.finishPrimaryWithSecondary
     * @see SplitPairRule.finishSecondaryWithPrimary
     * @see SplitPlaceholderRule.finishPrimaryWithPlaceholder
     */
    class FinishBehavior private constructor(
        /** The description of this [FinishBehavior] */
        private val description: String,
        /** The enum value defined in `splitLayoutDirection` attributes in `attrs.xml` */
        internal val value: Int,
    ) {
        override fun toString(): String = description

        companion object {
            /** Never finish the associated container. */
            @JvmField
            val NEVER = FinishBehavior("NEVER", 0)
            /**
             * Always finish the associated container independent of the current presentation mode.
             */
            @JvmField
            val ALWAYS = FinishBehavior("ALWAYS", 1)
            /**
             * Only finish the associated container when displayed adjacent to the one being
             * finished. Does not finish the associated one when containers are stacked on top of
             * each other.
             */
            @JvmField
            val ADJACENT = FinishBehavior("ADJACENT", 2)

            @JvmStatic
            internal fun getFinishBehaviorFromValue(
                @IntRange(from = 0, to = 2) value: Int
            ): FinishBehavior =
                when (value) {
                    NEVER.value -> NEVER
                    ALWAYS.value -> ALWAYS
                    ADJACENT.value -> ADJACENT
                    else -> throw IllegalArgumentException("Unknown finish behavior:$value")
                }
        }
    }

    /**
     * Verifies if the provided parent bounds satisfy the dimensions and aspect ratio requirements
     * to apply the rule.
     */
    internal fun checkParentMetrics(context: Context, parentMetrics: WindowMetrics): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            return false
        }
        val bounds = Api30Impl.getBounds(parentMetrics)
        val density = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            context.resources.displayMetrics.density
        } else {
            Api34Impl.getDensity(parentMetrics, context)
        }
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
        val minHeightPx = convertDpToPx(density, minHeightDp)
        val minSmallestWidthPx = convertDpToPx(density, minSmallestWidthDp)
        // Always allow split if the min dimensions are 0.
        val validMinWidth = minWidthDp == SPLIT_MIN_DIMENSION_ALWAYS_ALLOW || width >= minWidthPx
        val validMinHeight = minHeightDp == SPLIT_MIN_DIMENSION_ALWAYS_ALLOW ||
            height >= minHeightPx
        val validSmallestMinWidth = minSmallestWidthDp == SPLIT_MIN_DIMENSION_ALWAYS_ALLOW ||
            min(width, height) >= minSmallestWidthPx
        val validAspectRatio = if (height >= width) {
            // Portrait
            maxAspectRatioInPortrait == ALWAYS_ALLOW ||
                height * 1f / width <= maxAspectRatioInPortrait.value
        } else {
            // Landscape
            maxAspectRatioInLandscape == ALWAYS_ALLOW ||
                width * 1f / height <= maxAspectRatioInLandscape.value
        }
        return validMinWidth && validMinHeight && validSmallestMinWidth && validAspectRatio
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

    @RequiresApi(34)
    internal object Api34Impl {
        @DoNotInline
        fun getDensity(windowMetrics: WindowMetrics, context: Context): Float {
            // TODO(b/265089843) remove the try catch after U is finalized.
            return try {
                windowMetrics.density
            } catch (e: NoSuchMethodError) {
                context.resources.displayMetrics.density
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SplitRule) return false

        if (!super.equals(other)) return false
        if (minWidthDp != other.minWidthDp) return false
        if (minHeightDp != other.minHeightDp) return false
        if (minSmallestWidthDp != other.minSmallestWidthDp) return false
        if (maxAspectRatioInPortrait != other.maxAspectRatioInPortrait) return false
        if (maxAspectRatioInLandscape != other.maxAspectRatioInLandscape) return false
        if (defaultSplitAttributes != other.defaultSplitAttributes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + minWidthDp
        result = 31 * result + minHeightDp
        result = 31 * result + minSmallestWidthDp
        result = 31 * result + maxAspectRatioInPortrait.hashCode()
        result = 31 * result + maxAspectRatioInLandscape.hashCode()
        result = 31 * result + defaultSplitAttributes.hashCode()
        return result
    }

    override fun toString(): String =
        "${SplitRule::class.java.simpleName}{" +
            " tag=$tag" +
            ", defaultSplitAttributes=$defaultSplitAttributes" +
            ", minWidthDp=$minWidthDp" +
            ", minHeightDp=$minHeightDp" +
            ", minSmallestWidthDp=$minSmallestWidthDp" +
            ", maxAspectRatioInPortrait=$maxAspectRatioInPortrait" +
            ", maxAspectRatioInLandscape=$maxAspectRatioInLandscape" +
            "}"
}
