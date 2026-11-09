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

package androidx.camera.core.internal.compat.quirk

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@Config(sdk = [Config.ALL_SDKS])
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
class FastSessionSwitchDisablerQuirkTest {

    private var initialBrand: String? = null
    private var initialModel: String? = null

    @Before
    fun setUp() {
        initialBrand = Build.BRAND
        initialModel = Build.MODEL
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", initialBrand)
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", initialModel)
    }

    @Test
    fun load_returnsTrue_whenMotoC() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "motorola")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "moto c")
        assertThat(FastSessionSwitchDisablerQuirk.load()).isTrue()
        assertThat(DeviceQuirks.get(FastSessionSwitchDisablerQuirk::class.java)).isNotNull()
    }

    @Test
    fun load_returnsTrue_whenMotoCUppercase() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Motorola")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "MOTO C")
        assertThat(FastSessionSwitchDisablerQuirk.load()).isTrue()
        assertThat(DeviceQuirks.get(FastSessionSwitchDisablerQuirk::class.java)).isNotNull()
    }

    @Test
    fun load_returnsFalse_forOtherDevices() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", "Google")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "Pixel 8")
        assertThat(FastSessionSwitchDisablerQuirk.load()).isFalse()
        assertThat(DeviceQuirks.get(FastSessionSwitchDisablerQuirk::class.java)).isNull()
    }
}
