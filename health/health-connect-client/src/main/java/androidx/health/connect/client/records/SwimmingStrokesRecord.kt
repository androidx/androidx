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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the number of swimming strokes. Type of swimming stroke must be provided. */
public class SwimmingStrokesRecord(
    /** Count of strokes. Optional field. Valid range: 1-1000000. */
    public val count: Long = 0,
    /**
     * Swimming style. Required field. Allowed values: [SwimmingType].
     *
     * @see SwimmingType
     */
    @property:SwimmingTypes public val type: String,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {
    init {
        requireNonNegative(value = count, name = "count")
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwimmingStrokesRecord) return false

        if (count != other.count) return false
        if (type != other.type) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + count.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    /** List of Swimming styles. */
    public object SwimmingType {
        const val FREESTYLE = "freestyle"
        const val BACKSTROKE = "backstroke"
        const val BREASTSTROKE = "breaststroke"
        const val BUTTERFLY = "butterfly"
        const val MIXED = "mixed"
        const val OTHER = "other"
    }
    /**
     * Swimming styles.
     * @suppress
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        value =
            [
                SwimmingType.FREESTYLE,
                SwimmingType.BACKSTROKE,
                SwimmingType.BREASTSTROKE,
                SwimmingType.BUTTERFLY,
                SwimmingType.MIXED,
                SwimmingType.OTHER,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class SwimmingTypes
}
