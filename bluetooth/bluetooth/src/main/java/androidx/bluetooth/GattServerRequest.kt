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

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt.GATT_READ_NOT_PERMITTED
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.bluetooth.BluetoothGatt.GATT_WRITE_NOT_PERMITTED
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
interface GattServerRequest {
    class ReadCharacteristicRequest internal constructor(
        private val serverImpl: GattServerImpl,
        internal val device: BluetoothDevice,
        private val requestId: Int,
        val offset: Int,
        val characteristic: GattCharacteristic
    ) : GattServerRequest {
        fun sendResponse(success: Boolean, value: ByteArray?) {
            serverImpl.sendResponse(
                device,
                requestId,
                if (success) GATT_SUCCESS else GATT_READ_NOT_PERMITTED,
                offset,
                value
            )
        }
    }

    class WriteCharacteristicRequest internal constructor(
        private val serverImpl: GattServerImpl,
        internal val device: BluetoothDevice,
        private val requestId: Int,
        val characteristic: GattCharacteristic,
        val isPreparedWrite: Boolean,
        val shouldResponse: Boolean,
        val offset: Int,
        val value: ByteArray?
    ) : GattServerRequest {
        fun sendResponse(success: Boolean) {
            serverImpl.sendResponse(
                device,
                requestId,
                if (success) GATT_SUCCESS else GATT_WRITE_NOT_PERMITTED,
                offset,
                value
            )
        }
    }
}
