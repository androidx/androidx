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

import android.bluetooth.le.AdvertiseCallback as FwkAdvertiseCallback
import android.bluetooth.le.AdvertiseSettings as FwkAdvertiseSettings
import android.bluetooth.le.AdvertisingSet as FwkAdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback as FwkAdvertisingSetCallback
import android.bluetooth.le.BluetoothLeAdvertiser as FwkBluetoothLeAdvertiser
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

internal fun getAdvertiseImpl(bleAdvertiser: FwkBluetoothLeAdvertiser): AdvertiseImpl {
    return if (Build.VERSION.SDK_INT >= 26) AdvertiseImplApi26(bleAdvertiser)
    else AdvertiseImplBase(bleAdvertiser)
}

private open class AdvertiseImplBase(val bleAdvertiser: FwkBluetoothLeAdvertiser) : AdvertiseImpl {

    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    override fun advertise(advertiseParams: AdvertiseParams) = callbackFlow {
        val callback =
            object : FwkAdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: FwkAdvertiseSettings) {
                    trySend(BluetoothLe.ADVERTISE_STARTED)
                }

                override fun onStartFailure(errorCode: Int) {
                    close(AdvertiseException(errorCode))
                }
            }

        bleAdvertiser.startAdvertising(
            advertiseParams.fwkAdvertiseSettings,
            advertiseParams.fwkAdvertiseData,
            callback
        )

        if (advertiseParams.durationMillis > 0) {
            delay(advertiseParams.durationMillis)
            close()
        }

        awaitClose { bleAdvertiser.stopAdvertising(callback) }
    }
}

@RequiresApi(26)
private class AdvertiseImplApi26(bleAdvertiser: FwkBluetoothLeAdvertiser) :
    AdvertiseImplBase(bleAdvertiser) {

    @RequiresPermission("android.permission.BLUETOOTH_ADVERTISE")
    override fun advertise(advertiseParams: AdvertiseParams) = callbackFlow {
        val callback =
            object : FwkAdvertisingSetCallback() {
                override fun onAdvertisingSetStarted(
                    advertisingSet: FwkAdvertisingSet?,
                    txPower: Int,
                    status: Int
                ) {
                    if (status == ADVERTISE_SUCCESS) {
                        trySend(BluetoothLe.ADVERTISE_STARTED)
                    } else {
                        close(AdvertiseException(status))
                    }
                }

                override fun onAdvertisingSetStopped(advertisingSet: FwkAdvertisingSet?) {
                    close()
                }

                override fun onAdvertisingEnabled(
                    advertisingSet: FwkAdvertisingSet?,
                    enable: Boolean,
                    status: Int
                ) {
                    if (!enable) close()
                }
            }

        bleAdvertiser.startAdvertisingSet(
            advertiseParams.fwkAdvertiseSetParams(),
            advertiseParams.fwkAdvertiseData,
            /*scanResponse=*/ null,
            /*periodicParameters=*/ null,
            /*periodicData=*/ null,
            // round up
            (advertiseParams.durationMillis.toInt() + 9) / 10,
            /*maxExtendedAdvertisingEvents=*/ 0,
            callback
        )

        if (advertiseParams.durationMillis > 0) {
            delay(advertiseParams.durationMillis)
            close()
        }

        awaitClose { bleAdvertiser.stopAdvertisingSet(callback) }
    }
}
