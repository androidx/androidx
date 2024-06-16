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

/**
 * Layout configuration rules for individual activities with split layouts. Take precedence over
 * [SplitPairRule].
 */
class ActivityRule
internal constructor(
    tag: String?,
    /**
     * Filters used to choose when to apply this rule. The rule will be applied if any one of the
     * provided filters matches.
     */
    val filters: Set<ActivityFilter>,
    /**
     * Whether the activity should always be expanded on launch. Some activities are supposed to
     * expand to the full task bounds, independent of the state of the split. An example is an
     * activity that blocks all user interactions, such as a warning dialog.
     */
    val alwaysExpand: Boolean = false
) : EmbeddingRule(tag) {

    /**
     * Builder for [ActivityRule].
     *
     * @param filters See [ActivityRule.filters].
     */
    class Builder(
        private val filters: Set<ActivityFilter>,
    ) {
        private var tag: String? = null
        private var alwaysExpand: Boolean = false

        /**
         * Sets whether the activity should always be expanded on launch. Some activities are
         * supposed to expand to the full task bounds, independent of the state of the split. An
         * example is an activity that blocks all user interactions, such as a warning dialog.
         *
         * @param alwaysExpand whether the activity should always be expanded on launch.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")
        fun setAlwaysExpand(alwaysExpand: Boolean): Builder = apply {
            this.alwaysExpand = alwaysExpand
        }

        /**
         * Sets a unique string to identify this [ActivityRule], which defaults to `null`.
         *
         * @param tag unique string to identify this [ActivityRule].
         */
        fun setTag(tag: String?): Builder = apply { this.tag = tag }

        /**
         * Builds an `ActivityRule` instance.
         *
         * @return The new `ActivityRule` instance.
         */
        fun build() = ActivityRule(tag, filters, alwaysExpand)
    }

    /**
     * Creates a new immutable instance by adding a filter to the set.
     *
     * @see filters
     */
    internal operator fun plus(filter: ActivityFilter): ActivityRule {
        return ActivityRule(tag, filters + filter, alwaysExpand)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivityRule) return false

        if (!super.equals(other)) return false
        if (filters != other.filters) return false
        if (alwaysExpand != other.alwaysExpand) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + filters.hashCode()
        result = 31 * result + alwaysExpand.hashCode()
        return result
    }

    override fun toString(): String {
        return "ActivityRule:{" +
            "tag={$tag}," +
            "filters={$filters}, " +
            "alwaysExpand={$alwaysExpand}}"
    }
}
