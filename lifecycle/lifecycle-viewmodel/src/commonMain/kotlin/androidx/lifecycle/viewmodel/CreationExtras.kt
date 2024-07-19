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
package androidx.lifecycle.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.CreationExtras.Key
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * A map-like object holding pairs of [CreationExtras.Key] and [Any], enabling efficient value
 * retrieval for each key. Each key in [CreationExtras] is unique, storing only one value per key.
 *
 * [CreationExtras] is used in [ViewModelProvider.Factory.create] to provide extra information to
 * the [Factory]. This makes [Factory] implementations stateless, simplifying factory injection by
 * not requiring all information at construction time.
 *
 * This abstract class supports read-only access; use [MutableCreationExtras] for read-write access.
 */
public abstract class CreationExtras internal constructor() {
    internal val extras: MutableMap<Key<*>, Any?> = mutableMapOf()

    /**
     * Key for the elements of [CreationExtras]. [T] represents the type of element associated with
     * this key.
     */
    public interface Key<T>

    /**
     * Returns the value to which the specified [key] is associated, or null if this
     * [CreationExtras] contains no mapping for the key.
     */
    public abstract operator fun <T> get(key: Key<T>): T?

    /** Compares the specified object with this [CreationExtras] for equality. */
    override fun equals(other: Any?): Boolean = other is CreationExtras && extras == other.extras

    /** Returns the hash code value for this [CreationExtras]. */
    override fun hashCode(): Int = extras.hashCode()

    /**
     * Returns a string representation of this [CreationExtras]. The string representation consists
     * of a list of key-value mappings in the order returned by the [CreationExtras]'s iterator.
     */
    override fun toString(): String = "CreationExtras(extras=$extras)"

    /** An empty read-only [CreationExtras]. */
    public object Empty : CreationExtras() {
        override fun <T> get(key: Key<T>): T? = null
    }

    public companion object {
        /** Returns an unique [Key] to be associated with an extra. */
        @JvmStatic public inline fun <reified T> Key(): Key<T> = object : Key<T> {}
    }
}

/**
 * A modifiable [CreationExtras] that holds pairs of [CreationExtras.Key] and [Any], allowing
 * efficient value retrieval for each key.
 *
 * Each key in [CreationExtras] is unique, storing only one value per key.
 *
 * @see [CreationExtras]
 */
public class MutableCreationExtras
/**
 * Constructs a [MutableCreationExtras] containing the elements of the specified `initialExtras`, in
 * the order they are returned by the [Map]'s iterator.
 */
internal constructor(initialExtras: Map<Key<*>, Any?>) : CreationExtras() {

    /**
     * Constructs a [MutableCreationExtras] containing the elements of the specified
     * [initialExtras], in the order they are returned by the [CreationExtras]'s iterator.
     */
    @JvmOverloads
    public constructor(initialExtras: CreationExtras = Empty) : this(initialExtras.extras)

    init {
        extras += initialExtras
    }

    /** Associates the specified [t] with the specified [key] in this [CreationExtras]. */
    public operator fun <T> set(key: Key<T>, t: T) {
        extras[key] = t
    }

    /**
     * Returns the value to which the specified [key] is associated, or null if this
     * [CreationExtras] contains no mapping for the key.
     */
    @Suppress("UNCHECKED_CAST") public override fun <T> get(key: Key<T>): T? = extras[key] as T?
}

/**
 * Checks if the [CreationExtras] contains the given [key].
 *
 * This method allows to use the `key in creationExtras` syntax for checking whether an [key] is
 * contained in the [CreationExtras].
 */
public operator fun CreationExtras.contains(key: Key<*>): Boolean = key in extras

/**
 * Creates a new read-only [CreationExtras] by replacing or adding entries to [this] extras from
 * another [creationExtras].
 *
 * The returned [CreationExtras] preserves the entry iteration order of the original
 * [CreationExtras].
 *
 * Those entries of another [creationExtras] that are missing in [this] extras are iterated in the
 * end in the order of that [creationExtras].
 */
public operator fun CreationExtras.plus(creationExtras: CreationExtras): MutableCreationExtras =
    MutableCreationExtras(initialExtras = extras + creationExtras.extras)

/** Appends or replaces all entries from the given [creationExtras] in [this] mutable extras. */
public operator fun MutableCreationExtras.plusAssign(creationExtras: CreationExtras) {
    extras += creationExtras.extras
}
