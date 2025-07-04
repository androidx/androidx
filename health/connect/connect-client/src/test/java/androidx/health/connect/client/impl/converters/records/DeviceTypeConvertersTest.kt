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

package androidx.health.connect.client.impl.converters.records

import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.DeviceTypes
import androidx.health.platform.client.proto.DataProto
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTypeConvertersTest {
    @Test
    fun allDeviceTypes_compatible() {
        for ((deviceType, deviceTypeString) in DEVICE_TYPE_INT_TO_STRING_MAP.entries) {
            assertThat(Device(type = deviceType).toProto())
                .isEqualTo(DataProto.Device.newBuilder().setType(deviceTypeString).build())
            assertThat(DataProto.Device.newBuilder().setType(deviceTypeString).build().toDevice())
                .isEqualTo(Device(type = deviceType))
        }
    }

    @Test
    fun unknownDeviceType_fallBackUnknown() {
        val totalDeviceTypes = DEVICE_TYPE_INT_TO_STRING_MAP.size

        assertThat(Device(type = totalDeviceTypes).toProto())
            .isEqualTo(DataProto.Device.newBuilder().setType(DeviceTypes.UNKNOWN).build())
        assertThat(DataProto.Device.newBuilder().setType("unrecognized enum").build().toDevice())
            .isEqualTo(Device(type = Device.Companion.TYPE_UNKNOWN))
    }
}
