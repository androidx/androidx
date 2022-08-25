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
import android.util.LayoutDirection
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.core.util.Preconditions.checkArgument
import androidx.core.util.Preconditions.checkArgumentNonnegative
import androidx.window.core.ExperimentalWindowApi

/**
 * Configuration rules for split placeholders.
 */
@ExperimentalWindowApi
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
     * @see SplitPlaceholderFinishBehavior
     */
    @SplitPlaceholderFinishBehavior
    val finishPrimaryWithPlaceholder: Int

    // TODO(b/229656253): Reduce visibility to remove from public API.
    @Deprecated(
        message = "Visibility of the constructor will be reduced.",
        replaceWith = ReplaceWith("androidx.window.embedding.SplitPlaceholderRule.Builder")
    )
    constructor(
        filters: Set<ActivityFilter>,
        placeholderIntent: Intent,
        isSticky: Boolean,
        @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int = FINISH_ALWAYS,
        @IntRange(from = 0) minWidth: Int = 0,
        @IntRange(from = 0) minSmallestWidth: Int = 0,
        @FloatRange(from = 0.0, to = 1.0) splitRatio: Float = 0.5f,
        @IntRange(from = 0, to = 3) layoutDirection: Int = LayoutDirection.LOCALE
    ) : super(minWidth, minSmallestWidth, splitRatio, layoutDirection) {
        checkArgumentNonnegative(minWidth, "minWidth must be non-negative")
        checkArgumentNonnegative(minSmallestWidth, "minSmallestWidth must be non-negative")
        checkArgument(splitRatio in 0.0..1.0, "splitRatio must be in 0.0..1.0 range")
        checkArgument(finishPrimaryWithPlaceholder != FINISH_NEVER,
            "FINISH_NEVER is not a valid configuration for SplitPlaceholderRule. " +
                "Please use FINISH_ALWAYS or FINISH_ADJACENT instead or refer to the current API.")
        this.filters = filters.toSet()
        this.placeholderIntent = placeholderIntent
        this.isSticky = isSticky
        this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
    }

    // TODO(b/243345984): Update FinishBehavior to enum-like class instead of integer constants
    internal constructor(
        tag: String? = null,
        filters: Set<ActivityFilter>,
        placeholderIntent: Intent,
        isSticky: Boolean,
        @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int = FINISH_ALWAYS,
        @IntRange(from = 0) minWidth: Int = 0,
        @IntRange(from = 0) minHeight: Int = 0,
        @IntRange(from = 0) minSmallestWidth: Int = 0,
        defaultSplitAttributes: SplitAttributes,
    ) : super(tag, minWidth, minHeight, minSmallestWidth, defaultSplitAttributes) {
        checkArgumentNonnegative(minWidth, "minWidth must be non-negative")
        checkArgumentNonnegative(minHeight, "minHeight must be non-negative")
        checkArgumentNonnegative(minSmallestWidth, "minSmallestWidth must be non-negative")
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
     * @param filters See [SplitPlaceholderRule.filters].
     * @param placeholderIntent See [SplitPlaceholderRule.placeholderIntent].
     * @param minWidth See [SplitPlaceholderRule.minWidth].
     * @param minHeight See [SplitPlaceholderRule.minHeight]
     * @param minSmallestWidth See [SplitPlaceholderRule.minSmallestWidth].
     */
    class Builder(
        private val filters: Set<ActivityFilter>,
        private val placeholderIntent: Intent,
        @IntRange(from = 0)
        private val minWidth: Int,
        @IntRange(from = 0)
        private val minHeight: Int,
        @IntRange(from = 0)
        private val minSmallestWidth: Int,
    ) {
        @SplitPlaceholderFinishBehavior
        private var finishPrimaryWithPlaceholder: Int = FINISH_ALWAYS
        private var isSticky: Boolean = false
        private var defaultSplitAttributes: SplitAttributes = SplitAttributes.Builder().build()
        private var tag: String? = null

        /**
         * @see SplitPlaceholderRule.finishPrimaryWithPlaceholder
         */
        fun setFinishPrimaryWithPlaceholder(
            @SplitPlaceholderFinishBehavior finishPrimaryWithPlaceholder: Int
        ): Builder =
            apply {
               this.finishPrimaryWithPlaceholder = finishPrimaryWithPlaceholder
            }

        /**
         * @see SplitPlaceholderRule.isSticky
         */
        fun setSticky(isSticky: Boolean): Builder =
            apply { this.isSticky = isSticky }

        /** @see SplitPlaceholderRule.defaultSplitAttributes */
        fun setDefaultSplitAttributes(defaultSplitAttributes: SplitAttributes): Builder =
            apply { this.defaultSplitAttributes = defaultSplitAttributes }

        /** @see SplitPlaceholderRule.tag */
        fun setTag(tag: String?): Builder =
            apply { this.tag = tag }

        fun build() = SplitPlaceholderRule(
            tag,
            filters,
            placeholderIntent,
            isSticky,
            finishPrimaryWithPlaceholder,
            minWidth,
            minHeight,
            minSmallestWidth,
            defaultSplitAttributes,
        )
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     * @see filters
     */
    internal operator fun plus(filter: ActivityFilter): SplitPlaceholderRule {
        val newSet = mutableSetOf<ActivityFilter>()
        newSet.addAll(filters)
        newSet.add(filter)
        return SplitPlaceholderRule(
            tag,
            newSet.toSet(),
            placeholderIntent,
            isSticky,
            finishPrimaryWithPlaceholder,
            minWidth,
            minHeight,
            minSmallestWidth,
            defaultSplitAttributes,
        )
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
             ", minWidth=$minWidth" +
             ", minHeight=$minHeight" +
             ", minSmallestWidth=$minSmallestWidth" +
             ", placeholderIntent=$placeholderIntent" +
             ", isSticky=$isSticky" +
             ", finishPrimaryWithPlaceholder=$finishPrimaryWithPlaceholder" +
             ", filters=$filters" +
             "}"
}