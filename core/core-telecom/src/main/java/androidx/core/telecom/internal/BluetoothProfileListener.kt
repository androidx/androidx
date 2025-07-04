/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.core.telecom.internal

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.internal.utils.EndpointUtils

/**
 * This class is responsible for getting [BluetoothProfile]s from the [BluetoothManager] pre-call
 * and emitting them to the [PreCallEndpointsUpdater] as [androidx.core.telecom.CallEndpointCompat]s
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class BluetoothProfileListener(
    context: Context,
    private val mPreCallEndpointsUpdater: PreCallEndpointsUpdater,
    private val mUuidSessionId: Int,
) : BluetoothProfile.ServiceListener, AutoCloseable {
    /** Constants used for this class */
    companion object {
        private val TAG: String = BluetoothProfileListener::class.java.simpleName
        private val BLUETOOTH_PROFILES =
            listOf(
                BluetoothProfile.HEADSET,
                BluetoothProfile.LE_AUDIO,
                BluetoothProfile.HEARING_AID,
            )
    }

    /** Managers used for this class */
    private val mBluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val mBluetoothAdapter: BluetoothAdapter? = mBluetoothManager?.adapter

    /**
     * Internal class tracking. Need to track the endpoints so that when a profile is disconnected,
     * the client can be informed. Also, for cleanup purposes, the proxy should be tracked to close
     * when the pre-call endpoints API job is cancelled.
     */
    private data class ProfileData(
        val endpoints: MutableList<CallEndpointCompat>?,
        val proxy: BluetoothProfile?,
    )

    private val mProfileToData: HashMap<Int, ProfileData> = HashMap()

    /** On class init, get the proxies in order for the profile service connection to establish */
    init {
        getBluetoothProfileProxies(context)
    }

    /** When the listener is destroyed, close the proxies that were connected */
    override fun close() {
        Log.i(TAG, "close: uuidSessionId=[$mUuidSessionId]")
        closeBluetoothProfileProxies()
    }

    private fun getBluetoothProfileProxies(context: Context) {
        BLUETOOTH_PROFILES.forEach { profile ->
            runCatching {
                    val hasProxy = mBluetoothAdapter?.getProfileProxy(context, this, profile)
                    Log.d(TAG, "gBPP: btProfile=[$profile] isConnect=[$hasProxy]")
                }
                .onFailure { e ->
                    Log.e(TAG, "gBPP: hit exception while getting bluetooth profile=[$profile]", e)
                }
        }
    }

    private fun closeBluetoothProfileProxies() {
        mProfileToData.entries
            .filter { (_, profileData) -> profileData.proxy != null }
            .forEach { (profile, profileData) ->
                runCatching { mBluetoothAdapter?.closeProfileProxy(profile, profileData.proxy) }
                    .onFailure { e ->
                        Log.e(
                            TAG,
                            "cBPP: hit exception when closing proxy for profile=[$profile]",
                            e,
                        )
                    }
            }
    }

    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
        Log.i(TAG, "onServiceConnected: profile=[$profile], proxy=[$proxy]")
        val endpoints: MutableList<CallEndpointCompat> = ArrayList()
        if (proxy != null) {
            for (device in proxy.connectedDevices) {
                endpoints.add(makeEndpoint(device))
            }
        }
        // populate internal map for profile
        mProfileToData[profile] = ProfileData(endpoints.toMutableList(), proxy)
        // update client
        mPreCallEndpointsUpdater.endpointsAddedUpdate(endpoints)
    }

    override fun onServiceDisconnected(profile: Int) {
        Log.i(TAG, "onServiceDisconnected: profile=[$profile]")
        val endpointsToRemove = mProfileToData[profile]?.endpoints ?: mutableListOf()
        // clear internal map for profile
        mProfileToData[profile] = ProfileData(mutableListOf(), null)
        // update the client
        mPreCallEndpointsUpdater.endpointsRemovedUpdate(endpointsToRemove.toList())
    }

    /**
     * ============================================================================================
     * Helpers
     * ============================================================================================
     */
    private fun getBluetoothDeviceName(device: BluetoothDevice): String {
        var name: String = EndpointUtils.BLUETOOTH_DEVICE_DEFAULT_NAME
        try {
            name = device.name
        } catch (e: SecurityException) {
            Log.e(TAG, "getBluetoothDeviceName: hit SecurityException while getting device name", e)
        }
        return name
    }

    private fun getBluetoothDeviceAddress(device: BluetoothDevice): String {
        var address: String = CallEndpointCompat.UNKNOWN_MAC_ADDRESS
        try {
            address = device.address
        } catch (e: Exception) {
            Log.e(TAG, "getBluetoothDeviceAddress: hit exception while getting device address", e)
        }
        return address
    }

    private fun makeEndpoint(device: BluetoothDevice): CallEndpointCompat {
        val bluetoothDeviceName = getBluetoothDeviceName(device)
        val uuidForBluetoothDevice =
            CallEndpointUuidTracker.getUuid(
                mUuidSessionId,
                CallEndpointCompat.TYPE_BLUETOOTH,
                bluetoothDeviceName,
            )
        val callEndpoint =
            CallEndpointCompat(
                bluetoothDeviceName,
                CallEndpointCompat.TYPE_BLUETOOTH,
                uuidForBluetoothDevice,
            )
        callEndpoint.mMackAddress = getBluetoothDeviceAddress(device)
        return callEndpoint
    }
}
