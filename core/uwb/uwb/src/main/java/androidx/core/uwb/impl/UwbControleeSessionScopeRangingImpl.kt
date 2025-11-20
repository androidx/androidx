/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Build
import android.ranging.RangingManager
import android.ranging.RangingPreference
import android.ranging.raw.RawResponderRangingConfig
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbControleeSessionScope

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class UwbControleeSessionScopeRangingImpl(
    rangingManager: RangingManager,
    rangingCapabilities: RangingCapabilities,
    localAddress: UwbAddress,
) :
    UwbClientSessionScopeRangingImpl(rangingManager, rangingCapabilities, localAddress),
    UwbControleeSessionScope {
    companion object {
        private const val TAG = "UwbControleeSessionScopeRangingImpl"
    }

    init {
        Log.v(TAG, "Controlee Session scope created")
    }

    override fun buildRangingPreference(parameters: RangingParameters): RangingPreference {

        if (parameters.peerDevices.size != 1) {
            throw IllegalArgumentException("Responder must have exactly one peer.")
        }
        val peer = parameters.peerDevices[0]
        val configBuilderControlee = RawResponderRangingConfig.Builder()
        val rawRangingDeviceObject =
            android.ranging.raw.RawRangingDevice.Builder()
                .setRangingDevice(android.ranging.RangingDevice.Builder().build())
                .setUwbRangingParams(buildUwbRangingParams(peer.address))
                .build()

        configBuilderControlee.setRawRangingDevice(rawRangingDeviceObject)
        mAddressDeviceMap[peer.address] = rawRangingDeviceObject.rangingDevice
        return RangingPreference.Builder(
                RangingPreference.DEVICE_ROLE_RESPONDER,
                configBuilderControlee.build(),
            )
            .setSessionConfig(buildSessionConfig())
            .build()
    }
}
