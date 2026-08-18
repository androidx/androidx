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
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Abstract [ViewModelProvider.Factory] that creates a [SavedStateHandle] for every requested
 * [ViewModel].
 *
 * Subclasses implement the abstract [create] method to instantiate [ViewModel]s.
 *
 * **Deprecated:** Use [viewModelFactory] or implement [ViewModelProvider.Factory] directly,
 * combined with [CreationExtras.createSavedStateHandle]. This base class creates a
 * [SavedStateHandle] for every [ViewModel], even when unnecessary, causing overhead.
 *
 * For example:
 * ```
 * viewModelFactory { initializer { MyViewModel(createSavedStateHandle()) } }
 * ```
 */
@Deprecated(
    "Use `viewModelFactory` or implement `ViewModelProvider.Factory`, combined with `CreationExtras.createSavedStateHandle()`."
)
public abstract class AbstractSavedStateViewModelFactory : Factory {
    private val savedStateRegistryOwner: SavedStateRegistryOwner?
    private val viewModelStoreOwner: ViewModelStoreOwner?
    private val defaultArgs: Bundle?

    /**
     * Constructs a new [AbstractSavedStateViewModelFactory].
     *
     * When constructed this way, the component for which the [SavedStateHandle] is scoped must have
     * called [enableSavedStateHandles]. See [CreationExtras.createSavedStateHandle] for more
     * details.
     */
    public constructor() {
        this.savedStateRegistryOwner = null
        this.viewModelStoreOwner = null
        this.defaultArgs = null
    }

    /**
     * Constructs a new [AbstractSavedStateViewModelFactory].
     *
     * @param owner [SavedStateRegistryOwner] that will provide restored state for created
     *   [ViewModel]s. Must implement [ViewModelStoreOwner] to support state retention.
     * @param defaultArgs default values to populate the [SavedStateHandle] if no state is restored
     * @throws IllegalArgumentException if the [owner] does not implement [ViewModelStoreOwner]
     */
    public constructor(owner: SavedStateRegistryOwner, defaultArgs: Bundle?) {
        require(owner is ViewModelStoreOwner) {
            "SavedStateRegistryOwner must implement ViewModelStoreOwner to support SavedStateHandles"
        }
        this.savedStateRegistryOwner = owner
        this.viewModelStoreOwner = owner
        this.defaultArgs = defaultArgs
    }

    /**
     * Creates a new instance of the given [Class].
     *
     * @param modelClass [Class] of the [ViewModel] to create
     * @param extras [CreationExtras] passed to the [Factory] to create the [ViewModel]
     * @return new [ViewModel] instance of type [T]
     * @throws IllegalStateException if the [extras] do not contain
     *   [ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
     */
    public override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val key =
            checkNotNull(extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]) {
                "VIEW_MODEL_KEY must always be provided by ViewModelProvider"
            }

        // If a factory constructed in the old way use the old infra to create SavedStateHandle.
        return if (savedStateRegistryOwner != null) {
            create(key, modelClass)
        } else {
            create(key, modelClass, extras.createSavedStateHandle())
        }
    }

    private fun <T : ViewModel> create(key: String, modelClass: Class<T>): T {
        if (savedStateRegistryOwner == null || viewModelStoreOwner == null) {
            throw UnsupportedOperationException(
                "AbstractSavedStateViewModelFactory constructed with empty constructor supports " +
                    "only calls to create(modelClass: Class<T>, extras: CreationExtras)."
            )
        }

        // Register controller under host. Ensures createSavedStateHandle() resolves it.
        SavedStateHandleController.getOrCreate(savedStateRegistryOwner, viewModelStoreOwner)
        attachSavedStateHandleOnNextRecreation(savedStateRegistryOwner)

        // Construct CreationExtras. Preserves owner default extras, overrides with factory keys.
        val extras =
            CreationExtras(initialExtras = viewModelStoreOwner.defaultViewModelCreationExtras) {
                this[SAVED_STATE_REGISTRY_OWNER_KEY] = savedStateRegistryOwner
                this[VIEW_MODEL_STORE_OWNER_KEY] = viewModelStoreOwner
                this[VIEW_MODEL_KEY] = key
                if (defaultArgs != null) {
                    this[DEFAULT_ARGS_KEY] = defaultArgs
                }
            }

        // Retrieve SavedStateHandle from registered controller via CreationExtras.
        val handle = extras.createSavedStateHandle()

        return create(key, modelClass, handle)
    }

    /**
     * Creates a new instance of the given [Class].
     *
     * @param modelClass [Class] of the [ViewModel] to create
     * @return new [ViewModel] instance of type [T]
     * @throws IllegalArgumentException if the given [modelClass] is a local or anonymous class
     * @throws UnsupportedOperationException if this factory was constructed with the empty
     *   constructor, and therefore has no [SavedStateRegistryOwner]
     */
    public override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // ViewModelProvider calls correct create that support same modelClass with different keys
        // If a developer manually calls this method, there is no "key" in picture, so factory
        // simply uses classname internally as key.
        val canonicalName =
            requireNotNull(modelClass.canonicalName) {
                "Local and anonymous classes can not be ViewModels"
            }
        return create(canonicalName, modelClass)
    }

    /**
     * Creates a new instance of the given [Class].
     *
     * @param key key associated with the requested [ViewModel]
     * @param modelClass [Class] of the [ViewModel] to create
     * @param handle [SavedStateHandle] associated with the [ViewModel] to create
     * @return new [ViewModel] instance of type [T]
     */
    protected abstract fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T
}
