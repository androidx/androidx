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
import java.util.UUID
import kotlin.random.Random
import androidx.test.filters.MediumTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class BluetoothGattCharacteristicTest {
    companion object {
        fun generateUUID(): UUID {
            return UUID.randomUUID()
        }

        fun generatePermissions(): Int {
            // Permission are bit from 1<<0 to 1<<8, but ignoring 1<<3
            return Random.nextBits(20) and (BluetoothGattCharacteristic.PERMISSION_READ or
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED or
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM or
                BluetoothGattCharacteristic.PERMISSION_WRITE or
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED or
                BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM)
        }

        fun generateProperties(): Int {
            // Permission are bit from 1<<0 to 1<<7
            return Random.nextBits(20) and (BluetoothGattCharacteristic.PROPERTY_BROADCAST or
                BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS or
                BluetoothGattCharacteristic.PROPERTY_INDICATE or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                BluetoothGattCharacteristic.PROPERTY_READ or
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
        }
    }

    @Test
    fun constructorWithValues_createsInstanceCorrectly() {
        repeat(5) {
            val uuid = generateUUID()
            val permissions = generatePermissions()
            val properties = generateProperties()

            val characteristic = BluetoothGattCharacteristic(uuid, properties, permissions)

            Assert.assertEquals(permissions, characteristic.fwkCharacteristic.permissions)
            Assert.assertEquals(properties, characteristic.fwkCharacteristic.properties)
            Assert.assertEquals(uuid, characteristic.fwkCharacteristic.uuid)
            Assert.assertEquals(permissions, characteristic.permissions)
            Assert.assertEquals(properties, characteristic.properties)
            Assert.assertEquals(uuid, characteristic.uuid)
            Assert.assertEquals(null, characteristic.service)
        }
    }

    @Test
    fun bluetoothGattCharacteristicBundleable() {
        repeat(5) {
            val uuid = generateUUID()
            val permissions = generatePermissions()
            val properties = generateProperties()

            val characteristic = BluetoothGattCharacteristic(uuid, properties, permissions)
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            repeat(5) {
                val descriptorUUID = BluetoothGattDescriptorTest.generateUUID()
                val descriptorPermission = BluetoothGattDescriptorTest.generatePermissions()

                val descriptor = BluetoothGattDescriptor(descriptorUUID, descriptorPermission)
                characteristic.addDescriptor(descriptor)

                Assert.assertEquals(characteristic, descriptor.characteristic)
            }

            val bundle: Bundle = characteristic.toBundle()
            val newCharacteristic: BluetoothGattCharacteristic =
                BluetoothGattCharacteristic.CREATOR.fromBundle(bundle)

            Assert.assertEquals(newCharacteristic.permissions, characteristic.permissions)
            Assert.assertEquals(newCharacteristic.instanceId, characteristic.instanceId)
            Assert.assertEquals(newCharacteristic.uuid, characteristic.uuid)
            Assert.assertEquals(newCharacteristic.properties, characteristic.properties)
            Assert.assertEquals(newCharacteristic.writeType, characteristic.writeType)
            Assert.assertEquals(newCharacteristic.descriptors.size, characteristic.descriptors.size)
            newCharacteristic.descriptors.forEach {
                val foundDescriptor = characteristic.getDescriptor(it.uuid)

                Assert.assertEquals(foundDescriptor?.permissions, it.permissions)
                Assert.assertEquals(foundDescriptor?.uuid, it.uuid)
                Assert.assertEquals(foundDescriptor?.characteristic, characteristic)
                Assert.assertEquals(it.characteristic, newCharacteristic)
            }
        }
    }
}