/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.health.connect.client.records.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeviceTest {

    @Test
    fun constructor_argumentsOmitted_displayNameIsNull() {
        val device = Device(type = Device.TYPE_WATCH)

        assertThat(device.displayName).isNull()
    }

    @Test
    fun constructor_onlyDisplayName_setsDisplayName() {
        val device = Device(type = Device.TYPE_SCALE, displayName = "Bathroom scale")

        assertThat(device.manufacturer).isNull()
        assertThat(device.model).isNull()
        assertThat(device.displayName).isEqualTo("Bathroom scale")
    }

    @Test
    fun constructor_allArguments_setsAllProperties() {
        val device = Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3", "Alex's watch")

        assertThat(device.type).isEqualTo(Device.TYPE_WATCH)
        assertThat(device.manufacturer).isEqualTo("Google")
        assertThat(device.model).isEqualTo("Pixel Watch 3")
        assertThat(device.displayName).isEqualTo("Alex's watch")
    }

    @Test
    fun equals_threeArgumentConstructor_equalsNullDisplayName() {
        val threeArguments = Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3")
        val nullDisplayName = Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3", null)

        assertThat(threeArguments).isEqualTo(nullDisplayName)
        assertThat(threeArguments.hashCode()).isEqualTo(nullDisplayName.hashCode())
    }

    @Test
    fun equals_differentDisplayName_notEqual() {
        val device = Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3", "Alex's watch")
        val otherDevice = Device(Device.TYPE_WATCH, "Google", "Pixel Watch 3", "Sam's watch")

        assertThat(device).isNotEqualTo(otherDevice)
    }

    @Test
    fun toString_includesDisplayName() {
        val device = Device(type = Device.TYPE_SCALE, displayName = "Bathroom scale")

        assertThat(device.toString()).contains("displayName=Bathroom scale")
    }
}
