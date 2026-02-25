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

package androidx.camera.camera2.compat.quirk

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.backported.fixes.BackportedFixManager
import androidx.core.backported.fixes.KnownIssues
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class PixelJpegRSupportedQuirkTest(private val brand: String, private val fingerprint: String) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Brand: {0}, Fingerprint: {1}")
        fun data() =
            listOf(
                // Pixel 8, Known Fingerprint -> Quirk Loaded, Issue Fixed -> Result True
                arrayOf(
                    "google",
                    "google/blazer/blazer:16/BD3A.250721.001.B7/13955164:user/release-keys",
                ),

                // Pixel 8, Unknown Fingerprint -> Quirk Loaded, Issue Not Fixed -> Result False
                // Note: Quirk is only loaded on API 34+. On older APIs, it's not loaded, so result
                // is True.
                arrayOf(
                    "google",
                    "google/blazer/blazer:16/BD3A.250721.001.B7/13999999:user/release-keys",
                ),

                // Samsung -> Quirk Not Loaded (Not Google) -> Result True (No Issue)
                arrayOf(
                    "Samsung",
                    "samsung/blazer/blazer:16/BD3A.250721.001.B7/13955164:user/release-keys",
                ),
            )
    }

    @Before
    fun setUp() {
        ReflectionHelpers.setStaticField(Build::class.java, "BRAND", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "MANUFACTURER", brand)
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", fingerprint)
    }

    @Test
    fun isColorToneIssueFixed() {
        val quirk = DeviceQuirks[PixelJpegRSupportedQuirk::class.java]
        val isFixed = quirk == null
        assertThat(isFixed).isEqualTo(expectedFixed())
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun returnCorrectSupportedOutputFormats_fromImageCaptureCapabilities(): Unit = runBlocking {
        val cameraProvider = setupCameraAndRetrieveCameraProvider()

        val cameraInfo =
            CameraSelector.DEFAULT_BACK_CAMERA.filter(cameraProvider.availableCameraInfos).first()

        val capabilities = ImageCapture.getImageCaptureCapabilities(cameraInfo)
        val supportedFormats = capabilities.supportedOutputFormats

        if (expectedFixed()) {
            assertThat(supportedFormats).contains(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        } else {
            assertThat(supportedFormats).doesNotContain(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        }

        cameraProvider.shutdownAsync().await()
    }

    private fun expectedFixed(): Boolean {
        return BackportedFixManager().isFixed(KnownIssues.KI_398591036)
    }

    private fun setupCameraAndRetrieveCameraProvider(): ProcessCameraProvider = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val shadowCameraManager = shadowOf(cameraManager)

        shadowCameraManager.addCamera("0", createFakeCameraCharacteristics())

        ProcessCameraProvider.getInstance(context).await()
    }

    private fun createFakeCameraCharacteristics(): CameraCharacteristics {
        val cameraCharacteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(cameraCharacteristics).apply {
            set(CameraCharacteristics.LENS_FACING, CameraMetadata.LENS_FACING_BACK)
            set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, Rect(0, 0, 10, 10))
            set(CameraCharacteristics.SENSOR_ORIENTATION, 0)
            set(CameraCharacteristics.FLASH_INFO_AVAILABLE, false)
            set(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            )
            set(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                StreamConfigurationMapBuilder.newBuilder()
                    .addOutputSize(ImageFormat.JPEG_R, Size(1920, 1080))
                    .build(),
            )
        }
        return cameraCharacteristics
    }
}
