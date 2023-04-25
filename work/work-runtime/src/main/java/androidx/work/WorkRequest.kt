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

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.work.impl.utils.toMillisCompat
import androidx.work.impl.model.WorkSpec
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * The base class for specifying parameters for work that should be enqueued in [WorkManager].
 * There are two concrete implementations of this class: [OneTimeWorkRequest] and
 * [PeriodicWorkRequest].
 */
abstract class WorkRequest internal constructor(
    /**
     * The unique identifier associated with this unit of work.
     */
    open val id: UUID,
    /**
     * The [WorkSpec] associated with this unit of work.
     *
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val workSpec: WorkSpec,
    /**
     * The tags associated with this unit of work.
     *
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val tags: Set<String>
) {

    /**
     * Gets the string for the unique identifier associated with this unit of work.
     *
     * @return The string identifier for this unit of work
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val stringId: String
        get() = id.toString()

    /**
     * A builder for [WorkRequest]s.  There are two concrete implementations of this class:
     * [OneTimeWorkRequest.Builder] and [PeriodicWorkRequest.Builder].
     */
    abstract class Builder<B : Builder<B, *>, W : WorkRequest> internal constructor(
        internal val workerClass: Class<out ListenableWorker>
    ) {
        internal var backoffCriteriaSet = false
        internal var id: UUID = UUID.randomUUID()
        internal var workSpec: WorkSpec = WorkSpec(id.toString(), workerClass.name)
        internal val tags: MutableSet<String> = mutableSetOf(workerClass.name)

        /**
         * The id of the request.
         *
         * It is a useful for the creation of `WorkRequest` for the [WorkManager.updateWork],
         * that uses `id` for identifying an work that should be updated.
         */
        @SuppressWarnings("SetterReturnsThis")
        fun setId(id: UUID): B {
            this.id = id
            workSpec = WorkSpec(id.toString(), workSpec)
            return thisObject
        }

        /**
         * Sets the backoff policy and backoff delay for the work.  The default values are
         * [BackoffPolicy.EXPONENTIAL] and
         * {@value WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS}, respectively.  `backoffDelay`
         * will be clamped between [WorkRequest.MIN_BACKOFF_MILLIS] and
         * [WorkRequest.MAX_BACKOFF_MILLIS].
         *
         * @param backoffPolicy The [BackoffPolicy] to use when increasing backoff time
         * @param backoffDelay Time to wait before retrying the work in `timeUnit` units
         * @param timeUnit The [TimeUnit] for `backoffDelay`
         * @return The current [Builder]
         */
        fun setBackoffCriteria(
            backoffPolicy: BackoffPolicy,
            backoffDelay: Long,
            timeUnit: TimeUnit
        ): B {
            backoffCriteriaSet = true
            workSpec.backoffPolicy = backoffPolicy
            workSpec.setBackoffDelayDuration(timeUnit.toMillis(backoffDelay))
            return thisObject
        }

        /**
         * Sets the backoff policy and backoff delay for the work.  The default values are
         * [BackoffPolicy.EXPONENTIAL] and
         * {@value WorkRequest#DEFAULT_BACKOFF_DELAY_MILLIS}, respectively.  `duration` will
         * be clamped between [WorkRequest.MIN_BACKOFF_MILLIS] and
         * [WorkRequest.MAX_BACKOFF_MILLIS].
         *
         * @param backoffPolicy The [BackoffPolicy] to use when increasing backoff time
         * @param duration Time to wait before retrying the work
         * @return The current [Builder]
         */
        @RequiresApi(26)
        fun setBackoffCriteria(backoffPolicy: BackoffPolicy, duration: Duration): B {
            backoffCriteriaSet = true
            workSpec.backoffPolicy = backoffPolicy
            workSpec.setBackoffDelayDuration(duration.toMillisCompat())
            return thisObject
        }

        /**
         * Adds constraints to the [WorkRequest].
         *
         * @param constraints The constraints for the work
         * @return The current [Builder]
         */
        fun setConstraints(constraints: Constraints): B {
            workSpec.constraints = constraints
            return thisObject
        }

        /**
         * Adds input [Data] to the work.  If a worker has prerequisites in its chain, this
         * Data will be merged with the outputs of the prerequisites using an [InputMerger].
         *
         * @param inputData key/value pairs that will be provided to the worker
         * @return The current [Builder]
         */
        fun setInputData(inputData: Data): B {
            workSpec.input = inputData
            return thisObject
        }

        /**
         * Adds a tag for the work.  You can query and cancel work by tags.  Tags are particularly
         * useful for modules or libraries to find and operate on their own work.
         *
         * @param tag A tag for identifying the work in queries.
         * @return The current [Builder]
         */
        fun addTag(tag: String): B {
            tags.add(tag)
            return thisObject
        }

        /**
         * Specifies that the results of this work should be kept for at least the specified amount
         * of time.  After this time has elapsed, the results **may** be pruned at the discretion
         * of WorkManager when there are no pending dependent jobs.
         *
         * When the results of a work are pruned, it becomes impossible to query for its
         * [WorkInfo].
         *
         * Specifying a long duration here may adversely affect performance in terms of app storage
         * and database query time.
         *
         * @param duration The minimum duration of time (in `timeUnit` units) to keep the
         * results of this work
         * @param timeUnit The unit of time for `duration`
         * @return The current [Builder]
         */
        fun keepResultsForAtLeast(duration: Long, timeUnit: TimeUnit): B {
            workSpec.minimumRetentionDuration = timeUnit.toMillis(duration)
            return thisObject
        }

        /**
         * Specifies that the results of this work should be kept for at least the specified amount
         * of time.  After this time has elapsed, the results may be pruned at the discretion
         * of WorkManager when this WorkRequest has reached a finished state (see
         * [WorkInfo.State.isFinished]) and there are no pending dependent jobs.
         *
         * When the results of a work are pruned, it becomes impossible to query for its
         * [WorkInfo].
         *
         * Specifying a long duration here may adversely affect performance in terms of app storage
         * and database query time.
         *
         * @param duration The minimum duration of time to keep the results of this work
         * @return The current [Builder]
         */
        @RequiresApi(26)
        fun keepResultsForAtLeast(duration: Duration): B {
            workSpec.minimumRetentionDuration = duration.toMillisCompat()
            return thisObject
        }

        /**
         * Sets an initial delay for the [WorkRequest].
         *
         * @param duration The length of the delay in `timeUnit` units
         * @param timeUnit The units of time for `duration`
         * @return The current [Builder]
         * @throws IllegalArgumentException if the given initial delay will push the execution time
         * past `Long.MAX_VALUE` and cause an overflow
         */
        open fun setInitialDelay(duration: Long, timeUnit: TimeUnit): B {
            workSpec.initialDelay = timeUnit.toMillis(duration)
            require(Long.MAX_VALUE - System.currentTimeMillis() > workSpec.initialDelay) {
                ("The given initial delay is too large and will cause an overflow!")
            }
            return thisObject
        }

        /**
         * Sets an initial delay for the [WorkRequest].
         *
         * @param duration The length of the delay
         * @return The current [Builder]
         * @throws IllegalArgumentException if the given initial delay will push the execution time
         * past `Long.MAX_VALUE` and cause an overflow
         */
        @RequiresApi(26)
        open fun setInitialDelay(duration: Duration): B {
            workSpec.initialDelay = duration.toMillisCompat()
            require(Long.MAX_VALUE - System.currentTimeMillis() > workSpec.initialDelay) {
                "The given initial delay is too large and will cause an overflow!"
            }
            return thisObject
        }

        /**
         * Marks the [WorkRequest] as important to the user.  In this case, WorkManager
         * provides an additional signal to the OS that this work is important.
         *
         * @param policy The [OutOfQuotaPolicy] to be used.
         */
        @SuppressLint("MissingGetterMatchingBuilder")
        open fun setExpedited(policy: OutOfQuotaPolicy): B {
            workSpec.expedited = true
            workSpec.outOfQuotaPolicy = policy
            return thisObject
        }

        /**
         * Builds a [WorkRequest] based on this [Builder].
         *
         * @return A [WorkRequest] based on this [Builder]
         */
        fun build(): W {
            val returnValue = buildInternal()
            val constraints = workSpec.constraints
            // Check for unsupported constraints.
            val hasUnsupportedConstraints =
                (Build.VERSION.SDK_INT >= 24 && constraints.hasContentUriTriggers() ||
                    constraints.requiresBatteryNotLow() ||
                    constraints.requiresCharging() ||
                    Build.VERSION.SDK_INT >= 23 && constraints.requiresDeviceIdle())
            if (workSpec.expedited) {
                require(!hasUnsupportedConstraints) {
                    "Expedited jobs only support network and storage constraints"
                }
                require(workSpec.initialDelay <= 0) { "Expedited jobs cannot be delayed" }
            }
            // Create a new id and WorkSpec so this WorkRequest.Builder can be used multiple times.
            setId(UUID.randomUUID())
            return returnValue
        }

        internal abstract fun buildInternal(): W

        internal abstract val thisObject: B

        /**
         * Sets the initial state for this work.  Used in testing only.
         *
         * @param state The [WorkInfo.State] to set
         * @return The current [Builder]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        fun setInitialState(state: WorkInfo.State): B {
            workSpec.state = state
            return thisObject
        }

        /**
         * Sets the initial run attempt count for this work.  Used in testing only.
         *
         * @param runAttemptCount The initial run attempt count
         * @return The current [Builder]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        fun setInitialRunAttemptCount(runAttemptCount: Int): B {
            workSpec.runAttemptCount = runAttemptCount
            return thisObject
        }

        /**
         * Sets the enqueue time for this work. Used in testing only.
         *
         * @param lastEnqueueTime The enqueue time in `timeUnit` units
         * @param timeUnit The [TimeUnit] for `periodStartTime`
         * @return The current [Builder]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        fun setLastEnqueueTime(lastEnqueueTime: Long, timeUnit: TimeUnit): B {
            workSpec.lastEnqueueTime = timeUnit.toMillis(lastEnqueueTime)
            return thisObject
        }

        /**
         * Sets when the scheduler actually schedules the worker.
         *
         * @param scheduleRequestedAt The time at which the scheduler scheduled a worker.
         * @param timeUnit The [TimeUnit] for `scheduleRequestedAt`
         * @return The current [Builder]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @VisibleForTesting
        fun setScheduleRequestedAt(scheduleRequestedAt: Long, timeUnit: TimeUnit): B {
            workSpec.scheduleRequestedAt = timeUnit.toMillis(scheduleRequestedAt)
            return thisObject
        }
    }

    companion object {
        /**
         * The default initial backoff time (in milliseconds) for work that has to be retried.
         */
        const val DEFAULT_BACKOFF_DELAY_MILLIS = 30000L

        /**
         * The maximum backoff time (in milliseconds) for work that has to be retried.
         */
        @SuppressLint("MinMaxConstant")
        const val MAX_BACKOFF_MILLIS = 5 * 60 * 60 * 1000L // 5 hours

        /**
         * The minimum backoff time for work (in milliseconds) that has to be retried.
         */
        @SuppressLint("MinMaxConstant")
        const val MIN_BACKOFF_MILLIS = 10 * 1000L // 10 seconds.
    }
}