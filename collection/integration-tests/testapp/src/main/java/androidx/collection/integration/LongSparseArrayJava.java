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
import androidx.collection.LongSparseArray;

/**
 * Integration (actually build) test that LongSparseArray can be subclassed.
 *
 * @param <E> element type
 */
@SuppressWarnings("unused")
class LongSparseArrayJava<E> extends LongSparseArray<E> {

    @Override
    public LongSparseArray<E> clone() {
        return super.clone();
    }

    @Nullable
    @Override
    public E get(long key) {
        return super.get(key);
    }

    @Override
    public E get(long key, E valueIfKeyNotFound) {
        return super.get(key, valueIfKeyNotFound);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void delete(long key) {
        super.delete(key);
    }

    @Override
    public void remove(long key) {
        super.remove(key);
    }

    @Override
    public boolean remove(long key, Object value) {
        return super.remove(key, value);
    }

    @Override
    public void removeAt(int index) {
        super.removeAt(index);
    }

    @Nullable
    @Override
    public E replace(long key, E value) {
        return super.replace(key, value);
    }

    @Override
    public boolean replace(long key, E oldValue, E newValue) {
        return super.replace(key, oldValue, newValue);
    }

    @Override
    public void put(long key, E value) {
        super.put(key, value);
    }

    @Override
    public void putAll(@NonNull LongSparseArray<? extends E> other) {
        super.putAll(other);
    }

    @Nullable
    @Override
    public E putIfAbsent(long key, E value) {
        return super.putIfAbsent(key, value);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public long keyAt(int index) {
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
    public int indexOfKey(long key) {
        return super.indexOfKey(key);
    }

    @Override
    public int indexOfValue(E value) {
        return super.indexOfValue(value);
    }

    @Override
    public boolean containsKey(long key) {
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
    public void append(long key, E value) {
        super.append(key, value);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static boolean sourceCompatibility() {
        LongSparseArray<Integer> array = new LongSparseArray<>(10);
        array.put(0, null);
        array.putAll(array);
        array.putIfAbsent(1, null);
        array.append(2, null);
        array.remove(2);
        array.removeAt(2);
        array.setValueAt(3, null);
        array.clear();

        //noinspection NumberEquality
        return array.size() == 0 && array.isEmpty() && array.get(0) == array.get(2, null)
                && array.get(2, null) == null && array.containsKey(0) && array.containsValue(null)
                && array.remove(0, 0) && array.replace(0, null, 1) && array.replace(0, null) == null
                && array.indexOfKey(0) == array.indexOfValue(null) && array.valueAt(3) == null;
    }
}
