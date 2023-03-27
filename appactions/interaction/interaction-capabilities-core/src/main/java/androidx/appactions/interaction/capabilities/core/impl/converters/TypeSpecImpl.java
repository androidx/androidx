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
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** TypeSpecImpl is used to convert between java objects in capabilities/values and Struct proto. */
final class TypeSpecImpl<T, BuilderT extends BuilderOf<T>> implements TypeSpec<T> {

    /** The list of FieldBinding objects. */
    final List<FieldBinding<T, BuilderT>> mBindings;

    /** Validates the Struct during conversion to java object. */
    final Optional<CheckedInterfaces.Consumer<Struct>> mStructValidator;

    /** Supplies BuilderT instances. */
    final Supplier<BuilderT> mBuilderSupplier;

    TypeSpecImpl(
            List<FieldBinding<T, BuilderT>> bindings,
            Supplier<BuilderT> builderSupplier,
            Optional<CheckedInterfaces.Consumer<Struct>> structValidator) {
        this.mBindings = Collections.unmodifiableList(bindings);
        this.mBuilderSupplier = builderSupplier;
        this.mStructValidator = structValidator;
    }

    /** Converts a java object into a Struct proto using List of FieldBinding. */
    @NonNull
    @Override
    public Struct toStruct(@NonNull T object) {
        Struct.Builder builder = Struct.newBuilder();
        for (FieldBinding<T, BuilderT> binding : mBindings) {
            binding
                    .valueGetter()
                    .apply(object)
                    .ifPresent(value -> builder.putFields(binding.name(), value));
        }
        return builder.build();
    }

    /**
     * Converts a Struct back into java object.
     *
     * @throws StructConversionException if the Struct is malformed.
     */
    @NonNull
    @Override
    public T fromStruct(@NonNull Struct struct) throws StructConversionException {
        if (mStructValidator.isPresent()) {
            mStructValidator.get().accept(struct);
        }

        BuilderT builder = mBuilderSupplier.get();
        Map<String, Value> fieldsMap = struct.getFieldsMap();
        for (FieldBinding<T, BuilderT> binding : mBindings) {
            Optional<Value> value = Optional.ofNullable(fieldsMap.get(binding.name()));
            binding.valueSetter().accept(builder, value);
        }
        return builder.build();
    }
}
