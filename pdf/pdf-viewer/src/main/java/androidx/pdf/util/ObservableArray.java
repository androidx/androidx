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

/**
 * Interface for an observable array of values, which can be exposed by a Subject. Array is
 * understood here as a mapping of integers (keys) to Objects, with possible gaps in the indices
 * (see {@link SparseArray}). In particular, unlike most lists' indices, the key of a value won't
 * change because another value is inserted or removed at a different key/index.
 *
 * @param <T> The type of the values in the container
 * <p>Example: <code>
 * class Subject {
 * public ObservableArray&lt;View;&gt; pages() {...}
 * }
 * </code> in which case clients of pages of this Subject can read its values as: <code>
 * Subject subject;
 * for (int i : subject.pages().keys()) {
 * View view = subject.pages().get(i);
 * ...
 * }
 *
 * and can become observers like e.g.:
 * Object observerKey = subject.pages().addObserver(new ArrayObserver() {...});
 * ... later
 * subject.pages().removeObserver(observerKey);
 * </code>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ObservableArray<T> extends Observable<ObservableArray.ArrayObserver<T>> {

    /** Returns the value associated to the given key, or null if none. */
    @Nullable
    T get(int key);

    /** Iterable over the set of mapped keys, in crescent order. */
    @NonNull
    Iterable<Integer> keys();

    /** The number of values in this array. */
    int size();

    /**
     * Interface to be provided by observers of this container.
     * @param <T>
     */
    interface ArrayObserver<T> {

        /** Signals a new value has been added to the array. */
        void onValueAdded(int index, T addedValue);

        /** Signals a value has been removed from the array. */
        void onValueRemoved(int index, T removedValue);

        /** Signals a value has been replaced by another in the array. */
        void onValueReplaced(int index, T previousValue, T newValue);
    }
}
