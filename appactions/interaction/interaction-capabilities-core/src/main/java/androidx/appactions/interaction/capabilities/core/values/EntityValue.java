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

import com.google.auto.value.AutoValue;

import java.util.Optional;

/**
 * Represents an entity value for {@code ActionCapability} which includes a value and optionally an
 * id.
 */
@AutoValue
public abstract class EntityValue {

    /** Returns a new Builder to build a EntityValue. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_EntityValue.Builder();
    }

    /** Returns a EntityValue that has both its id and value set to the given identifier. */
    @NonNull
    public static EntityValue ofId(@NonNull String id) {
        return EntityValue.newBuilder().setId(id).setValue(id).build();
    }

    /** Returns a EntityValue that has the given value and no id. */
    @NonNull
    public static EntityValue ofValue(@NonNull String value) {
        return EntityValue.newBuilder().setValue(value).build();
    }

    /** Returns the id of the EntityValue. */
    @NonNull
    public abstract Optional<String> getId();

    /** Returns the value of the EntityValue. */
    @NonNull
    public abstract String getValue();

    /** Builder for {@link EntityValue}. */
    @AutoValue.Builder
    public abstract static class Builder {
        /** Sets the identifier of the EntityValue to be built. */
        @NonNull
        public abstract Builder setId(@NonNull String id);

        /** Sets The value of the EntityValue to be built. */
        @NonNull
        public abstract Builder setValue(@NonNull String value);

        /** Builds and returns the EntityValue. */
        @NonNull
        public abstract EntityValue build();
    }
}
