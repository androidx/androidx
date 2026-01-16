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

package androidx.camera.extensions.internal

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.core.impl.AdapterCameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 33)
class SupportedCameraOperationsTest {
    val context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        setupCameraCharacteristics()
    }

    private fun setupCameraCharacteristics() {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.LENS_FACING,
            CameraCharacteristics.LENS_FACING_BACK,
        )
        shadowCharacteristics.set(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
            arrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA,
            ),
        )
        val cameraManager =
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        (Shadow.extract<Any>(cameraManager) as ShadowCameraManager).addCamera("0", characteristics)
    }

    private fun testSupportedCameraOperation(
        supportedCaptureRequestKeys: List<CaptureRequest.Key<out Any>>,
        @AdapterCameraInfo.CameraOperation expectSupportedOperations: Set<Int>,
    ) {
        val fakeCamera2ExtensionsInfo: Camera2ExtensionsInfoProvider =
            mock(Camera2ExtensionsInfoProvider::class.java)
        val fakeCameraExtensionCharacteristics = mock(CameraExtensionCharacteristics::class.java)
        `when`(fakeCameraExtensionCharacteristics.getAvailableCaptureRequestKeys(anyInt()))
            .thenReturn(supportedCaptureRequestKeys.toSet())
        `when`(fakeCamera2ExtensionsInfo.getExtensionCharacteristics(anyString()))
            .thenReturn(fakeCameraExtensionCharacteristics)
        `when`(fakeCamera2ExtensionsInfo.isExtensionAvailable(anyString(), anyInt()))
            .thenReturn(true)
        val vendorExtender =
            Camera2ExtensionsVendorExtender(ExtensionMode.NIGHT, fakeCamera2ExtensionsInfo)
        val cameraInfo = FakeCameraInfoInternal("0", context)
        vendorExtender.init(cameraInfo)
        val sessionProcessor = vendorExtender.createSessionProcessor(context)!!
        assertThat(sessionProcessor.supportedCameraOperations)
            .containsExactlyElementsIn(expectSupportedOperations)
    }

    @Test
    fun supportedCameraOperations_zoomIsEnabled_androidR() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.CONTROL_ZOOM_RATIO),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_ZOOM),
        )
    }

    @Test
    fun supportedCameraOperations_cropregion_zoomIsEnabled_androidR() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.SCALER_CROP_REGION),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_ZOOM),
        )
    }

    @Test
    fun supportedCameraOperations_autoFocusIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys =
                listOf(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_TRIGGER),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_AUTO_FOCUS),
        )
    }

    @Test
    fun supportedCameraOperations_afRegionIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.CONTROL_AF_REGIONS),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_AF_REGION),
        )
    }

    @Test
    fun supportedCameraOperations_aeRegionIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.CONTROL_AE_REGIONS),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_AE_REGION),
        )
    }

    @Test
    fun supportedCameraOperations_awbRegionIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.CONTROL_AWB_REGIONS),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_AWB_REGION),
        )
    }

    @Test
    fun supportedCameraOperations_torchIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys =
                listOf(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_TORCH),
        )
    }

    @Test
    fun supportedCameraOperations_flashIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys =
                listOf(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                ),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_FLASH),
        )
    }

    @Test
    fun supportedCameraOperations_exposureCompensationIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION),
            expectSupportedOperations =
                setOf(AdapterCameraInfo.CAMERA_OPERATION_EXPOSURE_COMPENSATION),
        )
    }

    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun supportedCameraOperations_extensionStrengthIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(CaptureRequest.EXTENSION_STRENGTH),
            expectSupportedOperations = setOf(AdapterCameraInfo.CAMERA_OPERATION_EXTENSION_STRENGTH),
        )
    }
}
