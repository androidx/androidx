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

package androidx.camera.camera2.pipe.integration.impl

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.FakeTestUseCase
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.TestDeferrableSurface
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator
import androidx.camera.core.CameraInfo
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.OutputSurfaceConfiguration
import androidx.camera.core.impl.RequestProcessor
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.SessionProcessor.CaptureCallback
import androidx.camera.core.impl.SessionProcessorSurface
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCamera2Interop::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M)
@DoNotInstrument
class SessionProcessorManagerTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeSessionProcessor = object : SessionProcessor {
        val previewOutputConfigId = 0
        val imageCaptureOutputConfigId = 1
        val imageAnalysisOutputConfigId = 2

        var lastParameters: androidx.camera.core.impl.Config? = null
        var startCapturesCount = 0

        override fun initSession(
            cameraInfo: CameraInfo,
            outputSurfaceConfiguration: OutputSurfaceConfiguration,
        ): SessionConfig {
            Log.debug { "$this#initSession" }
            val previewSurface = SessionProcessorSurface(
                outputSurfaceConfiguration.previewOutputSurface.surface,
                previewOutputConfigId
            ).also {
                it.setContainerClass(Preview::class.java)
            }
            val imageCaptureSurface = SessionProcessorSurface(
                outputSurfaceConfiguration.imageCaptureOutputSurface.surface,
                imageCaptureOutputConfigId
            ).also {
                it.setContainerClass(ImageCapture::class.java)
            }
            val imageAnalysisSurface =
                outputSurfaceConfiguration.imageAnalysisOutputSurface?.surface?.let { surface ->
                    SessionProcessorSurface(
                        surface,
                        imageAnalysisOutputConfigId
                    ).also {
                        it.setContainerClass(ImageAnalysis::class.java)
                    }
                }
            return SessionConfig.Builder().apply {
                setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                addSurface(previewSurface)
                addSurface(imageCaptureSurface)
                imageAnalysisSurface?.let { addSurface(it) }
            }.build()
        }

        override fun deInitSession() {
            Log.debug { "$this#deInitSession" }
        }

        override fun setParameters(config: androidx.camera.core.impl.Config) {
            Log.debug { "$this#setParameters" }
            lastParameters = config
        }

        override fun onCaptureSessionStart(requestProcessor: RequestProcessor) {
            TODO("Not yet implemented")
        }

        override fun onCaptureSessionEnd() {
            TODO("Not yet implemented")
        }

        override fun startRepeating(callback: CaptureCallback): Int {
            TODO("Not yet implemented")
        }

        override fun stopRepeating() {
            TODO("Not yet implemented")
        }

        override fun startCapture(
            postviewEnabled: Boolean,
            callback: CaptureCallback
        ): Int {
            Log.debug { "$this#startCapture" }
            startCapturesCount++
            return 0
        }

        override fun abortCapture(captureSequenceId: Int) {
            TODO("Not yet implemented")
        }
    }
    private val fakeCameraId = CameraId.fromCamera2Id("0")
    private val fakeCameraInfoAdapter = FakeCameraInfoAdapterCreator.createCameraInfoAdapter(
        fakeCameraId
    )

    private val sessionProcessorManager =
        SessionProcessorManager(fakeSessionProcessor, fakeCameraInfoAdapter, testScope)

    @Test
    fun testInitializeSucceedsWithPreview() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any())).thenReturn(
            CameraGraph.Config(fakeCameraId, emptyList())
        )
        val fakePreviewUseCase = createFakeTestUseCase(
            "Preview",
            CameraDevice.TEMPLATE_PREVIEW,
            Preview::class.java
        )
        val fakeImageCaptureUseCase = createFakeTestUseCase(
            "ImageCapture",
            CameraDevice.TEMPLATE_STILL_CAPTURE,
            ImageCapture::class.java
        )

        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakePreviewUseCase, fakeImageCaptureUseCase)
        ).join()
        verify(useCaseManager).createCameraGraphConfig(any(), any(), any())
        verify(useCaseManager).tryResumeUseCaseManager(any())
    }

    @Test
    fun testInitializeSucceedsWithStreamSharing() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any())).thenReturn(
            CameraGraph.Config(fakeCameraId, emptyList())
        )
        val fakeStreamSharingUseCase = createFakeTestUseCase(
            "Preview",
            CameraDevice.TEMPLATE_PREVIEW,
            StreamSharing::class.java
        )
        val fakeImageCaptureUseCase = createFakeTestUseCase(
            "ImageCapture",
            CameraDevice.TEMPLATE_STILL_CAPTURE,
            ImageCapture::class.java
        )

        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakeStreamSharingUseCase, fakeImageCaptureUseCase)
        ).join()
        verify(useCaseManager).createCameraGraphConfig(any(), any(), any())
        verify(useCaseManager).tryResumeUseCaseManager(any())
    }

    @Test
    fun testSubmitCaptureConfigs() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any())).thenReturn(
            CameraGraph.Config(fakeCameraId, emptyList())
        )
        val fakePreviewUseCase = createFakeTestUseCase(
            "Preview",
            CameraDevice.TEMPLATE_PREVIEW,
            Preview::class.java
        )
        val fakeImageCaptureUseCase = createFakeTestUseCase(
            "ImageCapture",
            CameraDevice.TEMPLATE_STILL_CAPTURE,
            ImageCapture::class.java
        )

        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakePreviewUseCase, fakeImageCaptureUseCase)
        ).join()
        sessionProcessorManager.sessionConfig = SessionConfig.Builder().build()

        val jpegRotation = 90
        val jpegQuality = 95
        val captureConfig = CaptureConfig.Builder().apply {
            templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
            addImplementationOption(CaptureConfig.OPTION_ROTATION, jpegRotation)
            addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY, jpegQuality)
        }.build()
        sessionProcessorManager.submitCaptureConfigs(
            listOf(captureConfig),
            listOf(object : CaptureCallback {})
        )

        val parameters = fakeSessionProcessor.lastParameters as? CaptureRequestOptions
        assertThat(parameters).isNotNull()
        val rotation = parameters!!.getCaptureRequestOption(CaptureRequest.JPEG_ORIENTATION, null)
        val quality = parameters.getCaptureRequestOption(CaptureRequest.JPEG_QUALITY, null)
        assertThat(rotation).isEqualTo(jpegRotation)
        assertThat(quality).isEqualTo(jpegQuality)

        assertThat(fakeSessionProcessor.startCapturesCount).isEqualTo(1)
    }

    private fun <T> createFakeTestUseCase(
        name: String,
        template: Int,
        containerClass: Class<T>,
    ): FakeTestUseCase {
        val deferrableSurface = createTestDeferrableSurface(containerClass)
        return FakeTestUseCase(
            FakeUseCaseConfig.Builder().setTargetName(name).useCaseConfig
        ).apply {
            setupSessionConfig(
                SessionConfig.Builder().also { sessionConfigBuilder ->
                    sessionConfigBuilder.setTemplateType(template)
                    sessionConfigBuilder.addSurface(deferrableSurface)
                }
            )
        }
    }

    private fun <T> createTestDeferrableSurface(containerClass: Class<T>): TestDeferrableSurface {
        return TestDeferrableSurface().apply {
            setContainerClass(containerClass)
            terminationFuture.addListener({ cleanUp() }, testDispatcher.asExecutor())
        }
    }
}
