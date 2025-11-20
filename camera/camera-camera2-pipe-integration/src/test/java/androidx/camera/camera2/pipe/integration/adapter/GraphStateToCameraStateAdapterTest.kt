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

import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.integration.testing.FakeCameraGraph
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock

@RunWith(RobolectricCameraPipeTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class GraphStateToCameraStateAdapterTest {
    private lateinit var fakeCameraStateAdapter: CameraStateAdapter
    private lateinit var fakeCameraGraph: FakeCameraGraph
    private lateinit var graphStateToCameraStateAdapter: GraphStateToCameraStateAdapter

    @Before
    fun setUp() {
        fakeCameraStateAdapter = mock()
        fakeCameraGraph = FakeCameraGraph()
        graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(fakeCameraStateAdapter)
    }

    @Test
    fun forwardsAllEventsInOrder_whenGraphIsInitialized() {
        // Arrange: Initialize the cameraGraph property.
        graphStateToCameraStateAdapter.cameraGraph = fakeCameraGraph
        val testError =
            GraphState.GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)

        val inOrder = inOrder(fakeCameraStateAdapter)

        // Act: Call listener methods in a sequence.
        graphStateToCameraStateAdapter.onGraphStarting()
        graphStateToCameraStateAdapter.onGraphStarted()
        graphStateToCameraStateAdapter.onGraphStopping()
        graphStateToCameraStateAdapter.onGraphError(testError)
        graphStateToCameraStateAdapter.onGraphStopped()

        // Assert: Verify calls in order.
        inOrder
            .verify(fakeCameraStateAdapter)
            .onGraphStateUpdated(fakeCameraGraph, GraphState.GraphStateStarting)
        inOrder
            .verify(fakeCameraStateAdapter)
            .onGraphStateUpdated(fakeCameraGraph, GraphState.GraphStateStarted)
        inOrder
            .verify(fakeCameraStateAdapter)
            .onGraphStateUpdated(fakeCameraGraph, GraphState.GraphStateStopping)
        inOrder.verify(fakeCameraStateAdapter).onGraphStateUpdated(fakeCameraGraph, testError)
        inOrder
            .verify(fakeCameraStateAdapter)
            .onGraphStateUpdated(fakeCameraGraph, GraphState.GraphStateStopped)
    }

    @Test
    fun onGraphError_forwardsErrorWhenActive() {
        // Arrange
        graphStateToCameraStateAdapter.cameraGraph = fakeCameraGraph
        val testError =
            GraphState.GraphStateError(CameraError.ERROR_CAMERA_DEVICE, willAttemptRetry = true)

        // Act
        graphStateToCameraStateAdapter.onGraphStarting()
        graphStateToCameraStateAdapter.onGraphError(testError)

        // Assert
        verify(fakeCameraStateAdapter)
            .onGraphStateUpdated(fakeCameraGraph, GraphState.GraphStateStarting)
        verify(fakeCameraStateAdapter).onGraphStateUpdated(fakeCameraGraph, testError)
    }
}
