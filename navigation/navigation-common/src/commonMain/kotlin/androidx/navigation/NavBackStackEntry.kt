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

@file:JvmName("NavBackStackEntryKt")
@file:JvmMultifileClass

package androidx.navigation

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.get
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.internal.NavContext
import androidx.navigation.serialization.decodeArguments
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer

/**
 * Representation of an entry in the back stack of a [androidx.navigation.NavController]. The
 * [Lifecycle], [ViewModelStore], and [SavedStateRegistry] provided via this object are valid for
 * the lifetime of this destination on the back stack: when this destination is popped off the back
 * stack, the lifecycle will be destroyed, state will no longer be saved, and ViewModels will be
 * cleared.
 */
public class NavBackStackEntry
private constructor(
    internal val context: NavContext?,
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public var destination: NavDestination,
    internal val immutableArgs: SavedState? = null,
    internal var hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
    internal val viewModelStoreProvider: NavViewModelStoreProvider? = null,
    public val id: String = randomUuid(),
    internal val savedState: SavedState? = null,
) :
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var savedStateRegistryAttached = false
    private val defaultFactory by lazy { SavedStateViewModelFactory() }

    private val lifecycleRegistry = LifecycleRegistry(this)

    /** Used to create the [SavedStateViewModel] */
    private val navResultSavedStateFactory by lazy {
        viewModelFactory { initializer { SavedStateViewModel(createSavedStateHandle()) } }
    }

    /**
     * The arguments used for this entry. Note that the arguments of a NavBackStackEntry are
     * immutable and defined when you `navigate()` to the destination - changes you make to this
     * SavedState will not be reflected in future calls to this property.
     *
     * @return The arguments used when this entry was created
     */
    public val arguments: SavedState?
        get() =
            if (immutableArgs == null) {
                null
            } else {
                savedState { putAll(immutableArgs) }
            }

    /** The [SavedStateHandle] for this entry. */
    @get:MainThread
    public val savedStateHandle: SavedStateHandle by lazy {
        check(savedStateRegistryAttached) {
            "You cannot access the NavBackStackEntry's SavedStateHandle until it is added to " +
                "the NavController's back stack (i.e., the Lifecycle of the NavBackStackEntry " +
                "reaches the CREATED state)."
        }
        check(lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "You cannot access the NavBackStackEntry's SavedStateHandle after the " +
                "NavBackStackEntry is destroyed."
        }
        ViewModelProvider.create(this, navResultSavedStateFactory).get<SavedStateViewModel>().handle
    }

    /**
     * {@inheritDoc}
     *
     * If the [androidx.navigation.NavHost] has not called
     * [androidx.navigation.NavHostController.setLifecycleOwner], the Lifecycle will be capped at
     * [Lifecycle.State.CREATED].
     */
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var maxLifecycle: Lifecycle.State = Lifecycle.State.INITIALIZED
        set(maxState) {
            field = maxState
            updateState()
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun handleLifecycleEvent(event: Lifecycle.Event) {
        hostLifecycleState = event.targetState
        updateState()
    }

    /** Update the state to be the lower of the two constraints: */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun updateState() {
        if (!savedStateRegistryAttached) {
            savedStateRegistryController.performAttach()
            savedStateRegistryAttached = true
            if (viewModelStoreProvider != null) {
                enableSavedStateHandles()
            }
            // Perform the restore just once, the first time updateState() is called
            // and specifically *before* we move up the Lifecycle
            savedStateRegistryController.performRestore(savedState)
        }
        if (hostLifecycleState.ordinal < maxLifecycle.ordinal) {
            lifecycleRegistry.currentState = hostLifecycleState
        } else {
            lifecycleRegistry.currentState = maxLifecycle
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if called before the [lifecycle] has moved to
     *   [Lifecycle.State.CREATED] or before the [androidx.navigation.NavHost] has called
     *   [androidx.navigation.NavHostController.setViewModelStore].
     */
    public override val viewModelStore: ViewModelStore
        get() {
            check(savedStateRegistryAttached) {
                "You cannot access the NavBackStackEntry's ViewModels until it is added to " +
                    "the NavController's back stack (i.e., the Lifecycle of the " +
                    "NavBackStackEntry reaches the CREATED state)."
            }
            check(lifecycle.currentState != Lifecycle.State.DESTROYED) {
                "You cannot access the NavBackStackEntry's ViewModels after the " +
                    "NavBackStackEntry is destroyed."
            }
            checkNotNull(viewModelStoreProvider) {
                "You must call setViewModelStore() on your NavHostController before " +
                    "accessing the ViewModelStore of a navigation graph."
            }
            return viewModelStoreProvider.get(id)
        }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = defaultFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val extras = MutableCreationExtras()
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            arguments?.let { args -> extras[DEFAULT_ARGS_KEY] = args }
            extras.setPlatformExtras(context)
            return extras
        }

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun saveState(outBundle: SavedState) {
        savedStateRegistryController.performSave(outBundle)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is NavBackStackEntry) return false

        val argsMatch =
            if (immutableArgs == other.immutableArgs) {
                true
            } else if (immutableArgs != null && other.immutableArgs != null) {
                immutableArgs.read { contentDeepEquals(other.immutableArgs) }
            } else {
                false
            }

        return id == other.id &&
            destination == other.destination &&
            lifecycle == other.lifecycle &&
            savedStateRegistry == other.savedStateRegistry &&
            argsMatch
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + destination.hashCode()
        immutableArgs?.read { result = 31 * result + contentDeepHashCode() }
        result = 31 * result + lifecycle.hashCode()
        result = 31 * result + savedStateRegistry.hashCode()
        return result
    }

    override fun toString(): String {
        return buildString {
            append(this::class.simpleName)
            append("($id)")
            append(" destination=")
            append(destination)
        }
    }

    public companion object {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(
            entry: NavBackStackEntry,
            arguments: SavedState? = entry.arguments,
        ): NavBackStackEntry =
            NavBackStackEntry(
                    entry.context,
                    entry.destination,
                    arguments,
                    entry.hostLifecycleState,
                    entry.viewModelStoreProvider,
                    entry.id,
                    entry.savedState,
                )
                .apply {
                    hostLifecycleState = entry.hostLifecycleState
                    maxLifecycle = entry.maxLifecycle
                }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun create(
            context: NavContext?,
            destination: NavDestination,
            arguments: SavedState? = null,
            hostLifecycleState: Lifecycle.State = Lifecycle.State.CREATED,
            viewModelStoreProvider: NavViewModelStoreProvider? = null,
            id: String = randomUuid(),
            savedState: SavedState? = null,
        ): NavBackStackEntry =
            NavBackStackEntry(
                context,
                destination,
                arguments,
                hostLifecycleState,
                viewModelStoreProvider,
                id,
                savedState,
            )

        internal fun randomUUID(): String = randomUuid()
    }
}

private class SavedStateViewModel(val handle: SavedStateHandle) : ViewModel()

/**
 * Returns route as an object of type [T]
 *
 * Extrapolates arguments from [NavBackStackEntry.arguments] and recreates object [T]
 *
 * @param [T] the entry's [NavDestination.route] as a [KClass]
 * @return A new instance of this entry's [NavDestination.route] as an object of type [T]
 */
public inline fun <reified T> NavBackStackEntry.toRoute(): T = toRoute(T::class)

/**
 * Returns route as an object of type [T]
 *
 * Extrapolates arguments from [NavBackStackEntry.arguments] and recreates object [T]
 *
 * @param [route] the entry's [NavDestination.route] as a [KClass]
 * @return A new instance of this entry's [NavDestination.route] as an object of type [T]
 */
@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
public fun <T> NavBackStackEntry.toRoute(route: KClass<*>): T {
    val savedState = arguments ?: savedState()
    val typeMap = destination.arguments.mapValues { it.value.type }
    return route.serializer().decodeArguments(savedState, typeMap) as T
}

internal expect fun randomUuid(): String

internal expect fun MutableCreationExtras.setPlatformExtras(context: NavContext?)
