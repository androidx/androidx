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

package androidx.core.uwb.rxjava3.mock

import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbDevice.Companion.createForAddress
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.uwb.RangingPosition
import com.google.android.gms.nearby.uwb.RangingSessionCallback
import com.google.android.gms.nearby.uwb.UwbDevice
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** A default implementation of [UwbClientSessionScope] used for testing. */
class TestUwbClientSessionScope(
    private val uwbClient: TestUwbClient,
    override val rangingCapabilities: RangingCapabilities,
    override val localAddress: UwbAddress
) : UwbClientSessionScope {
    private var sessionStarted = false
    private val uwbDevice = createForAddress(ByteArray(0))
    val defaultRangingParameters = RangingParameters(
        RangingParameters.CONFIG_UNICAST_DS_TWR,
        0,
        0,
        null,
        null,
        null,
        ImmutableList.of(uwbDevice),
        RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC
    )

    override fun prepareSession(parameters: RangingParameters) = callbackFlow {
        if (sessionStarted) {
            throw IllegalStateException(
                "Ranging has already started. To initiate " +
                    "a new ranging session, create a new client session scope."
            )
        }

        val configId = com.google.android.gms.nearby.uwb.RangingParameters.UwbConfigId.CONFIG_ID_1
        val updateRate =
            com.google.android.gms.nearby.uwb.RangingParameters.RangingUpdateRate.AUTOMATIC
        val parametersBuilder = com.google.android.gms.nearby.uwb.RangingParameters.Builder()
            .setSessionId(defaultRangingParameters.sessionId)
            .setUwbConfigId(configId)
            .setRangingUpdateRate(updateRate)
        parametersBuilder.addPeerDevice(UwbDevice.createForAddress(uwbDevice.address.address))
        val callback =
            object : RangingSessionCallback {
                var rangingInitialized = false
                override fun onRangingInitialized(device: UwbDevice) {
                    rangingInitialized = true
                }

                override fun onRangingResult(device: UwbDevice, position: RangingPosition) {
                    trySend(
                        RangingResult.RangingResultPosition(
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
                        RangingResult.RangingResultPeerDisconnected(
                            androidx.core.uwb.UwbDevice(UwbAddress(device.address.address))
                        )
                    )
                }
            }

        try {
            uwbClient.startRanging(parametersBuilder.build(), callback)
            sessionStarted = true
        } catch (e: ApiException) {
            // do nothing
        }

        awaitClose {
            try {
                uwbClient.stopRanging(callback)
            } catch (e: ApiException) {
                // do nothing
            }
        }
    }
}
