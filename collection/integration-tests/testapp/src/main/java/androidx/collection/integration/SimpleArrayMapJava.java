/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.collection.SimpleArrayMap;

/**
 * Integration (actually build) test that LruCache can be subclassed.
 */
@SuppressWarnings("unused")
public class SimpleArrayMapJava extends SimpleArrayMap<Integer, String> {
    public SimpleArrayMapJava() {
        super();
    }

    public SimpleArrayMapJava(int capacity) {
        super(capacity);
    }

    public SimpleArrayMapJava(@NonNull SimpleArrayMap<Integer, String> map) {
        super(map);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void ensureCapacity(int minimumCapacity) {
        super.ensureCapacity(minimumCapacity);
    }

    @Override
    public boolean containsKey(@Nullable Integer key) {
        return super.containsKey(key);
    }

    @Override
    public int indexOfKey(@Nullable Integer key) {
        return super.indexOfKey(key);
    }

    @Override
    public boolean containsValue(@NonNull String value) {
        return super.containsValue(value);
    }

    @Nullable
    @Override
    public String get(@NonNull Integer key) {
        return super.get(key);
    }

    @NonNull
    @Override
    public String getOrDefault(@Nullable Object key, @NonNull String defaultValue) {
        return super.getOrDefault(key, defaultValue);
    }

    @NonNull
    @Override
    public Integer keyAt(int index) {
        return super.keyAt(index);
    }

    @NonNull
    @Override
    public String valueAt(int index) {
        return super.valueAt(index);
    }

    @NonNull
    @Override
    public String setValueAt(int index, @NonNull String value) {
        return super.setValueAt(index, value);
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Nullable
    @Override
    public String put(@NonNull Integer key, @NonNull String value) {
        return super.put(key, value);
    }

    @Override
    public void putAll(@NonNull SimpleArrayMap<? extends Integer, ? extends String> map) {
        super.putAll(map);
    }

    @Nullable
    @Override
    public String putIfAbsent(@NonNull Integer key, @NonNull String value) {
        return super.putIfAbsent(key, value);
    }

    @Nullable
    @Override
    public String remove(@NonNull Integer key) {
        return super.remove(key);
    }

    @Override
    public boolean remove(@NonNull Integer key, @NonNull String value) {
        return super.remove(key, value);
    }

    @NonNull
    @Override
    public String removeAt(int index) {
        return super.removeAt(index);
    }

    @Nullable
    @Override
    public String replace(@NonNull Integer key, @NonNull String value) {
        return super.replace(key, value);
    }

    @Override
    public boolean replace(@NonNull Integer key, @NonNull String oldValue,
            @NonNull String newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }

    @SuppressWarnings("ReferenceEquality")
    public static boolean sourceCompatibility() {
        SimpleArrayMap<Integer, String> map = new SimpleArrayMap<>(10);
        map.put(0, "");
        map.putIfAbsent(1, "");
        map.clear();

        //noinspection StringEquality
        return map.isEmpty() && map.size() == 0
                && map.get(5) == map.getOrDefault(5, null)
                && map.containsKey(2) && map.containsValue("")
                && map.indexOfKey(0) == 0 && map.remove(1) == null;
    }
}
