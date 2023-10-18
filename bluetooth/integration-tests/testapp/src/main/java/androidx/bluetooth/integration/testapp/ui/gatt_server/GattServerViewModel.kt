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

package androidx.bluetooth.integration.testapp.ui.gatt_server

import androidx.bluetooth.GattCharacteristic
import androidx.bluetooth.GattService
import androidx.lifecycle.ViewModel

class GattServerViewModel : ViewModel() {

    private companion object {
        private const val TAG = "GattServerViewModel"
    }

    private val _gattServerServices = mutableListOf<GattService>()
    val gattServerServices: List<GattService> = _gattServerServices

    private val gattServerServicesCharacteristicValueMap =
        mutableMapOf<GattCharacteristic, ByteArray>()

    fun addGattService(gattService: GattService) {
        _gattServerServices.add(gattService)
    }

    fun addGattCharacteristic(service: GattService, characteristic: GattCharacteristic) {
        val index = _gattServerServices.indexOf(service)
        if (index < 0) return
        _gattServerServices[index] = GattService(service.uuid,
            service.characteristics.toMutableList().apply {
                add(characteristic)
            }
        )
    }

    fun readGattCharacteristicValue(characteristic: GattCharacteristic): ByteArray {
        return gattServerServicesCharacteristicValueMap[characteristic] ?: ByteArray(0)
    }

    fun updateGattCharacteristicValue(characteristic: GattCharacteristic, value: ByteArray) {
        gattServerServicesCharacteristicValueMap[characteristic] = value
    }
}
