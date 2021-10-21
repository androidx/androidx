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

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package androidx.camera.integration.core

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.concurrent.futures.await
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class Camera2InteropIntegrationTest {

    @get:Rule
    val mUseCameraRule: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    private var processCameraProvider: ProcessCameraProvider? = null

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        processCameraProvider = ProcessCameraProvider.getInstance(context).await()
    }

    @After
    fun tearDown(): Unit = runBlocking {
        processCameraProvider?.apply {
            withContext(Dispatchers.Main) {
                unbindAll()
            }
            shutdown().await()
        }
    }

    @Test
    fun cameraDeviceListener_receivesClose_afterUnbindAll(): Unit = runBlocking {
        val previewBuilder = Preview.Builder()
        val deviceStateFlow = Camera2Interop.Extender(previewBuilder).createDeviceStateFlow()

        withContext(Dispatchers.Main) {
            processCameraProvider!!.bindToLifecycle(
                TestLifecycleOwner(Lifecycle.State.RESUMED),
                CameraSelector.DEFAULT_BACK_CAMERA, previewBuilder.build()
            )
        }

        // Call unbindAll() once device state is [DeviceState.Opened]. If another state occurs
        // first, the StateFlow subscriber will unsubscribe with the last state. Once
        // [DeviceState.Opened] occurs, we will call unbindAll(), and the next state should be
        // [DeviceState.Closed].
        var unbindAllCalled = false
        val lastState = deviceStateFlow.dropWhile { state ->
            when (state) {
                is DeviceState.Unknown -> true // Filter out this state from the downstream flow
                is DeviceState.Opened -> {
                    withContext(Dispatchers.Main) { processCameraProvider!!.unbindAll() }
                    unbindAllCalled = true
                    true // Filter out this state from the downstream flow
                }
                else -> false // Forward to the downstream flow
            }
        }.first()

        assertThat(unbindAllCalled).isTrue()
        assertThat(lastState).isEqualTo(DeviceState.Closed)
    }

    @Test
    fun canUseCameraSelector_fromCamera2CameraIdAndCameraFilter(): Unit = runBlocking {
        val camera2CameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(CAMERA_SERVICE) as CameraManager
        camera2CameraManager.cameraIdList.forEach { id ->
            val cameraSelector = CameraSelector.Builder()
                .addCameraFilter {
                    it.filter { Camera2CameraInfo.from(it).cameraId == id }
                }.build()

            withContext(Dispatchers.Main) {
                val camera = processCameraProvider!!.bindToLifecycle(
                    TestLifecycleOwner(Lifecycle.State.CREATED),
                    cameraSelector
                )
                assertThat(Camera2CameraInfo.from(camera.cameraInfo).cameraId).isEqualTo(id)
            }
        }
    }

    @Test
    fun canUseCameraSelector_fromCamera2CameraIdAndAvailableCameraInfos(): Unit = runBlocking {
        val camera2CameraManager = ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(CAMERA_SERVICE) as CameraManager
        camera2CameraManager.cameraIdList.forEach { id ->
            withContext(Dispatchers.Main) {
                val cameraSelector =
                    processCameraProvider!!.availableCameraInfos.find {
                        Camera2CameraInfo.from(it).cameraId == id
                    }?.cameraSelector

                val camera = processCameraProvider!!.bindToLifecycle(
                    TestLifecycleOwner(Lifecycle.State.CREATED),
                    cameraSelector!!
                )
                assertThat(Camera2CameraInfo.from(camera.cameraInfo).cameraId).isEqualTo(id)
            }
        }
    }

    // Sealed class for converting CameraDevice.StateCallback into a StateFlow
    sealed class DeviceState {
        object Unknown : DeviceState()
        object Opened : DeviceState()
        object Closed : DeviceState()
        object Disconnected : DeviceState()
        data class Error(val errorCode: Int) : DeviceState()
    }

    /**
     * Returns a [StateFlow] which will signal the states of the camera defined in [DeviceState].
     */
    private fun <T> Camera2Interop.Extender<T>.createDeviceStateFlow(): StateFlow<DeviceState> =
        MutableStateFlow<DeviceState>(DeviceState.Unknown).apply {
            val stateCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    // tryEmit should always succeed for [MutableStateFlow]
                    value = DeviceState.Opened
                }

                override fun onClosed(camera: CameraDevice) {
                    tryEmit(DeviceState.Closed)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    tryEmit(DeviceState.Disconnected)
                }

                override fun onError(camera: CameraDevice, errorCode: Int) {
                    tryEmit(DeviceState.Error(errorCode))
                }
            }
            setDeviceStateCallback(stateCallback)
        }.asStateFlow()
}
