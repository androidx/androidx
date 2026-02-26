/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.adapter

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import androidx.camera.camera2.config.CameraAppComponent
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.DoNotDisturbException
import androidx.camera.camera2.pipe.testing.CameraPipeSimulator
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.InitializationException
import androidx.camera.core.impl.CameraUpdateException
import androidx.test.core.app.ApplicationProvider
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class CameraSurfaceAdapterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockCameraAppComponent: CameraAppComponent = mock()

    private lateinit var fakeMetadata0: CameraMetadata
    private lateinit var fakeMetadata1: CameraMetadata
    private lateinit var fakeCameraDevices: FakeCameraDevices
    private lateinit var fakeCameraPipe: CameraPipeSimulator

    @Before
    fun setUp() {
        // Arrange: Create more realistic fake metadata that includes a StreamConfigurationMap.
        fakeMetadata0 = createFakeMetadata("0")
        fakeMetadata1 =
            createFakeMetadata("1", CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)

        val cameraMetadataMap =
            mapOf(FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to listOf(fakeMetadata0, fakeMetadata1))
        fakeCameraDevices =
            FakeCameraDevices(
                defaultCameraBackendId = FakeCameraBackend.FAKE_CAMERA_BACKEND_ID,
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap = cameraMetadataMap,
            )
        fakeCameraPipe = CameraPipeSimulator.create(TestScope(), context)

        whenever(mockCameraAppComponent.getCameraDevices()).thenReturn(fakeCameraDevices)
        whenever(mockCameraAppComponent.getCameraPipe()).thenReturn(fakeCameraPipe)
    }

    @Test
    fun constructor_initializesSuccessfully_withValidCameras() {
        // Act: Create the adapter with an initial set of valid camera IDs.
        val surfaceAdapter = CameraSurfaceAdapter(context, mockCameraAppComponent, setOf("0", "1"))

        // Assert: The adapter should be usable for both cameras.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("0")).isTrue()
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("1")).isTrue()
    }

    @Test
    fun constructor_throwsInitializationException_whenInitializationFails() {
        // Arrange: Create a special CameraDevices that will fail for camera "1".
        val failingCameraDevices = ThrowingCameraDevices(failingId = CameraId("1"))
        whenever(mockCameraAppComponent.getCameraDevices()).thenReturn(failingCameraDevices)

        // Act & Assert: The constructor should fail and throw an InitializationException.
        assertThrows<InitializationException> {
            CameraSurfaceAdapter(context, mockCameraAppComponent, setOf("0", "1"))
        }
    }

    @Test
    fun onCamerasUpdated_removesStaleCameraData() {
        // Arrange: Initialize with two cameras.
        val surfaceAdapter = CameraSurfaceAdapter(context, mockCameraAppComponent, setOf("0", "1"))
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("0")).isTrue()

        // Act: Update with a list that now only contains camera "1".
        surfaceAdapter.onCamerasUpdated(listOf("1"))

        // Assert: Querying for the removed camera "0" now fails.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("0")).isFalse()
        // Assert: Querying for camera "1" still succeeds.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("1")).isTrue()
    }

    @Test
    fun onCamerasUpdated_addsNewCameraData() {
        // Arrange: Initialize with only camera "0".
        val surfaceAdapter = CameraSurfaceAdapter(context, mockCameraAppComponent, setOf("0"))
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("1")).isFalse()

        // Act: Update with a list that includes the new camera "1".
        surfaceAdapter.onCamerasUpdated(listOf("0", "1"))

        // Assert: Querying for the newly added camera "1" now succeeds.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("1")).isTrue()
    }

    @Test
    fun onCamerasUpdated_abortsAndThrows_whenUpdateFails() {
        // Arrange: Initialize with camera "0".
        val surfaceAdapter = CameraSurfaceAdapter(context, mockCameraAppComponent, setOf("0"))

        // Create a special CameraDevices that will fail when metadata for "1" is requested,
        // but will succeed for "0" and "2".
        val failingCameraDevices =
            ThrowingCameraDevices(failingId = CameraId("1"), underlyingFake = fakeCameraDevices)
        whenever(mockCameraAppComponent.getCameraDevices()).thenReturn(failingCameraDevices)

        // Act & Assert: The update call, which includes a good camera ("2") and a bad
        // camera ("1"), should fail with CameraUpdateException.
        assertThrows<CameraUpdateException> {
            surfaceAdapter.onCamerasUpdated(listOf("0", "1", "2"))
        }

        // Assert: The internal state must not have changed.
        // 1. The original camera "0" must still exist.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("0")).isTrue()
        // 2. The "bad" camera "1" should not have been added.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("1")).isFalse()
        // 3. The "good" new camera "2" should NOT have been partially added.
        assertThat(surfaceAdapter.checkIfSupportedCombinationExist("2")).isFalse()
    }

    /** Helper to create fake metadata with a valid StreamConfigurationMap. */
    private fun createFakeMetadata(
        cameraIdString: String,
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY,
    ): CameraMetadata {
        val mockMap: StreamConfigurationMap = mock()
        whenever(mockMap.getOutputSizes(anyInt())).thenReturn(arrayOf(Size(640, 480)))
        whenever(mockMap.getOutputSizes(SurfaceTexture::class.java))
            .thenReturn(arrayOf(Size(640, 480)))

        val characteristicsMap =
            mapOf(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel,
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP to mockMap,
            )
        return FakeCameraMetadata(
            cameraId = CameraId(cameraIdString),
            characteristics = characteristicsMap,
        )
    }

    /**
     * A test-only implementation of [CameraDevices] that delegates all calls to a
     * [FakeCameraDevices] instance, except for `awaitCameraMetadata`, which will throw an exception
     * for a specific camera ID.
     */
    private class ThrowingCameraDevices(
        private val failingId: CameraId,
        private val underlyingFake: FakeCameraDevices =
            FakeCameraDevices(
                FakeCameraBackend.FAKE_CAMERA_BACKEND_ID,
                emptySet(),
                mapOf(
                    FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to
                        listOf(FakeCameraMetadata(cameraId = failingId))
                ),
            ),
    ) : CameraDevices by underlyingFake {
        override fun awaitCameraMetadata(
            cameraId: CameraId,
            cameraBackendId: CameraBackendId?,
        ): CameraMetadata? {
            if (cameraId == failingId) {
                throw DoNotDisturbException("Failing for test purposes!")
            }
            return underlyingFake.awaitCameraMetadata(cameraId, cameraBackendId)
        }

        override fun awaitCameraMetadata(cameraId: CameraId) = awaitCameraMetadata(cameraId, null)
    }
}
