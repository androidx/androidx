/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.work.analytics

import java.util.UUID

/**
 * A specification for querying [WorkMetricsInfo]s.
 *
 * Each component in a [WorkMetricsQuery] is `AND`-ed with the others. Each value in a component is
 * `OR`-ed.
 */
@ExperimentalWorkMetricsApi
public class WorkMetricsQuery
internal constructor(
    /** The [List] of [androidx.work.WorkRequest] ids being queried. */
    public val workIds: List<UUID> = emptyList(),

    /** The [List] of [WorkMetricsInfo.State]s being queried. */
    public val states: List<WorkMetricsInfo.State> = emptyList(),

    /** The [List] of tags being queried. */
    public val tags: List<String> = emptyList(),

    /** The [List] of worker class names being queried. */
    public val workerClassNames: List<String> = emptyList(),

    /**
     * The start of the enqueue time range filter (inclusive), in milliseconds since epoch (e.g.
     * [System.currentTimeMillis]).
     */
    public val beginTimeMillis: Long = 0L,

    /**
     * The end of the enqueue time range filter (inclusive), in milliseconds since epoch (e.g.
     * [System.currentTimeMillis]).
     */
    public val endTimeMillis: Long = Long.MAX_VALUE,
) {
    /** A builder for [WorkMetricsQuery]. */
    public class Builder {
        private val workIds = mutableListOf<UUID>()
        private val states = mutableListOf<WorkMetricsInfo.State>()
        private val tags = mutableListOf<String>()
        private val workerClassNames = mutableListOf<String>()
        private var beginTimeMillis: Long = 0L
        private var endTimeMillis: Long = Long.MAX_VALUE

        /**
         * Adds a [List] of [androidx.work.WorkRequest] `workIds` to the [WorkMetricsQuery].
         *
         * @param workIds The [List] [androidx.work.WorkRequest] `workIds` to add
         * @return the instance of the [Builder]
         */
        public fun addWorkIds(workIds: List<UUID>): Builder {
            this.workIds += workIds
            return this
        }

        /**
         * Adds [androidx.work.WorkRequest] `workIds` to the [WorkMetricsQuery].
         *
         * @param workIds The [androidx.work.WorkRequest] `workIds` to add
         * @return the instance of the [Builder]
         */
        public fun addWorkIds(vararg workIds: UUID): Builder {
            this.workIds += workIds.toList()
            return this
        }

        /**
         * Adds a [List] of [WorkMetricsInfo.State]s to the [WorkMetricsQuery].
         *
         * @param states The [List] of [WorkMetricsInfo.State]s to add
         * @return the instance of the [Builder]
         */
        public fun addStates(states: List<WorkMetricsInfo.State>): Builder {
            this.states += states
            return this
        }

        /**
         * Adds [WorkMetricsInfo.State]s to the [WorkMetricsQuery].
         *
         * @param states The [WorkMetricsInfo.State]s to add
         * @return the instance of the [Builder]
         */
        public fun addStates(vararg states: WorkMetricsInfo.State): Builder {
            this.states += states.toList()
            return this
        }

        /**
         * Adds a [List] of tags to the [WorkMetricsQuery].
         *
         * @param tags The [List] of tags to add
         * @return the instance of the [Builder]
         */
        public fun addTags(tags: List<String>): Builder {
            this.tags += tags
            return this
        }

        /**
         * Adds tags to the [WorkMetricsQuery].
         *
         * @param tags The tags to add
         * @return the instance of the [Builder]
         */
        public fun addTags(vararg tags: String): Builder {
            this.tags += tags.toList()
            return this
        }

        /**
         * Adds a [List] of worker class names to the [WorkMetricsQuery].
         *
         * @param workerClassNames The [List] of worker class names to add
         * @return the instance of the [Builder]
         */
        public fun addWorkerClassNames(workerClassNames: List<String>): Builder {
            this.workerClassNames += workerClassNames
            return this
        }

        /**
         * Adds worker class names to the [WorkMetricsQuery].
         *
         * @param workerClassNames The worker class names to add
         * @return the instance of the [Builder]
         */
        public fun addWorkerClassNames(vararg workerClassNames: String): Builder {
            this.workerClassNames += workerClassNames.toList()
            return this
        }

        /**
         * Sets the start of the enqueue time range to filter the [WorkMetricsQuery].
         *
         * @param beginTimeMillis The start of the time range (inclusive), in milliseconds since
         *   epoch (e.g. [System.currentTimeMillis]).
         * @return the instance of the [Builder]
         */
        public fun setBeginTimeMillis(beginTimeMillis: Long): Builder {
            this.beginTimeMillis = beginTimeMillis
            return this
        }

        /**
         * Sets the end of the enqueue time range to filter the [WorkMetricsQuery].
         *
         * @param endTimeMillis The end of the time range (inclusive), in milliseconds since epoch
         *   (e.g. [System.currentTimeMillis]).
         * @return the instance of the [Builder]
         */
        public fun setEndTimeMillis(endTimeMillis: Long): Builder {
            this.endTimeMillis = endTimeMillis
            return this
        }

        /**
         * Creates an instance of [WorkMetricsQuery].
         *
         * @return the [WorkMetricsQuery] instance
         */
        public fun build(): WorkMetricsQuery {
            require(beginTimeMillis >= 0) { "beginTimeMillis must be non-negative" }
            require(endTimeMillis >= 0) { "endTimeMillis must be non-negative" }
            require(beginTimeMillis <= endTimeMillis) {
                "beginTimeMillis must be less than or equal to endTimeMillis"
            }
            return WorkMetricsQuery(
                workIds = workIds.toList(),
                states = states.toList(),
                tags = tags.toList(),
                workerClassNames = workerClassNames.toList(),
                beginTimeMillis = beginTimeMillis,
                endTimeMillis = endTimeMillis,
            )
        }
    }
}
