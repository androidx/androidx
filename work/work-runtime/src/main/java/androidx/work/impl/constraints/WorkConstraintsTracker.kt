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
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.GuardedBy
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

private typealias OnConstraintState = (ConstraintsState) -> Unit

public sealed class ConstraintsState {
    public object ConstraintsMet : ConstraintsState()

    public data class ConstraintsNotMet(@StopReason val reason: Int) : ConstraintsState()
}

public fun WorkConstraintsTracker.listen(
    spec: WorkSpec,
    dispatcher: CoroutineDispatcher,
    listener: OnConstraintsStateChangedListener,
): Job {
    return CoroutineScope(dispatcher).launch {
        track(spec).collect { listener.onConstraintsStateChanged(spec, it) }
    }
}

public fun interface OnConstraintsStateChangedListener {
    public fun onConstraintsStateChanged(workSpec: WorkSpec, state: ConstraintsState)
}

public class WorkConstraintsTracker(private val controllers: List<ConstraintController>) {
    /** @param trackers Constraints trackers */
    public constructor(
        trackers: Trackers
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

    public fun track(spec: WorkSpec): Flow<ConstraintsState> {
        val flows = controllers.filter { it.hasConstraint(spec) }.map { it.track(spec.constraints) }
        return combine(flows) { states ->
                states.firstOrNull { it != ConstraintsMet } ?: ConstraintsMet
            }
            .distinctUntilChanged()
    }

    public fun areAllConstraintsMet(workSpec: WorkSpec): Boolean {
        val controllers = controllers.filter { it.isCurrentlyConstrained(workSpec) }

        if (controllers.isNotEmpty()) {
            Logger.get()
                .debug(
                    TAG,
                    "Work ${workSpec.id} constrained by " +
                        controllers.joinToString { it.javaClass.simpleName },
                )
        }
        return controllers.isEmpty()
    }
}

private val TAG = Logger.tagWithPrefix("WorkConstraintsTracker")

@RequiresApi(28)
public fun NetworkRequestConstraintController(
    context: Context
): NetworkRequestConstraintController {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return NetworkRequestConstraintController(manager)
}

private const val DefaultNetworkRequestTimeoutMs = 1000L

// Due to foreground workers constraint tracking we can't simply rely on JobScheduler to kick off
// workers. For APIs 28 and 29 the tracker is not shared because we rely on registerNetworkCallback
// with specific a NetworkRequest to get a signal that required Network is available. However, for
// APIs 30+ we rely on registerDefaultNetwork with a single shared tracker and check if network
// satisfies requirement via request.canBeSatisfiedBy(). Using a shared network callback avoids the
// TooManyRequestsException error that can occur when too many callbacks are registered
// (b/231499040).
@RequiresApi(28)
public class NetworkRequestConstraintController(
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
        val timeoutJob = launch {
            delay(timeoutMs)
            Logger.get()
                .debug(
                    TAG,
                    "NetworkRequestConstraintController didn't receive " +
                        "neither onCapabilitiesChanged/onLost callback, sending " +
                        "`ConstraintsNotMet` after $timeoutMs ms",
                )
            trySend(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
        }

        val onConstraintState: (ConstraintsState) -> Unit = {
            timeoutJob.cancel()
            trySend(it)
        }
        val tryUnregister =
            if (Build.VERSION.SDK_INT >= 30) {
                SharedNetworkCallback.addCallback(
                    connManager = connManager,
                    networkRequest = networkRequest,
                    onConstraintState = onConstraintState,
                )
            } else {
                IndividualNetworkCallback.addCallback(
                    connManager = connManager,
                    networkRequest = networkRequest,
                    onConstraintState = onConstraintState,
                )
            }
        awaitClose { tryUnregister() }
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

/**
 * A [ConnectivityManager.NetworkCallback] used to track network constraints for workers with
 * [NetworkRequest] on API 28-29.
 */
@RequiresApi(28)
private class IndividualNetworkCallback
private constructor(private val onConstraintState: OnConstraintState) :
    ConnectivityManager.NetworkCallback() {
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        Logger.get().debug(TAG, "NetworkRequestConstraintController onCapabilitiesChanged callback")
        onConstraintState(ConstraintsMet)
    }

    override fun onLost(network: Network) {
        Logger.get().debug(TAG, "NetworkRequestConstraintController onLost callback")
        onConstraintState(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
    }

    companion object {
        fun addCallback(
            connManager: ConnectivityManager,
            networkRequest: NetworkRequest,
            onConstraintState: OnConstraintState,
        ): () -> Unit {
            val networkCallback = IndividualNetworkCallback(onConstraintState)
            var callbackRegistered = false
            try {
                Logger.get().debug(TAG, "NetworkRequestConstraintController register callback")
                connManager.registerNetworkCallback(networkRequest, networkCallback)
                callbackRegistered = true
            } catch (ex: RuntimeException) {
                // Catch TooManyRequestsException since there is an app limit of 100 registered
                // callbacks. Since the limit is shared with other libraries and app code, we try to
                // avoid crashing. The constraint will timeout and the work won't be done
                // opportunistically but will eventually execute through its Job.
                if (ex.javaClass.name.endsWith("TooManyRequestsException")) {
                    Logger.get()
                        .debug(
                            TAG,
                            "NetworkRequestConstraintController couldn't register callback",
                            ex,
                        )
                    onConstraintState(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY))
                } else {
                    throw ex
                }
            }
            return {
                if (callbackRegistered) {
                    Logger.get()
                        .debug(TAG, "NetworkRequestConstraintController unregister callback")
                    connManager.unregisterNetworkCallback(networkCallback)
                }
            }
        }
    }
}

/**
 * A singleton [ConnectivityManager.NetworkCallback] used to track network constraints for all
 * workers with [NetworkRequest] on API 30+. Using a shared callback avoids the app hitting the app
 * limit of registered callbacks (b/231499040).
 */
@RequiresApi(30)
private object SharedNetworkCallback : ConnectivityManager.NetworkCallback() {

    private val requestsLock = Any()
    @GuardedBy("requestsLock")
    private val requests = mutableMapOf<OnConstraintState, NetworkRequest>()

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        Logger.get().debug(TAG, "NetworkRequestConstraintController onCapabilitiesChanged callback")
        synchronized(requestsLock) { requests.entries.toList() }
            .forEach { (onConstraintState, request) ->
                onConstraintState(
                    if (request.canBeSatisfiedBy(networkCapabilities)) {
                        ConstraintsMet
                    } else {
                        ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY)
                    }
                )
            }
    }

    override fun onLost(network: Network) {
        Logger.get().debug(TAG, "NetworkRequestConstraintController onLost callback")
        synchronized(requestsLock) { requests.keys.toList() }
            .forEach { it(ConstraintsNotMet(STOP_REASON_CONSTRAINT_CONNECTIVITY)) }
    }

    fun addCallback(
        connManager: ConnectivityManager,
        networkRequest: NetworkRequest,
        onConstraintState: OnConstraintState,
    ): () -> Unit {
        synchronized(requestsLock) {
            val registerCallback = requests.isEmpty()
            requests.put(onConstraintState, networkRequest)
            if (registerCallback) {
                Logger.get()
                    .debug(TAG, "NetworkRequestConstraintController register shared callback")
                connManager.registerDefaultNetworkCallback(this)
            }
        }
        return {
            synchronized(requestsLock) {
                requests.remove(onConstraintState)
                if (requests.isEmpty()) {
                    Logger.get()
                        .debug(TAG, "NetworkRequestConstraintController unregister shared callback")
                    connManager.unregisterNetworkCallback(this)
                }
            }
        }
    }
}
