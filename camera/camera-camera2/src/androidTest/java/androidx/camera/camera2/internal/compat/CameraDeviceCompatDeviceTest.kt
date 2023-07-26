/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.camera.camera2.internal.compat

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.camera2.AsyncCameraDevice
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.compat.params.OutputConfigurationCompat
import androidx.camera.camera2.internal.compat.params.SessionConfigurationCompat
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.core.os.HandlerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

/**
 * Tests some of the methods of [CameraDeviceCompat] on device.
 *
 *
 * These need to run on device since they rely on native implementation details of the
 * [CameraDevice] class on some API levels.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class CameraDeviceCompatDeviceTest {
    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private var cameraDevice: AsyncCameraDevice? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var compatHandlerThread: HandlerThread? = null
    private var compatHandler: Handler? = null

    @Before
    @Throws(CameraAccessException::class, InterruptedException::class)
    fun setUp() {
        val cameraManager = ApplicationProvider.getApplicationContext<Context>().getSystemService(
            Context.CAMERA_SERVICE
        ) as CameraManager
        val cameraIds = cameraManager.cameraIdList
        Assume.assumeTrue("No cameras found on device.", cameraIds.isNotEmpty())
        val cameraId = cameraIds[0]
        compatHandlerThread = HandlerThread("DispatchThread")
        compatHandlerThread!!.start()
        compatHandler = HandlerCompat.createAsync(compatHandlerThread!!.looper)
        cameraDevice = AsyncCameraDevice(cameraManager, cameraId, compatHandler!!)
        try {
            cameraDevice!!.openAsync().get()
        } catch (ex: Exception) {
            throw AssertionError("Unable to open camera.", ex)
        }
        val streamConfigurationMap = cameraManager.getCameraCharacteristics(
            cameraId
        ).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val validSizes = streamConfigurationMap!!.getOutputSizes(
            SurfaceTexture::class.java
        )
        Assume.assumeTrue("No valid sizes available for SurfaceTexture.", validSizes.isNotEmpty())
        surfaceTexture = SurfaceTexture(0)
        surfaceTexture!!.setDefaultBufferSize(validSizes[0].width, validSizes[0].height)
        surface = Surface(surfaceTexture)
    }

    @After
    @Throws(InterruptedException::class)
    fun tearDown() {
        if (cameraDevice != null) {
            cameraDevice!!.closeAsync().get()
        }
        if (surface != null) {
            surface!!.release()
        }
        if (surfaceTexture != null) {
            surfaceTexture!!.release()
        }
        if (compatHandlerThread != null) {
            compatHandlerThread!!.quitSafely()
        }
    }

    @Test
    @Throws(CameraAccessExceptionCompat::class)
    fun canConfigureCaptureSession() {
        val outputConfig = OutputConfigurationCompat(
            surface!!
        )
        val stateCallback = Mockito.mock(
            CameraCaptureSession.StateCallback::class.java
        )
        val sessionConfig = SessionConfigurationCompat(
            SessionConfigurationCompat.SESSION_REGULAR,
            listOf(outputConfig),
            Dispatchers.Default.asExecutor(),
            stateCallback
        )
        val deviceCompat = CameraDeviceCompat.toCameraDeviceCompat(
            cameraDevice!!.openAsync().get(),
            compatHandler!!
        )
        try {
            deviceCompat.createCaptureSession(sessionConfig)
        } catch (e: CameraAccessExceptionCompat) {
            // If the camera has been disconnected during the test (likely due to another process
            // stealing the camera), then we will skip the test.
            Assume.assumeTrue(
                "Camera disconnected during test.",
                e.reason != CameraAccessException.CAMERA_DISCONNECTED
            )
            throw e
        }
        Mockito.verify(stateCallback, Mockito.timeout(3000)).onConfigured(
            ArgumentMatchers.any(
                CameraCaptureSession::class.java
            )
        )
    }
}
