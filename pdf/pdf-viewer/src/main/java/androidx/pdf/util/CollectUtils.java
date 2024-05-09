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
import androidx.annotation.RestrictTo;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Utilities related to Collections.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CollectUtils {

    /**
     * Creates an {@link Iterable} over the values mapped in a {@link SparseArray}. Ideally, only
     * non-null values would be returned, although it's always possible to associate a null value
     * to a key in SparseArray, in which case the null value would be returned.
     */
    @NonNull
    public static <T> Iterable<T> asIterable(final @NonNull SparseArray<T> array) {
        return new Iterable<T>() {
            @NonNull
            @Override
            public Iterator<T> iterator() {
                return makeIterator(array);
            }
        };
    }

    /**
     *
     */
    @NonNull
    public static <T> Iterator<T> makeIterator(final @NonNull SparseArray<T> array) {
        return new Iterator<T>() {

            private int mIndex = 0;

            @Override
            public boolean hasNext() {
                return mIndex < array.size();
            }

            @Override
            public T next() {
                if (mIndex >= array.size()) {
                    throw new NoSuchElementException(
                            String.format("Request element #%d of %d", mIndex, array.size()));
                }
                return array.valueAt(mIndex++);
            }

            @Override
            public void remove() {
                array.removeAt(mIndex - 1);
            }
        };
    }

    /** Creates an {@link Iterable} over the keys mapped in a {@link SparseArray}. */
    @NonNull
    public static Iterable<Integer> iterableKeys(final @NonNull SparseArray<?> array) {
        return new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return makeKeyIterator(array);
            }
        };
    }

    /**
     *
     */
    @NonNull
    public static Iterator<Integer> makeKeyIterator(final @NonNull SparseArray<?> array) {
        return new Iterator<Integer>() {

            private int mIndex = 0;

            @Override
            public boolean hasNext() {
                return mIndex < array.size();
            }

            @Override
            public Integer next() {
                if (mIndex >= array.size()) {
                    throw new NoSuchElementException(
                            String.format("Request element #%d of %d", mIndex, array.size()));
                }
                return array.keyAt(mIndex++);
            }

            @Override
            public void remove() {
                array.removeAt(mIndex - 1);
            }
        };
    }

    private CollectUtils() {
    }
}
