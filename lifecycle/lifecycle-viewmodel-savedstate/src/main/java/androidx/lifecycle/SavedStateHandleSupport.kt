/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("SavedStateHandleSupport")

package androidx.lifecycle

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner

private const val VIEWMODEL_KEY = "androidx.lifecycle.internal.SavedStateHandlesVM"

/**
 * Enables the support of [SavedStateHandle] in a component.
 *
 * After this method, [createSavedStateHandle] can be called on [CreationExtras] containing this
 * [SavedStateRegistryOwner] / [ViewModelStoreOwner].
 *
 * Must be called while component is in `INITIALIZED` or `CREATED` state and before
 * a [ViewModel] with [SavedStateHandle] is requested.
 */
@MainThread
fun <T> T.enableSavedStateHandles()
    where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    val currentState = lifecycle.currentState
    require(
        currentState == Lifecycle.State.INITIALIZED || currentState == Lifecycle.State.CREATED
    )

    // make sure that SavedStateHandlesVM is created.
    ViewModelProvider(this, object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SavedStateHandlesVM() as T
        }
    })[VIEWMODEL_KEY, SavedStateHandlesVM::class.java]

    savedStateRegistry.runOnNextRecreation(SavedStateHandleAttacher::class.java)
}

private fun createSavedStateHandle(
    savedStateRegistryOwner: SavedStateRegistryOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    key: String,
    defaultArgs: Bundle?
): SavedStateHandle {
    val vm = viewModelStoreOwner.savedStateHandlesVM
    val savedStateRegistry = savedStateRegistryOwner.savedStateRegistry
    val handle = SavedStateHandle.createHandle(
        savedStateRegistry.consumeRestoredStateForKey(key), defaultArgs
    )
    val controller = SavedStateHandleController(key, handle)
    controller.attachToLifecycle(savedStateRegistry, savedStateRegistryOwner.lifecycle)
    vm.controllers.add(controller)

    return handle
}

/**
 * Creates `SavedStateHandle` that can be used in your ViewModels
 *
 * This function requires `this.installSavedStateHandleSupport()` call during the component
 * initialization. Latest versions of androidx components like `ComponentActivity`, `Fragment`,
 * `NavBackStackEntry` makes this call automatically.
 *
 * This [CreationExtras] must contain [SAVED_STATE_REGISTRY_OWNER_KEY],
 * [VIEW_MODEL_STORE_OWNER_KEY] and [VIEW_MODEL_KEY].
 *
 * @throws IllegalArgumentException if this `CreationExtras` are missing required keys:
 * `ViewModelStoreOwnerKey`, `SavedStateRegistryOwnerKey`, `VIEW_MODEL_KEY`
 */
@MainThread
public fun CreationExtras.createSavedStateHandle(): SavedStateHandle {
    val savedStateRegistryOwner = this[SAVED_STATE_REGISTRY_OWNER_KEY]
        ?: throw IllegalArgumentException(
            "CreationExtras must have a value by `SAVED_STATE_REGISTRY_OWNER_KEY`"
        )
    val viewModelStateRegistryOwner = this[VIEW_MODEL_STORE_OWNER_KEY]
        ?: throw IllegalArgumentException(
            "CreationExtras must have a value by `VIEW_MODEL_STORE_OWNER_KEY`"
        )

    val defaultArgs = this[DEFAULT_ARGS_KEY]
    val key = this[VIEW_MODEL_KEY] ?: throw IllegalArgumentException(
        "CreationExtras must have a value by `VIEW_MODEL_KEY`"
    )
    return createSavedStateHandle(
        savedStateRegistryOwner, viewModelStateRegistryOwner, key, defaultArgs
    )
}

internal object ThrowingFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw IllegalStateException(
            "installSavedStateHandleSupport() wasn't called " +
                "prior to createSavedStateHandle() call"
        )
    }
}

internal val ViewModelStoreOwner.savedStateHandlesVM: SavedStateHandlesVM
    get() =
        ViewModelProvider(this, ThrowingFactory)[VIEWMODEL_KEY, SavedStateHandlesVM::class.java]

internal class SavedStateHandlesVM : ViewModel() {
    val controllers = mutableListOf<SavedStateHandleController>()
}

// it reconnects existent SavedStateHandles to SavedStateRegistryOwner when it is recreated
internal class SavedStateHandleAttacher : SavedStateRegistry.AutoRecreated {
    override fun onRecreated(owner: SavedStateRegistryOwner) {
        if (owner !is ViewModelStoreOwner) {
            throw java.lang.IllegalStateException(
                "Internal error: SavedStateHandleAttacher should be registered only on components" +
                    "that implement ViewModelStoreOwner"
            )
        }
        val viewModelStore = (owner as ViewModelStoreOwner).viewModelStore
        // if savedStateHandlesVM wasn't created previously, we shouldn't trigger a creation of it
        if (!viewModelStore.keys().contains(VIEWMODEL_KEY)) return
        owner.savedStateHandlesVM.controllers.forEach {
            it.attachToLifecycle(owner.savedStateRegistry, owner.lifecycle)
        }
        owner.savedStateRegistry.runOnNextRecreation(SavedStateHandleAttacher::class.java)
    }
}

/**
 * A key for [SavedStateRegistryOwner] that corresponds to [ViewModelStoreOwner]
 * of a [ViewModel] that is being created.
 */
@JvmField
val SAVED_STATE_REGISTRY_OWNER_KEY = object : CreationExtras.Key<SavedStateRegistryOwner> {}

/**
 * A key for [ViewModelStoreOwner] that is an owner of a [ViewModel] that is being created.
 */
@JvmField
val VIEW_MODEL_STORE_OWNER_KEY = object : CreationExtras.Key<ViewModelStoreOwner> {}

/**
 * A key for default arguments that should be passed to [SavedStateHandle] if needed.
 */
@JvmField
val DEFAULT_ARGS_KEY = object : CreationExtras.Key<Bundle> {}
