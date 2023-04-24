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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.adapter.CameraControlAdapter
import androidx.camera.camera2.pipe.testing.toCameraControlAdapter
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraCaptureFailure
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.core.internal.CameraUseCaseAdapter
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraXUtil
import androidx.camera.testing.fakes.FakeUseCase
import androidx.camera.testing.fakes.FakeUseCaseConfig
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
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

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CaptureConfigAdapterDeviceTest {

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    private var cameraControl: CameraControlAdapter? = null
    private var camera: CameraUseCaseAdapter? = null
    private lateinit var testDeferrableSurface: TestDeferrableSurface
    private lateinit var fakeUseCase: FakeTestUseCase

    @Before
    fun setUp() = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(DEFAULT_LENS_FACING_SELECTOR))
        testDeferrableSurface = TestDeferrableSurface()
        fakeUseCase = FakeTestUseCase(
            FakeUseCaseConfig.Builder().setTargetName("UseCase").useCaseConfig
        ).apply {
            setupSessionConfig(SessionConfig.Builder().also { sessionConfigBuilder ->
                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW)
                sessionConfigBuilder.addSurface(testDeferrableSurface)
            })
        }

        val context: Context = ApplicationProvider.getApplicationContext()
        CameraXUtil.initialize(
            context, CameraPipeConfig.defaultConfig()
        )
        camera = CameraUtil.createCameraUseCaseAdapter(
            context, CameraSelector.Builder().requireLensFacing(
                DEFAULT_LENS_FACING_SELECTOR
            ).build()
        ).apply {
            withContext(Dispatchers.Main) {
                addUseCases(listOf(fakeUseCase))
            }
        }

        cameraControl = camera!!.cameraControl.toCameraControlAdapter()
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
        val captureConfig = CaptureConfig.Builder().apply {
                templateType = CameraDevice.TEMPLATE_PREVIEW
                addTag(tagKey, tagValue)
                addSurface(testDeferrableSurface)
                addCameraCaptureCallback(object : CameraCaptureCallback() {
                    override fun onCaptureCompleted(cameraCaptureResult: CameraCaptureResult) {
                        deferred.complete(cameraCaptureResult)
                    }

                    override fun onCaptureFailed(failure: CameraCaptureFailure) {
                        deferred.completeExceptionally(Throwable(failure.reason.toString()))
                    }

                    override fun onCaptureCancelled() {
                        deferred.cancel()
                    }
                })
            }.build()

        // Act
        cameraControl!!.submitStillCaptureRequests(
            listOf(captureConfig),
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
            ImageCapture.FLASH_TYPE_ONE_SHOT_FLASH,
        )

        // Assert
        Truth.assertThat(
            withTimeoutOrNull(timeMillis = 5000) {
                deferred.await()
            }!!.tagBundle.getTag(tagKey)
        ).isEqualTo(tagValue)
    }

    private class FakeTestUseCase(
        config: FakeUseCaseConfig,
    ) : FakeUseCase(config) {

        fun setupSessionConfig(sessionConfigBuilder: SessionConfig.Builder) {
            updateSessionConfig(sessionConfigBuilder.build())
            notifyActive()
        }
    }

    private class TestDeferrableSurface : DeferrableSurface() {
        init {
            terminationFuture.addListener(
                { cleanUp() }, Dispatchers.IO.asExecutor()
            )
        }

        private val surfaceTexture = SurfaceTexture(0).also {
            it.setDefaultBufferSize(640, 480)
        }
        val testSurface = Surface(surfaceTexture)

        override fun provideSurface(): ListenableFuture<Surface> {
            return Futures.immediateFuture(testSurface)
        }

        fun cleanUp() {
            testSurface.release()
            surfaceTexture.release()
        }
    }
}