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

import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.impl.UseCaseManager
import androidx.camera.camera2.impl.UseCaseThreads
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InOrder
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock as ktMock
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
            val releaseFuture = cameraInternalAdapter.release()
            val secondReleaseFuture = cameraInternalAdapter.release()

            // Assert: Verify that UseCaseManager.close() runs once, the release future completes
            // normally (not self-cancelled), and threads.scope is cancelled afterward.
            verify(useCaseManager, times(1)).close()
            assertThat(releaseFuture.isDone).isTrue()
            assertThat(releaseFuture.isCancelled).isFalse()
            assertThat(releaseFuture.get()).isNull()
            assertThat(threads.scope.isActive).isFalse()
            assertThat(secondReleaseFuture.isDone).isTrue()
            assertThat(secondReleaseFuture.isCancelled).isFalse()
            assertThat(secondReleaseFuture.get()).isNull()
        }

    @Test
    fun release_suspendingClose_cancelsScopeAfterCompletionAndIsolatesCallerCancel() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val closeGate = CompletableDeferred<Unit>()
        val suspendingManager =
            ktMock<UseCaseManager> {
                onBlocking { close() } doSuspendableAnswer { closeGate.await() }
            }
        val suspendingThreads = mock(UseCaseThreads::class.java)
        `when`(suspendingThreads.scope).thenReturn(scope)

        val adapter =
            CameraInternalAdapter(
                cameraConfig,
                suspendingManager,
                cameraInfo,
                cameraController,
                suspendingThreads,
                cameraStateAdapter,
            )

        val firstFuture = adapter.release()
        val secondFuture = adapter.release()
        testScheduler.advanceUntilIdle()

        assertThat(firstFuture.isDone).isFalse()
        assertThat(secondFuture.isDone).isFalse()
        assertThat(scope.isActive).isTrue()

        // Cancelling one caller's pending future must not cancel other waiters or abort close().
        firstFuture.cancel(true)
        assertThat(firstFuture.isCancelled).isTrue()
        assertThat(secondFuture.isDone).isFalse()
        assertThat(scope.isActive).isTrue()

        closeGate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertThat(secondFuture.isDone).isTrue()
        assertThat(secondFuture.isCancelled).isFalse()
        assertThat(secondFuture.get()).isNull()
        assertThat(scope.isActive).isFalse()

        val thirdFuture = adapter.release()
        assertThat(thirdFuture.isDone).isTrue()
        assertThat(thirdFuture.isCancelled).isFalse()
        verify(suspendingManager, times(1)).close()
    }
}
