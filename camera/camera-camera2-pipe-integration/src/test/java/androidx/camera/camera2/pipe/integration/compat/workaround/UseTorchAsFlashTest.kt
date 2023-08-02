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
import android.os.Build
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraNoResponseWhenEnablingFlashQuirk
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ImageCaptureWashedOutImageQuirk
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.StreamConfigurationMapBuilder

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class UseTorchAsFlashTest {

    @Test
    fun shouldUseTorchAsFlash_default_isFalse() {
        val useTorchAsFlash = createUseTorchAsFlash()

        assertThat(useTorchAsFlash.shouldUseTorchAsFlash()).isFalse()
    }

    @Test
    fun shouldUseTorchAsFlash_withCameraNoResponseWhenEnablingFlashQuirk_isTrue() {
        CameraNoResponseWhenEnablingFlashQuirk.AFFECTED_MODELS.forEach { model ->
            ShadowBuild.setModel(model)
            val useTorchAsFlash = createUseTorchAsFlash()

            assertThat(useTorchAsFlash.shouldUseTorchAsFlash()).isTrue()
        }
    }

    @Test
    fun shouldUseTorchAsFlash_withImageCaptureWashedOutImageQuirk_isTrue() {
        ImageCaptureWashedOutImageQuirk.BUILD_MODELS.forEach { model ->
            ShadowBuild.setModel(model)
            val useTorchAsFlash = createUseTorchAsFlash()

            assertThat(useTorchAsFlash.shouldUseTorchAsFlash()).isTrue()
        }
    }

    @Test
    fun shouldUseTorchAsFlash_lensFacingFront_isFalse() {
        CameraNoResponseWhenEnablingFlashQuirk.AFFECTED_MODELS.forEach { model ->
            ShadowBuild.setModel(model)
            val useTorchAsFlash =
                createUseTorchAsFlash(lensFacing = CameraCharacteristics.LENS_FACING_FRONT)

            assertThat(useTorchAsFlash.shouldUseTorchAsFlash()).isFalse()
        }
    }

    private fun createUseTorchAsFlash(
        lensFacing: Int = CameraCharacteristics.LENS_FACING_BACK
    ): UseTorchAsFlash {
        val metadata = FakeCameraMetadata(mapOf(CameraCharacteristics.LENS_FACING to lensFacing))

        return UseTorchAsFlash.Bindings.provideUseTorchAsFlash(
            CameraQuirks(
                metadata,
                StreamConfigurationMapCompat(
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
