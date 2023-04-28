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

import junit.framework.TestCase.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothAddressTest {

    companion object {
        // TODO(kihongs) Change to actual public address if possible
        private const val TEST_ADDRESS_PUBLIC = "00:43:A8:23:10:F0"
        private const val TEST_ADDRESS_RANDOM = "F0:43:A8:23:10:00"
        private const val TEST_ADDRESS_UNKNOWN = "F0:43:A8:23:10:12"
    }

    @Test
    fun constructorWithAddressTypePublic() {
        val addressType = AddressType.ADDRESS_TYPE_PUBLIC

        val bluetoothAddress = BluetoothAddress(TEST_ADDRESS_PUBLIC, addressType)

        assertEquals(TEST_ADDRESS_PUBLIC, bluetoothAddress.address)
        assertEquals(addressType, bluetoothAddress.addressType)
    }

    @Test
    fun constructorWithAddressTypeRandom() {
        val addressType = AddressType.ADDRESS_TYPE_RANDOM

        val bluetoothAddress = BluetoothAddress(TEST_ADDRESS_RANDOM, addressType)

        assertEquals(TEST_ADDRESS_RANDOM, bluetoothAddress.address)
        assertEquals(addressType, bluetoothAddress.addressType)
    }

    @Test
    fun constructorWithAddressTypeUnknown() {
        val addressType = AddressType.ADDRESS_TYPE_UNKNOWN

        val bluetoothAddress = BluetoothAddress(TEST_ADDRESS_UNKNOWN, addressType)

        assertEquals(TEST_ADDRESS_UNKNOWN, bluetoothAddress.address)
        assertEquals(addressType, bluetoothAddress.addressType)
    }

    @Test
    fun constructorWithInvalidAddressType() {
        val invalidAddressType = -1

        val bluetoothAddress = BluetoothAddress(TEST_ADDRESS_UNKNOWN, invalidAddressType)

        assertEquals(TEST_ADDRESS_UNKNOWN, bluetoothAddress.address)
        assertEquals(AddressType.ADDRESS_TYPE_UNKNOWN, bluetoothAddress.addressType)
    }

    @Test
    fun constructorWithInvalidAddress() {
        val invalidAddress = "invalidAddress"

        assertFailsWith<IllegalArgumentException> {
            BluetoothAddress(invalidAddress, AddressType.ADDRESS_TYPE_UNKNOWN)
        }
    }
}