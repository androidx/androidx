/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.navigation3.runtime.deeplink

@DslMarker public annotation class RequestExtrasDsl

/**
 * Provides a [RequestExtrasScope] to build a [RequestExtras] for a [DeepLinkRequest].
 *
 * @param builder the DSL that provides a [RequestExtrasScope] to build a [RequestExtras]
 */
public inline fun requestExtras(builder: RequestExtrasScope.() -> Unit): RequestExtras =
    RequestExtrasScope().apply(builder).build()

/**
 * The base Key associated with a value of type [T].
 *
 * [RequestExtras] keys must implement this interface.
 */
public interface RequestExtrasKey<T : Any>

/** Scope provided to the [requestExtras] dsl builder. */
@RequestExtrasDsl
public class RequestExtrasScope @PublishedApi internal constructor() {
    private val map = mutableMapOf<RequestExtrasKey<*>, Any>()

    /**
     * Stores [value] associated with [key].
     *
     * @param key key associated with value type [T]
     * @param value data to store
     */
    public fun <T : Any> put(key: RequestExtrasKey<T>, value: T) {
        map[key] = value
    }

    @PublishedApi internal fun build(): RequestExtras = RequestExtras(map)
}

/**
 * A map-like class that stores key-value pairs of [RequestExtrasKey] to its associated value for
 * use as [DeepLinkRequest.extras].
 *
 * Construct with [emptyRequestExtras] or with [requestExtras] DSL.
 */
public class RequestExtras
internal constructor(private val internalMap: Map<RequestExtrasKey<*>, Any> = mutableMapOf()) {
    /** Number of key-value pairs stored. */
    public val size: Int
        get() = internalMap.size

    /** Returns `true` if empty, `false` otherwise. */
    public fun isEmpty(): Boolean = internalMap.isEmpty()

    /** Returns `true` if not empty, `false` otherwise. */
    public fun isNotEmpty(): Boolean = internalMap.isNotEmpty()

    /**
     * Returns the value for [key], or `null` if absent.
     *
     * @param key key associated with value type [T]
     * @return the value of type [T], or `null`
     */
    @Suppress("UNCHECKED_CAST")
    public operator fun <T : Any> get(key: RequestExtrasKey<T>): T? = internalMap[key] as? T

    /**
     * Checks if a value is present for [key].
     *
     * @param key key to check
     * @return `true` if present, `false` otherwise
     */
    public operator fun contains(key: RequestExtrasKey<*>): Boolean = internalMap.containsKey(key)

    /**
     * Returns a new [RequestExtras] containing all entries of this instance plus entries from
     * [other]. Entries in [other] take precedence over duplicate keys.
     */
    public operator fun plus(other: RequestExtras): RequestExtras =
        RequestExtras((internalMap + other.internalMap).toMutableMap())

    /**
     * Returns a new [RequestExtras] containing all entries of this instance except keys in [other].
     */
    public operator fun minus(other: RequestExtras): RequestExtras =
        RequestExtras(internalMap - other.internalMap.keys)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RequestExtras) return false
        return internalMap == other.internalMap
    }

    override fun hashCode(): Int = internalMap.hashCode()

    override fun toString(): String = "RequestExtras($internalMap)"
}

/** Returns an empty [RequestExtras]. */
public fun emptyRequestExtras(): RequestExtras = RequestExtras()
