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
 * <code>int</code>. This type-specific subclass enables performance benefit by allowing
 * calls to a {@link #setValue(Object, int) setValue()} function that takes the primitive
 * <code>int</code> type and avoids autoboxing and other overhead associated with the
 * <code>Integer</code> class.
 *
 * @param <T> The class on which the Property is declared.
 */
public abstract class IntProperty<T> extends Property<T, Integer> {

    /**
     * A constructor that takes an identifying name for the int property. This name
     * will show up as a part of {@link ObjectAnimator#getPropertyName()} when the
     * {@link ObjectAnimator} is created with a {@link Property} object as its animation property.
     * This name will also appear in systrace as a part of the animator name.
     *
     * @param name name to be used to identify the property
     * @see #IntProperty()
     */
    public IntProperty(@NonNull String name) {
        super(Integer.class, name);
    }

    /**
     * A constructor that creates an int property instance with an empty name. To create a named
     * int property, see {@link #IntProperty(String)}
     *
     * @see #IntProperty(String)
     */
    public IntProperty() {
        super(Integer.class, "");
    }

    /**
     * A type-specific variant of {@link #set(Object, Integer)} that is faster when dealing
     * with fields of type <code>int</code>.
     */
    public abstract void setValue(@NonNull T object, int value);

    @Override
    public final void set(
            @NonNull T object,
            @SuppressLint("AutoBoxing") /* Generics */ @NonNull Integer value
    ) {
        setValue(object, value);
    }
}
