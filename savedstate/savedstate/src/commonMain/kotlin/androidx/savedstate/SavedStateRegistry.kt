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
import androidx.core.bundle.Bundle
import androidx.lifecycle.Lifecycle

/**
 * An interface for plugging components that consumes and contributes to the saved state.
 *
 *
 * This objects lifetime is bound to the lifecycle of owning component: when activity or
 * fragment is recreated, new instance of the object is created as well.
 */
expect class SavedStateRegistry internal constructor() {
    /**
     * Whether the state was restored after creation and can be safely consumed
     * with [consumeRestoredStateForKey].
     *
     * [isRestored] == true if state was restored
     */
    @get: MainThread
    var isRestored: Boolean
        private set

    /**
     * Consumes saved state previously supplied by [SavedStateProvider] registered
     * via [registerSavedStateProvider] with the given `key`.
     *
     *
     * This call clears an internal reference to returned saved state, so if you call it second time
     * in the row it will return `null`.
     *
     *
     * All unconsumed values will be saved during `onSaveInstanceState(Bundle savedState)`
     *
     *
     * This method can be called after `super.onCreate(savedStateBundle)` of the corresponding
     * component. Calling it before that will result in `IllegalArgumentException`.
     * [Lifecycle.Event.ON_CREATE] can be used as a signal
     * that a saved state can be safely consumed.
     *
     * @param key a key with which [SavedStateProvider] was previously registered.
     * @return `S` with the previously saved state or {@code null}
     */
    @MainThread
    fun consumeRestoredStateForKey(key: String): Bundle?

    /**
     * Registers a [SavedStateProvider] by the given `key`. This
     * `savedStateProvider` will be called
     * during state saving phase, returned object will be associated with the given `key`
     * and can be used after the restoration via [.consumeRestoredStateForKey].
     *
     *
     * If there is unconsumed value with the same `key`,
     * the value supplied by `savedStateProvider` will be overridden and
     * will be written to resulting saved state.
     *
     * If a provider was already registered with the given `key`, an implementation should
     * throw an [IllegalArgumentException]
     *
     * @param key      a key with which returned saved state will be associated
     * @param provider savedStateProvider to get saved state.
     */
    @MainThread
    fun registerSavedStateProvider(
        key: String,
        provider: SavedStateProvider
    )

    /**
     * Get a previously registered [SavedStateProvider].
     *
     * @param key The key used to register the [SavedStateProvider] when it was registered
     *            with registerSavedStateProvider(String, SavedStateProvider).
     *
     * Returns the [SavedStateProvider] previously registered with
     * [registerSavedStateProvider] or null if no provider
     * has been registered with the given key.
     */
    fun getSavedStateProvider(key: String): SavedStateProvider?

    /**
     * Unregisters a component previously registered by the given `key`
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    fun unregisterSavedStateProvider(key: String)

    /**
     * An interface for an owner of this [SavedStateRegistry] to attach this
     * to a [Lifecycle].
     */
    @MainThread
    internal fun performAttach(lifecycle: Lifecycle)

    /**
     * An interface for an owner of this [SavedStateRegistry] to restore saved state.
     *
     */
    @MainThread
    internal fun performRestore(savedState: Bundle?)

    /**
     * An interface for an owner of this [SavedStateRegistry]
     * to perform state saving, it will call all registered providers and
     * merge with unconsumed state.
     *
     * @param outBundle Bundle in which to place a saved state
     */
    @MainThread
    internal fun performSave(outBundle: Bundle)

    /**
     * This interface marks a component that contributes to saved state.
     */
    fun interface SavedStateProvider {
        /**
         * Called to retrieve a state from a component before being killed
         * so later the state can be received from [consumeRestoredStateForKey]
         *
         * Returns `S` with your saved state.
         */
        fun saveState(): Bundle
    }
}
