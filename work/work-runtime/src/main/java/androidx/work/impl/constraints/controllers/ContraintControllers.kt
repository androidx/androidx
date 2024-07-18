/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.impl.constraints.controllers

import android.os.Build
import androidx.work.Logger
import androidx.work.NetworkType
import androidx.work.NetworkType.TEMPORARILY_UNMETERED
import androidx.work.NetworkType.UNMETERED
import androidx.work.StopReason
import androidx.work.WorkInfo
import androidx.work.impl.constraints.ConstraintListener
import androidx.work.impl.constraints.ConstraintsState
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
import androidx.work.impl.constraints.ConstraintsState.ConstraintsNotMet
import androidx.work.impl.constraints.NetworkState
import androidx.work.impl.constraints.trackers.BatteryNotLowTracker
import androidx.work.impl.constraints.trackers.ConstraintTracker
import androidx.work.impl.model.WorkSpec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

abstract class ConstraintController<T>(
    private val tracker: ConstraintTracker<T>
) {
    @StopReason
    abstract val reason: Int
    abstract fun hasConstraint(workSpec: WorkSpec): Boolean
    abstract fun isConstrained(value: T): Boolean

    fun track(): Flow<ConstraintsState> = callbackFlow {
        val listener = object : ConstraintListener<T> {
            override fun onConstraintChanged(newValue: T) {
                val value = if (isConstrained(newValue))
                    ConstraintsNotMet(reason) else ConstraintsMet
                channel.trySend(value)
            }
        }
        tracker.addListener(listener)
        awaitClose {
            tracker.removeListener(listener)
        }
    }

    fun isConstrained(workSpec: WorkSpec): Boolean {
        return hasConstraint(workSpec) && isConstrained(tracker.readSystemState())
    }
}

/**
 * A [ConstraintController] for battery charging events.
 */
class BatteryChargingController(tracker: ConstraintTracker<Boolean>) :
    ConstraintController<Boolean>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_CHARGING
    override fun hasConstraint(workSpec: WorkSpec) = workSpec.constraints.requiresCharging()

    override fun isConstrained(value: Boolean) = !value
}

/**
 * A [ConstraintController] for battery not low events.
 */
class BatteryNotLowController(tracker: BatteryNotLowTracker) :
    ConstraintController<Boolean>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW
    override fun hasConstraint(workSpec: WorkSpec) = workSpec.constraints.requiresBatteryNotLow()

    override fun isConstrained(value: Boolean) = !value
}

/**
 * A [ConstraintController] for monitoring that the network connection is unmetered.
 */
class NetworkUnmeteredController(tracker: ConstraintTracker<NetworkState>) :
    ConstraintController<NetworkState>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY
    override fun hasConstraint(workSpec: WorkSpec): Boolean {
        val requiredNetworkType = workSpec.constraints.requiredNetworkType
        return requiredNetworkType == UNMETERED ||
            (Build.VERSION.SDK_INT >= 30 && requiredNetworkType == TEMPORARILY_UNMETERED)
    }

    override fun isConstrained(value: NetworkState) = !value.isConnected || value.isMetered
}

/**
 * A [ConstraintController] for storage not low events.
 */
class StorageNotLowController(tracker: ConstraintTracker<Boolean>) :
    ConstraintController<Boolean>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW
    override fun hasConstraint(workSpec: WorkSpec) = workSpec.constraints.requiresStorageNotLow()

    override fun isConstrained(value: Boolean) = !value
}

/**
 * A [ConstraintController] for monitoring that the network connection is not roaming.
 */
class NetworkNotRoamingController(tracker: ConstraintTracker<NetworkState>) :
    ConstraintController<NetworkState>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY
    override fun hasConstraint(workSpec: WorkSpec): Boolean {
        return workSpec.constraints.requiredNetworkType == NetworkType.NOT_ROAMING
    }

    /**
     * Check for not-roaming constraint on API 24+, when JobInfo#NETWORK_TYPE_NOT_ROAMING was added,
     * to be consistent with JobScheduler functionality.
     */
    override fun isConstrained(value: NetworkState): Boolean {
        return if (Build.VERSION.SDK_INT < 24) {
            Logger.get().debug(
                TAG, "Not-roaming network constraint is not supported before API 24, " +
                    "only checking for connected state."
            )
            !value.isConnected
        } else !value.isConnected || !value.isNotRoaming
    }

    companion object {
        private val TAG = Logger.tagWithPrefix("NetworkNotRoamingCtrlr")
    }
}

/**
 * A [ConstraintController] for monitoring that any usable network connection is available.
 *
 *
 * For API 26 and above, usable means that the [NetworkState] is validated, i.e.
 * it has a working internet connection.
 *
 *
 * For API 25 and below, usable simply means that [NetworkState] is connected.
 */
class NetworkConnectedController(tracker: ConstraintTracker<NetworkState>) :
    ConstraintController<NetworkState>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY
    override fun hasConstraint(workSpec: WorkSpec) =
        workSpec.constraints.requiredNetworkType == NetworkType.CONNECTED

    override fun isConstrained(value: NetworkState) =
        if (Build.VERSION.SDK_INT >= 26) {
            !value.isConnected || !value.isValidated
        } else {
            !value.isConnected
        }
}

/**
 * A [ConstraintController] for monitoring that the network connection is metered.
 */
class NetworkMeteredController(tracker: ConstraintTracker<NetworkState>) :
    ConstraintController<NetworkState>(tracker) {
    override val reason = WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY
    override fun hasConstraint(workSpec: WorkSpec) =
        workSpec.constraints.requiredNetworkType == NetworkType.METERED

    /**
     * Check for metered constraint on API 26+, when JobInfo#NETWORK_METERED was added, to
     * be consistent with JobScheduler functionality.
     */
    override fun isConstrained(value: NetworkState): Boolean {
        return if (Build.VERSION.SDK_INT < 26) {
            Logger.get().debug(
                TAG, "Metered network constraint is not supported before API 26, " +
                    "only checking for connected state."
            )
            !value.isConnected
        } else !value.isConnected || !value.isMetered
    }

    companion object {
        private val TAG = Logger.tagWithPrefix("NetworkMeteredCtrlr")
    }
}
