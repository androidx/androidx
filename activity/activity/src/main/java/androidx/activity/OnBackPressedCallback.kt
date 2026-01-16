/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.annotation.MainThread
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Class for handling [OnBackPressedDispatcher.onBackPressed] callbacks without strongly coupling
 * that implementation to a subclass of [ComponentActivity].
 *
 * This class maintains its own [enabled state][isEnabled]. Only when this callback is enabled will
 * it receive callbacks to [handleOnBackPressed].
 *
 * Note that the enabled state is an additional layer on top of the
 * [androidx.lifecycle.LifecycleOwner] passed to [OnBackPressedDispatcher.addCallback] which
 * controls when the callback is added and removed to the dispatcher.
 *
 * By calling [remove], this callback will be removed from any [OnBackPressedDispatcher] it has been
 * added to. It is strongly recommended to instead disable this callback to handle temporary changes
 * in state.
 *
 * @param enabled The default enabled state for this callback.
 * @see OnBackPressedDispatcher
 */
public abstract class OnBackPressedCallback(enabled: Boolean) {

    /**
     * This [OnBackPressedCallback] class will delegate all interactions to [eventHandlers], which
     * provides a KMP-compatible API while preserving behavior compatibility with existing callback
     * mechanisms.
     *
     * @see [OnBackPressedDispatcher.eventDispatcher]
     */
    private val eventHandlers: MutableList<OnBackPressedEventHandler> = mutableListOf()

    /**
     * The enabled state of the callback. Only when this callback is enabled will it receive
     * callbacks to [handleOnBackPressed].
     *
     * When registered with a [androidx.lifecycle.LifecycleOwner], the callback is only active when
     * **both** this property is `true` and the [androidx.lifecycle.Lifecycle] is at least
     * [androidx.lifecycle.Lifecycle.State.STARTED].
     */
    @get:MainThread
    @set:MainThread
    public var isEnabled: Boolean = enabled
        set(value) {
            field = value
            for (callback in eventHandlers) {
                // Only enable if the Lifecycle is active. isLifecycleActive is always
                // true unless this callback was registered with a LifecycleOwner.
                callback.isBackEnabled = callback.isLifecycleActive && value
            }
        }

    private val closeables = CopyOnWriteArrayList<AutoCloseable>()

    /** Removes this callback from any [OnBackPressedDispatcher] it is currently added to. */
    @MainThread
    public fun remove() {
        for (closeable in closeables) {
            closeable.close()
        }
        closeables.clear()
        for (callback in eventHandlers) {
            callback.remove()
        }
        eventHandlers.clear()
    }

    /**
     * Callback for handling the system UI generated equivalent to
     * [OnBackPressedDispatcher.dispatchOnBackStarted].
     *
     * This will only be called by the framework on API 34 and above.
     */
    @Suppress("CallbackMethodName") /* mirror handleOnBackPressed local style */
    @MainThread
    public open fun handleOnBackStarted(backEvent: BackEventCompat) {}

    /**
     * Callback for handling the system UI generated equivalent to
     * [OnBackPressedDispatcher.dispatchOnBackProgressed].
     *
     * This will only be called by the framework on API 34 and above.
     */
    @Suppress("CallbackMethodName") /* mirror handleOnBackPressed local style */
    @MainThread
    public open fun handleOnBackProgressed(backEvent: BackEventCompat) {}

    /** Callback for handling the [OnBackPressedDispatcher.onBackPressed] event. */
    @MainThread public abstract fun handleOnBackPressed()

    /**
     * Callback for handling the system UI generated equivalent to
     * [OnBackPressedDispatcher.dispatchOnBackCancelled].
     *
     * This will only be called by the framework on API 34 and above.
     */
    @Suppress("CallbackMethodName") /* mirror handleOnBackPressed local style */
    @MainThread
    public open fun handleOnBackCancelled() {}

    internal fun addCloseable(closeable: AutoCloseable) {
        closeables += closeable
    }

    internal fun removeCloseable(closeable: AutoCloseable) {
        closeables -= closeable
    }

    internal fun createNavigationEventHandler(
        info: NavigationEventInfo
    ): OnBackPressedEventHandler {
        val newHandler = OnBackPressedEventHandler(onBackPressedCallback = this, info)
        eventHandlers += newHandler
        return newHandler
    }

    internal class OnBackPressedEventHandler(
        private val onBackPressedCallback: OnBackPressedCallback,
        info: NavigationEventInfo,
    ) :
        NavigationEventHandler<NavigationEventInfo>(
            initialInfo = info,
            isBackEnabled = onBackPressedCallback.isEnabled,
        ) {

        /**
         * Controls whether the associated `Lifecycle` is in an active state (at least
         * `Lifecycle.State.STARTED`).
         *
         * When this value changes, it automatically updates [isBackEnabled] to ensure the
         * underlying dispatcher is only enabled when **both** the lifecycle is active and the
         * [OnBackPressedCallback.isEnabled] is explicitly set to `true`.
         *
         * Defaults to `true` for use cases where no `LifecycleOwner` is associated.
         */
        var isLifecycleActive: Boolean = true
            set(value) {
                field = value
                // Automatically sync the effective state whenever the lifecycle state changes.
                isBackEnabled = value && onBackPressedCallback.isEnabled
            }

        override fun onBackStarted(event: NavigationEvent) {
            onBackPressedCallback.handleOnBackStarted(BackEventCompat(event))
        }

        override fun onBackProgressed(event: NavigationEvent) {
            onBackPressedCallback.handleOnBackProgressed(BackEventCompat(event))
        }

        override fun onBackCompleted() {
            onBackPressedCallback.handleOnBackPressed()
        }

        override fun onBackCancelled() {
            onBackPressedCallback.handleOnBackCancelled()
        }
    }
}
