/*
 * Copyright 2017 The Android Open Source Project
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
@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released
@file:OptIn(ExperimentalStdlibApi::class)

package androidx.lifecycle

import androidx.annotation.EmptySuper
import androidx.annotation.MainThread
import androidx.lifecycle.viewmodel.internal.SynchronizedObject
import androidx.lifecycle.viewmodel.internal.VIEW_MODEL_SCOPE_KEY
import androidx.lifecycle.viewmodel.internal.createViewModelScope
import androidx.lifecycle.viewmodel.internal.synchronized
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob

/**
 * Prepares and manages data for the UI.
 *
 * ViewModels are scoped to a [ViewModelStoreOwner] and are retained as long as their owner is
 * alive. This retention allows ViewModels to persist across configuration changes (e.g., Android
 * screen rotations), making the managed data immediately available to the new owner instance.
 *
 * Examples of [ViewModelStoreOwner]s include a `ComponentActivity` or a `Fragment`.
 *
 * The diagram below visualizes the relationship between the owner, store, and ViewModels:
 * ```
 *   ViewModelStoreOwner
 *            |
 *            v (owns)
 *     ViewModelStore <================= Retained across configuration changes
 *      |          |
 *      v          v
 *   ViewModelA  ViewModelB
 *                 |
 *                 v (destroys)
 *            onCleared() (called when owner is permanently destroyed)
 * ```
 *
 * Multiple UI components can share a single [ViewModel] to exchange data or coordinate state. To do
 * this, resolve the [ViewModel] using a shared, wider-scoped [ViewModelStoreOwner] (such as the
 * parent destination in a navigation graph, a parent fragment, or the containing activity).
 *
 * A ViewModel's sole responsibility is managing UI state and data. It must never hold references to
 * a `View` or any `Context` that could cause memory leaks.
 *
 * An example in Compose:
 * ```kotlin
 * class UserViewModel : ViewModel() {
 *     private val _user = MutableStateFlow<User?>(null)
 *     val user: StateFlow<User?> = _user.asStateFlow()
 * }
 *
 * @Composable
 * fun UserScreen(viewModel: UserViewModel = viewModel()) {
 *     val user by viewModel.user.collectAsStateWithLifecycle()
 *     UserContent(user)
 * }
 * ```
 *
 * @see ViewModelProvider
 * @see ViewModelStore
 * @see ViewModelStoreOwner
 */
