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
import androidx.annotation.RestrictTo;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts from internal proto representation as defined in the AppActionContext to a public java
 * type which can be consumed by developers.
 *
 * @param <T>
 */
@FunctionalInterface
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface SlotTypeConverter<T> {
    @NonNull
    static <T> SlotTypeConverter<List<T>> ofRepeated(
            @NonNull ParamValueConverter<T> singularConverter) {
        return (paramValues) -> {
            List<T> results = new ArrayList<>();
            for (ParamValue paramValue : paramValues) {
                results.add(singularConverter.fromParamValue(paramValue));
            }
            return results;
        };
    }

    /** This converter will throw IndexOutOfBoundsException if the input List is empty. */
    @NonNull
    static <T> SlotTypeConverter<T> ofSingular(
            @NonNull ParamValueConverter<T> singularConverter) {
        return (paramValues) -> singularConverter.fromParamValue(paramValues.get(0));
    }

    T convert(@NonNull List<ParamValue> protoList) throws StructConversionException;
}
