/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.activity.compose.internal

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.CallSuper
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo

/**
 * A unified back handler that provides a single API for back gesture events.
 *
 * This abstract class simplifies back handling by providing a single set of methods (e.g.,
 * [onBackStarted], [onBackCompleted]) to implement. It then internally provides implementations for
 * both the legacy [OnBackPressedCallback] and the new [NavigationEventHandler], allowing it to be
 * used with either dispatcher system via [BackHandlerDispatcherCompat].
 *
 * Subclasses must implement [onBackCompleted] and can optionally override other back-handling
 * methods.
 *
 * @param info The [NavigationEventInfo] associated with this handler, used by the
 *   [navigationEventHandler].
 */
internal abstract class BackHandlerCompat(val info: NavigationEventInfo) {

    /** @see NavigationEventHandler.onBackStarted */
    open fun onBackStarted(event: BackEventCompat) {}

    /** @see NavigationEventHandler.onBackProgressed */
    open fun onBackProgressed(event: BackEventCompat) {}

    /** @see NavigationEventHandler.onBackCompleted */
    abstract fun onBackCompleted()

    /** @see NavigationEventHandler.onBackCancelled */
    open fun onBackCancelled() {}

    /** @see NavigationEventHandler.isBackEnabled */
    @get:CallSuper
    @set:CallSuper
    open var isBackEnabled: Boolean
        get() = onBackPressedCallback.isEnabled && navigationEventHandler.isBackEnabled
        set(value) {
            onBackPressedCallback.isEnabled = value
            navigationEventHandler.isBackEnabled = value
        }

    /**
     * The legacy [OnBackPressedCallback] implementation.
     *
     * It delegates all events to the unified methods of this [BackHandlerCompat] class. This is
     * used when [BackHandlerDispatcherCompat] is connected to an [OnBackPressedDispatcher].
     */
    val onBackPressedCallback =
        object : OnBackPressedCallback(enabled = false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                this@BackHandlerCompat.onBackStarted(backEvent)
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                this@BackHandlerCompat.onBackProgressed(backEvent)
            }

            override fun handleOnBackPressed() {
                this@BackHandlerCompat.onBackCompleted()
            }

            override fun handleOnBackCancelled() {
                this@BackHandlerCompat.onBackCancelled()
            }
        }

    /**
     * The new [NavigationEventHandler] implementation.
     *
     * It delegates all events to the unified methods of this [BackHandlerCompat] class. This is
     * used when [BackHandlerDispatcherCompat] is connected to a [NavigationEventDispatcher].
     */
    val navigationEventHandler =
        object : NavigationEventHandler<NavigationEventInfo>(info, isBackEnabled = false) {
            override fun onBackStarted(event: NavigationEvent) {
                this@BackHandlerCompat.onBackStarted(BackEventCompat(event))
            }

            override fun onBackProgressed(event: NavigationEvent) {
                this@BackHandlerCompat.onBackProgressed(BackEventCompat(event))
            }

            override fun onBackCompleted() {
                this@BackHandlerCompat.onBackCompleted()
            }

            override fun onBackCancelled() {
                this@BackHandlerCompat.onBackCancelled()
            }
        }
}
