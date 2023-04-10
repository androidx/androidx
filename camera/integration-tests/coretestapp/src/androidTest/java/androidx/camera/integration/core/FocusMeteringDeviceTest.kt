/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.integration.core.util.CameraPipeUtil
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.equalTo
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class FocusMeteringDeviceTest(
    private val selectorName: String,
    private val cameraSelector: CameraSelector,
    private val implName: String,
    private val cameraXConfig: CameraXConfig
) {
    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraXConfig)
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "selector={0},config={2}")
        fun data() = listOf(
            arrayOf(
                "front",
                CameraSelector.DEFAULT_FRONT_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "front",
                CameraSelector.DEFAULT_FRONT_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            ),
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                Camera2Config::class.simpleName,
                Camera2Config.defaultConfig()
            ),
            arrayOf(
                "back",
                CameraSelector.DEFAULT_BACK_CAMERA,
                CameraPipeConfig::class.simpleName,
                CameraPipeConfig.defaultConfig()
            )
        )
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider

    private val meteringPointFactory = SurfaceOrientedMeteringPointFactory(1f, 1f)
    private val validMeteringPoint = meteringPointFactory.createPoint(0.5f, 0.5f)
    private val invalidMeteringPoint = meteringPointFactory.createPoint(0f, 1.1f)

    @Before
    fun setUp(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(cameraSelector.lensFacing!!))

        ProcessCameraProvider.configureInstance(cameraXConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        val captureCallback = CameraSessionCaptureCallback()

        withContext(Dispatchers.Main) {
            val fakeLifecycleOwner = FakeLifecycleOwner()
            fakeLifecycleOwner.startAndResume()
            camera = cameraProvider.bindToLifecycle(
                fakeLifecycleOwner,
                cameraSelector,
                ImageCapture.Builder().also { builder ->
                    captureCallback.let {
                        CameraPipeUtil.setCameraCaptureSessionCallback(
                            implName,
                            builder,
                            it
                        )
                    }
                }.build()
            )
        }

        if (implName == CameraPipeConfig::class.simpleName) {
            // TODO(b/264396089): Remove this waiting for camera opening to be completed
            //  when camera can be closed properly without this
            captureCallback.await(5000)
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10, TimeUnit.SECONDS]
            }
        }

        if (selectorName == "front" && implName == CameraPipeConfig::class.simpleName) {
            // TODO(b/264332446): Replace this delay with some API like closeAll() once available
            delay(5000)
        }
    }

    @Test
    fun resultFutureCompletes_whenFocusMeteringStarted() = runBlocking {
        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        val result = camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        val focusMeteringResultCallback = FocusMeteringResultCallback()
        Futures.addCallback<FocusMeteringResult>(
            result,
            focusMeteringResultCallback,
            ContextCompat.getMainExecutor(context)
        )

        focusMeteringResultCallback.await()

        assertWithMessage("Result future for startFocusAndMetering operation did not complete!")
            .that(focusMeteringResultCallback.successResult != null ||
                focusMeteringResultCallback.failureThrowable != null
            ).isTrue()
    }

    @Test
    fun canStartFocusMeteringSuccessfully() = runBlocking {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        assumeThat(
            "FocusMeteringAction not supported!",
            camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction), equalTo(true)
        )

        val result = camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        val focusMeteringResultCallback = FocusMeteringResultCallback()
        Futures.addCallback<FocusMeteringResult>(
            result,
            focusMeteringResultCallback,
            ContextCompat.getMainExecutor(context)
        )

        focusMeteringResultCallback.await()

        assertWithMessage("FocusMetering failed!")
            .that(focusMeteringResultCallback.successResult).isNotNull()
    }

    @Test
    fun isFocusMeteringSupported_whenMeteringPointValid() = runBlocking {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        assertThat(camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction)).isTrue()
    }

    @Test
    fun isFocusMeteringUnsupported_whenMeteringPointInvalid() = runBlocking {
        val focusMeteringAction = FocusMeteringAction.Builder(invalidMeteringPoint).build()

        assertThat(camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction)).isFalse()
    }

    @Test
    fun focusMeteringSucceeds_whenSupported() = runBlocking {
        assumeThat(
            "No AF/AE/AWB region available on this device!",
            hasMeteringRegion(cameraSelector), equalTo(true)
        )

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        assumeThat(
            "FocusMeteringAction not supported!",
            camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction), equalTo(true)
        )

        val result = camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        val focusMeteringResultCallback = FocusMeteringResultCallback()
        Futures.addCallback<FocusMeteringResult>(
            result,
            focusMeteringResultCallback,
            CameraXExecutors.mainThreadExecutor()
        )

        focusMeteringResultCallback.await()

        assertWithMessage("FocusMetering failed!")
            .that(focusMeteringResultCallback.successResult).isNotNull()
    }

    @Test
    fun focusMeteringFailsWithIllegalArgumentException_whenMeteringPointInvalid() = runBlocking {
        val focusMeteringAction = FocusMeteringAction.Builder(invalidMeteringPoint).build()

        assumeThat(
            "FocusMeteringAction supported!",
            camera.cameraInfo.isFocusMeteringSupported(focusMeteringAction), equalTo(false)
        )

        val result = camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        val focusMeteringResultCallback = FocusMeteringResultCallback()
        Futures.addCallback<FocusMeteringResult>(
            result,
            focusMeteringResultCallback,
            CameraXExecutors.mainThreadExecutor()
        )

        focusMeteringResultCallback.await()

        assertWithMessage("FocusMetering succeeded, should have failed for invalid argument!")
            .that(focusMeteringResultCallback.failureThrowable)
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun focusMeteringFailsWithOperationCanceledException_whenNoUseCaseIsBound() = runBlocking {
        withContext(Dispatchers.Main) {
            cameraProvider.unbindAll()
        }

        val focusMeteringAction = FocusMeteringAction.Builder(validMeteringPoint).build()

        val result = camera.cameraControl.startFocusAndMetering(focusMeteringAction)
        val focusMeteringResultCallback = FocusMeteringResultCallback()
        Futures.addCallback<FocusMeteringResult>(
            result,
            focusMeteringResultCallback,
            CameraXExecutors.mainThreadExecutor()
        )

        focusMeteringResultCallback.await()

        assertWithMessage("FocusMetering succeeded, should have failed for inactive camera!")
            .that(focusMeteringResultCallback.failureThrowable)
            .isInstanceOf(CameraControl.OperationCanceledException::class.java)
    }

    private fun hasMeteringRegion(selector: CameraSelector): Boolean {
        return try {
            val cameraCharacteristics = CameraUtil.getCameraCharacteristics(
                selector.lensFacing!!
            )
            cameraCharacteristics?.let { characteristics ->
                ((characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!! > 0) ||
                    (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)!! > 0) ||
                    (characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AWB)!! > 0))
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    class FocusMeteringResultCallback : FutureCallback<FocusMeteringResult?> {
        private var latch = CountDownLatch(1)

        @Volatile
        var successResult: FocusMeteringResult? = null
        @Volatile
        var failureThrowable: Throwable? = null

        override fun onSuccess(result: FocusMeteringResult?) {
            successResult = result
            latch.countDown()
        }

        override fun onFailure(t: Throwable) {
            failureThrowable = t
            latch.countDown()
        }

        suspend fun await(timeoutMs: Long = 10000) {
            withContext(Dispatchers.IO) {
                latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    class CameraSessionCaptureCallback : CameraCaptureSession.CaptureCallback() {
        private val latch = CountDownLatch(1)

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            latch.countDown()
        }

        suspend fun await(timeoutMs: Long = 10000) {
            withContext(Dispatchers.IO) {
                latch.await(timeoutMs, TimeUnit.MILLISECONDS)
            }
        }
    }
}
