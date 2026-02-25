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

import java.util.Objects

/**
 * Detailed telemetry data for a completed update check request.
 *
 * This class encapsulates performance metrics and attribution data for a single invocation of the
 * update check logic. It is provided to the host application via the
 * [UpdateInfoService.onRequestCompleted] hook to enable monitoring of latency, resource contention,
 * and client usage.
 *
 * This class is immutable.
 */
public class UpdateCheckTelemetry(
    /**
     * The outcome of the update check request.
     *
     * Value is one of the constants allowed by [UpdateFetchOutcome.Code], such as
     * [UpdateFetchOutcome.CACHE_HIT].
     */
    @get:UpdateFetchOutcome.Code public val outcome: Int,

    /**
     * The total wall-clock time (in milliseconds) elapsed from the start of the request until the
     * result was returned.
     *
     * This represents the user-perceived latency of the IPC call.
     */
    public val totalDurationMillis: Long,

    /**
     * The time (in milliseconds) spent waiting for the concurrency lock.
     *
     * A high value here indicates "thundering herd" contention, where multiple clients are
     * requesting updates simultaneously, causing threads to block while waiting for an in-progress
     * fetch to complete.
     */
    public val lockWaitDurationMillis: Long,

    /**
     * The time (in milliseconds) spent processing the request **after acquiring the concurrency
     * lock**.
     * * This metric excludes the time spent waiting for the lock ([lockWaitDurationMillis]).
     * * For [UpdateFetchOutcome.FETCHED], this includes the network request duration and disk
     *   persistence time.
     * * For [UpdateFetchOutcome.CACHE_HIT], this represents the time to read from local storage.
     * * For [UpdateFetchOutcome.COALESCED], this represents the minimal time spent reading the
     *   freshly updated cache after the lock was acquired.
     */
    public val processingDurationMillis: Long,

    /**
     * The time (in milliseconds) spent waiting for the network fetch implementation to complete.
     *
     * This is a subset of [processingDurationMillis] that strictly measures the duration of the
     * [UpdateInfoService.fetchUpdates] call, excluding local persistence and serialization
     * overhead.
     *
     * This value is 0 if no network fetch was attempted (e.g. for [UpdateFetchOutcome.CACHE_HIT] or
     * [UpdateFetchOutcome.THROTTLED], or [UpdateFetchOutcome.COALESCED]).
     */
    public val fetchDurationMillis: Long,

    /**
     * The Android User ID (UID) of the client application that initiated this request.
     *
     * This value is captured at the start of the transaction via [UpdateInfoService.getCallerUid].
     * * **Default:** Represents the physical IPC caller (`Binder.getCallingUid()`).
     * * **Proxied:** If the host application overrides `getCallerUid` (e.g., to support a service
     *   broker architecture), this represents the *logical* client identity provided by that
     *   implementation.
     *
     * **Usage:** Hosts should use this field to attribute resource usage, latency, and throttling
     * events to specific client applications (e.g., differentiating between the System Settings app
     * and a background telemetry agent).
     */
    public val callerUid: Int,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UpdateCheckTelemetry

        if (outcome != other.outcome) return false
        if (totalDurationMillis != other.totalDurationMillis) return false
        if (lockWaitDurationMillis != other.lockWaitDurationMillis) return false
        if (processingDurationMillis != other.processingDurationMillis) return false
        if (fetchDurationMillis != other.fetchDurationMillis) return false
        if (callerUid != other.callerUid) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            outcome,
            totalDurationMillis,
            lockWaitDurationMillis,
            processingDurationMillis,
            fetchDurationMillis,
            callerUid,
        )
    }

    override fun toString(): String {
        return "UpdateCheckTelemetry(" +
            "outcome=$outcome, " +
            "totalLatencyMillis=$totalDurationMillis, " +
            "lockWaitMillis=$lockWaitDurationMillis, " +
            "executionMillis=$processingDurationMillis, " +
            "fetchLatencyMillis=$fetchDurationMillis, " +
            "callerUid=$callerUid)"
    }
}
