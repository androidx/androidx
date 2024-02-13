/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.MainThread
import java.io.Closeable
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * ViewModel is a class that is responsible for preparing and managing the data for
 * an [Activity][android.app.Activity] or a [Fragment][androidx.fragment.app.Fragment].
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
public abstract class ViewModel {

    /**
     * Holds a mapping between [String] keys and [AutoCloseable] resources that have been associated
     * with this [ViewModel].
     *
     * The associated resources will be [AutoCloseable.close] right before the [ViewModel.onCleared]
     * is called. This provides automatic resource cleanup upon [ViewModel] release.
     *
     * The clearing order is:
     * 1. [keyToCloseables][AutoCloseable.close]
     * 2. [closeables][Closeable.close]
     * 3. [ViewModel.onCleared]
     *
     * Compatibility considerations:
     * - **Why manual synchronization?** [java.util.concurrent.ConcurrentHashMap] can cause value
     * loss on the Android API 21 and 22. Using a regular map and controlling access with
     * [synchronized] guarantees reliable cleanup even on older devices. See b/37042460.
     * - **Why nullable collections?** Since [clear] is final, this method is still called on mock
     * objects. In those cases, [bagOfTags] is `null`. It'll always be empty though because
     * [addCloseable] and [getCloseable] are open so we can skip clearing it.
     */
    @Suppress("RedundantNullableReturnType")
    private val bagOfTags: MutableMap<String, AutoCloseable>? = mutableMapOf()

    /**
     * @see [bagOfTags]
     */
    @Suppress("RedundantNullableReturnType")
    private val closeables: MutableSet<AutoCloseable>? = mutableSetOf()

    @Volatile
    private var isCleared = false

    /**
     * Construct a new ViewModel instance.
     *
     * You should **never** manually construct a ViewModel outside of a
     * [ViewModelProvider.Factory].
     */
    public constructor()

    /**
     * Construct a new ViewModel instance. Any [AutoCloseable] objects provided here
     * will be closed directly before [ViewModel.onCleared] is called.
     *
     * You should **never** manually construct a ViewModel outside of a
     * [ViewModelProvider.Factory].
     */
    @Deprecated(message = "Replaced by `AutoCloseable` overload.", level = DeprecationLevel.HIDDEN)
    public constructor(vararg closeables: Closeable) {
        if (this.closeables != null) {
            this.closeables += closeables
        }
    }

    /**
     * Construct a new ViewModel instance. Any [AutoCloseable] objects provided here
     * will be closed directly before [ViewModel.onCleared] is called.
     *
     * You should **never** manually construct a ViewModel outside of a
     * [ViewModelProvider.Factory].
     */
    public constructor(vararg closeables: AutoCloseable) {
        if (this.closeables != null) {
            this.closeables += closeables
        }
    }

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     *
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    protected open fun onCleared() {}

    @MainThread
    internal fun clear() {
        isCleared = true
        if (bagOfTags != null) {
            synchronized(bagOfTags) {
                for (value in bagOfTags.values) {
                    // see comment for the similar call in `setTagIfAbsent`
                    closeWithRuntimeException(value)
                }
            }
        }
        if (closeables != null) {
            synchronized(closeables) {
                for (closeable in closeables) {
                    closeWithRuntimeException(closeable)
                }
            }
            closeables.clear()
        }
        onCleared()
    }

    /**
     * Add a new [AutoCloseable] object that will be closed directly before
     * [ViewModel.onCleared] is called.
     *
     * If `onCleared()` has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     *
     * @param key A key that allows you to retrieve the closeable passed in by using the same
     *            key with [ViewModel.getCloseable]
     * @param closeable The object that should be [AutoCloseable.close] directly before
     *                  [ViewModel.onCleared] is called.
     */
    public fun addCloseable(key: String, closeable: AutoCloseable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (isCleared) {
            closeWithRuntimeException(closeable)
            return
        }

        if (bagOfTags != null) {
            synchronized(bagOfTags) { bagOfTags.put(key, closeable) }
        }
    }

    /**
     * Add a new [AutoCloseable] object that will be closed directly before
     * [ViewModel.onCleared] is called.
     *
     * If `onCleared()` has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     *
     * @param closeable The object that should be [closed][AutoCloseable.close] directly before
     *                  [ViewModel.onCleared] is called.
     */
    public open fun addCloseable(closeable: AutoCloseable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (isCleared) {
            closeWithRuntimeException(closeable)
            return
        }

        if (this.closeables != null) {
            synchronized(this.closeables) {
                this.closeables += closeable
            }
        }
    }

    /**
     * Add a new [Closeable] object that will be closed directly before
     * [ViewModel.onCleared] is called.
     *
     * If `onCleared()` has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     *
     * @param closeable The object that should be [closed][Closeable.close] directly before
     *                  [ViewModel.onCleared] is called.
     */
    @Deprecated(message = "Replaced by `AutoCloseable` overload.", level = DeprecationLevel.HIDDEN)
    public open fun addCloseable(closeable: Closeable) {
        // Upcast to `AutoCloseable` to avoid recursive call.
        addCloseable(closeable as AutoCloseable)
    }

    /**
     * Returns the closeable previously added with [ViewModel.addCloseable] with the given [key].
     *
     * @param key The key that was used to add the Closeable.
     */
    public fun <T : AutoCloseable> getCloseable(key: String): T? {
        if (bagOfTags == null) {
            return null
        }
        synchronized(bagOfTags) {
            @Suppress("UNCHECKED_CAST")
            return bagOfTags[key] as T?
        }
    }

    private fun closeWithRuntimeException(instance: Any) {
        if (instance is Closeable) {
            try {
                instance.close()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}

private const val JOB_KEY = "androidx.lifecycle.ViewModelCoroutineScope.JOB_KEY"

/**
 * [CoroutineScope] tied to this [ViewModel].
 * This scope will be canceled when ViewModel will be cleared, i.e. [ViewModel.onCleared] is called
 *
 * This scope is bound to
 * [Dispatchers.Main.immediate][kotlinx.coroutines.MainCoroutineDispatcher.immediate]
 */
public val ViewModel.viewModelScope: CoroutineScope
    get() {
        return getCloseable<CloseableCoroutineScope>(JOB_KEY) ?: CloseableCoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate
        ).also { newClosableScope ->
            addCloseable(JOB_KEY, newClosableScope)
        }
    }

private class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
        coroutineContext.cancel()
    }
}
