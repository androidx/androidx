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

package androidx.appactions.interaction.capabilities.core.impl.converters;

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appactions.builtintypes.experimental.types.Thing;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.protobuf.ListValue;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder for {@link TypeSpec}. TypeSpec converts T instance to and from {@code Value.structValue}
 * @param <T> the type this TypeSpec is for
 * @param <BuilderT> the type that builds T objects
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TypeSpecBuilder<T, BuilderT> {
    private final List<FieldBinding<T, BuilderT>> mBindings = new ArrayList<>();
    private final Supplier<BuilderT> mBuilderSupplier;
    private final Function<BuilderT, T> mBuilderFinalizer;
    private CheckedInterfaces.Consumer<Struct> mStructValidator;
    private Function<T, String> mIdentifierGetter = (unused) -> null;

    private TypeSpecBuilder(
            String typeName,
            Supplier<BuilderT> builderSupplier,
            Function<BuilderT, T> builderFinalizer) {
        this.mBuilderSupplier = builderSupplier;
        this.mBuilderFinalizer = builderFinalizer;
        this.bindStringField("@type", (unused) -> typeName, (builder, val) -> {})
                .setStructValidator(
                        struct -> {
                            if (!getFieldFromStruct(struct, "@type")
                                    .getStringValue()
                                    .equals(typeName)) {
                                throw new StructConversionException(
                                        String.format(
                                                "Struct @type field must be equal to %s.",
                                                typeName));
                            }
                        });
    }

    private static Value getStringValue(String string) {
        return Value.newBuilder().setStringValue(string).build();
    }

    private static Value getListValue(List<Value> values) {
        return Value.newBuilder()
                .setListValue(ListValue.newBuilder().addAllValues(values).build())
                .build();
    }

    /**
     * Returns a Value in a Struct, IllegalArgumentException is caught and wrapped in
     * StructConversionException.
     *
     * @param struct the Struct to get values from.
     * @param key    the String key of the field to retrieve.
     */
    private static Value getFieldFromStruct(Struct struct, String key)
            throws StructConversionException {
        try {
            return struct.getFieldsOrThrow(key);
        } catch (IllegalArgumentException e) {
            throw new StructConversionException(
                    String.format("%s does not exist in Struct", key), e);
        }
    }

    /**
     * Creates a new instance of TypeSpecBuilder.
     *
     * @param typeName the name of the type.
     * @param builderSupplier a function which supplies new Builder instances for the type.
     * @param builderFinalizer a function that gets the built object from the builder.
     */
    @NonNull
    public static <T, BuilderT> TypeSpecBuilder<T, BuilderT> newBuilder(
            @NonNull String typeName,
            @NonNull Supplier<BuilderT> builderSupplier,
            @NonNull Function<BuilderT, T> builderFinalizer) {
        return new TypeSpecBuilder<>(typeName, builderSupplier, builderFinalizer);
    }

    /**
     * Creates a new TypeSpecBuilder for a child class of Thing (temporary BuiltInTypes).
     *
     * <p>Comes with bindings for Thing fields.
     */
    static <T extends Thing, BuilderT extends Thing.Builder<?>>
            TypeSpecBuilder<T, BuilderT> newBuilderForThing(
                    String typeName,
                    Supplier<BuilderT> builderSupplier,
                    Function<BuilderT, T> builderFinalizer) {
        return new TypeSpecBuilder<>(typeName, builderSupplier, builderFinalizer)
                .bindIdentifier(Thing::getIdentifier)
                .bindStringField("identifier", Thing::getIdentifier, BuilderT::setIdentifier)
                .bindStringField(
                        "name",
                        thing -> {
                            if (thing.getName() == null) {
                                return null;
                            }
                            return thing.getName().asText();
                        },
                        BuilderT::setName);
    }

    private TypeSpecBuilder<T, BuilderT> setStructValidator(
            CheckedInterfaces.Consumer<Struct> structValidator) {
        this.mStructValidator = structValidator;
        return this;
    }

    /**
     * Binds a function that returns the identifier of the object.
     */
    @NonNull
    public TypeSpecBuilder<T, BuilderT> bindIdentifier(
            @NonNull Function<T, String> identifierGetter
    ) {
        this.mIdentifierGetter = identifierGetter;
        return this;
    }

    private TypeSpecBuilder<T, BuilderT> bindFieldInternal(
            String name,
            Function<T, Value> valueGetter,
            CheckedInterfaces.BiConsumer<BuilderT, Value> valueSetter) {
        mBindings.add(FieldBinding.create(name, valueGetter, valueSetter));
        return this;
    }

    private <V> TypeSpecBuilder<T, BuilderT> bindRepeatedFieldInternal(
            String name,
            Function<T, List<V>> valueGetter,
            BiConsumer<BuilderT, List<V>> valueSetter,
            Function<V, Value> toValue,
            CheckedInterfaces.Function<Value, V> fromValue) {
        return bindFieldInternal(
                name,
                /** valueGetter= */
                object -> {
                    List<V> valueList = valueGetter.apply(object);
                    if (valueList == null) {
                        return null;
                    }
                    return getListValue(
                            valueList.stream()
                                    .map(toValue)
                                    .filter(Objects::nonNull)
                                    .collect(toImmutableList()));
                },
                /** valueSetter= */
                (builder, repeatedValue) -> {
                    if (repeatedValue.getListValue() == null) {
                        return;
                    }
                    List<Value> values = repeatedValue.getListValue().getValuesList();
                    List<V> convertedValues = new ArrayList<>();
                    for (Value value : values) {
                        convertedValues.add(fromValue.apply(value));
                    }
                    valueSetter.accept(builder, Collections.unmodifiableList(convertedValues));
                });
    }

    /** binds a String field to read from / write to Struct */
    @NonNull
    public TypeSpecBuilder<T, BuilderT> bindStringField(
            @NonNull String name,
            @NonNull Function<T, String> stringGetter,
            @NonNull BiConsumer<BuilderT, String> stringSetter) {
        return bindFieldInternal(
                name,
                (object) -> {
                    String value = stringGetter.apply(object);
                    if (value == null) {
                        return null;
                    }
                    return TypeSpecBuilder.getStringValue(value);
                },
                (builder, value) -> {
                    if (value.hasStringValue()) {
                        stringSetter.accept(builder, value.getStringValue());
                    }
                });
    }

    /** Binds a spec field to read from / write to Struct. */
    @SuppressWarnings("LambdaLast")
    @NonNull
    public <V> TypeSpecBuilder<T, BuilderT> bindSpecField(
            @NonNull String name,
            @NonNull Function<T, V> valueGetter,
            @NonNull BiConsumer<BuilderT, V> valueSetter,
            @NonNull TypeSpec<V> spec) {
        return bindFieldInternal(
                name,
                (object) -> {
                    V value = valueGetter.apply(object);
                    if (value == null) {
                        return null;
                    }
                    return spec.toValue(value);
                },
                (builder, value) -> valueSetter.accept(builder, spec.fromValue(value)));
    }

    /** binds a repeated spec field to read from / write to Struct. */
    @SuppressWarnings("LambdaLast")
    @NonNull
    public <V> TypeSpecBuilder<T, BuilderT> bindRepeatedSpecField(
            @NonNull String name,
            @NonNull Function<T, List<V>> valueGetter,
            @NonNull BiConsumer<BuilderT, List<V>> valueSetter,
            @NonNull TypeSpec<V> spec) {
        return bindRepeatedFieldInternal(
                name,
                valueGetter,
                valueSetter,
                spec::toValue,
                spec::fromValue);
    }

    /**
     * Builds the TypeSpec instance.
     */
    @NonNull
    public TypeSpec<T> build() {
        return new TypeSpecImpl<>(
                mIdentifierGetter,
                mBindings,
                mBuilderSupplier,
                mBuilderFinalizer,
                mStructValidator);
    }
}
