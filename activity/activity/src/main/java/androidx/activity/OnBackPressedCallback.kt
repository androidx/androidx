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
import androidx.navigationevent.NavigationEventCallback
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign

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
abstract class OnBackPressedCallback(enabled: Boolean) {

    /**
     * This [OnBackPressedCallback] class will delegate all interactions to [eventCallbacks], which
     * provides a KMP-compatible API while preserving behavior compatibility with existing callback
     * mechanisms.
     *
     * @see [OnBackPressedDispatcher.eventDispatcher]
     */
    private val eventCallbacks: MutableList<NavigationEventCallback> = mutableListOf()

    /**
     * The enabled state of the callback. Only when this callback is enabled will it receive
     * callbacks to [handleOnBackPressed].
     *
     * Note that the enabled state is an additional layer on top of the
     * [androidx.lifecycle.LifecycleOwner] passed to [OnBackPressedDispatcher.addCallback] which
     * controls when the callback is added and removed to the dispatcher.
     */
    @get:MainThread
    @set:MainThread
    var isEnabled: Boolean = enabled
        set(value) {
            field = value
            for (callback in eventCallbacks) {
                callback.isEnabled = value
            }
        }

    private val closeables = CopyOnWriteArrayList<AutoCloseable>()

    /** Removes this callback from any [OnBackPressedDispatcher] it is currently added to. */
    @MainThread
    fun remove() {
        for (closeable in closeables) {
            closeable.close()
        }
        closeables.clear()
        for (callback in eventCallbacks) {
            callback.remove()
        }
        eventCallbacks.clear()
    }

    /**
     * Callback for handling the system UI generated equivalent to
     * [OnBackPressedDispatcher.dispatchOnBackStarted].
     *
     * This will only be called by the framework on API 34 and above.
     */
    @Suppress("CallbackMethodName") /* mirror handleOnBackPressed local style */
    @MainThread
    open fun handleOnBackStarted(backEvent: BackEventCompat) {}

    /**
     * Callback for handling the system UI generated equivalent to
     * [OnBackPressedDispatcher.dispatchOnBackProgressed].
     *
     * This will only be called by the framework on API 34 and above.
     */
    @Suppress("CallbackMethodName") /* mirror handleOnBackPressed local style */
    @MainThread
    open fun handleOnBackProgressed(backEvent: BackEventCompat) {}

    /** Callback for handling the [OnBackPressedDispatcher.onBackPressed] event. */
    @MainThread abstract fun handleOnBackPressed()

    /**
     * Callback for handling the system UI generated equivalent to
     * [OnBackPressedDispatcher.dispatchOnBackCancelled].
     *
     * This will only be called by the framework on API 34 and above.
     */
    @Suppress("CallbackMethodName") /* mirror handleOnBackPressed local style */
    @MainThread
    open fun handleOnBackCancelled() {}

    internal fun addCloseable(closeable: AutoCloseable) {
        closeables += closeable
    }

    internal fun removeCloseable(closeable: AutoCloseable) {
        closeables -= closeable
    }

    internal fun createNavigationEventCallback(): NavigationEventCallback {
        val newCallback = EventCallback(this)
        eventCallbacks += newCallback
        return newCallback
    }

    private class EventCallback(private val onBackPressedCallback: OnBackPressedCallback) :
        NavigationEventCallback(isEnabled = onBackPressedCallback.isEnabled) {
        override fun onEventStarted(event: NavigationEvent) {
            onBackPressedCallback.handleOnBackStarted(BackEventCompat(event))
        }

        override fun onEventProgressed(event: NavigationEvent) {
            onBackPressedCallback.handleOnBackProgressed(BackEventCompat(event))
        }

        override fun onEventCompleted() {
            onBackPressedCallback.handleOnBackPressed()
        }

        override fun onEventCancelled() {
            onBackPressedCallback.handleOnBackCancelled()
        }
    }
}
