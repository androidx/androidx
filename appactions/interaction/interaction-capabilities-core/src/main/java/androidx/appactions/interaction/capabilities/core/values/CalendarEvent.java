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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.values.properties.Attendee;

import com.google.auto.value.AutoValue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Represents a CalendarEvent. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class CalendarEvent extends Thing {
    /** Create a new CalendarEvent.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_CalendarEvent.Builder();
    }

    /** Returns the start date. */
    @NonNull
    public abstract Optional<ZonedDateTime> getStartDate();

    /** Returns the end date. */
    @NonNull
    public abstract Optional<ZonedDateTime> getEndDate();

    /** Returns the {@link Attendee}s in the CalendarEvent. */
    @NonNull
    public abstract List<Attendee> getAttendeeList();

    /** Builder class for building a CalendarEvent. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<CalendarEvent> {

        private final List<Attendee> mAttendeeList = new ArrayList<>();

        /** Sets start date. */
        @NonNull
        public abstract Builder setStartDate(@NonNull ZonedDateTime startDate);

        /** Sets end date. */
        @NonNull
        public abstract Builder setEndDate(@NonNull ZonedDateTime endDate);

        /** Adds a person. */
        @NonNull
        public final Builder addAttendee(@NonNull Person person) {
            mAttendeeList.add(new Attendee(person));
            return this;
        }

        /** Adds a Attendee. */
        @NonNull
        public final Builder addAttendee(@NonNull Attendee attendee) {
            mAttendeeList.add(attendee);
            return this;
        }

        /** Add a list of attendees. */
        @NonNull
        public final Builder addAllAttendee(@NonNull Iterable<Attendee> attendees) {
            for (Attendee attendee : attendees) {
                mAttendeeList.add(attendee);
            }
            return this;
        }

        /** Add a list of persons. */
        @NonNull
        public final Builder addAllPerson(@NonNull Iterable<Person> persons) {
            for (Person person : persons) {
                mAttendeeList.add(new Attendee(person));
            }
            return this;
        }

        /** Builds and returns the CalendarEvent instance. */
        @Override
        @NonNull
        public final CalendarEvent build() {
            setAttendeeList(mAttendeeList);
            return autoBuild();
        }

        /** Sets the attendees of the CalendarEvent. */
        @NonNull
        abstract Builder setAttendeeList(@NonNull List<Attendee> attendeeList);

        @NonNull
        abstract CalendarEvent autoBuild();
    }
}
