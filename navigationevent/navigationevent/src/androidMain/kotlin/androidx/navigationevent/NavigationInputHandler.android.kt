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

package androidx.navigationevent

import android.os.Build
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi

/**
 * Provides input to the given [NavigationEventDispatcher].
 *
 * TODO(mgalhardo): Consider moving this to `commonMain` once the design of `InputHandler` for other
 *   platforms is better understood.
 */
public class NavigationInputHandler(private val dispatcher: NavigationEventDispatcher) {
    private var invoker: OnBackInvokedDispatcher? = null
    private val onBackInvokedCallback: OnBackInvokedCallback? =
        if (Build.VERSION.SDK_INT == 33) {
            OnBackInvokedCallback { dispatcher.dispatchOnCompleted() }
        } else if (Build.VERSION.SDK_INT >= 34) {
            createOnBackAnimationCallback(dispatcher)
        } else {
            null
        }

    private var backInvokedCallbackRegistered = false

    /**
     * Sets the [OnBackInvokedDispatcher] for handling system back for Android SDK T+.
     *
     * @param invoker the [OnBackInvokedDispatcher] to be set on this dispatcher
     */
    @RequiresApi(33)
    public fun setOnBackInvokedDispatcher(invoker: OnBackInvokedDispatcher) {
        this.invoker = invoker
        dispatcher.addOnHasEnabledCallbacksChangedCallback { updateBackInvokedCallbackState() }
        updateBackInvokedCallbackState()
    }

    @RequiresApi(33)
    private fun updateBackInvokedCallbackState() {
        val shouldBeRegistered = dispatcher.hasEnabledCallbacks()
        val dispatcher = invoker
        if (dispatcher != null && onBackInvokedCallback != null) {
            if (shouldBeRegistered && !backInvokedCallbackRegistered) {
                dispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    onBackInvokedCallback,
                )
                backInvokedCallbackRegistered = true
            } else if (!shouldBeRegistered && backInvokedCallbackRegistered) {
                dispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
                backInvokedCallbackRegistered = false
            }
        }
    }

    @RequiresApi(34)
    private fun createOnBackAnimationCallback(
        dispatcher: NavigationEventDispatcher
    ): OnBackInvokedCallback {
        return object : OnBackAnimationCallback {
            override fun onBackStarted(backEvent: BackEvent) {
                dispatcher.dispatchOnStarted(NavigationEvent(backEvent))
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                dispatcher.dispatchOnProgressed(NavigationEvent(backEvent))
            }

            override fun onBackInvoked() {
                dispatcher.dispatchOnCompleted()
            }

            override fun onBackCancelled() {
                dispatcher.dispatchOnCancelled()
            }
        }
    }
}
