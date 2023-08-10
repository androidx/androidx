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
import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * Represents a Bluetooth characteristic.
 */
class GattCharacteristic internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkCharacteristic: FwkCharacteristic
) {
    companion object {
        /**
         * It permits broadcasts of the characteristic.
         */
        const val PROPERTY_BROADCAST = FwkCharacteristic.PROPERTY_BROADCAST
        /**
         * It permits reads of the characteristic.
         */
        const val PROPERTY_READ = FwkCharacteristic.PROPERTY_READ

        /**
         * It permits writes of the characteristic without response.
         */
        const val PROPERTY_WRITE_NO_RESPONSE =
            FwkCharacteristic.PROPERTY_WRITE_NO_RESPONSE

        /**
         * It permits writes of the characteristic with response.
         */
        const val PROPERTY_WRITE = FwkCharacteristic.PROPERTY_WRITE

        /**
         * It permits notifications of a characteristic value without acknowledgment.
         */
        const val PROPERTY_NOTIFY = FwkCharacteristic.PROPERTY_NOTIFY

        /**
         * It permits indications of a characteristic value with acknowledgment.
         */
        const val PROPERTY_INDICATE = FwkCharacteristic.PROPERTY_INDICATE

        /**
         * It permits signed writes to the characteristic value.
         */
        const val PROPERTY_SIGNED_WRITE = FwkCharacteristic.PROPERTY_SIGNED_WRITE

        /**
         * Additional characteristic properties are defined.
         */
        const val PROPERTY_EXTENDS_PROP = FwkCharacteristic.PROPERTY_EXTENDED_PROPS

        /**
         * Creates a [GattCharacteristic] instance for a GATT server.
         */
        @JvmStatic
        fun of(uuid: UUID, properties: Int): GattCharacteristic {
            var permissions = 0
            if ((properties and PROPERTY_READ) != 0) {
                permissions = permissions or FwkCharacteristic.PERMISSION_READ
            }
            if ((properties and (PROPERTY_WRITE or PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                permissions = permissions or FwkCharacteristic.PERMISSION_WRITE
            }
            if ((properties and PROPERTY_SIGNED_WRITE) != 0) {
                permissions = permissions or FwkCharacteristic.PERMISSION_WRITE_SIGNED
            }
            val fwkCharacteristic = FwkCharacteristic(uuid, properties, permissions)
            return GattCharacteristic(fwkCharacteristic)
        }
    }

    /**
     * The UUID of the characteristic.
     */
    val uuid: UUID
        get() = fwkCharacteristic.uuid

    /**
     * The properties of the characteristic.
     */
    val properties: Int
        get() = fwkCharacteristic.properties

    /**
     * The permissions for the characteristic.
     */
    internal val permissions: Int
        get() = fwkCharacteristic.permissions

    internal var service: GattService? = null
}
