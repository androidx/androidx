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

import android.annotation.SuppressLint
import android.os.Build
import android.ranging.RangingDevice
import android.ranging.RangingManager
import android.ranging.RangingPreference
import android.ranging.raw.RawInitiatorRangingConfig
import android.ranging.raw.RawRangingDevice
import android.ranging.raw.RawResponderRangingConfig
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.uwb.RangingCapabilities
import androidx.core.uwb.RangingControleeParameters
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControllerSessionScope

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class UwbControllerSessionScopeRangingImpl(
    rangingManager: RangingManager,
    rangingCapabilities: RangingCapabilities,
    localAddress: UwbAddress,
    override val uwbComplexChannel: UwbComplexChannel,
) :
    UwbClientSessionScopeRangingImpl(rangingManager, rangingCapabilities, localAddress),
    UwbControllerSessionScope {
    companion object {
        private const val TAG = "UwbControllerSessionScopeRangingImpl"
    }

    private val mConfigBuilderController: RawInitiatorRangingConfig.Builder =
        RawInitiatorRangingConfig.Builder()

    init {
        Log.d(TAG, "Uwb Controller session Scope created")
    }

    override fun buildRangingPreference(parameters: RangingParameters): RangingPreference {
        for (peer in parameters.peerDevices) {
            val rangingDevice = RangingDevice.Builder().build()
            val rawRangingDeviceObject =
                RawRangingDevice.Builder()
                    .setRangingDevice(rangingDevice)
                    .setUwbRangingParams(buildUwbRangingParams(peer.address))
                    .build()

            mConfigBuilderController.addRawRangingDevice(rawRangingDeviceObject)
            mAddressDeviceMap[peer.address] = rawRangingDeviceObject.rangingDevice
        }

        return RangingPreference.Builder(
                RangingPreference.DEVICE_ROLE_INITIATOR,
                mConfigBuilderController.build(),
            )
            .setSessionConfig(buildSessionConfig())
            .build()
    }

    @SuppressLint("MissingPermission")
    override suspend fun addControlee(address: UwbAddress) {
        if (mRangingSession == null) {
            throw IllegalStateException("Ranging session not started")
        }

        val configBuilderControlee = RawResponderRangingConfig.Builder()
        val rawRangingDeviceObject =
            RawRangingDevice.Builder()
                .setRangingDevice(RangingDevice.Builder().build())
                .setUwbRangingParams(buildUwbRangingParams(address))
                .build()
        configBuilderControlee.setRawRangingDevice(rawRangingDeviceObject)
        mAddressDeviceMap[address] = rawRangingDeviceObject.rangingDevice
        mRangingSession?.addDeviceToRangingSession(configBuilderControlee.build())
    }

    @SuppressLint("MissingPermission")
    override suspend fun removeControlee(address: UwbAddress) {
        val rangingDevice = mAddressDeviceMap[address]
        if (rangingDevice == null) {
            throw IllegalStateException("UwbAddress not found")
        }
        if (mRangingSession == null) {
            throw IllegalStateException("Ranging Session not started")
        }
        mRangingSession?.removeDeviceFromRangingSession(rangingDevice)
        mAddressDeviceMap.remove(address)
    }

    @SuppressLint("MissingPermission")
    override suspend fun reconfigureRangingInterval(intervalSkipCount: Int) {
        if (mRangingSession == null) {
            throw IllegalStateException("Ranging session not started")
        }

        Log.d(TAG, "reconfigureRangingInterval(intervalSkipCount: $intervalSkipCount) called")
        mRangingSession?.reconfigureRangingInterval(intervalSkipCount)
    }

    @SuppressLint("MissingPermission")
    override suspend fun addControlee(address: UwbAddress, parameters: RangingControleeParameters) {
        if (mRangingSession == null) {
            throw IllegalStateException("Ranging session not started")
        }

        val configBuilderControlee = RawResponderRangingConfig.Builder()
        val rawRangingDeviceObject =
            RawRangingDevice.Builder()
                .setRangingDevice(RangingDevice.Builder().build())
                .setUwbRangingParams(
                    buildUwbRangingParams(
                        address,
                        parameters.subSessionId,
                        parameters.subSessionKey,
                    ) // ->function in Client file, used to build uwbRangingParms
                )
                .build()

        configBuilderControlee.setRawRangingDevice(rawRangingDeviceObject)
        mAddressDeviceMap[address] = rawRangingDeviceObject.rangingDevice
        mRangingSession?.addDeviceToRangingSession(configBuilderControlee.build())
    }
}
