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
import androidx.core.uwb.RangingResult.RangingResultPosition
import androidx.core.uwb.RangingResult.RangingResultPeerDisconnected
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControleeSessionScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.uwb.RangingPosition
import com.google.android.gms.nearby.uwb.RangingSessionCallback
import com.google.android.gms.nearby.uwb.UwbClient
import com.google.android.gms.nearby.uwb.UwbComplexChannel
import com.google.android.gms.nearby.uwb.UwbDevice
import kotlinx.coroutines.flow.callbackFlow
import androidx.core.uwb.helper.handleApiException
import kotlinx.coroutines.channels.awaitClose

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
            RangingParameters.UWB_CONFIG_ID_1 ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_1
            RangingParameters.UWB_CONFIG_ID_3 ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_3
            else ->
                com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.UNKNOWN
        }
        val updateRate = when (parameters.updateRateType) {
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.AUTOMATIC
            RangingParameters.RANGING_UPDATE_RATE_FREQUENT ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.FREQUENT
            RangingParameters.RANGING_UPDATE_RATE_INFREQUENT ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.INFREQUENT
            else ->
                com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.UNKNOWN
        }
        val parametersBuilder = com.google.android.gms.nearby.uwb.RangingParameters.Builder()
            .setSessionId(parameters.sessionId)
            .setUwbConfigId(configId)
            .setRangingUpdateRate(updateRate)
            .setSessionKeyInfo(parameters.sessionKeyInfo)
            .setUwbConfigId(parameters.uwbConfigType)
            .setComplexChannel(
                parameters.complexChannel?.let {
                    UwbComplexChannel.Builder()
                        .setChannel(it.channel)
                        .setPreambleIndex(it.preambleIndex)
                        .build()
                })
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
            uwbClient.startRanging(parametersBuilder.build(), callback)
            sessionStarted = true
        } catch (e: ApiException) {
            handleApiException(e)
        }

        awaitClose {
            try {
                uwbClient.stopRanging(callback)
            } catch (e: ApiException) {
                handleApiException(e)
            }
        }
    }
}