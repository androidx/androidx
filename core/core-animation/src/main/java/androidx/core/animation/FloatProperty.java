/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.util.Property;

import androidx.annotation.NonNull;

/**
 * An implementation of {@link android.util.Property} to be used specifically with fields of type
 * <code>float</code>. This type-specific subclass enables performance benefit by allowing
 * calls to a {@link #setValue(Object, float) setValue()} function that takes the primitive
 * <code>float</code> type and avoids autoboxing and other overhead associated with the
 * <code>Float</code> class.
 *
 * @param <T> The class on which the Property is declared.
 */
public abstract class FloatProperty<T> extends Property<T, Float> {

    /**
     * A constructor that takes an identifying name for the float property. This name
     * will show up as a part of {@link ObjectAnimator#getPropertyName()} when the
     * {@link ObjectAnimator} is created with a {@link Property} instance as the animation property.
     * This name will also appear in systrace as a part of the animator name.
     *
     * @param name name to be used to identify the property
     * @see #FloatProperty()
     */
    public FloatProperty(@NonNull String name) {
        super(Float.class, name);
    }

    /**
     * A constructor that creates a float property instance with an empty name. To create a named
     * float property, see {@link #FloatProperty(String)}
     * @see #FloatProperty(String)
     */
    public FloatProperty() {
        super(Float.class, "");
    }

    /**
     * A type-specific variant of {@link #set(Object, Float)} that is faster when dealing
     * with fields of type <code>float</code>.
     */
    public abstract void setValue(@NonNull T object, float value);

    @Override
    public final void set(
            @NonNull T object,
            @SuppressLint("AutoBoxing") /* Generics */ @NonNull Float value
    ) {
        setValue(object, value);
    }
}
