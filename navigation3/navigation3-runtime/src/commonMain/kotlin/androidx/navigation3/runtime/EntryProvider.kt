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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmSuppressWildcards
import kotlin.reflect.KClass

@DslMarker public annotation class EntryDsl

/**
 * Provides a [EntryProviderScope] to build an entryProvider that provides NavEntries.
 *
 * @param [T] the type of the [NavEntry] key
 * @param fallback the fallback [NavEntry] when the provider cannot find an entry associated with a
 *   given key on the backStack
 * @param builder the DSL extension that provides a [EntryProviderScope] to build an entryProvider
 *   that provides NavEntries.
 * @return an entryProvider that provides the NavEntry associated with a given key
 */
public inline fun <T : Any> entryProvider(
    noinline fallback: (unknownScreen: T) -> NavEntry<T> = {
        throw IllegalStateException("Unknown screen $it")
    },
    builder: EntryProviderScope<T>.() -> Unit,
): (T) -> NavEntry<T> = EntryProviderScope(fallback).apply(builder).build()

/**
 * The scope for constructing a new [NavEntry] with Kotlin DSL
 *
 * @param [T] the type of the [NavEntry] key
 * @param fallback the fallback [NavEntry] when the provider cannot find an entry associated with a
 *   given key on the backStack
 */
@EntryDsl
public class EntryProviderScope<T : Any>(private val fallback: (unknownScreen: T) -> NavEntry<T>) {
    private val clazzProviders = mutableMapOf<KClass<out T>, EntryClassProvider<out T>>()
    private val providers = mutableMapOf<Any, EntryProvider<out T>>()

    /**
     * Builds a [NavEntry] for the given [key] that displays [content].
     *
     * @param K the type of the key for this NavEntry
     * @param key key for this entry
     * @param contentKey A unique, stable id that uniquely identifies the content of this NavEntry.
     *   To maximize stability, it should be derived from the [key]. The contentKey type must be
     *   saveable (i.e. on Android, it should be saveable via Android). Defaults to
     *   [key].toString().
     * @param metadata provides information to the display
     * @param content content for this entry to be displayed when this entry is active
     */
    public fun <K : T> addEntryProvider(
        key: K,
        contentKey: Any = defaultContentKey(key),
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (K) -> Unit,
    ) {
        require(key !in providers) {
            "An `entry` with the key `key` has already been added: ${key}."
        }
        providers[key] = EntryProvider(key, contentKey, metadata, content)
    }

    /**
     * Add an entry provider to the [EntryProviderScope]
     *
     * @param K the type of the key for this NavEntry
     * @param key key for this entry
     * @param contentKey A unique, stable id that uniquely identifies the content of this NavEntry.
     *   To maximize stability, it should be derived from the [key]. The contentKey type must be
     *   saveable (i.e. on Android, it should be saveable via Android). Defaults to
     *   [key].toString().
     * @param metadata provides information to the display
     * @param content content for this entry to be displayed when this entry is active
     */
    public fun <K : T> EntryProviderScope<T>.entry(
        key: K,
        contentKey: Any = defaultContentKey(key),
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (K) -> Unit,
    ) {
        addEntryProvider(key, contentKey, metadata, content)
    }

    /**
     * Builds a [NavEntry] for the given [clazz] that displays [content].
     *
     * @param K the type of the key for this NavEntry
     * @param clazz the KClass<T> of the key for this NavEntry
     * @param clazzContentKey A factory of unique, stable ids that uniquely identifies the content
     *   of this NavEntry. To maximize stability, it should be derived from the factory's provided
     *   key. The resulting key must be saveable (i.e. on Android, it should be saveable via
     *   Android). The generated key will be stored in [NavEntry.contentKey].
     * @param metadata provides information to the display
     * @param content content for this entry to be displayed when this entry is active
     */
    public fun <K : T> addEntryProvider(
        clazz: KClass<out K>,
        clazzContentKey: (key: @JvmSuppressWildcards K) -> Any = { defaultContentKey(it) },
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (K) -> Unit,
    ) {
        require(clazz !in clazzProviders) {
            "An `entry` with the same `clazz` has already been added: ${clazz.simpleName}."
        }
        clazzProviders[clazz] = EntryClassProvider(clazz, clazzContentKey, metadata, content)
    }

    /**
     * Add an entry provider to the [EntryProviderScope]
     *
     * @param K the type of the key for this NavEntry
     * @param clazzContentKey A factory of unique, stable ids that uniquely identifies the content
     *   of this NavEntry. To maximize stability, it should be derived from the factory's provided
     *   key. The resulting key must be saveable (i.e. on Android, it should be saveable via
     *   Android). The generated key will be stored in [NavEntry.contentKey].
     * @param metadata provides information to the display
     * @param content content for this entry to be displayed when this entry is active
     */
    public inline fun <reified K : T> entry(
        noinline clazzContentKey: (key: @JvmSuppressWildcards K) -> Any = { defaultContentKey(it) },
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (K) -> Unit,
    ) {
        addEntryProvider(K::class, clazzContentKey, metadata, content)
    }

    /**
     * Returns an instance of entryProvider created from the entry providers set on this builder.
     */
    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun build(): (T) -> NavEntry<T> = { key ->
        val entryClassProvider = clazzProviders[key::class] as? EntryClassProvider<T>
        val entryProvider = providers[key] as? EntryProvider<T>
        entryClassProvider?.run { NavEntry(key, clazzContentKey(key), metadata, content) }
            ?: entryProvider?.run { NavEntry(key, contentKey, metadata, content) }
            ?: fallback.invoke(key)
    }
}

/**
 * Holds a Entry class, metadata, and content for that class
 *
 * @param K the type of the key for this NavEntry
 * @param clazz the KClass<T> of the key for this NavEntry
 * @param clazzContentKey A factory of unique, stable ids that uniquely identifies the content of
 *   this NavEntry. To maximize stability, it should be derived from the factory's provided key. The
 *   resulting key must be saveable (i.e. on Android, it should be saveable via Android). The
 *   generated key will be stored in [NavEntry.contentKey].
 * @param metadata provides information to the display
 * @param content content for this entry to be displayed when this entry is active
 */
@Suppress("DataClassDefinition")
private data class EntryClassProvider<K : Any>(
    val clazz: KClass<K>,
    val clazzContentKey: (key: K) -> Any,
    val metadata: Map<String, Any>,
    val content: @Composable (K) -> Unit,
)

/**
 * Holds a Entry class, metadata, and content for that key
 *
 * @param K the type of the key for this NavEntry
 * @param key key for this entry
 * @param contentKey A unique, stable id that uniquely identifies the content of this NavEntry. To
 *   maximize stability, it should be derived from the [key]. The contentKey type must be saveable
 *   (i.e. on Android, it should be saveable via Android). Defaults to [key].toString().
 * @param metadata provides information to the display
 * @param content content for this entry to be displayed when this entry is active
 */
@Suppress("DataClassDefinition")
private data class EntryProvider<K : Any>(
    val key: K,
    val contentKey: Any,
    val metadata: Map<String, Any>,
    val content: @Composable (K) -> Unit,
)
