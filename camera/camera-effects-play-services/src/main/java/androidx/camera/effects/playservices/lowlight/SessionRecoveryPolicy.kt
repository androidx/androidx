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

import androidx.annotation.WorkerThread

/**
 * Decides when a Low Light Boost session may be recovered in the background.
 *
 * Recovery is mainly for a session that died, e.g. when Google Play Services restarts. It is
 * throttled ([AUTO_RECOVERY_MIN_INTERVAL_MS]) and capped per camera stream
 * ([MAX_AUTO_RECOVERY_ATTEMPTS_PER_STREAM]), so a session that keeps dying right after it is
 * created doesn't cause an endless recovery loop. The policy only keeps this throttling state;
 * callers pass the current time and start the recovery.
 *
 * **Threading:** used on the GL thread only.
 */
@WorkerThread
internal class SessionRecoveryPolicy {
    private var lastAttemptMs = 0L

    private var attemptsForCurrentStream = 0

    /**
     * Allows an immediate recovery attempt and resets the recovery cap, e.g. when the app enables
     * Low Light Boost, a new camera stream starts, or the camera stream resumes after a pause.
     */
    internal fun reset() {
        lastAttemptMs = 0L
        attemptsForCurrentStream = 0
    }

    /**
     * Called when the active session, started at [sessionStartTimeMs], died at [nowMs]. Only a
     * session that stayed alive for a while resets the recovery cap, so a session that keeps dying
     * right after creation can't trigger an endless recovery loop.
     */
    internal fun onSessionDied(sessionStartTimeMs: Long, nowMs: Long) {
        if (nowMs - sessionStartTimeMs >= STABLE_SESSION_DURATION_MS) {
            attemptsForCurrentStream = 0
        }
    }

    /**
     * Returns true if at least [AUTO_RECOVERY_MIN_INTERVAL_MS] has elapsed since the last attempt
     * and the recovery cap ([MAX_AUTO_RECOVERY_ATTEMPTS_PER_STREAM]) has not been reached.
     */
    internal fun canAttemptRecovery(nowMs: Long): Boolean {
        if (lastAttemptMs != 0L && nowMs - lastAttemptMs < AUTO_RECOVERY_MIN_INTERVAL_MS) {
            return false
        }
        return attemptsForCurrentStream < MAX_AUTO_RECOVERY_ATTEMPTS_PER_STREAM
    }

    /** Records a recovery attempt started at [nowMs]. */
    internal fun onRecoveryAttempted(nowMs: Long) {
        lastAttemptMs = nowMs
        attemptsForCurrentStream++
    }

    private companion object {
        // The timing values below are tuned for Google Play Services Low Light Boost behavior
        // (b/448524973) and can be revisited when the service changes.

        /** The minimum interval between background recovery attempts. */
        private const val AUTO_RECOVERY_MIN_INTERVAL_MS = 2000L

        /**
         * The maximum number of background recovery attempts for a camera stream, so a session that
         * keeps dying right after it is created doesn't cause an endless recovery loop. A new
         * SurfaceRequest for the same camera stream that arrives before the current one completes,
         * e.g. after a target rotation change, keeps the count. The count is reset when a session
         * that stayed alive for [STABLE_SESSION_DURATION_MS] dies, when the app enables Low Light
         * Boost, and when the camera stream resumes after a pause.
         */
        private const val MAX_AUTO_RECOVERY_ATTEMPTS_PER_STREAM = 3

        /**
         * How long a session must stay alive before its death resets the recovery cap
         * ([MAX_AUTO_RECOVERY_ATTEMPTS_PER_STREAM]).
         */
        private const val STABLE_SESSION_DURATION_MS = 10_000L
    }
}
