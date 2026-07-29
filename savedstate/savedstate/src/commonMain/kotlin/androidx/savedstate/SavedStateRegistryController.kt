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

package androidx.savedstate

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.savedstate.internal.SavedStateRegistryImpl
import kotlin.jvm.JvmStatic

/**
 * An API for [SavedStateRegistryOwner] implementations to control [SavedStateRegistry].
 *
 * A [SavedStateRegistryOwner] should call [performRestore] to restore the state of the
 * [SavedStateRegistry], and [performSave] to gather the saved state from it.
 */
public class SavedStateRegistryController
private constructor(private val impl: SavedStateRegistryImpl) {

    /** The [SavedStateRegistry] controlled by this controller. */
    public val savedStateRegistry: SavedStateRegistry = SavedStateRegistry(impl)

    /**
     * Performs the initial, one-time attachment necessary to configure this [SavedStateRegistry].
     *
     * Call this when the owner's [Lifecycle] is [Lifecycle.State.INITIALIZED] and before calling
     * [performRestore].
     */
    @MainThread
    public fun performAttach() {
        impl.performAttach()
    }

    /**
     * Restores the saved state for the owner of this [SavedStateRegistry].
     *
     * @param savedState The restored state.
     */
    @MainThread
    public fun performRestore(savedState: SavedState?) {
        impl.performRestore(savedState)
    }

    /**
     * Saves the state for the owner of this [SavedStateRegistry].
     *
     * This calls all registered providers and merges their states with the unconsumed state.
     *
     * @param outBundle The [SavedState] in which to place the saved state.
     */
    @MainThread
    public fun performSave(outBundle: SavedState) {
        impl.performSave(outBundle)
    }

    public companion object {

        /**
         * Creates a [SavedStateRegistryController].
         *
         * Call this during construction of the [SavedStateRegistryOwner].
         */
        @JvmStatic
        public fun create(owner: SavedStateRegistryOwner): SavedStateRegistryController {
            return SavedStateRegistryController(
                SavedStateRegistryImpl(
                    owner = owner,
                    onAttach = { onAttachSavedStateRegistryController(owner) },
                )
            )
        }
    }
}

/**
 * Platform-specific attachment logic for [SavedStateRegistryController].
 *
 * @param owner The owner whose lifecycle/state registry is being configured.
 */
internal expect fun onAttachSavedStateRegistryController(owner: SavedStateRegistryOwner)
