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
@file:OptIn(ExperimentalStdlibApi::class)

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.lifecycle.viewmodel.internal.Lock
import androidx.lifecycle.viewmodel.internal.VIEW_MODEL_SCOPE_KEY
import androidx.lifecycle.viewmodel.internal.createViewModelScope
import androidx.lifecycle.viewmodel.internal.withLock
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob

/**
 * ViewModel is a class that is responsible for preparing and managing the data for
 * an [Activity][androidx.activity.ComponentActivity] or a [Fragment][androidx.fragment.app.Fragment].
 * It also handles the communication of the Activity / Fragment with the rest of the application
 * (e.g. calling the business logic classes).
 *
 * A ViewModel is always created in association with a scope (a fragment or an activity) and will
 * be retained as long as the scope is alive. E.g. if it is an Activity, until it is finished.
 *
 * In other words, this means that a ViewModel will not be destroyed if its owner is destroyed for a
 * configuration change (e.g. rotation). The new owner instance just re-connects to the existing
 * model.
 *
 * The purpose of the ViewModel is to acquire and keep the information that is necessary for an
 * Activity or a Fragment. The Activity or the Fragment should be able to observe changes in the
 * ViewModel. ViewModels usually expose this information via [Lifecycle][androidx.lifecycle.LiveData] or
 * Android Data Binding. You can also use any observability construct from your favorite framework.
 *
 * ViewModel's only responsibility is to manage the data for the UI. It **should never** access
 * your view hierarchy or hold a reference back to the Activity or the Fragment.
 *
 * Typical usage from an Activity standpoint would be:
 *
 * ```
 * class UserActivity : ComponentActivity {
 *     private val viewModel by viewModels<UserViewModel>()
 *
 *     override fun onCreate(savedInstanceState: Bundle) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.user_activity_layout)
 *         viewModel.user.observe(this) { user: User ->
 *             // update ui.
 *         }
 *         requireViewById(R.id.button).setOnClickListener {
 *             viewModel.doAction()
 *         }
 *     }
 * }
 * ```
 *
 * ViewModel would be:
 *
 * ```
 * class UserViewModel : ViewModel {
 *     private val userLiveData = MutableLiveData<User>()
 *     val user: LiveData<User> get() = userLiveData
 *
 *     init {
 *         // trigger user load.
 *     }
 *
 *     fun doAction() {
 *         // depending on the action, do necessary business logic calls and update the
 *         // userLiveData.
 *     }
 * }
 * ```
 *
 * ViewModels can also be used as a communication layer between different Fragments of an Activity.
 * Each Fragment can acquire the ViewModel using the same key via their Activity. This allows
 * communication between Fragments in a de-coupled fashion such that they never need to talk to
 * the other Fragment directly.
 *
 * ```
 * class MyFragment : Fragment {
 *   val viewModel by activityViewModels<UserViewModel>()
 * }
 *```
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
     * @param viewModelScope a [CoroutineScope] to be cancelled when the [ViewModel] is cleared,
     *  right **before** the [onCleared] method is called.
     */
    public constructor(viewModelScope: CoroutineScope)

    /**
     * Creates a new [ViewModel].
     *
     * You should **never** manually create a [ViewModel] outside of a [ViewModelProvider.Factory].
     *
     * @param closeables the resources to be closed when the [ViewModel] is cleared,
     *  right **before** the [onCleared] method is called.
     */
    public constructor(vararg closeables: AutoCloseable)

    /**
     * Creates a new [ViewModel].
     *
     * You should **never** manually create a [ViewModel] outside of a [ViewModelProvider.Factory].
     *
     * @param viewModelScope a [CoroutineScope] to be cancelled when the [ViewModel] is cleared,
     *  right **before** the [onCleared] method is called.
     * @param closeables the resources to be closed when the [ViewModel] is cleared,
     *  right **before** the [onCleared] method is called.
     */
    public constructor(viewModelScope: CoroutineScope, vararg closeables: AutoCloseable)

    /**
     * This method will be called when this [ViewModel] is no longer used and will be destroyed.
     *
     * It is useful when the [ViewModel] observes data and you need to clear the subscriptions to
     * prevent a memory leak, as the subscriptions might hold a reference to the [ViewModel] even
     * after it is no longer needed.
     *
     * **Clearing Sequence:**
     * 1. [AutoCloseable.close] resources added **without** a key via [addCloseable].
     * 2. [AutoCloseable.close] resources added **with** a key via [addCloseable].
     * 3. Invoke the [onCleared] callback.
     */
    protected open fun onCleared()

    /**
     * Clears all resources associated with this [ViewModel] and marks it as cleared.
     *
     * A cleared [ViewModel] should no longer be used, and any newly associated resources will be
     * immediately closed.
     *
     * **Clearing Sequence:**
     * 1. [AutoCloseable.close] resources added **without** a key via [addCloseable].
     * 2. [AutoCloseable.close] resources added **with** a key via [addCloseable].
     * 3. Invoke the [onCleared] callback.
     */
    @MainThread
    internal fun clear()

    /**
     * Adds an [AutoCloseable] resource with an associated [key] to this [ViewModel]. The resource
     * will be closed right **before** the [onCleared] method is called.
     *
     * If the [key] already has a resource associated with it, the old resource will be replaced
     * and closed immediately.
     *
     * If [onCleared] has already been called, the provided resource will not be added and will be
     * closed immediately.
     *
     * @param key the key to associate with the resource, for retrieval with [getCloseable].
     * @param closeable the resource to be closed when the [ViewModel] is cleared,
     *  right **before** the [onCleared] method is called.
     */
    public fun addCloseable(key: String, closeable: AutoCloseable)

    /**
     * Adds an [AutoCloseable] resource to this [ViewModel]. The resource will be closed right
     * **before** the [onCleared] method is called.
     *
     * If [onCleared] has already been called, the provided resource will not be added and will be
     * closed immediately.
     *
     * @param closeable the resource to be closed when the [ViewModel] is cleared,
     *  right **before** the [onCleared] method is called.
     */
    public open fun addCloseable(closeable: AutoCloseable)

    /**
     * Returns the [AutoCloseable] resource associated to the given [key], or `null` if such a
     * [key] is not present in this [ViewModel].
     *
     * @param key the key associated with a resource via [addCloseable].
     */
    public fun <T : AutoCloseable> getCloseable(key: String): T?
}

/**
 * The [CoroutineScope] associated with this [ViewModel].
 *
 * The [CoroutineScope.coroutineContext] is configured with:
 * - [SupervisorJob]: ensures children jobs can fail independently of each other.
 * - [MainCoroutineDispatcher.immediate]: executes jobs immediately on the main (UI) thread. If
 *  the [Dispatchers.Main] is not available on the current platform (e.g., Linux), we fallback
 *  to an [EmptyCoroutineContext].
 *
 * This scope is automatically cancelled when the [ViewModel] is cleared, and can be replaced by
 * using the [ViewModel] constructor overload that takes in a `viewModelScope: CoroutineScope`.
 *
 * For background execution, use [kotlinx.coroutines.withContext] to switch to appropriate
 * dispatchers (e.g., [kotlinx.coroutines.IO]).
 *
 * @see ViewModel.onCleared
 */
public val ViewModel.viewModelScope: CoroutineScope
    get() = viewModelScopeLock.withLock {
        getCloseable(VIEW_MODEL_SCOPE_KEY)
            ?: createViewModelScope().also { scope -> addCloseable(VIEW_MODEL_SCOPE_KEY, scope) }
    }

private val viewModelScopeLock = Lock()
