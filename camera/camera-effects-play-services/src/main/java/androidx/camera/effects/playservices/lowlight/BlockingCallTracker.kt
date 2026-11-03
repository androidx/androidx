/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.effects.playservices.lowlight

import android.os.Build
import android.os.SystemClock
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import com.google.android.gms.cameralowlight.LowLightBoostSession

/**
 * Runs synchronous Google Play Services calls and records how long they blocked the GL thread since
 * the latest camera frame was handled. Camera frames queue up, or are dropped, meanwhile, so
 * LowLightBoostFrameRouter doesn't mistake frames delayed by such a call for a resumed camera
 * stream.
 *
 * **Threading:** used on the GL thread only.
 */
@WorkerThread
internal class BlockingCallTracker {
    /** How long synchronous calls blocked the GL thread since [onInputFrameHandled]. */
    internal var blockedMsSinceLastFrame = 0L
        private set

    /**
     * Runs [block], a synchronous Google Play Services call, and adds how long it blocked to
     * [blockedMsSinceLastFrame].
     */
    internal inline fun <T> call(block: () -> T): T {
        val startMs = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            addBlockedTime(SystemClock.elapsedRealtime() - startMs)
        }
    }

    /**
     * Adds [durationMs] to [blockedMsSinceLastFrame]. Only [call] should use it; it is internal
     * rather than private because the inline [call] can't use private members.
     */
    internal fun addBlockedTime(durationMs: Long) {
        blockedMsSinceLastFrame += durationMs
    }

    /** Called when a camera frame was handled. */
    internal fun onInputFrameHandled() {
        blockedMsSinceLastFrame = 0L
    }

    /**
     * Returns the camera [Surface] of [session], or null if it can't be retrieved, e.g. because the
     * session died in Google Play Services.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    internal fun cameraSurfaceOf(session: LowLightBoostSession): Surface? = call {
        try {
            session.getCameraSurface()
        } catch (e: Exception) {
            Logger.w(TAG, "Failed to get the LowLightBoostSession camera surface", e)
            null
        }
    }

    private companion object {
        private const val TAG = "LlbBlockingCalls"
    }
}
