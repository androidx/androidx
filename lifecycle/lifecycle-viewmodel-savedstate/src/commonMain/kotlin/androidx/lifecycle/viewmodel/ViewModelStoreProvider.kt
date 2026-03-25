/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.lifecycle.viewmodel

import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.collection.mutableScatterMapOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.savedState
import kotlin.jvm.JvmOverloads

/**
 * Manages a set of [ViewModelStore] instances scoped to a parent [ViewModelStore].
 *
 * This class allows the creation of "child" scopes that survive configuration changes (via the
 * parent) but can be independently cleared when no longer needed.
 *
 * **Important:** This class prevents a child [ViewModel] from being cleared while they are still in
 * use (e.g., during exit animations). Consumers must call [acquireToken] to mark a child
 * [ViewModelStore] as active and then call [ReferenceToken.close] to release the token when
 * finished. Calling [clearKey] or [clearAllKeys] will only perform the actual cleanup once all of a
 * store's tokens have been released.
 *
 * **Null parentStore:** If [parentStore] is **EXPLICITLY** `null`, this creates a root provider
 * that runs independently. It manages its own state and will not be automatically cleared by
 * configuration changes; you must manually call [clearAllKeys] to clean it up.
 *
 * @param parentStore The parent [ViewModelStore] to bind to, or `null` for an independent root
 *   provider.
 * @param parentKey A unique identifier used to scope this provider and its underlying state within
 *   the [parentStore]. Providers sharing the same key will share the same internal state. A `null`
 *   key is valid and acts as its own distinct shared scope. Defaults to `null`.
 * @param defaultArgs The default [SavedState] arguments to use for child stores. These arguments
 *   are merged with any default arguments in [defaultCreationExtras]. If the same key exists in
 *   both, the value from [defaultArgs] takes precedence.
 * @param defaultCreationExtras The default creation extras to use for child stores.
 * @param defaultFactory The default factory to use for child stores.
 */
