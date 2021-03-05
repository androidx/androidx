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
package androidx.navigation

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.UUID

/**
 * Representation of an entry in the back stack of a [NavController]. The
 * [Lifecycle], [ViewModelStore], and [SavedStateRegistry] provided via
 * this object are valid for the lifetime of this destination on the back stack: when this
 * destination is popped off the back stack, the lifecycle will be destroyed, state
 * will no longer be saved, and ViewModels will be cleared.
 */
public class NavBackStackEntry @JvmOverloads internal constructor(
    private val context: Context,
    /**
     * Gets the destination associated with this entry
     * @return The destination that is currently visible to users
     */
    public val destination: NavDestination,
    /**
     * Gets the arguments used for this entry
     * @return The arguments used when this entry was created
     */
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var arguments: Bundle?,
    navControllerLifecycleOwner: LifecycleOwner?,
    private val navControllerViewModel: NavControllerViewModel?,
    // Internal unique name for this navBackStackEntry;
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val id: UUID = UUID.randomUUID(),
    savedState: Bundle? = null
) : LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    private var lifecycle = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var hostLifecycle = Lifecycle.State.CREATED
    private val defaultFactory by lazy {
        SavedStateViewModelFactory((context.applicationContext as Application), this, arguments)
    }

    /**
     * Gets the [SavedStateHandle] for this entry.
     *
     * @return the SavedStateHandle for this entry
     */
    public val savedStateHandle: SavedStateHandle by lazy {
        ViewModelProvider(
            this, NavResultSavedStateFactory(this, null)
        ).get(SavedStateViewModel::class.java).handle
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun replaceArguments(newArgs: Bundle?) {
        arguments = newArgs
    }

    /**
     * {@inheritDoc}
     *
     * If the [NavHost] has not called [NavHostController.setLifecycleOwner], the
     * Lifecycle will be capped at [Lifecycle.State.CREATED].
     */
    public override fun getLifecycle(): Lifecycle {
        return lifecycle
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var maxLifecycle: Lifecycle.State = Lifecycle.State.RESUMED
        set(maxState) {
            field = maxState
            updateState()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun handleLifecycleEvent(event: Lifecycle.Event) {
        hostLifecycle = getStateAfter(event)
        updateState()
    }

    /**
     * Update the state to be the lower of the two constraints:
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun updateState() {
        if (hostLifecycle.ordinal < maxLifecycle.ordinal) {
            lifecycle.currentState = hostLifecycle
        } else {
            lifecycle.currentState = maxLifecycle
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if called before the [NavHost] has called
     * [NavHostController.setViewModelStore].
     */
    public override fun getViewModelStore(): ViewModelStore {
        checkNotNull(navControllerViewModel) {
            "You must call setViewModelStore() on your NavHostController before accessing the " +
                "ViewModelStore of a navigation graph."
        }
        return navControllerViewModel.getViewModelStore(id)
    }

    public override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return defaultFactory
    }

    public override fun getSavedStateRegistry(): SavedStateRegistry {
        return savedStateRegistryController.savedStateRegistry
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun saveState(outBundle: Bundle) {
        savedStateRegistryController.performSave(outBundle)
    }

    /**
     * Used to create the {SavedStateViewModel}
     */
    private class NavResultSavedStateFactory(
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle?
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return SavedStateViewModel(handle) as T
        }
    }

    private class SavedStateViewModel(val handle: SavedStateHandle) : ViewModel()

    internal companion object {
        /**
         * Copied from LifecycleRegistry.getStateAfter()
         * TODO: update to Event.getTargetState() when navigation's lifecycle-core dependency is updated
         */
        internal fun getStateAfter(event: Lifecycle.Event): Lifecycle.State {
            when (event) {
                Lifecycle.Event.ON_CREATE, Lifecycle.Event.ON_STOP -> return Lifecycle.State.CREATED
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_PAUSE -> return Lifecycle.State.STARTED
                Lifecycle.Event.ON_RESUME -> return Lifecycle.State.RESUMED
                Lifecycle.Event.ON_DESTROY -> return Lifecycle.State.DESTROYED
                Lifecycle.Event.ON_ANY -> {
                }
            }
            throw IllegalArgumentException("Unexpected event value $event")
        }
    }

    init {
        savedStateRegistryController.performRestore(savedState)
        if (navControllerLifecycleOwner != null) {
            hostLifecycle = navControllerLifecycleOwner.lifecycle.currentState
        }
    }
}
