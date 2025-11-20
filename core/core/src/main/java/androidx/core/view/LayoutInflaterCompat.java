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

package androidx.core.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import org.jspecify.annotations.NonNull;

/**
 * Helper for accessing features in {@link LayoutInflater}.
 */
@SuppressWarnings("deprecation")
public final class LayoutInflaterCompat {

    @SuppressWarnings("deprecation")
    static class Factory2Wrapper implements LayoutInflater.Factory2 {
        final LayoutInflaterFactory mDelegateFactory;

        Factory2Wrapper(LayoutInflaterFactory delegateFactory) {
            mDelegateFactory = delegateFactory;
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            return mDelegateFactory.onCreateView(null, name, context, attrs);
        }

        @Override
        public View onCreateView(View parent, String name, Context context,
                AttributeSet attributeSet) {
            return mDelegateFactory.onCreateView(parent, name, context, attributeSet);
        }

        @Override
        public @NonNull String toString() {
            return getClass().getName() + "{" + mDelegateFactory + "}";
        }
    }

    /*
     * Hide the constructor.
     */
    private LayoutInflaterCompat() {
    }

    /**
     * Attach a custom Factory interface for creating views while using
     * this LayoutInflater. This must not be null, and can only be set once;
     * after setting, you can not change the factory.
     *
     * @see LayoutInflater#setFactory(android.view.LayoutInflater.Factory)
     *
     * @deprecated Use {@link #setFactory2(LayoutInflater, LayoutInflater.Factory2)} instead to set
     * and {@link LayoutInflater#getFactory2()} to get the factory.
     */
    @Deprecated
    public static void setFactory(
            @NonNull LayoutInflater inflater, @NonNull LayoutInflaterFactory factory) {
        inflater.setFactory2(new Factory2Wrapper(factory));
    }

    /**
     * Attach a custom {@link LayoutInflater.Factory2} for creating views while using
     * this {@link LayoutInflater}. This must not be null, and can only be set once;
     * after setting, you can not change the factory.
     *
     * @see LayoutInflater#setFactory2(android.view.LayoutInflater.Factory2)
     */
    public static void setFactory2(
            @NonNull LayoutInflater inflater, LayoutInflater.@NonNull Factory2 factory) {
        inflater.setFactory2(factory);
    }

    /**
     * Return the current {@link LayoutInflaterFactory} (or null). This is
     * called on each element name. If the factory returns a View, add that
     * to the hierarchy. If it returns null, proceed to call onCreateView(name).
     *
     * @return The {@link LayoutInflaterFactory} associated with the
     * {@link LayoutInflater}. Will be {@code null} if the inflater does not
     * have a {@link LayoutInflaterFactory} but a raw {@link LayoutInflater.Factory}.
     * @see LayoutInflater#getFactory()
     *
     * @deprecated Use {@link #setFactory2(LayoutInflater, LayoutInflater.Factory2)} to set and
     * {@link LayoutInflater#getFactory2()} to get the factory.
     */
    @Deprecated
    public static LayoutInflaterFactory getFactory(LayoutInflater inflater) {
        LayoutInflater.Factory factory = inflater.getFactory();
        if (factory instanceof Factory2Wrapper) {
            return ((Factory2Wrapper) factory).mDelegateFactory;
        }
        return null;
    }
}
