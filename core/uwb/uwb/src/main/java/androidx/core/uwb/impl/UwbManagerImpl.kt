/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.uwb.impl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingParameters.Companion.CONFIG_MULTICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_PROVISIONED_MULTICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_PROVISIONED_UNICAST_DS_TWR
import androidx.core.uwb.RangingParameters.Companion.CONFIG_UNICAST_DS_TWR
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbManager
import androidx.core.uwb.backend.IUwb
import androidx.core.uwb.exceptions.UwbServiceNotAvailableException
import androidx.core.uwb.helper.checkSystemFeature
import androidx.core.uwb.helper.handleApiException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import kotlinx.coroutines.tasks.await

internal class UwbManagerImpl(private val context: Context) : UwbManager {
    companion object {
        const val TAG = "UwbMangerImpl"
        val PUBLIC_AVAILABLE_CONFIG_IDS =
            setOf(
                CONFIG_UNICAST_DS_TWR,
                CONFIG_MULTICAST_DS_TWR,
                CONFIG_PROVISIONED_UNICAST_DS_TWR,
                CONFIG_PROVISIONED_MULTICAST_DS_TWR
            )
        var iUwb: IUwb? = null
    }

    init {
        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    iUwb = IUwb.Stub.asInterface(service)
                    Log.i(TAG, "iUwb service created successfully.")
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    iUwb = null
                }
            }
        val intent = Intent("androidx.core.uwb.backend.service")
        intent.setPackage("androidx.core.uwb.backend")
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    @Deprecated("Renamed to controleeSessionScope")
    override suspend fun clientSessionScope(): UwbClientSessionScope {
        return createClientSessionScope(false)
    }

    override suspend fun controleeSessionScope(): UwbControleeSessionScope {
        return createClientSessionScope(false) as UwbControleeSessionScope
    }

    override suspend fun controllerSessionScope(): UwbControllerSessionScope {
        return createClientSessionScope(true) as UwbControllerSessionScope
    }

    override suspend fun isAvailable(): Boolean {
        checkSystemFeature(context)
        return if (isGmsDevice()) Nearby.getUwbControllerClient(context).isAvailable.await()
        else {
            val client = iUwb?.controllerClient
            client?.isAvailable ?: false
        }
    }

    private fun isGmsDevice(): Boolean {
        val pm = context.packageManager
        val hasGmsCore =
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context, /* minApkVersion */ 230100000) ==
                ConnectionResult.SUCCESS
        val isChinaGcoreDevice =
            pm.hasSystemFeature("cn.google.services") &&
                pm.hasSystemFeature("com.google.android.feature.services_updater")
        return hasGmsCore && !isChinaGcoreDevice
    }

    private suspend fun createClientSessionScope(isController: Boolean): UwbClientSessionScope {
        checkSystemFeature(context)
        return if (isGmsDevice()) createGmsClientSessionScope(isController)
        else createAospClientSessionScope(isController)
    }

    private suspend fun createGmsClientSessionScope(isController: Boolean): UwbClientSessionScope {
        Log.i(TAG, "Creating Gms Client session scope")
        val uwbClient =
            if (isController) Nearby.getUwbControllerClient(context)
            else Nearby.getUwbControleeClient(context)
        if (!uwbClient.isAvailable().await()) {
            Log.e(TAG, "Uwb availability : false")
            throw UwbServiceNotAvailableException(
                "Cannot start a ranging session when UWB is " + "unavailable"
            )
        }
        try {
            val nearbyLocalAddress = uwbClient.localAddress.await()
            val nearbyRangingCapabilities = uwbClient.rangingCapabilities.await()
            val localAddress = UwbAddress(nearbyLocalAddress.address)
            val supportedConfigIds = nearbyRangingCapabilities.supportedConfigIds.toMutableList()
            supportedConfigIds.retainAll(PUBLIC_AVAILABLE_CONFIG_IDS)
            val rangingCapabilities =
                RangingCapabilities(
                    nearbyRangingCapabilities.supportsDistance(),
                    nearbyRangingCapabilities.supportsAzimuthalAngle(),
                    nearbyRangingCapabilities.supportsElevationAngle(),
                    nearbyRangingCapabilities.minRangingInterval,
                    nearbyRangingCapabilities.supportedChannels.toSet(),
                    nearbyRangingCapabilities.supportedNtfConfigs.toSet(),
                    supportedConfigIds.toSet(),
                    nearbyRangingCapabilities.supportedSlotDurations.toSet(),
                    nearbyRangingCapabilities.supportedRangingUpdateRates.toSet(),
                    nearbyRangingCapabilities.supportsRangingIntervalReconfigure(),
                    nearbyRangingCapabilities.hasBackgroundRangingSupport()
                )
            return if (isController) {
                val uwbComplexChannel = uwbClient.complexChannel.await()
                UwbControllerSessionScopeImpl(
                    uwbClient,
                    rangingCapabilities,
                    localAddress,
                    UwbComplexChannel(uwbComplexChannel.channel, uwbComplexChannel.preambleIndex)
                )
            } else {
                UwbControleeSessionScopeImpl(uwbClient, rangingCapabilities, localAddress)
            }
        } catch (e: ApiException) {
            handleApiException(e)
            throw RuntimeException(
                "Unexpected error. This indicates that the library is not " +
                    "up-to-date with the service backend."
            )
        }
    }

    private fun createAospClientSessionScope(isController: Boolean): UwbClientSessionScope {
        Log.i(TAG, "Creating Aosp Client session scope")
        val uwbClient = if (isController) iUwb?.controllerClient else iUwb?.controleeClient
        if (uwbClient == null) {
            Log.e(TAG, "Failed to get UwbClient. AOSP backend is not available.")
        }
        try {
            val aospLocalAddress = uwbClient!!.localAddress
            val aospRangingCapabilities = uwbClient.rangingCapabilities
            val localAddress = aospLocalAddress?.address?.let { UwbAddress(it) }
            val rangingCapabilities =
                aospRangingCapabilities?.let {
                    RangingCapabilities(
                        it.supportsDistance,
                        it.supportsAzimuthalAngle,
                        it.supportsElevationAngle,
                        it.minRangingInterval,
                        it.supportedChannels.toSet(),
                        it.supportedNtfConfigs.toSet(),
                        it.supportedConfigIds
                            .toMutableList()
                            .filter { it in PUBLIC_AVAILABLE_CONFIG_IDS }
                            .toSet(),
                        it.supportedSlotDurations.toSet(),
                        it.supportedRangingUpdateRates.toSet(),
                        it.supportsRangingIntervalReconfigure,
                        it.hasBackgroundRangingSupport
                    )
                }
            return if (isController) {
                val uwbComplexChannel = uwbClient.complexChannel
                UwbControllerSessionScopeAospImpl(
                    uwbClient,
                    rangingCapabilities!!,
                    localAddress!!,
                    UwbComplexChannel(uwbComplexChannel!!.channel, uwbComplexChannel.preambleIndex)
                )
            } else {
                UwbControleeSessionScopeAospImpl(uwbClient, rangingCapabilities!!, localAddress!!)
            }
        } catch (e: Exception) {
            throw e
        }
    }
}
