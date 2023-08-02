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

import android.os.Build
import android.util.Log
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.core.util.Consumer
import androidx.work.impl.DefaultRunnableScheduler
import androidx.work.impl.Scheduler
import androidx.work.impl.utils.INITIAL_ID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

/**
 * The Configuration object used to customize [WorkManager] upon initialization.
 * Configuration contains various parameters used to setup WorkManager.  For example, it is possible
 * to customize the [Executor] used by [Worker]s here.
 *
 * To set a custom Configuration for WorkManager, see [WorkManager.initialize].
 */
class Configuration internal constructor(builder: Builder) {
    /**
     * The [Executor] used by [WorkManager] to execute [Worker]s.
     */
    val executor: Executor

    /**
     * The [Executor] used by [WorkManager] for all its internal business logic
     */
    val taskExecutor: Executor

    /**
     * The [Clock] used by [WorkManager] to calculate schedules and perform book-keeping.
     */
    val clock: Clock

    /**
     * The [WorkerFactory] used by [WorkManager] to create [ListenableWorker]s
     */
    val workerFactory: WorkerFactory

    /**
     * The [InputMergerFactory] used by [WorkManager] to create instances of
     * [InputMerger]s.
     */
    val inputMergerFactory: InputMergerFactory

    /**
     * The [RunnableScheduler] to keep track of timed work in the in-process
     * scheduler.
     */
    val runnableScheduler: RunnableScheduler

    /**
     * The exception handler that is used to intercept
     * exceptions caused when trying to initialize [WorkManager].
     */
    val initializationExceptionHandler: Consumer<Throwable>?

    /**
     * The exception handler that can be used to intercept exceptions
     * caused when trying to schedule [WorkRequest]s.
     */
    val schedulingExceptionHandler: Consumer<Throwable>?

    /**
     * The [String] name of the process where work should be scheduled.
     */
    val defaultProcessName: String?

    /**
     * The minimum logging level, corresponding to the constants found in [android.util.Log]
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val minimumLoggingLevel: Int

    /**
     * The first valid id (inclusive) used by [WorkManager] when creating new
     * instances of [android.app.job.JobInfo]s.
     *
     * If the current `jobId` goes beyond the bounds of the defined range of
     * ([Configuration.minJobSchedulerId], [Configuration.maxJobSchedulerId]), it is reset to
     * ([Configuration.minJobSchedulerId]).
     */
    val minJobSchedulerId: Int

    /**
     * The last valid id (inclusive) used by [WorkManager] when
     * creating new instances of [android.app.job.JobInfo]s.
     *
     * If the current `jobId` goes beyond the bounds of the defined range of
     * ([Configuration.minJobSchedulerId], [Configuration.maxJobSchedulerId]), it is reset to
     * ([Configuration.minJobSchedulerId]).
     */
    val maxJobSchedulerId: Int

    /**
     * Maximum number of Workers with [Constraints.contentUriTriggers] that
     * could be enqueued simultaneously.
     *
     * Unlike the other workers Workers with [Constraints.contentUriTriggers] must immediately
     * occupy slots in JobScheduler to avoid missing updates, thus they are separated in the
     * its own category.
     */
    val contentUriTriggerWorkersLimit: Int

    /**
     * The maximum number of system requests which can be enqueued by [WorkManager]
     * when using [android.app.job.JobScheduler] or [android.app.AlarmManager]
     */
    @get:IntRange(from = MIN_SCHEDULER_LIMIT.toLong(), to = Scheduler.MAX_SCHEDULER_LIMIT.toLong())
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val maxSchedulerLimit: Int

