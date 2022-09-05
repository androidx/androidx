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

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Build
import android.util.LayoutDirection
import android.view.WindowMetrics
import androidx.annotation.DoNotInline
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.window.core.ExperimentalWindowApi
import kotlin.math.min

/**
 * Split configuration rules for activities that are launched to side in a split.
 * Define the visual properties of the split. Can be set either statically via
 * [SplitController.Companion.initialize] or at runtime via
 * [SplitController.registerRule]. The rules can only be  applied to activities that
 * belong to the same application and are running in the same process. The rules are always
 * applied only to activities that will be started  after the rules were set.
 *
 * Note that regardless of whether the minimal requirements ([minWidth], [minHeight] or
 * [minSmallestWidth]) are met or not, [SplitAttributesCalculator.computeSplitAttributesForParams]
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
@ExperimentalWindowApi
open class SplitRule internal constructor(
    tag: String? = null,
    /**
     * The smallest value of width of the parent task window when the split should be used, in
     * pixels. Set to `0` to always show the split regardless of task window metrics.
     * When the window size is smaller than requested here, activities in the secondary
     * container will be stacked on top of the activities in the primary one, completely overlapping
     * them.
     */
    @IntRange(from = 0)
    val minWidth: Int,

    /**
     * The smallest value of height of the parent task window when the split should be used, in
     * pixels. Set to `0` to always show the split regardless of the task window metrics.
     * When the window size is smaller than requested here, activities in the secondary
     * container will be stacked on top of the activities in the primary one, completely overlapping
     * them. It is useful if it's necessary to split the parent window horizontally for this
     * [SplitRule].
     *
     * @see SplitAttributes.LayoutDirection.TOP_TO_BOTTOM
     * @see SplitAttributes.LayoutDirection.BOTTOM_TO_TOP
     */
    @IntRange(from = 0)
    val minHeight: Int,

    /**
     * The smallest value of the smallest possible width of the parent task window in any rotation
     * when the split should be used, in pixels. Set to `0` to always show the split regardless of
     * the task window metrics. When the window size is smaller than requested here, activities in
     * the secondary container will be stacked on top of the activities in the primary one,
     * completely overlapping them.
     */
    @IntRange(from = 0)
    val minSmallestWidth: Int,
    /**
     * The default [SplitAttributes] to apply on the activity containers pair when the host task
     * bounds satisfy [minWidth] or [minSmallestWidth] requirements.
     */
    val defaultSplitAttributes: SplitAttributes,
) : EmbeddingRule(tag) {
    // TODO(b/229656253): remove this constructor when the deprecated constructors are removed.
    @SuppressLint("Range") // The range is covered by boundary check.
    internal constructor(
        minWidth: Int,
        minSmallestWidth: Int,
        @FloatRange(from = 0.0, to = 1.0) splitRatio: Float,
        @IntRange(from = 0, to = 3) layoutDirection: Int,
    ) : this(
        tag = null,
        minWidth,
        minHeight = 0, // Set as 0 to provide compatibility for deprecated constructors.
        minSmallestWidth,
        SplitAttributes.Builder()
            .setSplitType(
                if (splitRatio == 0.0f || splitRatio == 1.0f) {
                    SplitAttributes.SplitType.expandContainers()
                } else {
                    SplitAttributes.SplitType.ratio(splitRatio)
                }
            ).setLayoutDirection(
                when (layoutDirection) {
                    LayoutDirection.LTR -> SplitAttributes.LayoutDirection.LEFT_TO_RIGHT
                    LayoutDirection.RTL -> SplitAttributes.LayoutDirection.RIGHT_TO_LEFT
                    LayoutDirection.LOCALE -> SplitAttributes.LayoutDirection.LOCALE
                    else -> throw IllegalArgumentException(
                        "Unsupported layout direction constant: $layoutDirection"
                    )
                }
            ).build())

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
    }

    /**
     * Defines whether an associated container should be finished together with the one that's
     * already being finished based on their current presentation mode.
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(FINISH_NEVER, FINISH_ALWAYS, FINISH_ADJACENT)
    internal annotation class SplitFinishBehavior

    /**
     * Verifies if the provided parent bounds are large enough to apply the rule.
     */
    internal fun checkParentMetrics(parentMetrics: WindowMetrics): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            return false
        }
        val bounds = Api30Impl.getBounds(parentMetrics)
        return checkParentBounds(bounds)
    }

    /**
     * @see checkParentMetrics
     */
    internal fun checkParentBounds(bounds: Rect): Boolean {
        val validMinWidth = (minWidth == 0 || bounds.width() >= minWidth)
        val validMinHeight = (minHeight == 0 || bounds.height() >= minHeight)
        val validSmallestMinWidth = (
            minSmallestWidth == 0 ||
                min(bounds.width(), bounds.height()) >= minSmallestWidth
            )
        return validMinWidth && validMinHeight && validSmallestMinWidth
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
        if (minWidth != other.minWidth) return false
        if (minHeight != other.minHeight) return false
        if (minSmallestWidth != other.minSmallestWidth) return false
        if (defaultSplitAttributes != other.defaultSplitAttributes) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + minWidth
        result = 31 * result + minHeight
        result = 31 * result + minSmallestWidth
        result = 31 * result + defaultSplitAttributes.hashCode()
        return result
    }
}