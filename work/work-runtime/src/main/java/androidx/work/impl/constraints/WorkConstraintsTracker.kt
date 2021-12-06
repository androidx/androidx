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
package androidx.work.impl.constraints

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Logger
import androidx.work.impl.constraints.controllers.BatteryChargingController
import androidx.work.impl.constraints.controllers.BatteryNotLowController
import androidx.work.impl.constraints.controllers.ConstraintController
import androidx.work.impl.constraints.controllers.NetworkConnectedController
import androidx.work.impl.constraints.controllers.NetworkMeteredController
import androidx.work.impl.constraints.controllers.NetworkNotRoamingController
import androidx.work.impl.constraints.controllers.NetworkUnmeteredController
import androidx.work.impl.constraints.controllers.StorageNotLowController
import androidx.work.impl.model.WorkSpec
import androidx.work.impl.utils.taskexecutor.TaskExecutor

interface WorkConstraintsTracker {
    /**
     * Replaces the list of tracked [WorkSpec]s to monitor if their constraints are met.
     *
     * @param workSpecs A list of [WorkSpec]s to monitor constraints for
     */
    fun replace(workSpecs: Iterable<WorkSpec>)

    /**
     * Resets and clears all tracked [WorkSpec]s.
     */
    fun reset()
}

/**
 * Tracks [WorkSpec]s and their [androidx.work.Constraints], and notifies an optional
 * [WorkConstraintsCallback] when all of their constraints are met or not met.
 */
class WorkConstraintsTrackerImpl @VisibleForTesting internal constructor(
    private val callback: WorkConstraintsCallback?,
    private val constraintControllers: Array<ConstraintController<*>>,
) : WorkConstraintsTracker, ConstraintController.OnConstraintUpdatedCallback {
    // We need to keep hold a lock here for the cases where there is 1 WCT tracking a list of
    // WorkSpecs. Changes in constraints are notified on the main thread. Enqueues / Cancellations
    // occur on the task executor thread pool. So there is a chance of
    // ConcurrentModificationExceptions.
    private val lock: Any = Any()

    /**
     * @param context      The application [Context]
     * @param taskExecutor The [TaskExecutor] being used by WorkManager.
     * @param callback     The callback is only necessary when you need
     * [WorkConstraintsTrackerImpl] to notify you about changes in
     * constraints for the list of [WorkSpec]'s that it is tracking.
     */
    constructor(
        context: Context,
        taskExecutor: TaskExecutor,
        callback: WorkConstraintsCallback?
    ) : this(
        callback,
        arrayOf(
            BatteryChargingController(context.applicationContext, taskExecutor),
            BatteryNotLowController(context.applicationContext, taskExecutor),
            StorageNotLowController(context.applicationContext, taskExecutor),
            NetworkConnectedController(context.applicationContext, taskExecutor),
            NetworkUnmeteredController(context.applicationContext, taskExecutor),
            NetworkNotRoamingController(context.applicationContext, taskExecutor),
            NetworkMeteredController(context.applicationContext, taskExecutor)
        )
    )

    /**
     * Replaces the list of tracked [WorkSpec]s to monitor if their constraints are met.
     *
     * @param workSpecs A list of [WorkSpec]s to monitor constraints for
     */
    override fun replace(workSpecs: Iterable<WorkSpec>) {
        synchronized(lock) {
            for (controller in constraintControllers) {
                controller.setCallback(null)
            }
            for (controller in constraintControllers) {
                controller.replace(workSpecs)
            }
            for (controller in constraintControllers) {
                controller.setCallback(this)
            }
        }
    }

    /**
     * Resets and clears all tracked [WorkSpec]s.
     */
    override fun reset() {
        synchronized(lock) {
            for (controller in constraintControllers) {
                controller.reset()
            }
        }
    }

    /**
     * Returns `true` if all the underlying constraints for a given WorkSpec are met.
     *
     * @param workSpecId The [WorkSpec] id
     * @return `true` if all the underlying constraints for a given [WorkSpec] are
     * met.
     */
    fun areAllConstraintsMet(workSpecId: String): Boolean {
        synchronized(lock) {
            val controller = constraintControllers.firstOrNull {
                it.isWorkSpecConstrained(workSpecId)
            }
            if (controller != null) {
                Logger.get().debug(
                    TAG, "Work $workSpecId constrained by ${controller.javaClass.simpleName}"
                )
            }
            return controller == null
        }
    }

    override fun onConstraintMet(workSpecIds: List<String>) {
        synchronized(lock) {
            val unconstrainedWorkSpecIds = workSpecIds.filter { areAllConstraintsMet(it) }
            unconstrainedWorkSpecIds.forEach {
                Logger.get().debug(TAG, "Constraints met for $it")
            }
            callback?.onAllConstraintsMet(unconstrainedWorkSpecIds)
        }
    }

    override fun onConstraintNotMet(workSpecIds: List<String>) {
        synchronized(lock) { callback?.onAllConstraintsNotMet(workSpecIds) }
    }
}

private val TAG = Logger.tagWithPrefix("WorkConstraintsTracker")
