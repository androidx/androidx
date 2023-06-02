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
import java.util.UUID
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothGattCharacteristicTest {
    @Test
    fun constructorWithFwkInstance() {
        val propertiesMap = mapOf(
            FwkBluetoothGattCharacteristic.PROPERTY_BROADCAST to
                BluetoothGattCharacteristic.PROPERTY_BROADCAST,
            FwkBluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS to
                BluetoothGattCharacteristic.PROPERTY_EXTENDS_PROP,
            FwkBluetoothGattCharacteristic.PROPERTY_INDICATE to
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
            FwkBluetoothGattCharacteristic.PROPERTY_NOTIFY
                to BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            FwkBluetoothGattCharacteristic.PROPERTY_READ
                to BluetoothGattCharacteristic.PROPERTY_READ,
            FwkBluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE
                to BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
            FwkBluetoothGattCharacteristic.PROPERTY_WRITE
                to BluetoothGattCharacteristic.PROPERTY_WRITE,
            FwkBluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                to BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        )

        val permissionMap = mapOf(
            FwkBluetoothGattCharacteristic.PERMISSION_READ to
                BluetoothGattCharacteristic.PERMISSION_READ,
            FwkBluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED to
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED,
            FwkBluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM to
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM,
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE to
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED to
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED,
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM to
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM,
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED to
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED,
            FwkBluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM to
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM
        )

        propertiesMap.forEach {
            val charUuid = UUID.randomUUID()
            val fwkGattCharacteristic = FwkBluetoothGattCharacteristic(charUuid, it.key,
                /*permissions=*/0)
            val gattCharacteristic = BluetoothGattCharacteristic(fwkGattCharacteristic)

            Assert.assertEquals(fwkGattCharacteristic.uuid, gattCharacteristic.uuid)
            Assert.assertEquals(it.value, gattCharacteristic.properties)
        }

        permissionMap.forEach {
            val charUuid = UUID.randomUUID()
            val fwkGattCharacteristic = FwkBluetoothGattCharacteristic(charUuid,
                /*properties=*/0, it.key)
            val gattCharacteristic = BluetoothGattCharacteristic(fwkGattCharacteristic)

            Assert.assertEquals(fwkGattCharacteristic.uuid, gattCharacteristic.uuid)
            Assert.assertEquals(it.value, gattCharacteristic.permissions)
        }

        val charUuid = UUID.randomUUID()
        val fwkGattCharacteristic = FwkBluetoothGattCharacteristic(
            charUuid,
            /*properties=*/0, /*permissions=*/0
        )
        val descUuid1 = UUID.randomUUID()
        val descUuid2 = UUID.randomUUID()

        val desc1 = FwkBluetoothGattDescriptor(descUuid1, /*permission=*/0)
        val desc2 = FwkBluetoothGattDescriptor(descUuid2, /*permission=*/0)
        fwkGattCharacteristic.addDescriptor(desc1)
        fwkGattCharacteristic.addDescriptor(desc2)

        val characteristicWithDescriptors = BluetoothGattCharacteristic(fwkGattCharacteristic)

        Assert.assertEquals(2, characteristicWithDescriptors.descriptors.size)
        Assert.assertEquals(descUuid1, characteristicWithDescriptors.descriptors[0].uuid)
        Assert.assertEquals(descUuid2, characteristicWithDescriptors.descriptors[1].uuid)
    }
}