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
import androidx.work.impl.constraints.trackers.Trackers
import androidx.work.impl.model.WorkSpec

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
     * @param trackers Constraints trackers
     * @param callback     The callback is only necessary when you need
     * [WorkConstraintsTrackerImpl] to notify you about changes in
     * constraints for the list of [WorkSpec]'s that it is tracking.
     */
    constructor(
        trackers: Trackers,
        callback: WorkConstraintsCallback?
    ) : this(
        callback,
        arrayOf(
            BatteryChargingController(trackers.batteryChargingTracker),
            BatteryNotLowController(trackers.batteryNotLowTracker),
            StorageNotLowController(trackers.storageNotLowTracker),
            NetworkConnectedController(trackers.networkStateTracker),
            NetworkUnmeteredController(trackers.networkStateTracker),
            NetworkNotRoamingController(trackers.networkStateTracker),
            NetworkMeteredController(trackers.networkStateTracker)
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
                controller.callback = null
            }
            for (controller in constraintControllers) {
                controller.replace(workSpecs)
            }
            for (controller in constraintControllers) {
                controller.callback = this
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

    override fun onConstraintMet(workSpecs: List<WorkSpec>) {
        synchronized(lock) {
            val unconstrainedWorkSpecIds = workSpecs.filter { areAllConstraintsMet(it.id) }
            unconstrainedWorkSpecIds.forEach {
                Logger.get().debug(TAG, "Constraints met for $it")
            }
            callback?.onAllConstraintsMet(unconstrainedWorkSpecIds)
        }
    }

    override fun onConstraintNotMet(workSpecs: List<WorkSpec>) {
        synchronized(lock) { callback?.onAllConstraintsNotMet(workSpecs) }
    }
}

private val TAG = Logger.tagWithPrefix("WorkConstraintsTracker")
