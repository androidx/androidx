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

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A binding between a parameter and its Property converter / Argument setter.
 *
 * @param <ValueTypeT>
 */
@AutoValue
public abstract class TaskParamBinding<ValueTypeT> {

    /** Create a TaskParamBinding for a slot. */
    static <ValueTypeT> TaskParamBinding<ValueTypeT> create(
            String name,
            Predicate<ParamValue> groundingPredicate,
            GenericResolverInternal<ValueTypeT> resolver,
            ParamValueConverter<ValueTypeT> converter,
            Optional<DisambigEntityConverter<ValueTypeT>> entityConverter,
            Optional<SearchActionConverter<ValueTypeT>> searchActionConverter) {
        return new AutoValue_TaskParamBinding<>(
                name, groundingPredicate, resolver, converter, entityConverter,
                searchActionConverter);
    }

    /** Returns the name of this param. */
    @NonNull
    public abstract String name();

    /** Tests whether the ParamValue requires app-driven grounding or not. */
    @NonNull
    public abstract Predicate<ParamValue> groundingPredicate();

    /** Stores concrete resolver for the slot. */
    @NonNull
    public abstract GenericResolverInternal<ValueTypeT> resolver();

    /** Converts from internal {@code ParamValue} proto to public {@code ValueTypeT}. */
    @NonNull
    public abstract ParamValueConverter<ValueTypeT> converter();

    /** Converts from the {@code ValueTypeT} to app-driven disambig entities i.e. {@code Entity}. */
    @NonNull
    public abstract Optional<DisambigEntityConverter<ValueTypeT>> entityConverter();

    /** Converts an ungrounded {@code ParamValue} to a {@code SearchAction} object. */
    @NonNull
    public abstract Optional<SearchActionConverter<ValueTypeT>> searchActionConverter();
}
