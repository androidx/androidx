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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.activity

import android.os.Build
import android.window.OnBackInvokedDispatcher
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.OnBackInvokedDefaultInput
import androidx.navigationevent.OnBackInvokedOverlayInput

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

    private var hasEnabledCallbacks = false

    /**
     * This [OnBackPressedDispatcher] class will delegate all interactions to [eventDispatcher],
     * which provides a KMP-compatible API while preserving behavior compatibility with existing
     * callback mechanisms.
     *
     * @see [OnBackPressedCallback.eventHandlers]
     */
    internal val eventDispatcher = NavigationEventDispatcher { fallbackOnBackPressed?.run() }

    /**
     * Input source representing back events initiated by this dispatcher (for example, via a direct
     * call to [onBackPressed]).
     */
    private val eventInput = OnBackPressedEventInput()

    init {
        // Connects this dispatcher's input to the event dispatcher.
        eventDispatcher.addInput(eventInput)
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
        eventDispatcher.addInput(
            OnBackInvokedDefaultInput(invoker),
            NavigationEventDispatcher.PRIORITY_DEFAULT,
        )
        eventDispatcher.addInput(
            OnBackInvokedOverlayInput(invoker),
            NavigationEventDispatcher.PRIORITY_OVERLAY,
        )
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
        val info = OnBackPressedCallbackInfo(onBackPressedCallback)
        val handler = onBackPressedCallback.createNavigationEventHandler(info)
        eventDispatcher.addHandler(handler)
    }

    /**
     * Registers a new [OnBackPressedCallback] that follows the lifecycle of the given
     * [LifecycleOwner].
     *
     * The callback is registered once and stays attached for the lifetime of the [LifecycleOwner].
     * Its [OnBackPressedCallback.isEnabled] state automatically follows the lifecycle: it becomes
     * enabled when the lifecycle is at least [Lifecycle.State.STARTED] and disabled otherwise.
     *
     * When the [LifecycleOwner] is [Lifecycle.State.DESTROYED], the callback is removed and
     * lifecycle tracking stops. If the lifecycle is already destroyed when this method is called,
     * the callback is not added.
     *
     * ## Legacy Behavior
     * To restore the legacy add/remove behavior, set
     * [ActivityFlags.isOnBackPressedLifecycleOrderMaintained] to `false`. In legacy mode, the
     * handler is added on [Lifecycle.Event.ON_START] and removed on [Lifecycle.Event.ON_STOP],
     * which may change dispatch ordering across lifecycle transitions.
     *
     * @param owner The [LifecycleOwner] that controls when the callback should be active.
     * @param onBackPressedCallback The callback to register.
     * @see onBackPressed
     */
    @MainThread
    @OptIn(ExperimentalActivityApi::class)
    fun addCallback(owner: LifecycleOwner, onBackPressedCallback: OnBackPressedCallback) {
        val lifecycle = owner.lifecycle

        if (lifecycle.currentState === State.DESTROYED) {
            return // Do not add the callback if the lifecycle is already destroyed.
        }

        val info = OnBackPressedCallbackInfo(onBackPressedCallback, owner)
        val eventHandler = onBackPressedCallback.createNavigationEventHandler(info)

        if (ActivityFlags.isOnBackPressedLifecycleOrderMaintained) {
            // Start disabled; will be enabled by lifecycle events.
            eventHandler.isBackEnabled = false

            // Add handler immediately to fix its position in the dispatch queue.
            eventDispatcher.addHandler(eventHandler)
        }

        // This observer manages the callback's lifecycle-aware registration.
        val lifecycleObserver =
            object : LifecycleEventObserver, AutoCloseable {
                override fun onStateChanged(source: LifecycleOwner, event: Event) {
                    // Sync enabled state with the lifecycle.
                    if (ActivityFlags.isOnBackPressedLifecycleOrderMaintained) {
                        eventHandler.isBackEnabled =
                            event.targetState.isAtLeast(State.STARTED) &&
                                onBackPressedCallback.isEnabled
                    } else {
                        if (event == Event.ON_START) {
                            // Register the INNER callback only when the lifecycle enters STARTED.
                            // NOTE: This ADDS the callback to the top of the dispatching stack.
                            eventDispatcher.addHandler(eventHandler)
                        } else if (event == Event.ON_STOP) {
                            // Removes the callback from the dispatching stack.
                            eventHandler.remove()
                        }
                    }

                    if (event == Event.ON_DESTROY) {
                        // Removes the callback from the dispatching stack.
                        eventHandler.remove()
                        // Stop lifecycle tracking if destroyed.
                        lifecycle.removeObserver(observer = this)
                    }
                }

                override fun close() {
                    // Stop lifecycle tracking when the callback is removed manually.
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
    @MainThread fun hasEnabledCallbacks(): Boolean = hasEnabledCallbacks

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackStarted(backEvent: BackEventCompat) {
        eventInput.backStarted(backEvent.toNavigationEvent())
    }

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackProgressed(backEvent: BackEventCompat) {
        eventInput.backProgressed(backEvent.toNavigationEvent())
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
        eventInput.backCompleted()
    }

    @VisibleForTesting
    @MainThread
    fun dispatchOnBackCancelled() {
        eventInput.backCancelled()
    }

    /**
     * Bridges [OnBackPressedDispatcher] to the underlying [NavigationEventDispatcher]:
     * - Exposes the protected `dispatch*` methods from [NavigationEventInput] so the outer
     *   [OnBackPressedDispatcher] can forward back events.
     * - Keeps [hasEnabledCallbacks] in sync with the dispatcher to preserve the legacy
     *   [hasEnabledCallbacks] method and [onHasEnabledCallbacksChanged] callback.
     */
    private inner class OnBackPressedEventInput : NavigationEventInput() {

        /**
         * Syncs the enabled-handler count back to [OnBackPressedDispatcher].
         *
         * This preserves the legacy [hasEnabledCallbacks] contract and triggers the external
         * [onHasEnabledCallbacksChanged] consumer when present.
         */
        override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
            hasEnabledCallbacks = hasEnabledHandlers
            onHasEnabledCallbacksChanged?.accept(hasEnabledHandlers)
        }

        /** Forwards a "back started" gesture to the dispatcher. */
        fun backStarted(event: NavigationEvent) {
            dispatchOnBackStarted(event)
        }

        /** Forwards intermediate progress for a back gesture. */
        fun backProgressed(event: NavigationEvent) {
            dispatchOnBackProgressed(event)
        }

        /** Forwards a cancellation of an in-progress back gesture. */
        fun backCancelled() {
            dispatchOnBackCancelled()
        }

        /** Forwards completion of a back gesture. */
        fun backCompleted() {
            dispatchOnBackCompleted()
        }
    }
}

/**
 * Creates and registers a new [OnBackPressedCallback] that invokes [onBackPressed].
 *
 * If a [LifecycleOwner] is provided, the callback’s enabled state automatically follows the
 * lifecycle: it is enabled while the lifecycle is at least [State.STARTED] and disabled otherwise.
 * The callback stays registered until the [LifecycleOwner] is destroyed.
 *
 * A default [enabled] state can be supplied.
 *
 * ## Legacy Behavior
 * To restore the legacy add/remove behavior, set
 * [ActivityFlags.isOnBackPressedLifecycleOrderMaintained] to `false`. In legacy mode, the handler
 * is added on [Lifecycle.Event.ON_START] and removed on [Lifecycle.Event.ON_STOP], which may change
 * dispatch ordering across lifecycle transitions.
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

private data class OnBackPressedCallbackInfo(
    val callback: OnBackPressedCallback,
    val owner: LifecycleOwner? = null,
) : NavigationEventInfo()
