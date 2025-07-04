/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.saveable

import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState

/**
 * Key used to store and retrieve the [SavedState] associated with the AndroidX [SavedStateRegistry]
 * from the Compose [SaveableStateRegistry].
 *
 * This key serves as a bridge between Compose and AndroidX state-saving mechanisms, enabling the
 * [SaveableStateRegistryWrapper] to delegate state restoration and saving between the two systems.
 * It must be unique and consistent to ensure proper round-tripping of the saved state.
 */
private const val PROVIDER_KEY = "androidx.savedstate.SavedStateRegistry"

/**
 * A bridge between [SaveableStateRegistry] and [SavedStateRegistryOwner], enabling interoperability
 * between Compose's saveable state mechanism and AndroidX's SavedState infrastructure.
 *
 * This wrapper class integrates a [SaveableStateRegistry] into the traditional Android
 * [SavedStateRegistry] by implementing the owner interface and managing a
 * [SavedStateRegistryController]. This allows Compose-based state (using [rememberSaveable]) to
 * interoperate with non-Compose components that rely on [SavedStateRegistry].
 */
internal class SaveableStateRegistryWrapper(base: SaveableStateRegistry) :
    SaveableStateRegistry by base, SavedStateRegistryOwner {

    /** Controls save/restore operations for the child [SavedStateRegistry]. */
    val controller = SavedStateRegistryController.create(owner = this)

    /**
     * Provides a child lifecycle associated with the [SavedStateRegistry].
     *
     * [LifecycleRegistry] enforces main-thread access only. While this is safe for most Compose
     * Android, it causes test failures in Compose Multiplatform, where work can occur on different
     * threads.
     *
     * To unblock those tests, this implementation uses [LifecycleRegistry], which bypasses the
     * main-thread check. This is a temporary workaround.
     */
    @Suppress("VisibleForTests") // TODO(mgalhardo): Fix when Lifecycle supports multi-threading.
    override val lifecycle = LifecycleRegistry.createUnsafe(owner = this)

    override val savedStateRegistry = controller.savedStateRegistry

    init {
        // Restore the AndroidX Registry state using the Compose Registry.
        controller.performRestore(savedState = consumeRestored(key = PROVIDER_KEY) as? SavedState)

        // Save AndroidX Registry state into the Compose registry.
        registerProvider(key = PROVIDER_KEY) {
            val result = savedState()
            controller.performSave(outBundle = result)
            return@registerProvider if (result.read { isEmpty() }) {
                null // Return null if state is empty, since nulls aren't stored.
            } else {
                result
            }
        }
    }
}
