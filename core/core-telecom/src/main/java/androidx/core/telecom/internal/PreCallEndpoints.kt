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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallEndpointCompat
import kotlinx.coroutines.channels.SendChannel

@RequiresApi(Build.VERSION_CODES.O)
internal class PreCallEndpoints(
    var mCurrentDevices: MutableList<CallEndpointCompat>,
    var mSendChannel: SendChannel<List<CallEndpointCompat>>
) {
    // earpiece, speaker, unknown, wired_headset
    val mNonBluetoothEndpoints: HashMap<Int, CallEndpointCompat> = HashMap()

    // all bt endpoints
    val mBluetoothEndpoints: HashMap<String, CallEndpointCompat> = HashMap()

    companion object {
        private val TAG: String = PreCallEndpoints::class.java.simpleName.toString()

        // endpoints added constants
        const val ALREADY_TRACKING_ENDPOINT: Int = 0
        const val START_TRACKING_NEW_ENDPOINT: Int = 1

        // endpoints removed constants
        const val NOT_TRACKING_REMOVED_ENDPOINT: Int = 0
        const val STOP_TRACKING_REMOVED_ENDPOINT: Int = 1
    }

    init {
        for (device in mCurrentDevices) {
            if (device.isBluetoothType()) {
                mBluetoothEndpoints[device.name.toString()] = device
            } else {
                mNonBluetoothEndpoints[device.type] = device
            }
        }
    }

    fun endpointsAddedUpdate(addedCallEndpoints: List<CallEndpointCompat>) {
        var addedDevicesCount = 0
        for (maybeNewEndpoint in addedCallEndpoints) {
            addedDevicesCount += maybeAddCallEndpoint(maybeNewEndpoint)
        }
        if (addedDevicesCount > 0) {
            updateClient()
        } else {
            Log.d(TAG, "endpointsAddedUpdate: no new added endpoints, not updating client!")
        }
    }

    fun endpointsRemovedUpdate(removedCallEndpoints: List<CallEndpointCompat>) {
        var removedDevicesCount = 0
        for (maybeRemovedDevice in removedCallEndpoints) {
            removedDevicesCount += maybeRemoveCallEndpoint(maybeRemovedDevice)
        }
        if (removedDevicesCount > 0) {
            mCurrentDevices =
                (mBluetoothEndpoints.values + mNonBluetoothEndpoints.values).toMutableList()
            updateClient()
        } else {
            Log.d(TAG, "endpointsRemovedUpdate: no removed endpoints, not updating client!")
        }
    }

    internal fun isCallEndpointBeingTracked(endpoint: CallEndpointCompat?): Boolean {
        return mCurrentDevices.contains(endpoint)
    }

    @VisibleForTesting
    internal fun maybeAddCallEndpoint(endpoint: CallEndpointCompat): Int {
        if (endpoint.isBluetoothType()) {
            if (!mBluetoothEndpoints.containsKey(endpoint.name.toString())) {
                mBluetoothEndpoints[endpoint.name.toString()] = endpoint
                mCurrentDevices.add(endpoint)
                return START_TRACKING_NEW_ENDPOINT
            } else {
                return ALREADY_TRACKING_ENDPOINT
            }
        } else {
            if (!mNonBluetoothEndpoints.containsKey(endpoint.type)) {
                mNonBluetoothEndpoints[endpoint.type] = endpoint
                mCurrentDevices.add(endpoint)
                return START_TRACKING_NEW_ENDPOINT
            } else {
                return ALREADY_TRACKING_ENDPOINT
            }
        }
    }

    @VisibleForTesting
    internal fun maybeRemoveCallEndpoint(endpoint: CallEndpointCompat): Int {
        if (endpoint.isBluetoothType()) {
            if (mBluetoothEndpoints.containsKey(endpoint.name.toString())) {
                mBluetoothEndpoints.remove(endpoint.name.toString())
                return STOP_TRACKING_REMOVED_ENDPOINT
            } else {
                return NOT_TRACKING_REMOVED_ENDPOINT
            }
        } else {
            if (mNonBluetoothEndpoints.containsKey(endpoint.type)) {
                mNonBluetoothEndpoints.remove(endpoint.type)
                return STOP_TRACKING_REMOVED_ENDPOINT
            } else {
                return NOT_TRACKING_REMOVED_ENDPOINT
            }
        }
    }

    private fun updateClient() {
        mSendChannel.trySend(mCurrentDevices)
    }
}
