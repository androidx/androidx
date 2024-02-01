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
import android.bluetooth.BluetoothGattDescriptor as FwkBluetoothGattDescriptor
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * Represents a Bluetooth characteristic.
 */
class GattCharacteristic internal constructor(
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var fwkCharacteristic: FwkBluetoothGattCharacteristic
) {
    @Target(AnnotationTarget.TYPE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        flag = true, value = [
            PROPERTY_BROADCAST,
            PROPERTY_READ,
            PROPERTY_WRITE_NO_RESPONSE,
            PROPERTY_WRITE,
            PROPERTY_NOTIFY,
            PROPERTY_INDICATE,
            PROPERTY_SIGNED_WRITE,
            PROPERTY_EXTENDED_PROPS
        ]
    )
    annotation class Property

    companion object {
        /**
         * It permits broadcasts of the characteristic.
         */
        const val PROPERTY_BROADCAST = FwkBluetoothGattCharacteristic.PROPERTY_BROADCAST

        /**
         * It permits reads of the characteristic.
         */
        const val PROPERTY_READ = FwkBluetoothGattCharacteristic.PROPERTY_READ

        /**
         * It permits writes of the characteristic without response.
         */
        const val PROPERTY_WRITE_NO_RESPONSE =
            FwkBluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE

        /**
         * It permits writes of the characteristic with response.
         */
        const val PROPERTY_WRITE = FwkBluetoothGattCharacteristic.PROPERTY_WRITE

        /**
         * It permits notifications of a characteristic value without acknowledgment.
         */
        const val PROPERTY_NOTIFY = FwkBluetoothGattCharacteristic.PROPERTY_NOTIFY

        /**
         * It permits indications of a characteristic value with acknowledgment.
         */
        const val PROPERTY_INDICATE = FwkBluetoothGattCharacteristic.PROPERTY_INDICATE

        /**
         * It permits signed writes to the characteristic value.
         */
        const val PROPERTY_SIGNED_WRITE = FwkBluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE

        /**
         * Additional characteristic properties are defined.
         */
        const val PROPERTY_EXTENDED_PROPS = FwkBluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS

        @JvmStatic
        private fun getPermissionsWithProperties(properties: @Property Int): Int {
            var permissions = 0
            if ((properties and PROPERTY_READ) != 0) {
                permissions = permissions or FwkBluetoothGattCharacteristic.PERMISSION_READ
            }
            if ((properties and (PROPERTY_WRITE or PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                permissions = permissions or FwkBluetoothGattCharacteristic.PERMISSION_WRITE
            }
            if ((properties and PROPERTY_SIGNED_WRITE) != 0) {
                permissions = permissions or FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED
            }
            return permissions
        }
    }

    constructor(uuid: UUID, properties: @Property Int) : this(
        FwkBluetoothGattCharacteristic(
            uuid, properties, getPermissionsWithProperties(properties)
        )
    ) {
        if (isSubscribable) {
            val cccDescriptor = FwkBluetoothGattDescriptor(
                GattCommon.UUID_CCCD,
                FwkBluetoothGattDescriptor.PERMISSION_READ or
                    FwkBluetoothGattDescriptor.PERMISSION_WRITE
            )
            fwkCharacteristic.addDescriptor(cccDescriptor)
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
    val properties: @Property Int
        get() = fwkCharacteristic.properties

    internal val isSubscribable: Boolean
        get() = (properties and (PROPERTY_NOTIFY or PROPERTY_INDICATE)) != 0

    /**
     * The permissions for the characteristic.
     */
    internal val permissions: Int
        get() = fwkCharacteristic.permissions

    internal var service: GattService? = null
}
