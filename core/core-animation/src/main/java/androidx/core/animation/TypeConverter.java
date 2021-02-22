/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.animation;

import androidx.annotation.NonNull;

/**
 * Abstract base class used convert type T to another type V. This
 * is necessary when the value types of in animation are different
 * from the property type.
 *
 * @param <T> type T. It can be converted to type V
 * @param <V> type V. It can be converted from type T
 * @see PropertyValuesHolder#setConverter(TypeConverter)
 */
public abstract class TypeConverter<T, V> {
    private final Class<T> mFromClass;
    private final Class<V> mToClass;

    /**
     * Constructor for creating a type converter instance that converts type T to another type V.
     *
     * @param fromClass class of the value type that feeds into the converter
     * @param toClass class of the value type expected out of the converter
     */
    public TypeConverter(@NonNull Class<T> fromClass, @NonNull Class<V> toClass) {
        mFromClass = fromClass;
        mToClass = toClass;
    }

    /**
     * Returns the target converted type. Used by the animation system to determine
     * the proper setter function to call.
     * @return The Class to convert the input to.
     */
    @NonNull Class<V> getTargetType() {
        return mToClass;
    }

    /**
     * Returns the source conversion type.
     */
    @NonNull Class<T> getSourceType() {
        return mFromClass;
    }

    /**
     * Converts a value from one type to another.
     * @param value The Object to convert.
     * @return A value of type V, converted from <code>value</code>.
     */
    public abstract @NonNull V convert(@NonNull T value);
}
