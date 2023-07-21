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

import android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY
import android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ
import java.util.UUID
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BluetoothGattCharacteristicTest {
    @Test
    fun constructorWithFwkInstance() {
        val characteristicUuid = UUID.randomUUID()
        val properties = PROPERTY_READ or PROPERTY_NOTIFY
        val permissions = PERMISSION_READ

        val fwkGattCharacteristic = android.bluetooth.BluetoothGattCharacteristic(
            characteristicUuid,
            properties,
            permissions,
        )
        val gattCharacteristic = BluetoothGattCharacteristic(fwkGattCharacteristic)

        Assert.assertEquals(fwkGattCharacteristic.uuid, gattCharacteristic.uuid)
        Assert.assertEquals(fwkGattCharacteristic.properties, gattCharacteristic.properties)
        Assert.assertEquals(fwkGattCharacteristic.permissions, gattCharacteristic.permissions)
    }
}