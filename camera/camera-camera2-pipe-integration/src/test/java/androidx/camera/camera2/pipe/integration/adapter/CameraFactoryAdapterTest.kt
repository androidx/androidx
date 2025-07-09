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

package androidx.camera.camera2.pipe.integration.adapter

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.testing.FakeCameraBackend
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.CameraSelector
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.core.internal.StreamSpecsCalculator.Companion.NO_OP_STREAM_SPECS_CALCULATOR
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.util.ReflectionHelpers

@ExperimentalCoroutinesApi
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraFactoryAdapterTest {
    private val testScope = TestScope()
    private lateinit var threadConfig: CameraThreadConfig
    private val originalFingerprint = Build.FINGERPRINT

    private val metadata0 = createFakeMetadata("0", CameraCharacteristics.LENS_FACING_BACK, true)
    private val metadata1 = createFakeMetadata("1", CameraCharacteristics.LENS_FACING_FRONT, true)
    private val metadata2 = createFakeMetadata("2", CameraCharacteristics.LENS_FACING_BACK, true)
    private val metadata3 = createFakeMetadata("3", CameraCharacteristics.LENS_FACING_BACK, false)

    private lateinit var fakeCameraDevices: FakeDynamicCameraDevices
    private lateinit var fakeCameraPipe: CameraPipe

    @Before
    fun setUp() {
        threadConfig =
            CameraThreadConfig.create(
                CameraXExecutors.mainThreadExecutor(),
                Handler(Looper.getMainLooper()),
            )

        // 1. Create a standard FakeCameraDevices that knows about all possible cameras' metadata.
        val baseFakeDevices =
            FakeCameraDevices(
                defaultCameraBackendId = FakeCameraBackend.FAKE_CAMERA_BACKEND_ID,
                concurrentCameraBackendIds = emptySet(),
                cameraMetadataMap =
                    mapOf(
                        FakeCameraBackend.FAKE_CAMERA_BACKEND_ID to
                            listOf(metadata0, metadata1, metadata2, metadata3)
                    ),
            )

        // 2. Create our delegating fake, overriding the cameraIdsFlow to be controllable.
        fakeCameraDevices = FakeDynamicCameraDevices(baseFakeDevices)

        // 3. A minimal fake CameraPipe that returns our dynamic CameraDevices instance.
        fakeCameraPipe =
            object : CameraPipe by mock() {
                override fun cameras(): CameraDevices = fakeCameraDevices
            }
    }

    @After
    fun tearDown() {
        // Restore the original fingerprint after each test
        setFingerprint(originalFingerprint)
    }

    @Test
    fun constructor_initializesWithAllCompatibleCameras_whenNoSelector() =
        testScope.runTest {
            // Arrange
            setFingerprint("fake-fingerprint") // Trigger backward compatibility filter

            // Act
            val factory = createCameraFactoryAdapter(null)

            // Assert
            assertThat(factory.availableCameraIds).containsExactly("0", "1", "2")
        }

    @Test
    fun constructor_initializesWithFilteredCameras_whenSelectorIsUsed() =
        testScope.runTest {
            // Arrange
            setFingerprint("fake-fingerprint") // Trigger backward compatibility filter

            // Act
            val factory = createCameraFactoryAdapter(CameraSelector.DEFAULT_BACK_CAMERA)

            // Assert
            assertThat(factory.availableCameraIds).containsExactly("0", "2")
        }

    @Test
    fun constructor_notFilterIncompatibleCameras_whenBuildFingerprintIsRobolectric() =
        testScope.runTest {
            // Arrange
            setFingerprint("robolectric") // Should skip backward compatibility filter

            // Act
            val factory = createCameraFactoryAdapter(null)

            // Assert
            assertThat(factory.availableCameraIds).containsExactly("0", "1", "2", "3")
        }

    @Test
    fun onCameraIdsUpdated_refreshesAndFiltersList() =
        testScope.runTest {
            // Arrange
            setFingerprint("fake-fingerprint")
            val factory = createCameraFactoryAdapter(null)

            // Assert initial state
            assertThat(factory.availableCameraIds).containsExactly("0", "1", "2")

            // Act: Simulate camera "1" being removed from the system
            factory.onCameraIdsUpdated(listOf("0", "2", "3"))

            // Assert: The list should be re-filtered. "3" is incompatible, "1" is gone.
            assertThat(factory.availableCameraIds).containsExactly("0", "2")
        }

    @Test
    fun onCameraIdsUpdated_appliesCameraSelectorToNewList() =
        testScope.runTest {
            // Arrange
            setFingerprint("fake-fingerprint")
            val factory = createCameraFactoryAdapter(CameraSelector.DEFAULT_BACK_CAMERA)

            // Assert initial state is correct (filters by selector AND compatibility)
            assertThat(factory.availableCameraIds).containsExactly("0", "2")

            // Act: Simulate a new compatible BACK camera ("4") being added, while "1" (FRONT) is
            // removed.
            val metadata4 = createFakeMetadata("4", CameraCharacteristics.LENS_FACING_BACK, true)
            fakeCameraDevices.addMetadata(metadata4) // Make the backend aware of the new camera
            // Simulates a new list to trigger the update.
            factory.onCameraIdsUpdated(
                listOf(metadata0.camera, metadata2.camera, metadata3.camera, metadata4.camera).map {
                    it.value
                }
            )

            // Assert: The selector is re-applied. "3" is filtered out for compat.
            assertThat(factory.availableCameraIds).containsExactly("0", "2", "4")
        }

    @Test
    fun shutdown_callsShutdownOnDependencies() =
        testScope.runTest {
            // Arrange
            var isShutdown = false
            val cameraPipe =
                object : CameraPipe by fakeCameraPipe {
                    override fun shutdown() {
                        isShutdown = true
                    }
                }

            val factory = createCameraFactoryAdapter(null, cameraPipe)

            // Act
            factory.shutdown()

            // Assert
            assertThat(isShutdown).isTrue()
        }

    private fun setFingerprint(fingerprint: String) {
        ReflectionHelpers.setStaticField(Build::class.java, "FINGERPRINT", fingerprint)
    }

    private fun createFakeMetadata(
        cameraId: String,
        lensFacing: Int,
        hasCompatCap: Boolean,
    ): FakeCameraMetadata {
        val capabilities =
            if (hasCompatCap) {
                intArrayOf(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
            } else {
                intArrayOf()
            }
        return FakeCameraMetadata(
            cameraId = CameraId(cameraId),
            characteristics =
                mapOf(
                    CameraCharacteristics.LENS_FACING to lensFacing,
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to
                        CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES to capabilities,
                ),
        )
    }

    private fun createCameraFactoryAdapter(
        availableCameraSelector: CameraSelector?,
        cameraPipe: CameraPipe = fakeCameraPipe,
    ) =
        CameraFactoryAdapter(
            lazyOf(cameraPipe),
            ApplicationProvider.getApplicationContext(),
            threadConfig,
            CameraInteropStateCallbackRepository(),
            availableCameraSelector,
            NO_OP_STREAM_SPECS_CALCULATOR,
        )

    /**
     * A test-only class that delegates to a standard [FakeCameraDevices] but overrides the methods
     * related to camera presence to allow for dynamic updates during a test.
     */
    private class FakeDynamicCameraDevices(private val delegate: FakeCameraDevices) :
        CameraDevices by delegate {
        private var dynamicCameraIdsFlow = MutableStateFlow<List<CameraId>>(emptyList())
        private val dynamicMetadataMap =
            mutableMapOf<CameraId, androidx.camera.camera2.pipe.CameraMetadata>()

        init {
            // Initialize with the delegate's state to be in sync.
            val initialIds = delegate.awaitCameraIds() ?: emptyList()
            for (id in initialIds) {
                val metadata = delegate.awaitCameraMetadata(id)
                if (metadata != null) {
                    dynamicMetadataMap[id] = metadata
                }
            }
            dynamicCameraIdsFlow = MutableStateFlow(initialIds)
        }

        fun addMetadata(metadata: androidx.camera.camera2.pipe.CameraMetadata) {
            dynamicMetadataMap[metadata.camera] = metadata
            dynamicCameraIdsFlow.value = dynamicMetadataMap.map { it.key }
        }

        // --- Override ONLY the methods we need to control ---
        override suspend fun getCameraIds(cameraBackendId: CameraBackendId?): List<CameraId>? =
            dynamicCameraIdsFlow.value

        override suspend fun getCameraMetadata(
            cameraId: CameraId,
            cameraBackendId: CameraBackendId?,
        ): androidx.camera.camera2.pipe.CameraMetadata? = dynamicMetadataMap[cameraId]

        override fun cameraIdsFlow(cameraBackendId: CameraBackendId?): Flow<List<CameraId>> =
            dynamicCameraIdsFlow

        override fun awaitCameraIds(cameraBackendId: CameraBackendId?): List<CameraId>? =
            dynamicCameraIdsFlow.value

        override fun awaitCameraMetadata(
            cameraId: CameraId,
            cameraBackendId: CameraBackendId?,
        ): androidx.camera.camera2.pipe.CameraMetadata? {
            return dynamicMetadataMap[cameraId]
        }
    }
}
