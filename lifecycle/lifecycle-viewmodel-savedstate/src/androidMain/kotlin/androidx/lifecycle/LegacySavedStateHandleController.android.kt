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

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle.Companion.createHandle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner

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
        val controller =
            viewModel.getCloseable<SavedStateHandleController>(TAG_SAVED_STATE_HANDLE_CONTROLLER)
        if (controller != null && !controller.isAttached) {
            controller.attachToLifecycle(registry, lifecycle)
            tryToAddRecreator(registry, lifecycle)
        }
    }

    private fun tryToAddRecreator(registry: SavedStateRegistry, lifecycle: Lifecycle) {
        val currentState = lifecycle.currentState
        if (
            currentState === Lifecycle.State.INITIALIZED ||
                currentState.isAtLeast(Lifecycle.State.STARTED)
        ) {
            registry.runOnNextRecreation(OnRecreation::class.java)
        } else {
            lifecycle.addObserver(
                object : LifecycleEventObserver {
                    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                        if (event === Lifecycle.Event.ON_START) {
                            lifecycle.removeObserver(this)
                            registry.runOnNextRecreation(OnRecreation::class.java)
                        }
                    }
                }
            )
        }
    }

    internal class OnRecreation : SavedStateRegistry.AutoRecreated {
        override fun onRecreated(owner: SavedStateRegistryOwner) {
            check(owner is ViewModelStoreOwner) {
                ("Internal error: OnRecreation should be registered only on components " +
                    "that implement ViewModelStoreOwner")
            }
            val viewModelStore = (owner as ViewModelStoreOwner).viewModelStore
            val savedStateRegistry = owner.savedStateRegistry
            for (key in viewModelStore.keys()) {
                val viewModel = viewModelStore[key]
                attachHandleIfNeeded(viewModel!!, savedStateRegistry, owner.lifecycle)
            }
            if (viewModelStore.keys().isNotEmpty()) {
                savedStateRegistry.runOnNextRecreation(OnRecreation::class.java)
            }
        }
    }
}
