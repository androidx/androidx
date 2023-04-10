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

package androidx.bluetooth.integration.testapp.experimental

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService

sealed interface GattServerCallback {
    data class OnConnectionStateChange(
        val device: BluetoothDevice?,
        val status: Int,
        val newState: Int
    ) : GattServerCallback

    data class OnServiceAdded(
        val status: Int,
        val service: BluetoothGattService?
    ) : GattServerCallback

    data class OnCharacteristicReadRequest(
        val device: BluetoothDevice?,
        val requestId: Int,
        val offset: Int,
        val characteristic: BluetoothGattCharacteristic?
    ) : GattServerCallback

    data class OnCharacteristicWriteRequest(
        val device: BluetoothDevice?,
        val requestId: Int,
        val characteristic: BluetoothGattCharacteristic?,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray?
    ) : GattServerCallback

    data class OnDescriptorReadRequest(
        val device: BluetoothDevice?,
        val requestId: Int,
        val offset: Int,
        val descriptor: BluetoothGattDescriptor?
    ) : GattServerCallback

    data class OnDescriptorWriteRequest(
        val device: BluetoothDevice?,
        val requestId: Int,
        val descriptor: BluetoothGattDescriptor?,
        val preparedWrite: Boolean,
        val responseNeeded: Boolean,
        val offset: Int,
        val value: ByteArray?
    ) : GattServerCallback

    data class OnExecuteWrite(
        val device: BluetoothDevice?,
        val requestId: Int,
        val execute: Boolean
    ) : GattServerCallback

    data class OnNotificationSent(
        val device: BluetoothDevice?,
        val status: Int
    ) : GattServerCallback

    data class OnMtuChanged(
        val device: BluetoothDevice?,
        val mtu: Int
    ) : GattServerCallback

    data class OnPhyUpdate(
        val device: BluetoothDevice?,
        val txPhy: Int,
        val rxPhy: Int,
        val status: Int
    ) : GattServerCallback

    data class OnPhyRead(
        val device: BluetoothDevice?,
        val txPhy: Int,
        val rxPhy: Int,
        val status: Int
    ) : GattServerCallback
}
