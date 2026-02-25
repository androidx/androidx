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

package androidx.security.state.provider

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

/**
 * A default [UpdateCheckRateLimiter] implementation backed by [SharedPreferences].
 *
 * This limiter enforces a minimum time interval between allowed update check attempts. Crucially,
 * the state (the timestamp of the last attempt) is persisted to disk using [SharedPreferences].
 * This ensures that the rate limit is enforced even if the hosting application process is killed
 * and restarted, or if the device is rebooted.
 *
 * This class is `open` to allow host applications to extend it if they need to override specific
 * behaviors while keeping the storage mechanism.
 *
 * @param context The context used to access [SharedPreferences].
 * @param throttleIntervalMillis The minimum duration (in milliseconds) that must elapse between
 *   allowed update checks. Defaults to 1 hour.
 * @param prefsFileName The name of the [SharedPreferences] file to use for storing the state.
 *   Defaults to "UpdateCheckRateLimiter".
 */
internal open class SharedPreferencesUpdateCheckRateLimiter
@JvmOverloads
constructor(
    context: Context,
    private val throttleIntervalMillis: Long = TimeUnit.HOURS.toMillis(1),
    private val prefsFileName: String = "UpdateCheckRateLimiter",
) : UpdateCheckRateLimiter {

    // Use MODE_PRIVATE to sandbox the data to this application, preventing
    // other apps from tampering with the rate limit state.
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)

    private companion object {
        private const val KEY_LAST_ATTEMPT_TIME = "last_attempt_time_millis"
    }

    /**
     * Checks if the time since the last recorded attempt is less than the configured
     * [throttleIntervalMillis].
     *
     * This method retrieves the last attempt timestamp from storage and compares it against the
     * current system time.
     *
     * @return `true` if the operation should be blocked (throttled). `false` if the interval has
     *   passed and the operation is allowed.
     */
    override fun shouldThrottle(): Boolean {
        val lastAttemptTime = sharedPreferences.getLong(KEY_LAST_ATTEMPT_TIME, 0L)
        val timeSinceLastAttempt = System.currentTimeMillis() - lastAttemptTime

        // If timeSinceLastAttempt is negative, it implies the system clock was moved backwards
        // (or the file was corrupted with a future timestamp). In this case, we default to
        // throttling the request to prevent potential abuse.
        if (timeSinceLastAttempt < 0) {
            return true
        }

        // Throttle if the time passed is strictly less than the required interval.
        return timeSinceLastAttempt < throttleIntervalMillis
    }

    /**
     * Records the current system time as the timestamp of the last update check attempt.
     *
     * This method commits the timestamp to [SharedPreferences] immediately. It is intended to be
     * called right before the network request is initiated.
     */
    override fun noteAttempt() {
        sharedPreferences.edit().putLong(KEY_LAST_ATTEMPT_TIME, System.currentTimeMillis()).apply()
    }
}
