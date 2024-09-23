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

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.lifecycle.LegacySavedStateHandleController.TAG_SAVED_STATE_HANDLE_CONTROLLER
import androidx.lifecycle.LegacySavedStateHandleController.attachHandleIfNeeded
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Skeleton of androidx.lifecycle.ViewModelProvider.KeyedFactory that creates [SavedStateHandle] for
 * every requested [ViewModel]. The subclasses implement [create] to actually instantiate
 * `androidx.lifecycle.ViewModel`s.
 */
public abstract class AbstractSavedStateViewModelFactory :
    ViewModelProvider.OnRequeryFactory, ViewModelProvider.Factory {

    private var savedStateRegistry: SavedStateRegistry? = null
    private var lifecycle: Lifecycle? = null
    private var defaultArgs: Bundle? = null

    /**
     * Constructs this factory.
     *
     * When a factory is constructed this way, a component for which [SavedStateHandle] is scoped
     * must have called [enableSavedStateHandles]. See [CreationExtras.createSavedStateHandle] docs
     * for more details.
     */
    constructor() {}

    /**
     * Constructs this factory.
     *
     * @param owner [SavedStateRegistryOwner] that will provide restored state for created
     *   [ViewModels][ViewModel]
     * @param defaultArgs values from this `Bundle` will be used as defaults by [SavedStateHandle]
     *   passed in [ViewModels][ViewModel] if there is no previously saved state or previously saved
     *   state misses a value by such key
     */
    constructor(owner: SavedStateRegistryOwner, defaultArgs: Bundle?) {
        savedStateRegistry = owner.savedStateRegistry
        lifecycle = owner.lifecycle
        this.defaultArgs = defaultArgs
    }

    /**
     * Creates a new instance of the given `Class`.
     *
     * @param modelClass a `Class` whose instance is requested
     * @param extras an additional information for this creation request
     * @return a newly created ViewModel
     * @throws IllegalStateException if no VIEW_MODEL_KEY provided by ViewModelProvider
     */
    public override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val key =
            extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
                ?: throw IllegalStateException(
                    "VIEW_MODEL_KEY must always be provided by ViewModelProvider"
                )
        // if a factory constructed in the old way use the old infra to create SavedStateHandle
        return if (savedStateRegistry != null) {
            create(key, modelClass)
        } else {
            create(key, modelClass, extras.createSavedStateHandle())
        }
    }

    private fun <T : ViewModel> create(key: String, modelClass: Class<T>): T {
        val controller =
            LegacySavedStateHandleController.create(
                savedStateRegistry!!,
                lifecycle!!,
                key,
                defaultArgs
            )
        val viewModel = create(key, modelClass, controller.handle)
        viewModel.addCloseable(TAG_SAVED_STATE_HANDLE_CONTROLLER, controller)
        return viewModel
    }

    /**
     * Creates a new instance of the given `Class`.
     *
     * @param modelClass a `Class` whose instance is requested
     * @return a newly created ViewModel
     * @throws IllegalArgumentException if the given [modelClass] is local or anonymous class.
     * @throws UnsupportedOperationException if AbstractSavedStateViewModelFactory constructed with
     *   empty constructor, therefore no [SavedStateRegistryOwner] available for lifecycle
     */
    public override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // ViewModelProvider calls correct create that support same modelClass with different keys
        // If a developer manually calls this method, there is no "key" in picture, so factory
        // simply uses classname internally as as key.
        val canonicalName =
            modelClass.canonicalName
                ?: throw IllegalArgumentException(
                    "Local and anonymous classes can not be ViewModels"
                )
        if (lifecycle == null) {
            throw UnsupportedOperationException(
                "AbstractSavedStateViewModelFactory constructed " +
                    "with empty constructor supports only calls to " +
                    "create(modelClass: Class<T>, extras: CreationExtras)."
            )
        }
        return create(canonicalName, modelClass)
    }

    /**
     * Creates a new instance of the given `Class`.
     *
     * @param key a key associated with the requested ViewModel
     * @param modelClass a `Class` whose instance is requested
     * @param handle a handle to saved state associated with the requested ViewModel
     * @return the newly created ViewModel </T>
     */
    protected abstract fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun onRequery(viewModel: ViewModel) {
        // is need only for legacy path
        if (savedStateRegistry != null) {
            attachHandleIfNeeded(viewModel, savedStateRegistry!!, lifecycle!!)
        }
    }
}
