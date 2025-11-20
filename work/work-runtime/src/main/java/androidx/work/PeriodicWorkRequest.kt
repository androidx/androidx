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
import androidx.annotation.RequiresApi
import androidx.work.PeriodicWorkRequest.Companion.MIN_PERIODIC_INTERVAL_MILLIS
import androidx.work.impl.utils.toMillisCompat
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * A [WorkRequest] for repeating work. This work executes multiple times until it is cancelled, with
 * the first execution happening immediately or as soon as the given [Constraints] are met. The next
 * execution will happen during the period interval; note that execution may be delayed because
 * [WorkManager] is subject to OS battery optimizations, such as doze mode.
 *
 * You can control when the work executes in the period interval more exactly - see
 * [PeriodicWorkRequest.Builder] for documentation on `flexInterval`s.
 *
 * Periodic work has a minimum interval of 15 minutes.
 *
 * Periodic work is intended for use cases where you want a fairly consistent delay between
 * consecutive runs, and you are willing to accept inexactness due to battery optimizations and doze
 * mode. Please note that if your periodic work has constraints, it will not execute until the
 * constraints are met, even if the delay between periods has been met.
 *
 * If you need to schedule work that happens exactly at a certain time or only during a certain time
 * window, you should consider using [OneTimeWorkRequest]s.
 *
 * The normal lifecycle of a PeriodicWorkRequest is `ENQUEUED -> RUNNING -> ENQUEUED`. By
 * definition, periodic work cannot terminate in a succeeded or failed state, since it must recur.
 * It can only terminate if explicitly cancelled. However, in the case of retries, periodic work
 * will still back off according to [PeriodicWorkRequest.Builder.setBackoffCriteria].
 *
 * Periodic work cannot be part of a chain or graph of work.
 */
