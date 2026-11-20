/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraDevice
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import androidx.camera.camera2.adapter.CameraControlAdapter
import androidx.camera.camera2.testing.toCameraControlAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.ImmediateSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraXUtil
import androidx.camera.testing.impl.fakes.FakeUseCase
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

private const val DEFAULT_LENS_FACING_SELECTOR = CameraSelector.LENS_FACING_BACK
private const val CAPTURE_TIMEOUT_MS = 10_000L

@LargeTest
@RunWith(AndroidJUnit4::class)
class CaptureConfigAdapterDeviceTest {

    @get:Rule val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTestAndPostTest()

    private var cameraControl: CameraControlAdapter? = null
    private var camera: CameraUseCaseAdapter? = null
    private lateinit var testDeferrableSurface: DeferrableSurface
    private lateinit var fakeUseCase: FakeTestUseCase

    @Before
    fun setUp() = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(DEFAULT_LENS_FACING_SELECTOR))
        val firstRepeatingResult = CompletableDeferred<Unit>()
        testDeferrableSurface = createDrainingDeferrableSurface()
        fakeUseCase =
            FakeTestUseCase(FakeUseCaseConfig.Builder().setTargetName("UseCase").useCaseConfig)
                .apply {
                    setupSessionConfig(
                        SessionConfig.Builder().also { sessionConfigBuilder ->
                            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                            sessionConfigBuilder.addSurface(testDeferrableSurface)
                            sessionConfigBuilder.addRepeatingCameraCaptureCallback(
                                object : CameraCaptureCallback() {
                                    override fun onCaptureCompleted(
                                        captureConfigId: Int,
                                        cameraCaptureResult: CameraCaptureResult,
                                    ) {
                                        firstRepeatingResult.complete(Unit)
                                    }
                                }
                            )
                        }
                    )
                }

        val context: Context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(context, Camera2Config.defaultConfig())
        camera =
            CameraUtil.createCameraUseCaseAdapter(
                    context,
                    CameraSelector.Builder()
                        .requireLensFacing(DEFAULT_LENS_FACING_SELECTOR)
                        .build(),
                )
                .apply { withContext(Dispatchers.Main) { addUseCases(listOf(fakeUseCase)) } }

        cameraControl = camera!!.cameraControl.toCameraControlAdapter()
        Truth.assertWithMessage(
                "Timed out waiting ${CAPTURE_TIMEOUT_MS}ms for initial repeating frame"
            )
            .that(withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { firstRepeatingResult.await() })
            .isNotNull()
    }

    @After
    fun tearDown() {
        camera?.detachUseCases()
        if (this::testDeferrableSurface.isInitialized) {
            testDeferrableSurface.close()
        }
        CameraXUtil.shutdown()[10000, TimeUnit.MILLISECONDS]
    }

    @Test
    fun tagBundleTest() = runBlocking {
        // Arrange
        val deferred = CompletableDeferred<CameraCaptureResult>()
        val tagKey = "TestTagBundleKey"
        val tagValue = "testing"
        val captureConfig =
            CaptureConfig.Builder()
                .apply {
                    templateType = CameraDevice.TEMPLATE_PREVIEW
                    addTag(tagKey, tagValue)
                    addSurface(testDeferrableSurface)
                    addCameraCaptureCallback(
                        object : CameraCaptureCallback() {
                            override fun onCaptureCompleted(
                                captureConfigId: Int,
                                cameraCaptureResult: CameraCaptureResult,
                            ) {
                                deferred.complete(cameraCaptureResult)
                            }

                            override fun onCaptureFailed(
                                captureConfigId: Int,
                                failure: CameraCaptureFailure,
                            ) {
                                deferred.completeExceptionally(
                                    AssertionError("Capture failed: ${failure.reason}")
                                )
                            }

                            override fun onCaptureCancelled(captureConfigId: Int) {
                                deferred.completeExceptionally(AssertionError("Capture cancelled"))
                            }
                        }
                    )
                }
                .build()

        // Act
        cameraControl!!.submitStillCaptureRequests(
            listOf(captureConfig),
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert
        val result = withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { deferred.await() }
        Truth.assertWithMessage("Timed out waiting ${CAPTURE_TIMEOUT_MS}ms for capture result")
            .that(result)
            .isNotNull()
        Truth.assertThat(result!!.tagBundle.getTag(tagKey)).isEqualTo(tagValue)
    }

    @Test
    fun captureConfigIdTest() = runBlocking {
        // Arrange
        val deferredCompleted = CompletableDeferred<Int>()
        val deferredStarted = CompletableDeferred<Int>()
        val expectedCaptureConfigId = 101
        val captureConfig =
            CaptureConfig.Builder()
                .apply {
                    templateType = CameraDevice.TEMPLATE_PREVIEW
                    setId(expectedCaptureConfigId)
                    addSurface(testDeferrableSurface)
                    addCameraCaptureCallback(
                        object : CameraCaptureCallback() {
                            override fun onCaptureCompleted(
                                captureConfigId: Int,
                                cameraCaptureResult: CameraCaptureResult,
                            ) {
                                deferredCompleted.complete(captureConfigId)
                            }

                            override fun onCaptureStarted(captureConfigId: Int) {
                                deferredStarted.complete(captureConfigId)
                            }
                        }
                    )
                }
                .build()

        // Act
        cameraControl!!.submitStillCaptureRequests(
            listOf(captureConfig),
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert
        Truth.assertThat(withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { deferredStarted.await() })
            .isEqualTo(expectedCaptureConfigId)
        Truth.assertThat(withTimeoutOrNull(CAPTURE_TIMEOUT_MS) { deferredCompleted.await() })
            .isEqualTo(expectedCaptureConfigId)
    }

    private class FakeTestUseCase(config: FakeUseCaseConfig) : FakeUseCase(config) {

        fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
            updateSessionConfig(listOf(sessionConfigBuilder.build()))
            notifyActive()
        }
    }

    private fun createDrainingDeferrableSurface(): DeferrableSurface {
        val handlerThread = HandlerThread("TestDeferrableSurface").apply { start() }
        val handler = Handler(handlerThread.looper)
        val imageReader =
            ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 4).apply {
                setOnImageAvailableListener(
                    { reader ->
                        try {
                            reader.acquireLatestImage()?.close()
                        } catch (_: IllegalStateException) {
                            // ImageReader may already be closed during teardown.
                        }
                    },
                    handler,
                )
            }
        return ImmediateSurface(imageReader.surface, Size(640, 480), ImageFormat.YUV_420_888)
            .also { surface ->
                surface.terminationFuture.addListener(
                    {
                        imageReader.setOnImageAvailableListener(null, null)
                        imageReader.close()
                        handlerThread.quitSafely()
                    },
                    CameraXExecutors.newHandlerExecutor(handler),
                )
            }
    }
}
