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

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.jvm.JvmField
import kotlin.jvm.JvmName

/**
 * Enables support for [SavedStateHandle] in a component.
 *
 * After calling this method, [CreationExtras.createSavedStateHandle] can be invoked on
 * [CreationExtras] containing the [SavedStateRegistryOwner] and [ViewModelStoreOwner].
 *
 * This must be called while the component is in the [Lifecycle.State.INITIALIZED] or
 * [Lifecycle.State.CREATED] state, and before requesting any [ViewModel] that requires a
 * [SavedStateHandle].
 */
@MainThread
public fun <T> T.enableSavedStateHandles()
    where T : SavedStateRegistryOwner, T : ViewModelStoreOwner {
    val currentState = lifecycle.currentState
    require(
        currentState == Lifecycle.State.INITIALIZED || currentState == Lifecycle.State.CREATED
    ) {
        "Failed to enable `SavedStateHandle` for `$this`. The `Lifecycle.State` must be " +
            "`INITIALIZED` or `CREATED`, but was `$currentState`. You must call " +
            "`enableSavedStateHandles()` before the `Lifecycle.State` moves to `STARTED`."
    }

    // Creates and registers the controller. The returned instance is discarded as we only need the
    // registration side effect here.
    SavedStateHandleController.getOrCreate(owner = this, viewModelStoreOwner = this)
}

/**
 * Creates a [SavedStateHandle] for use in your [ViewModel]s.
 *
 * This function requires a call to [enableSavedStateHandles] during component initialization.
 * Modern Jetpack components (such as `ComponentActivity`, `Fragment`, and `NavBackStackEntry`)
 * perform this call automatically.
 *
 * The [CreationExtras] must contain [SAVED_STATE_REGISTRY_OWNER_KEY], [VIEW_MODEL_STORE_OWNER_KEY],
 * and [VIEW_MODEL_KEY].
 *
 * @return new [SavedStateHandle] instance
 * @throws IllegalArgumentException If the [CreationExtras] is missing any of the required keys:
 *   [VIEW_MODEL_STORE_OWNER_KEY], [SAVED_STATE_REGISTRY_OWNER_KEY], or [VIEW_MODEL_KEY].
 */
@MainThread
public fun CreationExtras.createSavedStateHandle(): SavedStateHandle {
    val savedStateRegistryOwner =
        requireNotNull(this[SAVED_STATE_REGISTRY_OWNER_KEY]) {
            "CreationExtras must have a value by `SAVED_STATE_REGISTRY_OWNER_KEY`"
        }
    val key =
        requireNotNull(this[VIEW_MODEL_KEY]) {
            "CreationExtras must have a value by `VIEW_MODEL_KEY`"
        }
    requireNotNull(this[VIEW_MODEL_STORE_OWNER_KEY]) {
        "CreationExtras must have a value by `VIEW_MODEL_STORE_OWNER_KEY`"
    }

    val controller = SavedStateHandleController.getOrNull(savedStateRegistryOwner)
    checkNotNull(controller) {
        "enableSavedStateHandles() wasn't called prior to createSavedStateHandle() call"
    }

    return controller.getOrCreateHandle(key, defaultArgs = this[DEFAULT_ARGS_KEY])
}

/**
 * A [CreationExtras.Key] to retrieve the [SavedStateRegistryOwner] associated with the [ViewModel]
 * being created.
 */
@JvmField
public val SAVED_STATE_REGISTRY_OWNER_KEY: CreationExtras.Key<SavedStateRegistryOwner> =
    CreationExtras.Key<SavedStateRegistryOwner>()

/**
 * A [CreationExtras.Key] to retrieve the [ViewModelStoreOwner] associated with the [ViewModel]
 * being created.
 */
@JvmField
public val VIEW_MODEL_STORE_OWNER_KEY: CreationExtras.Key<ViewModelStoreOwner> =
    CreationExtras.Key<ViewModelStoreOwner>()

/**
 * A [CreationExtras.Key] to retrieve the default [SavedState] arguments to be passed to the
 * [SavedStateHandle].
 */
@JvmField
public val DEFAULT_ARGS_KEY: CreationExtras.Key<SavedState> = CreationExtras.Key<SavedState>()
