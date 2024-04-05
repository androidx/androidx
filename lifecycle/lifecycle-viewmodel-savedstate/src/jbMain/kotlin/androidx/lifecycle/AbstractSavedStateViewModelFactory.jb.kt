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

package androidx.lifecycle

import androidx.annotation.RestrictTo
import androidx.core.bundle.Bundle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.reflect.KClass

public actual abstract class AbstractSavedStateViewModelFactory :
    ViewModelProvider.OnRequeryFactory,
    ViewModelProvider.Factory {

    private var savedStateRegistry: SavedStateRegistry? = null
    private var lifecycle: Lifecycle? = null
    private var defaultArgs: Bundle? = null

    actual constructor() {}

    actual constructor(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) {
        savedStateRegistry = owner.savedStateRegistry
        lifecycle = owner.lifecycle
        this.defaultArgs = defaultArgs
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if no VIEW_MODEL_KEY provided by ViewModelProvider
     */
    public override fun <T : ViewModel> create(
        modelClass: KClass<T>,
        extras: CreationExtras
    ): T {
        val key = extras[ViewModelProvider.VIEW_MODEL_KEY]
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun <T : ViewModel> create(key: String, modelClass: KClass<T>): T {
        val controller = LegacySavedStateHandleController
            .create(savedStateRegistry!!, lifecycle!!, key, defaultArgs)
        val viewModel = create(key, modelClass, controller.handle)
        viewModel.addCloseable(LegacySavedStateHandleController.TAG_SAVED_STATE_HANDLE_CONTROLLER, controller)
        return viewModel
    }

    protected actual open fun <T : ViewModel> create(
        key: String,
        modelClass: KClass<T>,
        handle: SavedStateHandle
    ): T = throw NotImplementedError()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun onRequery(viewModel: ViewModel) {
        // is need only for legacy path
        if (savedStateRegistry != null) {
            LegacySavedStateHandleController.attachHandleIfNeeded(
                viewModel,
                savedStateRegistry!!,
                lifecycle!!
            )
        }
    }
}
