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
import androidx.appactions.interaction.capabilities.core.values.properties.Participant;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Represents a Call. */
@AutoValue
public abstract class Call extends Thing {
    /** Create a new Call.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_Call.Builder();
    }

    /** Returns the call format, e.g. video or audio. */
    @NonNull
    public abstract Optional<CallFormat> getCallFormat();

    /** Returns the {@link Participant}s in the call. */
    @NonNull
    @SuppressWarnings("AutoValueImmutableFields")
    public abstract List<Participant> getParticipantList();

    /** Format of the call. */
    public enum CallFormat {
        AUDIO("Audio"),
        VIDEO("Video");

        private final String mCallFormat;

        CallFormat(String callFormat) {
            this.mCallFormat = callFormat;
        }

        @Override
        public String toString() {
            return mCallFormat;
        }
    }

    /** Builder class for building a Call. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder> implements BuilderOf<Call> {

        private final List<Participant> mParticipantList = new ArrayList<>();

        /** Sets call format. */
        @NonNull
        public abstract Builder setCallFormat(@NonNull CallFormat callFormat);

        /** Adds a person. */
        @NonNull
        public final Builder addParticipant(@NonNull Person person) {
            mParticipantList.add(new Participant(person));
            return this;
        }

        /** Adds a Participant. */
        @NonNull
        public final Builder addParticipant(@NonNull Participant participant) {
            mParticipantList.add(participant);
            return this;
        }

        /** Add a list of participants. */
        @NonNull
        public final Builder addAllParticipant(@NonNull Iterable<Participant> participants) {
            for (Participant participant : participants) {
                mParticipantList.add(participant);
            }
            return this;
        }

        /** Add a list of persons. */
        @NonNull
        public final Builder addAllPerson(@NonNull Iterable<Person> persons) {
            for (Person person : persons) {
                mParticipantList.add(new Participant(person));
            }
            return this;
        }

        /** Builds and returns the Call instance. */
        @Override
        @NonNull
        public final Call build() {
            setParticipantList(mParticipantList);
            return autoBuild();
        }

        /** Sets the participants of the call. */
        @NonNull
        abstract Builder setParticipantList(@NonNull List<Participant> participantList);

        @NonNull
        abstract Call autoBuild();
    }
}
