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
import androidx.savedstate.SavedStateRegistry.AutoRecreated
import androidx.savedstate.SavedStateRegistryOwner

/**
 * Schedules a recreation hook to automatically reconnect the [SavedStateHandleController] when the
 * host [SavedStateRegistryOwner] is recreated.
 *
 * Since [SavedStateHandleController] delegates to the current [SavedStateRegistryOwner], it can
 * directly access its host's [Lifecycle] and [SavedStateRegistry].
 *
 * This serves as a safety net for backwards compatibility when using legacy factories like
 * [SavedStateViewModelFactory] or [AbstractSavedStateViewModelFactory]. If the component undergoes
 * a configuration change and the client does not immediately retrieve the [ViewModel] (and thus
 * does not invoke the factory/controller), the recreator hook will restore the controller and
 * reattach it to the new registry prior to state saving, preventing data loss.
 */
internal fun SavedStateHandleController.attachSavedStateHandleOnNextRecreation() {
    val lifecycle = this.lifecycle
    val currentState = lifecycle.currentState
    if (
        currentState == Lifecycle.State.INITIALIZED ||
            currentState.isAtLeast(Lifecycle.State.STARTED)
    ) {
        savedStateRegistry.runOnNextRecreation(OnRecreation::class.java)
    } else {
        lifecycle.addObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                lifecycle.removeObserver(this)
                savedStateRegistry.runOnNextRecreation(OnRecreation::class.java)
            }
        }
    }
}

/**
 * An [AutoRecreated] hook that serves as a safety net to automatically reconnect
 * [SavedStateHandleController] to the new [SavedStateRegistry] on host recreation.
 *
 * This class is instantiated via reflection by the [SavedStateRegistry] during the host's creation
 * phase if it was previously registered.
 */
private class OnRecreation : AutoRecreated {
    override fun onRecreated(owner: SavedStateRegistryOwner) {
        check(owner is ViewModelStoreOwner) {
            "Internal error: OnRecreation should be registered only on components " +
                "that implement ViewModelStoreOwner. Received owner: $owner"
        }

        // Reconnect the surviving StateHolder (and its active SavedStateHandles)
        // to the new SavedStateRegistry of the recreated host. This ensures that
        // the state will still be saved even if the client never accesses the ViewModels
        // on the new host instance.
        val controller = SavedStateHandleController.getOrCreate(owner)
        // Prime the recreation hook again for the next configuration change.
        controller.attachSavedStateHandleOnNextRecreation()
    }
}