public class ViewModelStoreProvider
@JvmOverloads
public constructor(
    private val parentStore: ViewModelStore?,
    private val parentKey: Any? = null,
    private val defaultArgs: SavedState = savedState(),
    private val defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    private val defaultFactory: Factory = SavedStateViewModelFactory(),
) {

    /**
     * Constructs a [ViewModelStoreProvider] bound to a parent [ViewModelStoreOwner].
     *
     * @param parentOwner The parent [ViewModelStoreOwner] to bind to, or `null` for an independent
     *   root provider.
     * @param parentKey A unique identifier used to scope this provider and its underlying state
     *   within the [parentOwner]. Providers sharing the same key will share the same internal
     *   state. A `null` key is valid and acts as its own distinct shared scope. Defaults to `null`.
     * @param defaultArgs The default [SavedState] arguments to use for child stores. These
     *   arguments are merged with any default arguments in [defaultCreationExtras]. If the same key
     *   exists in both, the value from [defaultArgs] takes precedence.
     * @param defaultCreationExtras The default creation extras to use for child stores. Defaults to
     *   resolving from the [parentOwner].
     * @param defaultFactory The default factory to use for child stores. Defaults to resolving from
     *   the [parentOwner].
     */
    @JvmOverloads
    public constructor(
        parentOwner: ViewModelStoreOwner?,
        parentKey: Any? = null,
        defaultArgs: SavedState = savedState(),
        defaultCreationExtras: CreationExtras = parentOwner.defaultViewModelCreationExtras,
        defaultFactory: Factory = parentOwner.defaultViewModelProviderFactory,
    ) : this(
        parentStore = parentOwner?.viewModelStore,
        parentKey,
        defaultArgs,
        defaultCreationExtras,
        defaultFactory,
    )

    private val stateHolder: StateHolder by lazy {
        // If store exists, delegate state persistence to it (survives config changes). If store is
        // null (Root), we hold the state directly (survives as long as this Provider exists).
        parentStore?.getOrPut(parentKey) { StateHolder() } ?: StateHolder()
    }

    /**
     * Increments the reference count for the [ViewModelStore] associated with the given [key],
     * ensuring it is not cleared until the returned [ReferenceToken] is released.
     *
     * @param key The unique identifier for the child scope. A `null` key is valid and is treated as
     *   a distinct scope.
     * @return A token that must be released via [ReferenceToken.close] when the caller no longer
     *   requires the store.
     */
    public fun acquireToken(key: Any?): ReferenceToken {
        val entry = stateHolder.getOrCreate(key)
        entry.refCount++
        return ReferenceToken {
            entry.refCount--
            if (entry.isDisposable && entry.refCount <= 0) {
                stateHolder.remove(key)
            }
        }
    }

    /**
     * Retrieves or creates a [ViewModelStore] associated with the given [key].
     *
     * If a store with this key already exists, it is returned. If not, a new store is created. To
     * protect this store from being prematurely cleared, you must call [acquireToken].
     *
     * @param key The unique identifier for the child scope. A `null` key is valid and is treated as
     *   a distinct scope.
     * @return The [ViewModelStore] tied to the provided key.
     */
    public fun getOrCreate(key: Any?): ViewModelStore {
        return stateHolder.getOrCreate(key).store
    }

    /**
     * Retrieves or creates a [ViewModelStoreOwner] associated with the given [key].
     *
     * This method creates a new lightweight wrapper around the [ViewModelStore].
     *
     * **Important:** This does *not* automatically increment the reference count. If you are
     * holding onto this owner asynchronously or across recompositions, you should call
     * [acquireToken] to protect its lifecycle.
     *
     * **Saved State Support:** If a [savedStateRegistryOwner] is provided, the returned
     * [ViewModelStoreOwner] will also implement [SavedStateRegistryOwner], delegating state
     * resolution to the provided owner. This is required if ViewModels within this scope depend on
     * [SavedStateHandle]. When saved state is enabled and [defaultFactory] is not explicitly
     * overridden, it automatically upgrades to a [SavedStateViewModelFactory].
     *
     * @param key The unique identifier for the child scope. A `null` key is valid and is treated as
     *   a distinct scope.
     * @param savedStateRegistryOwner An optional parent registry owner to delegate saved state
     *   operations to. If `null`, the returned owner will not support [SavedStateHandle].
     * @param defaultArgs The default [SavedState] arguments to use for child stores. These
     *   arguments are merged with any default arguments in [defaultCreationExtras]. If the same key
     *   exists in both, the value from [defaultArgs] takes precedence.
     * @param defaultFactory An optional override for the default [ViewModelProvider.Factory].
     * @param defaultCreationExtras An optional override for the default [CreationExtras].
     * @return A scoped [ViewModelStoreOwner], which optionally supports saved state.
     * @throws IllegalArgumentException If [savedStateRegistryOwner] is provided but its lifecycle
     *   is past the INITIALIZED or CREATED state.
     */
    @JvmOverloads
    public fun getOrCreateOwner(
        key: Any?,
        savedStateRegistryOwner: SavedStateRegistryOwner? = null,
        defaultArgs: SavedState = this.defaultArgs,
        defaultCreationExtras: CreationExtras = this.defaultCreationExtras,
        defaultFactory: Factory = this.defaultFactory,
    ): ViewModelStoreOwner {
        val viewModelStore = getOrCreate(key)
        return if (savedStateRegistryOwner == null) {
            // If no saved state is required, return a basic owner.
            ViewModelStoreOwner(
                viewModelStore = viewModelStore,
                defaultArgs = defaultArgs,
                defaultFactory = defaultFactory,
                defaultCreationExtras = defaultCreationExtras,
            )
        } else {
            // If saved state is required, return the full delegate owner.
            ViewModelStoreOwner(
                viewModelStore = viewModelStore,
                savedStateRegistryOwner = savedStateRegistryOwner,
                defaultArgs = defaultArgs,
                defaultFactory = defaultFactory,
                defaultCreationExtras = defaultCreationExtras,
            )
        }
    }

    /**
     * Marks the [ViewModelStore] associated with the given [key] as removable.
     *
     * If the store currently has a reference count of zero, it is cleared immediately. Otherwise,
     * the actual cleanup is deferred until all acquired tokens are released.
     *
     * @param key The unique identifier for the child scope. Passing `null` will target the specific
     *   scope associated with the `null` key.
     */
    public fun clearKey(key: Any?) {
        stateHolder.clearKey(key)
    }

    /**
     * Triggers a cleanup pass on all managed stores.
     *
     * Any [ViewModelStore] that has a reference count of zero will have its [ViewModelStore.clear]
     * method called and will be removed from the internal map. Stores with active references are
     * marked as removable and will be deferred until their count reaches zero.
     */
    public fun clearAllKeys() {
        stateHolder.clearAllKeys()
    }

    /**
     * Represents an active hold on a specific [ViewModelStore].
     *
     * As long as this token remains active, the underlying ViewModels will survive calls to
     * [clearKey] or [clearAllKeys]. This allows them to safely outlive their immediate UI (e.g.,
     * during exit animations).
     *
     * Calling [close] releases this hold and decrements the reference count for the store. If the
     * store has been marked for removal and this was the last active reference, the store will be
     * immediately cleared.
     */
    public fun interface ReferenceToken : AutoCloseable

    /** Holds the state for a single child [store]. */
    private data class Entry(
        val key: Any?,
        val store: ViewModelStore = ViewModelStore(),
        var refCount: Int = 0,
        var isDisposable: Boolean = false,
    )

    /**
     * The internal ViewModel that survives configuration changes (rotation, etc.) on the host.
     *
     * It retains all child [ViewModelStore] instances. It ensures that child stores are not lost
     * when the `parent` is recreated, but are properly cleared when the parent is permanently
     * destroyed.
     */
    private class StateHolder : ViewModel() {
        val entries = mutableScatterMapOf<Any?, Entry>()

        fun getOrCreate(key: Any?) = entries.getOrPut(key) { Entry(key) }

        fun remove(key: Any?) = entries.remove(key)?.store?.clear()

        fun clearKey(key: Any?) {
            val entry = entries[key] ?: return
            entry.isDisposable = true
            if (entry.isDisposable && entry.refCount <= 0) {
                remove(key)
            }
        }

        fun clearAllKeys() {
            // We do not force dispose; we always wait for the reference count to hit 0.
            // This prevents ViewModels from being cleared while still in use (e.g., in an
            // animating dialog window living for extra frames).
            entries.toScatterMap().forEachValue { entry ->
                entry.isDisposable = true
                if (entry.refCount <= 0) {
                    remove(entry.key)
                }
            }
        }

        override fun onCleared() {
            clearAllKeys()
        }
    }
}

/** Returns a new read-only [ScatterMap] with the specified mappings. */
private fun <K, V> ScatterMap<K, V>.toScatterMap(): ScatterMap<K, V> =
    if (isEmpty()) emptyScatterMap() else MutableScatterMap<K, V>(size).also { it.putAll(this) }