    /**
     * @return `true` If the default task [Executor] is being used
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val isUsingDefaultTaskExecutor: Boolean

    init {
        executor = builder.executor ?: createDefaultExecutor(isTaskExecutor = false)
        isUsingDefaultTaskExecutor = builder.taskExecutor == null
        // This executor is used for *both* WorkManager's tasks and Room's query executor.
        // So this should not be a single threaded executor. Writes will still be serialized
        // as this will be wrapped with an SerialExecutor.
        taskExecutor = builder.taskExecutor ?: createDefaultExecutor(isTaskExecutor = true)
        clock = builder.clock ?: SystemClock()
        workerFactory = builder.workerFactory ?: WorkerFactory.getDefaultWorkerFactory()
        inputMergerFactory = builder.inputMergerFactory ?: NoOpInputMergerFactory
        runnableScheduler = builder.runnableScheduler ?: DefaultRunnableScheduler()
        minimumLoggingLevel = builder.loggingLevel
        minJobSchedulerId = builder.minJobSchedulerId
        maxJobSchedulerId = builder.maxJobSchedulerId
        maxSchedulerLimit = if (Build.VERSION.SDK_INT == 23) {
            // We double schedule jobs in SDK 23. So use half the number of max slots specified.
            builder.maxSchedulerLimit / 2
        } else {
            builder.maxSchedulerLimit
        }
        initializationExceptionHandler = builder.initializationExceptionHandler
        schedulingExceptionHandler = builder.schedulingExceptionHandler
        defaultProcessName = builder.defaultProcessName
        contentUriTriggerWorkersLimit = builder.contentUriTriggerWorkersLimit
    }

    /**
     * A Builder for [Configuration]s.
     */
    class Builder {
        internal var executor: Executor? = null
        internal var workerFactory: WorkerFactory? = null
        internal var inputMergerFactory: InputMergerFactory? = null
        internal var taskExecutor: Executor? = null
        internal var clock: Clock? = null
        internal var runnableScheduler: RunnableScheduler? = null
        internal var initializationExceptionHandler: Consumer<Throwable>? = null
        internal var schedulingExceptionHandler: Consumer<Throwable>? = null
        internal var defaultProcessName: String? = null
        internal var loggingLevel: Int = Log.INFO
        internal var minJobSchedulerId: Int = INITIAL_ID
        internal var maxJobSchedulerId: Int = Int.MAX_VALUE
        internal var maxSchedulerLimit: Int = MIN_SCHEDULER_LIMIT
        internal var contentUriTriggerWorkersLimit: Int = DEFAULT_CONTENT_URI_TRIGGERS_WORKERS_LIMIT

        /**
         * Creates a new [Configuration.Builder].
         */
        constructor()

        /**
         * Creates a new [Configuration.Builder] with an existing [Configuration] as its
         * template.
         *
         * @param configuration An existing [Configuration] to use as a template
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        constructor(configuration: Configuration) {
            // Note that these must be accessed through fields and not the getters, which can
            // otherwise manipulate the returned value (see getMaxSchedulerLimit(), for example).
            executor = configuration.executor
            workerFactory = configuration.workerFactory
            inputMergerFactory = configuration.inputMergerFactory
            taskExecutor = configuration.taskExecutor
            clock = configuration.clock
            loggingLevel = configuration.minimumLoggingLevel
            minJobSchedulerId = configuration.minJobSchedulerId
            maxJobSchedulerId = configuration.maxJobSchedulerId
            maxSchedulerLimit = configuration.maxSchedulerLimit
            runnableScheduler = configuration.runnableScheduler
            initializationExceptionHandler = configuration.initializationExceptionHandler
            schedulingExceptionHandler = configuration.schedulingExceptionHandler
            defaultProcessName = configuration.defaultProcessName
        }

        /**
         * Specifies a custom [WorkerFactory] for WorkManager.
         *
         * @param workerFactory A [WorkerFactory] for creating [ListenableWorker]s
         * @return This [Builder] instance
         */
        fun setWorkerFactory(workerFactory: WorkerFactory): Builder {
            this.workerFactory = workerFactory
            return this
        }

        /**
         * Specifies a custom [InputMergerFactory] for WorkManager.
         *
         * @param inputMergerFactory A [InputMergerFactory] for creating [InputMerger]s
         * @return This [Builder] instance
         */
        fun setInputMergerFactory(inputMergerFactory: InputMergerFactory): Builder {
            this.inputMergerFactory = inputMergerFactory
            return this
        }

        /**
         * Specifies a custom [Executor] for WorkManager.
         *
         * @param executor An [Executor] for running [Worker]s
         * @return This [Builder] instance
         */
        fun setExecutor(executor: Executor): Builder {
            this.executor = executor
            return this
        }

        /**
         * Specifies a [Executor] which will be used by WorkManager for all its
         * internal book-keeping.
         *
         * For best performance this [Executor] should be bounded.
         *
         * For more information look at [androidx.room.RoomDatabase.Builder.setQueryExecutor].
         *
         * @param taskExecutor The [Executor] which will be used by WorkManager for
         * all its internal book-keeping
         * @return This [Builder] instance
         */
        fun setTaskExecutor(taskExecutor: Executor): Builder {
            this.taskExecutor = taskExecutor
            return this
        }

