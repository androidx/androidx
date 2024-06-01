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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Constraints
import androidx.work.Logger
import androidx.work.StopReason
import androidx.work.WorkInfo.Companion.STOP_REASON_CONSTRAINT_CONNECTIVITY
import androidx.work.impl.constraints.ConstraintsState.ConstraintsMet
import androidx.work.impl.constraints.ConstraintsState.ConstraintsNotMet
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

sealed class ConstraintsState {
    object ConstraintsMet : ConstraintsState()

    data class ConstraintsNotMet(@StopReason val reason: Int) : ConstraintsState()
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

class WorkConstraintsTracker(private val controllers: List<ConstraintController>) {
    /** @param trackers Constraints trackers */
    constructor(
        trackers: Trackers,
    ) : this(
        listOfNotNull(
            BatteryChargingController(trackers.batteryChargingTracker),
            BatteryNotLowController(trackers.batteryNotLowTracker),
            StorageNotLowController(trackers.storageNotLowTracker),
            NetworkConnectedController(trackers.networkStateTracker),
            NetworkUnmeteredController(trackers.networkStateTracker),
            NetworkNotRoamingController(trackers.networkStateTracker),
            NetworkMeteredController(trackers.networkStateTracker),
            if (Build.VERSION.SDK_INT >= 28) NetworkRequestConstraintController(trackers.context)
            else null,
        )
    )

    fun track(spec: WorkSpec): Flow<ConstraintsState> {
        val flows = controllers.filter { it.hasConstraint(spec) }.map { it.track(spec.constraints) }
        return combine(flows) { states ->
                states.firstOrNull { it != ConstraintsMet } ?: ConstraintsMet
            }
            .distinctUntilChanged()
    }

    fun areAllConstraintsMet(workSpec: WorkSpec): Boolean {
        val controllers = controllers.filter { it.isCurrentlyConstrained(workSpec) }

        if (controllers.isNotEmpty()) {
            Logger.get()
                .debug(
                    TAG,
                    "Work ${workSpec.id} constrained by " +
                        controllers.joinToString { it.javaClass.simpleName }
                )
        }
        return controllers.isEmpty()
    }
}

private val TAG = Logger.tagWithPrefix("WorkConstraintsTracker")

@RequiresApi(28)
fun NetworkRequestConstraintController(context: Context): NetworkRequestConstraintController {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return NetworkRequestConstraintController(manager)
}

private val DefaultNetworkRequestTimeoutMs = 1000L

// So we don't have a tracker that is shared, because we rely on
// registerNetworkCallback with specific NetworkRequest to get a signal that
// required Network is available. Alternatively we could have used a tracker with
// registerDefaultNetwork and check if network satisfies requirement via
// `request.canBeSatisfiedBy()`. However this method available only since API level 30,
// that would significantly limit the feature availability. While we can simply rely on JobScheduler
// to kick off the workers on API level 28-30, we also need to track constraint for
// foreground workers, thus we need still controller on levels 28-30.
@RequiresApi(28)
class NetworkRequestConstraintController(
    private val connManager: ConnectivityManager,
    private val timeoutMs: Long = DefaultNetworkRequestTimeoutMs,
) : ConstraintController {
    override fun track(constraints: Constraints): Flow<ConstraintsState> = callbackFlow {
        val networkRequest = constraints.requiredNetworkRequest
        if (networkRequest == null) {
            channel.close()
            return@callbackFlow
        }
        // we don't want immediately send ConstraintsNotMet, because it will immediately
        // stop the work in case foreground worker, even though network could be present
        // However, we need to send it eventually, because otherwise we won't stop foreground
        // worker at all, if there is no available network.
        val job = launch {
            delay(timeoutMs)
            Logger.get()
                .debug(
                    TAG,
                    "NetworkRequestConstraintController didn't receive " +
                        "neither  onCapabilitiesChanged/onLost callback, sending " +
                        "`ConstraintsNotMet` after $timeoutMs ms"
                )
            trySend(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
        }

        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    job.cancel()
                    Logger.get()
                        .debug(
                            TAG,
                            "NetworkRequestConstraintController onCapabilitiesChanged callback"
                        )
                    trySend(ConstraintsMet)
                }

                override fun onLost(network: Network) {
                    job.cancel()
                    Logger.get().debug(TAG, "NetworkRequestConstraintController onLost callback")
                    trySend(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
                }
            }
        Logger.get().debug(TAG, "NetworkRequestConstraintController register callback")
        connManager.registerNetworkCallback(networkRequest, networkCallback)
        awaitClose {
            Logger.get().debug(TAG, "NetworkRequestConstraintController unregister callback")
            connManager.unregisterNetworkCallback(networkCallback)
        }
    }

    override fun hasConstraint(workSpec: WorkSpec): Boolean =
        workSpec.constraints.requiredNetworkRequest != null

    override fun isCurrentlyConstrained(workSpec: WorkSpec): Boolean {
        // It happens because ConstraintTrackingWorker can still run on API level 28
        // after OS upgrade, because we're wrapping workers as ConstraintTrackingWorker at
        // the enqueue time instead of execution time.
        // However, ConstraintTrackingWorker won't have requiredNetworkRequest set
        // because they were enqueued on APIs 23..25, in this case we don't throw.
        if (!hasConstraint(workSpec)) return false
        throw IllegalStateException(
            "isCurrentlyConstrained() must never be called on" +
                "NetworkRequestConstraintController. isCurrentlyConstrained() is called only " +
                "on older platforms where NetworkRequest isn't supported"
        )
    }
}
