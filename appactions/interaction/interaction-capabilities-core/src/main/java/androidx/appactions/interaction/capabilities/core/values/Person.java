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

import com.google.auto.value.AutoValue;

import java.util.Optional;

/** Represents a person. */
@AutoValue
public abstract class Person extends Thing {
    /** Create a new Person.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_Person.Builder();
    }

    /** Returns the email. */
    @NonNull
    public abstract Optional<String> getEmail();

    /** Returns the telephone. */
    @NonNull
    public abstract Optional<String> getTelephone();

    /** Builder class for building a Person. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder> implements
            BuilderOf<Person> {
        /** Sets the email. */
        @NonNull
        public abstract Builder setEmail(@NonNull String email);

        /** Sets the telephone. */
        @NonNull
        public abstract Builder setTelephone(@NonNull String telephone);
    }
}
