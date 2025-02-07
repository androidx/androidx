/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures any mindfulness session a user does. This can be mindfulness sessions like meditation,
 * breathing.
 *
 * Each record needs a start time and end time. Records don't need to be back-to-back or directly
 * after each other, there can be gaps in between.
 *
 * The ability to insert or read this record type is dependent on the version of HealthConnect
 * installed on the device. To check if available: call [HealthConnectFeatures.getFeatureStatus] and
 * pass [HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION] as an argument.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MindfulnessSessionRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    /** Type of mindfulness session (e.g. meditation, breathing). Required field. */
    @property:MindfulnessSessionTypes val mindfulnessSessionType: Int,
    /** Title of the session. Optional field. */
    val title: String? = null,
    /** Additional notes for the session. Optional field. */
    val notes: String? = null,
    override val metadata: Metadata,
) : IntervalRecord {

    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MindfulnessSessionRecord) return false

        if (title != other.title) return false
        if (notes != other.notes) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mindfulnessSessionType.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "MindfulnessSessionRecord(startTime=$startTime, startZoneOffset=$startZoneOffset, endTime=$endTime, endZoneOffset=$endZoneOffset, mindfulnessSessionType=$mindfulnessSessionType, title=$title, notes=$notes, metadata=$metadata)"
    }

    companion object {
        /**
         * Metric identifier to retrieve the total mindfulness session duration from
         * [androidx.health.connect.client.aggregate.AggregationResult]. To check if this metric is
         * available, use [HealthConnectFeatures.getFeatureStatus] with
         * [HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION] as the argument.
         */
        @JvmField
        val MINDFULNESS_DURATION_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric("MindfulnessSession")

        /**
         * Can be used to represent any generic mindfulness session that does not fall into a
         * specific category. Any unknown new value definition will also fall automatically into
         * [MINDFULNESS_SESSION_TYPE_UNKNOWN].
         *
         * Next Id: 7.
         */

        /** Use this type if the mindfulness session type is unknown. */
        const val MINDFULNESS_SESSION_TYPE_UNKNOWN = 0

        /** Meditation mindfulness session. */
        const val MINDFULNESS_SESSION_TYPE_MEDITATION = 1

        /** Other mindfulness session. */
        const val MINDFULNESS_SESSION_TYPE_OTHER = 2

        /** Guided breathing mindfulness session. */
        const val MINDFULNESS_SESSION_TYPE_BREATHING = 3

        /** Music/soundscapes mindfulness session. */
        const val MINDFULNESS_SESSION_TYPE_MUSIC = 4

        /** Stretches/movement mindfulness session. */
        const val MINDFULNESS_SESSION_TYPE_MOVEMENT = 5

        /** Unguided mindfulness session. */
        const val MINDFULNESS_SESSION_TYPE_UNGUIDED = 6

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val MINDFULNESS_SESSION_TYPE_STRING_TO_INT_MAP =
            mapOf(
                "breathing" to MINDFULNESS_SESSION_TYPE_BREATHING,
                "meditation" to MINDFULNESS_SESSION_TYPE_MEDITATION,
                "movement" to MINDFULNESS_SESSION_TYPE_MOVEMENT,
                "music" to MINDFULNESS_SESSION_TYPE_MUSIC,
                "other" to MINDFULNESS_SESSION_TYPE_OTHER,
                "unguided" to MINDFULNESS_SESSION_TYPE_UNGUIDED,
                "unknown" to MINDFULNESS_SESSION_TYPE_UNKNOWN
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val MINDFULNESS_SESSION_TYPE_INT_TO_STRING_MAP =
            MINDFULNESS_SESSION_TYPE_STRING_TO_INT_MAP.reverse()
    }

    /** List of supported activities on Health Platform. */
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(
        value =
            [
                MINDFULNESS_SESSION_TYPE_BREATHING,
                MINDFULNESS_SESSION_TYPE_MEDITATION,
                MINDFULNESS_SESSION_TYPE_MOVEMENT,
                MINDFULNESS_SESSION_TYPE_MUSIC,
                MINDFULNESS_SESSION_TYPE_OTHER,
                MINDFULNESS_SESSION_TYPE_UNGUIDED,
                MINDFULNESS_SESSION_TYPE_UNKNOWN
            ]
    )
    annotation class MindfulnessSessionTypes
}
