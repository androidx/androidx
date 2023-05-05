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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TypeSpecImpl is used to convert between java/kotlin objects in capabilities/values and Value
 * proto.
 */
final class TypeSpecImpl<T, BuilderT> implements TypeSpec<T> {
    /* The function to retrieve the identifier. */
    final Function<T, Optional<String>> mIdentifierGetter;

    /** The list of FieldBinding objects. */
    final List<FieldBinding<T, BuilderT>> mBindings;

    /** Validates the Struct during conversion to java object. */
    final Optional<CheckedInterfaces.Consumer<Struct>> mStructValidator;

    /** Supplies BuilderT instances. */
    final Supplier<BuilderT> mBuilderSupplier;

    /** Builds the object instance. */
    final Function<BuilderT, T> mBuilderFinalizer;

    TypeSpecImpl(
            Function<T, Optional<String>> identifierGetter,
            List<FieldBinding<T, BuilderT>> bindings,
            Supplier<BuilderT> builderSupplier,
            Function<BuilderT, T> builderFinalizer,
            Optional<CheckedInterfaces.Consumer<Struct>> structValidator) {
        this.mIdentifierGetter = identifierGetter;
        this.mBindings = Collections.unmodifiableList(bindings);
        this.mBuilderSupplier = builderSupplier;
        this.mStructValidator = structValidator;
        this.mBuilderFinalizer = builderFinalizer;
    }

    @Nullable
    @Override
    public String getIdentifier(T obj) {
        return mIdentifierGetter.apply(obj).orElse(null);
    }

    /** Converts a java object into a Struct proto using List of FieldBinding. */
    @NonNull
    @Override
    public Value toValue(@NonNull T obj) {
        Struct.Builder structBuilder = Struct.newBuilder();
        for (FieldBinding<T, BuilderT> binding : mBindings) {
            binding.getValueGetter()
                    .apply(obj)
                    .ifPresent(value -> structBuilder.putFields(binding.getName(), value));
        }
        return Value.newBuilder().setStructValue(structBuilder).build();
    }

    /**
     * Converts a Struct back into java object.
     *
     * @throws StructConversionException if the Struct is malformed.
     */
    @NonNull
    @Override
    public T fromValue(@NonNull Value value) throws StructConversionException {
        Struct struct = value.getStructValue();
        if (struct == null) {
            throw new StructConversionException(
                    String.format("TypeSpecImpl cannot deserializes non-Struct value: %s", value));
        }
        if (mStructValidator.isPresent()) {
            mStructValidator.get().accept(struct);
        }

        BuilderT builder = mBuilderSupplier.get();
        Map<String, Value> fieldsMap = struct.getFieldsMap();
        for (FieldBinding<T, BuilderT> binding : mBindings) {
            Optional<Value> fieldValue = Optional.ofNullable(fieldsMap.get(binding.getName()));
            binding.getValueSetter().accept(builder, fieldValue);
        }
        return mBuilderFinalizer.apply(builder);
    }
}
