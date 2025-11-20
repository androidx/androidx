/*
 * Copyright 2017 The Android Open Source Project
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
package androidx.work.impl.constraints.trackers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED
import android.net.NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.net.ConnectivityManagerCompat
import androidx.work.Logger
import androidx.work.impl.constraints.NetworkState
import androidx.work.impl.utils.registerDefaultNetworkCallbackCompat
import androidx.work.impl.utils.taskexecutor.TaskExecutor

/**
 * A [ConstraintTracker] for monitoring network state.
 *
 * For API 24 and up: Network state is tracked using a registered [NetworkCallback] with
 * [ConnectivityManager.registerDefaultNetworkCallback], added in API 24.
 *
 * For API 23 and below: Network state is tracked using a [android.content.BroadcastReceiver]. Much
 * less efficient than tracking with [NetworkCallback]s and [ConnectivityManager].
 *
 * Based on [android.app.job.JobScheduler]'s ConnectivityController on API 26. {@see
 * https://android.googlesource.com/platform/frameworks/base/+/oreo-release/services/core/java/com/android/server/job/controllers/ConnectivityController.java}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun NetworkStateTracker(
    context: Context,
    taskExecutor: TaskExecutor,
): ConstraintTracker<NetworkState> {
    // Based on requiring ConnectivityManager#registerDefaultNetworkCallback - added in API 24.
    return if (Build.VERSION.SDK_INT >= 24) {
        NetworkStateTracker24(context, taskExecutor)
    } else {
        NetworkStateTrackerPre24(context, taskExecutor)
    }
}

private val TAG = Logger.tagWithPrefix("NetworkStateTracker")

internal val ConnectivityManager.isActiveNetworkValidated: Boolean
    get() =
        try {
            val network = activeNetwork
            val capabilities = getNetworkCapabilities(network)
            (capabilities?.hasCapability(NET_CAPABILITY_VALIDATED)) ?: false
        } catch (exception: SecurityException) {
            // b/163342798
            Logger.get().error(TAG, "Unable to validate active network", exception)
            false
        }

@Suppress("DEPRECATION")
internal val ConnectivityManager.activeNetworkState: NetworkState
    get() {
        try {
            // Use getActiveNetworkInfo() instead of getNetworkInfo(network) because it can detect
            // VPNs.
            val info = activeNetworkInfo
            val isConnected = info != null && info.isConnected
            val isValidated = isActiveNetworkValidated
            val isMetered = ConnectivityManagerCompat.isActiveNetworkMetered(this)
            val isNotRoaming = info != null && !info.isRoaming
            return NetworkState(isConnected, isValidated, isMetered, isNotRoaming)
        } catch (exception: SecurityException) {
            // b/406629536 and b/163342798
            Logger.get().error(TAG, "Unable to get active network state", exception)
            return NetworkState(
                isConnected = false,
                isValidated = false,
                isMetered = false,
                isNotRoaming = true,
            )
        }
    }

@get:RequiresApi(28)
internal val NetworkCapabilities.activeNetworkState: NetworkState
    get() {
        val isConnected = hasCapability(NET_CAPABILITY_INTERNET)
        val isValidated = hasCapability(NET_CAPABILITY_VALIDATED)
        val isMetered = !hasCapability(NET_CAPABILITY_NOT_METERED) // API 28 only
        val isNotRoaming = hasCapability(NET_CAPABILITY_NOT_ROAMING) // API 28 only
        return NetworkState(isConnected, isValidated, isMetered, isNotRoaming)
    }

internal class NetworkStateTrackerPre24(context: Context, taskExecutor: TaskExecutor) :
    BroadcastReceiverConstraintTracker<NetworkState>(context, taskExecutor) {

    private val connectivityManager: ConnectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun onBroadcastReceive(intent: Intent) {
        @Suppress("DEPRECATION")
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            Logger.get().debug(TAG, "Network broadcast received")
            state = connectivityManager.activeNetworkState
        }
    }

    @Suppress("DEPRECATION")
    override val intentFilter: IntentFilter
        get() = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

    override fun readSystemState(): NetworkState = connectivityManager.activeNetworkState
}

@RequiresApi(24)
internal class NetworkStateTracker24(context: Context, taskExecutor: TaskExecutor) :
    ConstraintTracker<NetworkState>(context, taskExecutor) {

    private val connectivityManager: ConnectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun readSystemState(): NetworkState = connectivityManager.activeNetworkState

    private val networkCallback =
        object : NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                // The Network parameter is unreliable when a VPN app is running - use active
                // network.
                Logger.get().debug(TAG, "Network capabilities changed: $capabilities")
                state =
                    if (Build.VERSION.SDK_INT >= 28) {
                        // Get the active network state from the capabilities itself.
                        // b/323479909
                        capabilities.activeNetworkState
                    } else {
                        connectivityManager.activeNetworkState
                    }
            }

            override fun onLost(network: Network) {
                Logger.get().debug(TAG, "Network connection lost")
                state = connectivityManager.activeNetworkState
            }
        }

    override fun startTracking() {
        try {
            Logger.get().debug(TAG, "Registering network callback")
            connectivityManager.registerDefaultNetworkCallbackCompat(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Catching the exceptions since and moving on - this tracker is only used for
            // GreedyScheduler and there is nothing to be done about device-specific bugs.
            // IllegalStateException: Happening on NVIDIA Shield K1 Tablets.  See b/136569342.
            // SecurityException: Happening on Solone W1450.  See b/153246136.
            Logger.get().error(TAG, "Received exception while registering network callback", e)
        } catch (e: SecurityException) {
            Logger.get().error(TAG, "Received exception while registering network callback", e)
        }
    }

    override fun stopTracking() {
        try {
            Logger.get().debug(TAG, "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            // Catching the exceptions since and moving on - this tracker is only used for
            // GreedyScheduler and there is nothing to be done about device-specific bugs.
            // IllegalStateException: Happening on NVIDIA Shield K1 Tablets.  See b/136569342.
            // SecurityException: Happening on Solone W1450.  See b/153246136.
            Logger.get().error(TAG, "Received exception while unregistering network callback", e)
        } catch (e: SecurityException) {
            Logger.get().error(TAG, "Received exception while unregistering network callback", e)
        }
    }
}
