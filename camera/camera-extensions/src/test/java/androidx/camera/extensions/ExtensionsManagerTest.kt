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
import android.graphics.ImageFormat
import android.hardware.camera2.CameraExtensionCharacteristics
import android.util.Range
import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities
import androidx.camera.testing.impl.fakes.FakeCameraExtensionCapabilities.CaptureConfig
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 33)
class ExtensionsManagerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var extensionsManager: ExtensionsManager

    @After
    fun tearDown() {
        if (::extensionsManager.isInitialized) {
            extensionsManager.shutdown()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun getEstimatedCaptureLatencyRangeMillis_returnsValueFromCapabilities() = runBlocking {
        val extensionMode = ExtensionMode.BOKEH
        val latencyRange = Range(100L, 500L)
        val captureSize = Size(1920, 1080)

        val capabilities =
            FakeCameraExtensionCapabilities(
                estimatedCaptureLatencyRangeMillis =
                    mapOf(CaptureConfig(captureSize, ImageFormat.JPEG) to latencyRange),
                outputSizesFormat = mapOf(ImageFormat.JPEG to setOf(captureSize)),
            )

        val cameraInfo =
            FakeCameraInfoInternal("0").apply {
                setSupportedExtensions(setOf(CameraExtensionCharacteristics.EXTENSION_BOKEH))
                setCameraExtensionCapabilities(
                    CameraExtensionCharacteristics.EXTENSION_BOKEH,
                    capabilities,
                )
            }

        extensionsManager =
            ExtensionsManager.getInstance(context, createFakeCameraProvider(cameraInfo))
        val baseCameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraInfo.lensFacing).build()

        assertThat(
                extensionsManager.getEstimatedCaptureLatencyRange(baseCameraSelector, extensionMode)
            )
            .isEqualTo(latencyRange)
    }

    private fun createFakeCameraProvider(cameraInfo: CameraInfo) =
        object : CameraProvider {
            override val availableCameraInfos: List<CameraInfo>
                get() = listOf(cameraInfo)

            override val availableConcurrentCameraInfos: List<List<CameraInfo>>
                get() = emptyList()

            override val isConcurrentCameraModeOn: Boolean = false

            override fun hasCamera(cameraSelector: CameraSelector): Boolean = true

            override fun getCameraInfo(cameraSelector: CameraSelector): CameraInfo = cameraInfo
        }
}
