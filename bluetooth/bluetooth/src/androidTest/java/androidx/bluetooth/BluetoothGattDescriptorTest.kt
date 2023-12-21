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
import java.util.UUID
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothGattDescriptorTest {
    @Test
    fun constructorWithFwkInstance() {
        val permissionMap = mapOf(
            FwkBluetoothGattDescriptor.PERMISSION_READ to
                BluetoothGattDescriptor.PERMISSION_READ,
            FwkBluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED to
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED,
            FwkBluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM to
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM,
            FwkBluetoothGattDescriptor.PERMISSION_WRITE to
                BluetoothGattDescriptor.PERMISSION_WRITE,
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED to
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED,
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM to
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM,
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_SIGNED to
                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED,
            FwkBluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM to
                BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM
        )

        permissionMap.forEach {
            val descUuid = UUID.randomUUID()
            val fwkGattDescriptor = FwkBluetoothGattDescriptor(descUuid, it.key)
            val gattDescriptor = BluetoothGattDescriptor(fwkGattDescriptor)

            Assert.assertEquals(fwkGattDescriptor.uuid, gattDescriptor.uuid)
            Assert.assertEquals(it.value, gattDescriptor.permissions)
        }
    }
}