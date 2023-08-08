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

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.ApiKey
import com.google.android.gms.nearby.uwb.RangingCapabilities
import com.google.android.gms.nearby.uwb.RangingMeasurement
import com.google.android.gms.nearby.uwb.RangingParameters
import com.google.android.gms.nearby.uwb.RangingPosition
import com.google.android.gms.nearby.uwb.RangingSessionCallback
import com.google.android.gms.nearby.uwb.UwbAddress
import com.google.android.gms.nearby.uwb.UwbClient
import com.google.android.gms.nearby.uwb.UwbComplexChannel
import com.google.android.gms.nearby.uwb.UwbDevice
import com.google.android.gms.nearby.uwb.UwbStatusCodes
import com.google.android.gms.nearby.uwb.zze
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

/** A default implementation of [UwbClient] used in testing. */
public class TestUwbClient(
    val complexChannel: UwbComplexChannel,
    val localAddress: UwbAddress,
    val rangingCapabilities: RangingCapabilities,
    val isAvailable: Boolean
) : UwbClient {
    var stopRangingCalled = false
        private set
    private lateinit var callback: RangingSessionCallback
    private var startedRanging = false
    companion object {
        val rangingPosition = RangingPosition(
            RangingMeasurement(1, 1.0F), null, null, 20, -50)
    }
    override fun getApiKey(): ApiKey<zze> {
        TODO("Not yet implemented")
    }

    override fun addControlee(p0: UwbAddress): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun getComplexChannel(): Task<UwbComplexChannel> {
        return Tasks.forResult(complexChannel)
    }

    override fun getLocalAddress(): Task<UwbAddress> {
        return Tasks.forResult(localAddress)
    }

    override fun getRangingCapabilities(): Task<RangingCapabilities> {
        return Tasks.forResult(rangingCapabilities)
    }

    override fun isAvailable(): Task<Boolean> {
        return Tasks.forResult(isAvailable)
    }

    override fun removeControlee(p0: UwbAddress): Task<Void> {
        TODO("Not yet implemented")
    }

    override fun startRanging(
        parameters: RangingParameters,
        sessionCallback: RangingSessionCallback
    ): Task<Void> {
        if (startedRanging) {
            throw ApiException(Status(UwbStatusCodes.RANGING_ALREADY_STARTED))
        }
        callback = sessionCallback
        val peer = parameters.peerDevices.first()
        callback.onRangingResult(peer, rangingPosition)
        startedRanging = true
        return Tasks.forResult(null)
    }

    override fun stopRanging(callback: RangingSessionCallback): Task<Void> {
        if (stopRangingCalled) {
            throw RuntimeException("Stop Ranging has already been called.")
        }
        stopRangingCalled = true
        return Tasks.forResult(null)
    }

    fun disconnectPeer(device: UwbDevice) {
        callback.onRangingSuspended(device, 0)
    }
}
