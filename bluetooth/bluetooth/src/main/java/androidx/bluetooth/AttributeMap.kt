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

import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import android.bluetooth.BluetoothGattService as FwkService

internal class AttributeMap {
    private val services: MutableMap<FwkService, GattService> = mutableMapOf()
    private val characteristics: MutableMap<FwkCharacteristic, GattCharacteristic> =
        mutableMapOf()
    fun updateWithFrameworkServices(services: List<FwkService>) {
        this.services.clear()
        characteristics.clear()

        services.forEach { serv ->
            val serviceCharacteristics = mutableListOf<GattCharacteristic>()
            serv.characteristics.forEach { char ->
               GattCharacteristic(char).let {
                   characteristics[char] = it
                   serviceCharacteristics.add(it)
               }
            }
            this.services[serv] = GattService(serv, serviceCharacteristics)
        }
    }

    fun updateWithServices(services: List<GattService>) {
        this.services.clear()
        characteristics.clear()

        services.forEach { serv ->
            this.services[serv.fwkService] = serv
            serv.characteristics.forEach { char ->
                characteristics[char.fwkCharacteristic] = char
            }
        }
    }

    fun getServices(): List<GattService> {
        return services.values.toList()
    }

    fun fromFwkService(service: FwkService): GattService? {
        return services[service]
    }

    fun fromFwkCharacteristic(characteristic: FwkCharacteristic): GattCharacteristic? {
        return characteristics[characteristic]
    }
}
