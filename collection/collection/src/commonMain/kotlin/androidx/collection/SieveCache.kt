/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("KotlinRedundantDiagnosticSuppress", "NOTHING_TO_INLINE")

package androidx.collection

import androidx.annotation.IntRange
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.requirePrecondition
import kotlin.jvm.JvmField
import kotlin.math.max

private const val MaxSize = Int.MAX_VALUE.toLong() - 1

@PublishedApi internal const val NodeInvalidLink: Int = 0x7fff_ffff
@PublishedApi internal const val NodeLinkMask: Long = 0x7fff_ffffL
internal const val NodeLinksMask = 0x3fffffff_ffffffffL
internal const val NodeVisitedBit = 0x40000000_00000000L
internal const val NodeMetaMask = -0x40000000_00000000L // 0xc0000000_00000000UL.toLong()
internal const val NodeMetaAndNextMask = -0x3fffffff_80000001L // 0xc0000000_7fffffffUL.toLong()
internal const val NodeMetaAndPreviousMask = -0x00000000_80000000L // 0xffffffff_80000000UL.toLong()

internal const val EmptyNode = 0x3fffffff_ffffffffL
internal val EmptyNodes = LongArray(0)

/**
 * [SieveCache] is an in-memory cache that holds strong references to a limited number of values
 * determined by the cache's [maxSize] and the size of each value. When a value is added to a full
 * cache, one or more existing values are evicted from the cache using the
 * [SIEVE algorithm](https://cachemon.github.io/SIEVE-website/). Complete details about the
 * algorithm can be found in Zhang et al., 2024, SIEVE is Simpler than LRU: an Efficient Turn-Key
 * Eviction Algorithm for Web Caches, NSDI'24
 * ([paper](https://www.usenix.org/system/files/nsdi24-zhang-yazhuo.pdf)).
 *
 * Contrary to [LruCache], [SieveCache] does not maintain a list of entries based on their access
 * order, but on their insertion order. Eviction candidates are found by keeping track of the
 * "visited" status of each entry. This means that reading a value using [get] prevents that entry
 * from becoming an eviction candidate. In practice, [SieveCache] offers better hit ratio compared
 * to [LruCache].
 *
 * The underlying implementation is also designed to avoid all allocations on insertion, removal,
 * retrieval, and iteration. Allocations may still happen on insertion when the underlying storage
 * needs to grow to accommodate newly added entries to the table. In addition, this implementation
 * minimizes memory usage by avoiding the use of separate objects to hold key/value pairs. The
 * implementation follows the implementation of [ScatterMap].
 *
 * By default, the size of the cache is measured in number of entries. The caller can choose the
 * size and size unit of the values by passing their own [sizeOf] lambda, invoked whenever the cache
 * needs to query the size of a value.
 *
 * The [createValueFromKey] lambda can be used to compute values on demand from a key when querying
 * for an entry that does not exist in the cache.
 *
 * When a cached value is removed, either directly by the caller or via the eviction mechanism, you
 * can use the [onEntryRemoved] lambda to execute any side effect or perform any necessary cleanup.
 *
 * This implementation is not thread-safe: if multiple threads access this container concurrently,
 * and one or more threads modify the structure of the map (insertion or removal for instance), the
 * calling code must provide the appropriate synchronization. Multiple threads are safe to read from
 * this map concurrently if no write is happening.
 *
 * A [SieveCache] can hold a maximum of `Int.MAX_VALUE - 1` entries, independent of their computed
 * size.
 *
 * @param maxSize For caches that do not override [sizeOf], this is the maximum number of entries in
 *   the cache. For all other caches, this is the maximum sum of the sizes of the entries in this
 *   cache. The maximum size must be strictly greater than 0 and must be less than or equal to
 *   `Int.MAX_VALUE - 1`.
 * @param initialCapacity The initial desired capacity for this cache. The cache will honor this
 *   value by guaranteeing its internal structures can hold that many entries without requiring any
 *   allocations. The initial capacity can be set to 0.
 * @param sizeOf Returns the size of the entry for the specified key and value. The size of an entry
 *   cannot change after it was added to the cache, and must be >= 0.
 * @param createValueFromKey Called after a cache miss to compute a value for the specified key.
 *   Returning null from this lambda indicates that no value can be computed.
 * @param onEntryRemoved Called for entries that have been removed by the user of the cache, or
 *   automatically evicted. The lambda is supplied with multiple parameters. The `key` of the entry
 *   being removed or evicted. The original value (`oldValue`) of the entry if the entry is being
 *   evicted or replaced. The new value (`newValue`) for the key, if it exists. If non-null, the
 *   removal was caused by a `put()` or a `set()`, otherwise it was caused by an eviction or a
 *   removal. A boolean (`evicted`) set to `true` if the entry was evicted to make space in the
 *   cache, or set to `false` if the removal happened on demand or while replacing an existing value
 *   with [put].
 * @constructor Creates a new [SieveCache].
 */
