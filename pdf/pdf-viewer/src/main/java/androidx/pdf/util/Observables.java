/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.pdf.util.ObservableArray.ArrayObserver;
import androidx.pdf.util.ObservableValue.ValueObserver;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A few ready-to-use {@link Observable}s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Observables {

    private static final String TAG = Observables.class.getSimpleName();

    /**
     * An Observable that accepts multiple observers.
     *
     * @param <O> The type of the observers.
     */
    public static class MultiObservers<O> implements Iterable<O>, Observable<O> {

        private final Set<O> mObservers = new HashSet<O>();

        @NonNull
        @CanIgnoreReturnValue
        @Override
        public Object addObserver(O observer) {
            Preconditions.checkNotNull(observer);
            synchronized (mObservers) {
                Preconditions.checkState(
                        mObservers.add(observer),
                        String.format("Observer %s previously registered.", observer));
            }
            return observer;
        }

        @Override
        public void removeObserver(@NonNull Object observer) {
            synchronized (mObservers) {
                Preconditions.checkArgument(
                        mObservers.remove(observer),
                        String.format("Remove inexistant Observer %s.", observer));
            }
        }

        /** Clears all registered observers. */
        public void cleanup() {
            synchronized (mObservers) {
                mObservers.clear();
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }

        @NonNull
        @Override
        public Iterator<O> iterator() {
            Iterator<O> iterator;
            synchronized (mObservers) {
                iterator = new ArrayList<O>(mObservers).iterator();
            }
            return iterator;
        }

        /**
         *
         */
        @VisibleForTesting
        public boolean hasObservers() {
            return !mObservers.isEmpty();
        }
    }

    /**
     * A value that can be read and observed.
     *
     * @param <V> The type of the value.
     */
    public static class ExposedValue<V> extends MultiObservers<ValueObserver<V>>
            implements ObservableValue<V> {

        @Nullable
        protected V mValue;

        /**
         * Constructor with supplied initial value. Observers are not notified when assigning
         * initial
         * value.
         *
         * @param initialValue The initial value.
         */
        protected ExposedValue(@Nullable V initialValue) {
            mValue = initialValue;
        }

        /**
         * Sets a new value and notifies all observers (even if the new value is the same as it
         * was).
         */
        public void set(@Nullable V newValue) {
            V oldValue = mValue;
            mValue = newValue;
            notifyObservers(oldValue);
        }

        @Override
        public V get() {
            return mValue;
        }

        protected void notifyObservers(@Nullable V oldValue) {
            for (ValueObserver<V> observer : this) {
                observer.onChange(oldValue, mValue);
            }
        }
    }

    /** Shortcut method for creating a new {@link ExposedValue} instance. */
    @NonNull
    public static <V> ExposedValue<V> newExposedValue() {
        return new ExposedValue<V>(null);
    }

    /** Shortcut method for creating a new {@link ExposedValue} instance with an initial value. */
    @NonNull
    public static <V> ExposedValue<V> newExposedValueWithInitialValue(V initialValue) {
        return new ExposedValue<V>(initialValue);
    }

    /**
     * An array that can be read and observed. Array is understood here as a mapping of integers to
     * Objects, with possible gaps in the indices (see {@link SparseArray}). In particular, unlike
     * most lists, the index of a value won't change because another value is inserted or removed
     * at a
     * different index.
     *
     * @param <V> The type of the value.
     */
    public static class ExposedArray<V> extends AbstractObservable<ArrayObserver<V>>
            implements ObservableArray<V> {

        protected final SparseArray<V> mArray;

        public ExposedArray() {
            mArray = new SparseArray<V>();
        }

        @Override
        public int size() {
            return mArray.size();
        }

        /**
         *
         */
        @NonNull
        @Override
        public Iterable<Integer> keys() {
            return CollectUtils.iterableKeys(mArray);
        }

        /**
         *
         */
        @Nullable
        public V set(int index, V value) {
            V previousValue = mArray.get(index);
            mArray.put(index, value);
            for (ArrayObserver<V> observer : mObservers) {
                if (previousValue == null) {
                    observer.onValueAdded(index, value);
                } else {
                    observer.onValueReplaced(index, previousValue, value);
                }
            }
            return previousValue;
        }

        /**
         *
         */
        @Nullable
        public V remove(int index) {
            V previousValue = mArray.get(index);
            if (previousValue != null) {
                mArray.delete(index);
                for (ArrayObserver<V> observer : mObservers) {
                    observer.onValueRemoved(index, previousValue);
                }
            }
            return previousValue;
        }

        @Override
        @Nullable
        public V get(int index) {
            return mArray.get(index);
        }
    }

    /** Shortcut method for creating a new {@link ExposedArray} instance. */
    @NonNull
    public static <V> ExposedArray<V> newExposedArray() {
        return new ExposedArray<V>();
    }

    /**
     * A convenient way to make a Subject class observable is by inheriting this base class.
     *
     * @param <O> The type of the observers that can be registered with this subject.
     */
    public abstract static class AbstractObservable<O> implements Observable<O> {
        protected final MultiObservers<O> mObservers = new MultiObservers<O>();

        @NonNull
        @Override
        public Object addObserver(O observer) {
            return mObservers.addObserver(observer);
        }

        @Override
        public void removeObserver(@NonNull Object observer) {
            mObservers.removeObserver(observer);
        }

        @NonNull
        protected Iterable<O> getObservers() {
            return mObservers;
        }
    }

    private Observables() {
    }
}
