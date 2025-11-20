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

package androidx.camera.camera2.pipe.testing

import android.media.ImageReader
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.GraphState.GraphStateError
import androidx.camera.camera2.pipe.OutputId
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.media.ImageSource

/**
 * The base interface for [CameraGraphSimulator] and [FrameGraphSimulator].
 *
 * The simulator does not make (many) assumptions about how the simulator will be used, and for this
 * reason it does not automatically put the underlying graph into a "started" state. In most cases,
 * the test will need start the [CameraGraph], [simulateCameraStarted], and either configure
 * surfaces for the [CameraGraph] or call [initializeSurfaces] to put the graph into a state where
 * it is able to send and simulate interactions with the camera. This mirrors the normal lifecycle
 * of a [CameraGraph]. Tests using [CameraSimulator]s should also close them after they've completed
 * their use of the simulator.
 */
public interface CameraSimulator {
    /** Return true if this [CameraSimulator] has been closed. */
    public val isClosed: Boolean

    /** Simulate [CameraSimulator] starts. */
    public fun simulateCameraStarted()

    /** Simulate [CameraSimulator] stops. */
    public fun simulateCameraStopped()

    /** Simulate [CameraSimulator] modifies. */
    public fun simulateCameraModified()

    /** Simulate [CameraSimulator] has error. */
    public fun simulateCameraError(graphStateError: GraphStateError)

    /**
     * Configure streams in the [CameraSimulator] with fake surfaces that match the size of the
     * first output stream.
     */
    public fun initializeSurfaces()

    /**
     * Utility function to simulate next frame. Returns a [CameraGraphSimulator.FrameSimulator] that
     * can be used to trigger subsequent lifecycle events for the frame, such as capture completion
     * or buffer production.
     */
    public fun simulateNextFrame(
        advanceClockByNanos: Long = 33_366_666 // (2_000_000_000 / (60  / 1.001))
    ): CameraGraphSimulator.FrameSimulator

    /** Utility function to simulate the production of a [FakeImage]s for one or more streams. */
    public fun simulateImage(streamId: StreamId, imageTimestamp: Long, outputId: OutputId? = null)

    /**
     * Utility function to simulate the production of [FakeImage]s for all outputs on a specific
     * [request]. Use [simulateImage] to directly control simulation of individual outputs.
     * [physicalCameraId] should be used to select the correct output id when simulating images from
     * multi-resolution [ImageReader]s and [ImageSource]s
     */
    public fun simulateImages(
        request: Request,
        imageTimestamp: Long,
        physicalCameraId: CameraId? = null,
    )
}
