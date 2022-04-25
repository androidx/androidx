/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.collection;

import java.util.Iterator;
import java.util.NoSuchElementException;

abstract class IndexBasedArrayIterator<T> implements Iterator<T> {
    private int mSize;
    private int mIndex;
    private boolean mCanRemove;

    IndexBasedArrayIterator(int startingSize) {
        mSize = startingSize;
    }

    protected abstract T elementAt(int index);
    protected abstract void removeAt(int index);

    @Override
    public final boolean hasNext() {
        return mIndex < mSize;
    }

    @Override
    public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        T res = elementAt(mIndex);
        mIndex++;
        mCanRemove = true;
        return res;
    }

    @Override
    public void remove() {
        if (!mCanRemove) {
            throw new IllegalStateException();
        }
        // Attempt removal first so an UnsupportedOperationException retains a valid state.
        removeAt(--mIndex);
        mSize--;
        mCanRemove = false;
    }
}
