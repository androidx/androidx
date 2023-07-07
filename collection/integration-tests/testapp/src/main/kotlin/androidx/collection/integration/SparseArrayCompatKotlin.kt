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

package androidx.collection.integration

import androidx.collection.SparseArrayCompat

/**
 * Integration (actually build) test that SparseArrayCompat can be subclassed.
 */
@Suppress("unused")
class SparseArrayCompatKotlin : SparseArrayCompat<Int>() {
    override fun clone(): SparseArrayCompat<Int> {
        return super.clone()
    }

    override fun get(key: Int): Int? {
        return super.get(key)
    }

    override fun get(key: Int, defaultValue: Int): Int {
        return super.get(key, defaultValue)
    }

    @Deprecated(
        message = "Alias for remove(int).",
        replaceWith = ReplaceWith("remove(key)"),
    )
    override fun delete(key: Int) {
        @Suppress("DEPRECATION")
        super.delete(key)
    }

    override fun remove(key: Int) {
        super.remove(key)
    }

    override fun remove(key: Int, value: Any?): Boolean {
        return super.remove(key, value)
    }

    override fun removeAt(index: Int) {
        super.removeAt(index)
    }

    override fun removeAtRange(index: Int, size: Int) {
        super.removeAtRange(index, size)
    }

    override fun replace(key: Int, value: Int): Int? {
        return super.replace(key, value)
    }

    override fun replace(key: Int, oldValue: Int, newValue: Int): Boolean {
        return super.replace(key, oldValue, newValue)
    }

    override fun put(key: Int, value: Int) {
        super.put(key, value)
    }

    override fun putAll(other: SparseArrayCompat<out Int>) {
        super.putAll(other)
    }

    override fun putIfAbsent(key: Int, value: Int): Int? {
        return super.putIfAbsent(key, value)
    }

    override fun size(): Int {
        return super.size()
    }

    override fun isEmpty(): Boolean {
        return super.isEmpty()
    }

    override fun keyAt(index: Int): Int {
        return super.keyAt(index)
    }

    override fun valueAt(index: Int): Int {
        return super.valueAt(index)
    }

    override fun setValueAt(index: Int, value: Int) {
        super.setValueAt(index, value)
    }

    override fun indexOfKey(key: Int): Int {
        return super.indexOfKey(key)
    }

    override fun indexOfValue(value: Int): Int {
        return super.indexOfValue(value)
    }

    override fun containsKey(key: Int): Boolean {
        return super.containsKey(key)
    }

    override fun containsValue(value: Int): Boolean {
        return super.containsValue(value)
    }

    override fun clear() {
        super.clear()
    }

    override fun append(key: Int, value: Int) {
        super.append(key, value)
    }

    override fun toString(): String {
        return super.toString()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

/**
 * Sample usage of SparseArrayCompat for ensuring source compatibility.
 */
@Suppress("unused")
fun sparseArraySourceCompatibility() {
    val sparseArray = SparseArrayCompat<Int>()

    // Property / function syntax.
    sparseArray.isEmpty
    @Suppress("UsePropertyAccessSyntax")
    sparseArray.isEmpty()

    sparseArray.size()

    // Operator access
    @Suppress("UNUSED_VARIABLE")
    val returnsNullable = sparseArray[0] == null
}
