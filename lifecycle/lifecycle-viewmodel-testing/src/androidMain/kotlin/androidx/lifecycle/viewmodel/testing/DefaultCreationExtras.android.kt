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
@file:JvmName("DefaultCreationExtrasKt")

package androidx.lifecycle.viewmodel.testing

import android.os.Bundle
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Creates a default instance of [CreationExtras] pre-configured with all keys required to use
 * [SavedStateHandle].
 *
 * This function sets up the instance with:
 * - A fake [SavedStateRegistryOwner] assigned to [SAVED_STATE_REGISTRY_OWNER_KEY], delegating the
 *   [LifecycleOwner] to a [TestLifecycleOwner].
 * - A fake [ViewModelStoreOwner] assigned to [VIEW_MODEL_STORE_OWNER_KEY], containing an empty
 *   [ViewModelStore].
 */
@Suppress("FunctionName")
public actual fun DefaultCreationExtras(): CreationExtras {
    return DefaultCreationExtras(defaultArgs = Bundle())
}

/**
 * Creates a default instance of [CreationExtras] pre-configured with all keys required to use
 * [SavedStateHandle], with the specified [defaultArgs] as the [DEFAULT_ARGS_KEY].
 *
 * This function sets up the instance with:
 * - A fake [SavedStateRegistryOwner] assigned to [SAVED_STATE_REGISTRY_OWNER_KEY], delegating the
 *   [LifecycleOwner] to a [TestLifecycleOwner].
 * - A fake [ViewModelStoreOwner] assigned to [VIEW_MODEL_STORE_OWNER_KEY], containing an empty
 *   [ViewModelStore].
 */
@Suppress("FunctionName")
public fun DefaultCreationExtras(defaultArgs: Bundle): CreationExtras {
    val owner =
        object : ViewModelStoreOwner, LifecycleOwner, SavedStateRegistryOwner {
            override val viewModelStore = ViewModelStore()

            val lifecycleRegistry = LifecycleRegistry.createUnsafe(owner = this)
            override val lifecycle: Lifecycle = lifecycleRegistry

            val savedStateRegistryController = SavedStateRegistryController.create(owner = this)
            override val savedStateRegistry = savedStateRegistryController.savedStateRegistry
        }

    owner.savedStateRegistryController.performAttach()
    owner.savedStateRegistryController.performRestore(savedState = null)
    owner.enableSavedStateHandles()

    owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

    return MutableCreationExtras().apply {
        this[SAVED_STATE_REGISTRY_OWNER_KEY] = owner
        this[VIEW_MODEL_STORE_OWNER_KEY] = owner
        this[DEFAULT_ARGS_KEY] = defaultArgs
    }
}
