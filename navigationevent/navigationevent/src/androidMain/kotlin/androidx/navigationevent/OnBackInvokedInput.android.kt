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
 * Provides input to a [NavigationEventDispatcher] by registering an [OnBackInvokedCallback] with
 * the [OnBackInvokedDispatcher.PRIORITY_DEFAULT] priority to [onBackInvokedDispatcher]. The input
 * will unregister this callback when there's no enabled [NavigationEventHandler] with
 * [NavigationEventDispatcher.PRIORITY_DEFAULT] priority.
 *
 * @param onBackInvokedDispatcher the [OnBackInvokedDispatcher] to use.
 */
@RequiresApi(33)
public class OnBackInvokedDefaultInput(onBackInvokedDispatcher: OnBackInvokedDispatcher) :
    OnBackInvokedInput(onBackInvokedDispatcher, OnBackInvokedDispatcher.PRIORITY_DEFAULT)

/**
 * Provides input to a [NavigationEventDispatcher] by registering an [OnBackInvokedCallback] with
 * the [OnBackInvokedDispatcher.PRIORITY_OVERLAY] priority to [onBackInvokedDispatcher]. The input
 * will unregister this callback when there's no enabled [NavigationEventHandler] with
 * [NavigationEventDispatcher.PRIORITY_OVERLAY] priority.
 *
 * @param onBackInvokedDispatcher the [OnBackInvokedDispatcher] to use.
 */
@RequiresApi(33)
public class OnBackInvokedOverlayInput(onBackInvokedDispatcher: OnBackInvokedDispatcher) :
    OnBackInvokedInput(onBackInvokedDispatcher, OnBackInvokedDispatcher.PRIORITY_OVERLAY)

/**
 * Provides input to a [NavigationEventDispatcher] by registering an [OnBackInvokedCallback] to the
 * passed in [onBackInvokedDispatcher].
 *
 * @param onBackInvokedDispatcher the [OnBackInvokedDispatcher] to use.
 * @param onBackInvokedCallbackPriority the priority to use.
 */
@RequiresApi(33)
public sealed class OnBackInvokedInput(
    private val onBackInvokedDispatcher: OnBackInvokedDispatcher,
    private val onBackInvokedCallbackPriority: Int,
) : NavigationEventInput() {

    private val onBackInvokedCallback: OnBackInvokedCallback =
        if (Build.VERSION.SDK_INT == 33) {
            OnBackInvokedCallback { dispatchOnBackCompleted() }
        } else { // Build.VERSION.SDK_INT >= 34
            createOnBackAnimationCallback()
        }

    private var backInvokedCallbackRegistered = false

    override fun onRemoved() {
        updateBackInvokedCallbackState(false)
    }

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        updateBackInvokedCallbackState(hasEnabledHandlers)
    }

    private fun updateBackInvokedCallbackState(shouldBeRegistered: Boolean) {
        if (shouldBeRegistered && !backInvokedCallbackRegistered) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                onBackInvokedCallbackPriority,
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
                dispatchOnBackStarted(NavigationEvent(backEvent))
            }

            override fun onBackProgressed(backEvent: BackEvent) {
                dispatchOnBackProgressed(NavigationEvent(backEvent))
            }

            override fun onBackInvoked() {
                dispatchOnBackCompleted()
            }

            override fun onBackCancelled() {
                dispatchOnBackCancelled()
            }
        }
    }
}
