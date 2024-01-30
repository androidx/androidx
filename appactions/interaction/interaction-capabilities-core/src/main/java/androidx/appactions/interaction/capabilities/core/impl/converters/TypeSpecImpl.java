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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TypeSpecImpl is used to convert between java/kotlin objects in capabilities/values and Value
 * proto.
 */
final class TypeSpecImpl<T, BuilderT> implements TypeSpec<T> {
    /* The function to retrieve the identifier. */
    @NonNull
    final Function<T, String> mIdentifierGetter;

    /** The list of FieldBinding objects. */
    @NonNull
    final List<FieldBinding<T, BuilderT>> mBindings;

    /** Validates the Struct during conversion to java object. */
    @Nullable
    final CheckedInterfaces.Consumer<Struct> mStructValidator;

    /** Supplies BuilderT instances. */
    @NonNull
    final Supplier<BuilderT> mBuilderSupplier;

    /** Builds the object instance. */
    @NonNull
    final Function<BuilderT, T> mBuilderFinalizer;

    TypeSpecImpl(
            @NonNull Function<T, String> identifierGetter,
            @NonNull List<FieldBinding<T, BuilderT>> bindings,
            @NonNull Supplier<BuilderT> builderSupplier,
            @NonNull Function<BuilderT, T> builderFinalizer,
            @Nullable CheckedInterfaces.Consumer<Struct> structValidator) {
        this.mIdentifierGetter = identifierGetter;
        this.mBindings = Collections.unmodifiableList(bindings);
        this.mBuilderSupplier = builderSupplier;
        this.mStructValidator = structValidator;
        this.mBuilderFinalizer = builderFinalizer;
    }

    @Nullable
    @Override
    public String getIdentifier(T obj) {
        return mIdentifierGetter.apply(obj);
    }

    /** Converts a java object into a Struct proto using List of FieldBinding. */
    @NonNull
    @Override
    public Value toValue(@NonNull T obj) {
        Struct.Builder structBuilder = Struct.newBuilder();
        for (FieldBinding<T, BuilderT> binding : mBindings) {
            Value value = binding.getValueGetter().apply(obj);
            if (value != null) {
                structBuilder.putFields(binding.getName(), value);
            }
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
        if (mStructValidator != null) {
            mStructValidator.accept(struct);
        }

        BuilderT builder = mBuilderSupplier.get();
        Map<String, Value> fieldsMap = struct.getFieldsMap();
        for (FieldBinding<T, BuilderT> binding : mBindings) {
            Value fieldValue = fieldsMap.get(binding.getName());
            if (fieldValue != null) {
                binding.getValueSetter().accept(builder, fieldValue);
            }
        }
        return mBuilderFinalizer.apply(builder);
    }
}
