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

package androidx.glance.wear.tiles

import java.time.Instant

/**
 * TimeInterval class defines a period from [start] to [end]
 *
 * @param start The start time of the time interval
 * @param end The end time of the time interval
 */
@Suppress("DataClassDefinition")
@Deprecated("glance-wear-tiles is deprecated and will be removed")
public data class TimeInterval(
    val start: Instant = Instant.ofEpochMilli(0),
    val end: Instant = Instant.ofEpochMilli(Long.MAX_VALUE),
) {
    init {
        require(end > start) { "End time shall come after start time to form a valid interval" }
    }
}

@Deprecated("glance-wear-tiles is deprecated and will be removed")
public sealed interface TimelineMode {
    /**
     * The [GlanceTileService] provides a single UI. The layout is fixed, and only the information
     * inside the layout changes.
     */
    @Deprecated("glance-wear-tiles is deprecated and will be removed")
    public object SingleEntry : TimelineMode {
        public override fun toString(): String = "TimelineMode: SingleEntry"
    }

    /**
     * The [GlanceTileService] provides a UI for a fixed set of time intervals
     *
     * @param timeIntervals Used to build the list of time intervals, the list must not be empty.
     */
    @Deprecated("glance-wear-tiles is deprecated and will be removed")
    public class TimeBoundEntries(public val timeIntervals: Set<TimeInterval>) : TimelineMode {
        init {
            require(timeIntervals.isNotEmpty()) { "The set of time intervals cannot be empty" }
        }

        public override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TimeBoundEntries

            if (timeIntervals != other.timeIntervals) return false

            return true
        }

        public override fun hashCode(): Int = timeIntervals.hashCode()

        public override fun toString(): String =
            "TimelineMode.TimeBoundEntries(timeIntervals=$timeIntervals)"
    }
}
