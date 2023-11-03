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
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the sleep stage the user entered during a sleep session.
 *
 * @see SleepSessionRecord
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SleepStageRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    /** Type of sleep stage. Required field. */
    @property:StageTypes public val stage: Int,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {

    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SleepStageRecord) return false

        if (stage != other.stage) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + stage.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        const val STAGE_TYPE_UNKNOWN = 0
        const val STAGE_TYPE_AWAKE = 1
        const val STAGE_TYPE_SLEEPING = 2
        const val STAGE_TYPE_OUT_OF_BED = 3
        const val STAGE_TYPE_LIGHT = 4
        const val STAGE_TYPE_DEEP = 5
        const val STAGE_TYPE_REM = 6

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
                "unknown" to STAGE_TYPE_UNKNOWN
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val STAGE_TYPE_INT_TO_STRING_MAP =
            STAGE_TYPE_STRING_TO_INT_MAP.entries.associateBy({ it.value }, { it.key })
    }

    /**
     * Type of sleep stage.
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
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class StageTypes
}
