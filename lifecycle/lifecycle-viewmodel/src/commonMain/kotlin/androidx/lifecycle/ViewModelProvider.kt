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
import androidx.annotation.RestrictTo
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.internal.DefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.internal.ViewModelProviders
import kotlin.reflect.KClass

/**
 * A utility class that provides `ViewModels` for a scope.
 *
 * Default `ViewModelProvider` for an `Activity` or a `Fragment` can be obtained
 * by passing it to the constructor: `ViewModelProvider(myFragment)`
 */
public expect class ViewModelProvider {

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this `ViewModelProvider`.
     *
     *
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     * present.
     * @return A ViewModel that is an instance of the given type `T`.
     * @throws IllegalArgumentException if the given [modelClass] is local or anonymous class.
     */
    @MainThread
    public operator fun <T : ViewModel> get(modelClass: KClass<T>): T

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this `ViewModelProvider`.
     *
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param key        The key to use to identify the ViewModel.
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     * present.
     * @return A ViewModel that is an instance of the given type `T`.
     */
    @MainThread
    public operator fun <T : ViewModel> get(key: String, modelClass: KClass<T>): T

    /**
     * Implementations of `Factory` interface are responsible to instantiate ViewModels.
     */
    public interface Factory {

        /**
         * Creates a new instance of the given `Class`.
         *
         * @param modelClass a [KClass] whose instance is requested
         * @param extras an additional information for this creation request
         * @return a newly created [ViewModel]
         */
        public open fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open class OnRequeryFactory {
        public open fun onRequery(viewModel: ViewModel)
    }

    public companion object {
        /**
         * Creates a [ViewModelProvider]. This provider generates [ViewModel] instances using the
         * specified [Factory] and stores them within the [ViewModelStore] of the provided
         * [ViewModelStoreOwner].
         *
         * @param owner The [ViewModelStoreOwner] that will manage the lifecycle of the created
         *  [ViewModel] instances.
         * @param factory The [Factory] responsible for creating new [ViewModel] instances.
         * @param extras Additional data to be passed to the [Factory] during
         *  [ViewModel] creation.
         */
        public fun create(
            owner: ViewModelStoreOwner,
            factory: Factory = ViewModelProviders.getDefaultFactory(owner),
            extras: CreationExtras = ViewModelProviders.getDefaultCreationExtras(owner),
        ): ViewModelProvider

        /**
         * Creates a [ViewModelProvider]. This provider generates [ViewModel] instances using the
         * specified [Factory] and stores them within the [ViewModelStore] of the provided
         * [ViewModelStoreOwner].
         *
         * @param store `ViewModelStore` where ViewModels will be stored.
         * @param factory factory a `Factory` which will be used to instantiate new `ViewModels`
         * @param extras Additional data to be passed to the [Factory] during
         *  [ViewModel] creation.
         *  */
        public fun create(
            store: ViewModelStore,
            factory: Factory = DefaultViewModelProviderFactory,
            extras: CreationExtras = CreationExtras.Empty,
        ): ViewModelProvider
    }
}
