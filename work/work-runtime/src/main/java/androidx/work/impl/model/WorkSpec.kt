/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.work.impl.model

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.arch.core.util.Function
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.Logger
import androidx.work.OutOfQuotaPolicy
import androidx.work.OverwritingInputMerger
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_FLEX_MILLIS
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import java.util.UUID

// TODO: make a immutable
/** Stores information about a logical unit of work. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Entity(indices = [Index(value = ["schedule_requested_at"]), Index(value = ["last_enqueue_time"])])
public data class WorkSpec(
    @JvmField @ColumnInfo(name = "id") @PrimaryKey val id: String,
    @JvmField @ColumnInfo(name = "state") var state: WorkInfo.State = WorkInfo.State.ENQUEUED,
    @JvmField @ColumnInfo(name = "worker_class_name") var workerClassName: String,
    @JvmField
    @ColumnInfo(name = "input_merger_class_name")
    var inputMergerClassName: String = OverwritingInputMerger::class.java.name,
    @JvmField @ColumnInfo(name = "input") var input: Data = Data.EMPTY,
    @JvmField @ColumnInfo(name = "output") var output: Data = Data.EMPTY,
    @JvmField @ColumnInfo(name = "initial_delay") var initialDelay: Long = 0,
    @JvmField @ColumnInfo(name = "interval_duration") var intervalDuration: Long = 0,
    @JvmField @ColumnInfo(name = "flex_duration") var flexDuration: Long = 0,
    @JvmField @Embedded var constraints: Constraints = Constraints.NONE,
    @JvmField
    @ColumnInfo(name = "run_attempt_count")
    @IntRange(from = 0)
    var runAttemptCount: Int = 0,
    @JvmField
    @ColumnInfo(name = "backoff_policy")
    var backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
    @JvmField
    @ColumnInfo(name = "backoff_delay_duration")
    var backoffDelayDuration: Long = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,

    /** Time in millis when work was marked as ENQUEUED in database. */
    @JvmField
    @ColumnInfo(name = "last_enqueue_time", defaultValue = "$NOT_ENQUEUED")
    var lastEnqueueTime: Long = NOT_ENQUEUED,
    @JvmField
    @ColumnInfo(name = "minimum_retention_duration")
    var minimumRetentionDuration: Long = 0,

    /**
     * This field tells us if this [WorkSpec] instance, is actually currently scheduled and being
     * counted against the `SCHEDULER_LIMIT`. This bit is reset for PeriodicWorkRequests in API <
     * 23, because AlarmManager does not know of PeriodicWorkRequests. So for the next request to be
     * rescheduled this field has to be reset to `SCHEDULE_NOT_REQUESTED_AT`. For the JobScheduler
     * implementation, we don't reset this field because JobScheduler natively supports
     * PeriodicWorkRequests.
     */
    @JvmField
    @ColumnInfo(name = "schedule_requested_at")
    var scheduleRequestedAt: Long = SCHEDULE_NOT_REQUESTED_YET,

    /**
     * This is `true` when the WorkSpec needs to be hosted by a foreground service or a high
     * priority job.
     */
    @JvmField @ColumnInfo(name = "run_in_foreground") var expedited: Boolean = false,

    /**
     * When set to `true` this [WorkSpec] falls back to a regular job when an application runs out
     * of expedited job quota.
     */
    @JvmField
    @ColumnInfo(name = "out_of_quota_policy")
    var outOfQuotaPolicy: OutOfQuotaPolicy = OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST,

    /**
     * A number of periods that this worker has already run. This has no real implication for
     * OneTimeWork.
     */
    @ColumnInfo(name = "period_count", defaultValue = "0") var periodCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val generation: Int = 0,

    /**
     * If not Long.MAX_VALUE, this will be the next schedule time, regardless of configured delay.
     * Only valid for periodic workers
     */
    @ColumnInfo(name = "next_schedule_time_override", defaultValue = Long.MAX_VALUE.toString())
    var nextScheduleTimeOverride: Long = Long.MAX_VALUE,

    /**
     * Generation counter that tracks only the nextScheduleTimeOverride version, which allows the
     * overall generation to be incremented without clearing the nextScheduleTimeOverride. Eg. while
     * an override is set, a WorkSpec's constraints are changed using UPDATE, but the override time
     * is neither set nor cleared.
     *
     * We could implicitly cancel the nextScheduleTimeOverride since it was not specified in the
     * update. However, this would require every caller to know that there is an override, and what
     * the value of that time was, in order to make unrelated changes.
     *
     * Instead, we keep track of a separate override schedule generation, so only updates that
     * change or cancel the nextScheduleTimeOverride will affect the override generation.
     *
     * This allows WorkSpec changes to be made mid-worker run, and WorkerWrapper can still correctly
     * clear a previous nextScheduleTimeOverride upon conclusion by consulting the
     * overrideGeneration instead of the overall generation.
     */
    @ColumnInfo(name = "next_schedule_time_override_generation", defaultValue = "0")
    // If reset every min interval, would last 500 years.
    var nextScheduleTimeOverrideGeneration: Int = 0,
    @ColumnInfo(name = "stop_reason", defaultValue = "${WorkInfo.STOP_REASON_NOT_STOPPED}")
    val stopReason: Int = WorkInfo.STOP_REASON_NOT_STOPPED,
    @ColumnInfo(name = "trace_tag") var traceTag: String? = null,
    @ColumnInfo(name = "backoff_on_system_interruptions")
    var backOffOnSystemInterruptions: Boolean? = false,
) {
    public constructor(
        id: String,
        workerClassName_: String,
    ) : this(id = id, workerClassName = workerClassName_)

    public constructor(
        newId: String,
        other: WorkSpec,
    ) : this(
        id = newId,
        workerClassName = other.workerClassName,
        state = other.state,
        inputMergerClassName = other.inputMergerClassName,
        input = Data(other.input),
        output = Data(other.output),
        initialDelay = other.initialDelay,
        intervalDuration = other.intervalDuration,
        flexDuration = other.flexDuration,
        constraints = Constraints(other.constraints),
        runAttemptCount = other.runAttemptCount,
        backoffPolicy = other.backoffPolicy,
        backoffDelayDuration = other.backoffDelayDuration,
        lastEnqueueTime = other.lastEnqueueTime,
        minimumRetentionDuration = other.minimumRetentionDuration,
        scheduleRequestedAt = other.scheduleRequestedAt,
        expedited = other.expedited,
        outOfQuotaPolicy = other.outOfQuotaPolicy,
        periodCount = other.periodCount,
        nextScheduleTimeOverride = other.nextScheduleTimeOverride,
        nextScheduleTimeOverrideGeneration = other.nextScheduleTimeOverrideGeneration,
        stopReason = other.stopReason,
        traceTag = other.traceTag,
        backOffOnSystemInterruptions = other.backOffOnSystemInterruptions,
    )

    /** @param backoffDelayDuration The backoff delay duration in milliseconds */
    public fun setBackoffDelayDuration(backoffDelayDuration: Long) {
        if (backoffDelayDuration > WorkRequest.MAX_BACKOFF_MILLIS) {
            Logger.get().warning(TAG, "Backoff delay duration exceeds maximum value")
        }
        if (backoffDelayDuration < WorkRequest.MIN_BACKOFF_MILLIS) {
            Logger.get().warning(TAG, "Backoff delay duration less than minimum value")
        }

        this.backoffDelayDuration =
            backoffDelayDuration.coerceIn(
                WorkRequest.MIN_BACKOFF_MILLIS,
                WorkRequest.MAX_BACKOFF_MILLIS,
            )
    }

    val isPeriodic: Boolean
        get() = intervalDuration != 0L

    val isBackedOff: Boolean
        get() = state == WorkInfo.State.ENQUEUED && runAttemptCount > 0

    /**
     * Sets the periodic interval for this unit of work.
     *
     * @param intervalDuration The interval in milliseconds
     */
    public fun setPeriodic(intervalDuration: Long) {
        if (intervalDuration < MIN_PERIODIC_INTERVAL_MILLIS) {
            Logger.get()
                .warning(
                    TAG,
                    "Interval duration lesser than minimum allowed value; " +
                        "Changed to $MIN_PERIODIC_INTERVAL_MILLIS",
                )
        }
        setPeriodic(
            intervalDuration.coerceAtLeast(MIN_PERIODIC_INTERVAL_MILLIS),
            intervalDuration.coerceAtLeast(MIN_PERIODIC_INTERVAL_MILLIS),
        )
    }

    /**
     * Sets the periodic interval for this unit of work.
     *
     * @param intervalDuration The interval in milliseconds
     * @param flexDuration The flex duration in milliseconds
     */
    public fun setPeriodic(intervalDuration: Long, flexDuration: Long) {
        if (intervalDuration < MIN_PERIODIC_INTERVAL_MILLIS) {
            Logger.get()
                .warning(
                    TAG,
                    "Interval duration lesser than minimum allowed value; " +
                        "Changed to $MIN_PERIODIC_INTERVAL_MILLIS",
                )
        }

        this.intervalDuration = intervalDuration.coerceAtLeast(MIN_PERIODIC_INTERVAL_MILLIS)

        if (flexDuration < MIN_PERIODIC_FLEX_MILLIS) {
            Logger.get()
                .warning(
                    TAG,
                    "Flex duration lesser than minimum allowed value; " +
                        "Changed to $MIN_PERIODIC_FLEX_MILLIS",
                )
        }
        if (flexDuration > this.intervalDuration) {
            Logger.get()
                .warning(
                    TAG,
                    "Flex duration greater than interval duration; Changed to $intervalDuration",
                )
        }
        this.flexDuration = flexDuration.coerceIn(MIN_PERIODIC_FLEX_MILLIS, this.intervalDuration)
    }

    /**
     * Calculates the UTC time at which this [WorkSpec] should be allowed to run. This method
     * accounts for work that is backed off or periodic.
     *
     * If Backoff Policy is set to [BackoffPolicy.EXPONENTIAL], then delay increases at an
     * exponential rate with respect to the run attempt count and is capped at
     * [WorkRequest.MAX_BACKOFF_MILLIS].
     *
     * If Backoff Policy is set to [BackoffPolicy.LINEAR], then delay increases at an linear rate
     * with respect to the run attempt count and is capped at [WorkRequest.MAX_BACKOFF_MILLIS].
     *
     * Based on {@see
     * https://android.googlesource.com/platform/frameworks/base/+/master/services/core/java/com/android/server/job/JobSchedulerService.java#1125}
     *
     * Note that this runtime is for WorkManager internal use and may not match what the OS
     * considers to be the next runtime.
     *
     * For jobs with constraints, this represents the earliest time at which constraints should be
     * monitored for this work.
     *
     * For jobs without constraints, this represents the earliest time at which this work is allowed
     * to run.
     *
     * @return UTC time at which this [WorkSpec] should be allowed to run.
     */
    public fun calculateNextRunTime(): Long {
        return calculateNextRunTime(
            isBackedOff = isBackedOff,
            runAttemptCount = runAttemptCount,
            backoffPolicy = backoffPolicy,
            backoffDelayDuration = backoffDelayDuration,
            lastEnqueueTime = lastEnqueueTime,
            periodCount = periodCount,
            isPeriodic = isPeriodic,
            initialDelay = initialDelay,
            flexDuration = flexDuration,
            intervalDuration = intervalDuration,
            nextScheduleTimeOverride = nextScheduleTimeOverride,
        )
    }

    /** @return `true` if the [WorkSpec] has constraints. */
    public fun hasConstraints(): Boolean {
        return Constraints.NONE != constraints
    }

    override fun toString(): String {
        return "{WorkSpec: $id}"
    }

    /** A POJO containing the ID and state of a WorkSpec. */
    public data class IdAndState(
        @JvmField @ColumnInfo(name = "id") var id: String,
        @JvmField @ColumnInfo(name = "state") var state: WorkInfo.State,
    )

    /** A POJO containing externally queryable info for the WorkSpec. */
    public data class WorkInfoPojo(
        @ColumnInfo(name = "id") val id: String,
        @ColumnInfo(name = "state") val state: WorkInfo.State,
        @ColumnInfo(name = "output") val output: Data,
        @ColumnInfo(name = "initial_delay") val initialDelay: Long = 0,
        @ColumnInfo(name = "interval_duration") val intervalDuration: Long = 0,
        @ColumnInfo(name = "flex_duration") val flexDuration: Long = 0,
        @Embedded val constraints: Constraints,
        @ColumnInfo(name = "run_attempt_count") val runAttemptCount: Int,
        @ColumnInfo(name = "backoff_policy")
        var backoffPolicy: BackoffPolicy = BackoffPolicy.EXPONENTIAL,
        @ColumnInfo(name = "backoff_delay_duration")
        var backoffDelayDuration: Long = WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS,
        @ColumnInfo(name = "last_enqueue_time") var lastEnqueueTime: Long = 0,
        @ColumnInfo(name = "period_count", defaultValue = "0") var periodCount: Int = 0,
        @ColumnInfo(name = "generation") val generation: Int,
        @ColumnInfo(name = "next_schedule_time_override") val nextScheduleTimeOverride: Long,
        @ColumnInfo(name = "stop_reason") val stopReason: Int,
        @Relation(
            parentColumn = "id",
            entityColumn = "work_spec_id",
            entity = WorkTag::class,
            projection = ["tag"],
        )
        val tags: List<String>,

        // This is actually a 1-1 relationship. However Room 2.1 models the type as a List.
        // This will change in Room 2.2
        @Relation(
            parentColumn = "id",
            entityColumn = "work_spec_id",
            entity = WorkProgress::class,
            projection = ["progress"],
        )
        val progress: List<Data>,
    ) {
        val isPeriodic: Boolean
            get() = intervalDuration != 0L

        val isBackedOff: Boolean
            get() = state == WorkInfo.State.ENQUEUED && runAttemptCount > 0

        /**
         * Converts this POJO to a [WorkInfo].
         *
         * @return The [WorkInfo] represented by this POJO
         */
        public fun toWorkInfo(): WorkInfo {
            val progress = if (progress.isNotEmpty()) progress[0] else Data.EMPTY
            return WorkInfo(
                UUID.fromString(id),
                state,
                HashSet(tags),
                output,
                progress,
                runAttemptCount,
                generation,
                constraints,
                initialDelay,
                getPeriodicityOrNull(),
                calculateNextRunTimeMillis(),
                stopReason,
            )
        }

        private fun getPeriodicityOrNull() =
            if (intervalDuration != 0L) WorkInfo.PeriodicityInfo(intervalDuration, flexDuration)
            else null

        private fun calculateNextRunTimeMillis(): Long {
            return if (state == WorkInfo.State.ENQUEUED)
                calculateNextRunTime(
                    isBackedOff = isBackedOff,
                    runAttemptCount = runAttemptCount,
                    backoffPolicy = backoffPolicy,
                    backoffDelayDuration = backoffDelayDuration,
                    lastEnqueueTime = lastEnqueueTime,
                    periodCount = periodCount,
                    isPeriodic = isPeriodic,
                    initialDelay = initialDelay,
                    flexDuration = flexDuration,
                    intervalDuration = intervalDuration,
                    nextScheduleTimeOverride = nextScheduleTimeOverride,
                )
            else Long.MAX_VALUE
        }
    }

    public companion object {
        private val TAG = Logger.tagWithPrefix("WorkSpec")
        public const val SCHEDULE_NOT_REQUESTED_YET: Long = -1

        @JvmField
        public val WORK_INFO_MAPPER: Function<List<WorkInfoPojo>, List<WorkInfo>> =
            Function { input ->
                input?.map { it.toWorkInfo() }
            }

        public fun calculateNextRunTime(
            isBackedOff: Boolean,
            runAttemptCount: Int,
            backoffPolicy: BackoffPolicy,
            backoffDelayDuration: Long,
            lastEnqueueTime: Long,
            periodCount: Int,
            isPeriodic: Boolean,
            initialDelay: Long,
            flexDuration: Long,
            intervalDuration: Long,
            nextScheduleTimeOverride: Long,
        ): Long {
            // Override takes priority over backoff, but only applies to periodic work.
            return if (nextScheduleTimeOverride != Long.MAX_VALUE && isPeriodic) {
                return if (periodCount == 0) nextScheduleTimeOverride
                else
                    nextScheduleTimeOverride.coerceAtLeast(
                        lastEnqueueTime + MIN_PERIODIC_INTERVAL_MILLIS
                    )
            } else if (isBackedOff) {
                val isLinearBackoff = backoffPolicy == BackoffPolicy.LINEAR
                val delay =
                    if (isLinearBackoff) backoffDelayDuration * runAttemptCount
                    else Math.scalb(backoffDelayDuration.toFloat(), runAttemptCount - 1).toLong()
                lastEnqueueTime + delay.coerceAtMost(WorkRequest.MAX_BACKOFF_MILLIS)
            } else if (isPeriodic) {
                // The first run of a periodic work request is immediate in JobScheduler, so
                // don't apply intervalDuration to the first run.
                var schedule =
                    if (periodCount == 0) lastEnqueueTime + initialDelay
                    else lastEnqueueTime + intervalDuration

                val isFlexApplicable = flexDuration != intervalDuration
                // Flex only applies to the first run of a Periodic worker, to avoid
                // repeatedly pushing the schedule forward on every period.
                if (isFlexApplicable && periodCount == 0) {
                    // With flex, the first run does not run immediately, but instead respects
                    // the first interval duration.
                    schedule += (intervalDuration - flexDuration)
                }

                schedule
            } else if (lastEnqueueTime == NOT_ENQUEUED) {
                // If never enqueued, we aren't scheduled to run.
                Long.MAX_VALUE // 200 million years.
            } else {
                lastEnqueueTime + initialDelay
            }
        }
    }
}

public data class WorkGenerationalId(val workSpecId: String, val generation: Int)

public fun WorkSpec.generationalId(): WorkGenerationalId = WorkGenerationalId(id, generation)

private const val NOT_ENQUEUED = -1L
