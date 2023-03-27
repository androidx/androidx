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

/**
 * Represents a request to perform search for some in-app entities.
 *
 * @param <T>
 */
@AutoValue
public abstract class SearchAction<T> {

    /** Returns a new Builder instance for SearchAction. */
    @NonNull
    public static <T> Builder<T> newBuilder() {
        return new AutoValue_SearchAction.Builder<>();
    }

    /** The String query of this SearchAction. */
    @NonNull
    public abstract Optional<String> getQuery();

    /** The object to search for of this SearchAction. */
    @NonNull
    public abstract Optional<T> getObject();

    /**
     * Builder class for SearchAction.
     *
     * @param <T>
     */
    @AutoValue.Builder
    public abstract static class Builder<T> implements BuilderOf<SearchAction<T>> {
        /** Sets the String query of this SearchAction. */
        @NonNull
        public abstract Builder<T> setQuery(@NonNull String query);

        /** Sets the Object query of this SearchAction. */
        @NonNull
        public abstract Builder<T> setObject(T object);
    }
}
