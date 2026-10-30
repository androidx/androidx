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

package androidx.camera.extensions.internal.compat.quirk

import android.os.Build
import androidx.camera.extensions.ExtensionMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.TIRAMISU)
class ExtensionDisabledQuirkTest {

    private val allModes =
        listOf(
            ExtensionMode.BOKEH,
            ExtensionMode.HDR,
            ExtensionMode.NIGHT,
            ExtensionMode.FACE_RETOUCH,
            ExtensionMode.AUTO,
        )

    @Test
    fun notLoaded_onOtherDevices() {
        setDevice(brand = "Samsung", model = "SM-G998B")
        assertThat(ExtensionDisabledQuirk.load()).isFalse()

        setDevice(brand = "Google", model = "SM-S901U")
        assertThat(ExtensionDisabledQuirk.load()).isFalse()
    }

    @Test
    fun samsungA52s_disablesBokehAndFaceRetouchOnCamera0() {
        setDevice(brand = "samsung", model = "SM-A528B")
        assertThat(ExtensionDisabledQuirk.load()).isTrue()

        assertThat(disabledModes("0"))
            .containsExactly(ExtensionMode.BOKEH, ExtensionMode.FACE_RETOUCH)
        assertThat(disabledModes("1")).isEmpty()
    }

    @Test
    fun samsungS22_disablesAllModesOnCamera3() {
        setDevice(brand = "samsung", model = "SM-S901U")
        assertThat(ExtensionDisabledQuirk.load()).isTrue()

        assertThat(disabledModes("3")).containsExactlyElementsIn(allModes)
        assertThat(disabledModes("0")).isEmpty()
        assertThat(disabledModes("1")).isEmpty()
    }

    @Test
    fun samsungS22Ultra_disablesAllModesOnCamera3() {
        setDevice(brand = "samsung", model = "SM-S908U1")
        assertThat(ExtensionDisabledQuirk.load()).isTrue()

        assertThat(disabledModes("3")).containsExactlyElementsIn(allModes)
        assertThat(disabledModes("0")).isEmpty()
        assertThat(disabledModes("1")).isEmpty()
    }

    @Test
    fun samsungZFold5_disablesAutoOnCamera1() {
        setDevice(brand = "samsung", model = "SM-F946U1")
        assertThat(ExtensionDisabledQuirk.load()).isTrue()

        assertThat(disabledModes("1")).containsExactly(ExtensionMode.AUTO)
        assertThat(disabledModes("0")).isEmpty()
    }

    @Test
    fun neverDisablesNoneMode() {
        listOf("SM-A528B", "SM-S901U", "SM-S908U1", "SM-F946U1").forEach { model ->
            setDevice(brand = "samsung", model = model)
            val quirk = ExtensionDisabledQuirk()
            listOf("0", "1", "2", "3").forEach { cameraId ->
                assertThat(quirk.shouldDisableExtension(cameraId, ExtensionMode.NONE)).isFalse()
            }
        }
    }

    @Test
    fun doesNotDisableAnyMode_onOtherDevices() {
        setDevice(brand = "samsung", model = "SM-G998B")

        listOf("0", "1", "2", "3").forEach { cameraId ->
            assertThat(disabledModes(cameraId)).isEmpty()
        }
    }

    private fun disabledModes(cameraId: String): List<Int> {
        val quirk = ExtensionDisabledQuirk()
        return allModes.filter { quirk.shouldDisableExtension(cameraId, it) }
    }

    private fun setDevice(brand: String, model: String) {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setModel(model)
    }
}
