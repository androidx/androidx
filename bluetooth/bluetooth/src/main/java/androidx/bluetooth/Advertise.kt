/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AdvertiseImpl {
    fun advertise(advertiseParams: AdvertiseParams): Flow<@BluetoothLe.AdvertiseResult Int>
}

@SuppressLint("ObsoleteSdkInt")
internal fun getAdvertiseImpl(bleAdvertiser: BluetoothLeAdvertiser): AdvertiseImpl {
    return if (Build.VERSION.SDK_INT >= 26) AdvertiseImplApi26(bleAdvertiser)
    else AdvertiseImplBase(bleAdvertiser)
}

private open class AdvertiseImplBase(val bleAdvertiser: BluetoothLeAdvertiser) : AdvertiseImpl {

    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    override fun advertise(advertiseParams: AdvertiseParams) = callbackFlow {
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                trySend(BluetoothLe.ADVERTISE_STARTED)
            }

            override fun onStartFailure(errorCode: Int) {
                when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_DATA_TOO_LARGE)

                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)

                    ADVERTISE_FAILED_INTERNAL_ERROR ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_INTERNAL_ERROR)

                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                }
            }
        }

        bleAdvertiser.startAdvertising(
            advertiseParams.fwkAdvertiseSettings, advertiseParams.fwkAdvertiseData, callback
        )

        if (advertiseParams.duration.toMillis() > 0) {
            delay(advertiseParams.duration.toMillis())
            close()
        }

        awaitClose {
            bleAdvertiser.stopAdvertising(callback)
        }
    }
}

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(26)
private class AdvertiseImplApi26(
    bleAdvertiser: BluetoothLeAdvertiser
) : AdvertiseImplBase(bleAdvertiser) {

    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    override fun advertise(advertiseParams: AdvertiseParams) = callbackFlow {
        val callback = object : AdvertisingSetCallback() {
            override fun onAdvertisingSetStarted(
                advertisingSet: AdvertisingSet?,
                txPower: Int,
                status: Int
            ) {
                when (status) {
                    ADVERTISE_SUCCESS ->
                        trySend(BluetoothLe.ADVERTISE_STARTED)

                    ADVERTISE_FAILED_DATA_TOO_LARGE ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_DATA_TOO_LARGE)

                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)

                    ADVERTISE_FAILED_INTERNAL_ERROR ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_INTERNAL_ERROR)

                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
                        trySend(BluetoothLe.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
                }
            }

            override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                close()
            }

            override fun onAdvertisingEnabled(
                advertisingSet: AdvertisingSet?,
                enable: Boolean,
                status: Int
            ) {
                if (!enable) close()
            }
        }

        bleAdvertiser.startAdvertisingSet(
            advertiseParams.fwkAdvertiseSetParams,
            advertiseParams.fwkAdvertiseData,
            /*scanResponse=*/null,
            /*periodicParameters=*/null,
            /*periodicData=*/null,
            // round up
            (advertiseParams.duration.toMillis().toInt() + 9) / 10,
            /*maxExtendedAdvertisingEvents=*/0,
            callback
        )

        if (advertiseParams.duration.toMillis() > 0) {
            delay(advertiseParams.duration.toMillis())
            close()
        }

        awaitClose {
            bleAdvertiser.stopAdvertisingSet(callback)
        }
    }
}
