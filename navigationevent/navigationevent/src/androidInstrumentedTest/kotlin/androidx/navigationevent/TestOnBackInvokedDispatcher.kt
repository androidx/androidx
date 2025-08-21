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

import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi

@RequiresApi(33)
internal class TestOnBackInvokedDispatcher : OnBackInvokedDispatcher {
    public var registerCount: Int = 0
        private set

    public var unregisterCount: Int = 0
        private set

    private var callback: OnBackInvokedCallback? = null

    @Suppress("ExecutorRegistration") // Single thread only.
    override fun registerOnBackInvokedCallback(
        priority: Int, // Ignored for now.
        callback: OnBackInvokedCallback,
    ) {
        registerCount++
        this.callback = callback
    }

    @Suppress("ExecutorRegistration") // Single thread only.
    override fun unregisterOnBackInvokedCallback(callback: OnBackInvokedCallback) {
        unregisterCount++
        if (this.callback !== callback) {
            error("Unregistering incorrect callback")
        }
        this.callback = null
    }

    @RequiresApi(34)
    public fun dispatchOnBackStarted(event: BackEvent) {
        (callback as? OnBackAnimationCallback)?.onBackStarted(event)
    }

    @RequiresApi(34)
    public fun dispatchOnBackProgressed(event: BackEvent) {
        (callback as? OnBackAnimationCallback)?.onBackProgressed(event)
    }

    @RequiresApi(34)
    public fun dispatchOnBackCancelled() {
        (callback as? OnBackAnimationCallback)?.onBackCancelled()
    }

    public fun dispatchOnBackInvoked() {
        callback?.onBackInvoked()
    }
}
