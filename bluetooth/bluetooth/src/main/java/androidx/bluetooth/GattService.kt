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

import android.bluetooth.BluetoothGattService as FwkService
import java.util.UUID

/**
 * Represents a Bluetooth GATT service.
 */
class GattService internal constructor(
    internal val fwkService: FwkService,
    characteristics: List<GattCharacteristic>? = null
) {
    /**
     * the UUID of the service
     */
    val uuid: UUID
        get() = fwkService.uuid

    /**
     * a list of characteristics included in the service
     */
    val characteristics: List<GattCharacteristic>

    constructor(uuid: UUID, characteristics: List<GattCharacteristic>) :
        this(FwkService(uuid, FwkService.SERVICE_TYPE_PRIMARY), characteristics) {
        characteristics.forEach { fwkService.addCharacteristic(it.fwkCharacteristic) }
    }

    init {
        this.characteristics = characteristics?.toList()
            ?: fwkService.characteristics.map { GattCharacteristic(it) }
        this.characteristics.forEach { it.service = this }
    }

    /**
     * Gets a [GattCharacteristic] in the service with the given UUID.
     *
     * If the service includes multiple characteristics with the same UUID,
     * the first instance is returned.
     */
    fun getCharacteristic(uuid: UUID): GattCharacteristic? {
        return this.characteristics.firstOrNull { it.uuid == uuid }
    }
}
