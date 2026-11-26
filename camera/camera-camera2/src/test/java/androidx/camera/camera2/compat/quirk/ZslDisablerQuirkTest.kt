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

package androidx.camera.camera2.compat.quirk

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class ZslDisablerQuirkTest {

    @After
    fun tearDown() {
        ShadowBuild.reset()
    }

    @Test
    fun samsungSmF971_loadsQuirk() {
        ShadowBuild.setBrand("samsung")
        ShadowBuild.setModel("SM-F971U1")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun samsungFlip5_loadsQuirk() {
        ShadowBuild.setBrand("samsung")
        ShadowBuild.setModel("SM-F731U1")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun xiaomiRedmiNote13_loadsQuirk() {
        ShadowBuild.setBrand("Redmi")
        ShadowBuild.setModel("23129RA5FL")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun xiaomiRedmiNote12TapasGlobal_loadsQuirk() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Redmi")
        ShadowBuild.setDevice("tapas")
        ShadowBuild.setModel("23021RAAEG")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun xiaomiRedmiNote12TapasDeviceCaseInsensitive_loadsQuirk() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Redmi")
        ShadowBuild.setDevice("TAPAS")
        ShadowBuild.setModel("23027RAD4I")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun redmiNote12ModelPrefix_loadsQuirk() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Redmi")
        ShadowBuild.setDevice("other")
        ShadowBuild.setModel("23028RA60L")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun xiaomiBrandTapasDevice_loadsQuirk() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Xiaomi")
        ShadowBuild.setDevice("tapas")
        ShadowBuild.setModel("23021RAAEG")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun xiaomiRedmiNote12TopazNfc_loadsQuirk() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Redmi")
        ShadowBuild.setDevice("topaz")
        ShadowBuild.setModel("23021RAA2Y")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNotNull()
    }

    @Test
    fun xiaomiUnaffectedDevice_doesNotLoadQuirk() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Redmi")
        ShadowBuild.setDevice("other")
        ShadowBuild.setModel("OtherModel")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNull()
    }

    @Test
    fun nonXiaomiTapasDevice_doesNotLoadQuirk() {
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setBrand("Google")
        ShadowBuild.setDevice("tapas")
        ShadowBuild.setModel("Pixel 8")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNull()
    }

    @Test
    fun samsungUnaffectedModel_doesNotLoadQuirk() {
        ShadowBuild.setBrand("samsung")
        ShadowBuild.setModel("SM-F966U1")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNull()
    }

    @Test
    fun unaffectedDevice_doesNotLoadQuirk() {
        ShadowBuild.setBrand("Google")
        ShadowBuild.setModel("Pixel 8")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNull()
    }
}