public class SieveCache<K : Any, V : Any>
public constructor(
    @IntRange(from = 1, to = MaxSize) maxSize: Int,
    @IntRange(from = 0, to = MaxSize) initialCapacity: Int = DefaultScatterCapacity,
    private val sizeOf: (key: K, value: V) -> Int = { _, _ -> 1 },
    private val createValueFromKey: (key: K) -> V? = { null },
    private val onEntryRemoved: (key: K, oldValue: V, newValue: V?, evicted: Boolean) -> Unit =
        { _, _, _, _ ->
        }
) {
    @PublishedApi @JvmField internal var metadata: LongArray = EmptyGroup
    @PublishedApi @JvmField internal var keys: Array<Any?> = EMPTY_OBJECTS
    @PublishedApi @JvmField internal var values: Array<Any?> = EMPTY_OBJECTS
    private var nodes: LongArray = EmptyNodes

    private var _capacity: Int = 0
    private var growthLimit = 0
    private var _count: Int = 0

    private var _maxSize: Int = 0
    private var _size: Int = 0

    private var head = NodeInvalidLink
    private var tail = NodeInvalidLink
    private var hand = NodeInvalidLink

    init {
        requirePrecondition(maxSize > 0) { "maxSize must be > 0" }
        _maxSize = maxSize
        initializeStorage(unloadedCapacity(initialCapacity))
    }

    /**
     * Size of the cache in the unit defined by the implementation of [sizeOf] (by default, the
     * number of elements).
     *
     * @see maxSize
     */
    public val size: Int
        get() = _size

    /**
     * Return the maximum size of the cache before adding new elements causes existing elements to
     * be evicted. The unit of [maxSize] is defined by the implementation of [sizeOf]. Using the
     * default implementation of [sizeOf], [maxSize] indicates the a maximum number of elements.
     *
     * @see size
     */
    public val maxSize: Int
        get() = _maxSize

    /**
     * Returns the number of elements held in the cache.
     *
     * @see capacity
     */
    public val count: Int
        get() = _count

    /**
     * Returns the number of entries that can be stored in this cache without requiring internal
     * storage reallocation.
     *
     * @see count
     */
    public val capacity: Int
        get() = _capacity

    /** Returns `true` if this cache has at least one entry. */
    public fun any(): Boolean = _count != 0

    /** Returns `true` if this cache has no entries. */
    public fun none(): Boolean = _count == 0

    /** Indicates whether this cache is empty. */
    public fun isEmpty(): Boolean = _count == 0

    /** Returns `true` if this cache is not empty. */
    public fun isNotEmpty(): Boolean = _count != 0

    private fun initializeStorage(initialCapacity: Int) {
        val newCapacity =
            if (initialCapacity > 0) {
                // Since we use longs for storage, our capacity is never < 7, enforce
                // it here. We do have a special case for 0 to create small empty maps
                max(7, normalizeCapacity(initialCapacity))
            } else {
                0
            }
        _capacity = newCapacity
        initializeMetadata(newCapacity)
        keys = if (newCapacity == 0) EMPTY_OBJECTS else arrayOfNulls(newCapacity)
        values = if (newCapacity == 0) EMPTY_OBJECTS else arrayOfNulls(newCapacity)
        nodes =
            if (newCapacity == 0) EmptyNodes else LongArray(newCapacity).apply { fill(EmptyNode) }
    }

    private fun initializeMetadata(capacity: Int) {
        metadata =
            if (capacity == 0) {
                EmptyGroup
            } else {
                // Round up to the next multiple of 8 and find how many longs we need
                val size = (((capacity + 1 + ClonedMetadataCount) + 7) and 0x7.inv()) shr 3
                LongArray(size).apply {
                    fill(AllEmpty)
                    writeRawMetadata(this, capacity, Sentinel)
                }
            }
        initializeGrowth()
    }

    private fun initializeGrowth() {
        growthLimit = loadedCapacity(_capacity) - count
    }

    /**
     * Returns the value for [key] if it exists in the cache or can be created by
     * [createValueFromKey]. Return null if a value is not present in the cache and cannot be
     * created.
     */
    public operator fun get(key: K): V? {
        val index = findKeyIndex(key)
        if (index >= 0) {
            markNodeVisited(index)
            @Suppress("UNCHECKED_CAST") return values[index] as V?
        }

        val createdValue = createValueFromKey(key) ?: return null
        put(key, createdValue)

        return createdValue
    }

    /**
     * Adds [value] to the cache using the specific [key]. If [key] is already present in the cache,
     * the association is modified and the previously associated value is replaced with [value]. If
     * [key] is not present, a new entry is added to the map. If an existing value is replaced,
     * [onEntryRemoved] will be invoked with the `evicted` parameter set to `false`.
     *
     * When [value] is added to the cache, [sizeOf] is invoked to query its size. If the total size
     * of the cache, including the new value's size, is greater than [maxSize], existing entries
     * will be evicted. On each removal due to an eviction, [onEntryRemoved] will be invoked with
     * the `evicted` parameter set to `true`.
     */
    public inline operator fun set(key: K, value: V) {
        put(key, value)
    }

    /**
     * Adds [value] to the cache using the specific [key]. If [key] is already present in the map,
     * the association is modified and the previously associated value is replaced with [value]. If
     * [key] is not present, a new entry is added to the map. If an existing value is replaced,
     * [onEntryRemoved] will be invoked with the `evicted` parameter set to `false`.
     *
     * When [value] is added to the cache, [sizeOf] is invoked to query its size. If the total size
     * of the cache, including the new value's size, is greater than [maxSize], existing entries
     * will be evicted. On each removal due to an eviction, [onEntryRemoved] will be invoked with
     * the `evicted` parameter set to `true`.
     *
     * Return the previous value associated with the [key], or `null` if the key was not present in
     * the cache.
     */
    public fun put(key: K, value: V): V? {
        val index = findInsertIndex(key).let { index -> if (index < 0) index.inv() else index }
        @Suppress("UNCHECKED_CAST") val previousValue = values[index] as V?

        values[index] = value
        keys[index] = key

        moveNodeToHead(index)

        _size += sizeOf(key, value)

        if (previousValue != null) {
            _size -= sizeOf(key, previousValue)
            onEntryRemoved(key, previousValue, value, false)
        }

        // TODO: We should trim to size before doing the insertion. The insertion might cause
        //       the underlying storage to resize unnecessarily.
        trimToSize(_maxSize)

        return previousValue
    }

    /**
     * Puts all the [pairs] into this cache, using the first component of the pair as the key, and
     * the second component as the value. Calling this method is equivalent to calling [put] for
     * each input pair. See [put] for more details about the behavior of each insertion.
     */
    public fun putAll(@Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    /**
     * Puts all the [pairs] into this cache, using the first component of the pair as the key, and
     * the second component as the value. Calling this method is equivalent to calling [put] for
     * each input pair. See [put] for more details about the behavior of each insertion.
     */
    public fun putAll(pairs: Iterable<Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    /**
     * Puts all the [pairs] into this cache, using the first component of the pair as the key, and
     * the second component as the value. Calling this method is equivalent to calling [put] for
     * each input pair. See [put] for more details about the behavior of each insertion.
     */
    public fun putAll(pairs: Sequence<Pair<K, V>>) {
        for ((key, value) in pairs) {
            this[key] = value
        }
    }

    /**
     * Puts all the key/value mappings in the [from] map into this cache. Calling this method is
     * equivalent to calling [put] for each input pair. See [put] for more details about the
     * behavior of each insertion.
     */
    public fun putAll(from: Map<K, V>) {
        from.forEach { (key, value) -> this[key] = value }
    }

    /**
     * Puts all the key/value mappings in the [from] map into this cache. Calling this method is
     * equivalent to calling [put] for each input pair. See [put] for more details about the
     * behavior of each insertion.
     */
    public fun putAll(from: ScatterMap<K, V>) {
        from.forEach { key, value -> this[key] = value }
    }

    /**
     * Puts all the key/value mappings in the [from] cache into this cache. Calling this method is
     * equivalent to calling [put] for each input pair. See [put] for more details about the
     * behavior of each insertion.
     */
    public fun putAll(from: SieveCache<K, V>) {
        from.forEach { key, value -> this[key] = value }
    }

    /**
     * Puts the key/value mapping from the [pair] in this cache, using the first element as the key,
     * and the second element as the value. See [put] for more details about the insertion behavior.
     */
    public inline operator fun plusAssign(pair: Pair<K, V>) {
        this[pair.first] = pair.second
    }

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value. Calling this * method is equivalent to calling [put] for each
     * input pair. See [put] for more details about the behavior of each insertion.
     */
    public inline operator fun plusAssign(
        @Suppress("ArrayReturn") pairs: Array<out Pair<K, V>>
    ): Unit = putAll(pairs)

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value. Calling this * method is equivalent to calling [put] for each
     * input pair. See [put] for more details about the behavior of each insertion.
     */
    public inline operator fun plusAssign(pairs: Iterable<Pair<K, V>>): Unit = putAll(pairs)

    /**
     * Puts all the [pairs] into this map, using the first component of the pair as the key, and the
     * second component as the value. Calling this * method is equivalent to calling [put] for each
     * input pair. See [put] for more details about the behavior of each insertion.
     */
    public inline operator fun plusAssign(pairs: Sequence<Pair<K, V>>): Unit = putAll(pairs)

    /**
     * Puts all the key/value mappings in the [from] map into this map. Calling this method is
     * equivalent to calling [put] for each input pair. See [put] for more details about the
     * behavior of each insertion.
     */
    public inline operator fun plusAssign(from: Map<K, V>): Unit = putAll(from)

    /**
     * Puts all the key/value mappings in the [from] map into this map. Calling this method is
     * equivalent to calling [put] for each input pair. See [put] for more details about the
     * behavior of each insertion.
     */
    public inline operator fun plusAssign(from: ScatterMap<K, V>): Unit = putAll(from)

    /**
     * Puts all the key/value mappings in the [from] map into this map. Calling this method is
     * equivalent to calling [put] for each input pair. See [put] for more details about the
     * behavior of each insertion.
     */
    public inline operator fun plusAssign(from: SieveCache<K, V>): Unit = putAll(from)

    /**
     * Removes the specified [key] and its associated value from the cache. If the [key] was present
     * in the cache, this function returns the value that was present before removal, otherwise it
     * returns `null`. On successful removal, [sizeOf] will be invoked to query the size of the
     * removed element, and [onEntryRemoved] will be invoked with the `evicted` parameter set to
     * `false`.
     */
    public fun remove(key: K): V? {
        val index = findKeyIndex(key)
        if (index >= 0) {
            // Better codegen, and can only happen if the data structure is internally inconsistent
            val previousValue = removeValueAt(index) ?: return null
            _size -= sizeOf(key, previousValue)
            onEntryRemoved(key, previousValue, null, false)
            return previousValue
        }

        return null
    }

    /**
     * Removes the specified [key] and its associated value from the cache if the associated value
     * equals [value]. If the [key] was present in the cache, this function returns true, otherwise
     * it returns false. On successful removal, [sizeOf] will be invoked to query the size of the
     * removed element, and [onEntryRemoved] will be invoked with the `evicted` parameter set to
     * `false`.
     */
    public fun remove(key: K, value: V): Boolean {
        val index = findKeyIndex(key)
        if (index >= 0) {
            if (values[index] == value) {
                val previousValue = removeValueAt(index) ?: return false
                _size -= sizeOf(key, previousValue)
                onEntryRemoved(key, previousValue, null, false)
                return true
            }
        }
        return false
    }

    /** Removes any mapping for which the specified [predicate] returns true. */
    public fun removeIf(predicate: (K, V) -> Boolean) {
        forEachIndexed { index ->
            val key = keys[index]
            @Suppress("UNCHECKED_CAST")
            if (predicate(key as K, values[index] as V)) {
                val previousValue = removeValueAt(index) ?: return
                _size -= sizeOf(key, previousValue)
                onEntryRemoved(key, previousValue, null, false)
            }
        }
    }

    /** Removes the specified [key] and its associated value from the map. */
    public inline operator fun minusAssign(key: K) {
        remove(key)
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(@Suppress("ArrayReturn") keys: Array<out K>) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: Iterable<K>) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: Sequence<K>) {
        for (key in keys) {
            remove(key)
        }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: ScatterSet<K>) {
        keys.forEach { key -> remove(key) }
    }

    /** Removes the specified [keys] and their associated value from the map. */
    public inline operator fun minusAssign(keys: ObjectList<K>) {
        keys.forEach { key -> remove(key) }
    }

    /**
     * Removes all the entries from this cache. Upon each removal, [onEntryRemoved] is invoked with
     * the `evicted` parameter set to `true`.
     */
    public fun evictAll() {
        trimToSize(-1)
    }

    /**
     * Sets the maximum size of the cache to [maxSize], in the unit defined by the implementation of
     * [sizeOf]. The size must be strictly greater than 0. If the current total size of the entries
     * in the cache is greater than the new [maxSize], entries will be removed until the total size
     * is less than or equal to [maxSize]. Upon each removal, [onEntryRemoved] is invoked with the
     * `evicted` parameter set to `true`.
     */
    public fun resize(@IntRange(from = 1, to = MaxSize) maxSize: Int) {
        _maxSize = maxSize
        trimToSize(maxSize)
    }

    /**
     * Remove entries until the total size of the remaining entries is less than or equal to
     * [maxSize]. The size of the entries is defined by the implementation of [sizeOf]. Upon each
     * removal, [onEntryRemoved] is invoked with the `evicted` parameter set to `true`.
     *
     * If [maxSize] is set to -1 (or any negative value), all entries are removed.
     */
    public fun trimToSize(maxSize: Int) {
        while (true) {
            if (_size <= maxSize || count == 0) {
                return
            }

            val candidate = findEvictionCandidate()
            if (candidate == NodeInvalidLink) return

            @Suppress("UNCHECKED_CAST") val key = keys[candidate] as K
            // Better codegen compared to !!, and the continue can only happen if the data structure
            // has become internally inconsistent
            val value = removeValueAt(candidate) ?: continue

            _size -= sizeOf(key, value)
            onEntryRemoved(key, value, null, true)
        }
    }

    /**
     * Iterates over every key/value pair stored in this cache by invoking the specified [block]
     * lambda. The iteration order is not specified.
     *
     * **NOTE**: Iterating over the content of the cache does *not* mark entries as recently
     * visited, and therefore does not affect which entries get evicted first.
     */
    public inline fun forEach(block: (key: K, value: V) -> Unit) {
        val k = keys
        val v = values

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(k[index] as K, v[index] as V) }
    }

    /**
     * Iterates over every key stored in this cache by invoking the specified [block] lambda.
     *
     * **NOTE**: Iterating over the content of the cache does *not* mark entries as recently
     * visited, and therefore does not affect which entries get evicted first.
     */
    public inline fun forEachKey(block: (key: K) -> Unit) {
        val k = keys

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(k[index] as K) }
    }

    /**
     * Iterates over every value stored in this cache by invoking the specified [block] lambda.
     *
     * **NOTE**: Iterating over the content of the cache does *not* mark entries as recently
     * visited, and therefore does not affect which entries get evicted first.
     */
    public inline fun forEachValue(block: (value: V) -> Unit) {
        val v = values

        forEachIndexed { index -> @Suppress("UNCHECKED_CAST") block(v[index] as V) }
    }

    /** Returns true if all entries match the given [predicate]. */
    public inline fun all(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (!predicate(key, value)) return false }
        return true
    }

    /** Returns true if at least one entry matches the given [predicate]. */
    public inline fun any(predicate: (K, V) -> Boolean): Boolean {
        forEach { key, value -> if (predicate(key, value)) return true }
        return false
    }

    /** Returns the number of entries in this cache. */
    public fun count(): Int = size

    /** Returns the number of entries matching the given [predicate]. */
    public inline fun count(predicate: (K, V) -> Boolean): Int {
        var count = 0
        forEach { key, value -> if (predicate(key, value)) count++ }
        return count
    }

    /** Returns true if the specified [key] is present in this cache, false otherwise. */
    public operator fun contains(key: K): Boolean = findKeyIndex(key) >= 0

    /** Returns true if the specified [key] is present in this cache, false otherwise. */
    public fun containsKey(key: K): Boolean = findKeyIndex(key) >= 0

    /** Returns true if the specified [value] is present in this hash map, false otherwise. */
    public fun containsValue(value: V): Boolean {
        val v = values
        forEachIndexed { index ->
            @Suppress("UNCHECKED_CAST") if (value == v[index] as V) return true
        }
        return false
    }

    private fun findEvictionCandidate(): Int {
        val nodes = nodes

        var candidate = if (hand != NodeInvalidLink) hand else tail
        while (candidate != NodeInvalidLink && nodes[candidate].visited != 0) {
            val node = nodes[candidate]
            val previousIndex = node.previousNode
            nodes[candidate] = clearVisitedBit(node)
            candidate = if (previousIndex != NodeInvalidLink) previousIndex else tail
        }

        val previousIndex = nodes[candidate].previousNode
        hand = if (previousIndex != NodeInvalidLink) previousIndex else NodeInvalidLink

        return candidate
    }

    private inline fun moveNodeToHead(index: Int) {
        nodes[index] = createLinkToNext(head)

        if (head != NodeInvalidLink) {
            nodes[head] = setLinkToPrevious(nodes[head], index)
        }
        head = index

        if (tail == NodeInvalidLink) {
            tail = index
        }
    }

    private fun removeValueAt(index: Int): V? {
        _count -= 1

        writeMetadata(metadata, _capacity, index, Deleted)

        keys[index] = null
        val previousValue = values[index]
        values[index] = null

        removeNode(index)

        @Suppress("UNCHECKED_CAST") return previousValue as V?
    }

    private inline fun removeNode(index: Int) {
        val nodes = nodes
        val node = nodes[index]
        val previousIndex = node.previousNode
        val nextIndex = node.nextNode

        if (previousIndex != NodeInvalidLink) {
            nodes[previousIndex] = setLinkToNext(nodes[previousIndex], nextIndex)
        } else {
            head = nextIndex
        }

        if (nextIndex != NodeInvalidLink) {
            nodes[nextIndex] = setLinkToPrevious(nodes[nextIndex], previousIndex)
        } else {
            tail = previousIndex
        }

        if (hand == index) {
            hand = previousIndex
        }
        nodes[index] = EmptyNode
    }

    private inline fun markNodeVisited(index: Int) {
        nodes[index] = (nodes[index] and NodeLinksMask) or NodeVisitedBit
    }

    private fun findKeyIndex(key: K): Int {
        val hash = hash(key)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = h1(hash) and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (keys[index] == key) {
                    return index
                }
                m = m.next()
            }

            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        return -1
    }

    private fun findInsertIndex(key: K): Int {
        val hash = hash(key)
        val hash1 = h1(hash)
        val hash2 = h2(hash)

        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            var m = g.match(hash2)
            while (m.hasNext()) {
                val index = (probeOffset + m.get()) and probeMask
                if (keys[index] == key) {
                    return index
                }
                m = m.next()
            }

            if (g.maskEmpty() != 0L) {
                break
            }

            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }

        var index = findFirstAvailableSlot(hash1)
        if (growthLimit == 0 && !isDeleted(metadata, index)) {
            adjustStorage()
            index = findFirstAvailableSlot(hash1)
        }

        _count += 1
        growthLimit -= if (isEmpty(metadata, index)) 1 else 0
        writeMetadata(metadata, _capacity, index, hash2.toLong())

        return index.inv()
    }

    private fun findFirstAvailableSlot(hash1: Int): Int {
        val probeMask = _capacity
        var probeOffset = hash1 and probeMask
        var probeIndex = 0

        while (true) {
            val g = group(metadata, probeOffset)
            val m = g.maskEmptyOrDeleted()
            if (m != 0L) {
                return (probeOffset + m.lowestBitSet()) and probeMask
            }
            probeIndex += GroupWidth
            probeOffset = (probeOffset + probeIndex) and probeMask
        }
    }

    // Internal to prevent inlining
    internal fun adjustStorage() {
        if (_capacity > GroupWidth && count.toULong() * 32UL <= _capacity.toULong() * 25UL) {
            dropDeletes()
        } else {
            resizeStorage(nextCapacity(_capacity))
        }
    }

    // Internal to prevent inlining
    internal fun dropDeletes() {
        val metadata = metadata
        // TODO: This shouldn't be required, but without it the compiler generates an extra
        //       200+ aarch64 instructions to generate a NullPointerException.
        @Suppress("SENSELESS_COMPARISON") if (metadata == null) return

        val capacity = _capacity
        val keys = keys
        val values = values
        val nodes = nodes

        val indexMapping = IntArray(capacity)

        // Converts Sentinel and Deleted to Empty, and Full to Deleted
        convertMetadataForCleanup(metadata, capacity)

        var swapIndex = -1
        var index = 0

        // Drop deleted items and re-hashes surviving entries
        while (index != capacity) {
            var m = readRawMetadata(metadata, index)
            // Formerly Deleted entry, we can use it as a swap spot
            if (m == Empty) {
                swapIndex = index
                index++
                continue
            }

            // Formerly Full entries are now marked Deleted. If we see an
            // entry that's not marked Deleted, we can ignore it completely
            if (m != Deleted) {
                index++
                continue
            }

            val hash = hash(keys[index])
            val hash1 = h1(hash)
            val targetIndex = findFirstAvailableSlot(hash1)

            // Test if the current index (i) and the new index (targetIndex) fall
            // within the same group based on the hash. If the group doesn't change,
            // we don't move the entry
            val probeOffset = hash1 and capacity
            val newProbeIndex = ((targetIndex - probeOffset) and capacity) / GroupWidth
            val oldProbeIndex = ((index - probeOffset) and capacity) / GroupWidth

            if (newProbeIndex == oldProbeIndex) {
                val hash2 = h2(hash)
                writeRawMetadata(metadata, index, hash2.toLong())

                indexMapping[index] = index

                // Copies the metadata into the clone area
                metadata[metadata.size - 1] = metadata[0]

                index++
                continue
            }

            m = readRawMetadata(metadata, targetIndex)
            if (m == Empty) {
                // The target is empty so we can transfer directly
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())
                writeRawMetadata(metadata, index, Empty)

                keys[targetIndex] = keys[index]
                keys[index] = null

                values[targetIndex] = values[index]
                values[index] = null

                nodes[targetIndex] = nodes[index]
                nodes[index] = EmptyNode

                indexMapping[index] = targetIndex

                swapIndex = index
            } else /* m == Deleted */ {
                // The target isn't empty so we use an empty slot denoted by
                // swapIndex to perform the swap
                val hash2 = h2(hash)
                writeRawMetadata(metadata, targetIndex, hash2.toLong())

                if (swapIndex == -1) {
                    swapIndex = findEmptySlot(metadata, index + 1, capacity)
                }

                keys[swapIndex] = keys[targetIndex]
                keys[targetIndex] = keys[index]
                keys[index] = keys[swapIndex]

                values[swapIndex] = values[targetIndex]
                values[targetIndex] = values[index]
                values[index] = values[swapIndex]

                nodes[swapIndex] = nodes[targetIndex]
                nodes[targetIndex] = nodes[index]
                nodes[index] = nodes[swapIndex]

                indexMapping[index] = targetIndex
                indexMapping[targetIndex] = index

                // Since we exchanged two slots we must repeat the process with
                // element we just moved in the current location
                index--
            }

            // Copies the metadata into the clone area
            metadata[metadata.size - 1] = metadata[0]

            index++
        }

        initializeGrowth()

        fixupNodes(indexMapping)
    }

    // Internal to prevent inlining
    internal fun resizeStorage(newCapacity: Int) {
        val previousMetadata = metadata
        val previousKeys = keys
        val previousValues = values
        val previousNodes = nodes
        val previousCapacity = _capacity

        val indexMapping = IntArray(previousCapacity)

        initializeStorage(newCapacity)

        val newMetadata = metadata
        val newKeys = keys
        val newValues = values
        val newNodes = nodes
        val capacity = _capacity

        for (i in 0 until previousCapacity) {
            if (isFull(previousMetadata, i)) {
                val previousKey = previousKeys[i]
                val hash = hash(previousKey)
                val index = findFirstAvailableSlot(h1(hash))

                writeMetadata(newMetadata, capacity, index, h2(hash).toLong())
                newKeys[index] = previousKey
                newValues[index] = previousValues[i]
                newNodes[index] = previousNodes[i]

                indexMapping[i] = index
            }
        }

        fixupNodes(indexMapping)
    }

    private fun fixupNodes(mapping: IntArray) {
        val nodes = nodes
        for (i in nodes.indices) {
            val node = nodes[i]
            val previous = node.previousNode
            val next = node.nextNode
            nodes[i] = createLinks(node, previous, next, mapping)
        }
        if (head != NodeInvalidLink) head = mapping[head]
        if (tail != NodeInvalidLink) tail = mapping[tail]
        if (hand != NodeInvalidLink) hand = mapping[hand]
    }

    @PublishedApi
    internal inline fun forEachIndexed(block: (index: Int) -> Unit) {
        val m = metadata
        val lastIndex = m.size - 2 // We always have 0 or at least 2 entries

        for (i in 0..lastIndex) {
            var slot = m[i]
            if (slot.maskEmptyOrDeleted() != BitmaskMsb) {
                // Branch-less if (i == lastIndex) 7 else 8
                // i - lastIndex returns a negative value when i < lastIndex,
                // so 1 is set as the MSB. By inverting and shifting we get
                // 0 when i < lastIndex, 1 otherwise.
                val bitCount = 8 - ((i - lastIndex).inv() ushr 31)
                for (j in 0 until bitCount) {
                    if (isFull(slot and 0xffL)) {
                        val index = (i shl 3) + j
                        block(index)
                    }
                    slot = slot shr 8
                }
                if (bitCount != 8) return
            }
        }
    }

    /**
     * Returns the hash code value for this cache. The hash code the sum of the hash codes of each
     * key/value pair.
     */
    public override fun hashCode(): Int {
        var hash = 0

        forEach { key, value -> hash += key.hashCode() xor value.hashCode() }

        return hash
    }

    /**
     * Compares the specified object [other] with this cache for equality. The two objects are
     * considered equal if [other]:
     * - Is a [SieveCache]
     * - Has the same [size] and [count] as this cache
     * - Contains key/value pairs equal to this cache's pair
     */
    public override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is SieveCache<*, *>) {
            return false
        }
        if (other.size != size || other._count != _count) {
            return false
        }

        @Suppress("UNCHECKED_CAST") val o = other as SieveCache<Any, Any>

        forEach { key, value ->
            if (value != o[key]) {
                return false
            }
        }

        return true
    }

    override fun toString(): String {
        return "SieveCache[maxSize=$_maxSize, size=$_size, capacity=$_capacity, count=$_count]"
    }
}

