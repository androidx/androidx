/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.work

import androidx.annotation.IntRange
import androidx.work.WorkInfo.State
import java.util.UUID

/**
 * Information about a particular [WorkRequest] containing the id of the WorkRequest, its
 * current [State], output, tags, and run attempt count.  Note that output is only available
 * for the terminal states ([State.SUCCEEDED] and [State.FAILED]).
 */
class WorkInfo @JvmOverloads constructor(
    /**
     * The identifier of the [WorkRequest].
     */
    val id: UUID,
    /**
     * The current [State] of the [WorkRequest].
     */
    val state: State,
    /**
     * The [Set] of tags associated with the [WorkRequest].
     */
    val tags: Set<String>,
    /**
     * The output [Data] for the [WorkRequest]. If the WorkRequest is unfinished,
     * this is always [Data.EMPTY].
     */
    val outputData: Data = Data.EMPTY,
    /**
     * The progress [Data] associated with the [WorkRequest].
     */
    val progress: Data = Data.EMPTY,
    /**
     * The run attempt count of the [WorkRequest].  Note that for
     * [PeriodicWorkRequest]s, the run attempt count gets reset between successful runs.
     */
    @get:IntRange(from = 0)
    val runAttemptCount: Int = 0,

    /**
     * The latest generation of this Worker.
     *
     *
     * A work has multiple generations, if it was updated via
     * [WorkManager.updateWork] or
     * [WorkManager.enqueueUniquePeriodicWork] using
     * [ExistingPeriodicWorkPolicy.UPDATE].
     *
     *
     * If this worker is currently running, it can possibly be of an older generation rather than
     * returned by this function if an update has happened during an execution of this worker.
     */
    val generation: Int = 0,

    /**
     * [Constraints] of this worker.
     */
    val constraints: Constraints = Constraints.NONE,

    /** The initial delay for this work set in the [WorkRequest] */
    val initialDelayMillis: Long = 0,

    /**
     * For periodic work, the period and flex duration set in the [PeriodicWorkRequest].
     *
     * Null if this is onetime work.
     */
    val periodicityInfo: PeriodicityInfo? = null,

    /**
     * The earliest time this work is eligible to run next, if this work is [State.ENQUEUED].
     *
     * This is the earliest [System.currentTimeMillis] time that WorkManager would consider running
     * this work, regardless of any other system. It only represents the time that the
     * initialDelay, periodic configuration, and backoff criteria are considered to be met.
     *
     * Work will almost never run at this time in the real world. This method is intended for use
     * in scheduling tests or to check set schedules in app. Work run times are dependent on
     * many factors like the underlying system scheduler, doze and power saving modes of the OS, and
     * meeting any configured constraints. This is expected and is not considered a bug.
     *
     * The returned value may be in the past if the work was not able to run at that time. It will
     * be eligible to run any time after that time.
     *
     * Defaults to [Long.MAX_VALUE] for all other states, including if the work is currently
     * [State.RUNNING] or [State.BLOCKED] on prerequisite work.
     *
     * Even if this value is set, the work may not be registered with the system scheduler if
     * there are limited scheduling slots or other factors.
     */
    val nextScheduleTimeMillis: Long = Long.MAX_VALUE,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val workInfo = other as WorkInfo
        if (runAttemptCount != workInfo.runAttemptCount) return false
        if (generation != workInfo.generation) return false
        if (id != workInfo.id) return false
        if (state != workInfo.state) return false
        if (outputData != workInfo.outputData) return false
        if (constraints != workInfo.constraints) return false
        if (initialDelayMillis != workInfo.initialDelayMillis) return false
        if (periodicityInfo != workInfo.periodicityInfo) return false
        if (nextScheduleTimeMillis != workInfo.nextScheduleTimeMillis) return false
        return if (tags != workInfo.tags) false else progress == workInfo.progress
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + outputData.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + runAttemptCount
        result = 31 * result + generation
        result = 31 * result + constraints.hashCode()
        result = 31 * result + initialDelayMillis.hashCode()
        result = 31 * result + periodicityInfo.hashCode()
        result = 31 * result + nextScheduleTimeMillis.hashCode()
        return result
    }

    override fun toString(): String {
        return ("WorkInfo{id='$id', state=$state, " +
            "outputData=$outputData, tags=$tags, progress=$progress, " +
            "runAttemptCount=$runAttemptCount, generation=$generation, " +
            "constraints=$constraints}, initialDelayMillis=$initialDelayMillis, " +
            "periodicityInfo=$periodicityInfo, " +
            "nextScheduleTimeMillis=$nextScheduleTimeMillis")
    }

    /**
     * The current lifecycle state of a [WorkRequest].
     */
    enum class State {
        /**
         * Used to indicate that the [WorkRequest] is enqueued and eligible to run when its
         * [Constraints] are met and resources are available.
         */
        ENQUEUED,

        /**
         * Used to indicate that the [WorkRequest] is currently being executed.
         */
        RUNNING,

        /**
         * Used to indicate that the [WorkRequest] has completed in a successful state.  Note
         * that [PeriodicWorkRequest]s will never enter this state (they will simply go back
         * to [.ENQUEUED] and be eligible to run again).
         */
        SUCCEEDED,

        /**
         * Used to indicate that the [WorkRequest] has completed in a failure state.  All
         * dependent work will also be marked as `#FAILED` and will never run.
         */
        FAILED,

        /**
         * Used to indicate that the [WorkRequest] is currently blocked because its
         * prerequisites haven't finished successfully.
         */
        BLOCKED,

        /**
         * Used to indicate that the [WorkRequest] has been cancelled and will not execute.
         * All dependent work will also be marked as `#CANCELLED` and will not run.
         */
        CANCELLED;

        /**
         * Returns `true` if this State is considered finished:
         * [.SUCCEEDED], [.FAILED], and * [.CANCELLED]
         */
        val isFinished: Boolean
            get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    }

    /** A periodic work's interval and flex duration */
    class PeriodicityInfo(
        /**
         * The periodic work's configured repeat interval in millis, as configured in
         * [PeriodicWorkRequest.Builder]
         */
        val repeatIntervalMillis: Long,
        /**
         * The duration in millis in which this work repeats from the end of the `repeatInterval`,
         * as configured in [PeriodicWorkRequest.Builder].
         */
        val flexIntervalMillis: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val period = other as PeriodicityInfo
            return period.repeatIntervalMillis == repeatIntervalMillis &&
                period.flexIntervalMillis == flexIntervalMillis
        }

        override fun hashCode(): Int {
            return 31 * repeatIntervalMillis.hashCode() + flexIntervalMillis.hashCode()
        }

        override fun toString(): String {
            return "PeriodicityInfo{repeatIntervalMillis=$repeatIntervalMillis, " +
                "flexIntervalMillis=$flexIntervalMillis}"
        }
    }
}
