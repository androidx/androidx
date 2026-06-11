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

package androidx.work.analytics

import androidx.work.ListenableWorker.Result
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import java.util.UUID

/**
 * Metrics info and diagnostics about a particular [WorkRequest] period.
 *
 * This represents the period of time between the run being requested and the work actually
 * finishing. This may cover multiple run attempts (i.e. startWork calls) as the work may not finish
 * due to being stopped, retried, etc.
 *
 * A given [WorkRequest] may have multiple periods and thus multiple [WorkMetricsInfo] associated
 * with it.
 *
 * For example, updating a worker with WorkManager.updateWork is treated as starting a new
 * [WorkMetricsInfo] for the same [WorkRequest]. Similarly, a [PeriodicWorkRequest] finishing starts
 * a new [WorkMetricsInfo] for the new period.
 */
public class WorkMetricsInfo(
    /** The unique identifier for the [WorkRequest]. */
    public val workSpecId: UUID,

    /**
     * The specific generation of the [WorkRequest]. The generation is incremented every time the
     * [WorkRequest] is updated. See [WorkInfo.generation].
     */
    public val generation: Int,

    /** The worker class specified when the work request was enqueued. */
    public val workerClassName: String?,

    /** The set of tags associated with the work. See [WorkInfo.tags]. */
    public val tags: Set<String>,

    /** The current [WorkMetricsInfo.State] of the work. */
    public val state: State,

    /**
     * The timestamp (since epoch) when the [WorkRequest] was initially enqueued or updated. For a
     * [androidx.work.PeriodicWorkRequest], this is set every time a period finishes.
     */
    public val enqueueTimeMillis: Long,

    /**
     * The timestamp (since epoch) at which the work had all its prerequisite work finished, and it
     * starts waiting for constraints
     */
    public val unblockTimeMillis: Long,

    /** The timestamp (since epoch) at which this work was first started. */
    public val firstStartTimeMillis: Long,

    /**
     * The timestamp (since epoch) at which this work period finished, either by succeeding,
     * failing, cancelling, or being updated.
     */
    public val finishTimeMillis: Long,

    /**
     * The number of times this work has started in this period. This can occur multiple times for
     * various reasons e.g. worker returns [Result.retry], worker is stopped, app is closed.
     */
    public val runAttemptCount: Int = 0,

    /**
     * The number of times this worker returned [Result.retry] in this period. This is separate from
     * implicit retries such as stopping and starting or app kills.
     */
    public val explicitRetryCount: Int = 0,

    /** How many times this work stopped for each [WorkInfo.stopReason] in this period. */
    public val stopReasonCounts: Map<Int, Int> = emptyMap(),
) {
    public enum class State {
        /**
         * Used to indicate that the [androidx.work.WorkRequest] period is enqueued and still active
         * but blocked by pre-requisite work.
         */
        ENQUEUED_BLOCKED,

        /**
         * Used to indicate that the [androidx.work.WorkRequest] period is enqueued and still active
         * and will run when its constraints are met and resources are available.
         */
        ENQUEUED_PENDING,

        /** Used to indicate that this [androidx.work.WorkRequest] period is currently running. */
        RUNNING,

        /**
         * Used to indicate that this [androidx.work.WorkRequest] period completed in a successful
         * state.
         */
        SUCCEEDED,

        /**
         * Used to indicate that this [androidx.work.WorkRequest] period completed in a failed
         * state.
         */
        FAILED,

        /** Used to indicate that the [androidx.work.WorkRequest] period has been cancelled. */
        CANCELLED,

        /**
         * Used to indicate that the [androidx.work.WorkRequest] period is obsolete because the
         * [androidx.work.WorkRequest] was updated.
         */
        OBSOLETE_UPDATED,
    }
}
