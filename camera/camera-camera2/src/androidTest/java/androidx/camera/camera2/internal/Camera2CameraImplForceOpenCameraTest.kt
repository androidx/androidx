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

package androidx.camera.camera2.internal

import android.content.Context
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.camera.camera2.AsyncCameraDevice
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.CameraSelector
import androidx.camera.core.Logger
import androidx.camera.core.impl.CameraInternal.State
import androidx.camera.core.impl.CameraStateRegistry
import androidx.camera.core.impl.Observable.Observer
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.core.os.HandlerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests [Camera2CameraImpl]'s force opening camera behavior.
 *
 * The test opens a camera with Camera2 (using [CameraDevice]), then attempts to open the same
 * camera with CameraX (using [Camera2CameraImpl]).
 *
 * Camera opening behavior is different in API levels 21/22 compared to API levels 23 and above.
 * In API levels 21 and 22, a second camera client cannot open a camera until the first client
 * closes it, whereas in later API levels, the camera service steals the camera away from a
 * client when another one with the same or a higher priority attempts to open it.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class Camera2CameraImplForceOpenCameraTest {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    private lateinit var cameraId: String
    private lateinit var camera2Camera: AsyncCameraDevice
    private val mCameraXCameraToStateObserver = mutableMapOf<Camera2CameraImpl, Observer<State>>()

    @Before
    fun getCameraId() {
        val camId = CameraUtil.getCameraIdWithLensFacing(CameraSelector.LENS_FACING_BACK)
        assumeFalse("Device doesn't have a back facing camera", camId == null)
        cameraId = camId!!
    }

    @After
    fun releaseCameraResources() {
        if (::camera2Camera.isInitialized) {
            camera2Camera.closeAsync()
        }
        for (entry in mCameraXCameraToStateObserver) {
            releaseCameraXCameraResource(entry)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    fun openCameraImmediately_ifCameraCanBeStolen() {
        // Open the camera with Camera2
        val camera2CameraOpen = openCamera_camera2(cameraId)
        camera2CameraOpen.get()

        // Open the camera with CameraX, this steals it away from Camera2
        val cameraXCameraOpen = openCamera_cameraX(cameraId)
        cameraXCameraOpen.await()
    }

    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    fun openCameraWhenAvailable_ifCameraCannotBeStolen() {
        // Open the camera with Camera2
        val camera2CameraOpen = openCamera_camera2(cameraId)
        camera2CameraOpen.get()

        // Attempt to open the camera with CameraX, this will fail
        val cameraXCameraOpen = openCamera_cameraX(cameraId)
        assertThat(cameraXCameraOpen.timesOutWhileWaiting()).isTrue()

        // Close the camera with Camera2, and wait for it to be opened with CameraX
        camera2Camera.closeAsync()
        cameraXCameraOpen.await()
    }

    @Test
    fun openCameraWhenAvailable_ifMaxAllowedOpenedCamerasReached() {
        // Open the camera with CameraX
        val cameraOpen1 = openCamera_cameraX(cameraId)
        cameraOpen1.await()

        // Open the camera again with CameraX
        val cameraOpen2 = openCamera_cameraX(cameraId)
        assertThat(cameraOpen2.timesOutWhileWaiting()).isTrue()

        // Close the first camera instance, and wait for it to be opened with the second instance
        releaseCameraXCameraResource(mCameraXCameraToStateObserver.entries.first())
        cameraOpen2.await()
    }

    private fun openCamera_camera2(camId: String): ListenableFuture<CameraDevice> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camera2Camera = AsyncCameraDevice(cameraManager, camId, cameraHandler)
        return camera2Camera.openAsync()
    }

    private fun openCamera_cameraX(camId: String): Semaphore {
        // Build camera manager wrapper
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraManagerCompat = CameraManagerCompat.from(context)

        // Build camera info from cameraId
        val camera2CameraInfo = Camera2CameraInfoImpl(
            camId,
            cameraManagerCompat
        )

        // Initialize camera instance
        val camera = Camera2CameraImpl(
            cameraManagerCompat,
            camId,
            camera2CameraInfo,
            cameraRegistry,
            cameraExecutor,
            cameraHandler,
            DisplayInfoManager.getInstance(ApplicationProvider.getApplicationContext())
        )

        // Open the camera
        camera.open()
        val cameraOpenSemaphore = Semaphore(0)
        val stateObserver = object : Observer<State> {
            override fun onNewData(value: State?) {
                if (value == State.OPEN) {
                    Logger.d(TAG, "CameraX: Camera open")
                    cameraOpenSemaphore.release()
                }
            }

            override fun onError(throwable: Throwable) {
                Logger.e(TAG, "CameraX: Camera error $throwable")
            }
        }
        camera.cameraState.addObserver(cameraExecutor, stateObserver)
        mCameraXCameraToStateObserver[camera] = stateObserver
        return cameraOpenSemaphore
    }

    private fun releaseCameraXCameraResource(
        entry: MutableMap.MutableEntry<Camera2CameraImpl, Observer<State>>
    ) {
        entry.key.cameraState.removeObserver(entry.value)
        entry.key.release().get()
    }

    private fun Semaphore.await() {
        assertThat(tryAcquire(5, TimeUnit.SECONDS)).isTrue()
    }

    private fun Semaphore.timesOutWhileWaiting(): Boolean {
        val acquired = tryAcquire(5, TimeUnit.SECONDS)
        return !acquired
    }

    companion object {
        private const val TAG = "ForceOpenCameraTest"

        private lateinit var cameraHandlerThread: HandlerThread
        private lateinit var cameraHandler: Handler
        private lateinit var cameraExecutor: ExecutorService
        private val cameraRegistry: CameraStateRegistry by lazy { CameraStateRegistry(1) }

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            cameraHandlerThread = HandlerThread("cameraThread")
            cameraHandlerThread.start()
            cameraHandler = HandlerCompat.createAsync(cameraHandlerThread.looper)
            cameraExecutor = CameraXExecutors.newHandlerExecutor(cameraHandler)
        }

        @AfterClass
        @JvmStatic
        fun classTeardown() {
            cameraHandlerThread.quitSafely()
        }
    }
}