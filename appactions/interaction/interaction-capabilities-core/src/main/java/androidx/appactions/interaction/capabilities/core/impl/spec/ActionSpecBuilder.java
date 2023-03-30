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

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.PropertyConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter;
import androidx.appactions.interaction.capabilities.core.impl.spec.ParamBinding.ArgumentSetter;
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A builder for the {@code ActionSpec}.
 *
 * @param <PropertyT>
 * @param <ArgumentT>
 * @param <ArgumentBuilderT>
 * @param <OutputT>
 */
public final class ActionSpecBuilder<
        PropertyT, ArgumentT, ArgumentBuilderT extends BuilderOf<ArgumentT>, OutputT> {

    private final String mCapabilityName;
    private final Supplier<ArgumentBuilderT> mArgumentBuilderSupplier;
    private final ArrayList<ParamBinding<PropertyT, ArgumentT, ArgumentBuilderT>>
            mParamBindingList = new ArrayList<>();
    private final Map<String, Function<OutputT, List<ParamValue>>> mOutputBindings =
            new HashMap<>();

    private ActionSpecBuilder(
            String capabilityName, Supplier<ArgumentBuilderT> argumentBuilderSupplier) {
        this.mCapabilityName = capabilityName;
        this.mArgumentBuilderSupplier = argumentBuilderSupplier;
    }

    /**
     * Creates an empty {@code ActionSpecBuilder} with the given capability name. ArgumentT is set
     * to Object as a placeholder, which must be replaced by calling setArgument.
     */
    @NonNull
    public static ActionSpecBuilder<Void, Object, BuilderOf<Object>, Void> ofCapabilityNamed(
            @NonNull String capabilityName) {
        return new ActionSpecBuilder<>(capabilityName, () -> Object::new);
    }

    /** Sets the property type and returns a new {@code ActionSpecBuilder}. */
    @NonNull
    public <NewPropertyT>
            ActionSpecBuilder<NewPropertyT, ArgumentT, ArgumentBuilderT, OutputT> setDescriptor(
                    @NonNull Class<NewPropertyT> unused) {
        return new ActionSpecBuilder<>(this.mCapabilityName, this.mArgumentBuilderSupplier);
    }

    /** Sets the argument type and its builder and returns a new {@code ActionSpecBuilder}. */
    @NonNull
    public <NewArgumentT, NewArgumentBuilderT extends BuilderOf<NewArgumentT>>
            ActionSpecBuilder<PropertyT, NewArgumentT, NewArgumentBuilderT, OutputT> setArgument(
                    @NonNull Class<NewArgumentT> unused,
                    @NonNull Supplier<NewArgumentBuilderT> argumentBuilderSupplier) {
        return new ActionSpecBuilder<>(this.mCapabilityName, argumentBuilderSupplier);
    }

    @NonNull
    public <NewOutputT>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, NewOutputT> setOutput(
                    @NonNull Class<NewOutputT> unused) {
        return new ActionSpecBuilder<>(this.mCapabilityName, this.mArgumentBuilderSupplier);
    }

    /**
     * Binds the parameter name, getter and setter.
     *
     * @param paramName the name of this action' parameter.
     * @param paramGetter a getter of the param-specific info from the property.
     * @param argumentSetter a setter to the argument with the input from {@code ParamValue}.
     * @return the builder itself.
     */
    @NonNull
    private ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindParameterInternal(
                    @NonNull String paramName,
                    @NonNull Function<? super PropertyT, Optional<IntentParameter>> paramGetter,
                    @NonNull ArgumentSetter<ArgumentBuilderT> argumentSetter) {
        mParamBindingList.add(ParamBinding.create(paramName, paramGetter, argumentSetter));
        return this;
    }

    /**
     * Binds the parameter name, getter, and setter for a {@link TypeProperty}.
     *
     * <p>This parameter is required for any capability built from the generated {@link ActionSpec}.
     *
     * @param paramName the name of this action' parameter.
     * @param propertyGetter a getter of the TypeProperty from the property, which must be able to
     *     fetch a non-null {@code TypeProperty} from {@code PropertyT}.
     * @param paramConsumer a setter to set the string value in the argument builder.
     * @param paramValueConverter converter FROM assistant ParamValue proto
     * @param entityConverter converter TO assistant Entity proto
     * @return the builder itself.
     */
    @NonNull
    public <T, PossibleValueT>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindParameter(
                    @NonNull String paramName,
                    @NonNull
                            Function<? super PropertyT, TypeProperty<PossibleValueT>>
                                    propertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, T> paramConsumer,
                    @NonNull ParamValueConverter<T> paramValueConverter,
                    @NonNull EntityConverter<PossibleValueT> entityConverter) {
        return bindOptionalParameter(
                paramName,
                property -> Optional.of(propertyGetter.apply(property)),
                paramConsumer,
                paramValueConverter,
                entityConverter);
    }

    /**
     * Binds the parameter name, getter, and setter for a {@link TypeProperty}.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action' parameter.
     * @param optionalPropertyGetter an optional getter of the TypeProperty from the property, which
     *     may be able to fetch a non-null {@code TypeProperty} from {@code PropertyT}, or get
     *     {@link Optional#empty}.
     * @param paramConsumer a setter to set the string value in the argument builder.
     * @param paramValueConverter converter FROM assistant ParamValue proto
     * @param entityConverter converter TO assistant Entity proto
     * @return the builder itself.
     */
    @NonNull
    public <T, PossibleValueT>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
                    bindOptionalParameter(
                            @NonNull String paramName,
                            @NonNull
                                    Function<
                                                    ? super PropertyT,
                                                    Optional<TypeProperty<PossibleValueT>>>
                                            optionalPropertyGetter,
                            @NonNull BiConsumer<? super ArgumentBuilderT, T> paramConsumer,
                            @NonNull ParamValueConverter<T> paramValueConverter,
                            @NonNull EntityConverter<PossibleValueT> entityConverter) {
        return bindParameterInternal(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(
                                        p ->
                                                PropertyConverter.getIntentParameter(
                                                        paramName, p, entityConverter)),
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofSingular(paramValueConverter)
                                        .convert(paramList));
                    }
                });
    }

    /**
     * This is similar to {@link ActionSpecBuilder#bindOptionalParameter} but for setting a list of
     * entities instead.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     */
    @NonNull
    public <T, PossibleValueT>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
                    bindRepeatedParameter(
                            @NonNull String paramName,
                            @NonNull
                                    Function<
                                                    ? super PropertyT,
                                                    Optional<TypeProperty<PossibleValueT>>>
                                            optionalPropertyGetter,
                            @NonNull BiConsumer<? super ArgumentBuilderT, List<T>> paramConsumer,
                            @NonNull ParamValueConverter<T> paramValueConverter,
                            @NonNull EntityConverter<PossibleValueT> entityConverter) {
        return bindParameterInternal(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(
                                        p ->
                                                PropertyConverter.getIntentParameter(
                                                        paramName, p, entityConverter)),
                (argBuilder, paramList) ->
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofRepeated(paramValueConverter)
                                        .convert(paramList)));
    }

    /**
     * Binds an optional output.
     *
     * @param name the BII output slot name of this parameter.
     * @param outputGetter a getter of the output from the {@code OutputT} instance.
     * @param converter a converter from an output object to a ParamValue.
     */
    @NonNull
    @SuppressWarnings("JdkCollectors")
    public <T>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindOptionalOutput(
                    @NonNull String name,
                    @NonNull Function<OutputT, Optional<T>> outputGetter,
                    @NonNull Function<T, ParamValue> converter) {
        mOutputBindings.put(
                name,
                output -> {
                    Optional<T> optionalOut = outputGetter.apply(output);
                    List<ParamValue> paramValues = new ArrayList<>();
                    if (optionalOut.isPresent()) {
                        paramValues.add(converter.apply(optionalOut.get()));
                    }
                    return Collections.unmodifiableList(paramValues);
                });
        return this;
    }

    /**
     * Binds a repeated output.
     *
     * @param name the BII output slot name of this parameter.
     * @param outputGetter a getter of the output from the {@code OutputT} instance.
     * @param converter a converter from an output object to a ParamValue.
     */
    @NonNull
    @SuppressWarnings("JdkCollectors")
    public <T>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindRepeatedOutput(
                    @NonNull String name,
                    @NonNull Function<OutputT, List<T>> outputGetter,
                    @NonNull Function<T, ParamValue> converter) {
        mOutputBindings.put(
                name,
                output ->
                        outputGetter.apply(output).stream()
                                .map(converter)
                                .collect(toImmutableList()));
        return this;
    }

    /** Builds an {@code ActionSpec} from this builder. */
    @NonNull
    public ActionSpec<PropertyT, ArgumentT, OutputT> build() {
        return new ActionSpecImpl<>(
                mCapabilityName,
                mArgumentBuilderSupplier,
                Collections.unmodifiableList(mParamBindingList),
                mOutputBindings);
    }
}
