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

import android.bluetooth.BluetoothAdapter
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.util.Objects

/**
 * Represents a Bluetooth address for a remote device.
 *
 * @property address a valid Bluetooth MAC address
 * @property addressType a valid address type
 */
class BluetoothAddress(val address: String, @AddressType val addressType: Int) {
    companion object {
        /** Address type is public and registered with the IEEE. */
        const val ADDRESS_TYPE_PUBLIC: Int = 0

        /** Address type is random static. */
        const val ADDRESS_TYPE_RANDOM_STATIC: Int = 1

        /** Address type is random resolvable. */
        const val ADDRESS_TYPE_RANDOM_RESOLVABLE: Int = 2

        /** Address type is random non resolvable. */
        const val ADDRESS_TYPE_RANDOM_NON_RESOLVABLE: Int = 3

        /** Address type is unknown. */
        const val ADDRESS_TYPE_UNKNOWN: Int = 0xFFFF
    }
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ADDRESS_TYPE_PUBLIC,
        ADDRESS_TYPE_RANDOM_STATIC,
        ADDRESS_TYPE_RANDOM_RESOLVABLE,
        ADDRESS_TYPE_RANDOM_NON_RESOLVABLE,
        ADDRESS_TYPE_UNKNOWN
    )
    annotation class AddressType

    init {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw IllegalArgumentException("$address is not a valid Bluetooth address")
        }

        when (addressType) {
            ADDRESS_TYPE_PUBLIC,
            ADDRESS_TYPE_RANDOM_STATIC,
            ADDRESS_TYPE_RANDOM_RESOLVABLE,
            ADDRESS_TYPE_RANDOM_NON_RESOLVABLE,
            ADDRESS_TYPE_UNKNOWN -> Unit
            else -> throw IllegalArgumentException("$addressType is not a valid address type")
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is BluetoothAddress &&
            address == other.address &&
            addressType == other.addressType
    }

    override fun hashCode(): Int {
        return Objects.hash(address, addressType)
    }
}
