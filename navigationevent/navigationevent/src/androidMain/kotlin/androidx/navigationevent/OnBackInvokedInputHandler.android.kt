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

/** Provides input from OnBackInvokedCallback to the given [NavigationEventDispatcher]. */
@RequiresApi(33)
public class OnBackInvokedInputHandler(
    dispatcher: NavigationEventDispatcher,
    private val onBackInvokedDispatcher: OnBackInvokedDispatcher,
) : NavigationEventInputHandler(dispatcher) {
    private val onBackInvokedCallback: OnBackInvokedCallback =
        if (Build.VERSION.SDK_INT == 33) {
            OnBackInvokedCallback { dispatchOnCompleted() }
        } else { // Build.VERSION.SDK_INT >= 34
            createOnBackAnimationCallback()
        }

    private var backInvokedCallbackRegistered = false

    init {
        addOnHasEnabledCallbacksChangedCallback { hasEnabledCallbacks ->
            updateBackInvokedCallbackState(hasEnabledCallbacks)
        }
        updateBackInvokedCallbackState(dispatcher.hasEnabledCallbacks())
    }

    private fun updateBackInvokedCallbackState(shouldBeRegistered: Boolean) {
        if (shouldBeRegistered && !backInvokedCallbackRegistered) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                onBackInvokedCallback,
            )
            backInvokedCallbackRegistered = true
        } else if (!shouldBeRegistered && backInvokedCallbackRegistered) {
            onBackInvokedDispatcher.unregisterOnBackInvokedCallback(onBackInvokedCallback)
            backInvokedCallbackRegistered = false
        }
    }

    @RequiresApi(34)
    private fun createOnBackAnimationCallback(): OnBackInvokedCallback {
        return object : OnBackAnimationCallback {
            override fun onBackStarted(backEvent: BackEvent) {
                dispatchOnStarted(NavigationEvent(backEvent))
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                dispatchOnProgressed(NavigationEvent(backEvent))
            }

            override fun onBackInvoked() {
                dispatchOnCompleted()
            }

            override fun onBackCancelled() {
                dispatchOnCancelled()
            }
        }
    }
}
