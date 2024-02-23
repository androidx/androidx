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
package androidx.lifecycle

import androidx.core.bundle.Bundle
import androidx.lifecycle.SavedStateHandle.Companion.createHandle
import androidx.savedstate.SavedStateRegistry
import kotlin.jvm.JvmStatic

internal object LegacySavedStateHandleController {
    const val TAG_SAVED_STATE_HANDLE_CONTROLLER = "androidx.lifecycle.savedstate.vm.tag"

    @JvmStatic
    fun create(
        registry: SavedStateRegistry,
        lifecycle: Lifecycle,
        key: String?,
        defaultArgs: Bundle?
    ): SavedStateHandleController {
        val restoredState = registry.consumeRestoredStateForKey(key!!)
        val handle = createHandle(restoredState, defaultArgs)
        val controller = SavedStateHandleController(key, handle)
        controller.attachToLifecycle(registry, lifecycle)
        tryToAddRecreator(registry, lifecycle)
        return controller
    }

    @JvmStatic
    fun attachHandleIfNeeded(
        viewModel: ViewModel,
        registry: SavedStateRegistry,
        lifecycle: Lifecycle
    ) {
        val controller = viewModel.getCloseable<SavedStateHandleController>(
            TAG_SAVED_STATE_HANDLE_CONTROLLER
        )
        if (controller != null && !controller.isAttached) {
            controller.attachToLifecycle(registry, lifecycle)
            tryToAddRecreator(registry, lifecycle)
        }
    }
}

internal expect fun tryToAddRecreator(registry: SavedStateRegistry, lifecycle: Lifecycle)

