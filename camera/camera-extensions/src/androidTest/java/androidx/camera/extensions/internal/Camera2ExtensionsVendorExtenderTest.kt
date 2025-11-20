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

package androidx.camera.extensions.internal

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Size
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_VGA
import androidx.camera.extensions.ExtensionMode
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.fakes.FakeLifecycleOwner
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 31)
class Camera2ExtensionsVendorExtenderTest(
    @field:CameraSelector.LensFacing @param:CameraSelector.LensFacing private val lensFacing: Int,
    private val mode: Int,
) {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val cameraId2ExtensionCharacteristicsMap =
        Camera2ExtensionsUtil.createCameraId2CameraExtensionCharacteristicsMap(context)
    private val camera2ExtensionsVendorExtender =
        Camera2ExtensionsVendorExtender(
            mode,
            Camera2ExtensionsInfo(context.getSystemService(CameraManager::class.java)),
        )
    private val cameraId = CameraUtil.getCameraIdWithLensFacing(lensFacing)!!
    private val camera2ExtensionMode = Camera2ExtensionsUtil.convertCameraXModeToCamera2Mode(mode)
    private val cameraExtensionsCharacteristics = cameraId2ExtensionCharacteristicsMap[cameraId]!!
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraInfo: CameraInfo

    @Before
    fun setUp() {
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            cameraInfo =
                cameraProvider
                    .bindToLifecycle(
                        FakeLifecycleOwner(),
                        CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                    )
                    .cameraInfo
        }
    }

    @After
    fun teardown() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @Test
    fun isExtensionsAvailable_returnCorrectValue() {
        assertThat(camera2ExtensionsVendorExtender.isExtensionAvailable(cameraId, emptyMap()))
            .isEqualTo(
                cameraExtensionsCharacteristics.supportedExtensions.contains(camera2ExtensionMode)
            )
    }

    @Test
    fun getSupportedPreviewOutputResolutions_returnCorrectValue() {
        checkAvailabilityAndInit()
        camera2ExtensionsVendorExtender.supportedPreviewOutputResolutions.forEach {
            checkSupportedOutputSizes(it.first, it.second.toList())
        }
    }

    @Test
    fun getSupportedCaptureOutputResolutions_returnCorrectValue() {
        checkAvailabilityAndInit()
        camera2ExtensionsVendorExtender.supportedCaptureOutputResolutions.forEach {
            checkSupportedOutputSizes(it.first, it.second.toList())
        }
    }

    private fun checkSupportedOutputSizes(format: Int, supportedSizes: List<Size>) {
        assertThat(supportedSizes)
            .containsExactlyElementsIn(
                if (format != ImageFormat.PRIVATE) {
                    cameraExtensionsCharacteristics.getExtensionSupportedSizes(
                        camera2ExtensionMode,
                        format,
                    )
                } else {
                    cameraExtensionsCharacteristics.getExtensionSupportedSizes(
                        camera2ExtensionMode,
                        SurfaceTexture::class.java,
                    )
                }
            )
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun getSupportedPostviewOutputResolutions_returnCorrectValue() {
        checkAvailabilityAndInit()
        camera2ExtensionsVendorExtender.getSupportedPostviewResolutions(RESOLUTION_VGA).forEach {
            val format = it.key
            val supportedSizes = it.value.toList()
            assertThat(supportedSizes)
                .containsExactlyElementsIn(
                    cameraExtensionsCharacteristics.getPostviewSupportedSizes(
                        camera2ExtensionMode,
                        RESOLUTION_VGA,
                        format,
                    )
                )
        }
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun isPostviewAvailable_returnCorrectValue() {
        checkAvailabilityAndInit()
        assertThat(camera2ExtensionsVendorExtender.isPostviewAvailable)
            .isEqualTo(cameraExtensionsCharacteristics.isPostviewAvailable(camera2ExtensionMode))
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun isCaptureProcessProgressAvailable_returnCorrectValue() {
        checkAvailabilityAndInit()
        assertThat(camera2ExtensionsVendorExtender.isCaptureProcessProgressAvailable)
            .isEqualTo(
                cameraExtensionsCharacteristics.isCaptureProcessProgressAvailable(
                    camera2ExtensionMode
                )
            )
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun isExtensionStrengthAvailable_returnCorrectValue() {
        checkAvailabilityAndInit()
        assertThat(camera2ExtensionsVendorExtender.isExtensionStrengthAvailable)
            .isEqualTo(
                cameraExtensionsCharacteristics
                    .getAvailableCaptureRequestKeys(camera2ExtensionMode)
                    .contains(CaptureRequest.EXTENSION_STRENGTH)
            )
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun isCurrentExtensionModeAvailable_returnCorrectValue() {
        checkAvailabilityAndInit()
        assertThat(camera2ExtensionsVendorExtender.isCurrentExtensionModeAvailable)
            .isEqualTo(
                cameraExtensionsCharacteristics
                    .getAvailableCaptureResultKeys(camera2ExtensionMode)
                    .contains(CaptureResult.EXTENSION_CURRENT_TYPE)
            )
    }

    private fun checkAvailabilityAndInit() {
        assumeTrue(camera2ExtensionsVendorExtender.isExtensionAvailable(cameraId, emptyMap()))
        camera2ExtensionsVendorExtender.init(cameraInfo)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing = {0}, mode = {1}")
        fun data(): Collection<Array<Any>> =
            mutableListOf<Array<Any>>().apply {
                listOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT).forEach {
                    lensFacing ->
                    CameraUtil.getCameraIdWithLensFacing(lensFacing)?.let {
                        listOf(
                                ExtensionMode.BOKEH,
                                ExtensionMode.HDR,
                                ExtensionMode.NIGHT,
                                ExtensionMode.FACE_RETOUCH,
                                ExtensionMode.AUTO,
                            )
                            .forEach { mode -> add(arrayOf(lensFacing, mode)) }
                    }
                }
            }
    }
}
