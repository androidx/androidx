/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the user's length and type of sleep. Each record represents a time interval for a stage
 * of sleep.
 *
 * The start time of the record represents the start of the sleep stage and always needs to be
 * included. The timestamp represents the end of the sleep stage. Time intervals don't need to be
 * continuous but shouldn't overlap.
 *
 * Example code demonstrate how to read sleep session with stages:
 * @sample androidx.health.connect.client.samples.ReadSleepSessions
 *
 * When deleting a session, associated sleep stage records need to be deleted separately:
 * @sample androidx.health.connect.client.samples.DeleteSleepSession
 *
 * @see SleepStageRecord
 */
public class SleepSessionRecord @RestrictTo(RestrictTo.Scope.LIBRARY) constructor(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    /** Title of the session. Optional field. */
    public val title: String? = null,
    /** Additional notes for the session. Optional field. */
    public val notes: String? = null,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    public val stages: List<Stage> = emptyList(),
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {
    public constructor(
        startTime: Instant,
        startZoneOffset: ZoneOffset?,
        endTime: Instant,
        endZoneOffset: ZoneOffset?,
        /** Title of the session. Optional field. */
        title: String? = null,
        /** Additional notes for the session. Optional field. */
        notes: String? = null,
        metadata: Metadata = Metadata.EMPTY,
    ) : this(
        startTime,
        startZoneOffset,
        endTime,
        endZoneOffset,
        title,
        notes,
        emptyList(),
        metadata
    )

    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        if (stages.isNotEmpty()) {
            val sortedStages = stages.sortedWith { a, b -> a.startTime.compareTo(b.startTime) }
            for (i in 0 until sortedStages.lastIndex) {
                require(!sortedStages[i].endTime.isAfter(sortedStages[i + 1].startTime))
            }
            // check all stages are within parent session duration
            require(!sortedStages.first().startTime.isBefore(startTime))
            require(!sortedStages.last().endTime.isAfter(endTime))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SleepSessionRecord) return false

        if (title != other.title) return false
        if (notes != other.notes) return false
        if (stages != other.stages) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + title.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + stages.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        /**
         * Metric identifier to retrieve the total sleep session duration from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SLEEP_DURATION_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric("SleepSession")

        /** Use this type if the stage of sleep is unknown. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_UNKNOWN = 0

        /**
         * The user is awake and either known to be in bed, or it is unknown whether they are in bed
         * or not.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_AWAKE = 1

        /** The user is asleep but the particular stage of sleep (light, deep or REM) is unknown. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_SLEEPING = 2

        /** The user is out of bed and assumed to be awake. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_OUT_OF_BED = 3

        /** The user is in a light sleep stage. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_LIGHT = 4

        /** The user is in a deep sleep stage. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_DEEP = 5

        /** The user is in a REM sleep stage. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_REM = 6

        /** The user is awake and in bed. */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        const val STAGE_TYPE_AWAKE_IN_BED = 7

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val STAGE_TYPE_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                "awake" to STAGE_TYPE_AWAKE,
                "sleeping" to STAGE_TYPE_SLEEPING,
                "out_of_bed" to STAGE_TYPE_OUT_OF_BED,
                "light" to STAGE_TYPE_LIGHT,
                "deep" to STAGE_TYPE_DEEP,
                "rem" to STAGE_TYPE_REM,
                "awake_in_bed" to STAGE_TYPE_AWAKE_IN_BED,
                "unknown" to STAGE_TYPE_UNKNOWN
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val STAGE_TYPE_INT_TO_STRING_MAP =
            STAGE_TYPE_STRING_TO_INT_MAP.entries.associateBy({ it.value }, { it.key })
    }

    /**
     * Type of sleep stage.
     * @suppress
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
        [
            STAGE_TYPE_UNKNOWN,
            STAGE_TYPE_AWAKE,
            STAGE_TYPE_SLEEPING,
            STAGE_TYPE_OUT_OF_BED,
            STAGE_TYPE_LIGHT,
            STAGE_TYPE_DEEP,
            STAGE_TYPE_REM,
            STAGE_TYPE_AWAKE_IN_BED,
        ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class StageTypes

    /**
     * Captures the sleep stage the user entered during a sleep session.
     *
     * @see SleepSessionRecord
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public class Stage(
        val startTime: Instant,
        val endTime: Instant,
        @property:StageTypes val stage: Int,
    ) {
        init {
            require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Stage) return false

            if (stage != other.stage) return false
            if (startTime != other.startTime) return false
            if (endTime != other.endTime) return false

            return true
        }

        override fun hashCode(): Int {
            var result = stage.hashCode()
            result = 31 * result + startTime.hashCode()
            result = 31 * result + endTime.hashCode()
            return result
        }
    }
}