public expect abstract class ViewModel {

    /**
     * Creates a new [ViewModel].
     *
     * You should **never** manually create a [ViewModel] outside of a [ViewModelProvider.Factory].
     */
    public constructor()

    /**
     * Creates a new [ViewModel].
     *
     * You should **never** manually create a [ViewModel] outside of a [ViewModelProvider.Factory].
     *
     * @param viewModelScope [CoroutineScope] to be canceled when the [ViewModel] is cleared, right
     *   **before** the [onCleared] method is called
     */
    public constructor(viewModelScope: CoroutineScope)

    /**
     * Creates a new [ViewModel].
     *
     * You should **never** manually create a [ViewModel] outside of a [ViewModelProvider.Factory].
     *
     * @param closeables resources to be closed when the [ViewModel] is cleared, right **before**
     *   the [onCleared] method is called
     */
    public constructor(vararg closeables: AutoCloseable)

    /**
     * Creates a new [ViewModel].
     *
     * You should **never** manually create a [ViewModel] outside of a [ViewModelProvider.Factory].
     *
     * @param viewModelScope [CoroutineScope] to be canceled when the [ViewModel] is cleared, right
     *   **before** the [onCleared] method is called
     * @param closeables resources to be closed when the [ViewModel] is cleared, right **before**
     *   the [onCleared] method is called
     */
    public constructor(viewModelScope: CoroutineScope, vararg closeables: AutoCloseable)

    /**
     * This method will be called when this [ViewModel] is no longer used and will be destroyed.
     *
     * It is useful when the [ViewModel] observes data, and you need to clear the subscriptions to
     * prevent a memory leak, as the subscriptions might hold a reference to the [ViewModel] even
     * after it is no longer needed.
     *
     * **Clearing Sequence:**
     * 1. [Close][AutoCloseable.close] resources added **with** a key via [addCloseable].
     * 2. [Close][AutoCloseable.close] resources added via `constructor`.
     * 3. [Close][AutoCloseable.close] resources added **without** a key via [addCloseable].
     * 4. Invoke the [onCleared] callback.
     */
    @EmptySuper protected open fun onCleared()

    /**
     * Clears all resources associated with this [ViewModel] and marks it as cleared.
     *
     * A cleared [ViewModel] should no longer be used, and any newly associated resources will be
     * immediately closed.
     *
     * **Clearing Sequence:**
     * 1. [Close][AutoCloseable.close] resources added **with** a key via [addCloseable].
     * 2. [Close][AutoCloseable.close] resources added via `constructor`.
     * 3. [Close][AutoCloseable.close] resources added **without** a key via [addCloseable].
     * 4. Invoke the [onCleared] callback.
     */
    @MainThread internal fun clear()

    /**
     * Adds an [AutoCloseable] resource with an associated [key] to this [ViewModel]. The resource
     * will be closed right **before** the [onCleared] method is called.
     *
     * If the [key] already has a resource associated with it, the old resource will be replaced and
     * closed immediately.
     *
     * If [onCleared] has already been called, the provided resource will not be added and will be
     * closed immediately.
     *
     * @param key key to associate with the resource, for retrieval with [getCloseable]
     * @param closeable resource to be closed when the [ViewModel] is cleared, right **before** the
     *   [onCleared] method is called
     */
    public fun addCloseable(key: String, closeable: AutoCloseable)

    /**
     * Adds an [AutoCloseable] resource to this [ViewModel]. The resource will be closed right
     * **before** the [onCleared] method is called.
     *
     * If [onCleared] has already been called, the provided resource will not be added and will be
     * closed immediately.
     *
     * @param closeable resource to be closed when the [ViewModel] is cleared, right **before** the
     *   [onCleared] method is called
     */
    public open fun addCloseable(closeable: AutoCloseable)

    /**
     * Returns the [AutoCloseable] resource associated to the given [key], or `null` if such a [key]
     * is not present in this [ViewModel].
     *
     * @param key key associated with a resource via [addCloseable]
     */
    public fun <T : AutoCloseable> getCloseable(key: String): T?
}

/**
 * The [CoroutineScope] associated with this [ViewModel].
 *
 * The [CoroutineScope.coroutineContext] is configured with:
 * - [SupervisorJob]: ensures children jobs can fail independently of each other.
 * - [MainCoroutineDispatcher.immediate]: executes jobs immediately on the main (UI) thread. If the
 *   [Dispatchers.Main] is not available on the current platform (e.g., Linux), we fall back to an
 *   [EmptyCoroutineContext].
 *
 * This scope is automatically canceled when the [ViewModel] is cleared, and can be replaced by
 * using the [ViewModel] constructor overload that takes in a `viewModelScope: CoroutineScope`.
 *
 * For background execution, use [kotlinx.coroutines.withContext] to switch to appropriate
 * dispatchers (e.g., `kotlinx.coroutines.IO`).
 *
 * @see ViewModel.onCleared
 */
public val ViewModel.viewModelScope: CoroutineScope
    get() =
        synchronized(VIEW_MODEL_SCOPE_LOCK) {
            getCloseable(VIEW_MODEL_SCOPE_KEY)
                ?: createViewModelScope().also { scope ->
                    addCloseable(VIEW_MODEL_SCOPE_KEY, scope)
                }
        }

private val VIEW_MODEL_SCOPE_LOCK = SynchronizedObject()
