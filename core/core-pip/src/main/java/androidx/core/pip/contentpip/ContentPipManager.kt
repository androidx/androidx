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

package androidx.core.pip.contentpip

import android.app.ActivityOptions
import android.content.Intent
import androidx.activity.ComponentActivity
import java.util.concurrent.atomic.AtomicBoolean

internal object ContentPipManager {
    private val isTransitioning = AtomicBoolean(false)
    private var activeCallback: ContentPipCallback? = null
    private var finishProxyHook: (() -> Unit)? = null

    /** Attempts to trigger the fallback PiP pipeline. */
    internal fun triggerFallback(activity: ComponentActivity, callback: ContentPipCallback) {
        // Mutual exclusivity: Don't trigger if the caller Activity is in PiP
        // or the proxy ContentPipInternalActivity is already transitioning.
        if (activity.isInPictureInPictureMode || !isTransitioning.compareAndSet(false, true)) {
            return
        }

        cleanupInternalStates()

        if (!callback.onPrepareContentPip()) {
            isTransitioning.set(false)
            return
        }

        activeCallback = callback

        val intent =
            Intent(activity, ContentPipInternalActivity::class.java).apply {
                // MULTIPLE_TASK ensures we get a fresh task even if a previous one exists
                // NO_USER_ACTION prevents interrupting system gestures
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                )
            }
        val options = ActivityOptions.makeCustomAnimation(activity, 0, 0)
        activity.startActivity(intent, options.toBundle())
    }

    /** Consumes the pending callback. Called by [ContentPipInternalActivity]. */
    internal fun consumeHandoff(): ContentPipCallback? {
        val callback = activeCallback
        isTransitioning.set(false)
        return callback
    }

    /** Registers a hook to finish the proxy activity. */
    internal fun setFinishProxyHook(hook: () -> Unit) {
        finishProxyHook = hook
    }

    /** Signals the proxy task to finish and remove itself. */
    internal fun requestFinish() {
        finishProxyHook?.invoke()
        finishProxyHook = null
    }

    /** Clears the current session. */
    internal fun onTeardown(isDismissed: Boolean) {
        activeCallback?.onFinishContentPip(isDismissed)
        cleanupInternalStates()
    }

    private fun cleanupInternalStates() {
        activeCallback = null
        finishProxyHook = null
        isTransitioning.set(false)
    }
}
