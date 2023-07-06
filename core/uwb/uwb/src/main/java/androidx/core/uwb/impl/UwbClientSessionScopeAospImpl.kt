/*
 * Copyright 2022 The Android Open Source Project
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

import android.util.Log
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.backend.IRangingSessionCallback
import androidx.core.uwb.backend.IUwbClient
import androidx.core.uwb.backend.RangingPosition
import androidx.core.uwb.backend.UwbComplexChannel
import androidx.core.uwb.backend.UwbDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal class UwbClientSessionScopeAospImpl(
    private val uwbClient: IUwbClient,
    override val rangingCapabilities: RangingCapabilities,
    override val localAddress: UwbAddress,
) : UwbControleeSessionScope {
    companion object {
        private const val TAG = "UwbClientSessionScope"
    }
    private var sessionStarted = false

    override fun prepareSession(parameters: RangingParameters) = callbackFlow {
        if (sessionStarted) {
            throw IllegalStateException("Ranging has already started. To initiate " +
                "a new ranging session, create a new client session scope.")
        }

        val parametersBuilder1 = androidx.core.uwb.backend.RangingParameters()
        parametersBuilder1.uwbConfigId = when (parameters.uwbConfigType) {
            RangingParameters.CONFIG_UNICAST_DS_TWR -> RangingParameters.CONFIG_UNICAST_DS_TWR
            RangingParameters.CONFIG_MULTICAST_DS_TWR -> RangingParameters.CONFIG_MULTICAST_DS_TWR
            RangingParameters.CONFIG_UNICAST_DS_TWR_NO_AOA ->
                RangingParameters.CONFIG_UNICAST_DS_TWR_NO_AOA
            RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR ->
                RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR
            RangingParameters.CONFIG_PROVISIONED_MULTICAST_DS_TWR ->
                RangingParameters.CONFIG_PROVISIONED_MULTICAST_DS_TWR
            RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR_NO_AOA ->
                RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR_NO_AOA
            RangingParameters.CONFIG_PROVISIONED_INDIVIDUAL_MULTICAST_DS_TWR ->
                RangingParameters.CONFIG_PROVISIONED_INDIVIDUAL_MULTICAST_DS_TWR
            else -> throw IllegalArgumentException("The selected UWB Config Id is not a valid id.")
        }
        parametersBuilder1.rangingUpdateRate = when (parameters.updateRateType) {
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC ->
                RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
            RangingParameters.RANGING_UPDATE_RATE_FREQUENT ->
                RangingParameters.RANGING_UPDATE_RATE_FREQUENT
            RangingParameters.RANGING_UPDATE_RATE_INFREQUENT ->
                RangingParameters.RANGING_UPDATE_RATE_INFREQUENT
            else -> throw IllegalArgumentException(
                "The selected ranging update rate is not a valid update rate.")
        }
        parametersBuilder1.sessionId = parameters.sessionId
        parametersBuilder1.sessionKeyInfo = parameters.sessionKeyInfo
        if (parameters.uwbConfigType
            == RangingParameters.CONFIG_PROVISIONED_INDIVIDUAL_MULTICAST_DS_TWR) {
            parametersBuilder1.subSessionId = parameters.subSessionId
            parametersBuilder1.subSessionKeyInfo = parameters.subSessionKeyInfo
        }
        if (parameters.complexChannel != null) {
            val channel = UwbComplexChannel()
            channel.channel = parameters.complexChannel.channel
            channel.preambleIndex = parameters.complexChannel.preambleIndex
            parametersBuilder1.complexChannel = channel
        }

        val peerList = ArrayList<UwbDevice>()
        for (peer in parameters.peerDevices) {
            val device = UwbDevice()
            val address = androidx.core.uwb.backend.UwbAddress()
            address.address = peer.address.address
            device.address = address
            peerList.add(device)
        }
        parametersBuilder1.peerDevices = peerList
        val callback =
            object : IRangingSessionCallback.Stub() {
                override fun onRangingInitialized(device: UwbDevice) {
                    Log.i(TAG, "Started UWB ranging.")
                }

                override fun onRangingResult(device: UwbDevice, position: RangingPosition) {
                    trySend(
                        RangingResult.RangingResultPosition(
                            androidx.core.uwb.UwbDevice(UwbAddress(device.address?.address!!)),
                            androidx.core.uwb.RangingPosition(
                                position.distance?.let { RangingMeasurement(it.value) },
                                position.azimuth?.let {
                                    RangingMeasurement(it.value)
                                },
                                position.elevation?.let {
                                    RangingMeasurement(it.value)
                                },
                                position.elapsedRealtimeNanos
                            )
                        )
                    )
                }

                override fun onRangingSuspended(device: UwbDevice, reason: Int) {
                    trySend(
                        RangingResult.RangingResultPeerDisconnected(
                            androidx.core.uwb.UwbDevice(UwbAddress(device.address?.address!!))
                        )
                    )
                }

                override fun getInterfaceVersion(): Int {
                    return 0
                }

                override fun getInterfaceHash(): String {
                    return ""
                }
            }

        try {
            uwbClient.startRanging(parametersBuilder1, callback)
            sessionStarted = true
        } catch (e: Exception) {
            throw(e)
        }

        awaitClose {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    uwbClient.stopRanging(callback)
                } catch (e: Exception) {
                    throw(e)
                }
            }
        }
    }
}
