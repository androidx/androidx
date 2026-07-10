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

package androidx.camera.core.impl

import android.os.Build
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraConfig
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AdapterCameraInfoTest {

    @Test
    @Config(minSdk = 31)
    fun getSupportedExtensions_delegatesToCameraInfo() {
        val extensions = setOf(1, 2, 3)
        val cameraInfo = FakeCameraInfoInternal().apply { setSupportedExtensions(extensions) }
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, FakeCameraConfig())

        assertThat(adapterCameraInfo.supportedExtensions).isEqualTo(extensions)
    }

    @Test
    @Config(minSdk = 31)
    fun getCameraExtensionCapabilities_delegatesToCameraInfo() {
        val extensionMode = 1
        val capabilities = FakeCameraExtensionCapabilities()
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setCameraExtensionCapabilities(extensionMode, capabilities)
            }
        val adapterCameraInfo = AdapterCameraInfo(cameraInfo, FakeCameraConfig())

        assertThat(adapterCameraInfo.getCameraExtensionCapabilities(extensionMode))
            .isEqualTo(capabilities)
    }
}
