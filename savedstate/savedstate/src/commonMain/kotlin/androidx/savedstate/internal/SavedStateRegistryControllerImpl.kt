/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.savedstate.internal

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.write

internal class SavedStateRegistryControllerImpl(
    private val owner: SavedStateRegistryOwner,
    internal val onAttach: () -> Unit = {},
) {

    internal val registryImpl: SavedStateRegistryImpl =
        SavedStateRegistryImpl(
            onConsumeRestoredStateForKey = {
                check(registryImpl.isRestored) {
                    "You can 'consumeRestoredStateForKey' only after the corresponding component " +
                        "has moved to the 'CREATED' state"
                }
            }
        )

    val savedStateRegistry = SavedStateRegistry(registryImpl)

    private var attached = false

    @MainThread
    fun performAttach() {
        check(owner.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
            "Restarter must be created only during owner's initialization stage"
        }
        check(!attached) { "SavedStateRegistry was already attached." }

        onAttach()
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    registryImpl.isAllowingSavingState = true
                } else if (event == Lifecycle.Event.ON_STOP) {
                    registryImpl.isAllowingSavingState = false
                }
            }
        )
        attached = true
    }

    @MainThread
    fun performRestore(savedState: SavedState?) {
        if (!attached) {
            performAttach()
        }
        check(!owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            "performRestore cannot be called when owner is ${owner.lifecycle.currentState}"
        }
        check(!registryImpl.isRestored) { "SavedStateRegistry was already restored." }

        val restored =
            savedState?.read {
                if (contains(SAVED_COMPONENTS_KEY)) getSavedState(SAVED_COMPONENTS_KEY) else null
            }
        registryImpl.restoreState(restored)
    }

    @MainThread
    fun performSave(outBundle: SavedState) {
        val inState = registryImpl.saveState()
        if (inState.read { !isEmpty() }) {
            outBundle.write { putSavedState(SAVED_COMPONENTS_KEY, inState) }
        }
    }

    private companion object {
        private const val SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.BundlableSavedStateRegistry.key"
    }
}
