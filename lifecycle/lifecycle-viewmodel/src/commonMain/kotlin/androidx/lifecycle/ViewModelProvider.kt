/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.internal.DefaultViewModelProviderFactory
import kotlin.reflect.KClass

/**
 * A utility class that manages the lifecycle, caching, and instantiation of [ViewModel] instances.
 *
 * A [ViewModelProvider] acts as the central orchestrator that coordinates:
 * 1. **Caching / Retrieval**: It checks a [ViewModelStore] to see if an instance of the requested
 *    [ViewModel] class already exists under a given key. If found, it returns the cached instance.
 * 2. **Extras Injection**: It automatically populates a [MutableCreationExtras] with the unique
 *    registration key under [VIEW_MODEL_KEY], combining it with default [CreationExtras] provided
 *    by the owner.
 * 3. **Instantiation**: If no cached instance exists, it invokes a [ViewModelProvider.Factory] to
 *    create a new instance using the prepared extras, caches it in the [ViewModelStore], and
 *    returns it.
 *
 * To ensure that [ViewModel] instances survive configuration changes, the underlying
 * [ViewModelStore] must be retained (for example, by using a [ViewModelStoreOwner] such as
 * `ComponentActivity` or `Fragment` which automatically handles this retention).
 *
 * The following diagram illustrates the retrieval and creation flow of a [ViewModel] instance:
 * ```
 *                  ViewModelProvider.get(key)
 *                              |
 *                              v
 *                     Is ViewModel cached
 *                     in ViewModelStore?
 *                           /   \
 *                         Yes    No
 *                         /       \
 *                        v         v
 *              Return cached      Create via Factory & CreationExtras,
 *              ViewModel          cache in ViewModelStore, and return
 * ```
 *
 * @see CreationExtras
 * @see ViewModel
 * @see ViewModelProvider.Factory
 * @see ViewModelStore
 * @see ViewModelStoreOwner
 */
public expect class ViewModelProvider {

    /**
     * Returns an existing [ViewModel] or creates a new one in the scope (usually, a `Fragment` or
     * an `Activity`) associated with this [ViewModelProvider].
     *
     * The created [ViewModel] is associated with the given scope and is retained as long as the
     * scope is alive (e.g., until the `Activity` is finished or the process is killed).
     *
     * @param modelClass [KClass] of the [ViewModel] to retrieve or create
     * @return [ViewModel] instance of type [T]
     * @throws IllegalArgumentException if the given [modelClass] is a local or anonymous class
     */
    @MainThread public operator fun <T : ViewModel> get(modelClass: KClass<T>): T

    /**
     * Returns an existing [ViewModel] or creates a new one in the scope (usually, a `Fragment` or
     * an `Activity`) associated with this [ViewModelProvider].
     *
     * The created [ViewModel] is associated with the given scope and is retained as long as the
     * scope is alive (e.g., until the `Activity` is finished or the process is killed).
     *
     * @param key identifier of the [ViewModel]
     * @param modelClass [KClass] of the [ViewModel] to retrieve or create
     * @return [ViewModel] instance of type [T]
     */
    @MainThread public operator fun <T : ViewModel> get(key: String, modelClass: KClass<T>): T

    /**
     * Implementations of the [Factory] interface are responsible for instantiating [ViewModel]s.
     */
    public interface Factory {

        /**
         * Creates a new instance of the given [modelClass].
         *
         * @param modelClass [KClass] of the [ViewModel] to create
         * @param extras [CreationExtras] passed to the [Factory] to create the [ViewModel]
         * @return new [ViewModel] instance of type [T]
         */
        public open fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T
    }

    public companion object {
        /**
         * Creates a [ViewModelProvider] bound to the given [ViewModelStoreOwner].
         *
         * The provider generates [ViewModel] instances using the specified [Factory] and stores
         * them within the [ViewModelStore] of the [ViewModelStoreOwner].
         *
         * @param owner [ViewModelStoreOwner] that manages the lifecycle of the created [ViewModel]
         *   instances
         * @param factory [Factory] responsible for creating new [ViewModel] instances
         * @param extras [CreationExtras] passed to the [Factory] to create the [ViewModel]
         */
        public fun create(
            owner: ViewModelStoreOwner,
            factory: Factory = owner.defaultViewModelProviderFactory,
            extras: CreationExtras = owner.defaultViewModelCreationExtras,
        ): ViewModelProvider

        /**
         * Creates a [ViewModelProvider] backed by the given [ViewModelStore].
         *
         * The provider generates [ViewModel] instances using the specified [Factory] and stores
         * them within the provided [ViewModelStore].
         *
         * @param store [ViewModelStore] where the [ViewModel] instances are stored
         * @param factory [Factory] used to instantiate new [ViewModel] instances
         * @param extras [CreationExtras] passed to the [Factory] to create the [ViewModel]
         */
        public fun create(
            store: ViewModelStore,
            factory: Factory = DefaultViewModelProviderFactory,
            extras: CreationExtras = CreationExtras.Empty,
        ): ViewModelProvider

        /**
         * A [CreationExtras.Key] used to retrieve the key associated with a requested [ViewModel].
         *
         * The [ViewModelProvider] automatically includes the key in the [CreationExtras] passed to
         * [ViewModelProvider.Factory]. This applies to keys generated by either of these usage
         * patterns:
         * - `ViewModelProvider.get(key, MyViewModel::class)`: provided `key` is used.
         * - `ViewModelProvider.get(MyViewModel::class)`: generates a `key` from given `class`.
         */
        public val VIEW_MODEL_KEY: CreationExtras.Key<String>
    }
}
