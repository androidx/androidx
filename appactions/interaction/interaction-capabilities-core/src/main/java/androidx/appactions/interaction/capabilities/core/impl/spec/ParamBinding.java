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

package androidx.appactions.interaction.capabilities.core.impl.spec;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.properties.Property;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A binding between a parameter and its Property converter / Argument setter.
 */
@AutoValue
public abstract class ParamBinding<
        ArgumentsT, ArgumentsBuilderT extends BuilderOf<ArgumentsT>> {

    static <ArgumentsT, ArgumentsBuilderT extends BuilderOf<ArgumentsT>>
    ParamBinding<ArgumentsT, ArgumentsBuilderT> create(
            String name,
            Function<Map<String, Property<?>>,
                    Optional<IntentParameter>> paramGetter,
            ArgumentSetter<ArgumentsBuilderT> argumentSetter) {
        return new AutoValue_ParamBinding<>(name, paramGetter, argumentSetter);
    }

    /** Returns the name of this param. */
    @NonNull
    public abstract String name();

    /**
     * Converts a {@code Property Map} to an {@code IntentParameter} proto. The resulting proto is
     * the
     * format which we send the current params to Assistant (via. app actions context).
     */
    @NonNull
    public abstract Function<Map<String, Property<?>>,
            Optional<IntentParameter>> paramGetter();

    /**
     * Populates the {@code ArgumentsBuilderT} for this param with the {@code ParamValue} sent from
     * Assistant in Fulfillment.
     */
    @NonNull
    public abstract ArgumentSetter<ArgumentsBuilderT> argumentSetter();

    /**
     * Given a {@code List<ParamValue>}, convert it to user-visible type and set it into
     * ArgumentBuilder.
     *
     * @param <ArgumentsBuilderT>
     */
    @FunctionalInterface
    public interface ArgumentSetter<ArgumentsBuilderT> {

        /** Conversion from protos to user-visible type. */
        void setArguments(@NonNull ArgumentsBuilderT builder, @NonNull List<ParamValue> paramValues)
                throws StructConversionException;
    }
}
