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

/**
 * Represents a Bluetooth address for a remote device.
 *
 * @property address valid Bluetooth MAC address
 * @property addressType valid address type
 *
 */
class BluetoothAddress(val address: String, var addressType: Int) {
    init {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw IllegalArgumentException("$address is not a valid Bluetooth address")
        }

        if (addressType != AddressType.ADDRESS_TYPE_PUBLIC && addressType !=
            AddressType.ADDRESS_TYPE_RANDOM) {
            addressType = AddressType.ADDRESS_TYPE_UNKNOWN
        }
    }
}