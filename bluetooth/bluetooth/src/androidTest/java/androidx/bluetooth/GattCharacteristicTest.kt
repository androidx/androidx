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

import android.bluetooth.BluetoothGattCharacteristic as FwkCharacteristic
import java.util.UUID
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GattCharacteristicTest {
    @Test
    fun constructorWithFwkInstance() {
        val propertiesMap = mapOf(
            FwkCharacteristic.PROPERTY_BROADCAST to
                GattCharacteristic.PROPERTY_BROADCAST,
            FwkCharacteristic.PROPERTY_EXTENDED_PROPS to
                GattCharacteristic.PROPERTY_EXTENDED_PROPS,
            FwkCharacteristic.PROPERTY_INDICATE to
                GattCharacteristic.PROPERTY_INDICATE,
            FwkCharacteristic.PROPERTY_NOTIFY
                to GattCharacteristic.PROPERTY_NOTIFY,
            FwkCharacteristic.PROPERTY_READ
                to GattCharacteristic.PROPERTY_READ,
            FwkCharacteristic.PROPERTY_SIGNED_WRITE
                to GattCharacteristic.PROPERTY_SIGNED_WRITE,
            FwkCharacteristic.PROPERTY_WRITE
                to GattCharacteristic.PROPERTY_WRITE,
            FwkCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                to GattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
        )

        propertiesMap.forEach {
            val charUuid = UUID.randomUUID()
            val fwkGattCharacteristic = FwkCharacteristic(charUuid, it.key,
                /*permissions=*/0)
            val gattCharacteristic = GattCharacteristic(fwkGattCharacteristic)

            Assert.assertEquals(fwkGattCharacteristic.uuid, gattCharacteristic.uuid)
            Assert.assertEquals(it.value, gattCharacteristic.properties)
        }
    }

    @Test
    fun constructorWithUuid() {
        val uuid = UUID.randomUUID()

        val properties = GattCharacteristic.PROPERTY_READ

        val characteristic = GattCharacteristic(uuid, properties)

        Assert.assertEquals(uuid, characteristic.uuid)
        Assert.assertEquals(properties, characteristic.properties)
    }
}
