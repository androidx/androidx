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

/**
 * Interface for determining if an update check operation should be throttled.
 *
 * This abstraction allows the [UpdateInfoService] to delegate the decision of "should we fetch
 * updates?" to a separate component. This enables different strategies (e.g., time-based,
 * quota-based, battery-aware) and facilitates easier testing by allowing the injection of fake
 * limiters.
 *
 * Implementations of this interface are responsible for persisting any state required to enforce
 * the limit (e.g., storing the timestamp of the last attempt in SharedPreferences).
 */
internal interface UpdateCheckRateLimiter {

    /**
     * Determines whether the current update check operation should be throttled.
     *
     * This method is called by [UpdateInfoService] before attempting a network fetch.
     *
     * @return `true` if the operation should be throttled (blocked). `false` if the operation is
     *   allowed to proceed.
     */
    fun shouldThrottle(): Boolean

    /**
     * Records that an update check attempt has occurred.
     *
     * This method should be called immediately before or after the protected action (e.g., the
     * network request) is performed. Implementations should use this signal to reset their internal
     * timers, decrement quotas, or update timestamps.
     */
    fun noteAttempt()
}
