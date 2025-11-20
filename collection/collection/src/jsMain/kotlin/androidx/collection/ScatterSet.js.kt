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

import androidx.annotation.IntRange

public actual sealed class ScatterSet<E>
protected constructor(@PublishedApi internal val delegate: MutableSet<E>) {
    @get:IntRange(from = 0)
    public actual val capacity: Int
        get() = 0

    @get:IntRange(from = 0)
    public actual val size: Int
        get() = delegate.size

    public actual fun any(): Boolean = size != 0

    public actual fun none(): Boolean = size == 0

    public actual fun isEmpty(): Boolean = size == 0

    public actual fun isNotEmpty(): Boolean = size != 0

    public actual fun first(): E = delegate.first()

    public actual inline fun first(predicate: (element: E) -> Boolean): E {
        return delegate.first(predicate)
    }

    public actual inline fun firstOrNull(predicate: (element: E) -> Boolean): E? {
        return delegate.firstOrNull(predicate)
    }

    public actual inline fun forEach(block: (element: E) -> Unit) {
        // We must support removal during iteration, so a copy is required.
        delegate.toSet().forEach(block)
    }

    public actual inline fun all(predicate: (element: E) -> Boolean): Boolean {
        return delegate.all(predicate)
    }

    public actual inline fun any(predicate: (element: E) -> Boolean): Boolean {
        return delegate.any(predicate)
    }

    @IntRange(from = 0) public actual fun count(): Int = size

    @IntRange(from = 0)
    public actual inline fun count(predicate: (element: E) -> Boolean): Int {
        return delegate.count(predicate)
    }

    public actual operator fun contains(element: E): Boolean {
        return delegate.contains(element)
    }

    public actual fun joinToString(
        separator: CharSequence,
        prefix: CharSequence,
        postfix: CharSequence,
        limit: Int,
        truncated: CharSequence,
        transform: ((E) -> CharSequence)?,
    ): String {
        return delegate.joinToString(
            separator = separator,
            prefix = prefix,
            postfix = postfix,
            limit = limit,
            truncated = truncated,
            transform = transform,
        )
    }

    actual override fun hashCode(): Int = delegate.hashCode()

    actual override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is ScatterSet<*> && delegate == other.delegate
    }

    actual override fun toString(): String =
        joinToString(prefix = "[", postfix = "]") { element ->
            if (element === this) {
                "(this)"
            } else {
                element.toString()
            }
        }

    public actual fun asSet(): Set<E> = SetWrapper(this, delegate)
}

public actual class MutableScatterSet<E> private constructor(delegate: MutableSet<E>) :
    ScatterSet<E>(delegate) {

    public actual constructor(initialCapacity: Int) : this(HashSet(initialCapacity))

    public actual fun add(element: E): Boolean {
        return delegate.add(element)
    }

    public actual operator fun plusAssign(element: E) {
        delegate.plusAssign(element)
    }

    public actual fun addAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        return delegate.addAll(elements)
    }

    public actual fun addAll(elements: Iterable<E>): Boolean {
        return delegate.addAll(elements)
    }

    public actual fun addAll(elements: Sequence<E>): Boolean {
        return delegate.addAll(elements)
    }

    public actual fun addAll(elements: ScatterSet<E>): Boolean {
        return delegate.addAll(elements.delegate)
    }

    public actual fun addAll(elements: OrderedScatterSet<E>): Boolean {
        return delegate.addAll(elements.asSet())
    }

    public actual fun addAll(elements: ObjectList<E>): Boolean {
        return delegate.addAll(elements.asList())
    }

    public actual operator fun plusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        delegate.plusAssign(elements)
    }

    public actual operator fun plusAssign(elements: Iterable<E>) {
        delegate.plusAssign(elements)
    }

    public actual operator fun plusAssign(elements: Sequence<E>) {
        delegate.plusAssign(elements)
    }

    public actual operator fun plusAssign(elements: ScatterSet<E>) {
        delegate.plusAssign(elements.delegate)
    }

    public actual operator fun plusAssign(elements: OrderedScatterSet<E>) {
        delegate.plusAssign(elements.asSet())
    }

    public actual operator fun plusAssign(elements: ObjectList<E>) {
        delegate.plusAssign(elements.asList())
    }

    public actual fun remove(element: E): Boolean {
        return delegate.remove(element)
    }

    public actual operator fun minusAssign(element: E) {
        delegate.minusAssign(element)
    }

    public actual fun removeAll(@Suppress("ArrayReturn") elements: Array<out E>): Boolean {
        return delegate.removeAll(elements)
    }

    public actual fun removeAll(elements: Sequence<E>): Boolean {
        return delegate.removeAll(elements)
    }

    public actual fun removeAll(elements: Iterable<E>): Boolean {
        return delegate.removeAll(elements)
    }

    public actual fun removeAll(elements: ScatterSet<E>): Boolean {
        return delegate.removeAll(elements.delegate)
    }

    public actual fun removeAll(elements: OrderedScatterSet<E>): Boolean {
        return delegate.removeAll(elements.asSet())
    }

    public actual fun removeAll(elements: ObjectList<E>): Boolean {
        return delegate.removeAll(elements.asList())
    }

    public actual operator fun minusAssign(@Suppress("ArrayReturn") elements: Array<out E>) {
        delegate.minusAssign(elements)
    }

    public actual operator fun minusAssign(elements: Sequence<E>) {
        delegate.minusAssign(elements)
    }

    public actual operator fun minusAssign(elements: Iterable<E>) {
        delegate.minusAssign(elements)
    }

    public actual operator fun minusAssign(elements: ScatterSet<E>) {
        delegate.minusAssign(elements.delegate)
    }

    public actual operator fun minusAssign(elements: OrderedScatterSet<E>) {
        delegate.minusAssign(elements.asSet())
    }

    public actual operator fun minusAssign(elements: ObjectList<E>) {
        delegate.minusAssign(elements.asList())
    }

    public actual inline fun removeIf(predicate: (E) -> Boolean) {
        val i = delegate.iterator()
        while (i.hasNext()) {
            if (predicate(i.next())) {
                i.remove()
            }
        }
    }

    public actual fun retainAll(elements: Collection<E>): Boolean {
        return delegate.retainAll(elements)
    }

    public actual fun retainAll(elements: ScatterSet<E>): Boolean {
        return delegate.retainAll(elements.delegate)
    }

    public actual fun retainAll(elements: OrderedScatterSet<E>): Boolean {
        return delegate.retainAll(elements.asSet())
    }

    public actual inline fun retainAll(predicate: (E) -> Boolean): Boolean {
        val i = delegate.iterator()
        var removed = false
        while (i.hasNext()) {
            if (!predicate(i.next())) {
                i.remove()
                removed = true
            }
        }
        return removed
    }

    public actual fun clear() {
        delegate.clear()
    }

    @IntRange(from = 0)
    public actual fun trim(): Int {
        return 0
    }

    public actual fun asMutableSet(): MutableSet<E> = MutableSetWrapper(this, delegate)
}

private class SetWrapper<E>(private val scatterSet: ScatterSet<E>, private val set: Set<E>) :
    Set<E> by set {
    override fun hashCode() = set.hashCode()

    override fun equals(other: Any?) = set == other

    override fun toString() = scatterSet.toString()
}

private class MutableSetWrapper<E>(
    private val scatterSet: MutableScatterSet<E>,
    private val set: MutableSet<E>,
) : MutableSet<E> by set {
    override fun hashCode() = set.hashCode()

    override fun equals(other: Any?) = set == other

    override fun toString() = scatterSet.toString()
}
