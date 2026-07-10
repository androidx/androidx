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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.lifecycle.viewmodel.testing

import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.testing.internal.createScenarioExtras
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Creates a default instance of [CreationExtras] pre-configured with all keys required to use
 * [SavedStateHandle].
 *
 * This function sets up the instance with:
 * - A fake [SavedStateRegistryOwner] assigned to [SAVED_STATE_REGISTRY_OWNER_KEY].
 * - A fake [ViewModelStoreOwner] assigned to [VIEW_MODEL_STORE_OWNER_KEY], containing an empty
 *   [ViewModelStore].
 */
@Suppress("FunctionName")
public fun DefaultCreationExtras(): CreationExtras {
    return createScenarioExtras()
}

/**
 * Creates a default instance of [CreationExtras] pre-configured with all keys required to use
 * [SavedStateHandle], with the specified [defaultArgs] as the [DEFAULT_ARGS_KEY].
 *
 * This function sets up the instance with:
 * - A fake [SavedStateRegistryOwner] assigned to [SAVED_STATE_REGISTRY_OWNER_KEY].
 * - A fake [ViewModelStoreOwner] assigned to [VIEW_MODEL_STORE_OWNER_KEY], containing an empty
 *   [ViewModelStore].
 */
@Suppress("FunctionName")
public fun DefaultCreationExtras(defaultArgs: SavedState): CreationExtras {
    return createScenarioExtras(defaultArgs = defaultArgs)
}
