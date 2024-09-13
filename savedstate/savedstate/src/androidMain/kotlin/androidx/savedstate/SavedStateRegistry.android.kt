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
package androidx.savedstate

import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.savedstate.internal.SavedStateRegistryImpl

/**
 * An interface for plugging components that consumes and contributes to the saved state.
 *
 * This objects lifetime is bound to the lifecycle of owning component: when activity or fragment is
 * recreated, new instance of the object is created as well.
 */
class SavedStateRegistry internal constructor(
    private val impl: SavedStateRegistryImpl,
) {

    /**
     * Whether the state was restored after creation and can be safely consumed with
     * [consumeRestoredStateForKey].
     *
     * [isRestored] == true if state was restored
     */
    @get:MainThread
    val isRestored get() = impl.isRestored

    private var recreatorProvider: Recreator.SavedStateProvider? = null

    /**
     * Consumes saved state previously supplied by [SavedStateProvider] registered via
     * [registerSavedStateProvider] with the given `key`.
     *
     * This call clears an internal reference to returned saved state, so if you call it second time
     * in the row it will return `null`.
     *
     * All unconsumed values will be saved during `onSaveInstanceState(SavedState savedState)`
     *
     * This method can be called after `super.onCreate(savedStateBundle)` of the corresponding
     * component. Calling it before that will result in `IllegalArgumentException`.
     * [Lifecycle.Event.ON_CREATE] can be used as a signal that a saved state can be safely
     * consumed.
     *
     * @param key a key with which [SavedStateProvider] was previously registered.
     * @return `S` with the previously saved state or {@code null}
     */
    @MainThread
    fun consumeRestoredStateForKey(key: String): SavedState? = impl.consumeRestoredStateForKey(key)

    /**
     * Registers a [SavedStateProvider] by the given `key`. This `savedStateProvider` will be called
     * during state saving phase, returned object will be associated with the given `key` and can be
     * used after the restoration via [.consumeRestoredStateForKey].
     *
     * If there is unconsumed value with the same `key`, the value supplied by `savedStateProvider`
     * will be overridden and will be written to resulting saved state.
     *
     * If a provider was already registered with the given `key`, an implementation should throw an
     * [IllegalArgumentException]
     *
     * @param key a key with which returned saved state will be associated
     * @param provider savedStateProvider to get saved state.
     */
    @MainThread
    fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        impl.registerSavedStateProvider(key, provider)
    }

    /**
     * Get a previously registered [SavedStateProvider].
     *
     * @param key The key used to register the [SavedStateProvider] when it was registered with
     *   registerSavedStateProvider(String, SavedStateProvider).
     *
     * Returns the [SavedStateProvider] previously registered with [registerSavedStateProvider] or
     * null if no provider has been registered with the given key.
     */
    fun getSavedStateProvider(key: String): SavedStateProvider? = impl.getSavedStateProvider(key)

    /**
     * Unregisters a component previously registered by the given `key`
     *
     * @param key a key with which a component was previously registered.
     */
    @MainThread
    fun unregisterSavedStateProvider(key: String) = impl.unregisterSavedStateProvider(key)

    /**
     * Subclasses of this interface will be automatically recreated if they were previously
     * registered via [runOnNextRecreation].
     *
     * Subclasses must have a default constructor
     */
    interface AutoRecreated {
        /**
         * This method will be called during dispatching of
         * [androidx.lifecycle.Lifecycle.Event.ON_CREATE] of owning component which was restarted
         *
         * @param owner a component that was restarted
         */
        fun onRecreated(owner: SavedStateRegistryOwner)
    }

    /**
     * Executes the given class when the owning component restarted.
     *
     * The given class will be automatically instantiated via default constructor and method
     * [AutoRecreated.onRecreated] will be called. It is called as part of dispatching of
     * [androidx.lifecycle.Lifecycle.Event.ON_CREATE] event.
     *
     * @param clazz that will need to be instantiated on the next component recreation
     * @throws IllegalArgumentException if you try to call if after [Lifecycle.Event.ON_STOP] was
     *   dispatched
     */
    @MainThread
    fun runOnNextRecreation(clazz: Class<out AutoRecreated>) {
        check(impl.isAllowingSavingState) { "Can not perform this action after onSaveInstanceState" }
        recreatorProvider = recreatorProvider ?: Recreator.SavedStateProvider(this)
        try {
            clazz.getDeclaredConstructor()
        } catch (e: NoSuchMethodException) {
            throw IllegalArgumentException(
                "Class ${clazz.simpleName} must have " +
                    "default constructor in order to be automatically recreated",
                e
            )
        }
        recreatorProvider?.add(clazz.name)
    }

    /** This interface marks a component that contributes to saved state. */
    fun interface SavedStateProvider {
        /**
         * Called to retrieve a state from a component before being killed so later the state can be
         * received from [consumeRestoredStateForKey]
         *
         * Returns `S` with your saved state.
         */
        fun saveState(): SavedState
    }

    private companion object {
        private const val SAVED_COMPONENTS_KEY =
            "androidx.lifecycle.BundlableSavedStateRegistry.key"
    }
}
