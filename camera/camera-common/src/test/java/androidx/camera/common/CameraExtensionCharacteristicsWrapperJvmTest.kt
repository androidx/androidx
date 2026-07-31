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

package androidx.camera.common

import android.content.Context
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
public final class CameraExtensionCharacteristicsWrapperJvmTest {

    @Test
    @Config(sdk = [30])
    public fun loadFrom_returnsNull_onApiLevel30() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val wrapperFromContext =
            CameraExtensionCharacteristicsWrappers.loadFrom(
                context,
                CameraId("0"),
                CameraExtensionCharacteristics.EXTENSION_BOKEH,
            )
        val wrapperFromManager =
            CameraExtensionCharacteristicsWrappers.loadFrom(
                cameraManager,
                CameraId("0"),
                CameraExtensionCharacteristics.EXTENSION_BOKEH,
            )

        assertThat(wrapperFromContext).isNull()
        assertThat(wrapperFromManager).isNull()
    }

    @Test
    @Config(sdk = [30])
    public fun loadAvailableExtensionsFrom_returnsEmptyMap_onApiLevel30() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val extensionsFromContext =
            CameraExtensionCharacteristicsWrappers.loadAvailableExtensionsFrom(
                context,
                CameraId("0"),
            )
        val extensionsFromManager =
            CameraExtensionCharacteristicsWrappers.loadAvailableExtensionsFrom(
                cameraManager,
                CameraId("0"),
            )

        assertThat(extensionsFromContext).isEmpty()
        assertThat(extensionsFromManager).isEmpty()
    }
}
