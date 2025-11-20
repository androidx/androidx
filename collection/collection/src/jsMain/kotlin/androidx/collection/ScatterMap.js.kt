/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.collection

public actual sealed class ScatterMap<K, V>
protected constructor(@PublishedApi internal val delegate: MutableMap<K, V>) {
    public actual val capacity: Int
        get() = 0

    public actual val size: Int
        get() = delegate.size

    public actual fun any(): Boolean = size != 0

    public actual fun none(): Boolean = size == 0

    public actual fun isEmpty(): Boolean = size == 0

    public actual fun isNotEmpty(): Boolean = size != 0

    public actual operator fun get(key: K): V? = delegate.get(key)

    public actual fun getOrDefault(key: K, defaultValue: V): V {
        val result = delegate[key]
        return when {
            result != null -> result
            delegate.containsKey(key) -> {
                @Suppress("UNCHECKED_CAST") // Storing null guarantees a nullable V.
                null as V
            }
            else -> defaultValue
        }
    }

    public actual inline fun getOrElse(key: K, defaultValue: () -> V): V {
        return get(key) ?: defaultValue()
    }

    public actual inline fun forEach(block: (key: K, value: V) -> Unit) {
        delegate.forEach { (k, v) -> block(k, v) }
    }

    public actual inline fun forEachKey(block: (key: K) -> Unit) {
        delegate.keys.forEach(block)
    }

    public actual inline fun forEachValue(block: (value: V) -> Unit) {
        delegate.values.forEach(block)
    }

    public actual inline fun all(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (!predicate(key, value)) return false }
        return true
    }

    public actual inline fun any(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (predicate(key, value)) return true }
        return false
    }

    public actual fun count(): Int = size

    public actual inline fun count(predicate: (K, V) -> Boolean): Int {
        var count = 0
        forEach { key, value -> if (predicate(key, value)) count++ }
        return count
    }

    public actual operator fun contains(key: K): Boolean = delegate.contains(key)

    public actual fun containsKey(key: K): Boolean = delegate.contains(key)

    public actual fun containsValue(value: V): Boolean {
        forEachValue { v -> if (value == v) return true }
        return false
    }

    public actual fun joinToString(
        separator: CharSequence,
        prefix: CharSequence,
        postfix: CharSequence,
        limit: Int,
        truncated: CharSequence,
        transform: ((key: K, value: V) -> CharSequence)?,
    ): String {
        return delegate.entries.joinToString(
            separator = separator,
            prefix = prefix,
            postfix = postfix,
            limit = limit,
            truncated = truncated,
            transform = transform?.let { { transform(it.key, it.value) } },
        )
    }

    public actual override fun hashCode(): Int = delegate.hashCode()

    public actual override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is ScatterMap<*, *> && delegate == other.delegate
    }

    public actual override fun toString(): String {
        if (isEmpty()) {
            return "{}"
        }

        val s = StringBuilder().append('{')
        var i = 0
        for ((key, value) in delegate) {
            s.append(if (key === this) "(this)" else key)
            s.append('=')
            s.append(if (value === this) "(this)" else value)
            if (++i < size) {
                s.append(", ")
            }
        }

        return s.append('}').toString()
    }

    public actual fun asMap(): Map<K, V> = MapWrapper(this, delegate)
}

public actual class MutableScatterMap<K, V> private constructor(delegate: MutableMap<K, V>) :
    ScatterMap<K, V>(delegate) {

    public actual constructor(initialCapacity: Int) : this(HashMap(initialCapacity))

    public actual inline fun getOrPut(key: K, defaultValue: () -> V): V {
        return get(key) ?: defaultValue().also { set(key, it) }
    }

    public actual inline fun compute(key: K, computeBlock: (key: K, value: V?) -> V): V {
        val value = this[key]
        val computedValue = computeBlock(key, value)
        put(key, computedValue)
        return computedValue
    }

    public actual operator fun set(key: K, value: V) {
        delegate[key] = value
    }

    public actual fun put(key: K, value: V): V? {
        return delegate.put(key, value)
    }

    public actual fun putAll(@Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>) {
        delegate.putAll(pairs)
    }

    public actual fun putAll(pairs: Iterable<Pair<K, V>>) {
        delegate.putAll(pairs)
    }

    public actual fun putAll(pairs: Sequence<Pair<K, V>>) {
        delegate.putAll(pairs)
    }

    public actual fun putAll(from: Map<K, V>) {
        delegate.putAll(from)
    }

    public actual fun putAll(from: ScatterMap<K, V>) {
        delegate.putAll(from.delegate)
    }

    public actual inline operator fun plusAssign(pair: Pair<K, V>) {
        put(pair.first, pair.second)
    }

    public actual inline operator fun plusAssign(
        @Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>
    ): Unit = putAll(pairs)

    public actual inline operator fun plusAssign(pairs: Iterable<Pair<K, V>>): Unit = putAll(pairs)

    public actual inline operator fun plusAssign(pairs: Sequence<Pair<K, V>>): Unit = putAll(pairs)

    public actual inline operator fun plusAssign(from: Map<K, V>): Unit = putAll(from)

    public actual inline operator fun plusAssign(from: ScatterMap<K, V>): Unit =
        putAll(from.delegate)

    public actual fun remove(key: K): V? {
        return delegate.remove(key)
    }

    public actual fun remove(key: K, value: V): Boolean {
        if (delegate[key] == value) {
            delegate.remove(key)
            return true
        }
        return false
    }

    public actual inline fun removeIf(predicate: (K, V) -> Boolean) {
        val i = asMutableMap().iterator()
        while (i.hasNext()) {
            val (key, value) = i.next()
            if (predicate(key, value)) {
                i.remove()
            }
        }
    }

    public actual inline operator fun minusAssign(key: K) {
        remove(key)
    }

    public actual inline operator fun minusAssign(@Suppress("ArrayReturn") keys: Array<out K>) {
        delegate.minusAssign(keys)
    }

    public actual inline operator fun minusAssign(keys: Iterable<K>) {
        delegate.minusAssign(keys)
    }

    public actual inline operator fun minusAssign(keys: Sequence<K>) {
        delegate.minusAssign(keys)
    }

    public actual inline operator fun minusAssign(keys: ScatterSet<K>) {
        delegate.minusAssign(keys.delegate)
    }

    public actual inline operator fun minusAssign(keys: ObjectList<K>) {
        keys.forEach { key -> remove(key) }
    }

    public actual fun clear() {
        delegate.clear()
    }

    public actual fun trim(): Int {
        return 0
    }

    public actual fun asMutableMap(): MutableMap<K, V> = MutableMapWrapper(this, delegate)
}

private class MapWrapper<K, V>(
    private val scatterMap: ScatterMap<K, V>,
    private val map: Map<K, V>,
) : Map<K, V> by map {
    override fun hashCode() = map.hashCode()

    override fun equals(other: Any?) = map == other

    override fun toString() = scatterMap.toString()
}

private class MutableMapWrapper<K, V>(
    private val scatterMap: MutableScatterMap<K, V>,
    private val map: MutableMap<K, V>,
) : MutableMap<K, V> by map {
    override fun hashCode() = map.hashCode()

    override fun equals(other: Any?) = map == other

    override fun toString() = scatterMap.toString()
}
