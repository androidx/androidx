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

package androidx.camera.camera2.pipe.integration.adapter

import android.os.Build
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.internal.CameraGraphCreator
import androidx.camera.camera2.pipe.integration.testing.FakeCameraInfoAdapterCreator
import androidx.camera.camera2.pipe.testing.FakeCameraDevices
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_SINGLE
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
import androidx.camera.core.impl.CameraInfoInternal
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class CameraCoordinatorAdapterTest {

    private val cameraMetadata = FakeCameraMetadata()

    private val cameraDevices = FakeCameraDevices(
        defaultCameraBackendId = CameraBackendId("0"),
        concurrentCameraBackendIds = setOf(
            setOf(CameraBackendId("0"), CameraBackendId("1")),
            setOf(CameraBackendId("0"), CameraBackendId("2"))
        ),
        cameraMetadataMap = mapOf(CameraBackendId("0") to listOf(cameraMetadata))
    )

    private val mockCameraGraphCreator: CameraGraphCreator = mock()
    private val mockCameraInternalAdapter0: CameraInternalAdapter = mock()
    private val mockCameraInternalAdapter1: CameraInternalAdapter = mock()
    private val mockCameraInternalAdapter2: CameraInternalAdapter = mock()

    private val cameraCoordinatorAdapter = CameraCoordinatorAdapter(
        cameraDevices, mockCameraGraphCreator)

    @Before
    fun setUp() {
        cameraCoordinatorAdapter.registerCamera("0", mockCameraInternalAdapter0)
        cameraCoordinatorAdapter.registerCamera("1", mockCameraInternalAdapter1)
        cameraCoordinatorAdapter.registerCamera("2", mockCameraInternalAdapter2)
    }

    @Test
    fun getConcurrentCameraSelectors() {
        val cameraSelectors = cameraCoordinatorAdapter.concurrentCameraSelectors
        assertThat(cameraSelectors.size).isEqualTo(2)
        assertThat(cameraSelectors[0].size).isEqualTo(2)
        assertThat(cameraSelectors[1].size).isEqualTo(2)
    }

    @Test
    fun setAndGetActiveConcurrentCameraInfos() {
        cameraCoordinatorAdapter.activeConcurrentCameraInfos = mutableListOf(
            FakeCameraInfoAdapterCreator.createCameraInfoAdapter(cameraId = CameraId("0")),
            FakeCameraInfoAdapterCreator.createCameraInfoAdapter(cameraId = CameraId("1")))

        assertThat(cameraCoordinatorAdapter.activeConcurrentCameraInfos.size).isEqualTo(2)
        val cameraInfo0 = cameraCoordinatorAdapter.activeConcurrentCameraInfos[0]
            as CameraInfoInternal
        assertThat(cameraInfo0.cameraId).isEqualTo("0")
        val cameraInfo1 = cameraCoordinatorAdapter.activeConcurrentCameraInfos[1]
            as CameraInfoInternal
        assertThat(cameraInfo1.cameraId).isEqualTo("1")
        verify(mockCameraInternalAdapter0).resumeRefresh()
        verify(mockCameraInternalAdapter1).resumeRefresh()
    }

    @Test
    fun getPairedConcurrentCameraId() {
        assertThat(cameraCoordinatorAdapter.getPairedConcurrentCameraId("0")).isNull()

        cameraCoordinatorAdapter.activeConcurrentCameraInfos = mutableListOf(
            FakeCameraInfoAdapterCreator.createCameraInfoAdapter(cameraId = CameraId("0")),
            FakeCameraInfoAdapterCreator.createCameraInfoAdapter(cameraId = CameraId("1")))

        assertThat(cameraCoordinatorAdapter.getPairedConcurrentCameraId("0")).isEqualTo("1")
    }

    @Test
    fun setAndGetCameraOperatingMode() {
        cameraCoordinatorAdapter.cameraOperatingMode = CAMERA_OPERATING_MODE_CONCURRENT

        verify(mockCameraInternalAdapter0).pauseRefresh()
        verify(mockCameraInternalAdapter0, never()).resumeRefresh()
        verify(mockCameraInternalAdapter1).pauseRefresh()
        verify(mockCameraInternalAdapter1, never()).resumeRefresh()
        verify(mockCameraGraphCreator).setConcurrentModeOn(true)
        assertThat(cameraCoordinatorAdapter.cameraOperatingMode)
            .isEqualTo(CAMERA_OPERATING_MODE_CONCURRENT)

        cameraCoordinatorAdapter.cameraOperatingMode = CAMERA_OPERATING_MODE_SINGLE

        verify(mockCameraInternalAdapter0).resumeRefresh()
        verify(mockCameraInternalAdapter1).resumeRefresh()
        verify(mockCameraGraphCreator).setConcurrentModeOn(false)
        assertThat(cameraCoordinatorAdapter.cameraOperatingMode)
            .isEqualTo(CAMERA_OPERATING_MODE_SINGLE)
    }

    @Test
    fun shutdown() {
        cameraCoordinatorAdapter.cameraOperatingMode = CAMERA_OPERATING_MODE_CONCURRENT
        cameraCoordinatorAdapter.activeConcurrentCameraInfos = mutableListOf(
            FakeCameraInfoAdapterCreator.createCameraInfoAdapter(cameraId = CameraId("0")),
            FakeCameraInfoAdapterCreator.createCameraInfoAdapter(cameraId = CameraId("1")))

        cameraCoordinatorAdapter.shutdown()

        assertThat(cameraCoordinatorAdapter.cameraInternalMap).isEmpty()
        assertThat(cameraCoordinatorAdapter.activeConcurrentCameraInfos).isEmpty()
        assertThat(cameraCoordinatorAdapter.concurrentCameraIdMap).isEmpty()
        assertThat(cameraCoordinatorAdapter.concurrentCameraIdsSet).isEmpty()
        assertThat(cameraCoordinatorAdapter.cameraOperatingMode).isEqualTo(
            CAMERA_OPERATING_MODE_UNSPECIFIED)
        assertThat(cameraCoordinatorAdapter.concurrentModeOn).isFalse()
    }
}
