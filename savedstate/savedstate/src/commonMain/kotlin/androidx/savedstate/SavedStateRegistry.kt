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

/**
 * An interface for plugging in components that consume and contribute to the saved state.
 *
 * This object's lifetime is bound to the lifecycle of the owning component. When the activity or
 * fragment is recreated, a new instance of this object is created as well.
 */
public expect class SavedStateRegistry internal constructor(impl: SavedStateRegistryImpl) {

    /**
     * Contributes to the saved state.
     *
     * Implementations can optionally implement [SavedStateRestorer] to receive and restore state
     * during the state restoration phase.
     */
    public fun interface SavedStateProvider : androidx.savedstate.SavedStateProvider {
        /**
         * Called to retrieve the state from a component before it is killed so the state can be
         * retrieved later from [consumeRestoredStateForKey].
         *
         * @return The [SavedState] containing the saved state.
         */
        override fun saveState(): SavedState
    }

    /**
     * Returns `true` if the state was restored after creation and can be safely consumed with
     * [consumeRestoredStateForKey], `false` otherwise.
     */
    public val isRestored: Boolean

    /**
     * Consumes the saved state previously supplied by a [SavedStateProvider] registered via
     * [registerSavedStateProvider] with the given [key].
     *
     * If the registered [SavedStateProvider] implements [SavedStateRestorer], the state is restored
     * automatically during restoration, and subsequent manual calls to this method with the same
     * key will return `null`.
     *
     * This call clears the internal reference to the returned saved state. Subsequent calls with
     * the same key will return `null`.
     *
     * All unconsumed values are saved during state saving.
     *
     * Call this method after the corresponding component has been created. Calling it before
     * creation results in an [IllegalArgumentException]. [Lifecycle.Event.ON_CREATE] signals that a
     * saved state can be safely consumed.
     *
     * @param key The key with which the [SavedStateProvider] was previously registered.
     * @return The previously saved state, or `null` if none exists or it has already been consumed.
     */
    @MainThread public fun consumeRestoredStateForKey(key: String): SavedState?

    /**
     * Registers a [SavedStateProvider] with the given [key].
     *
     * This [SavedStateProvider] will be called during the state saving phase. The returned state
     * will be associated with the given [key] and can be consumed after restoration via
     * [consumeRestoredStateForKey].
     *
     * If the registered [provider] implements [SavedStateRestorer], its
     * [SavedStateRestorer.restoreState] method will be automatically invoked during the state
     * restoration phase, or immediately if state has already been restored.
     *
     * If there is an unconsumed value with the same [key], the value supplied by the
     * [SavedStateProvider] overrides it and is written to the resulting saved state.
     *
     * If a provider was already registered with the given [key], throws an
     * [IllegalArgumentException].
     *
     * @param key The key with which the returned saved state is associated.
     * @param provider The [SavedStateProvider] to get the saved state.
     */
    @MainThread public fun registerSavedStateProvider(key: String, provider: SavedStateProvider)

    /**
     * Returns the [SavedStateProvider] previously registered with [registerSavedStateProvider], or
     * `null` if no provider has been registered with the given [key].
     *
     * @param key The key used to register the [SavedStateProvider].
     */
    public fun getSavedStateProvider(key: String): SavedStateProvider?

    /**
     * Unregisters a component previously registered with the given [key].
     *
     * @param key The key with which the component was previously registered.
     */
    @MainThread public fun unregisterSavedStateProvider(key: String)
}
