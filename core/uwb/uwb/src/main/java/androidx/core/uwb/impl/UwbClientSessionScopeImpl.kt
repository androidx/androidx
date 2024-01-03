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

import android.util.Log
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult.RangingResultPeerDisconnected
import androidx.core.uwb.RangingResult.RangingResultPosition
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.helper.handleApiException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.uwb.RangingPosition
import com.google.android.gms.nearby.uwb.RangingSessionCallback
import com.google.android.gms.nearby.uwb.UwbClient
import com.google.android.gms.nearby.uwb.UwbComplexChannel
import com.google.android.gms.nearby.uwb.UwbDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

internal class UwbClientSessionScopeImpl(
    private val uwbClient: UwbClient,
    override val rangingCapabilities: RangingCapabilities,
    override val localAddress: UwbAddress
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

        val configId = when (parameters.uwbConfigType) {
            RangingParameters.CONFIG_UNICAST_DS_TWR ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_1
            RangingParameters.CONFIG_MULTICAST_DS_TWR ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_2
            RangingParameters.CONFIG_UNICAST_DS_TWR_NO_AOA ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_3
            RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_4
            RangingParameters.CONFIG_PROVISIONED_MULTICAST_DS_TWR ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_5
            RangingParameters.CONFIG_PROVISIONED_UNICAST_DS_TWR_NO_AOA ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_6
            RangingParameters.CONFIG_PROVISIONED_INDIVIDUAL_MULTICAST_DS_TWR ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_7
            else ->
                throw IllegalArgumentException("The selected UWB Config Id is not a valid id.")
        }
        val updateRate = when (parameters.updateRateType) {
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.AUTOMATIC
            RangingParameters.RANGING_UPDATE_RATE_FREQUENT ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.FREQUENT
            RangingParameters.RANGING_UPDATE_RATE_INFREQUENT ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.INFREQUENT
            else ->
                throw IllegalArgumentException("The selected ranging update rate is not a valid" +
                    " update rate.")
        }
        val parametersBuilder = com.google.android.gms.nearby.uwb.RangingParameters.Builder()
            .setSessionId(parameters.sessionId)
            .setUwbConfigId(configId)
            .setRangingUpdateRate(updateRate)
            .setComplexChannel(
                parameters.complexChannel?.let {
                    UwbComplexChannel.Builder()
                        .setChannel(it.channel)
                        .setPreambleIndex(it.preambleIndex)
                        .build()
                })
        if (parameters.sessionKeyInfo != null) {
            parametersBuilder.setSessionKeyInfo(parameters.sessionKeyInfo)
        }
        if (configId == com.google.android.gms.nearby.uwb
            .RangingParameters.UwbConfigId.CONFIG_ID_7) {
            parametersBuilder.setSubSessionId(parameters.subSessionId)
            parametersBuilder.setSubSessionKeyInfo(parameters.subSessionKeyInfo)
        }
        for (peer in parameters.peerDevices) {
            parametersBuilder.addPeerDevice(UwbDevice.createForAddress(peer.address.address))
        }
        val callback =
            object : RangingSessionCallback {
                override fun onRangingInitialized(device: UwbDevice) {
                    Log.i(TAG, "Started UWB ranging.")
                }

                override fun onRangingResult(device: UwbDevice, position: RangingPosition) {
                    trySend(
                        RangingResultPosition(
                            androidx.core.uwb.UwbDevice(UwbAddress(device.address.address)),
                            androidx.core.uwb.RangingPosition(
                                RangingMeasurement(position.distance.value),
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
                        RangingResultPeerDisconnected(
                            androidx.core.uwb.UwbDevice(UwbAddress(device.address.address))
                        )
                    )
                }
            }

        try {
            uwbClient.startRanging(parametersBuilder.build(), callback).await()
            sessionStarted = true
        } catch (e: ApiException) {
            handleApiException(e)
        }

        awaitClose {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                try {
                    uwbClient.stopRanging(callback).await()
                } catch (e: ApiException) {
                    handleApiException(e)
                }
            }
        }
    }
}
