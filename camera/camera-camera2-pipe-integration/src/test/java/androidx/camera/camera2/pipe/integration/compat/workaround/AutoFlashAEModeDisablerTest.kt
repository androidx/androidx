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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class AutoFlashAEModeDisablerTest {
    private val anyCameraId = "0"

    @Test
    fun changeAeAutoFlashToAeOn_onSamsungA300H() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A300H")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
    }

    @Test
    fun changeOnAutoFlashToOn_onSamsungA300YZ() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A300YZ")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
    }

    @Test
    fun keepAeOn_onSamsungA300H() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A300H")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
    }

    @Test
    fun keepAeAlwaysOn_onSamsungA300H() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A300H")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
    }

    @Test
    fun changeOnAutoFlashToOn_onSamsungJ5() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-J510FN")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
    }

    @Test
    fun keepAeAutoFlash_onSamsungOtherDevices() {
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", "Samsung")
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "SM-A3XXX")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
    }

    @Test
    fun changeOnAutoFlashToOn_onSamsungJ7FrontCamera() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "sm-j710f")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_FRONT)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON)
    }

    @Test
    fun keepAeAutoFlash_onSamsungJ7MainCamera() {
        ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "sm-j710f")
        val aeMode: Int = createAutoFlashAEModeDisabler(CameraCharacteristics.LENS_FACING_BACK)
            .getCorrectedAeMode(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        assertThat(aeMode).isEqualTo(CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
    }

    private fun createAutoFlashAEModeDisabler(lensFacing: Int): AutoFlashAEModeDisabler {
        val metadata = FakeCameraMetadata(mapOf(CameraCharacteristics.LENS_FACING to lensFacing))
        return AutoFlashAEModeDisabler.Bindings.provideAEModeDisabler(
            CameraQuirks(
                metadata, StreamConfigurationMapCompat(
                    StreamConfigurationMapBuilder.newBuilder().build(),
                    OutputSizesCorrector(
                        FakeCameraMetadata(),
                        StreamConfigurationMapBuilder.newBuilder().build()
                    )
                )
            )
        )
    }
}
