/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Memory-efficient map of keys to values with list-style random-access semantics.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ArrayMap<K, V> extends SimpleArrayMap<K, V> implements Map<K, V> {
    @Nullable EntrySet<K, V> mEntrySet;
    @Nullable KeySet<K, V> mKeySet;
    @Nullable ValueCollection<K, V> mValues;

    public ArrayMap() {
        super();
    }

    /**
     * Create a new ArrayMap with a given initial capacity.
     */
    public ArrayMap(int capacity) {
        super(capacity);
    }

    /**
     * Create a new ArrayMap with the mappings from the given ArrayMap.
     */
    @SuppressWarnings("unchecked")
    public ArrayMap(@NonNull SimpleArrayMap<K, V> map) {
        super(map);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public V get(@NonNull Object key) {
        return super.get((K) key);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public V remove(@NonNull Object key) {
        return super.remove((K) key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsKey(@Nullable Object value) {
        return super.containsKey((K) value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean containsValue(@NonNull Object value) {
        return super.containsValue((V) value);
    }

    /**
     * Determine if the array map contains all of the keys in the given collection.
     * @param collection The collection whose contents are to be checked against.
     * @return Returns true if this array map contains a key for every entry
     * in <var>collection</var>, else returns false.
     */
    public boolean containsAll(@NonNull Collection<?> collection) {
        for (Object o : collection) {
            if (!containsKey(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Perform a {@link #put(Object, Object)} of all key/value pairs in <var>map</var>
     * @param map The map whose contents are to be retrieved.
     */
    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> map) {
        ensureCapacity(size() + map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Remove all keys in the array map that exist in the given collection.
     * @param collection The collection whose contents are to be used to remove keys.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    public boolean removeAll(@NonNull Collection<?> collection) {
        int oldSize = size();
        for (Object o : collection) {
            remove(o);
        }
        return oldSize != size();
    }

    /**
     * Remove all keys in the array map that do <b>not</b> exist in the given collection.
     * @param collection The collection whose contents are to be used to determine which
     * keys to keep.
     * @return Returns true if any keys were removed from the array map, else false.
     */
    public boolean retainAll(@NonNull Collection<?> collection) {
        int oldSize = size();
        for (int i = size() - 1; i >= 0; i--) {
            if (!collection.contains(keyAt(i))) {
                removeAt(i);
            }
        }
        return oldSize != size();
    }

    /**
     * Return a {@link java.util.Set} for iterating over and interacting with all mappings
     * in the array map.
     *
     * <p><b>Note:</b> this is a very inefficient way to access the array contents, it
     * requires generating a number of temporary objects.</p>
     *
     * <p><b>Note:</b></p> the semantics of this
     * Set are subtly different than that of a {@link java.util.HashMap}: most important,
     * the {@link java.util.Map.Entry Map.Entry} object returned by its iterator is a single
     * object that exists for the entire iterator, so you can <b>not</b> hold on to it
     * after calling {@link java.util.Iterator#next() Iterator.next}.</p>
     */
    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = mEntrySet;
        if (entrySet == null) {
            entrySet = mEntrySet = new EntrySet<K, V>(this);
        }
        return entrySet;
    }

    /**
     * Return a {@link java.util.Set} for iterating over and interacting with all keys
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects.</p>
     */
    @NonNull
    @Override
    public Set<K> keySet() {
        Set<K> keySet = mKeySet;
        if (keySet == null) {
            keySet = mKeySet = new KeySet<K, V>(this);
        }
        return keySet;
    }

    /**
     * Return a {@link java.util.Collection} for iterating over and interacting with all values
     * in the array map.
     *
     * <p><b>Note:</b> this is a fairly inefficient way to access the array contents, it
     * requires generating a number of temporary objects.</p>
     */
    @NonNull
    @Override
    public Collection<V> values() {
        Collection<V> values = mValues;
        if (values == null) {
            values = mValues = new ValueCollection<K, V>(this);
        }
        return values;
    }
}
