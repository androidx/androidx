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

import android.bluetooth.BluetoothGattCharacteristic
import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * Represents a Bluetooth characteristic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class GattCharacteristic internal constructor(
    internal var fwkCharacteristic: BluetoothGattCharacteristic
) {
    companion object {
        /**
         * It permits broadcasts of the characteristic.
         */
        const val PROPERTY_BROADCAST = BluetoothGattCharacteristic.PROPERTY_BROADCAST
        /**
         * It permits reads of the characteristic.
         */
        const val PROPERTY_READ = BluetoothGattCharacteristic.PROPERTY_READ

        /**
         * It permits writes of the characteristic without response.
         */
        const val PROPERTY_WRITE_NO_RESPONSE =
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE

        /**
         * It permits writes of the characteristic with response.
         */
        const val PROPERTY_WRITE = BluetoothGattCharacteristic.PROPERTY_WRITE

        /**
         * It permits notifications of a characteristic value without acknowledgment.
         */
        const val PROPERTY_NOTIFY = BluetoothGattCharacteristic.PROPERTY_NOTIFY

        /**
         * It permits indications of a characteristic value with acknowledgment.
         */
        const val PROPERTY_INDICATE = BluetoothGattCharacteristic.PROPERTY_INDICATE

        /**
         * It permits signed writes to the characteristic value.
         */
        const val PROPERTY_SIGNED_WRITE = BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE

        /**
         * Additional characteristic properties are defined.
         */
        const val PROPERTY_EXTENDS_PROP = BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS

        const val PERMISSION_READ: Int = BluetoothGattCharacteristic.PERMISSION_READ
        const val PERMISSION_READ_ENCRYPTED: Int =
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
        const val PERMISSION_READ_ENCRYPTED_MITM: Int =
            BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
        const val PERMISSION_WRITE: Int = BluetoothGattCharacteristic.PERMISSION_WRITE
        const val PERMISSION_WRITE_ENCRYPTED: Int =
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
        const val PERMISSION_WRITE_ENCRYPTED_MITM: Int =
            BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
        const val PERMISSION_WRITE_SIGNED: Int = BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED
        const val PERMISSION_WRITE_SIGNED_MITM: Int =
            BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM

        const val WRITE_TYPE_DEFAULT: Int = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        const val WRITE_TYPE_SIGNED: Int = BluetoothGattCharacteristic.WRITE_TYPE_SIGNED
        const val WRITE_TYPE_NO_RESPONSE: Int = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
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
    val permissions: Int
        get() = fwkCharacteristic.permissions

    internal var service: GattService? = null
}

/**
 * Creates a [GattCharacteristic] instance for a GATT server.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
fun GattCharacteristic(uuid: UUID, properties: Int, permissions: Int): GattCharacteristic {
    val fwkCharacteristic = BluetoothGattCharacteristic(uuid, properties, permissions)
    return GattCharacteristic(fwkCharacteristic)
}