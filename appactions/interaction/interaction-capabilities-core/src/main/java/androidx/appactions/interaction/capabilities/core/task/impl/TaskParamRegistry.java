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

package androidx.appactions.interaction.capabilities.core.task.impl;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.converters.DisambigEntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Bindings for grounding and resolving arguments. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class TaskParamRegistry {

    @NonNull
    public static Builder builder() {
        return new AutoValue_TaskParamRegistry.Builder();
    }

    /** Map of argument name to param binding. */
    @NonNull
    public abstract Map<String, TaskParamBinding<?>> bindings();

    /** Builder for the TaskParamRegistry. */
    @AutoValue.Builder
    public abstract static class Builder {
        private final Map<String, TaskParamBinding<?>> mBindings = new HashMap<>();

        abstract Builder setBindings(Map<String, TaskParamBinding<?>> bindings);

        /**
         * Register some slot related objects and method references.
         *
         * @param paramName          the slot name.
         * @param groundingPredicate a function that returns true if ParamValue needs grounding,
         *                           false
         *                           otherwise.
         * @param resolver           the GenericResolverInternal instance wrapping developer's slot
         *                           listener
         * @param entityConverter    a function that converts developer provided grounded objects
         *                           to Entity proto
         * @param searchActionConverter
         * @param typeConverter      a function that converts a single ParamValue to some
         *                           developer-facing object type
         * @return
         * @param <T>
         */
        @NonNull
        public final <T> Builder addTaskParameter(
                @NonNull String paramName,
                @NonNull Predicate<ParamValue> groundingPredicate,
                @NonNull GenericResolverInternal<T> resolver,
                @NonNull Optional<DisambigEntityConverter<T>> entityConverter,
                @NonNull Optional<SearchActionConverter<T>> searchActionConverter,
                @NonNull ParamValueConverter<T> typeConverter) {
            mBindings.put(
                    paramName,
                    TaskParamBinding.create(
                            paramName,
                            groundingPredicate,
                            resolver,
                            typeConverter,
                            entityConverter,
                            searchActionConverter));
            return this;
        }

        abstract TaskParamRegistry autoBuild();

        @NonNull
        public TaskParamRegistry build() {
            setBindings(Collections.unmodifiableMap(mBindings));
            return autoBuild();
        }
    }
}
