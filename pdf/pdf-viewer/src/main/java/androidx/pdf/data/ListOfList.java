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

package androidx.pdf.data;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.util.Preconditions;

import java.util.AbstractList;
import java.util.List;

/**
 * Represents a List of List of type T, but only uses two 1-dimensional
 * lists internally to minimize overhead. Particularly useful for a large outer
 * list that contains mostly single-element inner lists.
 *
 * @param <T> the type of the elements in the list
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ListOfList<T> extends AbstractList<List<T>> {
    private final List<T> mValues;
    private final List<Integer> mIndexToFirstValue;

    public ListOfList(@NonNull List<T> values, @NonNull List<Integer> indexToFirstValue) {
        this.mValues = Preconditions.checkNotNull(values);
        this.mIndexToFirstValue = Preconditions.checkNotNull(indexToFirstValue);
    }

    @Override
    public List<T> get(int index) {
        if (index < 0 || index >= mIndexToFirstValue.size()) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int start = indexToFirstValue(index);
        int stop = indexToFirstValue(index + 1);
        Preconditions.checkState(start < stop, "Empty inner lists are not allowed.");
        return mValues.subList(start, stop);
    }

    @Override
    public int size() {
        return mIndexToFirstValue.size();
    }

    /** Returns the flattened, one-dimensional list of all values. */
    @NonNull
    public List<T> flatten() {
        return mValues;
    }

    protected int indexToFirstValue(int match) {
        return (match < mIndexToFirstValue.size()) ? mIndexToFirstValue.get(match) : mValues.size();
    }
}
