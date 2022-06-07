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

package androidx.collection.integration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

/**
 * Integration (actually build) test that LruCache can be subclassed.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class LruCacheJava<K, V> extends LruCache<K, V> {
    public LruCacheJava(int maxSize) {
        super(maxSize);
    }

    @Override
    protected int sizeOf(@NonNull K key, @NonNull V value) {
        return super.sizeOf(key, value);
    }

    @Override
    public void resize(int maxSize) {
        super.resize(maxSize);
    }

    @Override
    public void trimToSize(int maxSize) {
        super.trimToSize(maxSize);
    }

    @Override
    protected void entryRemoved(boolean evicted, @NonNull K key, @NonNull V oldValue,
            @Nullable V newValue) {
        super.entryRemoved(evicted, key, oldValue, newValue);
    }

    @Nullable
    @Override
    protected V create(@NonNull K key) {
        return super.create(key);
    }
}
