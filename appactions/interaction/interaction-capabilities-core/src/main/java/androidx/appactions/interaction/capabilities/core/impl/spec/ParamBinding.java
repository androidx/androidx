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
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A binding between a parameter and its Property converter / Argument setter.
 *
 * @param <PropertyT>
 * @param <ArgumentT>
 * @param <ArgumentBuilderT>
 */
@AutoValue
public abstract class ParamBinding<
        PropertyT, ArgumentT, ArgumentBuilderT extends BuilderOf<ArgumentT>> {

    static <PropertyT, ArgumentT, ArgumentBuilderT extends BuilderOf<ArgumentT>>
            ParamBinding<PropertyT, ArgumentT, ArgumentBuilderT> create(
                    String name,
                    Function<? super PropertyT, Optional<IntentParameter>> paramGetter,
                    ArgumentSetter<ArgumentBuilderT> argumentSetter) {
        return new AutoValue_ParamBinding<>(name, paramGetter, argumentSetter);
    }

    /** Returns the name of this param. */
    @NonNull
    public abstract String name();

    /**
     * Converts a {@code PropertyT} to an {@code IntentParameter} proto. The resulting proto is the
     * format which we send the current params to Assistant (via. app actions context).
     */
    @NonNull
    public abstract Function<? super PropertyT, Optional<IntentParameter>> paramGetter();

    /**
     * Populates the {@code ArgumentBuilderT} for this param with the {@code ParamValue} sent from
     * Assistant in Fulfillment.
     */
    @NonNull
    public abstract ArgumentSetter<ArgumentBuilderT> argumentSetter();

    /**
     * Givne a {@code List<ParamValue>}, convert it to user-visible type and set it into
     * ArgumentBuilder.
     *
     * @param <ArgumentBuilderT>
     */
    @FunctionalInterface
    public interface ArgumentSetter<ArgumentBuilderT> {
        void setArgument(@NonNull ArgumentBuilderT builder, @NonNull List<ParamValue> paramValues)
                throws StructConversionException;
    }
}
