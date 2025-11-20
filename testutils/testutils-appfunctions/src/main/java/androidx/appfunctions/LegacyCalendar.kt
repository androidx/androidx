/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import androidx.annotation.StringDef
import androidx.appsearch.annotation.Document

@Document(name = "com.google.android.appfunctions.schema.common.v1.calendar.Event")
data class LegacyEvent(
    @Document.Namespace val namespace: String = "", // unused
    /** The ID of the event. */
    @Document.Id val id: String,
    /** The title of the event. */
    @Document.StringProperty(required = true) val title: String,
    /** The description of the event. */
    @Document.StringProperty val description: String? = null,
    /** The start time (inclusive) of the event. */
    @Document.DocumentProperty(required = true) val startDate: LegacyDateTime,
    /** The end time (exclusive) of the event. */
    @Document.DocumentProperty(required = true) val endDate: LegacyDateTime,
    /**
     * The attendees of the event. IDs of
     * [com.google.android.appfunctions.schema.common.v1.persons.Person].
     */
    @Document.StringProperty val attendeeIds: List<String> = emptyList(),
    /** Whether the event is an all-day event. */
    @Document.BooleanProperty val allDay: Boolean? = null,
    /** The location of the event. */
    @Document.StringProperty val location: String? = null,
    /**
     * The recurrence schedule of the event. The RRULE, EXRULE, RDATE and EXDATE lines for a
     * recurring event as specified in RFC5545.
     */
    @Document.StringProperty val recurrenceSchedule: String? = null,
    /** The status of the event. */
    @EventStatus @Document.StringProperty val eventStatus: String? = null,
    /** Whether the event is read-only. */
    @Document.BooleanProperty val isReadOnly: Boolean? = null,
    /**
     * Whether the event is in a public calendar. A public calendar is one which can be seen by
     * anyone without needing to request for specific access.
     */
    @Document.BooleanProperty val isInPublicCalendar: Boolean? = null,
    /** Whether the user is the organizer of the event. */
    @Document.BooleanProperty val isOrganizer: Boolean? = null,
    /** The attendee status of the user themselves. */
    @EventAttendeeStatus @Document.StringProperty val selfAttendeeStatus: String? = null,
)

/** The status of the event. */
@StringDef(
    EventStatus.EVENT_UNSPECIFIED,
    EventStatus.EVENT_CONFIRMED,
    EventStatus.EVENT_TENTATIVE,
    EventStatus.EVENT_CANCELLED,
)
@Retention(AnnotationRetention.SOURCE)
annotation class EventStatus {
    companion object {
        /** The event status is unspecified. */
        const val EVENT_UNSPECIFIED = "EVENT_UNSPECIFIED"
        /** The event is confirmed. */
        const val EVENT_CONFIRMED = "EVENT_CONFIRMED"
        /** The event is tentative. */
        const val EVENT_TENTATIVE = "EVENT_TENTATIVE"
        /** The event is cancelled. */
        const val EVENT_CANCELLED = "EVENT_CANCELLED"
    }
}

/** The attendees event attendance status */
@StringDef(
    EventAttendeeStatus.STATUS_ACCEPTED,
    EventAttendeeStatus.STATUS_DECLINED,
    EventAttendeeStatus.STATUS_TENTATIVE,
)
@Retention(AnnotationRetention.SOURCE)
annotation class EventAttendeeStatus {
    companion object {
        /** The attendee accepted the event invitation. */
        const val STATUS_ACCEPTED = "STATUS_ACCEPTED"
        /** The attendee declined the event invitation. */
        const val STATUS_DECLINED = "STATUS_DECLINED"
        /** The attendee is uncertain about the event invitation. */
        const val STATUS_TENTATIVE = "STATUS_TENTATIVE"
    }
}
