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

import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InOrder
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricCameraPipeTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class CameraInternalAdapterTest {

    private lateinit var useCaseManager: UseCaseManager
    private lateinit var cameraInfo: CameraInfoInternal
    private lateinit var cameraController: CameraControlInternal
    private lateinit var threads: UseCaseThreads
    private lateinit var cameraStateAdapter: CameraStateAdapter
    private lateinit var cameraConfig: CameraConfig

    // Test dispatcher for controlling coroutines
    private val testDispatcher = UnconfinedTestDispatcher()

    // The class under test
    private lateinit var cameraInternalAdapter: CameraInternalAdapter

    @Before
    fun setUp() = runBlocking {
        // Initialize mocks for all dependencies
        useCaseManager = mock(UseCaseManager::class.java)
        cameraInfo = mock(CameraInfoInternal::class.java)
        cameraController = mock(CameraControlInternal::class.java)
        threads = mock(UseCaseThreads::class.java)
        cameraStateAdapter = mock(CameraStateAdapter::class.java)
        cameraConfig = mock(CameraConfig::class.java)

        // When threads.scope is called, return a scope using our test dispatcher
        // This gives us full control over the asynchronous execution.
        `when`(threads.scope).thenReturn(CoroutineScope(testDispatcher))

        cameraInternalAdapter =
            CameraInternalAdapter(
                cameraConfig,
                useCaseManager,
                cameraInfo,
                cameraController,
                threads,
                cameraStateAdapter,
            )
    }

    @Test
    fun onRemoved_updatesStateThenClosesManager() =
        runTest(testDispatcher) {
            // Act
            cameraInternalAdapter.onRemoved()

            // Assert: Verify the sequence of operations
            // Create an InOrder verifier to check that methods are called in the correct order.
            val inOrder: InOrder = inOrder(cameraStateAdapter, useCaseManager)

            // 1. Verify that onRemoved() was called first on the state adapter for an immediate
            // update.
            inOrder.verify(cameraStateAdapter).onRemoved()

            // 2. Verify that close() was called second on the use case manager for resource
            // cleanup.
            inOrder.verify(useCaseManager).close()
        }

    @Test
    fun release_closesUseCaseManager() =
        runTest(testDispatcher) {
            // Act
            cameraInternalAdapter.release()

            // Assert: Verify that the UseCaseManager is closed, which handles all resource cleanup.
            verify(useCaseManager).close()
        }
}
