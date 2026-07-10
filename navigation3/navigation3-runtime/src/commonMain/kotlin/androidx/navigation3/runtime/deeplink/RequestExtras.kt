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

import kotlin.collections.contains

@DslMarker public annotation class RequestExtrasDsl

/**
 * Provides a [RequestExtrasScope] to build a Map<String, Any> of extra information for a
 * [DeepLinkRequest].
 *
 * @param builder the DSL extension that provides a [RequestExtrasScope] to build a Map<String, Any>
 *   of [DeepLinkRequest] extras.
 */
public inline fun requestExtras(builder: RequestExtrasScope.() -> Unit): Map<String, Any> =
    RequestExtrasScope().apply(builder).build()

/**
 * The base Key associated with a value of type [T].
 *
 * All keys used for storing extras with the [requestExtras] DSL must implement this interface.
 */
public interface RequestExtrasKey<T : Any>

/** Scope provided to the [requestExtras] dsl builder. */
@RequestExtrasDsl
public class RequestExtrasScope {
    private val map: MutableMap<String, Any> = mutableMapOf()

    /**
     * Adds the key and value pair to the extras map.
     *
     * [T] the [value] type.
     *
     * @param key the key associated with the [value]
     * @param value the data to be added to the map of metadata
     */
    public fun <T : Any> put(key: RequestExtrasKey<T>, value: T) {
        map[key.toString()] = value
    }

    @PublishedApi internal fun build(): Map<String, Any> = map
}

/**
 * Returns the value for the given [key].
 *
 * Should be used to retrieve values from [DeepLinkRequest.extras].
 *
 * [T] the value type.
 *
 * @param key the key associated with the value
 */
@Suppress("UNCHECKED_CAST")
public operator fun <T : Any> Map<String, Any>.get(key: RequestExtrasKey<T>): T? =
    get(key.toString()) as? T

/**
 * Checks if this map contains a value for the given [key].
 *
 * Returns true if the map contains an entry with the given [key], false otherwise.
 *
 * Should be used to check entries from [DeepLinkRequest.extras].
 *
 * [T] the value type.
 *
 * @param key the key associated with the value
 */
public operator fun <T : Any> Map<String, Any>.contains(key: RequestExtrasKey<T>): Boolean =
    contains(key.toString())
