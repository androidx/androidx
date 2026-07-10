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

package androidx.security.state.provider

import android.annotation.SuppressLint
import androidx.annotation.IntDef

/**
 * Represents the detailed outcome of an update check request.
 *
 * This class defines constants describing *how* a request to `listAvailableUpdates` was satisfied.
 * It is used primarily for telemetry and monitoring, allowing host applications to distinguish
 * between requests served from the local cache, requests that triggered a network fetch, and
 * requests that were throttled or failed.
 *
 * Host applications receive this outcome in the [UpdateCheckTelemetry] object passed to
 * [UpdateInfoService.onRequestCompleted].
 */
public object UpdateFetchOutcome {
    /**
     * The request was satisfied immediately from the local cache.
     *
     * This outcome occurs when the cached data is considered fresh (e.g., it was last updated
     * within the freshness threshold) and no network locks needed to be acquired. This represents
     * the "Fast Path" for the service.
     */
    public const val CACHE_HIT: Int = 1

    /**
     * The request was satisfied from the local cache after waiting for another thread to complete a
     * network fetch.
     *
     * This outcome occurs when multiple clients request updates simultaneously (a "thundering
     * herd"). The current thread waited for the `fetchUpdates` lock, and by the time it acquired
     * the lock, another thread had already refreshed the cache. This is considered a "Slow Path"
     * outcome due to the lock wait time, even though no network call was made by this specific
     * thread.
     */
    public const val COALESCED: Int = 2

    /**
     * A network fetch was performed and fresh data was returned.
     *
     * This outcome occurs when the cache was stale, the rate limiter allowed the request, and the
     * [UpdateInfoService.fetchUpdates] implementation successfully retrieved new data from the
     * backend. This represents the "Slow Path" for the service.
     */
    public const val FETCHED: Int = 3

    /**
     * The request was blocked by the rate limiter.
     *
     * This outcome occurs when the cache was stale, but the rate limiter determined that a network
     * fetch should not be attempted (e.g., because a check was performed too recently). The service
     * returned cached data to the client to ensure graceful degradation.
     */
    public const val THROTTLED: Int = 4

    /**
     * The operation failed with an exception.
     *
     * This outcome occurs if an unhandled exception was thrown during any part of the update check
     * lifecycle (e.g., network failure, disk I/O error, or rate limiter error). The service
     * returned cached data to the client to prevent the crash from propagating.
     */
    public const val FAILED: Int = 5

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(CACHE_HIT, FETCHED, THROTTLED, COALESCED, FAILED)
    @SuppressLint(
        "PublicTypedef"
    ) // Exposed to allow host applications to validate the outcome field in [UpdateCheckTelemetry].
    public annotation class Code
}
