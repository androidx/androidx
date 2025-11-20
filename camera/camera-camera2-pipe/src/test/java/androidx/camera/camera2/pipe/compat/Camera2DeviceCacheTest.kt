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

package androidx.camera.camera2.pipe.compat

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import androidx.camera.camera2.pipe.testing.FakeThreads
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompat
import androidx.camera.featurecombinationquery.CameraDeviceSetupCompatFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import javax.inject.Provider
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class Camera2DeviceCacheTest {
    private val testScope = TestScope()

    private val fakeCameraManagerProvider =
        object : Provider<CameraManager> {
            val fakeCameraManager: CameraManager = mock()
            private var currentCameraIds = mutableListOf<String>()
            private val captor = argumentCaptor<CameraManager.AvailabilityCallback>()

            override fun get(): CameraManager? {
                return fakeCameraManager
            }

            fun setCameraIds(cameraIds: List<String>) {
                currentCameraIds = cameraIds.toMutableList()

                whenever(fakeCameraManager.cameraIdList).thenReturn(currentCameraIds.toTypedArray())
                whenever(fakeCameraManager.registerAvailabilityCallback(captor.capture(), any()))
                    .then {
                        for (cameraId in currentCameraIds) {
                            captor.lastValue.onCameraAvailable(cameraId)
                        }
                    }
            }

            fun notifyCameraAvailable(cameraId: String) {
                captor.lastValue.onCameraAvailable(cameraId)
            }

            fun notifyCameraUnavailable(cameraId: String) {
                captor.lastValue.onCameraUnavailable(cameraId)
            }
        }

    private val fakeThreads = FakeThreads.fromTestScope(testScope)
    private val packageManager: PackageManager = mock()

    private val context = ApplicationProvider.getApplicationContext() as Context

    private val mockErrorListener: CameraErrorListener = mock()
    val mockDeviceSetupFactoryProvider: Provider<CameraDeviceSetupCompatFactory> = mock()
    val mockDeviceSetupFactory: CameraDeviceSetupCompatFactory = mock()

    private val fakeCameraPipeJob = Job()
    private val fakeCameraPipeLifetime = CameraPipeLifetime(fakeCameraPipeJob)

    private fun setUpPackageManager(enableBack: Boolean, enableFront: Boolean) {
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
            .thenReturn(enableBack)
        whenever(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT))
            .thenReturn(enableFront)
    }

    @Test
    fun testCanGetCameraIds() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0"))
            setUpPackageManager(enableBack = false, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            val cameraIds = camera2DeviceCache.getCameraIds()

            assertThat(cameraIds).containsExactly(CameraId("0"))

            camera2DeviceCache.shutdown()
        }

    @Test
    fun testCanAwaitCameraIds() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            val cameraIds = camera2DeviceCache.awaitCameraIds()

            assertThat(cameraIds).containsExactly(CameraId("0"), CameraId("1"))

            camera2DeviceCache.shutdown()
        }

    @Test
    fun testCanGetCameraIdsFromFlow() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1", "2"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            val cameraIds = camera2DeviceCache.cameraIds.first()

            assertThat(cameraIds).containsExactly(CameraId("0"), CameraId("1"), CameraId("2"))

            camera2DeviceCache.shutdown()
        }

    @Test
    fun testCanGetEmptyCameraIdsFromFlow() =
        testScope.runTest {
            // Test that verifies regardless of the validity of our current camera ID list, we
            // should still get an initial value from the flow. The empty camera ID list is a
            // tricky edge in which no cameras would ever become available, but we should still
            // return an initial value.
            fakeCameraManagerProvider.setCameraIds(emptyList())
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            val cameraIds = camera2DeviceCache.cameraIds.first()
            assertTrue(cameraIds.isEmpty())

            camera2DeviceCache.shutdown()
        }

    @Test
    fun getCameraIdsCachesCameraIdList() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1", "2"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            // Get the camera ID list for the first time.
            val cameraIds = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds).containsExactly(CameraId("0"), CameraId("1"), CameraId("2"))
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // Get the camera ID list for the second time.
            val cameraIds2 = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds2).containsExactly(CameraId("0"), CameraId("1"), CameraId("2"))
            // Since the list is valid, we shouldn't query the camera ID list again.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            camera2DeviceCache.shutdown()
        }

    @Test
    fun getCameraIdsDoesNotCacheCameraIdListWithInvalidList() =
        testScope.runTest {
            // There should be at least 2 cameras if both cameras are supported.
            fakeCameraManagerProvider.setCameraIds(listOf("0"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            // Get the camera ID list for the first time.
            val cameraIds = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds).containsExactly(CameraId("0"))
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // Get the camera ID list for the second time.
            val cameraIds2 = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds2).containsExactly(CameraId("0"))
            // Since the list is invalid, we should query the camera ID list again.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(2)).cameraIdList

            camera2DeviceCache.shutdown()
        }

    @Test
    fun awaitCameraIdsCachesCameraIdList() =
        testScope.runTest {
            // If no cameras are supported, then we can and should cache an empty list.
            fakeCameraManagerProvider.setCameraIds(emptyList())
            setUpPackageManager(enableBack = false, enableFront = false)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            // Get the camera ID list for the first time.
            val cameraIds = camera2DeviceCache.awaitCameraIds()
            assertTrue(cameraIds != null && cameraIds.isEmpty())
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // Get the camera ID list for the second time.
            val cameraIds2 = camera2DeviceCache.awaitCameraIds()
            assertTrue(cameraIds2 != null && cameraIds2.isEmpty())
            // Since the list is valid, we shouldn't query the camera ID list again.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            camera2DeviceCache.shutdown()
        }

    @Test
    fun awaitCameraIdsDoesNotCacheCameraIdListWithInvalidList() =
        testScope.runTest {
            // There should be at least 1 camera if one of the cameras is supported.
            fakeCameraManagerProvider.setCameraIds(emptyList())
            setUpPackageManager(enableBack = true, enableFront = false)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            // Get the camera ID list for the first time. Note that even if the camera ID list is
            // invalid, the query should still succeed.
            val cameraIds = camera2DeviceCache.awaitCameraIds()
            assertNotNull(cameraIds)
            assertTrue(cameraIds.isEmpty())
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // Get the camera ID list for the second time.
            val cameraIds2 = camera2DeviceCache.awaitCameraIds()
            assertNotNull(cameraIds2)
            assertTrue(cameraIds2.isEmpty())
            // Since the list is invalid, we should query the camera ID list again.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(2)).cameraIdList

            camera2DeviceCache.shutdown()
        }

    @Test
    fun cameraIdsFlowCachesCameraIdList() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            val job = launch {
                camera2DeviceCache.cameraIds.collect {
                    assertThat(it).containsExactly(CameraId("0"), CameraId("1"))
                }
            }
            advanceUntilIdle()

            // When camera IDs are collected, we mark all cameras as available. The initial
            // camera ID lists should return [0, 1], so there should be only one query.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // A previously available camera is available again, this should not trigger a new
            // query as it should be cached.
            fakeCameraManagerProvider.notifyCameraAvailable("0")
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            job.cancel()
            camera2DeviceCache.shutdown()
        }

    @Test
    fun cameraIdsFlowDoesNotCacheCameraIdListWithInvalidList() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(emptyList())
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )
            val collectedCameraIds = mutableListOf<List<CameraId>>()

            val job = launch { camera2DeviceCache.cameraIds.collect { collectedCameraIds.add(it) } }
            advanceUntilIdle()
            // Make sure we still get a value even if no cameras were ever available.
            assertThat(collectedCameraIds).containsExactly(listOf<CameraId>())
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // A new camera is available. This should trigger a new query, though the list is still
            // invalid and shouldn't be cached.
            fakeCameraManagerProvider.setCameraIds(listOf("0"))
            fakeCameraManagerProvider.notifyCameraAvailable("0")
            advanceUntilIdle()
            assertThat(collectedCameraIds)
                .containsExactly(listOf<CameraId>(), listOf(CameraId("0")))
            verify(fakeCameraManagerProvider.fakeCameraManager, times(2)).cameraIdList

            // Get camera IDs. Given the list is still invalid, we should make another query.
            val cameraIds = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds).containsExactly(CameraId("0"))
            verify(fakeCameraManagerProvider.fakeCameraManager, times(3)).cameraIdList
            // Given the list is unchanged, we shouldn't be collecting any new values.
            assertThat(collectedCameraIds)
                .containsExactly(listOf<CameraId>(), listOf(CameraId("0")))

            // Mark camera 1 as available. The list is now valid.
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            fakeCameraManagerProvider.notifyCameraAvailable("1")
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(4)).cameraIdList
            assertThat(collectedCameraIds)
                .containsExactly(
                    listOf<CameraId>(),
                    listOf(CameraId("0")),
                    listOf(CameraId("0"), CameraId("1")),
                )

            // The list should be cached from now on.
            val cameraIds2 = camera2DeviceCache.awaitCameraIds()
            assertNotNull(cameraIds2)
            assertThat(cameraIds2).containsExactly(CameraId("0"), CameraId("1"))
            verify(fakeCameraManagerProvider.fakeCameraManager, times(4)).cameraIdList
            assertThat(collectedCameraIds)
                .containsExactly(
                    listOf<CameraId>(),
                    listOf(CameraId("0")),
                    listOf(CameraId("0"), CameraId("1")),
                )

            job.cancel()
            camera2DeviceCache.shutdown()
        }

    @Test
    fun cameraIdsFlowUpdatesWhenCameraIdListChanges() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )
            val collectedCameraIds = mutableListOf<List<CameraId>>()

            val job = launch { camera2DeviceCache.cameraIds.collect { collectedCameraIds.add(it) } }
            advanceUntilIdle()

            // When camera IDs are collected, we mark all cameras as available. The initial
            // camera ID lists should return [0, 1], so there should be only one query.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList
            assertThat(collectedCameraIds).containsExactly(listOf(CameraId("0"), CameraId("1")))

            // A new camera - Camera 2 shows up
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1", "2"))
            fakeCameraManagerProvider.notifyCameraAvailable("2")
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(2)).cameraIdList
            assertThat(collectedCameraIds)
                .containsExactly(
                    listOf(CameraId("0"), CameraId("1")),
                    listOf(CameraId("0"), CameraId("1"), CameraId("2")),
                )

            // Camera 2 is removed (unplugged).
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            fakeCameraManagerProvider.notifyCameraUnavailable("2")
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(3)).cameraIdList
            assertThat(collectedCameraIds)
                .containsExactly(
                    listOf(CameraId("0"), CameraId("1")),
                    listOf(CameraId("0"), CameraId("1"), CameraId("2")),
                    listOf(CameraId("0"), CameraId("1")),
                )

            job.cancel()
            camera2DeviceCache.shutdown()
        }

    @Test
    fun cameraIdListDoesNotUpdateWhenCameraIdsGoBelowMinimum() =
        testScope.runTest {
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            setUpPackageManager(enableBack = true, enableFront = true)
            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )
            val collectedCameraIds = mutableListOf<List<CameraId>>()

            val job = launch { camera2DeviceCache.cameraIds.collect { collectedCameraIds.add(it) } }
            advanceUntilIdle()

            val cameraIds = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds).containsExactly(CameraId("0"), CameraId("1"))
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1)).cameraIdList

            // Let's say camera 1 encounters an error and becomes unavailable. In such a case we
            // would dip below the minimum expected cameras, but we should still get the cached
            // cameras.
            fakeCameraManagerProvider.setCameraIds(listOf("0"))
            fakeCameraManagerProvider.notifyCameraUnavailable("1")
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(2)).cameraIdList
            val cameraIds2 = camera2DeviceCache.getCameraIds()
            assertThat(cameraIds2).containsExactly(CameraId("0"), CameraId("1"))
            assertThat(collectedCameraIds).containsExactly(listOf(CameraId("0"), CameraId("1")))

            // Then camera 1 comes back. Nothing should change.
            fakeCameraManagerProvider.setCameraIds(listOf("0", "1"))
            fakeCameraManagerProvider.notifyCameraAvailable("1")
            advanceUntilIdle()
            // Our cache should remain [0, 1], so camera 1 becoming available shouldn't trigger
            // a new query.
            verify(fakeCameraManagerProvider.fakeCameraManager, times(2)).cameraIdList
            val cameraIds3 = camera2DeviceCache.awaitCameraIds()
            assertThat(cameraIds3).containsExactly(CameraId("0"), CameraId("1"))
            assertThat(collectedCameraIds).containsExactly(listOf(CameraId("0"), CameraId("1")))

            job.cancel()
            camera2DeviceCache.shutdown()
        }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    @Test
    fun cameraDeviceSetupCompatIsCached() =
        testScope.runTest {
            val cameraId = CameraId("0")
            val mockSetupCompat: CameraDeviceSetupCompat = mock()
            whenever(mockDeviceSetupFactoryProvider.get()).thenReturn(mockDeviceSetupFactory)

            whenever(mockDeviceSetupFactoryProvider.get().getCameraDeviceSetupCompat(any()))
                .thenReturn(mockSetupCompat)

            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            // First call should invoke the factory
            camera2DeviceCache.getOrInitializeDeviceSetupCompat(cameraId)
            advanceUntilIdle()
            verify(mockDeviceSetupFactoryProvider.get(), times(1))
                .getCameraDeviceSetupCompat(cameraId.value)

            // Second call should return the cached value without invoking the factory again.
            camera2DeviceCache.getOrInitializeDeviceSetupCompat(cameraId)
            advanceUntilIdle()
            verify(mockDeviceSetupFactoryProvider.get(), times(1))
                .getCameraDeviceSetupCompat(cameraId.value)

            camera2DeviceCache.shutdown()
        }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
    @Test
    fun cameraDeviceSetupWrapperIsCached() =
        testScope.runTest {
            val cameraId = CameraId("0")
            val mockCameraDeviceSetup: CameraDevice.CameraDeviceSetup = mock()

            whenever(mockDeviceSetupFactoryProvider.get()).thenReturn(mockDeviceSetupFactory)
            whenever(
                    fakeCameraManagerProvider.fakeCameraManager.isCameraDeviceSetupSupported(
                        cameraId.value
                    )
                )
                .thenReturn(true)
            whenever(
                    fakeCameraManagerProvider.fakeCameraManager.getCameraDeviceSetup(cameraId.value)
                )
                .thenReturn(mockCameraDeviceSetup)

            val camera2DeviceCache =
                Camera2DeviceCache(
                    fakeCameraManagerProvider,
                    fakeThreads,
                    context,
                    packageManager,
                    mockErrorListener,
                    mockDeviceSetupFactoryProvider,
                    fakeCameraPipeLifetime,
                    fakeCameraPipeJob,
                )

            // First call should invoke the cameraManager
            camera2DeviceCache.getOrInitializeDeviceSetupWrapper(cameraId)
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1))
                .getCameraDeviceSetup(cameraId.value)

            // Second call should return the cached value
            camera2DeviceCache.getOrInitializeDeviceSetupWrapper(cameraId)
            advanceUntilIdle()
            verify(fakeCameraManagerProvider.fakeCameraManager, times(1))
                .getCameraDeviceSetup(cameraId.value)

            camera2DeviceCache.shutdown()
        }
}
