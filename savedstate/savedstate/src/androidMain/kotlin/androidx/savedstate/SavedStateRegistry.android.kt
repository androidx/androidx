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

actual class SavedStateRegistry
internal actual constructor(
    private val impl: SavedStateRegistryImpl,
) {

    @get:MainThread
    actual val isRestored: Boolean
        get() = impl.isRestored

    @MainThread
    actual fun consumeRestoredStateForKey(key: String): SavedState? =
        impl.consumeRestoredStateForKey(key)

    @MainThread
    actual fun registerSavedStateProvider(key: String, provider: SavedStateProvider) {
        impl.registerSavedStateProvider(key, provider)
    }

    actual fun getSavedStateProvider(key: String): SavedStateProvider? =
        impl.getSavedStateProvider(key)

    @MainThread
    actual fun unregisterSavedStateProvider(key: String) {
        impl.unregisterSavedStateProvider(key)
    }

    actual fun interface SavedStateProvider {
        actual fun saveState(): SavedState
    }

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

    private var recreatorProvider: Recreator.SavedStateProvider? = null

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
        check(impl.isAllowingSavingState) {
            "Can not perform this action after onSaveInstanceState"
        }
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
}
