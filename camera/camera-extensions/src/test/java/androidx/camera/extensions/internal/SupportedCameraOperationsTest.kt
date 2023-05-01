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
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.util.Pair
import android.util.Range
import android.util.Size
import androidx.camera.camera2.internal.Camera2CameraInfoImpl
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.impl.RestrictedCameraControl
import androidx.camera.extensions.impl.CaptureStageImpl
import androidx.camera.extensions.impl.ImageCaptureExtenderImpl
import androidx.camera.extensions.impl.advanced.AdvancedExtenderImpl
import androidx.camera.extensions.impl.advanced.Camera2SessionConfigImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceConfigurationImpl
import androidx.camera.extensions.impl.advanced.OutputSurfaceImpl
import androidx.camera.extensions.impl.advanced.RequestProcessorImpl
import androidx.camera.extensions.impl.advanced.SessionProcessorImpl
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager

@RunWith(ParameterizedRobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = arrayOf("androidx.camera.extensions.internal")
)
class SupportedCameraOperationsTest(
    private val extenderType: String
) {
    val context = RuntimeEnvironment.getApplication()

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun createTestSet(): List<String> {
            return listOf("basic", "advanced")
        }
    }
    private fun setCameraXExtensionsVersion(version: String) {
        val field = VersionName::class.java.getDeclaredField("CURRENT")
        field.isAccessible = true
        field[null] = VersionName(version)
    }

    private fun setExtensionRuntimeVersion(version: String) {
        ExtensionVersion.injectInstance(object : ExtensionVersion() {
            override fun isAdvancedExtenderSupportedInternal(): Boolean {
                return false
            }

            override fun getVersionObject(): Version {
                return Version.parse(version)!!
            }
        })
    }

    @Before
    fun setUp() {
        setupCameraCharacteristics()
        setCameraXExtensionsVersion("1.3.0")
        setExtensionRuntimeVersion("1.3.0")
    }

    private fun setupCameraCharacteristics() {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_FACING_BACK
        )
        shadowCharacteristics.set(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, arrayOf(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE,
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            )
        )
        val cameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager
        (Shadow.extract<Any>(cameraManager) as ShadowCameraManager)
            .addCamera("0", characteristics)
    }

    private fun testSupportedCameraOperation(
        supportedCaptureRequestKeys: List<CaptureRequest.Key<out Any>>,
        @RestrictedCameraControl.CameraOperation expectSupportedOperations: Set<Int>
    ) {
        var vendorExtender: VendorExtender? = null
        if (extenderType == "basic") {
            val fakeImageCaptureExtenderImpl = FakeImageCaptureExtenderImpl(
                supportedRequestKeys = supportedCaptureRequestKeys
            )
            vendorExtender = BasicVendorExtender(fakeImageCaptureExtenderImpl, null)
        } else if (extenderType == "advanced") {
            val fakeAdvancedExtenderImpl = FakeAdvancedVendorExtenderImpl(
                supportedRequestKeys = supportedCaptureRequestKeys
            )
            vendorExtender = AdvancedVendorExtender(fakeAdvancedExtenderImpl)
        }

        val cameraInfo = Camera2CameraInfoImpl("0", CameraManagerCompat.from(context))
        vendorExtender!!.init(cameraInfo)
        val sessionProcessor = vendorExtender.createSessionProcessor(context)!!
        assertThat(sessionProcessor.supportedCameraOperations)
            .containsExactlyElementsIn(expectSupportedOperations)
    }

    @Config(minSdk = Build.VERSION_CODES.R)
    @Test
    fun supportedCameraOperations_zoomIsEnabled_androidR() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_ZOOM_RATIO
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.ZOOM
            )
        )
    }

    @Config(minSdk = Build.VERSION_CODES.R)
    @Test
    fun supportedCameraOperations_cropregion_zoomIsEnabled_androidR() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.SCALER_CROP_REGION
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.ZOOM
            )
        )
    }

    @Config(maxSdk = Build.VERSION_CODES.Q)
    @Test
    fun supportedCameraOperations_zoomIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.SCALER_CROP_REGION
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.ZOOM
            )
        )
    }

    @Test
    fun supportedCameraOperations_autoFocusIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_TRIGGER
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.AUTO_FOCUS
            )
        )
    }

    @Test
    fun supportedCameraOperations_afRegionIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AF_REGIONS,
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.AF_REGION
            )
        )
    }

    @Test
    fun supportedCameraOperations_aeRegionIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AE_REGIONS,
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.AE_REGION
            )
        )
    }

    @Test
    fun supportedCameraOperations_awbRegionIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AWB_REGIONS,
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.AWB_REGION
            )
        )
    }

    @Test
    fun supportedCameraOperations_torchIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.FLASH_MODE
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.TORCH
            )
        )
    }

    @Test
    fun supportedCameraOperations_flashIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.FLASH
            )
        )
    }

    @Test
    fun supportedCameraOperations_exposureCompensationIsEnabled() {
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
            ),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.EXPOSURE_COMPENSATION
            )
        )
    }

    // For Basic extender under 1.3.0, ensures all operations are supported
    @Test
    fun supportedCameraOperations_allOperationsEnabled_basic1_2_and_below() {
        assumeTrue(extenderType == "basic")
        setExtensionRuntimeVersion("1.2.0")
        setCameraXExtensionsVersion("1.3.0")
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = emptyList(),
            expectSupportedOperations = setOf(
                RestrictedCameraControl.ZOOM,
                RestrictedCameraControl.AUTO_FOCUS,
                RestrictedCameraControl.TORCH,
                RestrictedCameraControl.AF_REGION,
                RestrictedCameraControl.AE_REGION,
                RestrictedCameraControl.AWB_REGION,
                RestrictedCameraControl.EXPOSURE_COMPENSATION,
                RestrictedCameraControl.FLASH,
            )
        )
    }

    @Test
    fun supportedCameraOperations_allOperationsDisabled_advanced1_2_and_below() {
        assumeTrue(extenderType == "advanced")
        setExtensionRuntimeVersion("1.2.0")
        setCameraXExtensionsVersion("1.3.0")
        testSupportedCameraOperation(
            supportedCaptureRequestKeys = listOf(
                CaptureRequest.SCALER_CROP_REGION,
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_REGIONS,
                CaptureRequest.CONTROL_AE_REGIONS,
                CaptureRequest.CONTROL_AWB_REGIONS,
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.FLASH_MODE,
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION
            ),
            expectSupportedOperations = emptySet() // No ops should be supported.
        )
    }

    private class FakeImageCaptureExtenderImpl(
        val supportedRequestKeys: List<CaptureRequest.Key<out Any>>
    ) : ImageCaptureExtenderImpl {
        override fun isExtensionAvailable(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics
        ): Boolean = true
        override fun init(cameraId: String, cameraCharacteristics: CameraCharacteristics) {
        }
        override fun getCaptureProcessor() = null
        override fun getCaptureStages(): List<CaptureStageImpl> = emptyList()
        override fun getMaxCaptureStage() = 2
        override fun getSupportedResolutions() = null
        override fun getEstimatedCaptureLatencyRange(size: Size?) = null
        override fun getAvailableCaptureRequestKeys(): List<CaptureRequest.Key<out Any>> {
            return supportedRequestKeys
        }

        override fun getAvailableCaptureResultKeys(): List<CaptureResult.Key<Any>> {
            return mutableListOf()
        }

        override fun getSupportedPostviewResolutions(
            captureSize: Size
        ): MutableList<Pair<Int, Array<Size>>>? = null

        override fun isCaptureProcessProgressAvailable() = false

        override fun getRealtimeCaptureLatency(): Pair<Long, Long>? = null
        override fun isPostviewAvailable() = false
        override fun onInit(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics,
            context: Context
        ) {}

        override fun onDeInit() {}
        override fun onPresetSession(): CaptureStageImpl? = null

        override fun onEnableSession(): CaptureStageImpl? = null

        override fun onDisableSession(): CaptureStageImpl? = null
        override fun onSessionType(): Int = SessionConfiguration.SESSION_REGULAR
    }

    private class FakeAdvancedVendorExtenderImpl(
        val supportedRequestKeys: List<CaptureRequest.Key<out Any>>
    ) : AdvancedExtenderImpl {
        override fun isExtensionAvailable(
            cameraId: String,
            characteristicsMap: MutableMap<String, CameraCharacteristics>
        ): Boolean = true

        override fun init(
            cameraId: String,
            characteristicsMap: MutableMap<String, CameraCharacteristics>
        ) {}
        override fun getEstimatedCaptureLatencyRange(
            cameraId: String,
            captureOutputSize: Size?,
            imageFormat: Int
        ): Range<Long>? = null
        override fun getSupportedPreviewOutputResolutions(
            cameraId: String
        ): Map<Int, MutableList<Size>> = emptyMap()
        override fun getSupportedCaptureOutputResolutions(
            cameraId: String
        ): Map<Int, MutableList<Size>> = emptyMap()

        override fun getSupportedPostviewResolutions(
            captureSize: Size
        ): Map<Int, MutableList<Size>> = emptyMap()
        override fun getSupportedYuvAnalysisResolutions(cameraId: String) = null
        override fun createSessionProcessor(): SessionProcessorImpl = DummySessionProcessorImpl()
        override fun getAvailableCaptureRequestKeys():
            List<CaptureRequest.Key<out Any>> = supportedRequestKeys

        override fun getAvailableCaptureResultKeys(): List<CaptureResult.Key<Any>> = emptyList()
        override fun isCaptureProcessProgressAvailable() = false
        override fun isPostviewAvailable() = false
    }

    private class DummySessionProcessorImpl : SessionProcessorImpl {
        override fun initSession(
            cameraId: String,
            cameraCharacteristicsMap: MutableMap<String, CameraCharacteristics>,
            context: Context,
            surfaceConfigs: OutputSurfaceConfigurationImpl
        ): Camera2SessionConfigImpl {
            throw UnsupportedOperationException("Not supported")
        }
        override fun initSession(
            cameraId: String,
            cameraCharacteristicsMap: MutableMap<String, CameraCharacteristics>,
            context: Context,
            previewSurfaceConfig: OutputSurfaceImpl,
            imageCaptureSurfaceConfig: OutputSurfaceImpl,
            imageAnalysisSurfaceConfig: OutputSurfaceImpl?
        ): Camera2SessionConfigImpl {
            throw UnsupportedOperationException("Not supported")
        }

        override fun deInitSession() {
            throw UnsupportedOperationException("Not supported")
        }

        override fun setParameters(parameters: MutableMap<CaptureRequest.Key<*>, Any>) {
            throw UnsupportedOperationException("Not supported")
        }

        override fun startTrigger(
            triggers: MutableMap<CaptureRequest.Key<*>, Any>,
            callback: SessionProcessorImpl.CaptureCallback
        ): Int {
            throw UnsupportedOperationException("Not supported")
        }

        override fun onCaptureSessionStart(requestProcessor: RequestProcessorImpl) {
            throw UnsupportedOperationException("Not supported")
        }

        override fun onCaptureSessionEnd() {
            throw UnsupportedOperationException("Not supported")
        }

        override fun startRepeating(callback: SessionProcessorImpl.CaptureCallback): Int {
            throw UnsupportedOperationException("Not supported")
        }

        override fun stopRepeating() {
            throw UnsupportedOperationException("Not supported")
        }

        override fun startCapture(callback: SessionProcessorImpl.CaptureCallback): Int {
            throw UnsupportedOperationException("Not supported")
        }

        override fun startCaptureWithPostview(callback: SessionProcessorImpl.CaptureCallback): Int {
            throw UnsupportedOperationException("Not supported")
        }

        override fun abortCapture(captureSequenceId: Int) {
            throw UnsupportedOperationException("Not supported")
        }

        override fun getRealtimeCaptureLatency(): Pair<Long, Long>? {
            throw UnsupportedOperationException("Not supported")
        }
    }
}