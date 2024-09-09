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

package androidx.core.telecom.test

import android.bluetooth.BluetoothDevice
import android.net.Uri
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.Call.Details
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.InCallService
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/** Ensure compatibility for APIs back to API level 29 */
object Compatibility {
    @JvmStatic
    fun getContactDisplayName(details: Details): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.getContactDisplayName(details)
        } else {
            details.callerDisplayName
        }
    }

    @JvmStatic
    fun getContactPhotoUri(details: Details): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.getContactDisplayUri(details)
        } else {
            null
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun getCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Impl.getCallState(call)
        } else {
            call.state
        }
    }

    @JvmStatic
    fun getBluetoothDeviceAlias(device: BluetoothDevice): Result<String?> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.getBluetoothDeviceAlias(device)
        } else {
            Result.success(null)
        }
    }

    @JvmStatic
    fun getEndpointIdentifier(endpoint: CallEndpoint): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.getEndpointIdentifier(endpoint)
        } else {
            null
        }
    }

    @JvmStatic
    fun getEndpointName(endpoint: CallEndpoint): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.getEndpointName(endpoint).toString()
        } else {
            null
        }
    }

    @JvmStatic
    fun getEndpointType(endpoint: CallEndpoint): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.getEndpointType(endpoint)
        } else {
            null
        }
    }

    @JvmStatic
    fun requestCallEndpointChange(
        service: InCallService,
        endpoint: CallEndpoint,
        executor: Executor,
        callback: OutcomeReceiver<Void, CallEndpointException>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Api34Impl.requestCallEndpointChange(service, endpoint, executor, callback)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
object Api30Impl {
    @JvmStatic
    fun getContactDisplayName(details: Details): String? {
        return details.contactDisplayName
    }

    @JvmStatic
    fun getBluetoothDeviceAlias(device: BluetoothDevice): Result<String?> {
        return try {
            Result.success(device.alias)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }
}

/** Ensure compatibility for [Call] APIs for API level 31+ */
@RequiresApi(Build.VERSION_CODES.S)
object Api31Impl {
    @JvmStatic
    fun getCallState(call: Call): Int {
        return call.details.state
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
object Api34Impl {

    @JvmStatic
    fun getContactDisplayUri(details: Details): Uri? {
        return details.contactPhotoUri
    }

    @JvmStatic
    fun getEndpointIdentifier(endpoint: CallEndpoint): String {
        return endpoint.identifier.toString()
    }

    @JvmStatic
    fun getEndpointName(endpoint: CallEndpoint): CharSequence {
        return endpoint.endpointName
    }

    @JvmStatic
    fun getEndpointType(endpoint: CallEndpoint): Int {
        return endpoint.endpointType
    }

    @JvmStatic
    fun requestCallEndpointChange(
        service: InCallService,
        endpoint: CallEndpoint,
        executor: Executor,
        callback: OutcomeReceiver<Void, CallEndpointException>
    ) {
        service.requestCallEndpointChange(endpoint, executor, callback)
    }
}
