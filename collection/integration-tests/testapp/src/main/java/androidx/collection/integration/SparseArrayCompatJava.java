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
import androidx.collection.SparseArrayCompat;

/**
 * Integration (actually build) test that SparseArrayCompat can be subclassed.
 *
 * @param <E> element type
 */
public class SparseArrayCompatJava<E> extends SparseArrayCompat<E> {
    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public int size() {
        return super.size();
    }

    public SparseArrayCompatJava() {
        super();
    }

    public SparseArrayCompatJava(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public SparseArrayCompat<E> clone() {
        return super.clone();
    }

    @Nullable
    @Override
    public E get(int key) {
        return super.get(key);
    }

    @Override
    public E get(int key, E valueIfKeyNotFound) {
        return super.get(key, valueIfKeyNotFound);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void delete(int key) {
        super.delete(key);
    }

    @Override
    public void remove(int key) {
        super.remove(key);
    }

    @Override
    public boolean remove(int key, Object value) {
        return super.remove(key, value);
    }

    @Override
    public void removeAt(int index) {
        super.removeAt(index);
    }

    @Override
    public void removeAtRange(int index, int size) {
        super.removeAtRange(index, size);
    }

    @Nullable
    @Override
    public E replace(int key, E value) {
        return super.replace(key, value);
    }

    @Override
    public boolean replace(int key, E oldValue, E newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public void put(int key, E value) {
        super.put(key, value);
    }

    @Override
    public void putAll(@NonNull SparseArrayCompat<? extends E> other) {
        super.putAll(other);
    }

    @Nullable
    @Override
    public E putIfAbsent(int key, E value) {
        return super.putIfAbsent(key, value);
    }

    @Override
    public int keyAt(int index) {
        return super.keyAt(index);
    }

    @Override
    public E valueAt(int index) {
        return super.valueAt(index);
    }

    @Override
    public void setValueAt(int index, E value) {
        super.setValueAt(index, value);
    }

    @Override
    public int indexOfKey(int key) {
        return super.indexOfKey(key);
    }

    @Override
    public int indexOfValue(E value) {
        return super.indexOfValue(value);
    }

    @Override
    public boolean containsKey(int key) {
        return super.containsKey(key);
    }

    @Override
    public boolean containsValue(E value) {
        return super.containsValue(value);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public void append(int key, E value) {
        super.append(key, value);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
