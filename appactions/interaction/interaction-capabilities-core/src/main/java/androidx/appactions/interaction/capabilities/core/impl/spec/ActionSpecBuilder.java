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
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.PropertyConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.impl.spec.ParamBinding.ArgumentSetter;
import androidx.appactions.interaction.capabilities.core.properties.EntityProperty;
import androidx.appactions.interaction.capabilities.core.properties.EnumProperty;
import androidx.appactions.interaction.capabilities.core.properties.IntegerProperty;
import androidx.appactions.interaction.capabilities.core.properties.SimpleProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringOrEnumProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;
import androidx.appactions.interaction.capabilities.core.values.EntityValue;
import androidx.appactions.interaction.capabilities.core.values.StringOrEnumValue;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindParameter(
            @NonNull String paramName,
            @NonNull Function<? super PropertyT, Optional<IntentParameter>> paramGetter,
            @NonNull ArgumentSetter<ArgumentBuilderT> argumentSetter) {
        mParamBindingList.add(ParamBinding.create(paramName, paramGetter, argumentSetter));
        return this;
    }

    /**
     * Binds the parameter name, getter, and setter for a {@link EntityProperty}.
     *
     * <p>This parameter is required for any capability built from the generated {@link ActionSpec}.
     *
     * @param paramName the name of this action' parameter.
     * @param propertyGetter a getter of the EntityProperty from the property, which must be able to
     *     fetch a non-null {@code EntityProperty} from {@code PropertyT}.
     * @param paramConsumer a setter to set the string value in the argument builder.
     * @return the builder itself.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindRequiredEntityParameter(
                    @NonNull String paramName,
                    @NonNull Function<? super PropertyT, EntityProperty> propertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, EntityValue> paramConsumer) {
        return bindEntityParameter(
                paramName,
                property ->
                        Optional.of(
                                PropertyConverter.getIntentParameter(
                                        paramName, propertyGetter.apply(property))),
                paramConsumer);
    }

    /**
     * Binds the parameter name, getter, and setter for a {@link EntityProperty}.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action' parameter.
     * @param optionalPropertyGetter an optional getter of the EntityProperty from the property,
     *     which may be able to fetch a non-null {@code EntityProperty} from {@code PropertyT}, or
     *     get {@link Optional#empty}.
     * @param paramConsumer a setter to set the string value in the argument builder.
     * @return the builder itself.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindOptionalEntityParameter(
                    @NonNull String paramName,
                    @NonNull
                            Function<? super PropertyT, Optional<EntityProperty>>
                                    optionalPropertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, EntityValue> paramConsumer) {
        return bindEntityParameter(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(p -> PropertyConverter.getIntentParameter(paramName, p)),
                paramConsumer);
    }

    /**
     * This is similar to {@link ActionSpectBuilder#bindOptionalEntityParameter} but for setting a
     * list of entities instead.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindRepeatedEntityParameter(
                    @NonNull String paramName,
                    @NonNull
                            Function<? super PropertyT, Optional<EntityProperty>>
                                    optionalPropertyGetter,
                    @NonNull
                            BiConsumer<? super ArgumentBuilderT, List<EntityValue>> paramConsumer) {
        return bindParameter(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(p -> PropertyConverter.getIntentParameter(paramName, p)),
                (argBuilder, paramList) ->
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofRepeated(TypeConverters::toEntityValue)
                                        .convert(paramList)));
    }

    private ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindEntityParameter(
            String paramName,
            Function<? super PropertyT, Optional<IntentParameter>> propertyGetter,
            BiConsumer<? super ArgumentBuilderT, EntityValue> paramConsumer) {
        return bindParameter(
                paramName,
                propertyGetter,
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofSingular(TypeConverters::toEntityValue)
                                        .convert(paramList));
                    }
                });
    }

    @NonNull
    public <T>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindStructParameter(
                    @NonNull String paramName,
                    @NonNull
                            Function<? super PropertyT, Optional<SimpleProperty>>
                                    optionalPropertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, T> paramConsumer,
                    @NonNull ParamValueConverter<T> paramValueConverter) {
        return bindParameter(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(p -> PropertyConverter.getIntentParameter(paramName, p)),
                (argBuilder, paramList) ->
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofSingular(paramValueConverter)
                                        .convert(paramList)));
    }

    @NonNull
    public <T>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
                    bindRepeatedStructParameter(
                            @NonNull String paramName,
                            @NonNull
                                    Function<? super PropertyT, Optional<SimpleProperty>>
                                            optionalPropertyGetter,
                            @NonNull BiConsumer<? super ArgumentBuilderT, List<T>> paramConsumer,
                            @NonNull ParamValueConverter<T> paramValueConverter) {
        return bindParameter(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(p -> PropertyConverter.getIntentParameter(paramName, p)),
                (argBuilder, paramList) ->
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofRepeated(paramValueConverter)
                                        .convert(paramList)));
    }

    /**
     * Binds an optional string parameter.
     *
     * @param paramName the BII slot name of this parameter.
     * @param propertyGetter a function that returns a {@code Optional<StringProperty>} given a
     *     {@code PropertyT} instance
     * @param paramConsumer a function that accepts a String into the argument builder.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindOptionalStringParameter(
                    @NonNull String paramName,
                    @NonNull Function<? super PropertyT, Optional<StringProperty>> propertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, String> paramConsumer) {
        return bindParameter(
                paramName,
                property ->
                        propertyGetter
                                .apply(property)
                                .map(
                                        stringProperty ->
                                                PropertyConverter.getIntentParameter(
                                                        paramName, stringProperty)),
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofSingular(TypeConverters::toStringValue)
                                        .convert(paramList));
                    }
                });
    }

    /**
     * Binds an required string parameter.
     *
     * @param paramName the BII slot name of this parameter.
     * @param propertyGetter a function that returns a {@code StringProperty} given a {@code
     *     PropertyT} instance
     * @param paramConsumer a function that accepts a String into the argument builder.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindRequiredStringParameter(
                    @NonNull String paramName,
                    @NonNull Function<? super PropertyT, StringProperty> propertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, String> paramConsumer) {
        return bindOptionalStringParameter(
                paramName, property -> Optional.of(propertyGetter.apply(property)), paramConsumer);
    }

    /**
     * Binds an repeated string parameter.
     *
     * @param paramName the BII slot name of this parameter.
     * @param propertyGetter a function that returns a {@code Optional<StringProperty>} given a
     *     {@code PropertyT} instance
     * @param paramConsumer a function that accepts a {@code List<String>} into the argument
     *     builder.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindRepeatedStringParameter(
                    @NonNull String paramName,
                    @NonNull Function<? super PropertyT, Optional<StringProperty>> propertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, List<String>> paramConsumer) {
        return bindParameter(
                paramName,
                property ->
                        propertyGetter
                                .apply(property)
                                .map(
                                        stringProperty ->
                                                PropertyConverter.getIntentParameter(
                                                        paramName, stringProperty)),
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofRepeated(TypeConverters::toStringValue)
                                        .convert(paramList));
                    }
                });
    }

    /**
     * Binds the parameter name, getter, and setter for a {@link EnumProperty}.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action parameter.
     * @param enumType
     * @param optionalPropertyGetter an optional getter of the EntityProperty from the property,
     *     which may be able to fetch a non-null {@code EnumProperty} from {@code PropertyT}, or get
     *     {@link Optional#empty}.
     * @param paramConsumer a setter to set the enum value in the argument builder.
     * @return the builder itself.
     * @param <EnumT>
     */
    @NonNull
    public <EnumT extends Enum<EnumT>>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
                    bindOptionalEnumParameter(
                            @NonNull String paramName,
                            @NonNull Class<EnumT> enumType,
                            @NonNull
                                    Function<? super PropertyT, Optional<EnumProperty<EnumT>>>
                                            optionalPropertyGetter,
                            @NonNull BiConsumer<? super ArgumentBuilderT, EnumT> paramConsumer) {
        return bindEnumParameter(
                paramName,
                enumType,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(e -> PropertyConverter.getIntentParameter(paramName, e)),
                paramConsumer);
    }

    private <EnumT extends Enum<EnumT>>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT> bindEnumParameter(
                    String paramName,
                    Class<EnumT> enumType,
                    Function<? super PropertyT, Optional<IntentParameter>> enumParamGetter,
                    BiConsumer<? super ArgumentBuilderT, EnumT> paramConsumer) {
        return bindParameter(
                paramName,
                enumParamGetter,
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        Optional<EnumT> enumValue =
                                EnumSet.allOf(enumType).stream()
                                        .filter(
                                                element ->
                                                        element.toString()
                                                                .equals(
                                                                        paramList
                                                                                .get(0)
                                                                                .getIdentifier()))
                                        .findFirst();
                        if (enumValue.isPresent()) {
                            paramConsumer.accept(argBuilder, enumValue.get());
                        }
                    }
                });
    }

    @NonNull
    public <EnumT extends Enum<EnumT>>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
                    bindRequiredStringOrEnumParameter(
                            @NonNull String paramName,
                            @NonNull Class<EnumT> enumType,
                            @NonNull
                                    Function<? super PropertyT, StringOrEnumProperty<EnumT>>
                                            propertyGetter,
                            @NonNull
                                    BiConsumer<? super ArgumentBuilderT, StringOrEnumValue<EnumT>>
                                            paramConsumer) {
        return bindStringOrEnumParameter(
                paramName,
                enumType,
                property ->
                        Optional.of(
                                PropertyConverter.getIntentParameter(
                                        paramName, propertyGetter.apply(property))),
                paramConsumer);
    }

    private <EnumT extends Enum<EnumT>>
            ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
                    bindStringOrEnumParameter(
                            String paramName,
                            Class<EnumT> enumType,
                            Function<? super PropertyT, Optional<IntentParameter>> propertyGetter,
                            BiConsumer<? super ArgumentBuilderT, StringOrEnumValue<EnumT>>
                                    paramConsumer) {
        return bindParameter(
                paramName,
                propertyGetter,
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        ParamValue param = paramList.get(0);
                        if (param.hasIdentifier()) {
                            Optional<EnumT> enumValue =
                                    EnumSet.allOf(enumType).stream()
                                            .filter(
                                                    element ->
                                                            element.toString()
                                                                    .equals(param.getIdentifier()))
                                            .findFirst();
                            if (enumValue.isPresent()) {
                                paramConsumer.accept(
                                        argBuilder, StringOrEnumValue.ofEnumValue(enumValue.get()));
                                return;
                            }
                        }
                        paramConsumer.accept(
                                argBuilder,
                                StringOrEnumValue.ofStringValue(
                                        SlotTypeConverter.ofSingular(TypeConverters::toStringValue)
                                                .convert(paramList)));
                    }
                });
    }

    /**
     * Binds the integer parameter name and setter for a {@link IntegerProperty}.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action' parameter.
     * @param optionalPropertyGetter
     * @param paramConsumer a setter to set the int value in the argument builder.
     * @return the builder itself.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindOptionalIntegerParameter(
                    @NonNull String paramName,
                    @NonNull
                            Function<? super PropertyT, Optional<IntegerProperty>>
                                    optionalPropertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, Integer> paramConsumer) {
        return bindParameter(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(e -> PropertyConverter.getIntentParameter(paramName, e)),
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofSingular(TypeConverters::toIntegerValue)
                                        .convert(paramList));
                    }
                });
    }

    /**
     * Binds a Boolean parameter.
     *
     * <p>This parameter is optional for any capability built from the generated {@link ActionSpec}.
     * If the Property Optional is not set, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action' parameter.
     * @param paramConsumer a setter to set the boolean value in the argument builder.
     * @param optionalPropertyGetter an optional getter of the EntityProperty from the property,
     *     which may be able to fetch a non-null {@code SimpleProperty} from {@code PropertyT}, or
     *     get {@link Optional#empty}.
     * @return the builder itself.
     */
    @NonNull
    public ActionSpecBuilder<PropertyT, ArgumentT, ArgumentBuilderT, OutputT>
            bindOptionalBooleanParameter(
                    @NonNull String paramName,
                    @NonNull
                            Function<? super PropertyT, Optional<SimpleProperty>>
                                    optionalPropertyGetter,
                    @NonNull BiConsumer<? super ArgumentBuilderT, Boolean> paramConsumer) {
        return bindParameter(
                paramName,
                property ->
                        optionalPropertyGetter
                                .apply(property)
                                .map(e -> PropertyConverter.getIntentParameter(paramName, e)),
                (argBuilder, paramList) -> {
                    if (!paramList.isEmpty()) {
                        paramConsumer.accept(
                                argBuilder,
                                SlotTypeConverter.ofSingular(TypeConverters::toBooleanValue)
                                        .convert(paramList));
                    }
                });
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
                output ->
                        outputGetter.apply(output).stream()
                                .map(converter)
                                .collect(toImmutableList()));
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
