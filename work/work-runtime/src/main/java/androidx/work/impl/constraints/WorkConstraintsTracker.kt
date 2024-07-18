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

import androidx.work.Logger
import androidx.work.StopReason
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class ConstraintsState {
    object ConstraintsMet : ConstraintsState()
    data class ConstraintsNotMet(
        @StopReason
        val reason: Int
    ) : ConstraintsState()
}

fun WorkConstraintsTracker.listen(
    spec: WorkSpec,
    dispatcher: CoroutineDispatcher,
    listener: OnConstraintsStateChangedListener
): Job {
    val job = Job()
    CoroutineScope(dispatcher + job).launch {
        track(spec).collect { listener.onConstraintsStateChanged(spec, it) }
    }
    return job
}

fun interface OnConstraintsStateChangedListener {
    fun onConstraintsStateChanged(workSpec: WorkSpec, state: ConstraintsState)
}

class WorkConstraintsTracker(
    private val controllers: List<ConstraintController<*>>
) {
    /**
     * @param trackers Constraints trackers
     */
    constructor(
        trackers: Trackers,
    ) : this(
        listOf(
            BatteryChargingController(trackers.batteryChargingTracker),
            BatteryNotLowController(trackers.batteryNotLowTracker),
            StorageNotLowController(trackers.storageNotLowTracker),
            NetworkConnectedController(trackers.networkStateTracker),
            NetworkUnmeteredController(trackers.networkStateTracker),
            NetworkNotRoamingController(trackers.networkStateTracker),
            NetworkMeteredController(trackers.networkStateTracker)
        )
    )

    fun track(spec: WorkSpec): Flow<ConstraintsState> {
        val flows = controllers.filter { it.hasConstraint(spec) }.map { it.track() }
        return combine(flows) { states ->
            states.firstOrNull { it != ConstraintsMet } ?: ConstraintsMet
        }.distinctUntilChanged()
    }

    fun areAllConstraintsMet(workSpec: WorkSpec): Boolean {
        val controllers = controllers.filter { it.isConstrained(workSpec) }

        if (controllers.isNotEmpty()) {
            Logger.get().debug(
                TAG, "Work ${workSpec.id} constrained by " +
                    controllers.joinToString { it.javaClass.simpleName }
            )
        }
        return controllers.isEmpty()
    }
}

private val TAG = Logger.tagWithPrefix("WorkConstraintsTracker")
