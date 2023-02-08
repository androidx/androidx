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

package androidx.camera.camera2.internal.concurrent

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.utils.MainThreadAsyncHandler
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = ["androidx.camera.camera2.internal"]
)
class Camera2CameraCoordinatorTest {

    private lateinit var cameraCoordinator: CameraCoordinator

    @Before
    fun setup() {
        val fakeCameraImpl = FakeCameraManagerImpl()
        val cameraCharacteristics0 = mock(CameraCharacteristics::class.java)
        Mockito.`when`(cameraCharacteristics0.get(CameraCharacteristics.LENS_FACING))
            .thenReturn(CameraCharacteristics.LENS_FACING_BACK)
        val cameraCharacteristics1 = mock(CameraCharacteristics::class.java)
        Mockito.`when`(cameraCharacteristics1.get(CameraCharacteristics.LENS_FACING))
            .thenReturn(CameraCharacteristics.LENS_FACING_FRONT)
        fakeCameraImpl.addCamera("0", cameraCharacteristics0)
        fakeCameraImpl.addCamera("1", cameraCharacteristics1)
        cameraCoordinator = Camera2CameraCoordinator(CameraManagerCompat.from(fakeCameraImpl))
        cameraCoordinator.init()
    }

    @Test
    fun getConcurrentCameraSelectors() {
        assertThat(cameraCoordinator.concurrentCameraSelectors).isNotEmpty()
        assertThat(cameraCoordinator.concurrentCameraSelectors[0]).isNotEmpty()
        assertThat(cameraCoordinator.concurrentCameraSelectors[0][0].lensFacing)
            .isEqualTo(CameraCharacteristics.LENS_FACING_BACK)
        assertThat(cameraCoordinator.concurrentCameraSelectors[0][1].lensFacing)
            .isEqualTo(CameraCharacteristics.LENS_FACING_FRONT)
    }

    @Test
    fun getPairedCameraId() {
        assertThat(cameraCoordinator.getPairedConcurrentCameraId("0")).isEqualTo("1")
        assertThat(cameraCoordinator.getPairedConcurrentCameraId("1")).isEqualTo("0")
    }

    @Test
    fun setAndIsConcurrentCameraMode() {
        assertThat(cameraCoordinator.isConcurrentCameraModeOn).isFalse()
        cameraCoordinator.setConcurrentCameraMode(true)
        assertThat(cameraCoordinator.isConcurrentCameraModeOn).isTrue()
        cameraCoordinator.setConcurrentCameraMode(false)
        assertThat(cameraCoordinator.isConcurrentCameraModeOn).isFalse()
    }

    @Test
    fun addAndRemoveListener() {
        val listener = mock(CameraCoordinator.ConcurrentCameraModeListener::class.java)
        cameraCoordinator.addListener(listener)
        cameraCoordinator.setConcurrentCameraMode(true)
        verify(listener).notifyConcurrentCameraModeUpdated(true)
        cameraCoordinator.setConcurrentCameraMode(false)
        verify(listener).notifyConcurrentCameraModeUpdated(false)

        reset(listener)
        cameraCoordinator.removeListener(listener)
        cameraCoordinator.setConcurrentCameraMode(true)
        verify(listener, never()).notifyConcurrentCameraModeUpdated(true)
    }

    private class FakeCameraManagerImpl : CameraManagerCompat.CameraManagerCompatImpl {

        private val mCameraManagerImpl = CameraManagerCompat.CameraManagerCompatImpl.from(
            ApplicationProvider.getApplicationContext(),
            MainThreadAsyncHandler.getInstance()
        )

        private val mCameraIdCharacteristics = HashMap<String, CameraCharacteristics>()

        fun addCamera(
            cameraId: String,
            cameraCharacteristics: CameraCharacteristics
        ) {
            mCameraIdCharacteristics[cameraId] = cameraCharacteristics
        }

        override fun getCameraIdList(): Array<String> {
            return mCameraIdCharacteristics.keys.toTypedArray()
        }

        override fun getConcurrentCameraIds(): MutableSet<MutableSet<String>> {
            return mutableSetOf(mCameraIdCharacteristics.keys)
        }

        override fun registerAvailabilityCallback(
            executor: Executor,
            callback: CameraManager.AvailabilityCallback
        ) {
        }

        override fun unregisterAvailabilityCallback(callback: CameraManager.AvailabilityCallback) {
        }

        override fun getCameraCharacteristics(cameraId: String): CameraCharacteristics {
            return mCameraIdCharacteristics[cameraId]!!
        }

        override fun openCamera(
            cameraId: String,
            executor: Executor,
            callback: CameraDevice.StateCallback
        ) {
        }

        override fun getCameraManager(): CameraManager {
            return mCameraManagerImpl.cameraManager
        }
    }
}