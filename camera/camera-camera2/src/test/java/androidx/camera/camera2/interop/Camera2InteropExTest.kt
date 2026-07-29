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

package androidx.camera.camera2.interop

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.impl.Camera2ImplConfig
import androidx.camera.camera2.impl.createSessionParameterOption
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.camera2.pipe.testing.HighEndDeviceTemplate
import androidx.camera.camera2.testing.FakeCameraInfoAdapterCreator.createCameraInfoAdapter
import androidx.camera.camera2.testing.FakeCameraProperties
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.InteropConfigurator
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.imageAnalysis
import androidx.camera.core.imageCapture
import androidx.camera.core.impl.MutableConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.preview
import androidx.camera.core.sessionConfig
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoOutput
import androidx.camera.video.videoCapture
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
@OptIn(ExperimentalCamera2Interop::class)
@Suppress("NewApi")
class Camera2InteropExTest {

    private val videoOutput = VideoOutput { request -> request.willNotProvideSurface() }

    @Test
    fun previewBuilder_camera2Interop() {
        val builder =
            Preview.Builder().camera2Interop {
                physicalCameraId = "0"
                streamUseCase = 3
            }
        val config = Camera2ImplConfig(builder.interopMutableConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("0")
        assertThat(config.getStreamUseCase(-1)).isEqualTo(3)
    }

    @Test
    fun imageAnalysisBuilder_camera2Interop() {
        val builder = ImageAnalysis.Builder().camera2Interop { physicalCameraId = "1" }
        val config = Camera2ImplConfig(builder.interopMutableConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("1")
    }

    @Test
    fun videoCaptureBuilder_camera2Interop() {
        val builder = VideoCapture.Builder(videoOutput).camera2Interop { physicalCameraId = "2" }
        val config = Camera2ImplConfig(builder.useCaseConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("2")
    }

    @Test
    fun imageCaptureBuilder_camera2Interop() {
        val builder =
            ImageCapture.Builder().camera2Interop {
                stillCaptureRequestTemplateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                physicalCameraId = "0"
            }
        val config = Camera2ImplConfig(builder.useCaseConfig)
        assertThat(config.getStillCaptureTemplateType(-1))
            .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("0")
    }

    @Test
    fun previewDsl_camera2Interop_properties() {
        val preview = preview {
            camera2Interop {
                physicalCameraId = "0"
                streamUseCase = 3
                mirrorMode = 1
                timestampBase = 2
                dynamicRangeProfile = 4L
                surfaceGroupId = 5
            }
        }
        val config = Camera2ImplConfig(preview.currentConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("0")
        assertThat(config.getStreamUseCase(-1)).isEqualTo(3)
        assertThat(config.getMirrorMode(-1)).isEqualTo(1)
        assertThat(config.getTimestampBase(-1)).isEqualTo(2)
        assertThat(config.getDynamicRangeProfile(-1)).isEqualTo(4L)
        assertThat(config.getSurfaceGroupId(-1)).isEqualTo(5)
    }

    @Test
    fun imageAnalysisDsl_camera2Interop() {
        val imageAnalysis = imageAnalysis { camera2Interop { physicalCameraId = "1" } }
        val config = Camera2ImplConfig(imageAnalysis.currentConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("1")
    }

    @Test
    fun imageAnalysisDsl_camera2Interop_properties() {
        val imageAnalysis = imageAnalysis { camera2Interop { physicalCameraId = "1" } }
        val config = Camera2ImplConfig(imageAnalysis.currentConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("1")
    }

    @Test
    fun videoCaptureDsl_camera2Interop() {
        val videoCapture = videoCapture(videoOutput) { camera2Interop { physicalCameraId = "2" } }
        val config = Camera2ImplConfig(videoCapture.currentConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("2")
    }

    @Test
    fun videoCaptureDsl_camera2Interop_properties() {
        val videoCapture = videoCapture(videoOutput) { camera2Interop { physicalCameraId = "2" } }
        val config = Camera2ImplConfig(videoCapture.currentConfig)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("2")
    }

    @Test
    fun imageCaptureDsl_camera2Interop_properties() {
        val imageCapture = imageCapture {
            camera2Interop {
                stillCaptureRequestTemplateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                physicalCameraId = "0"
            }
        }
        val config = Camera2ImplConfig(imageCapture.currentConfig)
        assertThat(config.getStillCaptureTemplateType(-1))
            .isEqualTo(CameraDevice.TEMPLATE_STILL_CAPTURE)
        assertThat(config.getPhysicalCameraId(null)).isEqualTo("0")
    }

    @Test
    fun sessionConfigBuilder_camera2Interop() {
        val preview = Preview.Builder().build()
        val builder =
            SessionConfig.Builder(preview).camera2Interop {
                sessionType = 4
                colorSpace = 5
            }
        val sessionConfig = builder.build()
        val config = Camera2ImplConfig(sessionConfig.interopConfig)
        assertThat(config.getSessionType(-1)).isEqualTo(4)
        assertThat(config.getColorSpace(-1)).isEqualTo(5)
    }

    @Test
    fun sessionConfigDsl_camera2Interop_properties() {
        val preview = Preview.Builder().build()
        val sessionConfig =
            sessionConfig(listOf(preview)) {
                camera2Interop {
                    sessionType = 4
                    colorSpace = 5
                    repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_PREVIEW
                }
            }
        val config = Camera2ImplConfig(sessionConfig.interopConfig)
        assertThat(config.getSessionType(-1)).isEqualTo(4)
        assertThat(config.getColorSpace(-1)).isEqualTo(5)
        assertThat(config.getCaptureRequestTemplate(-1)).isEqualTo(CameraDevice.TEMPLATE_PREVIEW)
    }

    @Test
    fun applyCamera2InteropAsync() {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        val future =
            cameraControl.applyCamera2InteropAsync {
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF,
                )
            }
        assertThat(future.isDone).isTrue()
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    fun applyCamera2InteropAsync_indexingOperatorTargets() {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        val future =
            cameraControl.applyCamera2InteropAsync {
                captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
            }
        assertThat(future.isDone).isTrue()
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    fun applyCamera2InteropAsync_properties() {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        val future =
            cameraControl.applyCamera2InteropAsync {
                repeatingCaptureRequestTemplate = CameraDevice.TEMPLATE_RECORD
            }
        assertThat(future.isDone).isTrue()
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getCaptureRequestTemplate(-1)).isEqualTo(CameraDevice.TEMPLATE_RECORD)
    }

    @Test
    fun applyCamera2InteropAsync_clearCaptureRequestOption() {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        cameraControl.applyCamera2InteropAsync {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF,
            )
        }
        val configBefore = Camera2ImplConfig(mutableConfig)
        assertThat(configBefore.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)

        cameraControl.applyCamera2InteropAsync {
            clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
        }
        val configAfter = Camera2ImplConfig(mutableConfig)
        assertThat(configAfter.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, null))
            .isNull()
    }

    @Test
    fun applyCamera2InteropAsync_clearAllCaptureRequestOptions() {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        cameraControl.applyCamera2InteropAsync {
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF,
            )
            setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF,
            )
        }
        cameraControl.applyCamera2InteropAsync { clearAllCaptureRequestOptions() }
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, null)).isNull()
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, null)).isNull()
    }

    @Test
    fun applyCamera2InteropAsync_repeatingCaptureCallback() {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        val callback = object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {}
        cameraControl.applyCamera2InteropAsync { repeatingCaptureCallback = callback }
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getSessionRepeatingCaptureCallback(null)).isNotNull()
    }

    @Test
    fun applyCamera2Interop_suspend_setsCaptureRequestOptions() = runBlocking {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        cameraControl.applyCamera2Interop {
            captureRequest[CaptureRequest.CONTROL_AE_MODE] = CaptureRequest.CONTROL_AE_MODE_OFF
        }
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
    }

    @Test
    fun applyCamera2Interop_suspend_repeatingCaptureCallback() = runBlocking {
        val mutableConfig = androidx.camera.core.impl.MutableOptionsBundle.create()
        val cameraControl = createCameraControl(mutableConfig)
        val callback = object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {}
        cameraControl.applyCamera2Interop { repeatingCaptureCallback = callback }
        val config = Camera2ImplConfig(mutableConfig)
        assertThat(config.getSessionRepeatingCaptureCallback(null)).isNotNull()
    }

    @Test
    fun imageCaptureDsl_camera2Interop_stillCaptureOptions() {
        val callback = object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {}
        val imageCapture = imageCapture {
            camera2Interop {
                stillCaptureRequest[CaptureRequest.CONTROL_AE_MODE] =
                    CaptureRequest.CONTROL_AE_MODE_OFF
                stillCaptureCallback = callback
            }
        }
        val config = Camera2ImplConfig(imageCapture.currentConfig)
        assertThat(config.getStillCaptureOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(config.retrieveOption(Camera2ImplConfig.STILL_CAPTURE_CALLBACK_OPTION, null))
            .isNotNull()
    }

    @Test
    fun sessionConfigDsl_camera2Interop_sessionOptions() {
        val preview = Preview.Builder().build()
        val deviceCallback =
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {}

                override fun onDisconnected(camera: CameraDevice) {}

                override fun onError(camera: CameraDevice, error: Int) {}
            }
        val sessionCallback =
            object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {}

                override fun onConfigureFailed(
                    session: android.hardware.camera2.CameraCaptureSession
                ) {}
            }
        val sessionConfig =
            sessionConfig(listOf(preview)) {
                camera2Interop {
                    sessionParameter[CaptureRequest.CONTROL_AE_MODE] =
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    deviceStateCallback = deviceCallback
                    sessionStateCallback = sessionCallback
                    captureRequest[CaptureRequest.CONTROL_AF_MODE] =
                        CaptureRequest.CONTROL_AF_MODE_OFF
                }
            }
        val config = Camera2ImplConfig(sessionConfig.interopConfig)
        @Suppress("UNCHECKED_CAST")
        val sessionParamOption =
            CaptureRequest.CONTROL_AE_MODE.createSessionParameterOption()
                as androidx.camera.core.impl.Config.Option<Int>
        assertThat(sessionConfig.interopConfig.retrieveOption(sessionParamOption, null))
            .isEqualTo(CaptureRequest.CONTROL_AE_MODE_OFF)
        assertThat(config.getDeviceStateCallback(null)).isNotNull()
        assertThat(config.getSessionStateCallback(null)).isNotNull()
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
    }

    @Test
    fun sessionConfigDsl_camera2Interop_clearCaptureRequestOptions() {
        val preview = Preview.Builder().build()
        val sessionConfig =
            sessionConfig(listOf(preview)) {
                camera2Interop {
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF,
                    )
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF,
                    )
                    clearCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE)
                }
            }
        val config = Camera2ImplConfig(sessionConfig.interopConfig)
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, null)).isNull()
        assertThat(config.getCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE))
            .isEqualTo(CaptureRequest.CONTROL_AF_MODE_OFF)
    }

    @Test
    fun cameraInfo_cameraId() {
        val cameraId = "42"
        val cameraInfo: CameraInfo = createCameraInfoAdapter(cameraId = CameraId(cameraId))
        assertThat(cameraInfo.cameraId).isEqualTo(cameraId)
    }

    @Test
    fun cameraInfo_cameraCharacteristics() {
        val cameraInfo =
            createCameraInfoWithCharacteristics(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
            )
        val hardwareLevel =
            cameraInfo.cameraCharacteristics.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
            )
        assertThat(hardwareLevel)
            .isEqualTo(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
    }

    @Test
    fun string_toCameraSelector() {
        val cameraId = "42"
        val cameraSelector = cameraId.toCameraSelector()
        assertThat(cameraSelector).isNotNull()
        assertCameraSelectorSelectsCameraId(cameraSelector, cameraId)
    }

    @Test
    fun targetInterfacesAreAnnotatedWithCameraXDsl() {
        val interfaces =
            listOf(
                UseCaseCamera2Interop::class.java,
                ImageCaptureCamera2Interop::class.java,
                SessionConfigCamera2Interop::class.java,
                CameraControlCamera2Interop::class.java,
            )
        for (iface in interfaces) {
            assertThat(iface.isAnnotationPresent(androidx.camera.core.CameraXDsl::class.java))
                .isTrue()
        }
    }

    private fun createCameraInfoWithCharacteristics(cameraHardwareLevel: Int): CameraInfo {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            cameraHardwareLevel,
        )

        class FakeCamera2Metadata(
            private val delegate: FakeCameraMetadata,
            private val cameraCharacteristics: CameraCharacteristics,
        ) : androidx.camera.camera2.pipe.CameraMetadata by delegate {
            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> unwrapAs(type: Class<T>): T? {
                if (type == CameraCharacteristics::class.java) {
                    return cameraCharacteristics as T
                }
                return delegate.unwrapAs(type)
            }
        }

        val fakeMetadata =
            FakeCameraMetadata.fromTemplate(
                template = HighEndDeviceTemplate,
                characteristicsOverrides =
                    mapOf(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to cameraHardwareLevel
                    ),
            )

        return createCameraInfoAdapter(
            cameraProperties =
                FakeCameraProperties(FakeCamera2Metadata(fakeMetadata, characteristics))
        )
    }

    private fun assertCameraSelectorSelectsCameraId(
        cameraSelector: CameraSelector,
        expectedCameraId: String,
    ) {
        val cameraInfo0 = createCameraInfoAdapter(cameraId = CameraId("0"))
        val cameraInfoExpected = createCameraInfoAdapter(cameraId = CameraId(expectedCameraId))
        val cameraInfo1 = createCameraInfoAdapter(cameraId = CameraId("1"))
        val filteredCameraInfos =
            cameraSelector.filter(listOf(cameraInfo0, cameraInfoExpected, cameraInfo1))
        assertThat(filteredCameraInfos).containsExactly(cameraInfoExpected)
    }

    private fun createCameraControl(mutableConfig: MutableConfig): CameraControl {
        return object : CameraControl {
            override fun getInteropMutableConfig(): MutableConfig = mutableConfig

            override fun applyInteropAsync(
                configurator: InteropConfigurator<in CameraControl>
            ): ListenableFuture<Void> {
                configurator.configure(this)
                return Futures.immediateFuture(null)
            }

            override fun enableTorch(torch: Boolean) = Futures.immediateFuture<Void>(null)

            override fun startFocusAndMetering(action: FocusMeteringAction) = TODO()

            override fun cancelFocusAndMetering() = TODO()

            override fun setZoomRatio(ratio: Float) = TODO()

            override fun setLinearZoom(linearZoom: Float) = TODO()

            override fun setExposureCompensationIndex(value: Int) = TODO()
        }
    }
}
