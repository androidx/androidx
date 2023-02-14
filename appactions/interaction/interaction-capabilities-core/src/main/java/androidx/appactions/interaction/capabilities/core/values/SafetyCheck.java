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

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

/** Represents a SafetyCheck. */
@AutoValue
public abstract class SafetyCheck extends Thing {
    /** Create a new SafetyCheck instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_SafetyCheck.Builder();
    }

    /** Returns the duration. */
    @NonNull
    public abstract Optional<Duration> getDuration();

    /** Returns the check-in time. */
    @NonNull
    public abstract Optional<ZonedDateTime> getCheckinTime();

    /** Builder class for building a SafetyCheck. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<SafetyCheck> {
        @NonNull
        public abstract Builder setDuration(@NonNull Duration duration);

        @NonNull
        public abstract Builder setCheckinTime(@NonNull ZonedDateTime checkinTime);

        /** Builds and returns the SafetyCheck instance. */
        @Override
        @NonNull
        public final SafetyCheck build() {
            return autoBuild();
        }

        @NonNull
        abstract SafetyCheck autoBuild();
    }
}
