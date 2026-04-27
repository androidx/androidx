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

package androidx.camera.extensions

import android.content.Context
import android.hardware.camera2.CameraExtensionCharacteristics
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 33)
class ExtensionSessionConfigTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var extensionsManager: ExtensionsManager

    @Before
    fun setUp() {
        val cameraInfo =
            FakeCameraInfoInternal().apply {
                setSupportedExtensions(setOf(CameraExtensionCharacteristics.EXTENSION_BOKEH))
            }
        val cameraProvider =
            object : CameraProvider {
                override val availableCameraInfos: List<androidx.camera.core.CameraInfo>
                    get() = listOf(cameraInfo)

                override val availableConcurrentCameraInfos:
                    List<List<androidx.camera.core.CameraInfo>>
                    get() = emptyList()

                override val isConcurrentCameraModeOn: Boolean = false

                override fun hasCamera(cameraSelector: CameraSelector): Boolean = true

                override fun getCameraInfo(
                    cameraSelector: CameraSelector
                ): androidx.camera.core.CameraInfo = cameraInfo
            }

        extensionsManager = ExtensionsManager.getInstanceAsync(context, cameraProvider).get()
    }

    @After
    fun tearDown() {
        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun builder_build_defaultIsAutoRotationEnabledFalse() {
        val config = ExtensionSessionConfig.Builder(ExtensionMode.BOKEH, extensionsManager).build()

        assertThat(config.isAutoRotationEnabled).isFalse()
    }

    @Test
    fun builder_setAutoRotationEnabled_configHasIsAutoRotationEnabledTrue() {
        val config =
            ExtensionSessionConfig.Builder(ExtensionMode.BOKEH, extensionsManager)
                .setAutoRotationEnabled(true)
                .build()

        assertThat(config.isAutoRotationEnabled).isTrue()
    }

    @Test
    fun constructor_isAutoRotationEnabledPassedToSuper() {
        val config =
            ExtensionSessionConfig(
                mode = ExtensionMode.BOKEH,
                extensionsManager = extensionsManager,
                isAutoRotationEnabled = true,
            )

        assertThat(config.isAutoRotationEnabled).isTrue()
    }
}
