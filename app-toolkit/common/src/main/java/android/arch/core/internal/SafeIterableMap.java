/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.core.internal;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * LinkedList, which pretends to be a map and supports modifications during iterations.
 * It is NOT thread safe.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SafeIterableMap<K, V> implements Iterable<Map.Entry<K, V>> {

    private Entry<K, V> mStart;
    private Entry<K, V> mEnd;
    // using WeakHashMap over List<WeakReference>, so we don't have to manually remove
    // WeakReferences that have null in them.
    private WeakHashMap<SupportRemove<K, V>, Boolean> mIterators = new WeakHashMap<>();
    private int mSize = 0;

    protected Entry<K, V> get(K k) {
        Entry<K, V> currentNode = mStart;
        while (currentNode != null) {
            if (currentNode.mKey.equals(k)) {
                break;
            }
            currentNode = currentNode.mNext;
        }
        return currentNode;
    }

    /**
     * If the specified key is not already associated
     * with a value, associates it with the given value.
     *
     * @param key key with which the specified value is to be associated
     * @param v   value to be associated with the specified key
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     */
    public V putIfAbsent(@NonNull K key, @NonNull V v) {
        Entry<K, V> entry = get(key);
        if (entry != null) {
            return entry.mValue;
        }
        put(key, v);
        return null;
    }

    protected Entry<K, V> put(@NonNull K key, @NonNull V v) {
        Entry<K, V> newEntry = new Entry<>(key, v);
        mSize++;
        if (mEnd == null) {
            mStart = newEntry;
            mEnd = mStart;
            return newEntry;
        }

        mEnd.mNext = newEntry;
        newEntry.mPrevious = mEnd;
        mEnd = newEntry;
        return newEntry;

    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with the specified key,
     * or {@code null} if there was no mapping for the key
     */
    public V remove(@NonNull K key) {
        Entry<K, V> toRemove = get(key);
        if (toRemove == null) {
            return null;
        }
        mSize--;
        if (!mIterators.isEmpty()) {
            for (SupportRemove<K, V> iter : mIterators.keySet()) {
                iter.supportRemove(toRemove);
            }
        }

        if (toRemove.mPrevious != null) {
            toRemove.mPrevious.mNext = toRemove.mNext;
        } else {
            mStart = toRemove.mNext;
        }

        if (toRemove.mNext != null) {
            toRemove.mNext.mPrevious = toRemove.mPrevious;
        } else {
            mEnd = toRemove.mPrevious;
        }

        toRemove.mNext = null;
        toRemove.mPrevious = null;
        return toRemove.mValue;
    }

    /**
     * @return the number of elements in this map
     */
    public int size() {
        return mSize;
    }

    /**
     * @return an ascending iterator, which doesn't include new elements added during an
     * iteration.
     */
    @NonNull
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        ListIterator<K, V> iterator = new AscendingIterator<>(mStart, mEnd);
        mIterators.put(iterator, false);
        return iterator;
    }

    /**
     * @return an descending iterator, which doesn't include new elements added during an
     * iteration.
     */
    public Iterator<Map.Entry<K, V>> descendingIterator() {
        DescendingIterator<K, V> iterator = new DescendingIterator<>(mEnd, mStart);
        mIterators.put(iterator, false);
        return iterator;
    }

    /**
     * return an iterator with additions.
     */
    public IteratorWithAdditions iteratorWithAdditions() {
        @SuppressWarnings("unchecked")
        IteratorWithAdditions iterator = new IteratorWithAdditions();
        mIterators.put(iterator, false);
        return iterator;
    }

    /**
     * @return eldest added entry or null
     */
    public Map.Entry<K, V> eldest() {
        return mStart;
    }

    /**
     * @return newest added entry or null
     */
    public Map.Entry<K, V> newest() {
        return mEnd;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SafeIterableMap)) {
            return false;
        }
        SafeIterableMap map = (SafeIterableMap) obj;
        if (this.size() != map.size()) {
            return false;
        }
        Iterator<Map.Entry<K, V>> iterator1 = iterator();
        Iterator iterator2 = map.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            Map.Entry<K, V> next1 = iterator1.next();
            Object next2 = iterator2.next();
            if ((next1 == null && next2 != null)
                    || (next1 != null && !next1.equals(next2))) {
                return false;
            }
        }
        return !iterator1.hasNext() && !iterator2.hasNext();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        Iterator<Map.Entry<K, V>> iterator = iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().toString());
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private abstract static class ListIterator<K, V> implements Iterator<Map.Entry<K, V>>,
            SupportRemove<K, V> {
        Entry<K, V> mExpectedEnd;
        Entry<K, V> mNext;

        ListIterator(Entry<K, V> start, Entry<K, V> expectedEnd) {
            this.mExpectedEnd = expectedEnd;
            this.mNext = start;
        }

        @Override
        public boolean hasNext() {
            return mNext != null;
        }

        @Override
        public void supportRemove(@NonNull Entry<K, V> entry) {
            if (mExpectedEnd == entry && entry == mNext) {
                mNext = null;
                mExpectedEnd = null;
            }

            if (mExpectedEnd == entry) {
                mExpectedEnd = backward(mExpectedEnd);
            }

            if (mNext == entry) {
                mNext = nextNode();
            }
        }

        private Entry<K, V> nextNode() {
            if (mNext == mExpectedEnd || mExpectedEnd == null) {
                return null;
            }
            return forward(mNext);
        }

        @Override
        public Map.Entry<K, V> next() {
            Map.Entry<K, V> result = mNext;
            mNext = nextNode();
            return result;
        }

        abstract Entry<K, V> forward(Entry<K, V> entry);

        abstract Entry<K, V> backward(Entry<K, V> entry);
    }

    static class AscendingIterator<K, V> extends ListIterator<K, V> {
        AscendingIterator(Entry<K, V> start, Entry<K, V> expectedEnd) {
            super(start, expectedEnd);
        }

        @Override
        Entry<K, V> forward(Entry<K, V> entry) {
            return entry.mNext;
        }

        @Override
        Entry<K, V> backward(Entry<K, V> entry) {
            return entry.mPrevious;
        }
    }

    private static class DescendingIterator<K, V> extends ListIterator<K, V> {

        DescendingIterator(Entry<K, V> start, Entry<K, V> expectedEnd) {
            super(start, expectedEnd);
        }

        @Override
        Entry<K, V> forward(Entry<K, V> entry) {
            return entry.mPrevious;
        }

        @Override
        Entry<K, V> backward(Entry<K, V> entry) {
            return entry.mNext;
        }
    }

    private class IteratorWithAdditions implements Iterator<Map.Entry<K, V>>, SupportRemove<K, V> {
        private Entry<K, V> mCurrent;
        private boolean mBeforeStart = true;

        @Override
        public void supportRemove(@NonNull Entry<K, V> entry) {
            if (entry == mCurrent) {
                mCurrent = mCurrent.mPrevious;
                mBeforeStart = mCurrent == null;
            }
        }

        @Override
        public boolean hasNext() {
            if (mBeforeStart) {
                return mStart != null;
            }
            return mCurrent != null && mCurrent.mNext != null;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (mBeforeStart) {
                mBeforeStart = false;
                mCurrent = mStart;
            } else {
                mCurrent = mCurrent != null ? mCurrent.mNext : null;
            }
            return mCurrent;
        }
    }

    interface SupportRemove<K, V> {
        void supportRemove(@NonNull Entry<K, V> entry);
    }

    static class Entry<K, V> implements Map.Entry<K, V> {
        @NonNull
        final K mKey;
        @NonNull
        final V mValue;
        Entry<K, V> mNext;
        Entry<K, V> mPrevious;

        Entry(@NonNull K key, @NonNull V value) {
            mKey = key;
            this.mValue = value;
        }

        @NonNull
        @Override
        public K getKey() {
            return mKey;
        }

        @NonNull
        @Override
        public V getValue() {
            return mValue;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("An entry modification is not supported");
        }

        @Override
        public String toString() {
            return mKey + "=" + mValue;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Entry)) {
                return false;
            }
            Entry entry = (Entry) obj;
            return mKey.equals(entry.mKey) && mValue.equals(entry.mValue);
        }
    }
}
