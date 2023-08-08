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

package androidx.bluetooth.integration.testapp.data.connection

import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattService
import java.util.UUID
import kotlinx.coroutines.Job

class DeviceConnection(
    val bluetoothDevice: BluetoothDevice
) {
    var job: Job? = null
    var onClickReadCharacteristic: OnClickCharacteristic? = null
    var onClickWriteCharacteristic: OnClickCharacteristic? = null
    var status = Status.DISCONNECTED
    var services = emptyList<GattService>()

    private val values = mutableMapOf<UUID, ByteArray?>()

    fun storeValueFor(characteristic: GattCharacteristic, value: ByteArray?) {
        values[characteristic.uuid] = value
    }

    fun valueFor(characteristic: GattCharacteristic): ByteArray? {
        return values[characteristic.uuid]
    }
}
