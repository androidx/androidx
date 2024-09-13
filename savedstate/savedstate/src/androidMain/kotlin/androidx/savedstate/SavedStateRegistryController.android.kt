/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.savedstate

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.savedstate.internal.SavedStateRegistryImpl

/**
 * An API for [SavedStateRegistryOwner] implementations to control [SavedStateRegistry].
 *
 * `SavedStateRegistryOwner` should call [performRestore] to restore state of [SavedStateRegistry]
 * and [performSave] to gather SavedState from it.
 */
class SavedStateRegistryController private constructor(
    private val impl: SavedStateRegistryImpl,
) {

    /** The [SavedStateRegistry] owned by this controller */
    val savedStateRegistry: SavedStateRegistry = SavedStateRegistry(impl)


    /**
     * Perform the initial, one time attachment necessary to configure this [SavedStateRegistry].
     * This must be called when the owner's [Lifecycle] is [Lifecycle.State.INITIALIZED] and before
     * you call [performRestore].
     */
    @MainThread
    fun performAttach() {
        impl.performAttach()
    }

    /**
     * An interface for an owner of this [SavedStateRegistry] to restore saved state.
     *
     * @param savedState restored state
     */
    @MainThread
    fun performRestore(savedState: SavedState?) {
        impl.performRestore(savedState)
    }

    /**
     * An interface for an owner of this [SavedStateRegistry] to perform state saving, it will call
     * all registered providers and merge with unconsumed state.
     *
     * @param outBundle SavedState in which to place a saved state
     */
    @MainThread
    fun performSave(outBundle: SavedState) {
        impl.performSave(outBundle)
    }

    companion object {
        /**
         * Creates a [SavedStateRegistryController].
         *
         * It should be called during construction time of [SavedStateRegistryOwner]
         */
        @JvmStatic
        fun create(owner: SavedStateRegistryOwner): SavedStateRegistryController {
            val impl = SavedStateRegistryImpl(
                owner,
                onAttach = { owner.lifecycle.addObserver(Recreator(owner)) },
            )
            return SavedStateRegistryController(impl)
        }
    }
}
