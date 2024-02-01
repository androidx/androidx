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

import android.bluetooth.BluetoothGattCharacteristic as FwkBluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService as FwkBluetoothGattService

internal class AttributeMap {
    private val fwkServices: MutableMap<FwkBluetoothGattService, GattService> = mutableMapOf()
    private val fwkCharacteristics: MutableMap<FwkBluetoothGattCharacteristic, GattCharacteristic> =
        mutableMapOf()

    fun updateWithFrameworkServices(fwkServices: List<FwkBluetoothGattService>) {
        this.fwkServices.clear()
        fwkCharacteristics.clear()

        fwkServices.forEach { serv ->
            val serviceCharacteristics = mutableListOf<GattCharacteristic>()
            serv.characteristics.forEach { char ->
                GattCharacteristic(char).let {
                    fwkCharacteristics[char] = it
                    serviceCharacteristics.add(it)
                }
            }
            this.fwkServices[serv] = GattService(serv, serviceCharacteristics)
        }
    }

    fun updateWithServices(services: List<GattService>) {
        this.fwkServices.clear()
        fwkCharacteristics.clear()

        services.forEach { serv ->
            this.fwkServices[serv.fwkService] = serv
            serv.characteristics.forEach { char ->
                fwkCharacteristics[char.fwkCharacteristic] = char
            }
        }
    }

    fun getServices(): List<GattService> {
        return fwkServices.values.toList()
    }

    fun fromFwkService(fwkService: FwkBluetoothGattService): GattService? {
        return fwkServices[fwkService]
    }

    fun fromFwkCharacteristic(
        fwkCharacteristic: FwkBluetoothGattCharacteristic
    ): GattCharacteristic? {
        return fwkCharacteristics[fwkCharacteristic]
    }
}
