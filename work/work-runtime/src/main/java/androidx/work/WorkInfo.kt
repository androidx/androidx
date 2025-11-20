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

import android.app.job.JobInfo
import android.app.job.JobScheduler
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.work.WorkInfo.Companion.STOP_REASON_NOT_STOPPED
import java.util.UUID

/**
 * Information about a particular [WorkRequest] containing the id of the WorkRequest, its current
 * [State], output, tags, and run attempt count. Note that output is only available for the terminal
 * states ([State.SUCCEEDED] and [State.FAILED]).
 */
public class WorkInfo
@JvmOverloads
constructor(
    /** The identifier of the [WorkRequest]. */
    public val id: UUID,
    /** The current [State] of the [WorkRequest]. */
    public val state: State,
    /** The [Set] of tags associated with the [WorkRequest]. */
    public val tags: Set<String>,
    /**
     * The output [Data] for the [WorkRequest]. If the WorkRequest is unfinished, this is always
     * [Data.EMPTY].
     */
    public val outputData: Data = Data.EMPTY,
    /** The progress [Data] associated with the [WorkRequest]. */
    public val progress: Data = Data.EMPTY,
    /**
     * The run attempt count of the [WorkRequest]. Note that for [PeriodicWorkRequest]s, the run
     * attempt count gets reset between successful runs.
     */
    @get:IntRange(from = 0) public val runAttemptCount: Int = 0,

    /**
     * The latest generation of this Worker.
     *
     * A work has multiple generations, if it was updated via [WorkManager.updateWork] or
     * [WorkManager.enqueueUniquePeriodicWork] using [ExistingPeriodicWorkPolicy.UPDATE].
     *
     * If this worker is currently running, it can possibly be of an older generation rather than
     * returned by this function if an update has happened during an execution of this worker.
     */
    public val generation: Int = 0,

    /** [Constraints] of this worker. */
    public val constraints: Constraints = Constraints.NONE,

    /** The initial delay for this work set in the [WorkRequest] */
    public val initialDelayMillis: Long = 0,

    /**
     * For periodic work, the period and flex duration set in the [PeriodicWorkRequest].
     *
     * Null if this is onetime work.
     */
    public val periodicityInfo: PeriodicityInfo? = null,

    /**
     * The earliest time this work is eligible to run next, if this work is [State.ENQUEUED].
     *
     * This is the earliest [System.currentTimeMillis] time that WorkManager would consider running
     * this work, regardless of any other system. It only represents the time that the initialDelay,
     * periodic configuration, and backoff criteria are considered to be met.
     *
     * Work will almost never run at this time in the real world. This method is intended for use in
     * scheduling tests or to check set schedules in app. Work run times are dependent on many
     * factors like the underlying system scheduler, doze and power saving modes of the OS, and
     * meeting any configured constraints. This is expected and is not considered a bug.
     *
     * The returned value may be in the past if the work was not able to run at that time. It will
     * be eligible to run any time after that time.
     *
     * Defaults to [Long.MAX_VALUE] for all other states, including if the work is currently
     * [State.RUNNING] or [State.BLOCKED] on prerequisite work.
     *
     * Even if this value is set, the work may not be registered with the system scheduler if there
     * are limited scheduling slots or other factors.
     */
    public val nextScheduleTimeMillis: Long = Long.MAX_VALUE,

    /**
     * The reason why this worker was stopped on the previous run attempt.
     *
     * For a worker being stopped, at first it should have attempted to run, i.e. its state should
     * be == RUNNING and then [ListenableWorker.onStopped] should have been called, resulting in
     * this worker's state going back [WorkInfo.State.ENQUEUED]. In this situation
     * (`runAttemptCount > 0` and `state == ENQUEUED`) this `stopReason` property could be checked
     * to see for additional information. Please note, that this state (`runAttemptCount > 0` and
     * `state == ENQUEUED`) can happen not only because a worker was stopped, but also when a worker
     * returns `ListenableWorker.Result.retry()`. In this situation this property will return
     * [STOP_REASON_NOT_STOPPED].
     */
    @StopReason @get:RequiresApi(31) public val stopReason: Int = STOP_REASON_NOT_STOPPED,
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
        if (stopReason != workInfo.stopReason) return false
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
        result = 31 * result + stopReason.hashCode()
        return result
    }

    override fun toString(): String {
        return ("WorkInfo{id='$id', state=$state, " +
            "outputData=$outputData, tags=$tags, progress=$progress, " +
            "runAttemptCount=$runAttemptCount, generation=$generation, " +
            "constraints=$constraints, initialDelayMillis=$initialDelayMillis, " +
            "periodicityInfo=$periodicityInfo, " +
            "nextScheduleTimeMillis=$nextScheduleTimeMillis}, " +
            "stopReason=$stopReason")
    }

    /** The current lifecycle state of a [WorkRequest]. */
    public enum class State {
        /**
         * Used to indicate that the [WorkRequest] is enqueued and eligible to run when its
         * [Constraints] are met and resources are available.
         */
        ENQUEUED,

        /** Used to indicate that the [WorkRequest] is currently being executed. */
        RUNNING,

        /**
         * Used to indicate that the [WorkRequest] has completed in a successful state. Note that
         * [PeriodicWorkRequest]s will never enter this state (they will simply go back to
         * [.ENQUEUED] and be eligible to run again).
         */
        SUCCEEDED,

        /**
         * Used to indicate that the [WorkRequest] has completed in a failure state. All dependent
         * work will also be marked as `#FAILED` and will never run.
         */
        FAILED,

        /**
         * Used to indicate that the [WorkRequest] is currently blocked because its prerequisites
         * haven't finished successfully.
         */
        BLOCKED,

        /**
         * Used to indicate that the [WorkRequest] has been cancelled and will not execute. All
         * dependent work will also be marked as `#CANCELLED` and will not run.
         */
        CANCELLED;

        /**
         * Returns `true` if this State is considered finished: [.SUCCEEDED], [.FAILED],
         * and * [.CANCELLED]
         */
        public val isFinished: Boolean
            get() = this == SUCCEEDED || this == FAILED || this == CANCELLED
    }

    /** A periodic work's interval and flex duration */
    public class PeriodicityInfo(
        /**
         * The periodic work's configured repeat interval in millis, as configured in
         * [PeriodicWorkRequest.Builder]
         */
        public val repeatIntervalMillis: Long,
        /**
         * The duration in millis in which this work repeats from the end of the `repeatInterval`,
         * as configured in [PeriodicWorkRequest.Builder].
         */
        public val flexIntervalMillis: Long,
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

    public companion object {

        /**
         * The foreground worker used up its maximum execution time and timed out.
         *
         * Foreground workers have a maximum execution time limit depending on the [ForegroundInfo]
         * type. See the notes on [android.content.pm.ServiceInfo] types.
         */
        public const val STOP_REASON_FOREGROUND_SERVICE_TIMEOUT: Int = -128

        /**
         * Additional stop reason, that is returned from [WorkInfo.stopReason] in cases when a
         * worker in question wasn't stopped. E.g. when a worker was just enqueued, but didn't run
         * yet.
         */
        public const val STOP_REASON_NOT_STOPPED: Int = -256

        /**
         * Stop reason that is used in cases when worker did stop, but the reason for this is
         * unknown. For example, when the app abruptly stopped due to a crash or when a device
         * suddenly ran out of the battery.
         */
        public const val STOP_REASON_UNKNOWN: Int = -512

        /**
         * The worker was cancelled directly by the app, either by calling cancel methods, e.g.
         * [WorkManager.cancelUniqueWork], or enqueueing uniquely named worker with a policy that
         * cancels an existing worker, e.g. [ExistingWorkPolicy.REPLACE].
         */
        public const val STOP_REASON_CANCELLED_BY_APP: Int = 1

        /** The job was stopped to run a higher priority job of the app. */
        public const val STOP_REASON_PREEMPT: Int = 2

        /**
         * The worker used up its maximum execution time and timed out. Each individual worker has a
         * maximum execution time limit, regardless of how much total quota the app has. See the
         * note on [JobScheduler] for the execution time limits.
         */
        public const val STOP_REASON_TIMEOUT: Int = 3

        /**
         * The device state (eg. Doze, battery saver, memory usage, etc) requires WorkManager to
         * stop this worker.
         */
        public const val STOP_REASON_DEVICE_STATE: Int = 4

        /**
         * The requested battery-not-low constraint is no longer satisfied.
         *
         * @see JobInfo.Builder.setRequiresBatteryNotLow
         */
        public const val STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW: Int = 5

        /**
         * The requested charging constraint is no longer satisfied.
         *
         * @see JobInfo.Builder.setRequiresCharging
         */
        public const val STOP_REASON_CONSTRAINT_CHARGING: Int = 6

        /** The requested connectivity constraint is no longer satisfied. */
        public const val STOP_REASON_CONSTRAINT_CONNECTIVITY: Int = 7

        /** The requested idle constraint is no longer satisfied. */
        public const val STOP_REASON_CONSTRAINT_DEVICE_IDLE: Int = 8

        /** The requested storage-not-low constraint is no longer satisfied. */
        public const val STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW: Int = 9

        /**
         * The app has consumed all of its current quota. Each app is assigned a quota of how much
         * it can run workers within a certain time frame. The quota is informed, in part, by app
         * standby buckets.
         *
         * @see android.app.job.JobParameters.STOP_REASON_QUOTA
         */
        public const val STOP_REASON_QUOTA: Int = 10

        /**
         * The app is restricted from running in the background.
         *
         * @see android.app.job.JobParameters.STOP_REASON_BACKGROUND_RESTRICTION
         */
        public const val STOP_REASON_BACKGROUND_RESTRICTION: Int = 11

        /**
         * The current standby bucket requires that the job stop now.
         *
         * @see android.app.job.JobParameters.STOP_REASON_APP_STANDBY
         */
        public const val STOP_REASON_APP_STANDBY: Int = 12

        /**
         * The user stopped the job. This can happen either through force-stop, adb shell commands,
         * uninstalling, or some other UI.
         *
         * @see android.app.job.JobParameters.STOP_REASON_USER
         */
        public const val STOP_REASON_USER: Int = 13

        /**
         * The system is doing some processing that requires stopping this job.
         *
         * @see android.app.job.JobParameters.STOP_REASON_SYSTEM_PROCESSING
         */
        public const val STOP_REASON_SYSTEM_PROCESSING: Int = 14

        /**
         * The system's estimate of when the app will be launched changed significantly enough to
         * decide this worker shouldn't be running right now.
         */
        public const val STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED: Int = 15
    }
}

