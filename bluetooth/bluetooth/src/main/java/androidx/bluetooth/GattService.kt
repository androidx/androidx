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

import android.bluetooth.BluetoothGattService
import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * Represents a Bluetooth GATT service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class GattService internal constructor(
    internal val fwkService: BluetoothGattService,
    characteristics: List<GattCharacteristic>? = null
) {
    val uuid: UUID
        get() = fwkService.uuid
    val characteristics: List<GattCharacteristic>

    init {
        this.characteristics = characteristics?.toList()
            ?: fwkService.characteristics.map { GattCharacteristic(it) }
        this.characteristics.forEach { it.service = this }
    }
}

/**
 * Creates a [GattService] instance for a GATT server.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun GattService(uuid: UUID, characteristics: List<GattCharacteristic>): GattService {
    val fwkService = BluetoothGattService(uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
    characteristics.forEach { fwkService.addCharacteristic(it.fwkCharacteristic) }
    return GattService(fwkService, characteristics)
}