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

import androidx.collection.SimpleArrayMap

/**
 * Integration (actually build) test that LruCache can be subclassed.
 */
@Suppress("RedundantOverride", "unused")
class SimpleArrayMapKotlin : SimpleArrayMap<Int, String>() {
    @Suppress("UNUSED_PARAMETER")
    fun indexOf(key: Any?, hash: Int): Int = 0

    fun indexOfNull(): Int = 0

    override fun clear() {
        super.clear()
    }

    override fun ensureCapacity(minimumCapacity: Int) {
        super.ensureCapacity(minimumCapacity)
    }

    override fun containsKey(key: Int): Boolean {
        return super.containsKey(key)
    }

    override fun indexOfKey(key: Int): Int {
        return super.indexOfKey(key)
    }

    override fun containsValue(value: String): Boolean {
        return super.containsValue(value)
    }

    override fun get(key: Int): String? {
        return super.get(key)
    }

    override fun getOrDefault(key: Any?, defaultValue: String): String {
        return super.getOrDefault(key, defaultValue)
    }

    override fun keyAt(index: Int): Int {
        return super.keyAt(index)
    }

    override fun valueAt(index: Int): String {
        return super.valueAt(index)
    }

    override fun setValueAt(index: Int, value: String): String {
        return super.setValueAt(index, value)
    }

    override fun isEmpty(): Boolean {
        return super.isEmpty()
    }

    override fun put(key: Int, value: String): String? {
        return super.put(key, value)
    }

    override fun putAll(map: SimpleArrayMap<out Int, out String>) {
        super.putAll(map)
    }

    override fun putIfAbsent(key: Int, value: String): String? {
        return super.putIfAbsent(key, value)
    }

    override fun remove(key: Int): String? {
        return super.remove(key)
    }

    override fun remove(key: Int, value: String): Boolean {
        return super.remove(key, value)
    }

    override fun removeAt(index: Int): String {
        return super.removeAt(index)
    }

    override fun replace(key: Int, value: String): String? {
        return super.replace(key, value)
    }

    override fun replace(key: Int, oldValue: String, newValue: String): Boolean {
        return super.replace(key, oldValue, newValue)
    }

    override fun size(): Int {
        return super.size()
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }
}

@Suppress("unused")
fun simpleArrayMapSourceCompatibility(): Boolean {
    val map = SimpleArrayMap<Int, String>(10)
    map.putAll(map)
    map.clear()

    @Suppress("ReplaceGetOrSet", "ReplaceCallWithBinaryOperator")
    return map.isEmpty() && map.size() == 0 && map[0] == map.get(0) &&
        map.getOrDefault(0, "").equals("") && map.put(0, "")?.equals(map.putIfAbsent(0, ""))!! &&
        map.removeAt(0).plus(map.remove(0)) == map.setValueAt(0, "")
}
