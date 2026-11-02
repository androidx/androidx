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
    fun unaffectedDevice_doesNotLoadQuirk() {
        ShadowBuild.setBrand("Google")
        ShadowBuild.setModel("Pixel 8")

        assertThat(DeviceQuirks[ZslDisablerQuirk::class.java]).isNull()
    }
}
