/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.core.impl.CameraSessionLifecycleCallback
import com.google.common.util.concurrent.MoreExecutors
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class CameraSessionLifecycleAdapterTest {
    private lateinit var sessionLifecycleAdapter: CameraSessionLifecycleAdapter
    private val executor = MoreExecutors.directExecutor()

    @Before
    fun setUp() {
        sessionLifecycleAdapter = CameraSessionLifecycleAdapter()
    }

    @Test
    fun addSessionLifecycleCallback_addsCallbackAndDispatchesEvents() {
        // Arrange
        val callback = mock(CameraSessionLifecycleCallback::class.java)
        sessionLifecycleAdapter.addSessionLifecycleCallback(executor, callback)

        // Act & Assert (Started State)
        sessionLifecycleAdapter.dispatchSessionLifecycle(GraphState.GraphStateStarted)
        verify(callback).onSessionStarted()

        // Act & Assert (Stopped State)
        sessionLifecycleAdapter.dispatchSessionLifecycle(GraphState.GraphStateStopped)
        verify(callback).onSessionStopped()

        // Act & Assert (Error State)
        val error =
            GraphState.GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = false)
        sessionLifecycleAdapter.dispatchSessionLifecycle(error)
        verify(callback).onSessionError()
    }

    @Test
    fun addSessionLifecycleCallback_preventsDuplicateRegistrations() {
        // Arrange
        val callback = mock(CameraSessionLifecycleCallback::class.java)
        sessionLifecycleAdapter.addSessionLifecycleCallback(executor, callback)
        sessionLifecycleAdapter.addSessionLifecycleCallback(executor, callback) // Duplicate

        // Act
        sessionLifecycleAdapter.dispatchSessionLifecycle(GraphState.GraphStateStopped)

        // Assert: Ensure it was registered only once, so verified exactly 1 invocation.
        verify(callback).onSessionStopped()
    }

    @Test
    fun removeSessionLifecycleCallback_stopsDispatchingEvents() {
        // Arrange
        val callback = mock(CameraSessionLifecycleCallback::class.java)
        sessionLifecycleAdapter.addSessionLifecycleCallback(executor, callback)

        // Act
        sessionLifecycleAdapter.removeSessionLifecycleCallback(callback)
        sessionLifecycleAdapter.dispatchSessionLifecycle(GraphState.GraphStateStarted)

        // Assert: Callback should not receive any updates after removal.
        verify(callback, never()).onSessionStarted()
    }
}
