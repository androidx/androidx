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
 */
@ExperimentalWindowApi
open class SplitRule internal constructor(
    /**
     * The smallest value of width of the parent window when the split should be used, in pixels.
     * When the window size is smaller than requested here, activities in the secondary container
     * will be stacked on top of the activities in the primary one, completely overlapping them.
     */
    @IntRange(from = 0)
    val minWidth: Int,

    /**
     * The smallest value of the smallest possible width of the parent window in any rotation
     * when the split should be used, in pixels. When the window size is smaller than requested
     * here, activities in the secondary container will be stacked on top of the activities in
     * the primary one, completely overlapping them.
     */
    @IntRange(from = 0)
    val minSmallestWidth: Int,

    /**
     * Defines what part of the width should be given to the primary activity. Defaults to an
     * equal width split.
     */
    @FloatRange(from = 0.0, to = 1.0)
    val splitRatio: Float = 0.5f,

    /**
     * The layout direction for the split.
     */
    @LayoutDir
    val layoutDirection: Int = LayoutDirection.LOCALE
) : EmbeddingRule() {

    @IntDef(LayoutDirection.LTR, LayoutDirection.RTL, LayoutDirection.LOCALE)
    @Retention(AnnotationRetention.SOURCE)
    // Not called LayoutDirection to avoid conflict with android.util.LayoutDirection
    internal annotation class LayoutDir

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
        val validSmallestMinWidth = (
            minSmallestWidth == 0 ||
                min(bounds.width(), bounds.height()) >= minSmallestWidth
            )
        return validMinWidth && validSmallestMinWidth
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

        if (minWidth != other.minWidth) return false
        if (minSmallestWidth != other.minSmallestWidth) return false
        if (splitRatio != other.splitRatio) return false
        if (layoutDirection != other.layoutDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = minWidth
        result = 31 * result + minSmallestWidth
        result = 31 * result + splitRatio.hashCode()
        result = 31 * result + layoutDirection
        return result
    }
}