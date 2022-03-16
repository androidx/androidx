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

@file:Suppress("unused")

package androidx.collection.integration

import androidx.collection.LongSparseArray

@Suppress("RedundantOverride")
class LongSparseArrayKotlin(initialCapacity: Int) : LongSparseArray<Int>(initialCapacity) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun clone(): LongSparseArray<Int> {
        return super.clone()
    }

    override fun get(key: Long): Int? {
        return super.get(key)
    }

    override fun get(key: Long, defaultValue: Int): Int {
        return super.get(key, defaultValue)
    }

    @Suppress("DEPRECATION")
    override fun delete(key: Long) {
        super.delete(key)
    }

    override fun remove(key: Long) {
        super.remove(key)
    }

    override fun remove(key: Long, value: Int): Boolean {
        return super.remove(key, value)
    }

    override fun removeAt(index: Int) {
        super.removeAt(index)
    }

    override fun replace(key: Long, value: Int): Int? {
        return super.replace(key, value)
    }

    override fun replace(key: Long, oldValue: Int, newValue: Int): Boolean {
        return super.replace(key, oldValue, newValue)
    }

    override fun put(key: Long, value: Int) {
        super.put(key, value)
    }

    override fun putAll(other: LongSparseArray<out Int>) {
        super.putAll(other)
    }

    override fun putIfAbsent(key: Long, value: Int): Int? {
        return super.putIfAbsent(key, value)
    }

    override fun size(): Int {
        return super.size()
    }

    override fun isEmpty(): Boolean {
        return super.isEmpty()
    }

    override fun keyAt(index: Int): Long {
        return super.keyAt(index)
    }

    override fun valueAt(index: Int): Int {
        return super.valueAt(index)
    }

    override fun setValueAt(index: Int, value: Int) {
        super.setValueAt(index, value)
    }

    override fun indexOfKey(key: Long): Int {
        return super.indexOfKey(key)
    }

    override fun indexOfValue(value: Int): Int {
        return super.indexOfValue(value)
    }

    override fun containsKey(key: Long): Boolean {
        return super.containsKey(key)
    }

    override fun containsValue(value: Int): Boolean {
        return super.containsValue(value)
    }

    override fun clear() {
        super.clear()
    }

    override fun append(key: Long, value: Int) {
        super.append(key, value)
    }

    override fun toString(): String {
        return super.toString()
    }
}

fun longSparseArraySourceCompatibility(): Boolean {
    val array = LongSparseArray<Int>(10)
    array.put(0, 0)
    array.putAll(array)
    array.putIfAbsent(1, 0)
    array.append(2, 0)
    array.remove(2)
    array.removeAt(2)
    array.setValueAt(3, 0)
    array.clear()

    return array.size() == 0 && array.isEmpty() && array.get(0) == array[0] &&
        array.get(2, 0) == 1 && array.containsKey(0) &&
        array.containsValue(0) && array.remove(0, 0) && array.replace(0, 0, 1) &&
        array.replace(0, 0) == null && array.indexOfKey(0) == array.indexOfValue(0) &&
        array.valueAt(3) == 0
}