/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.work.datatransfer

import android.content.Context
import java.util.UUID

/**
 * The base class for specifying parameters for network based data transfer work that should be
 * enqueued in DataTransferTaskManager.
 */
class UserInitiatedTaskRequest constructor(
    private val task: Class<out UserInitiatedTask>,
    /**
     * [FallbackPolicy] indicating what the library should do on Android 14- devices.
     */
    private val fallbackPolicy: FallbackPolicy = FallbackPolicy.FALLBACK_NONE,
    /**
     * [Constraints] required for this task to run.
     * The default value assumes a requirement of any internet.
     */
    private val _constraints: Constraints = Constraints(),
    /**
     * Sets the appropriate estimated upload/download byte info of the data transfer request
     * via the [TransferInfo] object.
     */
    private val _transferInfo: TransferInfo? = null,
    /**
     * A list of tags associated to this work. You can query and cancel work by tags.
     * Tags are particularly useful for modules or libraries to find and operate on their own work.
     */
    private val _tags: MutableList<String> = mutableListOf()
) {
    /**
     * The unique identifier associated with this unit of work.
     */
    private val id: UUID = UUID.randomUUID()
    val stringId: String
        get() = id.toString()

    val constraints: Constraints
        get() = _constraints

    val transferInfo: TransferInfo?
        get() = _transferInfo

    val tags: List<String>
        get() = _tags

    /**
     * The foreground service which will be used as a fallback solution on Android 14- devices.
     * This is only used if [FallbackPolicy.FALLBACK_TO_FOREGROUND_SERVICE] is set.
     */
    var service: Class<out AbstractUitService>? = null

    /**
     * [ForegroundServiceOnTaskFinishPolicy] indicating what should occur when the task is finished.
     */
    var onTaskFinishPolicy: ForegroundServiceOnTaskFinishPolicy =
        ForegroundServiceOnTaskFinishPolicy.FOREGROUND_SERVICE_STOP_FOREGROUND

    init {
        // Update the list of tags to include the UserInitiatedTask class name if available
        _tags += task.name
    }

    /**
     * Set the [AbstractUitService] service to fallback to on Android 14- devices along with
     * a [ForegroundServiceOnTaskFinishPolicy] policy which defines what will happen when the
     * task is finished.
     *
     * This is only used if [FallbackPolicy.FALLBACK_TO_FOREGROUND_SERVICE] is set. If this method
     * is not called and [FallbackPolicy.FALLBACK_TO_FOREGROUND_SERVICE] is set, an exception will
     * be thrown when the task is enqueued.
     *
     * Upon scheduling the task request, the library will call [Context.startForegroundService] with
     * [ACTION_UIT_SCHEDULE] on the given service here.
     * The app needs to call [android.app.Service.startForeground] within a certain amount of time,
     * otherwise it will crash with a [android.app.ForegroundServiceDidNotStartInTimeException].
     */
    fun setForegroundService(
        service: Class<out AbstractUitService>,
        onTaskFinishPolicy: ForegroundServiceOnTaskFinishPolicy =
            ForegroundServiceOnTaskFinishPolicy.FOREGROUND_SERVICE_STOP_FOREGROUND
    ) {
        this.service = service
        this.onTaskFinishPolicy = onTaskFinishPolicy
    }

    internal fun getTaskState(): TaskState {
        return TaskState.TASK_STATE_INVALID // TODO: update impl
    }

    suspend fun enqueue(@Suppress("UNUSED_PARAMETER") context: Context) {
        if (this.fallbackPolicy == FallbackPolicy.FALLBACK_TO_FOREGROUND_SERVICE &&
            this.service == null) {
            throw IllegalArgumentException("FallbackPolicy.FALLBACK_TO_FOREGROUND_SERVICE is set," +
                " but a foreground service has not been set via setForegroundService().")
        }
        // TODO: update impl
    }

    suspend fun cancel() {
        // TODO: update impl
    }

    companion object {
        const val ACTION_UIT_SCHEDULE =
            "androidx.work.datatransfer.UserInitiatedTaskRequest.SCHEDULE"
    }

    enum class FallbackPolicy {
        /**
         * Indicates to the library that it should not do anything on Android 14- devices. The
         * developer will perform the data transfer task on previous versions of Android with their
         * own logic.
         *
         * **This is the default policy.**
         */
        FALLBACK_NONE,

        /**
         * Indicates that the developer will provide an implementation of [AbstractUitService] which
         * will be used by the library to perform the data transfer work on Android 14- devices.
         *
         * _The foreground service fallback will act as a best effort to perform the data transfer
         * work on Android 14- devices._
         */
        FALLBACK_TO_FOREGROUND_SERVICE,
    }

    enum class ForegroundServiceOnTaskFinishPolicy {
        /**
         * This indicates that the foreground service should be stopped when the job is done.
         * This is the default behavior.
         */
        FOREGROUND_SERVICE_STOP_FOREGROUND,

        /**
         * This indicates that the foreground service should be left as is when the job is done
         * and the app will manage its lifecycle.
         */
        FOREGROUND_SERVICE_DETACH,
    }

    /**
     * The internal definition of the task states.
     */
    internal enum class TaskState {
        /**
         * Not a valid state.
         */
        TASK_STATE_INVALID,

        /**
         * The task has been scheduled but hasn't been put into execution, it may be waiting
         * for the constraints.
         * Or, it used to be running, but the constraint are no longer met, so the task was stopped.
         */
        TASK_STATE_SCHEDULED,

        /**
         * The task is being executed.
         */
        TASK_STATE_EXECUTING,

        /**
         * The task has finished.
         */
        TASK_STATE_FINISHED,
    }
}
