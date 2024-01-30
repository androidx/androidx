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

package androidx.camera.camera2.pipe.integration.internal

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.camera.camera2.pipe.integration.adapter.CameraFactoryProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.StreamConfigurationMapBuilder
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = ["androidx.camera.camera2.pipe.integration.adapter"]
)
class CameraCompatibilityFilterTest {

    private fun setupCameras() {
        val capabilities =
            intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)

        initCharacteristics("0", CameraCharacteristics.LENS_FACING_BACK, capabilities)
        initCharacteristics("1", CameraCharacteristics.LENS_FACING_FRONT, capabilities)
        initCharacteristics("2", CameraCharacteristics.LENS_FACING_BACK, capabilities)
        // Do not set REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE for the camera, so that
        // it will be filtered out if Build.FINGERPRINT is no "robolectric".
        initCharacteristics("3", CameraCharacteristics.LENS_FACING_BACK, null)
    }

    @Test
    fun filterOutIncompatibleCameras_withoutAvailableCameraSelector() {
        // Customizes Build.FINGERPRINT to be not "fingerprint", so that cameras without
        // REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE will be filtered.
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "fake-fingerprint")

        setupCameras()
        val cameraFactoryAdapter = CameraFactoryProvider().newInstance(
            ApplicationProvider.getApplicationContext(), CameraThreadConfig.create(
                CameraXExecutors.mainThreadExecutor(), Handler(Looper.getMainLooper())
            ), null, -1L
        )

        Truth.assertThat(cameraFactoryAdapter.availableCameraIds).containsExactly("0", "1", "2")
    }

    @Test
    fun filterOutIncompatibleCameras_withAvailableCameraSelector() {
        // Customizes Build.FINGERPRINT to be not "fingerprint", so that cameras without
        // REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE will be filtered.
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "fake-fingerprint")

        setupCameras()

        val cameraFactoryAdapter = CameraFactoryProvider().newInstance(
            ApplicationProvider.getApplicationContext(), CameraThreadConfig.create(
                CameraXExecutors.mainThreadExecutor(), Handler(Looper.getMainLooper())
            ), CameraSelector.DEFAULT_BACK_CAMERA, -1L
        )

        Truth.assertThat(cameraFactoryAdapter.availableCameraIds).containsExactly("0", "2")
    }

    @Test
    fun NotFilterOutIncompatibleCameras_whenBuildFingerprintIsRobolectric() {
        setupCameras()

        val cameraFactoryAdapter = CameraFactoryProvider().newInstance(
            ApplicationProvider.getApplicationContext(), CameraThreadConfig.create(
                CameraXExecutors.mainThreadExecutor(), Handler(Looper.getMainLooper())
            ), null, -1L
        )

        Truth.assertThat(cameraFactoryAdapter.availableCameraIds)
            .containsExactly("0", "1", "2", "3")
    }

    private fun initCharacteristics(cameraId: String, lensFacing: Int, capabilities: IntArray?) {
        val sensorWidth = 640
        val sensorHeight = 480

        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics =
            Shadow.extract<ShadowCameraCharacteristics>(characteristics).apply {

                set(CameraCharacteristics.LENS_FACING, lensFacing)

                set(
                    CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE,
                    Rect(0, 0, sensorWidth, sensorHeight)
                )

                set(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
                )

                set(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                    StreamConfigurationMapBuilder.newBuilder().build()
                )
            }

        capabilities?.let {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities
            )
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
            ApplicationProvider.getApplicationContext<Context>()
                .getSystemService(Context.CAMERA_SERVICE)
        ) as ShadowCameraManager).addCamera(cameraId, characteristics)
    }
}
