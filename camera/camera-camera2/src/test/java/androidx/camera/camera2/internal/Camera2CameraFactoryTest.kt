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
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCameraCharacteristics
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    minSdk = Build.VERSION_CODES.LOLLIPOP,
    instrumentedPackages = ["androidx.camera.camera2.internal"],
)
class Camera2CameraFactoryTest {
    private val originalFingerprint = Build.FINGERPRINT

    @After
    fun tearDown() {
        // Restore the original fingerprint after each test
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", originalFingerprint)
    }

    @Test
    fun constructor_filtersOutIncompatibleCameras_withoutAvailableCameraSelector() {
        // Arrange
        setFingerprint("fake-fingerprint") // Trigger backward compatibility filter
        setupCameras()

        // Act
        val camera2CameraFactory = createCameraFactory(null)

        // Assert
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "1", "2")
    }

    @Test
    fun constructor_filtersOutIncompatibleCameras_withAvailableCameraSelector() {
        // Arrange
        setFingerprint("fake-fingerprint") // Trigger backward compatibility filter
        setupCameras()

        // Act
        val camera2CameraFactory = createCameraFactory(CameraSelector.DEFAULT_BACK_CAMERA)

        // Assert
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "2")
    }

    @Test
    fun constructor_NotFilterOutIncompatibleCameras_whenBuildFingerprintIsRobolectric() {
        // Arrange
        setFingerprint("robolectric") // Should skip backward compatibility filter
        setupCameras()

        // Act
        val camera2CameraFactory = createCameraFactory(null)

        // Assert
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "1", "2", "3")
    }

    @Test
    fun onCameraIdsUpdated_refreshesAndFiltersList() {
        // Arrange
        setFingerprint("fake-fingerprint")
        setupCameras() // Initial state: "0", "1", "2", "3"
        val camera2CameraFactory = createCameraFactory(null)

        // Assert initial state
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "1", "2")

        // Act: Simulate a camera ("1") being removed from the system.
        camera2CameraFactory.onCameraIdsUpdated(listOf("0", "2", "3"))

        // Assert: The list should be re-filtered. "3" is incompatible, "1" is gone.
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "2")
    }

    @Test
    fun onCameraIdsUpdated_appliesCameraSelectorToNewList() {
        // Arrange
        setFingerprint("fake-fingerprint")
        setupCameras() // Initial state: "0", "1", "2", "3"
        val camera2CameraFactory = createCameraFactory(CameraSelector.DEFAULT_BACK_CAMERA)

        // Assert initial state (filters by selector and compatibility)
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "2")

        // Act: Simulate a new BACK camera ("4") being added. "1" (FRONT) is removed.
        initCharacteristic("4", CameraCharacteristics.LENS_FACING_BACK, getCompatCapabilities())
        camera2CameraFactory.onCameraIdsUpdated(listOf("0", "2", "3", "4"))

        // Assert: The selector should be re-applied to the new list.
        assertThat(camera2CameraFactory.availableCameraIds).containsExactly("0", "2", "4")
    }

    private fun setFingerprint(fingerprint: String) {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", fingerprint)
    }

    private fun createCameraFactory(
        availableCameraSelector: CameraSelector?
    ): Camera2CameraFactory {
        return Camera2CameraFactory(
            ApplicationProvider.getApplicationContext(),
            CameraThreadConfig.create(
                CameraXExecutors.mainThreadExecutor(),
                Handler(Looper.getMainLooper()),
            ),
            availableCameraSelector,
            -1L,
        )
    }

    private fun getCompatCapabilities() =
        intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)

    private fun setupCameras() {
        // Camera "0" and "1" won't be filtered by compat check, by heuristic.
        initCharacteristic("0", CameraCharacteristics.LENS_FACING_BACK, null)
        initCharacteristic("1", CameraCharacteristics.LENS_FACING_FRONT, null)
        // Camera "2" has backward compat capabilities.
        initCharacteristic("2", CameraCharacteristics.LENS_FACING_BACK, getCompatCapabilities())
        // Camera "3" does NOT have backward compat capabilities, will be filtered.
        initCharacteristic("3", CameraCharacteristics.LENS_FACING_BACK, null)
    }

    private fun initCharacteristic(cameraId: String, lensFacing: Int, capabilities: IntArray?) {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        val shadowCharacteristics = Shadow.extract<ShadowCameraCharacteristics>(characteristics)
        shadowCharacteristics.set(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
        )
        shadowCharacteristics.set(CameraCharacteristics.LENS_FACING, lensFacing)
        capabilities?.let {
            shadowCharacteristics.set(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                capabilities,
            )
        }

        // Add the camera to the camera service
        (Shadow.extract(
                ApplicationProvider.getApplicationContext<Context>()
                    .getSystemService(Context.CAMERA_SERVICE)
            ) as ShadowCameraManager)
            .addCamera(cameraId, characteristics)
    }
}
