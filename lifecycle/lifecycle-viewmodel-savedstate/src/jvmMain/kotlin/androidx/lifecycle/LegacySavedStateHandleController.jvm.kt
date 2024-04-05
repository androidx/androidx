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

import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner

internal actual fun tryToAddRecreator(registry: SavedStateRegistry, lifecycle: Lifecycle) {
    val currentState = lifecycle.currentState
    if (currentState === Lifecycle.State.INITIALIZED ||
        currentState.isAtLeast(Lifecycle.State.STARTED)) {
        registry.runOnNextRecreation(OnRecreation::class.java)
    } else {
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event
            ) {
                if (event === Lifecycle.Event.ON_START) {
                    lifecycle.removeObserver(this)
                    registry.runOnNextRecreation(OnRecreation::class.java)
                }
            }
        })
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
            LegacySavedStateHandleController.attachHandleIfNeeded(
                viewModel!!,
                savedStateRegistry,
                owner.lifecycle
            )
        }
        if (viewModelStore.keys().isNotEmpty()) {
            savedStateRegistry.runOnNextRecreation(OnRecreation::class.java)
        }
    }
}
