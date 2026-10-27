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

package androidx.camera.view.internal.compat.quirk

import android.os.Build
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeSessionProcessor
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class SurfaceViewNotSupportedInExtensionSessionQuirkTest {

    @Test
    @Config(maxSdk = Build.VERSION_CODES.TIRAMISU)
    fun load_returnsTrue_onApi33AndBelow() {
        assertThat(SurfaceViewNotSupportedInExtensionSessionQuirk.load()).isTrue()
        assertThat(DeviceQuirks.get(SurfaceViewNotSupportedInExtensionSessionQuirk::class.java))
            .isNotNull()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun load_returnsFalse_onApi34AndAbove() {
        assertThat(SurfaceViewNotSupportedInExtensionSessionQuirk.load()).isFalse()
        assertThat(DeviceQuirks.get(SurfaceViewNotSupportedInExtensionSessionQuirk::class.java))
            .isNull()
    }

    @Test
    fun isSurfaceViewNotSupported_returnsTrue_whenCamera2ExtensionSessionProcessorPresent() {
        val quirk = SurfaceViewNotSupportedInExtensionSessionQuirk()
        val cameraConfig =
            FakeCameraConfig(
                sessionProcessor =
                    FakeSessionProcessor(
                        sessionProcessorType = SessionProcessor.TYPE_CAMERA2_EXTENSION
                    )
            )

        assertThat(quirk.isSurfaceViewNotSupported(cameraConfig)).isTrue()
    }

    @Test
    fun isSurfaceViewNotSupported_returnsFalse_whenNonCamera2ExtensionSessionProcessorPresent() {
        val quirk = SurfaceViewNotSupportedInExtensionSessionQuirk()
        val cameraConfig =
            FakeCameraConfig(
                sessionProcessor =
                    FakeSessionProcessor(sessionProcessorType = SessionProcessor.TYPE_DEFAULT)
            )

        assertThat(quirk.isSurfaceViewNotSupported(cameraConfig)).isFalse()
    }

    @Test
    fun isSurfaceViewNotSupported_returnsFalse_whenSessionProcessorAbsent() {
        val quirk = SurfaceViewNotSupportedInExtensionSessionQuirk()
        val cameraConfig = FakeCameraConfig()

        assertThat(quirk.isSurfaceViewNotSupported(cameraConfig)).isFalse()
    }
}
