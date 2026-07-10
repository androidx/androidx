/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle

import androidx.collection.MutableScatterMap

/**
 * An ordered, map-based collection specifically designed for the Android Lifecycle observer
 * pattern.
 *
 * **The Problem:** Lifecycle observers frequently trigger events that cause other observers to be
 * added or removed while the collection is being iterated. Standard collections like
 * `LinkedHashMap` throw `ConcurrentModificationException` or require expensive iterator allocations
 * to handle this.
 *
 * **The Solution:** This map uses an intrusive doubly-linked list. When an entry is removed, it is
 * unlinked from the main chain but **retains its next/prev pointers**. This creates "Ghost Nodes"
 * that allow active iterators to safely traverse back to the live list without complex state
 * tracking or index shifting.
 *
 * Constraints:
 * - NOT thread-safe.
 * - Optimized for high-frequency iteration and zero-allocation modification.
 */
internal class FastSafeIterableMap<K : Any, V : Any> {

    /** Lookup table for O(1) access. Stores the intrusive entries. */
    private val map = MutableScatterMap<K, Entry<K, V>>()

    /** The start of the intrusive linked list. Used as the entry point for forward iteration. */
    private var head: Entry<K, V>? = null

    /**
     * The end of the intrusive linked list. New entries are appended here to maintain insertion
     * order.
     */
    private var tail: Entry<K, V>? = null

    operator fun contains(key: K): Boolean = map.containsKey(key)

    /**
     * Adds a value only if the key is not already present. Appends to the tail to ensure iteration
     * follows insertion order.
     */
    fun putIfAbsent(key: K, value: V): V? {
        val existing = map[key]
        if (existing != null) {
            return existing.value
        }

        val newEntry = Entry(key, value)
        map[key] = newEntry

        if (tail == null) {
            head = newEntry
            tail = newEntry
        } else {
            tail?.next = newEntry
            newEntry.prev = tail
            tail = newEntry
        }
        return null
    }

    /**
     * Removes the entry and marks it as a "Ghost Node".
     *
     * We intentionally do not null out the entry's [Entry.next] and [Entry.prev] pointers. If a
     * loop is currently processing this entry, those pointers serve as the "bridge" back to the
     * remaining elements in the map.
     */
    fun remove(key: K): V? {
        val entry = map.remove(key) ?: return null

        // Unlink from the live chain.
        if (entry.prev == null) head = entry.next else entry.prev?.next = entry.next
        if (entry.next == null) tail = entry.prev else entry.next?.prev = entry.prev

        // Marks this node as dead. Iterators check this flag to skip removed
        // elements that are still in their traversal path.
        entry.markRemoved()

        return entry.value
    }

    /**
     * Returns the entry that was inserted immediately before the entry associated with the given
     * [key], or null if no such entry exists.
     *
     * Note: Despite the name 'ceil', this retrieves the logical predecessor to support specific
     * Lifecycle iteration patterns.
     */
    fun ceil(key: K): Map.Entry<K, V>? = map[key]?.prev

    /**
     * Returns the first entry in the map.
     *
     * @throws IllegalArgumentException if the map is empty.
     */
    fun first(): Map.Entry<K, V> = head ?: throw NoSuchElementException("Collection is empty.")

    /**
     * Returns the last entry in the map.
     *
     * @throws IllegalArgumentException if the map is empty.
     */
    fun last(): Map.Entry<K, V> = tail ?: throw NoSuchElementException("Collection is empty.")

    fun lastOrNull(): Map.Entry<K, V>? = tail

    /** Current count of live (non-removed) elements. */
    fun size(): Int = map.size

    /**
     * Iterates forward through the map. Safe to add or remove elements (including the current one)
     * during execution.
     *
     * New elements added during iteration will be visited if they are appended after the current
     * position.
     */
    fun forEachWithAdditions(action: (Map.Entry<K, V>) -> Unit) {
        var current = head
        while (current != null) {
            // We check isRemoved because a previous step in this loop might
            // have removed 'current' or a future element we haven't reached yet.
            if (!current.isRemoved) {
                action(current)
            }
            // Move to next AFTER action. If current was removed, its 'next'
            // pointer still acts as a bridge to the rest of the list.
            current = current.next
        }
    }

    /**
     * Iterates in reverse order. Modification-safe via the same 'Ghost Node' logic used in forward
     * iteration.
     */
    fun forEachReversed(action: (Map.Entry<K, V>) -> Unit) {
        var current = tail
        while (current != null) {
            // We check isRemoved because a previous step in this loop might
            // have removed 'current' or a future element we haven't reached yet.
            if (!current.isRemoved) {
                action(current)
            }

            // Move to previous AFTER action. If current was removed, its 'previous'
            // pointer still acts as a bridge to the rest of the list.
            current = current.prev
        }
    }

    /** An intrusive doubly-linked list node that also serves as a [Map.Entry]. */
    private data class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V> {

        // Note: We retain next/prev pointers even after removal to allow active
        // iterators to "bridge" back to the live list from this node.
        var next: Entry<K, V>? = null
        var prev: Entry<K, V>? = null

        /**
         * Indicates this entry is no longer in the Map. Once true, it can never be set back to
         * false.
         */
        var isRemoved: Boolean = false
            private set

        /**
         * Marks this entry as a "Ghost Node." It remains linked to its neighbors to support
         * iterator safety, but will be skipped during traversal.
         */
        fun markRemoved() {
            isRemoved = true
        }
    }
}
