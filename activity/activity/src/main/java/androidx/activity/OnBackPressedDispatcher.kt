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
package androidx.activity

import android.os.Build
import android.window.OnBackInvokedDispatcher
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationInputHandler

/**
 * Dispatcher that can be used to register [OnBackPressedCallback] instances for handling the
 * [ComponentActivity.onBackPressed] callback via composition.
 *
 * ```
 * class FormEntryFragment : Fragment() {
 *   override fun onAttach(context: Context) {
 *     super.onAttach(context)
 *     val callback = object : OnBackPressedCallback(
 *       true // default to enabled
 *     ) {
 *       override fun handleOnBackPressed() {
 *         showAreYouSureDialog()
 *       }
 *     }
 *     requireActivity().onBackPressedDispatcher.addCallback(
 *       this, // LifecycleOwner
 *       callback
 *     )
 *   }
 * }
 * ```
 *
 * When constructing an instance of this class, the [fallbackOnBackPressed] can be set to receive a
 * callback if [onBackPressed] is called when [hasEnabledCallbacks] returns `false`.
 */
// Implementation/API compatibility note: previous releases included only the Runnable? constructor,
// which permitted both first-argument and trailing lambda call syntax to specify
// fallbackOnBackPressed. To avoid silently breaking source compatibility the new
// primary constructor has no optional parameters to avoid ambiguity/wrong overload resolution
// when a single parameter is provided as a trailing lambda.
class OnBackPressedDispatcher(
    @Suppress("unused") private val fallbackOnBackPressed: Runnable?,
    @Suppress("unused") private val onHasEnabledCallbacksChanged: Consumer<Boolean>?,
) {

    /**
     * This [OnBackPressedDispatcher] class will delegate all interactions to [eventDispatcher],
     * which provides a KMP-compatible API while preserving behavior compatibility with existing
     * callback mechanisms.
     *
     * @see [OnBackPressedCallback.eventCallback]
     */
    internal val eventDispatcher: NavigationEventDispatcher by lazy {
        NavigationEventDispatcher(
            fallbackOnBackPressed = { fallbackOnBackPressed?.run() },
            onHasEnabledCallbacksChanged = { enabled ->
                onHasEnabledCallbacksChanged?.accept(enabled)
            },
        )
    }

    @JvmOverloads
    constructor(fallbackOnBackPressed: Runnable? = null) : this(fallbackOnBackPressed, null)

    /**
     * Sets the [OnBackInvokedDispatcher] for handling system back for Android SDK T+.
     *
     * @param invoker the OnBackInvokedDispatcher to be set on this dispatcher
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setOnBackInvokedDispatcher(invoker: OnBackInvokedDispatcher) {
        NavigationInputHandler(eventDispatcher).setOnBackInvokedDispatcher(invoker)
    }

    /**
     * Add a new [OnBackPressedCallback]. Callbacks are invoked in the reverse order in which they
     * are added, so this newly added [OnBackPressedCallback] will be the first callback to receive
     * a callback if [onBackPressed] is called.
     *
     * This method is **not** [Lifecycle] aware - if you'd like to ensure that you only get
     * callbacks when at least [started][Lifecycle.State.STARTED], use [addCallback]. It is expected
     * that you call [OnBackPressedCallback.remove] to manually remove your callback.
     *
     * @param onBackPressedCallback The callback to add
     * @see onBackPressed
     */
    @MainThread
    fun addCallback(onBackPressedCallback: OnBackPressedCallback) {
        eventDispatcher.addCallback(onBackPressedCallback.createNavigationEventCallback())
    }

    /**
     * Receive callbacks to a new [OnBackPressedCallback] when the given [LifecycleOwner] is at
     * least [started][Lifecycle.State.STARTED].
     *
     * This will automatically call [addCallback] and remove the callback as the lifecycle state
     * changes. As a corollary, if your lifecycle is already at least
     * [started][Lifecycle.State.STARTED], calling this method will result in an immediate call to
     * [addCallback].
     *
     * When the [LifecycleOwner] is [destroyed][Lifecycle.State.DESTROYED], it will automatically be
     * removed from the list of callbacks. The only time you would need to manually call
     * [OnBackPressedCallback.remove] is if you'd like to remove the callback prior to destruction
     * of the associated lifecycle.
     *
     * If the Lifecycle is already [destroyed][Lifecycle.State.DESTROYED] when this method is
     * called, the callback will not be added.
     *
     * @param owner The LifecycleOwner which controls when the callback should be invoked
     * @param onBackPressedCallback The callback to add
     * @see onBackPressed
     */
    @MainThread
    fun addCallback(owner: LifecycleOwner, onBackPressedCallback: OnBackPressedCallback) {
        val lifecycle = owner.lifecycle

        if (lifecycle.currentState === Lifecycle.State.DESTROYED) {
            return // Do not add the callback if the lifecycle is already destroyed.
        }

        // This observer manages the callback's lifecycle-aware registration.
        val lifecycleObserver =
            object : LifecycleEventObserver, AutoCloseable {
                private val eventCallback: NavigationEventCallback =
                    onBackPressedCallback.createNavigationEventCallback()

                /**
                 * Manages lifecycle-aware registration of an [OnBackPressedCallback].
                 *
                 * Adds the callback to the top of the [NavigationEventDispatcher]'s stack on
                 * `ON_START`, and removes it on `ON_STOP` without closing, allowing it to
                 * re-register on restart.
                 *
                 * On `ON_DESTROY`, the callback is permanently removed and cleaned up.
                 *
                 * Repeated add/remove calls ensure the callback stays at the top of its lifecycle
                 * group's dispatching stack, maintaining correct dispatch order based on lifecycle
                 * state.
                 */
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event === Lifecycle.Event.ON_START) {
                        // Register the INNER callback only when the lifecycle enters STARTED.
                        // NOTE: This ADDS the callback to the top of the dispatching stack.
                        eventDispatcher.addCallback(eventCallback)
                    } else if (event === Lifecycle.Event.ON_STOP) {
                        // Removes the callback from the dispatching stack.
                        eventCallback.remove()
                    } else if (event === Lifecycle.Event.ON_DESTROY) {
                        // Removes the callback from the dispatching stack.
                        eventCallback.remove()
                        // Stop lifecycle tracking if destroyed.
                        lifecycle.removeObserver(observer = this)
                    }
                }

                // Stop lifecycle tracking when the callback is removed manually.
                override fun close() {
                    lifecycle.removeObserver(observer = this)
                }
            }

        // Ensures `LifecycleOwner` events are tracked by this observer.
        lifecycle.addObserver(observer = lifecycleObserver)
        // Ensures `OnBackPressedCallback.remove()` will stop lifecycle tracking.
        onBackPressedCallback.addCloseable(closeable = lifecycleObserver)
    }

    /**
     * Returns `true` if there is at least one [enabled][OnBackPressedCallback.isEnabled] callback
     * registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    @MainThread fun hasEnabledCallbacks(): Boolean = eventDispatcher.hasEnabledCallbacks()

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackStarted(backEvent: BackEventCompat) {
        eventDispatcher.dispatchOnStarted(backEvent.toNavigationEvent())
    }

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackProgressed(backEvent: BackEventCompat) {
        eventDispatcher.dispatchOnProgressed(backEvent.toNavigationEvent())
    }

    /**
     * Trigger a call to the currently added [callbacks][OnBackPressedCallback] in reverse order in
     * which they were added. Only if the most recently added callback is not
     * [enabled][OnBackPressedCallback.isEnabled] will any previously added callback be called.
     *
     * If [hasEnabledCallbacks] is `false` when this method is called, the [fallbackOnBackPressed]
     * set by the constructor will be triggered.
     */
    @MainThread
    fun onBackPressed() {
        eventDispatcher.dispatchOnCompleted()
    }

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackCancelled() {
        eventDispatcher.dispatchOnCancelled()
    }
}

/**
 * Create and add a new [OnBackPressedCallback] that calls [onBackPressed] in
 * [OnBackPressedCallback.handleOnBackPressed].
 *
 * If an [owner] is specified, the callback will only be added when the Lifecycle is
 * [androidx.lifecycle.Lifecycle.State.STARTED].
 *
 * A default [enabled] state can be supplied.
 */
@Suppress("RegistrationName")
fun OnBackPressedDispatcher.addCallback(
    owner: LifecycleOwner? = null,
    enabled: Boolean = true,
    onBackPressed: OnBackPressedCallback.() -> Unit,
): OnBackPressedCallback {
    val callback =
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
    if (owner != null) {
        addCallback(owner, callback)
    } else {
        addCallback(callback)
    }
    return callback
}
