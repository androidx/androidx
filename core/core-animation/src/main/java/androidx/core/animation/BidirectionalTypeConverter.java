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
 * Abstract base class used convert type T to another type V and back again. This
 * is necessary when the value types of in animation are different from the property
 * type. BidirectionalTypeConverter is needed when only the final value for the
 * animation is supplied to animators.
 *
 * @param <T> type T. It can be converted from and to type V
 * @param <V> type V. It can be converted from and to type T
 * @see PropertyValuesHolder#setConverter(TypeConverter)
 */
public abstract class BidirectionalTypeConverter<T, V> extends TypeConverter<T, V> {
    private BidirectionalTypeConverter<V, T> mInvertedConverter;

    /**
     * Constructor for creating a bidirectional type converter instance that can convert type T to
     * another type V and back again.
     *
     * @param fromClass class of type T
     * @param toClass class of type V
     */
    public BidirectionalTypeConverter(@NonNull Class<T> fromClass, @NonNull Class<V> toClass) {
        super(fromClass, toClass);
    }

    /**
     * Does a conversion from the target type back to the source type. The subclass
     * must implement this when a TypeConverter is used in animations and current
     * values will need to be read for an animation.
     * @param value The Object to convert.
     * @return A value of type T, converted from <code>value</code>.
     */
    @NonNull
    public abstract T convertBack(@NonNull V value);

    /**
     * Returns the inverse of this converter, where the from and to classes are reversed.
     * The inverted converter uses this convert to call {@link #convertBack(Object)} for
     * {@link #convert(Object)} calls and {@link #convert(Object)} for
     * {@link #convertBack(Object)} calls.
     * @return The inverse of this converter, where the from and to classes are reversed.
     */
    @NonNull
    public BidirectionalTypeConverter<V, T> invert() {
        if (mInvertedConverter == null) {
            mInvertedConverter = new InvertedConverter<>(this);
        }
        return mInvertedConverter;
    }

    private static class InvertedConverter<From, To> extends BidirectionalTypeConverter<From, To> {
        private BidirectionalTypeConverter<To, From> mConverter;

        InvertedConverter(@NonNull BidirectionalTypeConverter<To, From> converter) {
            super(converter.getTargetType(), converter.getSourceType());
            mConverter = converter;
        }

        @NonNull
        @Override
        public From convertBack(@NonNull To value) {
            return mConverter.convert(value);
        }

        @NonNull
        @Override
        public To convert(@NonNull From value) {
            return mConverter.convertBack(value);
        }
    }
}
