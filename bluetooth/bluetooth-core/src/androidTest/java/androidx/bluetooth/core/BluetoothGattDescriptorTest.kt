/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.bluetooth.core

import android.os.Bundle
import androidx.test.filters.SmallTest

import java.util.UUID
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class BluetoothGattDescriptorTest {

    companion object {
        fun generateUUID(): UUID {
            return UUID.randomUUID()
        }

        fun generatePermissions(): Int {
            // Permission are bit from 1<<0 to 1<<8, but ignoring 1<<3
            return Random.nextBits(20) and (BluetoothGattDescriptor.PERMISSION_READ or
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM or
                BluetoothGattDescriptor.PERMISSION_WRITE or
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED or
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM or
                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED or
                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM)
        }
    }

    @Test
    fun constructorWithValues_createsInstanceCorrectly() {
        repeat(5) {
            val uuid = generateUUID()
            val permissions = generatePermissions()

            val descriptor = BluetoothGattDescriptor(uuid, permissions)

            assertEquals(permissions, descriptor.fwkDescriptor.permissions)
            assertEquals(uuid, descriptor.fwkDescriptor.uuid)
            assertEquals(permissions, descriptor.permissions)
            assertEquals(uuid, descriptor.uuid)
            assertEquals(null, descriptor.characteristic)
        }
    }

    @Test
    fun bluetoothGattDescriptorBundleable() {
        repeat(5) {
            val uuid = generateUUID()
            val permissions = generatePermissions()
            val descriptor = BluetoothGattDescriptor(uuid, permissions)
            val bundle: Bundle = descriptor.toBundle()
            val newDescriptor: BluetoothGattDescriptor =
                BluetoothGattDescriptor.CREATOR.fromBundle(bundle)

            assertEquals(newDescriptor.permissions, descriptor.permissions)
            assertEquals(newDescriptor.uuid, descriptor.uuid)
        }
    }
}