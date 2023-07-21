/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.view.Surface
import androidx.camera.camera2.pipe.CameraController.ControllerState.CLOSED
import androidx.camera.camera2.pipe.graph.GraphListener

/**
 * A single [CameraController] handles the state and connection status for a [CameraGraph] instance.
 *
 * Calling [start] should eventually invoke [GraphListener.onGraphStarted] on the listener that was
 * used to create this [CameraController] instance. Creating a [CameraController] should not
 * initiate or start opening the underlying camera as part of the creation process.
 *
 * If the connection fails or the underlying camera encounters a failure that may be recoverable,
 * [GraphListener.onGraphStopped] should be invoked. If the state of the camera changes in any way
 * where a previously submitted request that was previously reject that might now be succeed (such
 * as configuring surfaces such that new surfaces would now be accepted)
 * [GraphListener.onGraphModified] should be invoked.
 *
 * Once [close] is invoked, this instance should not respond to any additional events.
 */
interface CameraController {
    val cameraId: CameraId

    /**
     * Whether the camera is being used in a foreground setting, and thus should be kept open on a
     * best-effort basis, for example continuously retrying on a longer timeout.
     */
    var isForeground: Boolean

    /**
     * Connect and start the underlying camera. This may be called on the main thread and should not
     * make long blocking calls. This may be called opportunistically (eg, whenever a lifecycle
     * indicates the camera should be in a running state)
     */
    fun start()

    /**
     * Disconnect from the underlying camera and stop this session. This may be called on the main
     * thread and should not make long blocking calls.
     */
    fun stop()

    /**
     * Restart the current session. This should basically perform stop() then start(). However, the
     * implementation should handle its internal states correctly, and only restart under the right
     * [CameraStatusMonitor.CameraStatus] and [ControllerState].
     */
    fun tryRestart(cameraStatus: CameraStatusMonitor.CameraStatus)

    /**
     * Close this instance. [start] and [stop] should not be invoked, and any additional calls will
     * be ignored once this method returns. Depending on implementation the underlying camera
     * connection may not be terminated immediately, depending on the [CameraBackend]
     */
    fun close()

    /**
     * Tell the [CameraController] the current mapping between [StreamId] and [Surface]s. This map
     * should always contain at least one entry, and should never contain [StreamId]s that were
     * missing from the [StreamGraph] that was used to create this [CameraController].
     */
    fun updateSurfaceMap(surfaceMap: Map<StreamId, Surface>)

    /**
     * ControllerState indicates the internal state of a [CameraController]. These states are needed
     * to make sure we only invoke [CameraController] methods under the right conditions.
     *
     * The following diagram illustrates the state transitions (all states also have a permissible
     * transition to [CLOSED]).
     *
     *   ```
     *   [STOPPED] --> [STARTED] --> [STOPPING] ---------.--------.
     *      ^              ^             |               |        |
     *      |              |             V               V        |
     *      |              '---------[DISCONNECTED]   [ERROR]     |
     *      |                                                     |
     *      '-----------------------------------------------------'
     *   ```
     */
    abstract class ControllerState internal constructor() {
        /** When the CameraController is started. This is set immediately as start() is called. */
        object STARTED : ControllerState()

        /** When the CameraController is stopping. This is set immediately as stop() is called. */
        object STOPPING : ControllerState()

        /** When the camera is stopped normally. */
        object STOPPED : ControllerState()

        /** When the camera is disconnected and can be later "reconnected". */
        object DISCONNECTED : ControllerState()

        /** When the camera shuts down with an unrecoverable error. */
        object ERROR : ControllerState()

        /** When the CameraController is closed, and no further operations can done on it. */
        object CLOSED : ControllerState()
    }
}
