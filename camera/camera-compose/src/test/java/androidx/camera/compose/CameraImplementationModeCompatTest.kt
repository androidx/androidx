/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.compose

import android.os.Build
import androidx.camera.core.CameraInfo
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.viewfinder.core.ImplementationMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument // Needed for Robolectric to correctly instrument classes in the same module
class CameraImplementationModeCompatTest {

    @Test
    @Config(minSdk = Build.VERSION_CODES.M) // Run on any API level
    fun chooseCompatibleMode_returnsEmbedded_whenHardwareLevelIsLegacy() {
        val cameraInfo = fakeCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(cameraInfo)

        // Assert: The mode should be EMBEDDED
        assertThat(mode).isEqualTo(ImplementationMode.EMBEDDED)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.N_MR1) // Run on API 25+ where default is EXTERNAL
    fun chooseCompatibleMode_returnsExternal_whenHardwareLevelIsNotLegacy_onHighApiLevel() {
        val cameraInfo = fakeCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(cameraInfo)

        // Assert: The mode should be EXTERNAL since the hardware level is not LEGACY and
        // the API level is high enough to not trigger the fallback.
        assertThat(mode).isEqualTo(ImplementationMode.EXTERNAL)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.N) // Run on API 24- where default is EMBEDDED
    fun chooseCompatibleMode_returnsEmbedded_whenHardwareLevelIsNotLegacy_onLowApiLevel() {
        val cameraInfo = fakeCameraInfo(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(cameraInfo)

        // Assert: The mode should be EMBEDDED due to the low API level fallback, even though
        // the hardware level is not LEGACY.
        assertThat(mode).isEqualTo(ImplementationMode.EMBEDDED)
    }

    private fun fakeCameraInfo(implementationType: String): CameraInfo =
        FakeCameraInfoInternal().apply { setImplementationType(implementationType) }
}
