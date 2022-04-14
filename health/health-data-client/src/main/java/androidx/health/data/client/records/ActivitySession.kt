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
package androidx.health.data.client.records

import androidx.annotation.RestrictTo
import androidx.health.data.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures any activity a user does. This can be common fitness activities like running or
 * different sports, as well as activities like meditation, gardening, and sleep.
 *
 * If the user was doing more than one activity during that time period, create a session for the
 * main activity type, and multiple segments for the different activity types. For example, if they
 * did a little bit of kick boxing and boxing during a mixed martial arts class, create a session
 * for mixed martial arts. You can then breakdown the different activity types into segments.
 *
 * Each record needs a start time and end time. Data points don't need to be back-to-back or
 * directly after each other, there can be gaps in between.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ActivitySession(
    /**
     * Type of activity (e.g. walking, swimming). Required field. Allowed values: [ActivityType].
     */
    @property:ActivityType public val activityType: String,
    /** Title of the session. Optional field. */
    public val title: String? = null,
    /** Additional notes for the session. Optional field. */
    public val notes: String? = null,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActivitySession) return false

        if (activityType != other.activityType) return false
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
        var result = 0
        result = 31 * result + activityType.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}