/**
 * Stops reason integers are divided in ranges since some corresponds to platform equivalents, while
 * other are WorkManager specific.
 * * `-512` - Special STOP_REASON_UNKNOWN
 * * `-256` - Special STOP_REASON_NOT_STOPPED
 * * `[-255, -128]` - Reserved for WM specific reasons (i.e. not reflected by JobScheduler).
 * * `[-127, -1]` - Unused on purpose.
 * * `[0, MAX_VALUE]` - Reserved for JobScheduler mirror reasons (i.e. JobParameters.STOP_REASON_X).
 */
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    WorkInfo.STOP_REASON_UNKNOWN,
    WorkInfo.STOP_REASON_NOT_STOPPED,
    WorkInfo.STOP_REASON_FOREGROUND_SERVICE_TIMEOUT,
    WorkInfo.STOP_REASON_CANCELLED_BY_APP,
    WorkInfo.STOP_REASON_PREEMPT,
    WorkInfo.STOP_REASON_TIMEOUT,
    WorkInfo.STOP_REASON_DEVICE_STATE,
    WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW,
    WorkInfo.STOP_REASON_CONSTRAINT_CHARGING,
    WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY,
    WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE,
    WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW,
    WorkInfo.STOP_REASON_QUOTA,
    WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION,
    WorkInfo.STOP_REASON_APP_STANDBY,
    WorkInfo.STOP_REASON_USER,
    WorkInfo.STOP_REASON_SYSTEM_PROCESSING,
    WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED,
)
internal annotation class StopReason