        /**
         * Sets a [Clock] for WorkManager to calculate schedules and perform book-keeping.
         *
         * This should only be overridden for testing. It must return the same value as
         * [System.currentTimeMillis] in production code.
         *
         * @param clock The [Clock] to use
         * @return This [Builder] instance
         */
        fun setClock(clock: Clock): Builder {
            this.clock = clock
            return this
        }

        /**
         * Specifies the range of [android.app.job.JobInfo] IDs that can be used by
         * [WorkManager].  WorkManager needs a range of at least `1000` IDs.
         *
         * JobScheduler uses integers as identifiers for jobs, and WorkManager delegates to
         * JobScheduler on certain API levels.  In order to not clash job codes used in the rest of
         * your app, you can use this method to tell WorkManager the valid range of job IDs that it
         * can use.
         *
         * The default values are `0` and `Integer#MAX_VALUE`.
         *
         * @param minJobSchedulerId The first valid [android.app.job.JobInfo] ID (inclusive).
         * @param maxJobSchedulerId The last valid [android.app.job.JobInfo] ID (inclusive).
         * @return This [Builder] instance
         * @throws IllegalArgumentException when the size of the range is less than 1000
         */
        fun setJobSchedulerJobIdRange(minJobSchedulerId: Int, maxJobSchedulerId: Int): Builder {
            require(maxJobSchedulerId - minJobSchedulerId >= 1000) {
                "WorkManager needs a range of at least 1000 job ids."
            }
            this.minJobSchedulerId = minJobSchedulerId
            this.maxJobSchedulerId = maxJobSchedulerId
            return this
        }

        /**
         * Specifies the maximum number of system requests made by [WorkManager]
         * when using [android.app.job.JobScheduler] or [android.app.AlarmManager].
         *
         * By default, WorkManager might schedule a large number of alarms or JobScheduler
         * jobs.  If your app uses JobScheduler or AlarmManager directly, this might exhaust the
         * OS-enforced limit on the number of jobs or alarms an app is allowed to schedule.  To help
         * manage this situation, you can use this method to reduce the number of underlying jobs
         * and alarms that WorkManager might schedule.
         *
         * When the application exceeds this limit, WorkManager maintains an internal queue of
         * [WorkRequest]s, and schedules them when slots become free.
         *
         * WorkManager requires a minimum of [Configuration.MIN_SCHEDULER_LIMIT] slots; this
         * is also the default value. The total number of slots also cannot exceed `50`.
         *
         * @param maxSchedulerLimit The total number of jobs which can be enqueued by
         * [WorkManager] when using [android.app.job.JobScheduler].
         * @return This [Builder] instance
         * @throws IllegalArgumentException if `maxSchedulerLimit` is less than
         * [Configuration.MIN_SCHEDULER_LIMIT]
         */
        fun setMaxSchedulerLimit(maxSchedulerLimit: Int): Builder {
            require(maxSchedulerLimit >= MIN_SCHEDULER_LIMIT) {
                "WorkManager needs to be able to schedule at least 20 jobs in JobScheduler."
            }
            this.maxSchedulerLimit = min(maxSchedulerLimit, Scheduler.MAX_SCHEDULER_LIMIT)
            return this
        }

        /**
         * Specifies the maximum number of Workers with [Constraints.contentUriTriggers] that
         * could be enqueued simultaneously.
         *
         * Unlike the other workers Workers with [Constraints.contentUriTriggers] must immediately
         * occupy slots in JobScheduler to avoid missing updates, thus they are separated in the
         * its own category.
         */
        fun setContentUriTriggerWorkersLimit(contentUriTriggerWorkersLimit: Int): Builder {
            this.contentUriTriggerWorkersLimit = max(contentUriTriggerWorkersLimit, 0)
            return this
        }

        /**
         * Specifies the minimum logging level, corresponding to the constants found in
         * [android.util.Log]. For example, specifying [android.util.Log.VERBOSE] will
         * log everything, whereas specifying [android.util.Log.ERROR] will only log errors
         * and assertions.The default value is [android.util.Log.INFO].
         *
         * @param loggingLevel The minimum logging level, corresponding to the constants found in
         * [android.util.Log]
         * @return This [Builder] instance
         */
        fun setMinimumLoggingLevel(loggingLevel: Int): Builder {
            this.loggingLevel = loggingLevel
            return this
        }

