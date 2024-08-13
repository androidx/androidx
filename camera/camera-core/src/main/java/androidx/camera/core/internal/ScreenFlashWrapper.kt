/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.core.internal

import androidx.annotation.GuardedBy
import androidx.camera.core.ImageCapture.ScreenFlash
import androidx.camera.core.ImageCapture.ScreenFlashListener
import androidx.camera.core.Logger

/**
 * Wrapper class around [ScreenFlash] to save the [ScreenFlashListener] passed to app.
 *
 * This allows us to clean up properly in case a capture is cancelled earlier (e.g. ImageCapture is
 * unbound after [apply] is invoked but [clear] is not).
 */
internal class ScreenFlashWrapper private constructor(private val screenFlash: ScreenFlash?) :
    ScreenFlash {
    private val lock = Object()

    @GuardedBy("lock") private var isClearScreenFlashPending: Boolean = false
    @GuardedBy("lock") private var pendingListener: ScreenFlashListener? = null

    companion object {
        private const val TAG = "ScreenFlashWrapper"

        @JvmStatic fun from(screenFlash: ScreenFlash?) = ScreenFlashWrapper(screenFlash)
    }

    override fun apply(expirationTimeMillis: Long, screenFlashListener: ScreenFlashListener) {
        synchronized(lock) {
            isClearScreenFlashPending = true
            pendingListener = screenFlashListener
        }

        screenFlash?.apply(expirationTimeMillis) {
            synchronized(lock) {
                if (pendingListener == null) {
                    Logger.w(TAG, "apply: pendingListener is null!")
                }
                completePendingScreenFlashListener()
            }
        }
            ?: run {
                Logger.e(TAG, "apply: screenFlash is null!")
                // Complete immediately in case this error case is invoked by some bug
                completePendingScreenFlashListener()
            }
    }

    override fun clear() {
        completePendingScreenFlashClear()
    }

    /** Gets the base [ScreenFlash] where the interface methods are delegated to. */
    fun getBaseScreenFlash(): ScreenFlash? = screenFlash

    /** Completes the pending [ScreenFlashListener], if any. */
    private fun completePendingScreenFlashListener() {
        synchronized(lock) {
            pendingListener?.onCompleted()
            pendingListener = null
        }
    }

    /** Completes pending [ScreenFlash.clear] invocation, if any. */
    private fun completePendingScreenFlashClear() {
        synchronized(lock) {
            if (isClearScreenFlashPending) {
                screenFlash?.clear()
                    ?: run {
                        Logger.e(TAG, "completePendingScreenFlashClear: screenFlash is null!")
                    }
            } else {
                Logger.w(TAG, "completePendingScreenFlashClear: none pending!")
            }
            isClearScreenFlashPending = false
        }
    }

    /** Completes all pending operations. */
    fun completePendingTasks() {
        completePendingScreenFlashListener()
        completePendingScreenFlashClear()
    }
}
