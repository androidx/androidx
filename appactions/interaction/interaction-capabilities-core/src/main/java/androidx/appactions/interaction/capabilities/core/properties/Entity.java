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

package androidx.appactions.interaction.capabilities.core.properties;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Entities are used when defining ActionCapability for defining possible values for ParamProperty.
 */
public final class Entity {
    private final Optional<String> mId;
    private final Optional<String> mName;
    private final List<String> mAlternateNames;

    private Entity(Builder builder) {
        this.mId = builder.mId;
        this.mName = builder.mName;
        this.mAlternateNames = builder.mAlternateNames;
    }

    /** Returns a new Builder to build an Entity instance. */
    @NonNull
    public static Builder newBuilder() {
        return new Builder();
    }

    /** The id of this Entity. */
    @NonNull
    public Optional<String> getId() {
        return mId;
    }

    /** The name of this entity. The name is what a user may say to refer to this Entity. */
    @NonNull
    public Optional<String> getName() {
        return mName;
    }

    /**
     * The alternate names of this entity. These are alternate names a user may say to refer to
     * this
     * Entity.
     */
    @NonNull
    public List<String> getAlternateNames() {
        if (mAlternateNames == null) {
            return Collections.emptyList();
        }
        return mAlternateNames;
    }

    /** Builder class for Entity. */
    public static class Builder {
        private Optional<String> mId = Optional.empty();
        private Optional<String> mName = Optional.empty();
        private @Nullable List<String> mAlternateNames = null;

        /** Sets the id of the Entity to be built. */
        @NonNull
        public Builder setId(@NonNull String id) {
            this.mId = Optional.of(id);
            return this;
        }

        /** Sets the name of the Entity to be built. */
        @NonNull
        public Builder setName(@NonNull String name) {
            this.mName = Optional.of(name);
            return this;
        }

        /** Sets the list of alternate names of the Entity to be built. */
        @NonNull
        public Builder setAlternateNames(@NonNull List<String> alternateNames) {
            this.mAlternateNames = alternateNames;
            return this;
        }

        /** Sets the list of alternate names of the Entity to be built. */
        @NonNull
        public final Builder setAlternateNames(@NonNull String... alternateNames) {
            return setAlternateNames(Collections.unmodifiableList(Arrays.asList(alternateNames)));
        }

        /** Builds and returns an Entity. */
        @NonNull
        public Entity build() {
            return new Entity(this);
        }
    }
}
