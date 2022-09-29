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
import androidx.test.filters.MediumTest
import java.util.UUID
import kotlin.random.Random
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class BluetoothGattServiceTest {
    companion object {
        fun generateUUID(): UUID {
            return UUID.randomUUID()
        }

        fun generateType(): Int {
            // Permission are bit from 1<<0 to 1<<8, but ignoring 1<<3
            return Random.nextBits(1)
        }
    }

    @Test
    fun constructorWithValues_createsInstanceCorrectly() {
        repeat(5) {
            val uuid = generateUUID()
            val type = generateType()

            val service = BluetoothGattService(uuid, type)

            Assert.assertEquals(uuid, service.uuid)
            Assert.assertEquals(type, service.type)
            Assert.assertEquals(uuid, service.fwkService.uuid)
            Assert.assertEquals(type, service.fwkService.type)
            Assert.assertEquals(0, service.includedServices.size)
            Assert.assertEquals(0, service.characteristics.size)
        }
    }

    @Test
    fun bluetoothGattServiceBundleable() {
        repeat(5) {

            val serviceUuid = generateUUID()
            val serviceType = generateType()

            val service = BluetoothGattService(serviceUuid, serviceType)

            repeat(5) {
                val uuid = BluetoothGattCharacteristicTest.generateUUID()
                val permissions = BluetoothGattCharacteristicTest.generatePermissions()
                val properties = BluetoothGattCharacteristicTest.generateProperties()

                val characteristic = BluetoothGattCharacteristic(uuid, properties, permissions)
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

                service.addCharacteristic(characteristic)

                Assert.assertEquals(characteristic.service, service)
            }

            repeat(5) {
                val uuid = generateUUID()
                val type = generateType()

                val includedService = BluetoothGattService(uuid, type)

                service.addService(includedService)
            }

            val bundle: Bundle = service.toBundle()
            val newService: BluetoothGattService =
                BluetoothGattService.CREATOR.fromBundle(bundle)

            Assert.assertEquals(newService.uuid, service.uuid)
            Assert.assertEquals(newService.instanceId, service.instanceId)
            Assert.assertEquals(newService.type, service.type)
            Assert.assertEquals(newService.characteristics.size, service.characteristics.size)
            Assert.assertEquals(newService.includedServices.size, service.includedServices.size)
            newService.characteristics.forEach {
                val foundCharacteristic = service.getCharacteristic(it.uuid)

                Assert.assertEquals(foundCharacteristic?.permissions, it.permissions)
                Assert.assertEquals(foundCharacteristic?.uuid, it.uuid)
                Assert.assertEquals(foundCharacteristic?.instanceId, it.instanceId)
                Assert.assertEquals(foundCharacteristic?.properties, it.properties)
                Assert.assertEquals(foundCharacteristic?.writeType, it.writeType)
                Assert.assertEquals(foundCharacteristic?.service, service)
                Assert.assertEquals(it.service, newService)
            }

            newService.includedServices.forEach {
                val foundService = service.getIncludedService(it.uuid)

                Assert.assertEquals(foundService?.uuid, it.uuid)
                Assert.assertEquals(foundService?.type, it.type)
                Assert.assertEquals(foundService?.instanceId, it.instanceId)
            }
        }
    }
}