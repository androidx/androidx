/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistry.AutoRecreated
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Bridges the gap between a [ViewModel]'s persistent state ([SavedStateHandle]) and the transient
 * [SavedStateRegistry] of its [LifecycleOwner].
 */
internal class SavedStateHandleController(
    private val key: String,
    registry: SavedStateRegistry,
    lifecycle: Lifecycle,
    defaultArgs: SavedState? = null,
) : AutoCloseable {

    constructor(
        key: String,
        owner: SavedStateRegistryOwner,
        defaultArgs: SavedState?,
    ) : this(key, owner.savedStateRegistry, owner.lifecycle, defaultArgs)

    // Prevents redundant observer registrations if the lifecycle hasn't actually been destroyed.
    private var isAttached: Boolean = false

    val handle =
        SavedStateHandle.createHandle(
            restoredState = registry.consumeRestoredStateForKey(key),
            defaultState = defaultArgs,
        )

    init {
        attachHandleIfNeeded(registry, lifecycle)
    }

    override fun close() {
        // This class has nothing to actually close, but all objects added via
        // ViewModel's addCloseable(key, Closeable) must be Closeable.
    }

    /**
     * Attach a [SavedStateHandle] from a [ViewModel] to the current [SavedStateRegistry].
     *
     * This is necessary because ViewModels outlive Activities/Fragments during configuration
     * changes. When the Activity is recreated, the [ViewModel] is still alive, but its
     * [SavedStateHandle] is holding onto a dead registry.
     *
     * This function:
     * 1. Connects the handle to the *new* registry so state can be saved again.
     * 2. Primes the [OnRecreation] hook to ensure this connection happens again on the next
     *    rotation/process death.
     */
    private fun attachHandleIfNeeded(registry: SavedStateRegistry?, lifecycle: Lifecycle?) {
        if (isAttached || registry == null || lifecycle == null) {
            return
        }

        isAttached = true
        lifecycle.addObserver { _, event ->
            if (event == Event.ON_DESTROY) {
                isAttached = false
                lifecycle.removeObserver(this)
            }
        }
        registry.registerSavedStateProvider(key, handle.savedStateProvider())

        val currentState = lifecycle.currentState
        if (currentState == State.INITIALIZED || currentState.isAtLeast(State.STARTED)) {
            registry.runOnNextRecreation(OnRecreation::class.java)
        } else {
            lifecycle.addObserver { _, event ->
                if (event == Event.ON_START) {
                    lifecycle.removeObserver(this)
                    registry.runOnNextRecreation(OnRecreation::class.java)
                }
            }
        }
    }

    /**
     * An [AutoRecreated] hook that re-attaches [SavedStateHandle]s to the new [SavedStateRegistry]
     * and [Lifecycle] after a configuration change or process death.
     *
     * ViewModels survive configuration changes (like rotation), but the [SavedStateRegistry] (owned
     * by the Activity/Fragment) dies and is replaced. This class bridges that gap by:
     * 1. Iterating through the existing [ViewModelStore].
     * 2. Finding any [SavedStateHandleController] hidden inside the ViewModels.
     * 3. Re-wiring them to the new Registry and Lifecycle.
     *
     * It automatically re-registers itself to ensure this restoration logic runs again on the next
     * recreation event.
     */
    private class OnRecreation : AutoRecreated {
        override fun onRecreated(owner: SavedStateRegistryOwner) {
            check(owner is ViewModelStoreOwner) {
                "Internal error: OnRecreation should be registered only on components " +
                    "that implement ViewModelStoreOwner. Received owner: $owner"
            }

            val keys = owner.viewModelStore.keys()

            for (key in keys) {
                val viewModel = owner.viewModelStore[key] ?: continue
                val controller = viewModel.getCloseable<SavedStateHandleController>(TAG) ?: continue
                controller.attachHandleIfNeeded(owner.savedStateRegistry, owner.lifecycle)
            }

            if (keys.isNotEmpty()) {
                owner.savedStateRegistry.runOnNextRecreation(OnRecreation::class.java)
            }
        }
    }

    companion object {
        const val TAG = "androidx.lifecycle.savedstate.vm.tag"
    }
}
