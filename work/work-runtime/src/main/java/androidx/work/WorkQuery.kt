/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.work

import android.annotation.SuppressLint
import java.util.UUID

/**
 * A specification for querying [WorkRequest]s. This is comprised of 4 components; namely ids,
 * unique work names, tags & work states.
 *
 * A [List] of [WorkRequest] ids, or a [List] of unique work names, or a [List] of [WorkRequest]
 * tags, or a [List] of [WorkInfo.State] can be specified.
 *
 * Each component in a [WorkQuery] is `AND`-ed with the others. Each value in a component is
 * `OR`-ed.
 *
 * Example: `(id1 OR id2 OR ...) AND (name1 OR name2 OR ...) AND (tag1 OR tag2 OR ...) AND (state1
 * OR state2 OR ...)`
 */
public class WorkQuery
internal constructor(
    /** The [List] of [WorkRequest] ids being queried. */
    public val ids: List<UUID> = emptyList(),

    /** The [List] of unique works name being queried */
    public val uniqueWorkNames: List<String> = emptyList(),

    /** The [List] of tags being queried */
    public val tags: List<String> = emptyList(),

    /** The [List] of [WorkInfo.State]s being queried */
    public val states: List<WorkInfo.State> = emptyList(),
) {
    /** A builder for [WorkQuery]. */
    public class Builder private constructor() {
        private val ids = mutableListOf<UUID>()
        private val uniqueWorkNames = mutableListOf<String>()
        private val tags = mutableListOf<String>()
        private val states = mutableListOf<WorkInfo.State>()

        /**
         * Adds a [List] of [WorkRequest] `ids` to the [WorkQuery]
         *
         * @param ids The [List] [WorkRequest] `ids` to add
         * @return the instance of the [Builder]
         */
        public fun addIds(ids: List<UUID>): Builder {
            this.ids += ids
            return this
        }

        /**
         * Adds a [List] of `uniqueWorkNames` to the [WorkQuery]
         *
         * @param uniqueWorkNames The [List] of unique work names to add
         * @return the instance of the [Builder]
         */
        public fun addUniqueWorkNames(uniqueWorkNames: List<String>): Builder {
            this.uniqueWorkNames += uniqueWorkNames
            return this
        }

        /**
         * Adds a [List] of [WorkRequest] tag to the [WorkQuery].
         *
         * @param tags The [List] of [WorkRequest] tags to add
         * @return the instance of the [Builder]
         */
        public fun addTags(tags: List<String>): Builder {
            this.tags += tags
            return this
        }

        /**
         * Adds a [List] of [WorkInfo.State]s to the [WorkQuery].
         *
         * @param states The [List] of [WorkInfo.State]s to add
         * @return the instance of the [Builder]
         */
        public fun addStates(states: List<WorkInfo.State>): Builder {
            this.states += states
            return this
        }

        /**
         * Creates an instance of [WorkQuery].
         *
         * @return the [WorkQuery] instance
         * @throws IllegalArgumentException if neither of ids, uniqueWorkNames, tags or states is
         *   set.
         */
        public fun build(): WorkQuery {
            if (ids.isEmpty() && uniqueWorkNames.isEmpty() && tags.isEmpty() && states.isEmpty()) {
                val message =
                    "Must specify ids, uniqueNames, tags or states when building a WorkQuery"
                throw IllegalArgumentException(message)
            }
            return WorkQuery(
                ids = ids,
                uniqueWorkNames = uniqueWorkNames,
                tags = tags,
                states = states,
            )
        }

        public companion object {
            /**
             * Creates a [WorkQuery.Builder] with a [List] of [WorkRequest] ids.
             *
             * @param ids The [List] of [WorkRequest] ids.
             * @return a [Builder] instance
             */
            @JvmStatic
            @SuppressLint("BuilderSetStyle")
            public fun fromIds(ids: List<UUID>): Builder {
                val builder = Builder()
                builder.addIds(ids)
                return builder
            }

            /**
             * Creates a [WorkQuery.Builder] with a [List] of `uniqueWorkNames`.
             *
             * @param uniqueWorkNames The [List] of unique work names
             * @return a [Builder] instance
             */
            @JvmStatic
            @SuppressLint("BuilderSetStyle")
            public fun fromUniqueWorkNames(uniqueWorkNames: List<String>): Builder {
                val builder = Builder()
                builder.addUniqueWorkNames(uniqueWorkNames)
                return builder
            }

            /**
             * Creates a [WorkQuery.Builder] with a [List] of [WorkRequest] tags.
             *
             * @param tags The [List] of [WorkRequest] tags
             * @return a [Builder] instance
             */
            @JvmStatic
            @SuppressLint("BuilderSetStyle")
            public fun fromTags(tags: List<String>): Builder {
                val builder = Builder()
                builder.addTags(tags)
                return builder
            }

            /**
             * Creates a [WorkQuery.Builder] with a [List] of [WorkInfo.State] states.
             *
             * @param states The [List] of [WorkInfo.State] to add to the [WorkQuery]
             * @return a [Builder] instance
             */
            @JvmStatic
            @SuppressLint("BuilderSetStyle")
            public fun fromStates(states: List<WorkInfo.State>): Builder {
                val builder = Builder()
                builder.addStates(states)
                return builder
            }
        }
    }

    public companion object {
        /**
         * Creates a query for [WorkRequest]s with the given ids.
         *
         * @param ids list of ids of [WorkRequest]s
         * @return a requested WorkQuery
         */
        @JvmStatic public fun fromIds(ids: List<UUID>): WorkQuery = WorkQuery(ids = ids)

        /**
         * Creates a query for [WorkRequest]s with the given ids.
         *
         * @param ids ids of [WorkRequest]s
         * @return a requested WorkQuery
         */
        @JvmStatic public fun fromIds(vararg ids: UUID): WorkQuery = WorkQuery(ids = ids.toList())

        /**
         * Creates a query for [WorkRequest]s with the given tags.
         *
         * @param tags tags of [WorkRequest]s
         * @return a requested WorkQuery
         */
        @JvmStatic public fun fromTags(tags: List<String>): WorkQuery = WorkQuery(tags = tags)

        /**
         * Creates a query for [WorkRequest]s with the given tags.
         *
         * @param tags tags of [WorkRequest]s
         * @return a requested WorkQuery
         */
        @JvmStatic
        public fun fromTags(vararg tags: String): WorkQuery = WorkQuery(tags = tags.toList())

        /**
         * Creates a query for [WorkRequest]s with the given unique names.
         *
         * @param uniqueWorkNames unique work names
         * @return a requested WorkQuery
         */
        @JvmStatic
        public fun fromUniqueWorkNames(vararg uniqueWorkNames: String): WorkQuery =
            WorkQuery(uniqueWorkNames = uniqueWorkNames.toList())

        /**
         * Creates a query for [WorkRequest]s with the given unique names.
         *
         * @param uniqueWorkNames The [List] of unique work names
         * @return a requested WorkQuery
         */
        @JvmStatic
        public fun fromUniqueWorkNames(uniqueWorkNames: List<String>): WorkQuery =
            WorkQuery(uniqueWorkNames = uniqueWorkNames)

        /**
         * Creates a [WorkQuery] for the workers in the given [WorkInfo.State] states.
         *
         * @param states The [List] of [WorkInfo.State]
         * @return a requested WorkQuery
         */
        @JvmStatic
        public fun fromStates(states: List<WorkInfo.State>): WorkQuery = WorkQuery(states = states)

        /**
         * Creates a [WorkQuery] for the workers in the given [WorkInfo.State] states.
         *
         * @param states states of workers
         * @return a requested WorkQuery
         */
        @JvmStatic
        public fun fromStates(vararg states: WorkInfo.State): WorkQuery =
            WorkQuery(states = states.toList())
    }
}
