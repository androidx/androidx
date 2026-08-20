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
import androidx.savedstate.internal.SavedStateRegistryImpl

/**
 * Registry for components that consume and contribute to saved state.
 *
 * Use this registry to save and restore component state across process death or recreation.
 */
public expect class SavedStateRegistry : SavedStateProvider, SavedStateRestorer {

    /** Creates an empty [SavedStateRegistry]. */
    public constructor()

    /**
     * Creates a [SavedStateRegistry] initialized with [initialState].
     *
     * @param initialState The initial saved state to restore from.
     */
    public constructor(initialState: SavedState?)

    internal constructor(impl: SavedStateRegistryImpl)

    override fun saveState(): SavedState

    override fun restoreState(savedState: SavedState?)

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
     * Returns `true` if state has been restored and can be safely consumed with
     * [consumeRestoredStateForKey], `false` otherwise.
     */
    public val isRestored: Boolean

    /**
     * Consumes the saved state previously supplied by a [SavedStateProvider] registered with the
     * given [key].
     *
     * If the registered [SavedStateProvider] implements [SavedStateRestorer], the state is restored
     * automatically during restoration, and calls to this method with the same key return `null`.
     *
     * This call clears the internal reference to the returned saved state. Subsequent calls with
     * the same key return `null`.
     *
     * All unconsumed values are preserved during state saving.
     *
     * @param key The key with which the [SavedStateProvider] was previously registered.
     * @return The previously saved state, or `null` if none exists or it has already been consumed.
     */
    @MainThread public fun consumeRestoredStateForKey(key: String): SavedState?

    /**
     * Registers a [SavedStateProvider] with the given [key].
     *
     * This [SavedStateProvider] will be called during state saving. The returned state is
     * associated with the given [key] and can be consumed after restoration via
     * [consumeRestoredStateForKey].
     *
     * If the registered [provider] implements [SavedStateRestorer], its
     * [SavedStateRestorer.restoreState] method is automatically invoked during state restoration,
     * or immediately if state has already been restored.
     *
     * If a provider was already registered with the given [key], it is replaced with the new
     * [provider].
     *
     * @param key The key to associate with the provider.
     * @param provider The [SavedStateProvider] to register.
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
