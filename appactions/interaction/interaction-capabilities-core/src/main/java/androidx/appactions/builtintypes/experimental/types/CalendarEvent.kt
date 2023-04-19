/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.builtintypes.experimental.types

// TODO(b/271634410): Update Attendee references
import androidx.appactions.builtintypes.experimental.properties.EndDate
import androidx.appactions.builtintypes.experimental.properties.Name
import androidx.appactions.builtintypes.experimental.properties.StartDate
import androidx.appactions.builtintypes.experimental.properties.Attendee
import java.time.LocalDate
import java.time.ZonedDateTime

interface CalendarEvent : Thing {
    val startDate: StartDate?
    val endDate: EndDate?
    val attendeeList: List<Attendee>
    override fun toBuilder(): Builder<*>

    companion object {
        @JvmStatic
        fun Builder(): Builder<*> = CalendarEventBuilderImpl()
    }

    interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
        fun setStartDate(startDate: StartDate?): Self
        fun setStartDate(value: LocalDate): Self
        fun setStartDate(value: ZonedDateTime): Self
        fun setEndDate(endDate: EndDate?): Self
        fun setEndDate(value: LocalDate): Self
        fun setEndDate(value: ZonedDateTime): Self
        fun addAttendee(attendee: Attendee): Self
        fun addAttendees(value: List<Attendee>): Self

        override fun build(): CalendarEvent
    }
}

private class CalendarEventBuilderImpl : CalendarEvent.Builder<CalendarEventBuilderImpl> {

    private var identifier: String? = null
    private var name: Name? = null
    private var startDate: StartDate? = null
    private var endDate: EndDate? = null
    private var attendeeList = mutableListOf<Attendee>()

    override fun build() =
        CalendarEventImpl(identifier, name, startDate, endDate, attendeeList.toList())

    override fun setStartDate(startDate: StartDate?): CalendarEventBuilderImpl = apply {
        this.startDate = startDate
    }

    override fun setStartDate(value: LocalDate): CalendarEventBuilderImpl = apply {
        startDate = StartDate(value)
    }

    override fun setStartDate(value: ZonedDateTime): CalendarEventBuilderImpl = apply {
        startDate = StartDate(value)
    }

    override fun setEndDate(endDate: EndDate?): CalendarEventBuilderImpl = apply {
        this.endDate = endDate
    }

    override fun setEndDate(value: LocalDate): CalendarEventBuilderImpl = apply {
        endDate = EndDate(value)
    }

    override fun setEndDate(value: ZonedDateTime): CalendarEventBuilderImpl = apply {
        endDate = EndDate(value)
    }

    override fun addAttendee(attendee: Attendee): CalendarEventBuilderImpl = apply {
        attendeeList.add(attendee)
    }

    override fun addAttendees(value: List<Attendee>): CalendarEventBuilderImpl = apply {
        attendeeList.addAll(value)
    }

    override fun setIdentifier(text: String?): CalendarEventBuilderImpl =
        apply { identifier = text }

    override fun setName(text: String): CalendarEventBuilderImpl = apply { name = Name(text) }

    override fun setName(name: Name?): CalendarEventBuilderImpl = apply { this.name = name }

    override fun clearName(): CalendarEventBuilderImpl = apply { name = null }
}

private class CalendarEventImpl(
    override val identifier: String?,
    override val name: Name?,
    override val startDate: StartDate?,
    override val endDate: EndDate?,
    override val attendeeList: List<Attendee>
) :
    CalendarEvent {
    override fun toBuilder(): CalendarEvent.Builder<*> =
        CalendarEventBuilderImpl()
            .setIdentifier(identifier)
            .setName(name)
            .setStartDate(startDate)
            .setEndDate(endDate)
            .addAttendees(attendeeList)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CalendarEventImpl) return false
        if (attendeeList != other.attendeeList) return false
        if (endDate != other.endDate) return false
        if (startDate != other.startDate) return false
        if (identifier != other.identifier) return false
        if (name != other.name) return false

        return true
    }
}
