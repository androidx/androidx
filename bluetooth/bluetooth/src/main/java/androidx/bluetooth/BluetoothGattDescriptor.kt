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

import android.bluetooth.BluetoothGattDescriptor as FwkBluetoothGattDescriptor
import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * Represents a Bluetooth GATT characteristic descriptor
 *
 * GATT descriptors contain additional information and attributes of a GATT characteristic,
 * [BluetoothGattCharacteristic]. They can be used to describe the characteristic's features or
 * to control certain behaviours of the characteristic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class BluetoothGattDescriptor internal constructor(
    internal var fwkDescriptor: FwkBluetoothGattDescriptor
) {
    companion object {
        /**
         * The descriptor is readable
         */
        const val PERMISSION_READ: Int = FwkBluetoothGattDescriptor.PERMISSION_READ

        /**
         * The descriptor is readable if encrypted
         */
        const val PERMISSION_READ_ENCRYPTED: Int =
            FwkBluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED

        /**
         * The descriptor is readable if person-in-the-middle protection is enabled
         */
        const val PERMISSION_READ_ENCRYPTED_MITM: Int =
            FwkBluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM

        /**
         * The descriptor is writable
         */
        const val PERMISSION_WRITE: Int = FwkBluetoothGattDescriptor.PERMISSION_WRITE

        /**
         * The descriptor is writable if encrypted
         */
        const val PERMISSION_WRITE_ENCRYPTED: Int =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED

        /**
         * The descriptor is writable if person-in-the-middle protection is enabled
         */
        const val PERMISSION_WRITE_ENCRYPTED_MITM: Int =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM

        /**
         * The descriptor is writable if authentication signature is used
         */
        const val PERMISSION_WRITE_SIGNED: Int = FwkBluetoothGattDescriptor.PERMISSION_WRITE_SIGNED

        /**
         * The descriptor is writable if person-in-the-middle protection is enabled
         * and authentication signature is used
         */
        const val PERMISSION_WRITE_SIGNED_MITM: Int =
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM
    }

    /**
     * The UUID of the descriptor.
     */
    val uuid: UUID
        get() = fwkDescriptor.uuid

    /**
     * The permissions for the descriptor.
     *
     * It is a combination of [PERMISSION_READ], [PERMISSION_READ_ENCRYPTED],
     * [PERMISSION_READ_ENCRYPTED_MITM], [PERMISSION_WRITE], [PERMISSION_WRITE_ENCRYPTED],
     * [PERMISSION_WRITE_ENCRYPTED_MITM], [PERMISSION_WRITE_SIGNED],
     * and [PERMISSION_WRITE_SIGNED_MITM].
     */
    val permissions: Int
        get() = fwkDescriptor.permissions
}