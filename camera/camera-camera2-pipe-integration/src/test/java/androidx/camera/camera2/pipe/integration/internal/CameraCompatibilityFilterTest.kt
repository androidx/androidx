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
import androidx.camera.core.internal.StreamSpecsCalculator.Companion.NO_OP_STREAM_SPECS_CALCULATOR
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import org.junit.After
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
    instrumentedPackages = ["androidx.camera.camera2.pipe.integration.adapter"],
)
class CameraCompatibilityFilterTest {

    private val originalFingerprint = Build.FINGERPRINT

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", originalFingerprint)
    }

    @Test
    fun filterOutIncompatibleCameras_withoutAvailableCameraSelector() {
        // Arrange
        // Customizes Build.FINGERPRINT to be not "fingerprint", so that cameras without
        // REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE will be filtered.
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "fake-fingerprint")
        setupCameras()
        val cameraFactoryAdapter =
            CameraFactoryProvider()
                .newInstance(
                    ApplicationProvider.getApplicationContext(),
                    CameraThreadConfig.create(
                        CameraXExecutors.mainThreadExecutor(),
                        Handler(Looper.getMainLooper()),
                    ),
                    null,
                    -1L,
                    null,
                    NO_OP_STREAM_SPECS_CALCULATOR,
                )

        // Assert: "0" and "1" are included by heuristic. "2" is included because it has the
        // capability. "3" is filtered out.
        Truth.assertThat(cameraFactoryAdapter.availableCameraIds).containsExactly("0", "1", "2")
    }

    @Test
    fun filterOutIncompatibleCameras_withAvailableCameraSelector() {
        // Arrange
        // Customizes Build.FINGERPRINT to be not "fingerprint", so that cameras without
        // REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE will be filtered.
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", "fake-fingerprint")
        setupCameras()

        val cameraFactoryAdapter =
            CameraFactoryProvider()
                .newInstance(
                    ApplicationProvider.getApplicationContext(),
                    CameraThreadConfig.create(
                        CameraXExecutors.mainThreadExecutor(),
                        Handler(Looper.getMainLooper()),
                    ),
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    -1L,
                    null,
                    NO_OP_STREAM_SPECS_CALCULATOR,
                )

        // Assert: "0" and "2" are the compatible back cameras. "1" (front) is filtered out by the
        // selector. "3" (incompatible back) is filtered out by the compatibility check.
        Truth.assertThat(cameraFactoryAdapter.availableCameraIds).containsExactly("0", "2")
    }

    @Test
    fun notFilterOutIncompatibleCameras_whenBuildFingerprintIsRobolectric() {
        // Arrange
        setupCameras()

        val cameraFactoryAdapter =
            CameraFactoryProvider()
                .newInstance(
                    ApplicationProvider.getApplicationContext(),
                    CameraThreadConfig.create(
                        CameraXExecutors.mainThreadExecutor(),
                        Handler(Looper.getMainLooper()),
                    ),
                    null,
                    -1L,
                    null,
                    NO_OP_STREAM_SPECS_CALCULATOR,
                )

        // Assert: The compatibility check is skipped, so all cameras are included.
        Truth.assertThat(cameraFactoryAdapter.availableCameraIds)
            .containsExactly("0", "1", "2", "3")
    }

    private fun setupCameras() {
        val capabilities =
            intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)

        // Camera "0" (BACK) and "1" (FRONT) do not have the capability but should be included by
        // heuristic.
        initCharacteristics("0", CameraCharacteristics.LENS_FACING_BACK, null)
        initCharacteristics("1", CameraCharacteristics.LENS_FACING_FRONT, null)
        // Camera "2" has the backward compatible capability.
        initCharacteristics("2", CameraCharacteristics.LENS_FACING_BACK, capabilities)
        // Camera "3" does not have the capability and should be filtered out.
        initCharacteristics("3", CameraCharacteristics.LENS_FACING_BACK, null)
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
                    Rect(0, 0, sensorWidth, sensorHeight),
                )

                set(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
                )

                set(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP,
                    StreamConfigurationMapBuilder.newBuilder().build(),
                )
            }

        capabilities?.let {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                capabilities,
            )
        }

        // Add the camera to the camera service
        (Shadow.extract<Any>(
                ApplicationProvider.getApplicationContext<Context>()
                    .getSystemService(Context.CAMERA_SERVICE)
            ) as ShadowCameraManager)
            .addCamera(cameraId, characteristics)
    }
}
