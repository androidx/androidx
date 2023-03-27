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

package androidx.appactions.interaction.capabilities.core.values.properties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.values.Person;

import com.google.auto.value.AutoValue;

import java.util.Optional;

/**
 * Represents the value of the union property: http://schema.org/participant, currently it only can
 * contain {@link Person}.
 */
public class Participant {
    private final Value mValue;

    public Participant(@NonNull Person person) {
        mValue = new AutoValue_Participant_Value(Optional.of(person));
    }

    @NonNull
    public Optional<Person> asPerson() {
        return mValue.person();
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof Participant) {
            Participant that = (Participant) object;
            return this.mValue.equals(that.mValue);
        }
        return false;
    }

    /** Represents the value in the this wrapper class. */
    @AutoValue
    abstract static class Value {
        abstract Optional<Person> person();
    }
}
