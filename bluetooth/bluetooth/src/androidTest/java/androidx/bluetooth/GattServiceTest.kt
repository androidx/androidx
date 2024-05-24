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
import android.bluetooth.BluetoothGattService as FwkService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Test cases for [GattService] */
@RunWith(JUnit4::class)
class GattServiceTest {

    @Test
    fun constructorWithFwkInstance() {
        val serviceUuid = UUID.randomUUID()
        val fwkGattService = FwkService(serviceUuid, SERVICE_TYPE_PRIMARY)

        val charUuid1 = UUID.randomUUID()
        val fwkCharacteristic1 = FwkCharacteristic(charUuid1, 0, 0)
        fwkGattService.addCharacteristic(fwkCharacteristic1)

        val charUuid2 = UUID.randomUUID()
        val fwkCharacteristic2 = FwkCharacteristic(charUuid2, 0, 0)
        fwkGattService.addCharacteristic(fwkCharacteristic2)

        val gattService = GattService(fwkGattService)

        assertThat(gattService.uuid).isEqualTo(fwkGattService.uuid)
        assertThat(gattService.characteristics.size).isEqualTo(2)
        assertThat(gattService.characteristics[0].uuid).isEqualTo(charUuid1)
        assertThat(gattService.characteristics[1].uuid).isEqualTo(charUuid2)
    }

    @Test
    fun constructorWithUuid() {
        val serviceUuid = UUID.randomUUID()

        val charUuid1 = UUID.randomUUID()
        val charUuid2 = UUID.randomUUID()
        val charUuid3 = UUID.randomUUID()

        val char1 = GattCharacteristic(charUuid1, /* properties= */ 0)
        val char2 = GattCharacteristic(charUuid2, /* properties= */ 0)
        val char3 = GattCharacteristic(charUuid3, /* properties= */ 0)

        val characteristics = mutableListOf(char1, char2)

        val gattService = GattService(serviceUuid, characteristics)

        assertThat(gattService.uuid).isEqualTo(serviceUuid)
        assertThat(gattService.characteristics.size).isEqualTo(2)

        assertThat(gattService.characteristics[0]).isSameInstanceAs(char1)
        assertThat(gattService.characteristics[1]).isSameInstanceAs(char2)

        // The characteristics list should be immutable
        characteristics.add(char3)
        assertThat(gattService.characteristics.size).isEqualTo(2)
    }
}
