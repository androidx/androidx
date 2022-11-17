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
import androidx.window.embedding.SplitRule.Companion.DEFAULT_SPLIT_MIN_DIMENSION_DP
import androidx.window.embedding.SplitRule.FinishBehavior.Companion.ADJACENT
import kotlin.math.min

/**
 * Split configuration rules for activities that are launched to side in a split.
 * Define the visual properties of the split. Can be set either statically via
 * [SplitController.Companion.initialize] or at runtime via
 * [SplitController.addRule]. The rules can only be  applied to activities that
 * belong to the same application and are running in the same process. The rules are always
 * applied only to activities that will be started  after the rules were set.
 *
 * Note that regardless of whether the minimal requirements ([minWidthDp], [minHeightDp] and
 * [minSmallestWidthDp]) are met or not, [SplitAttributesCalculator.computeSplitAttributesForParams]
 * will still be called for the rule if the calculator is registered via
 * [SplitController.setSplitAttributesCalculator]. Whether this [SplitRule]'s minimum requirements
 * are satisfied is dispatched in
 * [SplitAttributesCalculator.SplitAttributesCalculatorParams.isDefaultMinSizeSatisfied] instead.
 * The width and height could be verified in
 * [SplitAttributesCalculator.computeSplitAttributesForParams] as the sample[1] below shows.
 * It is useful if this rule is supported to split the parent container in different directions
 * with different device states.
 *
 * [1]:
 * ```
 * override computeSplitAttributesForParams(
 *     params: SplitAttributesCalculatorParams
 * ): SplitAttributes {
 *     val taskConfiguration = params.taskConfiguration
 *     val builder = SplitAttributes.Builder()
 *     if (taskConfiguration.screenWidthDp >= 600) {
 *         return builder
 *             .setLayoutDirection(SplitAttributes.LayoutDirection.LOCALE)
 *             .build()
 *     } else if (taskConfiguration.screenHeightDp >= 600)
 *         return builder
 *             .setLayoutDirection(SplitAttributes.LayoutDirection.TOP_TO_BOTTOM)
 *             .build()
 *     } else {
 *         // Fallback to expand the secondary container
 *         return builder
 *             .setSplitType(SplitAttributes.SplitType.expandSecondaryContainer())
 *             .build()
 *     }
 * }
 * ```
 * It is useful if this [SplitRule] is supported to split the parent container in different
 * directions with different device states.
 */
open class SplitRule internal constructor(
    tag: String? = null,
    /**
     * The smallest value of width of the parent task window when the split should be used, in DP.
     * When the window size is smaller than requested here, activities in the secondary container
     * will be stacked on top of the activities in the primary one, completely overlapping them.
     *
     * The default is [DEFAULT_SPLIT_MIN_DIMENSION_DP] if the app doesn't set.
     * `0` means to always allow split.
     */
    @IntRange(from = 0)
    val minWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,

    /**
     * The smallest value of height of the parent task window when the split should be used, in DP.
     * When the window size is smaller than requested here, activities in the secondary container
     * will be stacked on top of the activities in the primary one, completely overlapping them.
     * It is useful if it's necessary to split the parent window horizontally for this [SplitRule].
     *
     * The default is [DEFAULT_SPLIT_MIN_DIMENSION_DP] if the app doesn't set.
     * `0` means to always allow split.
     *
     * @see SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
     * @see SplitAttributes.LayoutDirection.BOTTOM_TO_TOP
     */
    @IntRange(from = 0)
    val minHeightDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,

    /**
     * The smallest value of the smallest possible width of the parent task window in any rotation
     * when the split should be used, in DP. When the window size is smaller than requested here,
     * activities in the secondary container will be stacked on top of the activities in the primary
     * one, completely overlapping them.
     *
     * The default is [DEFAULT_SPLIT_MIN_DIMENSION_DP] if the app doesn't set.
     * `0` means to always allow split.
     */
    @IntRange(from = 0)
    val minSmallestWidthDp: Int = DEFAULT_SPLIT_MIN_DIMENSION_DP,

    /**
     * The default [SplitAttributes] to apply on the activity containers pair when the host task
     * bounds satisfy [minWidthDp], [minHeightDp] and [minSmallestWidthDp] requirements.
     */
    val defaultSplitAttributes: SplitAttributes,
) : EmbeddingRule(tag) {

    companion object {
        /**
         * The default min dimension in DP for allowing split if it is not set by apps. The value
         * reflects [androidx.window.core.layout.WindowWidthSizeClass.MEDIUM].
         */
        const val DEFAULT_SPLIT_MIN_DIMENSION_DP = 600
    }

    /**
     * Determines what happens with the associated container when all activities are finished in
     * one of the containers in a split.
     *
     * For example, given that [SplitPairRule.finishPrimaryWithSecondary] is [ADJACENT] and
     * secondary container finishes. The primary associated container is finished if it's
     * side-by-side with secondary container. The primary associated container is not finished
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
             * Only finish the associated container when displayed side-by-side/adjacent to the one
             * being finished. Does not finish the associated one when containers are stacked on top
             * of each other.
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
     * Verifies if the provided parent bounds are large enough to apply the rule.
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
        val minWidthPx = convertDpToPx(density, minWidthDp)
        val minHeightPx = convertDpToPx(density, minHeightDp)
        val minSmallestWidthPx = convertDpToPx(density, minSmallestWidthDp)
        val validMinWidth = minWidthDp == 0 || bounds.width() >= minWidthPx
        val validMinHeight = minHeightDp == 0 || bounds.height() >= minHeightPx
        val validSmallestMinWidth =
            minSmallestWidthDp == 0 || min(bounds.width(), bounds.height()) >= minSmallestWidthPx
        return validMinWidth && validMinHeight && validSmallestMinWidth
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

        if (!super.equals(other)) return false
        if (minWidthDp != other.minWidthDp) return false
        if (minHeightDp != other.minHeightDp) return false
        if (minSmallestWidthDp != other.minSmallestWidthDp) return false
        if (defaultSplitAttributes != other.defaultSplitAttributes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + minWidthDp
        result = 31 * result + minHeightDp
        result = 31 * result + minSmallestWidthDp
        result = 31 * result + defaultSplitAttributes.hashCode()
        return result
    }
}