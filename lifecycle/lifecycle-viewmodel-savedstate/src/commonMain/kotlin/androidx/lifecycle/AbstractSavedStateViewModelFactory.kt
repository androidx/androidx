/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.core.bundle.Bundle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KClass

/**
 * Skeleton of androidx.lifecycle.ViewModelProvider.KeyedFactory
 * that creates [SavedStateHandle] for every requested [ViewModel].
 * The subclasses implement [create] to actually instantiate
 * `androidx.lifecycle.ViewModel`s.
 */
public expect abstract class AbstractSavedStateViewModelFactory :
    ViewModelProvider.Factory {

    /**
     * Constructs this factory.
     *
     * When a factory is constructed this way, a component for which [SavedStateHandle] is
     * scoped must have called [enableSavedStateHandles].
     * See [CreationExtras.createSavedStateHandle] docs for more
     * details.
     */
    constructor()

    /**
     * Constructs this factory.
     *
     * @param owner [SavedStateRegistryOwner] that will provide restored state for created
     * [ViewModels][ViewModel]
     * @param defaultArgs values from this `Bundle` will be used as defaults by
     * [SavedStateHandle] passed in [ViewModels][ViewModel] if there is no
     * previously saved state or previously saved state misses a value by such key
     */
    constructor(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    )

    /**
     * Creates a new instance of the given [KClass].
     *
     * @param key a key associated with the requested ViewModel
     * @param modelClass a [KClass] whose instance is requested
     * @param handle a handle to saved state associated with the requested ViewModel
     *
     * @return the newly created ViewModel
     */
    protected open fun <T : ViewModel> create(
        key: String,
        modelClass: KClass<T>,
        handle: SavedStateHandle
    ): T
}