        /**
         * Specifies the [RunnableScheduler] to be used by [WorkManager].
         *
         * This is used by the in-process scheduler to keep track of timed work.
         *
         * @param runnableScheduler The [RunnableScheduler] to be used
         * @return This [Builder] instance
         */
        fun setRunnableScheduler(runnableScheduler: RunnableScheduler): Builder {
            this.runnableScheduler = runnableScheduler
            return this
        }

        /**
         * Specifies a `Consumer<Throwable>` that can be used to intercept
         * exceptions caused when trying to initialize {@link WorkManager}, that usually happens
         * when WorkManager cannot access its internal datastore.
         *
         * This exception handler will be invoked on a thread bound to
         * [Configuration.taskExecutor].
         *
         * @param exceptionHandler an instance to handle exceptions
         * @return This [Builder] instance
         */
        fun setInitializationExceptionHandler(exceptionHandler: Consumer<Throwable>): Builder {
            this.initializationExceptionHandler = exceptionHandler
            return this
        }

        /**
         * Specifies a `Consumer<Throwable>` that can be used to intercept
         * exceptions caused when trying to schedule [WorkRequest]s.
         *
         * It allows the application to handle a [Throwable] throwable typically
         * caused when trying to schedule [WorkRequest]s.
         *
         * This exception handler will be invoked on a thread bound to
         * [Configuration.taskExecutor].
         *
         * @param schedulingExceptionHandler an instance to handle exceptions
         * @return This [Builder] instance
         */
        fun setSchedulingExceptionHandler(
            schedulingExceptionHandler: Consumer<Throwable>
        ): Builder {
            this.schedulingExceptionHandler = schedulingExceptionHandler
            return this
        }

        /**
         * Designates the primary process that [WorkManager] should schedule work in.
         *
         * @param processName The [String] process name.
         * @return This [Builder] instance
         */
        fun setDefaultProcessName(processName: String): Builder {
            defaultProcessName = processName
            return this
        }

        /**
         * Builds a [Configuration] object.
         *
         * @return A [Configuration] object with this [Builder]'s parameters.
         */
        fun build(): Configuration {
            return Configuration(this)
        }
    }

    /**
     * A class that can provide the [Configuration] for WorkManager and allow for on-demand
     * initialization of WorkManager.  To do this:
     *
     *  - Disable `androidx.work.WorkManagerInitializer` in your manifest
     *  - Implement the [Configuration.Provider] interface on your [android.app.Application] class
     *  - Use [WorkManager.getInstance] when accessing WorkManager (NOT [WorkManager.getInstance])
     *
     * Note that on-demand initialization may delay some useful features of WorkManager such as
     * automatic rescheduling of work following a crash and recovery from the application being
     * force-stopped by the user or device.
     *
     * @see WorkManager.initialize
     */
    interface Provider {
        /**
         * The [Configuration] used to initialize WorkManager
         */
        val workManagerConfiguration: Configuration
    }

    companion object {
        /**
         * The minimum number of system requests which can be enqueued by [WorkManager]
         * when using [android.app.job.JobScheduler] or [android.app.AlarmManager].
         */
        const val MIN_SCHEDULER_LIMIT = 20
    }
}

internal val DEFAULT_CONTENT_URI_TRIGGERS_WORKERS_LIMIT = 8

private fun createDefaultExecutor(isTaskExecutor: Boolean): Executor {
    val factory = object : ThreadFactory {
        private val threadCount = AtomicInteger(0)
        override fun newThread(runnable: Runnable): Thread {
            // Thread names are constrained to a max of 15 characters by the Linux Kernel.
            val prefix = if (isTaskExecutor) "WM.task-" else "androidx.work-"
            return Thread(runnable, "$prefix${threadCount.incrementAndGet()}")
        }
    }
    return Executors.newFixedThreadPool(
        // This value is the same as the core pool size for AsyncTask#THREAD_POOL_EXECUTOR.
        max(2, min(Runtime.getRuntime().availableProcessors() - 1, 4)),
        factory
    )
}
