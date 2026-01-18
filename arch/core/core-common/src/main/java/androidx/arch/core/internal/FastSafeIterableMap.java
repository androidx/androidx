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

package androidx.arch.core.internal;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Poor's man LinkedHashMap, which supports modifications during iterations.
 * Takes more memory that {@link SafeIterableMap}
 * It is NOT thread safe.
 *
 * @param <K> Key type
 * @param <V> Value type
 * @deprecated These are internal legacy utilities and should not be used.
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class FastSafeIterableMap<K, V> extends SafeIterableMap<K, V> {

    private final HashMap<K, Entry<K, V>> mHashMap = new HashMap<>();

    @SuppressWarnings("HiddenTypeParameter")
    @Override
    protected @Nullable Entry<K, V> get(K k) {
        return mHashMap.get(k);
    }

    @Override
    public V putIfAbsent(@NonNull K key, @NonNull V v) {
        Entry<K, V> current = get(key);
        if (current != null) {
            return current.mValue;
        }
        mHashMap.put(key, put(key, v));
        return null;
    }

    @Override
    public V remove(@NonNull K key) {
        V removed = super.remove(key);
        mHashMap.remove(key);
        return removed;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     */
    public boolean contains(K key) {
        return mHashMap.containsKey(key);
    }

    /**
     * Return an entry added to prior to an entry associated with the given key.
     *
     * @param k the key
     */
    public Map.@Nullable Entry<K, V> ceil(K k) {
        if (contains(k)) {
            return mHashMap.get(k).mPrevious;
        }
        return null;
    }
}
