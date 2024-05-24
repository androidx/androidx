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
import androidx.camera.camera2.pipe.integration.adapter.FakeTestUseCase
import androidx.camera.camera2.pipe.integration.adapter.RequestProcessorAdapter
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.adapter.TestDeferrableSurface
import androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator
import androidx.camera.camera2.pipe.integration.testing.FakeSessionProcessor
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.SessionProcessor.CaptureCallback
import androidx.camera.core.streamsharing.StreamSharing
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.testutils.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertNull
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@ExperimentalCoroutinesApi
@OptIn(ExperimentalCamera2Interop::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.M)
@DoNotInstrument
class SessionProcessorManagerTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeSessionProcessor = FakeSessionProcessor()
    private val fakeCameraId = CameraId.fromCamera2Id("0")
    private val fakeCameraInfoAdapter =
        FakeCameraInfoAdapterCreator.createCameraInfoAdapter(fakeCameraId)

    private val sessionProcessorManager =
        SessionProcessorManager(fakeSessionProcessor, fakeCameraInfoAdapter, testScope)

    @Test
    fun testInitializeSucceedsWithPreview() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any()))
            .thenReturn(CameraGraph.Config(fakeCameraId, emptyList()))
        val fakePreviewUseCase =
            createFakeTestUseCase("Preview", CameraDevice.TEMPLATE_PREVIEW, Preview::class.java)
        val fakeImageCaptureUseCase =
            createFakeTestUseCase(
                "ImageCapture",
                CameraDevice.TEMPLATE_STILL_CAPTURE,
                ImageCapture::class.java
            )

        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakePreviewUseCase, fakeImageCaptureUseCase)
        ) { useCaseManagerConfig ->
            assertNotNull(useCaseManagerConfig)
        }

        advanceUntilIdle()
        verify(useCaseManager).createCameraGraphConfig(any(), any(), eq(true))
    }

    @Test
    fun testInitializeSucceedsWithStreamSharing() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any()))
            .thenReturn(CameraGraph.Config(fakeCameraId, emptyList()))
        val fakeStreamSharingUseCase =
            createFakeTestUseCase(
                "Preview",
                CameraDevice.TEMPLATE_PREVIEW,
                StreamSharing::class.java
            )
        val fakeImageCaptureUseCase =
            createFakeTestUseCase(
                "ImageCapture",
                CameraDevice.TEMPLATE_STILL_CAPTURE,
                ImageCapture::class.java
            )

        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakeStreamSharingUseCase, fakeImageCaptureUseCase)
        ) { useCaseManagerConfig ->
            assertNotNull(useCaseManagerConfig)
        }

        advanceUntilIdle()
        verify(useCaseManager).createCameraGraphConfig(any(), any(), eq(true))
    }

    @Test
    fun testSubmitCaptureConfigs() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any()))
            .thenReturn(CameraGraph.Config(fakeCameraId, emptyList()))
        val fakePreviewUseCase =
            createFakeTestUseCase("Preview", CameraDevice.TEMPLATE_PREVIEW, Preview::class.java)
        val fakeImageCaptureUseCase =
            createFakeTestUseCase(
                "ImageCapture",
                CameraDevice.TEMPLATE_STILL_CAPTURE,
                ImageCapture::class.java
            )

        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakePreviewUseCase, fakeImageCaptureUseCase)
        ) { useCaseManagerConfig ->
            assertNotNull(useCaseManagerConfig)
        }
        sessionProcessorManager.sessionConfig = SessionConfig.Builder().build()
        advanceUntilIdle()

        val mockRequestProcessorAdapter: RequestProcessorAdapter = mock()
        sessionProcessorManager.onCaptureSessionStart(mockRequestProcessorAdapter)

        val jpegRotation = 90
        val jpegQuality = 95
        val captureConfig =
            CaptureConfig.Builder()
                .apply {
                    templateType = CameraDevice.TEMPLATE_STILL_CAPTURE
                    addImplementationOption(CaptureConfig.OPTION_ROTATION, jpegRotation)
                    addImplementationOption(CaptureConfig.OPTION_JPEG_QUALITY, jpegQuality)
                }
                .build()
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

    @Test
    fun testSessionProcessorManagerConfiguresNullWhenClosed() = runTest {
        val useCaseManager: UseCaseManager = mock()
        whenever(useCaseManager.createCameraGraphConfig(any(), any(), any()))
            .thenReturn(CameraGraph.Config(fakeCameraId, emptyList()))
        val fakePreviewUseCase =
            createFakeTestUseCase("Preview", CameraDevice.TEMPLATE_PREVIEW, Preview::class.java)
        val fakeImageCaptureUseCase =
            createFakeTestUseCase(
                "ImageCapture",
                CameraDevice.TEMPLATE_STILL_CAPTURE,
                ImageCapture::class.java
            )

        sessionProcessorManager.prepareClose()
        sessionProcessorManager.close()
        sessionProcessorManager.initialize(
            useCaseManager,
            listOf(fakePreviewUseCase, fakeImageCaptureUseCase)
        ) { useCaseManagerConfig ->
            assertNull(useCaseManagerConfig)
        }
    }

    private fun <T> createFakeTestUseCase(
        name: String,
        template: Int,
        containerClass: Class<T>,
    ): FakeTestUseCase {
        val deferrableSurface = createTestDeferrableSurface(containerClass)
        return FakeTestUseCase(FakeUseCaseConfig.Builder().setTargetName(name).useCaseConfig)
            .apply {
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