internal inline fun createLinks(node: Long, previous: Int, next: Int, mapping: IntArray): Long {
    return (node and NodeMetaMask) or
        (if (previous == NodeInvalidLink) NodeInvalidLink else mapping[previous]).toLong() shl
        31 or
        (if (next == NodeInvalidLink) NodeInvalidLink else mapping[next]).toLong()
}

// set meta to 0 (visited = false) and previous to NodeInvalidLink
internal inline fun createLinkToNext(next: Int) =
    0x3fffffff_80000000L or (next.toLong() and NodeLinkMask)

internal inline fun setLinkToPrevious(node: Long, previous: Int) =
    (node and NodeMetaAndNextMask) or ((previous.toLong() and NodeLinkMask) shl 31)

internal inline fun setLinkToNext(node: Long, next: Int) =
    (node and NodeMetaAndPreviousMask) or (next.toLong() and NodeLinkMask)

internal inline fun clearVisitedBit(node: Long) = node and NodeLinksMask

@PublishedApi
internal inline val Long.previousNode: Int
    get() = ((this shr 31) and NodeLinkMask).toInt()

@PublishedApi
internal inline val Long.nextNode: Int
    get() = (this and NodeLinkMask).toInt()

internal inline val Long.visited: Int
    get() = ((this shr 62) and 0x1).toInt()
