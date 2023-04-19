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

import androidx.appactions.builtintypes.experimental.types.Thing;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.protobuf.ListValue;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Builder for {@link TypeSpec}. */
final class TypeSpecBuilder<T, BuilderT> {
    private final List<FieldBinding<T, BuilderT>> mBindings = new ArrayList<>();
    private final Supplier<BuilderT> mBuilderSupplier;
    private final Function<BuilderT, T> mBuilderFinalizer;
    private CheckedInterfaces.Consumer<Struct> mStructValidator;
    private Function<T, Optional<String>> mIdentifierGetter = (unused) -> Optional.empty();

    private TypeSpecBuilder(
            String typeName,
            Supplier<BuilderT> builderSupplier,
            Function<BuilderT, T> builderFinalizer) {
        this.mBuilderSupplier = builderSupplier;
        this.mBuilderFinalizer = builderFinalizer;
        this.bindStringField("@type", (unused) -> Optional.of(typeName), (builder, val) -> {})
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

    static <T, BuilderT extends BuilderOf<T>> TypeSpecBuilder<T, BuilderT> newBuilder(
            String typeName, Supplier<BuilderT> builderSupplier) {
        return new TypeSpecBuilder<>(typeName, builderSupplier, BuilderT::build);
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
                .bindIdentifier(thing -> Optional.ofNullable(thing.getIdentifier()))
                .bindStringField(
                        "identifier",
                        thing -> Optional.ofNullable(thing.getIdentifier()),
                        BuilderT::setIdentifier)
                .bindStringField(
                        "name",
                        thing ->
                                Optional.ofNullable(thing.getName())
                                        .flatMap(name -> Optional.ofNullable(name.asText())),
                        BuilderT::setName);
    }

    private TypeSpecBuilder<T, BuilderT> setStructValidator(
            CheckedInterfaces.Consumer<Struct> structValidator) {
        this.mStructValidator = structValidator;
        return this;
    }

    TypeSpecBuilder<T, BuilderT> bindIdentifier(Function<T, Optional<String>> identifierGetter) {
        this.mIdentifierGetter = identifierGetter;
        return this;
    }

    private TypeSpecBuilder<T, BuilderT> bindFieldInternal(
            String name,
            Function<T, Optional<Value>> valueGetter,
            CheckedInterfaces.BiConsumer<BuilderT, Optional<Value>> valueSetter) {
        mBindings.add(FieldBinding.create(name, valueGetter, valueSetter));
        return this;
    }

    private <V> TypeSpecBuilder<T, BuilderT> bindRepeatedFieldInternal(
            String name,
            Function<T, List<V>> valueGetter,
            BiConsumer<BuilderT, List<V>> valueSetter,
            Function<V, Optional<Value>> toValue,
            CheckedInterfaces.Function<Value, V> fromValue) {
        return bindFieldInternal(
                name,
                /** valueGetter= */
                object -> {
                    if (valueGetter.apply(object).isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.of(
                            getListValue(
                                    valueGetter.apply(object).stream()
                                            .map(toValue)
                                            .filter(Optional::isPresent)
                                            .map(Optional::get)
                                            .collect(toImmutableList())));
                },
                /** valueSetter= */
                (builder, repeatedValue) -> {
                    List<Value> values =
                            repeatedValue
                                    .map(Value::getListValue)
                                    .map(ListValue::getValuesList)
                                    .orElseGet(Collections::emptyList);
                    List<V> convertedValues = new ArrayList<>();
                    for (Value value : values) {
                        convertedValues.add(fromValue.apply(value));
                    }
                    valueSetter.accept(builder, Collections.unmodifiableList(convertedValues));
                });
    }

    /** binds a String field to read from / write to Struct */
    TypeSpecBuilder<T, BuilderT> bindStringField(
            String name,
            Function<T, Optional<String>> stringGetter,
            BiConsumer<BuilderT, String> stringSetter) {
        return bindFieldInternal(
                name,
                (object) -> stringGetter.apply(object).map(TypeSpecBuilder::getStringValue),
                (builder, value) ->
                        value.map(Value::getStringValue)
                                .ifPresent(
                                        stringValue -> stringSetter.accept(builder, stringValue)));
    }

    /**
     * Binds an enum field to read from / write to Struct. The enum will be represented as a string
     * when converted to a Struct proto.
     */
    <E extends Enum<E>> TypeSpecBuilder<T, BuilderT> bindEnumField(
            String name,
            Function<T, Optional<E>> valueGetter,
            BiConsumer<BuilderT, E> valueSetter,
            Class<E> enumClass) {
        return bindFieldInternal(
                name,
                (object) ->
                        valueGetter
                                .apply(object)
                                .map(Enum::toString)
                                .map(TypeSpecBuilder::getStringValue),
                (builder, value) -> {
                    if (value.isPresent()) {
                        String stringValue = value.get().getStringValue();
                        E[] enumValues = enumClass.getEnumConstants();
                        if (enumValues != null) {
                            for (E enumValue : enumValues) {
                                if (enumValue.toString().equals(stringValue)) {
                                    valueSetter.accept(builder, enumValue);
                                    return;
                                }
                            }
                        }
                        throw new StructConversionException(
                                String.format("Failed to get enum from string %s", stringValue));
                    }
                });
    }

    /**
     * Binds a Duration field to read from / write to Struct. The Duration will be represented as an
     * ISO 8601 string when converted to a Struct proto.
     */
    TypeSpecBuilder<T, BuilderT> bindDurationField(
            String name,
            Function<T, Optional<Duration>> valueGetter,
            BiConsumer<BuilderT, Duration> valueSetter) {
        return bindFieldInternal(
                name,
                (object) ->
                        valueGetter
                                .apply(object)
                                .map(Duration::toString)
                                .map(TypeSpecBuilder::getStringValue),
                (builder, value) -> {
                    if (value.isPresent()) {
                        try {
                            valueSetter.accept(
                                    builder, Duration.parse(value.get().getStringValue()));
                        } catch (DateTimeParseException e) {
                            throw new StructConversionException(
                                    "Failed to parse ISO 8601 string to Duration", e);
                        }
                    }
                });
    }

    /**
     * Binds a ZonedDateTime field to read from / write to Struct. The ZonedDateTime will be
     * represented as an ISO 8601 string when converted to a Struct proto.
     */
    TypeSpecBuilder<T, BuilderT> bindZonedDateTimeField(
            String name,
            Function<T, Optional<ZonedDateTime>> valueGetter,
            BiConsumer<BuilderT, ZonedDateTime> valueSetter) {
        return bindFieldInternal(
                name,
                (object) ->
                        valueGetter
                                .apply(object)
                                .map(ZonedDateTime::toOffsetDateTime)
                                .map(OffsetDateTime::toString)
                                .map(TypeSpecBuilder::getStringValue),
                (builder, value) -> {
                    if (value.isPresent()) {
                        try {
                            valueSetter.accept(
                                    builder, ZonedDateTime.parse(value.get().getStringValue()));
                        } catch (DateTimeParseException e) {
                            throw new StructConversionException(
                                    "Failed to parse ISO 8601 string to ZonedDateTime", e);
                        }
                    }
                });
    }

    /** Binds a spec field to read from / write to Struct. */
    <V> TypeSpecBuilder<T, BuilderT> bindSpecField(
            String name,
            Function<T, Optional<V>> valueGetter,
            BiConsumer<BuilderT, V> valueSetter,
            TypeSpec<V> spec) {
        return bindFieldInternal(
                name,
                (object) ->
                        valueGetter
                                .apply(object)
                                .map(Function.identity()) // Static analyzer incorrectly
                                // throws error stating that the
                                // input to toStruct is nullable. This is a workaround to avoid
                                // the error from the analyzer.
                                .map(spec::toValue),
                (builder, value) -> {
                    if (value.isPresent()) {
                        valueSetter.accept(builder, spec.fromValue(value.get()));
                    }
                });
    }

    /** binds a repeated spec field to read from / write to Struct. */
    <V> TypeSpecBuilder<T, BuilderT> bindRepeatedSpecField(
            String name,
            Function<T, List<V>> valueGetter,
            BiConsumer<BuilderT, List<V>> valueSetter,
            TypeSpec<V> spec) {
        return bindRepeatedFieldInternal(
                name,
                valueGetter,
                valueSetter,
                (element) -> Optional.ofNullable(element).map(spec::toValue),
                (value) -> spec.fromValue(value));
    }

    TypeSpec<T> build() {
        return new TypeSpecImpl<T, BuilderT>(
                mIdentifierGetter,
                mBindings,
                mBuilderSupplier,
                mBuilderFinalizer,
                Optional.ofNullable(mStructValidator));
    }
}
