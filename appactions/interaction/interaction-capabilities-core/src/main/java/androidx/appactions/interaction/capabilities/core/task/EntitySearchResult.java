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

package androidx.appactions.interaction.capabilities.core.task;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents results from grounding an ungrounded value.
 *
 * @param <V>
 */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class EntitySearchResult<V> {

    /** Builds a EntitySearchResult with no possible values. */
    @NonNull
    public static <V> EntitySearchResult<V> empty() {
        return EntitySearchResult.<V>newBuilder().build();
    }

    @NonNull
    public static <V> Builder<V> newBuilder() {
        return new AutoValue_EntitySearchResult.Builder<>();
    }

    /**
     * The possible entity values for grounding. Returning exactly 1 result means the value will be
     * immediately accepted by the task. Returning multiple values will leave the argument in a
     * disambiguation state, and Assistant should ask for clarification from the user.
     */
    @NonNull
    public abstract List<V> possibleValues();

    /**
     * Builder for the EntitySearchResult.
     *
     * @param <V>
     */
    @AutoValue.Builder
    public abstract static class Builder<V> {
        private final List<V> mPossibleValues = new ArrayList<>();

        @NonNull
        abstract Builder<V> setPossibleValues(@NonNull List<V> possibleValues);

        @NonNull
        public final Builder<V> addPossibleValue(@NonNull V value) {
            mPossibleValues.add(value);
            return this;
        }

        abstract EntitySearchResult<V> autoBuild();

        @NonNull
        public EntitySearchResult<V> build() {
            setPossibleValues(mPossibleValues);
            return autoBuild();
        }
    }
}
