/*
 * Copyright (C) 2026 The Android Open Source Project
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

@file:JvmName("ContentPip")

package androidx.core.pip.contentpip

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Enables entering PiP on app switch for this Activity with Content PiP.
 *
 * This API hooks into standard Activity exit hints to trigger a fallback PiP pipeline when native
 * [Auto-Enter](https://developer.android.com/reference/android/app/PictureInPictureParams#isAutoEnterEnabled())
 * is suppressed by the OS (e.g. on Quick Switch).
 *
 * It also automatically monitors the Activity lifecycle and user interactions to "pull back" the
 * player and terminate the fallback task when the user returns to the app.
 *
 * **Threading:** This callback is strictly tied to the UI toolkit and will **always** be executed
 * synchronously on the main thread of your application.
 *
 * @param callback The callback to manage handoff between tasks.
 */
@SuppressLint("ExecutorRegistration")
public fun ComponentActivity.enablePipOnAppSwitch(callback: ContentPipCallback) {
    addOnUserLeaveHintListener {
        // Triggered synchronously to avoid interrupting system transitions
        if (callback.onInitContentPip()) {
            ContentPipManager.triggerFallback(this, callback)
        }
    }

    // Automatically handle "Pull Back" when returning to the app via lifecycle
    val handler = Handler(Looper.getMainLooper())
    lifecycle.addObserver(
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Delay slightly to ensure smooth transition back from PiP/Dialog
                    handler.postDelayed({ ContentPipManager.requestFinish() }, 200L)
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // If the activity is finishing (not just rotating), cleanup the proxy task
                    if (isFinishing) {
                        ContentPipManager.requestFinish()
                    }
                }
                else -> {}
            }
        }
    )

    // Automatically handle "Pull Back" on user interaction
    val originalCallback = window.callback
    window.callback =
        ContentPipWindowCallback(originalCallback) { ContentPipManager.requestFinish() }
}

/** An internal [Window.Callback] wrapper that intercepts interactions to signal PiP teardown. */
private class ContentPipWindowCallback(
    private val delegate: Window.Callback,
    private val onInteraction: () -> Unit,
) : Window.Callback by delegate {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        onInteraction()
        return delegate.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        onInteraction()
        return delegate.dispatchKeyEvent(event)
    }
}
