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
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.testing.fakes.FakeCamera
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
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
    fun chooseCompatibleMode_returnsEmbedded_whenHardwareLevelIsLegacy() {
        val camera = fakeCamera(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should be EMBEDDED
        assertThat(mode).isEqualTo(ImplementationMode.EMBEDDED)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.N_MR1) // Run on API 25+ where default is EXTERNAL
    fun chooseCompatibleMode_returnsExternal_whenHardwareLevelIsNotLegacy_onHighApiLevel() {
        val camera = fakeCamera(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should be EXTERNAL since the hardware level is not LEGACY and
        // the API level is high enough to not trigger the fallback.
        assertThat(mode).isEqualTo(ImplementationMode.EXTERNAL)
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.N) // Run on API 24- where default is EMBEDDED
    fun chooseCompatibleMode_returnsEmbedded_whenHardwareLevelIsNotLegacy_onLowApiLevel() {
        val camera = fakeCamera(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should be EMBEDDED due to the low API level fallback, even though
        // the hardware level is not LEGACY.
        assertThat(mode).isEqualTo(ImplementationMode.EMBEDDED)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun chooseCompatibleMode_returnsEmbedded_whenExtensionEnabled_onApi33() {
        val camera = fakeCamera(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, isExtensionEnabled = true)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should be EMBEDDED when Extension is enabled on API 33.
        assertThat(mode).isEqualTo(ImplementationMode.EMBEDDED)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun chooseCompatibleMode_returnsExternal_whenExtensionDisabled_onApi33() {
        val camera = fakeCamera(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, isExtensionEnabled = false)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should remain EXTERNAL when Extension is not enabled on API 33.
        assertThat(mode).isEqualTo(ImplementationMode.EXTERNAL)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun chooseCompatibleMode_returnsExternal_whenSessionProcessorIsNotCamera2Extension_onApi33() {
        val camera =
            fakeCamera(
                CameraInfo.IMPLEMENTATION_TYPE_CAMERA2,
                isExtensionEnabled = true,
                sessionProcessorType = SessionProcessor.TYPE_DEFAULT,
            )
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should remain EXTERNAL when the SessionProcessor doesn't create a
        // Camera2 CameraExtensionSession.
        assertThat(mode).isEqualTo(ImplementationMode.EXTERNAL)
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun chooseCompatibleMode_returnsExternal_whenExtensionEnabled_onApi34AndAbove() {
        val camera = fakeCamera(CameraInfo.IMPLEMENTATION_TYPE_CAMERA2, isExtensionEnabled = true)
        val mode = CameraImplementationModeCompat.chooseCompatibleMode(camera)

        // Assert: The mode should be EXTERNAL on API 34+ where SurfaceView is supported in
        // Extension sessions.
        assertThat(mode).isEqualTo(ImplementationMode.EXTERNAL)
    }

    private fun fakeCamera(
        implementationType: String,
        isExtensionEnabled: Boolean = false,
        sessionProcessorType: Int = SessionProcessor.TYPE_CAMERA2_EXTENSION,
    ): Camera {
        val cameraInfo =
            FakeCameraInfoInternal().apply { setImplementationType(implementationType) }
        return FakeCamera(null, cameraInfo).apply {
            if (isExtensionEnabled) {
                setExtendedConfig(
                    FakeCameraConfig(
                        FakeSessionProcessor(sessionProcessorType = sessionProcessorType),
                        /* postviewSupported= */ false,
                        /* captureProcessProgressSupported= */ false,
                        Identifier.create(Any()),
                    )
                )
            }
        }
    }
}