public class PeriodicWorkRequest internal constructor(builder: Builder) :
    WorkRequest(builder.id, builder.workSpec, builder.tags) {

    /** Builder for [PeriodicWorkRequest]s. */
    public class Builder : WorkRequest.Builder<Builder, PeriodicWorkRequest> {

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once every interval period. The
         * [PeriodicWorkRequest] is guaranteed to run exactly one time during this interval (subject
         * to OS battery optimizations, such as doze mode). The repeat interval must be greater than
         * or equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]. It may run immediately,
         * at the end of the period, or any time in between so long as the other conditions are
         * satisfied at the time. The run time of the [PeriodicWorkRequest] can be restricted to a
         * flex period within an interval (see `#Builder(Class, long, TimeUnit, long, TimeUnit)`).
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval in `repeatIntervalTimeUnit` units
         * @param repeatIntervalTimeUnit The [TimeUnit] for `repeatInterval`
         */
        public constructor(
            workerClass: Class<out ListenableWorker?>,
            repeatInterval: Long,
            repeatIntervalTimeUnit: TimeUnit,
        ) : super(workerClass) {
            workSpec.setPeriodic(repeatIntervalTimeUnit.toMillis(repeatInterval))
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once every interval period. The
         * [PeriodicWorkRequest] is guaranteed to run exactly one time during this interval (subject
         * to OS battery optimizations, such as doze mode). The repeat interval must be greater than
         * or equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]. It may run immediately,
         * at the end of the period, or any time in between so long as the other conditions are
         * satisfied at the time. The run time of the [PeriodicWorkRequest] can be restricted to a
         * flex period within an interval (see `#Builder(Class, long, TimeUnit, long, TimeUnit)`).
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval in `repeatIntervalTimeUnit` units
         * @param repeatIntervalTimeUnit The [TimeUnit] for `repeatInterval`
         */
        public constructor(
            workerClass: KClass<out ListenableWorker>,
            repeatInterval: Long,
            repeatIntervalTimeUnit: TimeUnit,
        ) : super(workerClass.java) {
            workSpec.setPeriodic(repeatIntervalTimeUnit.toMillis(repeatInterval))
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once every interval period. The
         * [PeriodicWorkRequest] is guaranteed to run exactly one time during this interval (subject
         * to OS battery optimizations, such as doze mode). The repeat interval must be greater than
         * or equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]. It may run immediately,
         * at the end of the period, or any time in between so long as the other conditions are
         * satisfied at the time. The run time of the [PeriodicWorkRequest] can be restricted to a
         * flex period within an interval (see `#Builder(Class, Duration, Duration)`).
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval
         */
        @RequiresApi(26)
        public constructor(
            workerClass: Class<out ListenableWorker>,
            repeatInterval: Duration,
        ) : super(workerClass) {
            workSpec.setPeriodic(repeatInterval.toMillisCompat())
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once every interval period. The
         * [PeriodicWorkRequest] is guaranteed to run exactly one time during this interval (subject
         * to OS battery optimizations, such as doze mode). The repeat interval must be greater than
         * or equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS]. It may run immediately,
         * at the end of the period, or any time in between so long as the other conditions are
         * satisfied at the time. The run time of the [PeriodicWorkRequest] can be restricted to a
         * flex period within an interval (see `#Builder(Class, Duration, Duration)`).
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval
         */
        @RequiresApi(26)
        public constructor(
            workerClass: KClass<out ListenableWorker>,
            repeatInterval: Duration,
        ) : super(workerClass.java) {
            workSpec.setPeriodic(repeatInterval.toMillisCompat())
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once within the **flex period** of
         * every interval period. See diagram below. The flex period begins at `repeatInterval -
         * flexInterval` to the end of the interval. The repeat interval must be greater than or
         * equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS] and the flex interval must be
         * greater than or equal to [PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS].
         *
         *  ```
         * [_____before flex_____|_____flex_____][_____before flex_____|_____flex_____]...
         * [___cannot run work___|_can run work_][___cannot run work___|_can run work_]...
         * \____________________________________/\____________________________________/...
         * interval 1                            interval 2             ...(repeat)
         * ```
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval in `repeatIntervalTimeUnit` units
         * @param repeatIntervalTimeUnit The [TimeUnit] for `repeatInterval`
         * @param flexInterval The duration in `flexIntervalTimeUnit` units for which this work
         *   repeats from the end of the `repeatInterval`
         * @param flexIntervalTimeUnit The [TimeUnit] for `flexInterval`
         */
        public constructor(
            workerClass: Class<out ListenableWorker?>,
            repeatInterval: Long,
            repeatIntervalTimeUnit: TimeUnit,
            flexInterval: Long,
            flexIntervalTimeUnit: TimeUnit,
        ) : super(workerClass) {
            workSpec.setPeriodic(
                repeatIntervalTimeUnit.toMillis(repeatInterval),
                flexIntervalTimeUnit.toMillis(flexInterval),
            )
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once within the **flex period** of
         * every interval period. See diagram below. The flex period begins at `repeatInterval -
         * flexInterval` to the end of the interval. The repeat interval must be greater than or
         * equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS] and the flex interval must be
         * greater than or equal to [PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS].
         *
         *  ```
         * [_____before flex_____|_____flex_____][_____before flex_____|_____flex_____]...
         * [___cannot run work___|_can run work_][___cannot run work___|_can run work_]...
         * \____________________________________/\____________________________________/...
         * interval 1                            interval 2             ...(repeat)
         * ```
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval in `repeatIntervalTimeUnit` units
         * @param repeatIntervalTimeUnit The [TimeUnit] for `repeatInterval`
         * @param flexInterval The duration in `flexIntervalTimeUnit` units for which this work
         *   repeats from the end of the `repeatInterval`
         * @param flexIntervalTimeUnit The [TimeUnit] for `flexInterval`
         */
        public constructor(
            workerClass: KClass<out ListenableWorker>,
            repeatInterval: Long,
            repeatIntervalTimeUnit: TimeUnit,
            flexInterval: Long,
            flexIntervalTimeUnit: TimeUnit,
        ) : super(workerClass.java) {
            workSpec.setPeriodic(
                repeatIntervalTimeUnit.toMillis(repeatInterval),
                flexIntervalTimeUnit.toMillis(flexInterval),
            )
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once within the **flex period** of
         * every interval period. See diagram below. The flex period begins at `repeatInterval -
         * flexInterval` to the end of the interval. The repeat interval must be greater than or
         * equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS] and the flex interval must be
         * greater than or equal to [PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS].
         *
         *  ```
         * [_____before flex_____|_____flex_____][_____before flex_____|_____flex_____]...
         * [___cannot run work___|_can run work_][___cannot run work___|_can run work_]...
         * \____________________________________/\____________________________________/...
         * interval 1                            interval 2             ...(repeat)
         * ```
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval
         * @param flexInterval The duration in for which this work repeats from the end of the
         *   `repeatInterval`
         */
        @RequiresApi(26)
        public constructor(
            workerClass: Class<out ListenableWorker?>,
            repeatInterval: Duration,
            flexInterval: Duration,
        ) : super(workerClass) {
            workSpec.setPeriodic(repeatInterval.toMillisCompat(), flexInterval.toMillisCompat())
        }

        /**
         * Creates a [PeriodicWorkRequest] to run periodically once within the **flex period** of
         * every interval period. See diagram below. The flex period begins at `repeatInterval -
         * flexInterval` to the end of the interval. The repeat interval must be greater than or
         * equal to [PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS] and the flex interval must be
         * greater than or equal to [PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS].
         *
         *  ```
         * [_____before flex_____|_____flex_____][_____before flex_____|_____flex_____]...
         * [___cannot run work___|_can run work_][___cannot run work___|_can run work_]...
         * \____________________________________/\____________________________________/...
         * interval 1                            interval 2             ...(repeat)
         * ```
         *
         * @param workerClass The [ListenableWorker] class to run for this work
         * @param repeatInterval The repeat interval
         * @param flexInterval The duration in for which this work repeats from the end of the
         *   `repeatInterval`
         */
        @RequiresApi(26)
        public constructor(
            workerClass: KClass<out ListenableWorker>,
            repeatInterval: Duration,
            flexInterval: Duration,
        ) : super(workerClass.java) {
            workSpec.setPeriodic(repeatInterval.toMillisCompat(), flexInterval.toMillisCompat())
        }

        /**
         * Overrides the next time this work is scheduled to run.
         *
         * Calling this method sets a specific time at which the work will be scheduled to run next,
         * overriding the normal interval, flex, initial delay, and backoff.
         *
         * This allows dynamic calculation of the next Periodic work schedule, which can be used to
         * implement advanced features like adaptive refresh times, custom retry behavior, or making
         * a newsfeed worker run before the user wakes up every morning without drift.
         * [ExistingPeriodicWorkPolicy.UPDATE] should be used with these techniques to avoid
         * cancelling a currently-running worker while scheduling the next one.
         *
         * This method only sets the single next Work schedule. After that Work finishes, the
         * override will be cleared and the Work will be scheduled normally according to the
         * interval or backoff. The override can be cleared by setting
         * [clearNextScheduleTimeOverride] on a work update request. Otherwise, the override time
         * will persist after unrelated invocations of [WorkManager.updateWork].
         *
         * This method can be used from outside or inside a [Worker.startWork] method. If the Worker
         * is currently running, then it will override the next time the Work starts, even if the
         * current Worker returns [ListenableWorker.Result.Retry]. This behavior can be used to
         * customize the backoff behavior of a Worker by catching Exceptions in startWork and using
         * this method to schedule a retry.
         *
         * [MIN_PERIODIC_INTERVAL_MILLIS] is enforced on this method to prevent infinite loops. If a
         * previous run time occurred less than the minimum period before the override time, then
         * the override schedule will be delayed to preserve the minimum spacing. This restriction
         * does not apply to the very first run of periodic work, which may be instant.
         *
         * Work will almost never run at this exact time in the real world. This method assigns the
         * scheduled run time accurately, but cannot guarantee an actual run time. Actual Work run
         * times are dependent on many factors like the underlying system scheduler, doze and power
         * saving modes of the OS, and meeting any configured constraints. This is expected and is
         * not considered a bug.
         *
         * @param nextScheduleTimeOverrideMillis The time, in [System.currentTimeMillis] time, to
         *   schedule this work next. If this is in the past, work may run immediately.
         */
        public fun setNextScheduleTimeOverride(nextScheduleTimeOverrideMillis: Long): Builder {
            require(nextScheduleTimeOverrideMillis != Long.MAX_VALUE) {
                "Cannot set Long.MAX_VALUE as the schedule override time"
            }

            workSpec.nextScheduleTimeOverride = nextScheduleTimeOverrideMillis
            workSpec.nextScheduleTimeOverrideGeneration = 1
            return this
        }

        /**
         * Clears any override set by [setNextScheduleTimeOverride].
         *
         * When an override is cleared, the next schedule is based on the previous enqueue time or
         * run time of the Work and the result of that previous run. Eg. if the previous run
         * returned [ListenableWorker.Result.Retry] at some time=T, and the next run was set by
         * override, then clearing that override will return the schedule to `T+backoffInterval` if
         * using linear backoff.
         *
         * Override may be cleared while a Worker is running. The worker will schedule the next run
         * based on its result type and interval.
         */
        public fun clearNextScheduleTimeOverride(): Builder {
            workSpec.nextScheduleTimeOverride = Long.MAX_VALUE
            // Clearing an override increments the generation.
            workSpec.nextScheduleTimeOverrideGeneration = 1
            return this
        }

        override fun buildInternal(): PeriodicWorkRequest {
            require(!(backoffCriteriaSet && workSpec.constraints.requiresDeviceIdle())) {
                "Cannot set backoff criteria on an idle mode job"
            }
            require(!workSpec.expedited) { "PeriodicWorkRequests cannot be expedited" }
            return PeriodicWorkRequest(this)
        }

        override val thisObject: Builder
            get() = this
    }

    public companion object {
        /** The minimum interval duration for [PeriodicWorkRequest] (in milliseconds). */
        @SuppressLint("MinMaxConstant")
        public const val MIN_PERIODIC_INTERVAL_MILLIS: Long = 15 * 60 * 1000L // 15 minutes.

        /** The minimum flex duration for [PeriodicWorkRequest] (in milliseconds). */
        @SuppressLint("MinMaxConstant")
        public const val MIN_PERIODIC_FLEX_MILLIS: Long = 5 * 60 * 1000L // 5 minutes.
    }
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [ListenableWorker].
 *
 * @param repeatInterval @see [PeriodicWorkRequest.Builder]
 * @param repeatIntervalTimeUnit @see [PeriodicWorkRequest.Builder]
 */
public inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Long,
    repeatIntervalTimeUnit: TimeUnit,
): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval, repeatIntervalTimeUnit)
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [ListenableWorker].
 *
 * @param repeatInterval @see [PeriodicWorkRequest.Builder]
 */
@RequiresApi(26)
public inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Duration
): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval)
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [ListenableWorker].
 *
 * @param repeatInterval @see [PeriodicWorkRequest.Builder]
 * @param repeatIntervalTimeUnit @see [PeriodicWorkRequest.Builder]
 * @param flexTimeInterval @see [PeriodicWorkRequest.Builder]
 * @param flexTimeIntervalUnit @see [PeriodicWorkRequest.Builder]
 */
public inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Long,
    repeatIntervalTimeUnit: TimeUnit,
    flexTimeInterval: Long,
    flexTimeIntervalUnit: TimeUnit,
): PeriodicWorkRequest.Builder {

    return PeriodicWorkRequest.Builder(
        W::class.java,
        repeatInterval,
        repeatIntervalTimeUnit,
        flexTimeInterval,
        flexTimeIntervalUnit,
    )
}

/**
 * Creates a [PeriodicWorkRequest.Builder] with a given [ListenableWorker].
 *
 * @param repeatInterval @see [PeriodicWorkRequest.Builder]
 * @param flexTimeInterval @see [PeriodicWorkRequest.Builder]
 */
@RequiresApi(26)
public inline fun <reified W : ListenableWorker> PeriodicWorkRequestBuilder(
    repeatInterval: Duration,
    flexTimeInterval: Duration,
): PeriodicWorkRequest.Builder {
    return PeriodicWorkRequest.Builder(W::class.java, repeatInterval, flexTimeInterval)
}
